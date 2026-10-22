package cn.tarot;

import java.io.File;
import java.lang.reflect.*;
import java.nio.file.Files;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.inventory.*;
import org.bukkit.potion.*;

/** Runs real activation, persistence and attribute replacement behind a native-use player fixture. */
final class ActivationStateTest {
    private static int checks;

    @SuppressWarnings("unchecked")
    static void run(TarotPlugin plugin) {
        check(Bukkit.isPrimaryThread(), "server thread");
        checks = 0;
        Map<UUID, Core.Active> active = null, savedActive = null;
        Map<UUID, Long> cooldowns = null, savedCooldowns = null, gestures = null, savedGestures = null;
        Set<UUID> icons = null, savedIcons = null;
        File originalFile = null, temporaryFile = null;
        long originalDuration = 0, originalLock = 0;
        boolean originalIcon = false, settingsSaved = false;
        Fixture fixture = null;
        ChargeController controller = null;
        try {
            originalFile = (File) field("stateFile").get(plugin);
            temporaryFile = Files.createTempFile(plugin.getDataFolder().toPath(), "activation-test-", ".yml").toFile();
            active = (Map<UUID, Core.Active>) field("active").get(plugin); savedActive = new HashMap<>(active);
            cooldowns = (Map<UUID, Long>) field("cooldowns").get(plugin); savedCooldowns = new HashMap<>(cooldowns);
            gestures = (Map<UUID, Long>) field("gestures").get(plugin); savedGestures = new HashMap<>(gestures);
            icons = (Set<UUID>) field("fakeIcon").get(plugin); savedIcons = new HashSet<>(icons);
            originalDuration = field("durationMillis").getLong(plugin);
            originalLock = field("lockMillis").getLong(plugin);
            originalIcon = field("iconEnabled").getBoolean(plugin); settingsSaved = true;
            field("stateFile").set(plugin, temporaryFile);
            field("durationMillis").setLong(plugin, 86_400_000L);
            field("lockMillis").setLong(plugin, 1_800_000L);
            field("iconEnabled").setBoolean(plugin, true);

            Fixture f = fixture = new Fixture();
            check(!f.attributes.isValid(), "attribute fixture never enters the world");
            f.attribute(Attribute.MAX_HEALTH).setBaseValue(20);
            f.attribute(Attribute.ARMOR).setBaseValue(2);
            f.attribute(Attribute.ATTACK_DAMAGE).setBaseValue(3);
            AttributeModifier album = modifier("tarot_album", "max_health", 2);
            AttributeModifier outside = modifier("other_plugin", "health", 1);
            AttributeModifier outsideArmor = modifier("other_plugin", "armor", 4);
            f.attribute(Attribute.MAX_HEALTH).addTransientModifier(album);
            f.attribute(Attribute.MAX_HEALTH).addTransientModifier(outside);
            f.attribute(Attribute.ARMOR).addTransientModifier(outsideArmor);
            f.main = plugin.makeCard(4, 3);
            f.off = plugin.makeCard(22, 5); // A distinct offhand card must never supply the activation.
            ItemStack mainBefore = f.main.clone(), offBefore = f.off.clone();
            controller = new ChargeController(plugin, item -> plugin.cardId(item) > 0,
                player -> (Boolean) invoke(plugin, "canCharge", new Class<?>[]{Player.class}, player),
                (player, progress) -> invoke(plugin, "renderCast", new Class<?>[]{Player.class, double.class}, player, progress),
                player -> invoke(plugin, "activateCard", new Class<?>[]{Player.class}, player));

            check(controller.request(f.player), "initial main-hand charge accepted");
            f.use(0); controller.tick();
            f.used = 98; controller.tick();
            check(!active.containsKey(f.uuid) && !cooldowns.containsKey(f.uuid), "charging does not begin effect or cooldown");
            near(f.attribute(Attribute.MAX_HEALTH).getValue(), 23, "charging does not grant an early bonus");
            long started = System.currentTimeMillis();
            finish(controller, f);
            Core.Active first = active.get(f.uuid);
            check(first != null && first.card() == 4 && first.level() == 3, "native completion activates the main-hand card");
            check(first.activated() >= started && first.activated() <= System.currentTimeMillis(), "deadlines start at successful completion");
            check(first.expires() - first.activated() == 86_400_000L, "effect lasts 24 real hours");
            check(first.unlocks() - first.activated() == 1_800_000L, "switch cooldown lasts 30 real minutes");
            check(Objects.equals(cooldowns.get(f.uuid), first.unlocks()), "completion enters cooldown immediately");
            check(mainBefore.equals(f.main) && offBefore.equals(f.off), "both hands preserve their exact cards");
            check(!canCharge(plugin, f), "a completed activation immediately blocks another charge");
            near(f.attribute(Attribute.MAX_HEALTH).getValue(), 23 + 6 * 1.45, "three-star health bonus reaches real Paper attribute");
            near(f.attribute(Attribute.ARMOR).getValue(), 6 + 2 * 1.45, "three-star armor bonus reaches real Paper attribute");
            near(f.attribute(Attribute.ATTACK_DAMAGE).getValue(), 3, "unrelated attribute unchanged");
            check(f.lastIcon != null && f.lastIcon.getType().equals(PotionEffectType.LUCK)
                && f.lastIcon.getDuration() > 1_727_900 && f.lastIcon.getDuration() <= 1_728_000, "vanilla status icon receives the 24-hour countdown");
            YamlConfiguration persisted = YamlConfiguration.loadConfiguration(temporaryFile);
            check(persisted.getInt("players." + f.uuid + ".card") == 4
                && persisted.getLong("players." + f.uuid + ".unlocks") == first.unlocks(), "successful completion persists its card and cooldown");

            f.main = plugin.makeCard(21, 5);
            check(!controller.request(f.player), "another main-hand card rejected while locked");
            check(active.get(f.uuid).equals(first), "rejected card does not replace current effect");
            near(f.attribute(Attribute.MAX_HEALTH).getValue(), 23 + 6 * 1.45, "locked switch preserves existing attributes");

            // Move the recorded activation into the past: exercise real deadline comparisons without sleeping.
            long earlier = System.currentTimeMillis() - 1_800_001L;
            Core.Active unlocked = new Core.Active(first.card(), first.level(), earlier, earlier + 86_400_000L, earlier + 1_800_000L);
            active.put(f.uuid, unlocked); cooldowns.put(f.uuid, unlocked.unlocks());
            check(canCharge(plugin, f) && !unlocked.expired(System.currentTimeMillis()), "30-minute expiry permits switching during the 24-hour effect");
            f.attributes.setHealth(f.attribute(Attribute.MAX_HEALTH).getValue());
            holdAndFinish(controller, f);
            Core.Active second = active.get(f.uuid);
            check(second != null && second.card() == 21 && second.level() == 5, "unlocked switch installs the new card");
            check(second.expires() - second.activated() == 86_400_000L
                && second.unlocks() - second.activated() == 1_800_000L
                && Objects.equals(cooldowns.get(f.uuid), second.unlocks()), "switch starts a fresh duration and cooldown");
            near(f.attribute(Attribute.MAX_HEALTH).getValue(), 23, "old card's health modifier is removed");
            near(f.attributes.getHealth(), 23, "switch safely clamps health to its new maximum");
            near(f.attribute(Attribute.ARMOR).getValue(), 6 + 6 * 2.1, "new armor replaces the old modifier without stacking");
            near(f.attribute(Attribute.ATTACK_DAMAGE).getValue(), 3 + 5 * 2.1, "new damage bonus applies");
            check(f.attribute(Attribute.MAX_HEALTH).getModifiers().containsAll(List.of(album, outside))
                && f.attribute(Attribute.ARMOR).getModifiers().contains(outsideArmor), "switch preserves collection and other plugins' modifiers");

            reload(plugin, active, cooldowns);
            check(second.equals(active.get(f.uuid)) && Objects.equals(cooldowns.get(f.uuid), second.unlocks()), "reload preserves exact activation and lock deadlines");
            invoke(plugin, "restore", new Class<?>[]{Player.class}, f.player);
            near(f.attribute(Attribute.ARMOR).getValue(), 6 + 6 * 2.1, "restoring a saved card does not stack bonuses");
            check(!canCharge(plugin, f), "reloaded active effect retains switch cooldown");

            reset(plugin, f, "cd");
            Core.Active reset = active.get(f.uuid);
            check(reset.card() == second.card() && reset.level() == second.level()
                && reset.activated() == second.activated() && reset.expires() == second.expires()
                && reset.unlocks() == 0 && !cooldowns.containsKey(f.uuid), "reset cd changes only the lock");
            near(f.attribute(Attribute.ATTACK_DAMAGE).getValue(), 3 + 5 * 2.1, "reset cd preserves the active attribute bonus");
            reload(plugin, active, cooldowns);
            check(reset.equals(active.get(f.uuid)) && canCharge(plugin, f), "reset cd persists and permits another card");

            f.main = plugin.makeCard(4, 1); holdAndFinish(controller, f);
            long retainedDeadline = cooldowns.get(f.uuid);
            reset(plugin, f, "buffer");
            check(!active.containsKey(f.uuid) && Objects.equals(cooldowns.get(f.uuid), retainedDeadline), "reset buffer removes only the effect and retains its lock");
            near(f.attribute(Attribute.MAX_HEALTH).getValue(), 23, "reset buffer removes the tarot health bonus");
            near(f.attribute(Attribute.ARMOR).getValue(), 6, "reset buffer preserves external armor");
            near(f.attribute(Attribute.ATTACK_DAMAGE).getValue(), 3, "reset buffer leaves no former card damage bonus");
            check(!icons.contains(f.uuid) && f.removedIcons > 0, "reset buffer removes the vanilla countdown icon");
            check(!controller.request(f.player), "clearing a buff cannot bypass its retained cooldown");
            reload(plugin, active, cooldowns);
            check(!active.containsKey(f.uuid) && Objects.equals(cooldowns.get(f.uuid), retainedDeadline)
                && !canCharge(plugin, f), "cooldown without an active effect survives reload");
            reset(plugin, f, "cd"); reload(plugin, active, cooldowns);
            check(!active.containsKey(f.uuid) && !cooldowns.containsKey(f.uuid) && canCharge(plugin, f), "reset cd also clears a persisted standalone cooldown");

            f.main = plugin.makeCard(4, 2); holdAndFinish(controller, f);
            long past = System.currentTimeMillis() - 86_400_001L;
            active.put(f.uuid, new Core.Active(4, 2, past, past + 86_400_000L, past + 1_800_000L));
            cooldowns.put(f.uuid, past + 1_800_000L);
            invoke(plugin, "restore", new Class<?>[]{Player.class}, f.player);
            check(!active.containsKey(f.uuid) && canCharge(plugin, f), "24-hour expiry removes effect and allows a fresh activation on rejoin");
            near(f.attribute(Attribute.MAX_HEALTH).getValue(), 23, "expired effect restores external-only health");
            check(!icons.contains(f.uuid), "expired effect removes the countdown icon");
            plugin.getLogger().info("TAROT ACTIVATION STATE SELFTEST PASS: " + checks
                + " checks; main-hand native completion, immediate 30-minute cooldown, 24-hour effect, unlocked switching, real Paper attributes, reset independence and persistence.");
        } catch (Exception ex) {
            throw new IllegalStateException("Activation state integration selftest failed", ex);
        } finally {
            if (controller != null) controller.shutdown();
            if (fixture != null) fixture.attributes.remove();
            if (active != null && savedActive != null) { active.clear(); active.putAll(savedActive); }
            if (cooldowns != null && savedCooldowns != null) { cooldowns.clear(); cooldowns.putAll(savedCooldowns); }
            if (gestures != null && savedGestures != null) { gestures.clear(); gestures.putAll(savedGestures); }
            if (icons != null && savedIcons != null) { icons.clear(); icons.addAll(savedIcons); }
            try {
                if (originalFile != null) field("stateFile").set(plugin, originalFile);
                if (settingsSaved) {
                    field("durationMillis").setLong(plugin, originalDuration);
                    field("lockMillis").setLong(plugin, originalLock);
                    field("iconEnabled").setBoolean(plugin, originalIcon);
                }
                if (temporaryFile != null) Files.deleteIfExists(temporaryFile.toPath());
            } catch (Exception ex) { throw new IllegalStateException("Activation state test cleanup failed", ex); }
        }
    }

    private static void holdAndFinish(ChargeController controller, Fixture f) {
        check(controller.request(f.player), "unlocked main-hand charge accepted");
        f.use(0); controller.tick(); f.used = 98; controller.tick(); finish(controller, f);
    }

    private static void finish(ChargeController controller, Fixture f) {
        // Paper 26.3 restarts the active item just before PlayerItemConsumeEvent: used time is zero here.
        f.used = 0;
        PlayerItemConsumeEvent event = new PlayerItemConsumeEvent(f.player, f.main.clone(), EquipmentSlot.HAND);
        controller.consume(event);
        check(event.isCancelled() && !controller.isCharging(f.player), "native completion keeps the card and terminates charging");
        f.using = false;
    }

    private static boolean canCharge(TarotPlugin plugin, Fixture f) {
        return (Boolean) invoke(plugin, "canCharge", new Class<?>[]{Player.class}, f.player);
    }
    private static void reload(TarotPlugin plugin, Map<UUID, Core.Active> active, Map<UUID, Long> cooldowns) {
        active.clear(); cooldowns.clear(); invoke(plugin, "loadState", new Class<?>[]{});
    }
    private static void reset(TarotPlugin plugin, Fixture f, String kind) {
        plugin.executeCommand(f.player, new String[]{"reset", kind, "ActivationTest"}, name -> name.equals("ActivationTest") ? f.player : null);
    }
    private static AttributeModifier modifier(String namespace, String key, double amount) {
        return new AttributeModifier(new NamespacedKey(namespace, key), amount, AttributeModifier.Operation.ADD_NUMBER);
    }
    private static Field field(String name) throws ReflectiveOperationException {
        Field f = TarotPlugin.class.getDeclaredField(name); f.setAccessible(true); return f;
    }
    private static Object invoke(TarotPlugin plugin, String name, Class<?>[] types, Object... args) {
        try {
            Method method = TarotPlugin.class.getDeclaredMethod(name, types); method.setAccessible(true); return method.invoke(plugin, args);
        } catch (ReflectiveOperationException ex) { throw new IllegalStateException("Activation test invoking " + name, ex); }
    }
    private static void near(double actual, double expected, String label) { check(Math.abs(actual - expected) < 1e-6, label + ": " + actual + " != " + expected); }
    private static void check(boolean value, String label) { checks++; if (!value) throw new IllegalStateException(label); }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, handler));
    }
    private static Object object(Object proxy, String method, Object[] args) {
        return switch (method) {
            case "equals" -> proxy == args[0]; case "hashCode" -> System.identityHashCode(proxy); case "toString" -> "ActivationTest";
            default -> throw new UnsupportedOperationException(method);
        };
    }

    private static final class Fixture {
        final UUID uuid = UUID.randomUUID();
        final World world = Bukkit.getWorlds().getFirst();
        // Bukkit creates a real CraftZombie and its attribute map, but does not spawn it or run entity ticks.
        final Zombie attributes = world.createEntity(new Location(world, 0, 80, 0), Zombie.class);
        ItemStack main, off, usingItem;
        boolean using;
        int used, removedIcons;
        PotionEffect lastIcon;
        final List<Component> actionBars = new ArrayList<>();
        final PlayerInventory inventory = proxy(PlayerInventory.class, (o, m, a) -> switch (m.getName()) {
            case "getItemInMainHand" -> main; case "getItemInOffHand" -> off; case "getSize" -> 0;
            default -> object(o, m.getName(), a);
        });
        final Player player = proxy(Player.class, (o, m, a) -> switch (m.getName()) {
            case "getUniqueId" -> uuid; case "getName" -> "ActivationTest"; case "getInventory" -> inventory;
            case "isOnline", "isInvisible", "hasPermission" -> true; case "isDead", "isSneaking" -> false;
            case "getGameMode" -> GameMode.SURVIVAL; case "getAttribute" -> attributes.getAttribute((Attribute) a[0]);
            case "getHealth" -> attributes.getHealth(); case "setHealth" -> { attributes.setHealth((Double) a[0]); yield null; }
            case "getLocation" -> attributes.getLocation(); case "getWorld" -> world;
            case "getPotionEffect" -> null;
            case "sendPotionEffectChange" -> { lastIcon = (PotionEffect) a[1]; yield null; }
            case "sendPotionEffectChangeRemove" -> { removedIcons++; lastIcon = null; yield null; }
            case "sendMessage", "playSound" -> null;
            case "sendActionBar" -> { actionBars.add((Component) a[0]); yield null; }
            case "hasActiveItem" -> using; case "getActiveItem" -> usingItem;
            case "getActiveItemHand" -> EquipmentSlot.HAND; case "getActiveItemUsedTime" -> used;
            case "clearActiveItem" -> { using = false; yield null; }
            default -> object(o, m.getName(), a);
        });
        AttributeInstance attribute(Attribute key) { return Objects.requireNonNull(attributes.getAttribute(key), key.toString()); }
        void use(int ticks) { usingItem = main.clone(); using = true; used = ticks; }
    }
}

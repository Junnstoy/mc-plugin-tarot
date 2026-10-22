package cn.tarot;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import net.kyori.adventure.text.Component;
import org.bukkit.*;
import org.bukkit.attribute.*;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.*;
import org.bukkit.persistence.PersistentDataType;

/** Real Paper items and item serialization, with synthetic disconnected players. */
final class AlbumServiceTest {
    private static int checks;
    static void run(TarotPlugin plugin) {
        check(Bukkit.isPrimaryThread(), "server thread required");
        checks = 0;
        Path root = null;
        try {
            root = Files.createTempDirectory(plugin.getDataFolder().toPath(), "tarot-album-selftest-").toAbsolutePath().normalize();
            persistence(plugin, root.resolve("persistence"));
            transactions(plugin, root.resolve("transactions"));
            effects(plugin, root.resolve("effects"));
            restartPersistence(plugin);
            plugin.getLogger().info("TAROT ALBUM SERVICE SELFTEST PASS: " + checks
                + " assertions; 110 metadata-preserving save/load pairs; full-inventory withdrawal;"
                + " duplicate rejection; deposit/withdraw save-failure rollback; carry-gated bonuses;"
                + " withdrawal downgrade; health preserved while changing bonuses; malformed-data rejection.");
        } catch (IOException ex) {
            throw new IllegalStateException("Album service selftest I/O failure", ex);
        } finally {
            if (root != null) {
                // This path was created above solely for this test and contains no player data.
                Path expectedParent = plugin.getDataFolder().toPath().toAbsolutePath().normalize();
                if (!root.getParent().equals(expectedParent) || !root.getFileName().toString().startsWith("tarot-album-selftest-"))
                    throw new IllegalStateException("Unexpected test cleanup path: " + root);
                try (var paths = Files.walk(root)) {
                    for (Path path : paths.sorted(Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
                } catch (IOException ex) { plugin.getLogger().warning("Could not clean album test directory: " + ex.getMessage()); }
            }
        }
    }
    private static void restartPersistence(TarotPlugin plugin) throws IOException {
        String configured=System.getProperty("tarot.validation.album-dir");
        if(configured==null)return;
        Path directory=Path.of(configured);AlbumStore store=new AlbumStore(directory);
        UUID id=UUID.fromString("00000000-0000-0000-0000-000000000031");
        if(!Files.exists(directory.resolve(id+".yml"))){
            AlbumStore.Data data=new AlbumStore.Data();data.particles=false;
            for(int star=1;star<=5;star++)for(int card=1;card<=22;card++)data.cards[AlbumRules.index(card,star)]=plugin.makeCard(card,star);
            store.save(id,data);plugin.getLogger().info("TAROT ALBUM RESTART SEEDED: 110 cards and particles=false.");
        }else{
            AlbumStore.Data data=store.load(id);check(!data.particles,"particle toggle survives process restart");
            for(int star=1;star<=5;star++)for(int card=1;card<=22;card++)
                check(plugin.makeCard(card,star).equals(data.cards[AlbumRules.index(card,star)]),"card survives process restart");
            plugin.getLogger().info("TAROT ALBUM RESTART SELFTEST PASS: 110 cards and particle setting restored across JVM restart.");
        }
    }
    private static void persistence(TarotPlugin plugin, Path directory) throws IOException {
        AlbumStore store = new AlbumStore(directory);
        UUID uuid = UUID.randomUUID();
        AlbumStore.Data missing = store.load(uuid);
        check(Arrays.stream(missing.cards).allMatch(Objects::isNull), "new player starts empty");
        AlbumStore.Data saved = new AlbumStore.Data();
        saved.particles = false;
        for (int star = 1; star <= 5; star++) for (int card = 1; card <= 22; card++) {
            ItemStack item = plugin.makeCard(card, star);
            var meta = item.getItemMeta();
            meta.displayName(Component.text("自定义名字 " + card + "/" + star));
            meta.getPersistentDataContainer().set(new NamespacedKey("album_test", "marker"),
                PersistentDataType.STRING, "preserve:" + card + ":" + star);
            item.setItemMeta(meta);
            saved.cards[AlbumRules.index(card, star)] = item;
        }
        store.save(uuid, saved);
        AlbumStore.Data loaded = new AlbumStore(directory).load(uuid);
        check(!loaded.particles, "particle setting persists");
        for (int index = 0; index < 110; index++) {
            check(saved.cards[index].equals(loaded.cards[index]), "all item metadata retained at " + index);
            check(saved.cards[index] != loaded.cards[index], "deserialized independent item " + index);
        }
        loaded.cards[0] = null;
        loaded.particles = true;
        store.save(uuid, loaded);
        AlbumStore.Data updated = store.load(uuid);
        check(updated.cards[0] == null && updated.cards[1] != null && updated.particles, "overwrite removes absent slot only");
        check(!Files.exists(directory.resolve(uuid + ".yml.tmp")), "atomic save leaves no temp file");
        for (String malformed : List.of(
                "format: 2\n",
                "format: 1\ncards: broken\n",
                "format: 1\ncards:\n  '110': anything\n",
                "format: 1\ncards:\n  '0': invalid-base64!!\n")) {
            UUID bad = UUID.randomUUID();
            Files.writeString(directory.resolve(bad + ".yml"), malformed);
            try { store.load(bad); throw new AssertionError("Malformed album must fail closed: " + malformed); }
            catch (IOException expected) { checks++; }
        }
    }
    private static void transactions(TarotPlugin plugin, Path directory) throws IOException {
        AlbumStore store = new AlbumStore(directory);
        AlbumService service = new AlbumService(plugin, store);
        Fixture f = new Fixture();
        var logger = plugin.getLogger();
        var previousFilter = logger.getFilter();
        String expectedFailure = "Cannot save album " + f.uuid + ":";
        logger.setFilter(record -> !record.getMessage().startsWith(expectedFailure)
            && (previousFilter == null || previousFilter.isLoggable(record)));
        try { transactions(plugin, directory, store, service, f); }
        finally { logger.setFilter(previousFilter); }
    }
    private static void transactions(TarotPlugin plugin, Path directory, AlbumStore store,
                                     AlbumService service, Fixture f) throws IOException {
        f.storage.setItem(35, service.makeAlbum());
        ItemStack special = plugin.makeCard(1, 1);
        var meta = special.getItemMeta();
        meta.displayName(Component.text("保留的自定义名称"));
        special.setItemMeta(meta);
        f.storage.setItem(0, special.clone());
        check(service.depositSlot(f.player, 0), "deposit succeeds");
        check(empty(f.storage.getItem(0)), "deposit consumes exactly original slot");
        check(service.progress(f.player).cardinality() == 1, "deposit registers exact collection");
        check(special.equals(service.snapshot(f.player)[0]), "deposit preserves metadata");
        ItemStack[] detached = service.snapshot(f.player);
        detached[0].setAmount(2);
        check(service.snapshot(f.player)[0].getAmount() == 1, "snapshot cannot mutate stored item");
        byte[] savedFile = Files.readAllBytes(directory.resolve(f.uuid + ".yml"));
        f.storage.setItem(0, special.clone());
        ItemStack[] before = copy(f.storage.getContents());
        check(!service.depositSlot(f.player, 0), "duplicate card/star rejected");
        check(Arrays.equals(before, f.storage.getContents()), "duplicate inventory untouched");
        check(Arrays.equals(savedFile, Files.readAllBytes(directory.resolve(f.uuid + ".yml"))), "duplicate file untouched");

        f.fill(); f.storage.setItem(35, service.makeAlbum());
        before = copy(f.storage.getContents());
        check(!service.withdraw(f.player, 0), "full inventory withdrawal rejected");
        check(Arrays.equals(before, f.storage.getContents()), "full withdrawal inventory unchanged");
        check(special.equals(service.snapshot(f.player)[0]), "full withdrawal retains exact saved card");
        check(Arrays.equals(savedFile, Files.readAllBytes(directory.resolve(f.uuid + ".yml"))), "full withdrawal file unchanged");
        f.storage.setItem(0, null);
        check(service.withdraw(f.player, 0), "one empty slot permits withdrawal");
        check(special.equals(f.storage.getItem(0)), "withdraw returns identical card metadata");
        check(service.snapshot(f.player)[0] == null && store.load(f.uuid).cards[0] == null, "withdraw removes stored copy durably");

        Path failingTemp = directory.resolve(f.uuid + ".yml.tmp");
        Files.createDirectory(failingTemp);
        before = copy(f.storage.getContents());
        check(!service.depositSlot(f.player, 0), "save failure rejects deposit");
        check(Arrays.equals(before, f.storage.getContents()), "failed deposit restores source card");
        check(service.snapshot(f.player)[0] == null && store.load(f.uuid).cards[0] == null, "failed deposit leaves no stored duplicate");
        Files.delete(failingTemp);
        check(service.depositSlot(f.player, 0), "deposit succeeds after transient I/O failure");
        Files.createDirectory(failingTemp);
        before = copy(f.storage.getContents());
        check(!service.withdraw(f.player, 0), "save failure rejects withdrawal");
        check(Arrays.equals(before, f.storage.getContents()), "failed withdrawal rolls back delivery");
        check(special.equals(service.snapshot(f.player)[0]) && special.equals(store.load(f.uuid).cards[0]), "failed withdrawal preserves stored card");
        Files.delete(failingTemp);

        f.cursor = plugin.makeCard(2, 2);
        check(!service.depositCursor(f.player, AlbumRules.index(2, 1)), "wrong star cursor rejected");
        check(f.cursor != null && f.cursor.getAmount() == 1, "wrong cursor retained");
        check(service.depositCursor(f.player, AlbumRules.index(2, 2)), "correct cursor accepted");
        check(empty(f.cursor), "cursor consumed once");
        f.cursor = plugin.makeCard(3, 2);
        ItemStack oldCursor = f.cursor.clone();
        Files.createDirectory(failingTemp);
        check(!service.depositCursor(f.player, AlbumRules.index(3, 2)), "cursor save failure rejected");
        check(oldCursor.equals(f.cursor) && !service.progress(f.player).get(AlbumRules.index(3, 2)), "cursor rollback restores original only");
        Files.delete(failingTemp);

        f.mode = GameMode.CREATIVE;
        f.storage.setItem(1, plugin.makeCard(4, 1));
        before = copy(f.storage.getContents());
        check(!service.depositSlot(f.player, 1) && !service.withdraw(f.player, 0), "creative storage edits denied");
        check(Arrays.equals(before, f.storage.getContents()), "creative rejection preserves inventory");
        f.mode = GameMode.SURVIVAL;
        f.storage.setItem(35, null);
        check(!service.depositSlot(f.player, 1) && !service.withdraw(f.player, 0), "album required for edits");
    }
    private static void effects(TarotPlugin plugin, Path directory) throws IOException {
        AlbumStore store = new AlbumStore(directory);
        AlbumService service = new AlbumService(plugin, store);
        Fixture f = new Fixture();
        AlbumStore.Data data = new AlbumStore.Data();
        for (int card = 1; card <= 11; card++) data.cards[AlbumRules.index(card, 1)] = plugin.makeCard(card, 1);
        store.save(f.uuid, data);
        service.refresh(f.player);
        close(f.attributes.get(Attribute.MAX_HEALTH).value(), 20, "not carrying grants no bonus");
        f.storage.setItem(35, service.makeAlbum());
        service.refresh(f.player);
        close(f.attributes.get(Attribute.MAX_HEALTH).value(), 21.2, "carrying applies milestones and bonds");
        f.storage.setItem(34, service.makeAlbum());
        service.refresh(f.player);
        close(f.attributes.get(Attribute.MAX_HEALTH).value(), 21.2, "multiple albums cannot stack bonuses");
        f.health = 21.2;
        f.storage.setItem(0, plugin.makeCard(12, 1));
        check(service.depositSlot(f.player, 0), "new bond card accepted");
        close(f.health, 21.2, "refresh never clamps health to temporary base while swapping modifiers");
        close(f.attributes.get(Attribute.ATTACK_DAMAGE).value(), 1.1, "new bond applies damage bonus");
        check(service.withdraw(f.player, AlbumRules.index(4, 1)), "withdraw bond card succeeds");
        close(f.attributes.get(Attribute.MAX_HEALTH).value(), 21, "withdraw removes broken health bond immediately");
        close(f.health, 21, "health clamps to final reduced maximum");
        // Another plugin's modifier must survive every album update and removal.
        AttributeModifier external = new AttributeModifier(new NamespacedKey("other_plugin", "health"), 2,
            AttributeModifier.Operation.ADD_NUMBER);
        f.attributes.get(Attribute.MAX_HEALTH).modifiers.add(external);
        f.storage.setItem(34, null); f.storage.setItem(35, null);
        service.refresh(f.player);
        close(f.attributes.get(Attribute.MAX_HEALTH).value(), 22, "putting album away removes only album modifiers");
        check(f.attributes.get(Attribute.MAX_HEALTH).modifiers.contains(external), "unrelated modifiers retained");
        check(service.progress(f.player).cardinality() == 11, "putting album away does not erase contents");
        f.storage.setItem(35, service.makeAlbum()); service.refresh(f.player);
        close(f.attributes.get(Attribute.MAX_HEALTH).value(), 23, "carrying again reactivates current bonuses");
        check(service.particlesEnabled(f.player), "particles enabled by default");
        check(service.toggleParticles(f.player), "particle setting toggles");
        check(!store.load(f.uuid).particles, "particle preference saved");
        close(f.attributes.get(Attribute.MAX_HEALTH).value(), 23, "particle preference does not remove bonuses");
    }
    private static final class AttributeState {
        final Attribute attribute;
        final double base;
        final List<AttributeModifier> modifiers = new ArrayList<>();
        final AttributeInstance instance;
        AttributeState(Attribute attribute, double base) {
            this.attribute = attribute; this.base = base;
            instance = proxy(AttributeInstance.class, (object, method, args) -> switch(method.getName()) {
                case "getAttribute" -> attribute;
                case "getBaseValue", "getDefaultValue" -> base;
                case "getValue" -> value();
                case "getModifiers" -> List.copyOf(modifiers);
                case "addTransientModifier", "addModifier" -> { modifiers.add((AttributeModifier)args[0]); yield null; }
                case "removeModifier" -> { modifiers.remove(args[0]); yield null; }
                default -> objectMethod(object, method.getName(), args);
            });
        }
        double value() {
            double addition = 0, scalar = 0, product = 1;
            for (AttributeModifier modifier : modifiers) switch(modifier.getOperation()) {
                case ADD_NUMBER -> addition += modifier.getAmount();
                case ADD_SCALAR -> scalar += modifier.getAmount();
                case MULTIPLY_SCALAR_1 -> product *= 1 + modifier.getAmount();
            }
            return (base + addition) * (1 + scalar) * product;
        }
    }
    private static final class Fixture {
        final Inventory storage = Bukkit.createInventory(null, 36);
        final UUID uuid = UUID.randomUUID();
        final List<String> messages = new ArrayList<>();
        final Map<Attribute, AttributeState> attributes = new HashMap<>();
        final PlayerInventory inventory;
        final Player player;
        final InventoryView view;
        ItemStack cursor;
        GameMode mode = GameMode.SURVIVAL;
        double health = 20;
        Fixture() {
            for (Attribute attribute : List.of(Attribute.MAX_HEALTH, Attribute.ATTACK_DAMAGE, Attribute.ARMOR,
                    Attribute.MOVEMENT_SPEED, Attribute.LUCK))
                attributes.put(attribute, new AttributeState(attribute, attribute == Attribute.MAX_HEALTH ? 20
                    : attribute == Attribute.ATTACK_DAMAGE ? 1 : attribute == Attribute.MOVEMENT_SPEED ? .1 : 0));
            inventory = proxy(PlayerInventory.class, (object, method, args) -> switch(method.getName()) {
                case "getItem" -> storage.getItem((Integer)args[0]);
                case "setItem" -> { storage.setItem((Integer)args[0], (ItemStack)args[1]); yield null; }
                case "getContents", "getStorageContents" -> storage.getContents();
                case "setContents", "setStorageContents" -> { storage.setContents((ItemStack[])args[0]); yield null; }
                case "getItemInMainHand" -> storage.getItem(0);
                case "getItemInOffHand" -> null;
                case "getType" -> InventoryType.PLAYER;
                case "getSize" -> 36;
                default -> objectMethod(object, method.getName(), args);
            });
            player = proxy(Player.class, (object, method, args) -> switch(method.getName()) {
                case "getInventory" -> inventory;
                case "getOpenInventory" -> getView();
                case "getUniqueId" -> uuid;
                case "getName" -> "TarotOfflineAlbumTest";
                case "getGameMode" -> mode;
                case "isDead", "isOnline" -> false;
                case "getAttribute" -> attributes.get((Attribute)args[0]).instance;
                case "getHealth" -> health;
                case "setHealth" -> { health = (Double)args[0]; yield null; }
                case "getLocation" -> new Location(Bukkit.getWorlds().getFirst(), 0, 80, 0);
                case "playSound" -> null;
                case "sendMessage" -> { messages.add(Arrays.toString(args)); yield null; }
                default -> objectMethod(object, method.getName(), args);
            });
            view = proxy(InventoryView.class, (object, method, args) -> switch(method.getName()) {
                case "getCursor" -> cursor;
                case "setCursor" -> { cursor = (ItemStack)args[0]; yield null; }
                case "getPlayer" -> player;
                default -> objectMethod(object, method.getName(), args);
            });
        }
        InventoryView getView() { return view; }
        void fill() { for(int slot = 0; slot < 36; slot++) storage.setItem(slot, new ItemStack(Material.STONE, 64)); }
    }
    private static ItemStack[] copy(ItemStack[] items) { return Arrays.stream(items).map(s -> s == null ? null : s.clone()).toArray(ItemStack[]::new); }
    private static boolean empty(ItemStack item) { return item == null || item.isEmpty(); }
    private static void check(boolean passed, String message) { checks++; if(!passed) throw new IllegalStateException("Album service selftest: " + message); }
    private static void close(double actual, double expected, String message) { check(Math.abs(actual - expected) < 1e-10, message + ": " + actual + " != " + expected); }
    private static Object objectMethod(Object object, String method, Object[] args) {
        return switch(method) {
            case "equals" -> object == args[0];
            case "hashCode" -> System.identityHashCode(object);
            case "toString" -> "TarotOfflineAlbumTestProxy";
            default -> throw new UnsupportedOperationException("Unexpected album test method: " + method);
        };
    }
    private static <T> T proxy(Class<T> type, InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(AlbumServiceTest.class.getClassLoader(), new Class<?>[]{type}, handler));
    }
}

package cn.tarot;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.block.Action;
import org.bukkit.event.Event;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

/** Exercises the native-use gates in an isolated player fixture on a running Paper server. */
final class ChargeControllerTest {
    static void run(TarotPlugin plugin) {
        check(Bukkit.isPrimaryThread(), "server thread");
        Fixture f = new Fixture();
        ItemStack card = plugin.makeCard(1, 1);
        ChargeController.configureCard(card);
        check(ChargeController.isConfigured(card), "canonical native component");
        check(card.getData(DataComponentTypes.CONSUMABLE).consumeSeconds() == 5f, "five seconds");
        check(!ChargeController.isConfigured(new ItemStack(Material.PAPER)), "ordinary paper unchanged");
        check(plugin.cardId(card) == 1 && plugin.cardLevel(card) == 1, "identity survives component");
        AtomicBoolean allowed = new AtomicBoolean(true);
        AtomicInteger activations = new AtomicInteger();
        List<Double> rendered = new ArrayList<>();
        ChargeController controller = new ChargeController(plugin, item -> plugin.cardId(item) > 0,
            player -> allowed.get(), (player, fraction) -> rendered.add(fraction), player -> activations.incrementAndGet());
        try {
            var controllerField=TarotPlugin.class.getDeclaredField("charging");controllerField.setAccessible(true);
            Object liveController=controllerField.get(plugin);
            try{
                controllerField.set(plugin,controller);f.reset(card);
                PlayerInteractEvent gesture=new PlayerInteractEvent(f.player,Action.RIGHT_CLICK_AIR,card,null,null,EquipmentSlot.HAND);
                plugin.interact(gesture);
                check(controller.isCharging(f.player)&&gesture.useItemInHand()==Event.Result.ALLOW,
                    "main-hand interaction enters real native hold without requiring sneak");
                controller.cancel(f.player);
                PlayerInteractEvent denied=new PlayerInteractEvent(f.player,Action.RIGHT_CLICK_AIR,card,null,null,EquipmentSlot.HAND);
                denied.setUseItemInHand(Event.Result.DENY);plugin.interact(denied);
                check(!controller.isCharging(f.player),"interaction respects item-use denial");
            }finally{controllerField.set(plugin,liveController);}
            f.reset(card);
            check(controller.request(f.player), "request accepted");
            for (int i = 0; i < 4; i++) controller.tick();
            check(!controller.isCharging(f.player) && activations.get() == 0 && rendered.isEmpty(),
                "click without native hold cannot charge");

            f.reset(card);
            check(controller.request(f.player), "hold accepted");
            f.use(0); controller.tick();
            f.used = 50; controller.tick();
            check(rendered.contains(0d) && rendered.contains(.5d), "progress uses native elapsed ticks");
            controller.stop(new PlayerStopUsingItemEvent(f.player, card, 50));
            f.used = 100;
            PlayerItemConsumeEvent released = f.consume(card, EquipmentSlot.HAND);
            controller.consume(released);
            check(released.isCancelled() && activations.get() == 0, "release prevents later completion");

            f.reset(card); controller.request(f.player); f.use(0); controller.tick();
            f.used = 50; controller.tick();
            int before = rendered.size();
            check(controller.request(f.player), "repeat request accepted without reset");
            controller.tick();
            check(rendered.size() == before, "repeat request preserves progress and render throttle");
            f.used = 52; controller.tick();
            check(rendered.getLast() == .52d, "repeat request continues existing hold");
            f.used = 100; controller.tick();
            check(activations.get() == 0, "scheduler alone cannot activate");
            f.used = 0; // Paper 26.3 restarts use inside completeUsingItem before firing the event.
            ItemStack beforeCard = f.main.clone();
            PlayerItemConsumeEvent complete = f.consume(card, EquipmentSlot.HAND);
            controller.consume(complete);
            check(complete.isCancelled() && activations.get() == 1 && beforeCard.equals(f.main),
                "native completion with reset elapsed time activates once and preserves exact card");
            controller.consume(f.consume(card, EquipmentSlot.HAND));
            check(activations.get() == 1, "duplicate completion cannot activate twice");

            f.reset(card); controller.request(f.player); f.use(98); controller.tick(); f.used = 0;
            PlayerItemConsumeEvent cancelled = f.consume(card, EquipmentSlot.HAND);
            cancelled.setCancelled(true); controller.consume(cancelled);
            check(activations.get() == 1 && cancelled.isCancelled(), "respects prior cancellation");

            f.reset(card); controller.request(f.player); f.use(0);
            PlayerItemConsumeEvent unobservedUse = f.consume(card, EquipmentSlot.HAND);
            controller.consume(unobservedUse);
            check(unobservedUse.isCancelled() && activations.get() == 1, "unobserved completion cannot activate");

            f.reset(card); controller.request(f.player); f.use(98); controller.tick(); f.used = 0;
            PlayerItemConsumeEvent wrongHand = f.consume(card, EquipmentSlot.OFF_HAND);
            controller.consume(wrongHand);
            check(wrongHand.isCancelled() && activations.get() == 1, "offhand never activates or consumes");
            f.reset(card); controller.request(f.player); f.use(50); f.hand = EquipmentSlot.OFF_HAND;
            for (int i = 0; i < 4; i++) controller.tick();
            check(!controller.isCharging(f.player), "actual offhand use rejected");

            f.reset(card); controller.request(f.player); f.use(50); controller.tick();
            var meta = f.main.getItemMeta(); meta.displayName(Component.text("Changed during hold")); f.main.setItemMeta(meta);
            controller.tick();
            check(!controller.isCharging(f.player), "metadata change cancels charge");
            f.reset(card); controller.request(f.player); f.use(50); controller.tick();
            f.main = plugin.makeCard(2, 1); ChargeController.configureCard(f.main); controller.tick();
            check(!controller.isCharging(f.player), "different card cancels charge");

            f.reset(card); controller.request(f.player); f.use(50); controller.tick();
            f.used = 0; controller.tick();
            check(!controller.isCharging(f.player) && !f.active, "native use restarting cancels original charge");

            f.reset(card); controller.request(f.player); f.use(98); controller.tick(); f.used = 0; allowed.set(false);
            controller.consume(f.consume(card, EquipmentSlot.HAND));
            check(activations.get() == 1, "cooldown rechecked at completion");
            check(!controller.request(f.player), "cooldown rejects new charge"); allowed.set(true);

            f.reset(card); f.mode = GameMode.CREATIVE;
            check(controller.request(f.player), "creative retains previous activation support");
            f.use(98); controller.tick(); f.used = 0; controller.consume(f.consume(card, EquipmentSlot.HAND));
            check(activations.get() == 2, "creative native completion activates");
            f.reset(card); f.mode = GameMode.SPECTATOR;
            check(!controller.request(f.player), "spectator cannot charge");
            f.reset(card); f.online = false;
            check(!controller.request(f.player), "offline cannot charge");
            f.reset(card); f.dead = true;
            check(!controller.request(f.player), "dead cannot charge");

            f.reset(card); controller.request(f.player); f.use(20);
            controller.swap(new PlayerSwapHandItemsEvent(f.player, card, new ItemStack(Material.AIR)));
            check(!controller.isCharging(f.player) && !f.active, "swapping hands aborts native use");
            f.reset(card); controller.request(f.player); f.use(20);
            controller.held(new PlayerItemHeldEvent(f.player, 0, 1));
            check(!controller.isCharging(f.player) && !f.active, "hotbar switch aborts native use");
            f.reset(card); controller.request(f.player); f.use(20); controller.shutdown();
            check(!controller.isCharging(f.player) && !f.active, "shutdown clears native hold");
            if (System.getProperty("tarot.validation.album-dir") != null) nativeCompletion(plugin, card);
            plugin.getLogger().info("TAROT CHARGE SELFTEST PASS: main hand, native hold, early release, reset elapsed time, cancellation, identity, repeat requests, modes and item preservation");
        } catch(ReflectiveOperationException ex){throw new IllegalStateException("Charge integration setup",ex);}
        finally { controller.shutdown(); }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Charge selftest: " + label);
    }

    /** Executes Paper's real start/update/complete methods without connecting or adding a player to the world.
     * Only online eligibility is adapted; item state, elapsed time and the consume event all come from NMS. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void nativeCompletion(TarotPlugin plugin, ItemStack card) throws ReflectiveOperationException {
        Class<?> serverType = Class.forName("net.minecraft.server.MinecraftServer");
        Class<?> levelType = Class.forName("net.minecraft.server.level.ServerLevel");
        Class<?> playerType = Class.forName("net.minecraft.server.level.ServerPlayer");
        Class<?> livingType = Class.forName("net.minecraft.world.entity.LivingEntity");
        Class<?> profileType = Class.forName("com.mojang.authlib.GameProfile");
        Class<?> infoType = Class.forName("net.minecraft.server.level.ClientInformation");
        Class<?> flowType = Class.forName("net.minecraft.network.protocol.PacketFlow");
        Class<?> connectionType = Class.forName("net.minecraft.network.Connection");
        Class<?> cookieType = Class.forName("net.minecraft.server.network.CommonListenerCookie");
        Class<?> listenerType = Class.forName("net.minecraft.server.network.ServerGamePacketListenerImpl");
        Class<?> handType = Class.forName("net.minecraft.world.InteractionHand");
        Class<?> itemType = Class.forName("net.minecraft.world.item.ItemStack");
        Object server = Bukkit.getServer().getClass().getMethod("getServer").invoke(Bukkit.getServer());
        var world = Bukkit.getWorlds().getFirst();
        Object level = world.getClass().getMethod("getHandle").invoke(world);
        Object profile = profileType.getConstructor(UUID.class, String.class).newInstance(UUID.randomUUID(), "TarotNativeTest");
        Object info = infoType.getMethod("createDefault").invoke(null);
        Object handle = playerType.getConstructor(serverType, levelType, profileType, infoType)
            .newInstance(server, level, profile, info);
        Object connection = connectionType.getConstructor(flowType)
            .newInstance(Enum.valueOf((Class) flowType, "SERVERBOUND"));
        Object cookie = cookieType.getMethod("createInitial", profileType, boolean.class).invoke(null, profile, false);
        Object listener = listenerType.getConstructor(serverType, connectionType, playerType, cookieType)
            .newInstance(server, connection, handle, cookie);
        playerType.getField("connection").set(handle, listener);
        Player actual = (Player) playerType.getMethod("getBukkitEntity").invoke(handle);
        Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> {
                if (method.getName().equals("isOnline")) return true;
                try { return method.invoke(actual, args); }
                catch (java.lang.reflect.InvocationTargetException ex) { throw ex.getCause(); }
            });
        AtomicInteger activations = new AtomicInteger();
        AtomicInteger elapsedAtCompletion = new AtomicInteger(-1);
        ChargeController controller = new ChargeController(plugin, item -> plugin.cardId(item) > 0,
            ignored -> true, (ignored, fraction) -> {}, ignored -> activations.incrementAndGet());
        org.bukkit.event.Listener capture = new org.bukkit.event.Listener() {};
        Bukkit.getPluginManager().registerEvent(PlayerItemConsumeEvent.class, capture, org.bukkit.event.EventPriority.LOWEST,
            (ignored, event) -> {
                var consume = (PlayerItemConsumeEvent) event;
                if (!consume.getPlayer().getUniqueId().equals(actual.getUniqueId())) return;
                elapsedAtCompletion.set(actual.getActiveItemUsedTime());
                check(actual.hasActiveItem() && actual.getActiveItemHand() == EquipmentSlot.HAND,
                    "Paper native completion retains the active main hand");
                PlayerItemConsumeEvent forwarded = new PlayerItemConsumeEvent(player, consume.getItem(), consume.getHand());
                forwarded.setCancelled(consume.isCancelled());
                controller.consume(forwarded);
                consume.setCancelled(forwarded.isCancelled());
            }, plugin, false);
        var start = livingType.getMethod("startUsingItem", handType);
        var update = livingType.getDeclaredMethod("updateUsingItem", itemType);
        update.setAccessible(true);
        var getUseItem = livingType.getMethod("getUseItem");
        Object mainHand = Enum.valueOf((Class) handType, "MAIN_HAND");
        try {
            actual.getInventory().setItemInMainHand(card.clone());
            check(controller.request(player), "real Paper main-hand request");
            start.invoke(handle, mainHand);
            for (int tick = 0; tick < ChargeController.CHARGE_TICKS - 1; tick++) {
                controller.tick(); update.invoke(handle, getUseItem.invoke(handle));
            }
            check(actual.getActiveItemUsedTime() == 99 && activations.get() == 0
                && elapsedAtCompletion.get() == -1, "real Paper cannot finish at 99 native ticks");
            controller.tick(); update.invoke(handle, getUseItem.invoke(handle));
            check(elapsedAtCompletion.get() == 0, "Paper resets elapsed time before its consume event");
            check(activations.get() == 1 && !actual.hasActiveItem()
                && card.equals(actual.getInventory().getItemInMainHand()),
                "real Paper completion activates once, clears use and preserves the card");

            // A real release dispatches PlayerStopUsingItemEvent before clearing the native item.
            org.bukkit.event.Listener stopCapture = new org.bukkit.event.Listener() {};
            Bukkit.getPluginManager().registerEvent(PlayerStopUsingItemEvent.class, stopCapture,
                org.bukkit.event.EventPriority.LOWEST, (ignored, event) -> {
                    var stop = (PlayerStopUsingItemEvent) event;
                    if (stop.getPlayer().getUniqueId().equals(actual.getUniqueId()))
                        controller.stop(new PlayerStopUsingItemEvent(player, stop.getItem(), stop.getTicksHeldFor()));
                }, plugin, false);
            try {
                check(controller.request(player), "real Paper second request");
                start.invoke(handle, mainHand);
                for (int tick = 0; tick < 50; tick++) {
                    controller.tick(); update.invoke(handle, getUseItem.invoke(handle));
                }
                livingType.getMethod("releaseUsingItem").invoke(handle);
                check(!controller.isCharging(player) && !actual.hasActiveItem() && activations.get() == 1,
                    "real Paper early release cancels without another activation");
            } finally { org.bukkit.event.HandlerList.unregisterAll(stopCapture); }
            plugin.getLogger().info("TAROT NATIVE CHARGE SELFTEST PASS: actual Paper use/consume/release, 99 ticks pending, 100 ticks completed, event elapsed=0, exact card retained");
        } finally {
            org.bukkit.event.HandlerList.unregisterAll(capture);
            controller.shutdown();
            actual.clearActiveItem();
        }
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        ItemStack main;
        ItemStack use;
        boolean active;
        boolean online = true;
        boolean dead;
        GameMode mode = GameMode.SURVIVAL;
        EquipmentSlot hand = EquipmentSlot.HAND;
        int used;
        final PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),
            new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getItemInMainHand" -> main;
                case "getItemInOffHand" -> new ItemStack(Material.AIR);
                case "getSize" -> 0;
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "ChargeTestInventory";
                default -> throw new UnsupportedOperationException("Inventory." + method.getName());
            });
        final Player player = (Player) Proxy.newProxyInstance(Player.class.getClassLoader(), new Class<?>[]{Player.class},
            (proxy, method, args) -> switch (method.getName()) {
                case "getUniqueId" -> id;
                case "getInventory" -> inventory;
                case "isOnline" -> online;
                case "isDead" -> dead;
                case "getGameMode" -> mode;
                case "hasActiveItem" -> active;
                case "getActiveItem" -> use;
                case "getActiveItemHand" -> hand;
                case "getActiveItemUsedTime" -> used;
                case "clearActiveItem" -> { active = false; yield null; }
                case "getName" -> "TarotChargeTest";
                case "equals" -> proxy == args[0];
                case "hashCode" -> System.identityHashCode(proxy);
                case "toString" -> "ChargeTestPlayer";
                default -> throw new UnsupportedOperationException("Player." + method.getName());
            });
        void reset(ItemStack card) {
            main = card.clone(); use = null; active = false; used = 0; online = true; dead = false;
            mode = GameMode.SURVIVAL; hand = EquipmentSlot.HAND;
        }
        void use(int ticks) { use = main.clone(); active = true; used = ticks; }
        PlayerItemConsumeEvent consume(ItemStack card, EquipmentSlot hand) {
            return new PlayerItemConsumeEvent(player, card.clone(), hand);
        }
    }
}

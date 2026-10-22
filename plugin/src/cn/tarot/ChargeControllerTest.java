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
                PlayerInteractEvent gesture=new PlayerInteractEvent(f.player,Action.RIGHT_CLICK_AIR,card,null,null,EquipmentSlot.OFF_HAND);
                plugin.interact(gesture);
                check(controller.isCharging(f.player)&&gesture.useItemInHand()==Event.Result.ALLOW,
                    "offhand interaction enters real native hold without requiring sneak");
                controller.cancel(f.player);
                PlayerInteractEvent denied=new PlayerInteractEvent(f.player,Action.RIGHT_CLICK_AIR,card,null,null,EquipmentSlot.OFF_HAND);
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
            PlayerItemConsumeEvent released = f.consume(card, EquipmentSlot.OFF_HAND);
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
            ItemStack beforeCard = f.off.clone();
            PlayerItemConsumeEvent complete = f.consume(card, EquipmentSlot.OFF_HAND);
            controller.consume(complete);
            check(complete.isCancelled() && activations.get() == 1 && beforeCard.equals(f.off),
                "native completion activates once and preserves exact card");
            controller.consume(f.consume(card, EquipmentSlot.OFF_HAND));
            check(activations.get() == 1, "duplicate completion cannot activate twice");

            f.reset(card); controller.request(f.player); f.use(100);
            PlayerItemConsumeEvent cancelled = f.consume(card, EquipmentSlot.OFF_HAND);
            cancelled.setCancelled(true); controller.consume(cancelled);
            check(activations.get() == 1 && cancelled.isCancelled(), "respects prior cancellation");

            f.reset(card); controller.request(f.player); f.use(99);
            PlayerItemConsumeEvent shortUse = f.consume(card, EquipmentSlot.OFF_HAND);
            controller.consume(shortUse);
            check(shortUse.isCancelled() && activations.get() == 1, "99 ticks insufficient");

            f.reset(card); controller.request(f.player); f.use(100);
            PlayerItemConsumeEvent wrongHand = f.consume(card, EquipmentSlot.HAND);
            controller.consume(wrongHand);
            check(wrongHand.isCancelled() && activations.get() == 1, "main hand never activates or consumes");
            f.reset(card); controller.request(f.player); f.use(50); f.hand = EquipmentSlot.HAND;
            for (int i = 0; i < 4; i++) controller.tick();
            check(!controller.isCharging(f.player), "actual main-hand use rejected");

            f.reset(card); controller.request(f.player); f.use(50); controller.tick();
            var meta = f.off.getItemMeta(); meta.displayName(Component.text("Changed during hold")); f.off.setItemMeta(meta);
            controller.tick();
            check(!controller.isCharging(f.player), "metadata change cancels charge");
            f.reset(card); controller.request(f.player); f.use(50); controller.tick();
            f.off = plugin.makeCard(2, 1); ChargeController.configureCard(f.off); controller.tick();
            check(!controller.isCharging(f.player), "different card cancels charge");

            f.reset(card); controller.request(f.player); f.use(50); controller.tick();
            f.used = 0; controller.tick();
            check(!controller.isCharging(f.player) && !f.active, "native use restarting cancels original charge");

            f.reset(card); controller.request(f.player); f.use(100); allowed.set(false);
            controller.consume(f.consume(card, EquipmentSlot.OFF_HAND));
            check(activations.get() == 1, "cooldown rechecked at completion");
            check(!controller.request(f.player), "cooldown rejects new charge"); allowed.set(true);

            f.reset(card); f.mode = GameMode.CREATIVE;
            check(controller.request(f.player), "creative retains previous activation support");
            f.use(100); controller.consume(f.consume(card, EquipmentSlot.OFF_HAND));
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
            plugin.getLogger().info("TAROT CHARGE SELFTEST PASS: native hold, early release, 100 ticks, cancellation, identity, repeat requests, modes and item preservation");
        } catch(ReflectiveOperationException ex){throw new IllegalStateException("Charge integration setup",ex);}
        finally { controller.shutdown(); }
    }

    private static void check(boolean condition, String label) {
        if (!condition) throw new IllegalStateException("Charge selftest: " + label);
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        ItemStack off;
        ItemStack use;
        boolean active;
        boolean online = true;
        boolean dead;
        GameMode mode = GameMode.SURVIVAL;
        EquipmentSlot hand = EquipmentSlot.OFF_HAND;
        int used;
        final PlayerInventory inventory = (PlayerInventory) Proxy.newProxyInstance(PlayerInventory.class.getClassLoader(),
            new Class<?>[]{PlayerInventory.class}, (proxy, method, args) -> switch (method.getName()) {
                case "getItemInOffHand" -> off;
                case "getItemInMainHand" -> new ItemStack(Material.AIR);
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
            off = card.clone(); use = null; active = false; used = 0; online = true; dead = false;
            mode = GameMode.SURVIVAL; hand = EquipmentSlot.OFF_HAND;
        }
        void use(int ticks) { use = off.clone(); active = true; used = ticks; }
        PlayerItemConsumeEvent consume(ItemStack card, EquipmentSlot hand) {
            return new PlayerItemConsumeEvent(player, card.clone(), hand);
        }
    }
}

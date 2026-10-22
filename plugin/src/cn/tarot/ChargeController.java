package cn.tarot;

import io.papermc.paper.datacomponent.DataComponentTypes;
import io.papermc.paper.datacomponent.item.Consumable;
import io.papermc.paper.datacomponent.item.consumable.ItemUseAnimation;
import io.papermc.paper.event.player.PlayerStopUsingItemEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.event.player.PlayerTeleportEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

/** Uses Minecraft's real item-use state; a click alone can never finish a charge. */
final class ChargeController implements Listener {
    static final int CHARGE_TICKS = 100;
    private static final Key SILENT = Key.key("minecraft:intentionally_empty");
    private final JavaPlugin plugin;
    private final Predicate<ItemStack> isCard;
    private final Predicate<Player> canStart;
    private final BiConsumer<Player, Double> render;
    private final Consumer<Player> activate;
    private final Map<UUID, Charge> charges = new HashMap<>();
    private BukkitTask task;

    private static final class Charge {
        final Player player;
        final ItemStack card;
        int waiting;
        int rendered = -1;
        boolean observed;
        Charge(Player player, ItemStack card) { this.player = player; this.card = card.clone(); }
    }

    ChargeController(JavaPlugin plugin, Predicate<ItemStack> isCard, Predicate<Player> canStart,
                     BiConsumer<Player, Double> render, Consumer<Player> activate) {
        this.plugin = plugin;
        this.isCard = isCard;
        this.canStart = canStart;
        this.render = render;
        this.activate = activate;
    }

    /** Set after ItemMeta writes, so a previously obtained meta cannot overwrite this component. */
    static void configureCard(ItemStack card) {
        card.setData(DataComponentTypes.CONSUMABLE, Consumable.consumable()
            .consumeSeconds(5f).animation(ItemUseAnimation.BLOCK).sound(SILENT)
            .hasConsumeParticles(false).effects(java.util.List.of()));
    }

    static boolean isConfigured(ItemStack card) {
        if (card == null || card.isEmpty()) return false;
        Consumable value = card.getData(DataComponentTypes.CONSUMABLE);
        return value != null && value.consumeSeconds() == 5f
            && value.animation() == ItemUseAnimation.BLOCK && value.sound().equals(SILENT)
            && !value.hasConsumeParticles() && value.consumeEffects().isEmpty();
    }

    void start() {
        if (task != null) return;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        task = Bukkit.getScheduler().runTaskTimer(plugin, this::tick, 1L, 1L);
    }

    void shutdown() {
        if (task != null) task.cancel();
        task = null;
        for (Charge charge : java.util.List.copyOf(charges.values())) cancel(charge.player);
        HandlerList.unregisterAll(this);
    }

    /** Call on an allowed OFF_HAND right-click; do not cancel its native item-use event. */
    boolean request(Player player) {
        ItemStack card = player.getInventory().getItemInOffHand();
        if (!eligible(player) || !isCard.test(card) || !isConfigured(card) || !canStart.test(player)) {
            cancel(player);
            return false;
        }
        Charge previous = charges.get(player.getUniqueId());
        if (previous == null || !previous.card.equals(card))
            charges.put(player.getUniqueId(), new Charge(player, card));
        return true;
    }

    boolean isCharging(Player player) { return charges.containsKey(player.getUniqueId()); }

    /** Package-visible for server selftests; scheduling never decides whether activation succeeds. */
    void tick() {
        for (Charge charge : java.util.List.copyOf(charges.values())) {
            Player player = charge.player;
            if (!eligible(player) || !charge.card.equals(player.getInventory().getItemInOffHand())) {
                cancel(player);
                continue;
            }
            if (!using(player, charge)) {
                // Interaction is dispatched before vanilla starts use. Give it at most three ticks.
                if (charge.observed || ++charge.waiting > 3) cancel(player);
                continue;
            }
            charge.observed = true;
            int used = player.getActiveItemUsedTime();
            if (used < 0 || used < charge.rendered) {
                cancel(player);
                continue;
            }
            if (charge.rendered < 0 || used - charge.rendered >= 2) {
                charge.rendered = used;
                render.accept(player, Math.min(1d, used / (double) CHARGE_TICKS));
            }
        }
    }

    private static boolean eligible(Player player) {
        return player.isOnline() && !player.isDead() && player.getGameMode() != GameMode.SPECTATOR;
    }

    private static boolean using(Player player, Charge charge) {
        return player.hasActiveItem() && player.getActiveItemHand() == EquipmentSlot.OFF_HAND
            && charge.card.equals(player.getActiveItem());
    }

    void cancel(Player player) {
        Charge charge = charges.remove(player.getUniqueId());
        if (charge != null && using(player, charge)) player.clearActiveItem();
    }

    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = false)
    public void consume(PlayerItemConsumeEvent event) {
        // Even an unapproved use (including the main hand) must never eat a tarot card.
        if (!isCard.test(event.getItem())) return;
        boolean previouslyCancelled = event.isCancelled();
        event.setCancelled(true);
        Player player = event.getPlayer();
        Charge charge = charges.remove(player.getUniqueId());
        if (previouslyCancelled || charge == null || event.getHand() != EquipmentSlot.OFF_HAND
            || !eligible(player) || !charge.card.equals(event.getItem())
            || !charge.card.equals(player.getInventory().getItemInOffHand()) || !using(player, charge)
            || player.getActiveItemUsedTime() < CHARGE_TICKS || !canStart.test(player)) return;
        // Paper sets remaining use time to zero before this event, including its lag compensation.
        // Cancelling consumption keeps the exact physical card and also resets native use afterward.
        activate.accept(player);
    }

    @EventHandler public void stop(PlayerStopUsingItemEvent event) {
        if (isCard.test(event.getItem())) charges.remove(event.getPlayer().getUniqueId());
    }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void swap(PlayerSwapHandItemsEvent event) { cancel(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void held(PlayerItemHeldEvent event) { cancel(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void drop(PlayerDropItemEvent event) { cancel(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void open(InventoryOpenEvent event) { if (event.getPlayer() instanceof Player p) cancel(p); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void click(InventoryClickEvent event) { if (event.getWhoClicked() instanceof Player p) cancel(p); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void drag(InventoryDragEvent event) { if (event.getWhoClicked() instanceof Player p) cancel(p); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void teleport(PlayerTeleportEvent event) { cancel(event.getPlayer()); }
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void gameMode(PlayerGameModeChangeEvent event) { cancel(event.getPlayer()); }
    @EventHandler public void world(PlayerChangedWorldEvent event) { cancel(event.getPlayer()); }
    @EventHandler public void quit(PlayerQuitEvent event) { cancel(event.getPlayer()); }
    @EventHandler public void death(PlayerDeathEvent event) { cancel(event.getEntity()); }
}

package cn.tarot;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCreativeEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.persistence.PersistentDataType;

/**
 * Server-only regression tests: real Paper item metadata, inventories, event
 * constructors and registered event dispatch; synthetic disconnected players
 * and views. This intentionally does not claim to test a connected client.
 */
final class UpgradeEventTest {
    private static final NamespacedKey CARD = new NamespacedKey("tarotrainbow", "card");
    private static final NamespacedKey LEVEL = new NamespacedKey("tarotrainbow", "level");

    static void run(TarotPlugin plugin) {
        check(Bukkit.isPrimaryThread(), "tests must run on the server thread");
        int successes = 0;
        for (int id = 1; id <= 22; id++) {
            for (int level = 1; level < 5; level++) {
                int raw = level % 2 == 0 ? 36 : 9;
                Fixture f = new Fixture(GameMode.SURVIVAL);
                f.fill(); // Upgrading must work even with no free inventory slots.
                f.target(raw, plugin.makeCard(id, level));
                f.cursor = plugin.makeCard(id, level);
                ItemStack[] before = copy(f.storage.getContents());
                InventoryClickEvent event = f.click(raw, id % 2 == 0 ? ClickType.LEFT : ClickType.RIGHT);
                dispatch(event);
                assertUpgrade(f, event, before, raw, id, level);
                successes++;
            }
        }
        for (int level = 1; level < 5; level++) {
            Fixture f = new Fixture(GameMode.ADVENTURE);
            int raw = level % 2 == 0 ? 9 : 36;
            f.target(raw, plugin.makeCard(22, level));
            f.cursor = plugin.makeCard(22, level);
            ItemStack[] before = copy(f.storage.getContents());
            InventoryClickEvent event = f.click(raw, ClickType.LEFT);
            dispatch(event);
            assertUpgrade(f, event, before, raw, 22, level);
            successes++;
        }

        reject(plugin, 22, 5, 22, 5, "五星");
        reject(plugin, 1, 1, 2, 1, "同名");
        reject(plugin, 1, 1, 1, 2, "同星");

        Fixture cancelled = new Fixture(GameMode.SURVIVAL);
        cancelled.target(9, plugin.makeCard(1, 1));
        cancelled.cursor = plugin.makeCard(1, 1);
        ItemStack[] cancelledBefore = copy(cancelled.storage.getContents());
        ItemStack cancelledCursor = cancelled.cursor.clone();
        InventoryClickEvent protectedClick = cancelled.click(9, ClickType.LEFT);
        protectedClick.setCancelled(true);
        dispatch(protectedClick);
        unchanged(cancelled, cancelledBefore, cancelledCursor, "pre-cancelled click");
        check(cancelled.messages.isEmpty(), "pre-cancelled click must be ignored");

        for (ClickType type : List.of(ClickType.SHIFT_LEFT, ClickType.NUMBER_KEY, ClickType.DOUBLE_CLICK)) {
            Fixture f = new Fixture(GameMode.SURVIVAL);
            f.target(9, plugin.makeCard(1, 1));
            f.cursor = plugin.makeCard(1, 1);
            ItemStack[] before = copy(f.storage.getContents());
            ItemStack cursorBefore = f.cursor.clone();
            InventoryClickEvent event = f.click(9, type);
            dispatch(event);
            check(!event.isCancelled(), "non-placement click must remain unchanged: " + type);
            unchanged(f, before, cursorBefore, type.toString());
        }

        // A creative pickup sends AIR as its proposed replacement. The old
        // mouse-click guard silently rejected this before explaining the mode.
        creative(plugin, true, true);
        creative(plugin, true, false);
        creative(plugin, false, false);

        Fixture drag = new Fixture(GameMode.SURVIVAL);
        drag.target(9, plugin.makeCard(1, 1));
        drag.cursor = plugin.makeCard(1, 1);
        ItemStack[] dragBefore = copy(drag.storage.getContents());
        ItemStack dragCursor = drag.cursor.clone();
        InventoryDragEvent nativeDrag = new InventoryDragEvent(drag.view,
            new ItemStack(Material.AIR), drag.cursor.clone(), false,
            Map.of(10, plugin.makeCard(1, 1)));
        dispatch(nativeDrag);
        check(!nativeDrag.isCancelled(), "native drag must not be intercepted");
        unchanged(drag, dragBefore, dragCursor, "native drag");

        plugin.getLogger().info("TAROT UPGRADE EVENT SELFTEST PASS: " + successes
            + " upgrades via registered event dispatch; raw9/raw36; survival/adventure; full inventory;"
            + " max-level/mismatch/pre-cancelled checks; creative feedback without mutation; native drag untouched.");
    }

    private static void assertUpgrade(Fixture f, InventoryClickEvent e, ItemStack[] before,
                                      int raw, int id, int oldLevel) {
        String label = "card=" + id + " level=" + oldLevel + " raw=" + raw;
        check(e.getClickedInventory() == f.player.getInventory(), "player inventory mapping " + label);
        check(e.getSlot() == (raw == 36 ? 0 : 9), "raw/local slot mapping " + label);
        check(e.isCancelled(), "upgrade must cancel vanilla swap " + label);
        ItemStack upgraded = f.target(raw);
        check(value(upgraded, CARD) == id && value(upgraded, LEVEL) == oldLevel + 1
            && upgraded.getAmount() == 1, "upgraded target " + label);
        check(empty(f.cursor), "material cursor must be consumed " + label);
        before[local(raw)] = upgraded.clone();
        check(Arrays.equals(before, f.storage.getContents()), "unrelated items must survive " + label);
        check(f.messages.stream().anyMatch(s -> s.contains("升级成功")), "success feedback " + label);
        check(f.sounds == 1, "success sound " + label);
    }

    private static void reject(TarotPlugin plugin, int sourceId, int sourceLevel,
                               int targetId, int targetLevel, String message) {
        Fixture f = new Fixture(GameMode.SURVIVAL);
        f.target(36, plugin.makeCard(targetId, targetLevel));
        f.cursor = plugin.makeCard(sourceId, sourceLevel);
        ItemStack[] before = copy(f.storage.getContents());
        ItemStack cursorBefore = f.cursor.clone();
        InventoryClickEvent event = f.click(36, ClickType.LEFT);
        dispatch(event);
        check(event.isCancelled(), "invalid pair must cancel the vanilla swap");
        unchanged(f, before, cursorBefore, "invalid pair");
        check(f.messages.stream().anyMatch(s -> s.contains(message)), "invalid pair feedback: " + message);
    }

    private static void creative(TarotPlugin plugin, boolean nativeCreative, boolean pickup) {
        Fixture f = new Fixture(GameMode.CREATIVE);
        f.target(9, plugin.makeCard(1, 1));
        f.cursor = pickup ? new ItemStack(Material.AIR) : plugin.makeCard(1, 1);
        ItemStack[] before = copy(f.storage.getContents());
        ItemStack cursorBefore = f.cursor.clone();
        InventoryClickEvent event = nativeCreative
            ? new InventoryCreativeEvent(f.view, InventoryType.SlotType.CONTAINER, 9, f.cursor.clone())
            : f.click(9, ClickType.LEFT);
        ItemStack proposed = event.getCursor().clone();
        dispatch(event);
        check(!event.isCancelled(), "creative movement must not be cancelled");
        check(Objects.equals(event.getCursor(), proposed), "creative proposed replacement unchanged");
        unchanged(f, before, cursorBefore, "creative event");
        check(f.messages.size() == 1 && f.messages.getFirst().contains("生存")
            && f.messages.getFirst().contains("冒险"), "creative feedback must explain supported modes");
        dispatch(event);
        check(f.messages.size() == 1, "creative hint must be throttled");
        unchanged(f, before, cursorBefore, "repeated creative event");
    }

    private static void dispatch(Event event) { Bukkit.getPluginManager().callEvent(event); }
    private static boolean empty(ItemStack item) { return item == null || item.getType().isAir() || item.getAmount() == 0; }
    private static int value(ItemStack item, NamespacedKey key) {
        return empty(item) ? 0 : item.getItemMeta().getPersistentDataContainer()
            .getOrDefault(key, PersistentDataType.INTEGER, 0);
    }
    private static ItemStack[] copy(ItemStack[] source) {
        return Arrays.stream(source).map(s -> s == null ? null : s.clone()).toArray(ItemStack[]::new);
    }
    private static void unchanged(Fixture f, ItemStack[] before, ItemStack cursor, String label) {
        check(Arrays.equals(before, f.storage.getContents()), label + " inventory mutated");
        check(Objects.equals(cursor, f.cursor), label + " cursor mutated");
        check(f.sounds == 0, label + " unexpectedly emitted a success sound");
    }
    private static int local(int raw) {
        if (raw >= 9 && raw < 36) return raw;
        if (raw >= 36 && raw < 45) return raw - 36;
        if (raw >= 5 && raw < 9) return 44 - raw;
        return raw == 45 ? 40 : raw;
    }
    private static void check(boolean passed, String message) {
        if (!passed) throw new IllegalStateException("Tarot upgrade event test failed: " + message);
    }

    private static final class Fixture {
        final Inventory storage = Bukkit.createInventory(null, 36);
        final Inventory top = Bukkit.createInventory(null, 9);
        final UUID uuid = UUID.randomUUID();
        final List<String> messages = new ArrayList<>();
        final GameMode mode;
        final PlayerInventory inventory;
        final Player player;
        final InventoryView view;
        ItemStack cursor = new ItemStack(Material.AIR);
        int sounds;

        Fixture(GameMode mode) {
            this.mode = mode;
            inventory = proxy(PlayerInventory.class, (object, method, args) -> switch (method.getName()) {
                case "getItem" -> storage.getItem((Integer) args[0]);
                case "setItem" -> { storage.setItem((Integer) args[0], (ItemStack) args[1]); yield null; }
                case "getContents", "getStorageContents" -> storage.getContents();
                case "getSize" -> 36;
                case "getType" -> InventoryType.PLAYER;
                default -> objectMethod(object, method.getName(), args);
            });
            player = proxy(Player.class, (object, method, args) -> switch (method.getName()) {
                case "getInventory" -> inventory;
                case "getGameMode" -> mode;
                case "getUniqueId" -> uuid;
                case "getName" -> "TarotOfflineEventTest";
                case "isOnline" -> false;
                case "getLocation" -> new Location(Bukkit.getWorlds().getFirst(), 0, 80, 0);
                case "playSound" -> { sounds++; yield null; }
                case "sendMessage" -> { messages.add(Arrays.toString(args)); yield null; }
                default -> objectMethod(object, method.getName(), args);
            });
            view = proxy(InventoryView.class, (object, method, args) -> switch (method.getName()) {
                case "getPlayer" -> player;
                case "getTopInventory" -> top;
                case "getBottomInventory" -> inventory;
                case "getType" -> InventoryType.CRAFTING;
                case "getInventory" -> (Integer) args[0] < 0 ? null : (Integer) args[0] < 5 ? top : inventory;
                case "convertSlot" -> local((Integer) args[0]);
                case "getItem" -> (Integer) args[0] < 5 ? top.getItem((Integer) args[0]) : target((Integer) args[0]);
                case "setItem" -> {
                    if ((Integer) args[0] < 5) top.setItem((Integer) args[0], (ItemStack) args[1]);
                    else target((Integer) args[0], (ItemStack) args[1]);
                    yield null;
                }
                case "getCursor" -> cursor;
                case "setCursor" -> { cursor = (ItemStack) args[0]; yield null; }
                case "countSlots" -> 46;
                default -> objectMethod(object, method.getName(), args);
            });
        }

        void fill() { for (int i = 0; i < 36; i++) storage.setItem(i, new ItemStack(Material.STONE, 64)); }
        void target(int raw, ItemStack item) { storage.setItem(local(raw), item); }
        ItemStack target(int raw) { return storage.getItem(local(raw)); }
        InventoryClickEvent click(int raw, ClickType click) {
            return new InventoryClickEvent(view, raw >= 36 ? InventoryType.SlotType.QUICKBAR
                : InventoryType.SlotType.CONTAINER, raw, click,
                raw == 36 ? InventoryAction.NOTHING : InventoryAction.SWAP_WITH_CURSOR);
        }
    }

    private static Object objectMethod(Object object, String method, Object[] args) {
        return switch (method) {
            case "equals" -> object == args[0];
            case "hashCode" -> System.identityHashCode(object);
            case "toString" -> "TarotOfflineEventTestProxy";
            default -> throw new UnsupportedOperationException("Unexpected test proxy method: " + method);
        };
    }

    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(UpgradeEventTest.class.getClassLoader(), new Class<?>[]{type}, handler));
    }
}

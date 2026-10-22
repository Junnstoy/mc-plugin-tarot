package cn.tarot;

import java.io.IOException;
import java.lang.reflect.Proxy;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
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

/** Server-only GUI event tests, isolated from every real player and album file. */
final class AlbumMenuTest {
    static void run(TarotPlugin plugin) {
        check(Bukkit.isPrimaryThread(), "must run on server thread");
        Path directory;
        try { directory = Files.createTempDirectory(plugin.getDataFolder().toPath(), "album-menu-test-"); }
        catch (IOException exception) { throw new IllegalStateException(exception); }
        AlbumService service = new AlbumService(plugin, new AlbumStore(directory));
        AlbumMenu menu = new AlbumMenu(plugin, service);
        Fixture f = new Fixture();
        Bukkit.getPluginManager().registerEvents(menu, plugin);
        try {
            f.storage.setItem(0, service.makeAlbum());
            menu.open(f.player, 1);
            check(AlbumMenu.isAlbumView(f.view), "album view holder");
            check(f.top.getItem(0).getType() == Material.GRAY_STAINED_GLASS_PANE, "uncollected placeholder");
            check(f.top.getSize() == 54 && f.top.getItem(45).getType() == Material.ENCHANTED_BOOK,
                "54-slot first-star page");

            ItemStack original = plugin.makeCard(1, 1);
            var meta = original.getItemMeta();
            meta.displayName(Component.text("Album test preserved name"));
            original.setItemMeta(meta);
            f.storage.setItem(9, original.clone());
            InventoryClickEvent deposit = f.click(54, ClickType.SHIFT_LEFT);
            dispatch(deposit, menu, plugin);
            check(deposit.isCancelled() && empty(f.storage.getItem(9)), "shift deposit consumes exactly one card");
            check(service.progress(f.player).cardinality() == 1 && service.progress(f.player).get(0), "correct card recorded");
            check(original.equals(service.snapshot(f.player)[0]), "original card metadata preserved in storage");
            menu.open(f.player, 1);
            check(f.top.getItem(0).getType() == Material.PAPER && !f.top.getItem(0).equals(original),
                "display is a decorated copy");

            int guarded = 0;
            for (ClickType type : List.of(ClickType.SHIFT_LEFT, ClickType.SHIFT_RIGHT, ClickType.NUMBER_KEY,
                    ClickType.SWAP_OFFHAND, ClickType.DOUBLE_CLICK, ClickType.DROP, ClickType.CONTROL_DROP,
                    ClickType.MIDDLE, ClickType.RIGHT, ClickType.UNKNOWN, ClickType.CREATIVE)) {
                ItemStack[] before = copy(f.storage.getContents());
                ItemStack[] display = copy(f.top.getContents());
                InventoryClickEvent event = f.click(0, type);
                dispatch(event, menu, plugin);
                check(event.isCancelled(), "cancel display extraction " + type);
                check(Arrays.equals(before, f.storage.getContents()) && Arrays.equals(display, f.top.getContents())
                    && empty(f.cursor) && service.progress(f.player).cardinality() == 1, "no transfer " + type);
                guarded++;
            }
            f.storage.setItem(9, plugin.makeCard(2, 1));
            f.cursor = plugin.makeCard(2, 1);
            ItemStack[] before = copy(f.storage.getContents());
            ItemStack cursor = f.cursor.clone();
            InventoryClickEvent bottom = f.click(54, ClickType.LEFT);
            dispatch(bottom, menu, plugin);
            check(bottom.isCancelled() && Arrays.equals(before, f.storage.getContents()) && cursor.equals(f.cursor),
                "album cancellation blocks normal tarot merge in bottom inventory");
            InventoryDragEvent drag = new InventoryDragEvent(f.view, new ItemStack(Material.AIR),
                f.cursor.clone(), false, Map.of(1, plugin.makeCard(2, 1), 55, plugin.makeCard(2, 1)));
            dispatch(drag, menu, plugin);
            check(drag.isCancelled() && Arrays.equals(before, f.storage.getContents()) && cursor.equals(f.cursor),
                "top/bottom drag blocked without touching cursor");

            InventoryClickEvent occupiedCursor = f.click(0, ClickType.LEFT);
            dispatch(occupiedCursor, menu, plugin);
            check(occupiedCursor.isCancelled() && service.progress(f.player).get(0) && cursor.equals(f.cursor),
                "withdrawal requires empty cursor");
            f.cursor = new ItemStack(Material.AIR);
            for (int slot = 1; slot < 36; slot++) f.storage.setItem(slot, new ItemStack(Material.STONE, 64));
            before = copy(f.storage.getContents());
            InventoryClickEvent full = f.click(0, ClickType.LEFT);
            dispatch(full, menu, plugin);
            check(full.isCancelled() && service.progress(f.player).get(0) && Arrays.equals(before, f.storage.getContents()),
                "full-inventory withdrawal preserves stored card and inventory");
            f.storage.setItem(9, null);
            InventoryClickEvent withdraw = f.click(0, ClickType.LEFT);
            dispatch(withdraw, menu, plugin);
            check(withdraw.isCancelled() && service.progress(f.player).isEmpty() && original.equals(f.storage.getItem(9)),
                "withdraw returns original card without display lore");

            f.cursor = plugin.makeCard(2, 1);
            InventoryClickEvent cursorDeposit = f.click(1, ClickType.LEFT);
            dispatch(cursorDeposit, menu, plugin);
            check(cursorDeposit.isCancelled() && empty(f.cursor) && service.progress(f.player).get(1), "cursor deposit recorded once");
            f.cursor = plugin.makeCard(3, 1);
            ItemStack wrong = f.cursor.clone();
            dispatch(f.click(3, ClickType.LEFT), menu, plugin);
            check(wrong.equals(f.cursor) && !service.progress(f.player).get(3), "mismatched cursor deposit preserved");
            f.cursor = new ItemStack(Material.AIR);

            f.storage.setItem(0, null);
            ItemStack[] beforeMissing = copy(f.storage.getContents());
            dispatch(f.click(1, ClickType.LEFT), menu, plugin);
            check(service.progress(f.player).get(1) && Arrays.equals(beforeMissing, f.storage.getContents()),
                "lost album blocks further withdrawal");
            f.storage.setItem(0, service.makeAlbum());
            f.mode = GameMode.CREATIVE;
            InventoryCreativeEvent creative = new InventoryCreativeEvent(f.view, InventoryType.SlotType.CONTAINER,
                1, plugin.makeCard(2, 1));
            dispatch(creative, menu, plugin);
            check(creative.isCancelled() && service.progress(f.player).get(1), "creative slot replacement canceled");
            f.mode = GameMode.SURVIVAL;

            // Both the prior close and refresh tasks must ignore a newly opened non-album view.
            dispatch(f.click(49, ClickType.LEFT), menu, plugin);
            Inventory external = Bukkit.createInventory(null, 9);
            f.top = external;
            int opens = f.opens, closes = f.closes;
            Bukkit.getScheduler().runTask(plugin, () -> {
                check(f.top == external && f.opens == opens && f.closes == closes,
                    "scheduled stale tab/refresh/close must not reopen or close another view");
                plugin.getLogger().info("TAROT ALBUM MENU DEFERRED SELFTEST PASS: stale view tasks ignored.");
                f.online = false;
            });
            plugin.getLogger().info("TAROT ALBUM MENU SELFTEST PASS: " + guarded
                + " forbidden click variants; bottom merge/drag/creative guarded; shift/cursor deposit; full withdrawal"
                + " preserved; original metadata restored; album carry check. Synthetic player, isolated store.");
        } finally {
            HandlerList.unregisterAll(menu);
            // Delete only this test's exact UUID file and directory; never touch live collection files.
            try {
                Files.deleteIfExists(directory.resolve(f.id + ".yml"));
                Files.deleteIfExists(directory.resolve(f.id + ".yml.tmp"));
                Files.deleteIfExists(directory);
            } catch (IOException exception) {
                plugin.getLogger().warning("Could not clean isolated album menu test files: " + exception.getMessage());
            }
        }
    }

    /** Select the actual registered Bukkit executors, without dispatching to the live AlbumService/Menu. */
    private static void dispatch(Event event, AlbumMenu menu, TarotPlugin plugin) {
        for (var listener : event.getHandlers().getRegisteredListeners()) {
            if (listener.getListener() != menu && listener.getListener() != plugin) continue;
            try { listener.callEvent(event); }
            catch (Exception exception) { throw new IllegalStateException("Album menu event dispatch failed", exception); }
        }
    }
    private static boolean empty(ItemStack item) { return item == null || item.isEmpty(); }
    private static ItemStack[] copy(ItemStack[] items) {
        return Arrays.stream(items).map(item -> item == null ? null : item.clone()).toArray(ItemStack[]::new);
    }
    private static void check(boolean condition, String message) {
        if (!condition) throw new IllegalStateException("Album menu test failed: " + message);
    }

    private static final class Fixture {
        final UUID id = UUID.randomUUID();
        final Inventory storage = Bukkit.createInventory(null, 36);
        Inventory top = Bukkit.createInventory(null, 9);
        final PlayerInventory inventory;
        final Player player;
        final InventoryView view;
        final List<String> messages = new ArrayList<>();
        ItemStack cursor = new ItemStack(Material.AIR);
        GameMode mode = GameMode.SURVIVAL;
        boolean online = true;
        int opens, closes;
        Fixture() {
            inventory = proxy(PlayerInventory.class, (object, method, args) -> switch (method.getName()) {
                case "getContents", "getStorageContents" -> storage.getContents();
                case "setContents", "setStorageContents" -> { storage.setContents((ItemStack[]) args[0]); yield null; }
                case "getItem" -> storage.getItem((Integer) args[0]);
                case "setItem" -> { storage.setItem((Integer) args[0], (ItemStack) args[1]); yield null; }
                case "getSize" -> 36;
                case "getType" -> InventoryType.PLAYER;
                default -> fallback(object, method.getName(), args);
            });
            player = proxy(Player.class, (object, method, args) -> switch (method.getName()) {
                case "getInventory" -> inventory;
                case "getUniqueId" -> id;
                case "getName" -> "AlbumMenuIsolatedTest";
                case "getGameMode" -> mode;
                case "getAttribute" -> null;
                case "getHealth" -> 20.0;
                case "isDead" -> false;
                case "isOnline" -> online;
                case "getOpenInventory" -> currentView();
                case "getItemOnCursor" -> cursor;
                case "setItemOnCursor" -> { cursor = (ItemStack) args[0]; yield null; }
                case "openInventory" -> { top = (Inventory) args[0]; opens++; yield currentView(); }
                case "closeInventory" -> { closes++; top = Bukkit.createInventory(null, 9); yield null; }
                case "getLocation" -> new Location(Bukkit.getWorlds().getFirst(), 0, 80, 0);
                case "sendMessage" -> { messages.add(Arrays.toString(args)); yield null; }
                case "playSound" -> null;
                default -> fallback(object, method.getName(), args);
            });
            view = proxy(InventoryView.class, (object, method, args) -> switch (method.getName()) {
                case "getPlayer" -> player;
                case "getTopInventory" -> top;
                case "getBottomInventory" -> inventory;
                case "getType" -> InventoryType.CHEST;
                case "getInventory" -> (Integer) args[0] < 0 ? null : (Integer) args[0] < top.getSize() ? top : inventory;
                case "convertSlot" -> local((Integer) args[0]);
                case "getItem" -> (Integer) args[0] < top.getSize() ? top.getItem((Integer) args[0]) : storage.getItem(local((Integer) args[0]));
                case "setItem" -> { if ((Integer) args[0] < top.getSize()) top.setItem((Integer) args[0], (ItemStack) args[1]);
                    else storage.setItem(local((Integer) args[0]), (ItemStack) args[1]); yield null; }
                case "getCursor" -> cursor;
                case "setCursor" -> { cursor = (ItemStack) args[0]; yield null; }
                case "countSlots" -> top.getSize() + 36;
                default -> fallback(object, method.getName(), args);
            });
        }
        InventoryView currentView() { return view; }
        int local(int raw) { if (raw < top.getSize()) return raw; int bottom = raw - top.getSize(); return bottom < 27 ? bottom + 9 : bottom - 27; }
        InventoryClickEvent click(int raw, ClickType type) {
            return new InventoryClickEvent(view, InventoryType.SlotType.CONTAINER, raw, type,
                InventoryAction.SWAP_WITH_CURSOR, type == ClickType.NUMBER_KEY ? 0 : -1);
        }
    }
    private static Object fallback(Object object, String method, Object[] args) {
        return switch (method) {
            case "equals" -> object == args[0];
            case "hashCode" -> System.identityHashCode(object);
            case "toString" -> "AlbumMenuIsolatedTestProxy";
            default -> throw new UnsupportedOperationException("Unexpected album menu proxy method " + method);
        };
    }
    private static <T> T proxy(Class<T> type, java.lang.reflect.InvocationHandler handler) {
        return type.cast(Proxy.newProxyInstance(AlbumMenuTest.class.getClassLoader(), new Class<?>[]{type}, handler));
    }
}

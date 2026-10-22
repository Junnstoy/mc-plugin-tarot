package cn.tarot;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/** All top slots are presentation copies. Only AlbumService transfers owned cards. */
public final class AlbumMenu implements Listener {
    private static final int TEXT = 0xd6d5e9, GOLD = 0xffdc70, CYAN = 0x80ddff, GREEN = 0x8fffb2;
    private final TarotPlugin plugin;
    private final AlbumService service;

    private static final class Holder implements InventoryHolder {
        final UUID owner;
        final int star;
        Inventory inventory;
        Holder(UUID owner, int star) { this.owner = owner; this.star = star; }
        @Override public Inventory getInventory() { return inventory; }
    }

    public AlbumMenu(TarotPlugin plugin, AlbumService service) {
        this.plugin = plugin;
        this.service = service;
    }

    public static boolean isAlbumView(InventoryView view) {
        return view != null && view.getTopInventory().getHolder() instanceof Holder;
    }

    public void open(Player player, int star) {
        if (!player.isOnline() || player.isDead()) return;
        if (!service.hasAlbum(player)) {
            tell(player, "请将塔罗收集册放在随身背包或副手中，再打开收集册。");
            return;
        }
        if (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE) {
            tell(player, "请在生存或冒险模式下存取收集册，避免创造背包复制卡牌。");
            return;
        }
        Holder holder = new Holder(player.getUniqueId(), Math.max(1, Math.min(5, star)));
        holder.inventory = Bukkit.createInventory(holder, 54, text("塔罗收集册 · " + "★".repeat(holder.star), GOLD));
        render(player, holder);
        player.openInventory(holder.inventory);
    }

    public void closeAll() {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (isAlbumView(player.getOpenInventory())) player.closeInventory();
        }
    }

    private static Component text(String line, int color) {
        return Component.text(line, TextColor.color(color)).decoration(TextDecoration.ITALIC, false);
    }

    private static void tell(Player player, String message) {
        player.sendMessage(text("[塔罗收集册] " + message, GOLD));
    }

    private static ItemStack icon(Material material, String title, int color, List<String> lines) {
        ItemStack stack = new ItemStack(material);
        ItemMeta meta = stack.getItemMeta();
        meta.itemName(text(title, color));
        meta.lore(lines.stream().map(line -> text(line, TEXT)).toList());
        stack.setItemMeta(meta);
        return stack;
    }

    private void render(Player player, Holder holder) {
        BitSet progress = service.progress(player);
        ItemStack[] stored = service.snapshot(player);
        Inventory inventory = holder.inventory;
        ItemStack filler = icon(Material.BLACK_STAINED_GLASS_PANE, " ", TEXT, List.of());
        for (int slot = 0; slot < 54; slot++) inventory.setItem(slot, filler.clone());
        for (int slot = 0; slot < AlbumRules.CARD_COUNT; slot++) {
            int id = slot + 1;
            int index = AlbumRules.index(id, holder.star);
            ItemStack owned = stored[index];
            if (owned == null || owned.getType().isAir()) {
                inventory.setItem(slot, icon(Material.GRAY_STAINED_GLASS_PANE,
                    "未收录 · " + TarotPlugin.CARDS[id - 1].name() + " " + "★".repeat(holder.star), TEXT,
                    List.of("拿起对应卡牌，左键点击此格存入", "或 Shift + 左键点击下方背包中的卡牌",
                        "同名同星只收录一张；卡牌仍可取回")));
            } else {
                ItemStack display = owned.clone();
                ItemMeta meta = display.getItemMeta();
                List<Component> lore = new ArrayList<>(meta.hasLore() ? meta.lore() : List.of());
                lore.add(text(" ", TEXT));
                lore.add(text("已收录 · 左键取回至背包", GREEN));
                lore.add(text("取回后立即失去对应进度、羁绊和奖励", GOLD));
                lore.add(text("此处为展示；收集奖励仅随身携带收集册时生效", TEXT));
                meta.lore(lore);
                display.setItemMeta(meta);
                inventory.setItem(slot, display);
            }
        }
        for (int star = 1; star <= AlbumRules.STAR_COUNT; star++) {
            inventory.setItem(44 + star, icon(star == holder.star ? Material.ENCHANTED_BOOK : Material.BOOK,
                "★".repeat(star) + " · " + AlbumRules.countAtStar(progress, star) + " / 22", star == holder.star ? GOLD : CYAN,
                List.of("左键查看此星级", "每个星级集齐 22 张解锁一档跟随粒子")));
        }
        List<String> rewards = new ArrayList<>();
        rewards.add("只统计存入册中的卡牌，每个同名同星计一次");
        rewards.add("随身携带生效；多个收集册共享个人收藏，不叠加奖励");
        rewards.add(" ");
        for (AlbumRules.Milestone milestone : AlbumRules.MILESTONES) {
            rewards.add((AlbumRules.count(progress) >= milestone.required() ? "✓ " : "○ ")
                + milestone.required() + " 种 · " + milestone.name() + "：" + AlbumRules.bonusesText(milestone.bonuses()));
        }
        rewards.add(" ");
        rewards.add("当前合计属性（含羁绊）：");
        List<String> totals = AlbumRules.totals(progress).stream().map(AlbumRules::bonusText).toList();
        rewards.addAll(totals.isEmpty() ? List.of("尚未获得收集奖励") : totals);
        rewards.add("幸运不会增加塔罗卡包的稀有牌概率");
        inventory.setItem(50, icon(Material.NETHER_STAR, "收藏进度 · " + AlbumRules.count(progress) + " / 110", GOLD, rewards));

        List<String> bonds = new ArrayList<>();
        bonds.add("成员均至少收录一张才成立；阶数取各成员最高星的最低值");
        bonds.add("取出卡牌后重新计算；可与已激活的塔罗牌效果叠加");
        for (AlbumRules.Bond bond : AlbumRules.BONDS) {
            int rank = AlbumRules.bondRank(progress, bond);
            bonds.add(" ");
            bonds.add((rank == 0 ? "○ " : "✓ ") + bond.name() + " · " + rank + " / 5 阶");
            bonds.add(bond.cards().stream().map(id -> TarotPlugin.CARDS[id - 1].name()).collect(Collectors.joining("、")));
            for (AlbumRules.Bonus bonus : bond.perStarBonuses()) {
                bonds.add("每阶 " + bonus.label() + " +" + number(bonus.amount() * (bonus.multiply() ? 100 : 1))
                    + (bonus.multiply() ? "%（基础值）" : ""));
            }
        }
        inventory.setItem(51, icon(Material.END_CRYSTAL, "羁绊一览", CYAN, bonds));

        List<Integer> completed = AlbumRules.completedStars(progress);
        int tier = AlbumRules.auraTier(progress);
        boolean particles = service.particlesEnabled(player);
        inventory.setItem(52, icon(particles ? Material.GLOWSTONE_DUST : Material.GUNPOWDER,
            "跟随粒子 · " + (particles ? "已开启" : "已关闭"), GOLD,
            List.of("左键切换显示，不影响属性奖励", "已集齐星级：" + (completed.isEmpty() ? "暂无" : completed.stream()
                .map(star -> "★".repeat(star)).collect(Collectors.joining(" / "))),
                "当前特效：" + (tier == 6 ? "110 种全收集 · 最终彩虹" : tier == 0 ? "尚未解锁" : "第 " + tier + " 档"),
                "需随身携带收集册；取回卡牌后即时重算")));
        inventory.setItem(53, icon(Material.BARRIER, "关闭收集册", TEXT,
            List.of("Shift + 左键背包卡牌：存入", "左键册中卡牌：取回", "收集册只打开自己的收藏，遗失册子不会删除收藏")));
    }

    private static String number(double value) {
        return String.format(java.util.Locale.ROOT, "%.3f", value).replaceAll("0+$", "").replaceAll("\\.$", "");
    }

    /** Verify the exact view again before scheduled changes so a stale click cannot reopen a closed menu. */
    private void next(Player player, Holder holder, Runnable action) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            if (player.isOnline() && player.getOpenInventory().getTopInventory() == holder.inventory) action.run();
        });
    }

    private boolean validate(Player player, Holder holder) {
        if (!holder.owner.equals(player.getUniqueId()) || !service.hasAlbum(player)
                || player.isDead() || (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.ADVENTURE)) {
            next(player, holder, player::closeInventory);
            return false;
        }
        return true;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void click(InventoryClickEvent event) {
        if (!(event.getView().getTopInventory().getHolder() instanceof Holder holder)) return;
        // This also blocks creative cloning, hotbar swaps, double clicks, dropping,
        // collection-to-cursor and every non-left-click operation on display copies.
        event.setCancelled(true);
        if (!(event.getWhoClicked() instanceof Player player) || !validate(player, holder)) return;
        if (event.getClickedInventory() == player.getInventory()) {
            if (event.getClick() == ClickType.SHIFT_LEFT && event.getSlot() >= 0 && event.getSlot() < 36) {
                if (service.depositSlot(player, event.getSlot())) next(player, holder, () -> render(player, holder));
            }
            return;
        }
        if (event.getClickedInventory() != holder.inventory || event.getClick() != ClickType.LEFT) return;
        int slot = event.getRawSlot();
        if (slot >= 0 && slot < AlbumRules.CARD_COUNT) {
            int index = AlbumRules.index(slot + 1, holder.star);
            ItemStack cursor = player.getItemOnCursor();
            boolean emptyCursor = cursor == null || cursor.getType().isAir();
            if (service.progress(player).get(index)) {
                if (!emptyCursor) { tell(player, "取回前请先清空鼠标上的物品；卡牌会放回随身背包。"); return; }
                if (service.withdraw(player, index)) next(player, holder, () -> render(player, holder));
            } else if (!emptyCursor) {
                if (service.depositCursor(player, index)) next(player, holder, () -> render(player, holder));
            } else {
                tell(player, "按住 Shift 左键点击下方背包卡牌即可存入；每种同名同星卡收录一张。");
            }
        } else if (slot >= 45 && slot <= 49) {
            next(player, holder, () -> open(player, slot - 44));
        } else if (slot == 52) {
            service.toggleParticles(player);
            next(player, holder, () -> render(player, holder));
        } else if (slot == 53) {
            next(player, holder, player::closeInventory);
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void drag(InventoryDragEvent event) {
        if (!isAlbumView(event.getView())) return;
        event.setCancelled(true);
        if (event.getWhoClicked() instanceof Player player
                && event.getView().getTopInventory().getHolder() instanceof Holder holder) validate(player, holder);
    }
}

package cn.tarot;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.OfflinePlayer;

/** Exercise the real installed PlaceholderAPI parser rather than calling the formatter alone. */
final class PapiIntegrationTest {
    private static int checks;
    @SuppressWarnings("unchecked")
    static void run(TarotPlugin plugin) {
        checks = 0;
        UUID uuid = UUID.randomUUID();
        Map<UUID, TarotPlaceholderValues.Snapshot> cache = null;
        try {
            check(PlaceholderAPI.isRegistered("tarot"), "registered expansion");
            Field field = TarotPlugin.class.getDeclaredField("placeholderCache"); field.setAccessible(true);
            cache = (Map<UUID, TarotPlaceholderValues.Snapshot>) field.get(plugin);
            OfflinePlayer player = (OfflinePlayer) Proxy.newProxyInstance(OfflinePlayer.class.getClassLoader(),
                new Class<?>[]{OfflinePlayer.class}, (object, method, args) -> switch (method.getName()) {
                    case "getUniqueId" -> uuid;
                    case "getPlayer" -> null;
                    case "isOnline", "hasPlayedBefore" -> false;
                    case "getName" -> "TarotPapiTest";
                    case "equals" -> object == args[0];
                    case "hashCode" -> System.identityHashCode(object);
                    case "toString" -> "TarotPapiTest";
                    default -> throw new UnsupportedOperationException(method.getName());
                });
            cache.put(uuid, TarotPlaceholderValues.Snapshot.empty());
            parsed(player, "effect_name", "无"); parsed(player, "album_progress", "0/110");
            parsed(player, "effect_remaining", "00:00:00"); parsed(player, "album_level", "0");
            parsed(null, "album_count", "0"); parsed(null, "effect_active", "false");
            parsed(player, "does_not_exist", "%tarot_does_not_exist%");
            BitSet all = new BitSet(); all.set(0, 110);
            var complete = new TarotPlaceholderValues.Snapshot(22, "世界", 5, "UR", 1, Long.MAX_VALUE,
                true, all, true, true, true);
            cache.put(uuid, complete);
            for (String key : List.of("effect_name", "effect_id", "effect_level", "effect_rarity", "effect_active",
                "effect_remaining", "effect_remaining_seconds", "switch_cd", "switch_cd_seconds", "bypass_cd",
                "album_count", "album_total", "album_progress", "album_percent", "album_percentage", "album_level",
                "album_carried", "album_rewards_active", "album_particles_enabled", "album_particles_active",
                "album_completed_stars", "album_milestones", "album_bonds"))
                parsed(player, key, TarotPlaceholderValues.format(key, complete, System.currentTimeMillis()));
            for (int star = 1; star <= 5; star++) {
                parsed(player, "album_star_" + star + "_count", "22");
                parsed(player, "album_star_" + star + "_progress", "22/22");
                parsed(player, "album_star_" + star + "_percent", "100.00");
                parsed(player, "album_star_" + star + "_complete", "true");
            }
            for (var bond : AlbumRules.BONDS) parsed(player, "album_bond_" + bond.key() + "_level", "5");
            String scoreboard = "%tarot_effect_name%|%tarot_effect_remaining%|%tarot_album_progress%|%tarot_album_level%";
            check(PlaceholderAPI.setPlaceholders(player, scoreboard).equals("世界|永久|110/110|6"), "multiple placeholders in one scoreboard");
            String async = CompletableFuture.supplyAsync(() -> PlaceholderAPI.setPlaceholders(player, scoreboard))
                .get(5, TimeUnit.SECONDS);
            check(async.equals("世界|永久|110/110|6"), "async scoreboard uses immutable cache");
            long now = System.currentTimeMillis();
            cache.put(uuid, new TarotPlaceholderValues.Snapshot(2, "魔术师", 2, "R", now + 86400000,
                now + 1800000, false, new BitSet(), false, false, true));
            parsed(player, "effect_name", "魔术师"); parsed(player, "bypass_cd", "false");
            long effect = Long.parseLong(PlaceholderAPI.setPlaceholders(player, "%tarot_effect_remaining_seconds%"));
            long cooldown = Long.parseLong(PlaceholderAPI.setPlaceholders(player, "%tarot_switch_cd_seconds%"));
            check(effect > 86390 && effect <= 86400, "live effect seconds");
            check(cooldown > 1790 && cooldown <= 1800, "live cooldown seconds");
            cache.put(uuid, new TarotPlaceholderValues.Snapshot(2, "魔术师", 2, "R", now - 1, now - 1,
                false, all, false, true, true));
            parsed(player, "effect_name", "无"); parsed(player, "effect_remaining_seconds", "0");
            parsed(player, "switch_cd_seconds", "0"); parsed(player, "album_rewards_active", "false");
            parsed(player, "album_particles_active", "false"); parsed(player, "album_level", "6");
            plugin.getLogger().info("TAROT PAPI INTEGRATION SELFTEST PASS: " + checks
                + " checks; real parser, async snapshots, timer expiry, album progress and bonds.");
        } catch (Exception ex) { throw new IllegalStateException("PAPI integration selftest failed", ex); }
        finally { if (cache != null) cache.remove(uuid); }
    }
    private static void parsed(OfflinePlayer player, String key, String expected) {
        String actual = PlaceholderAPI.setPlaceholders(player, "%tarot_" + key + "%");
        check(actual.equals(expected), key + ": expected=" + expected + ", actual=" + actual);
    }
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new IllegalStateException(message); }
}

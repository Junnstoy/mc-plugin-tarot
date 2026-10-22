package cn.tarot;

import java.util.ArrayList;
import java.util.List;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

/** Optional bridge; the main plugin loads this class only when PlaceholderAPI is enabled. */
public final class TarotPlaceholders extends PlaceholderExpansion {
    private final TarotPlugin plugin;
    private TarotPlaceholders(TarotPlugin plugin) { this.plugin = plugin; }

    /** Return a JDK-only cleanup handle so absence of PAPI never leaks into plugin signatures. */
    public static Runnable install(TarotPlugin plugin) {
        TarotPlaceholders expansion = new TarotPlaceholders(plugin);
        if (!expansion.register()) {
            plugin.getLogger().warning("Could not register tarot placeholders: identifier may already be in use.");
            return () -> {};
        }
        plugin.getLogger().info("PlaceholderAPI registered: %tarot_*%.");
        return expansion::unregister;
    }

    @Override public String getIdentifier() { return "tarot"; }
    @Override public String getAuthor() { return "Yuma"; }
    @Override public String getVersion() { return plugin.getPluginMeta().getVersion(); }
    @Override public boolean persist() { return true; }
    @Override public String onRequest(OfflinePlayer player, String params) {
        return TarotPlaceholderValues.format(params, plugin.placeholderSnapshot(player), System.currentTimeMillis());
    }
    @Override public List<String> getPlaceholders() {
        List<String> keys = new ArrayList<>(List.of("effect_active", "effect_name", "effect_id", "effect_level",
            "effect_rarity", "effect_remaining", "effect_remaining_seconds", "switch_cd", "switch_cd_seconds",
            "bypass_cd", "album_count", "album_total", "album_progress", "album_percent", "album_percentage",
            "album_level", "album_carried", "album_rewards_active", "album_particles_enabled",
            "album_particles_active", "album_completed_stars", "album_milestones", "album_bonds"));
        for (int star = 1; star <= AlbumRules.STAR_COUNT; star++)
            for (String suffix : List.of("count", "progress", "percent", "complete"))
                keys.add("album_star_" + star + "_" + suffix);
        for (var bond : AlbumRules.BONDS) keys.add("album_bond_" + bond.key() + "_level");
        return keys.stream().map(key -> "%tarot_" + key + "%").toList();
    }
}

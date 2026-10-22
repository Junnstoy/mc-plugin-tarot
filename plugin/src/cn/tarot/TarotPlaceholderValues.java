package cn.tarot;

import java.util.BitSet;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

/** Pure, immutable snapshots make scoreboard requests safe on asynchronous threads. */
public final class TarotPlaceholderValues {
    private TarotPlaceholderValues() {}

    public record Snapshot(int cardId, String cardName, int cardLevel, String rarity,
                           long effectExpires, long cooldownExpires, boolean bypass,
                           BitSet progress, boolean carried, boolean particlesEnabled, boolean rewardsEligible) {
        public Snapshot {
            cardName = Objects.requireNonNullElse(cardName, "");
            rarity = Objects.requireNonNullElse(rarity, "");
            progress = progress == null ? new BitSet() : (BitSet) progress.clone();
            AlbumRules.validate(progress);
        }
        @Override public BitSet progress() { return (BitSet) progress.clone(); }
        public static Snapshot empty() {
            return new Snapshot(0, "", 0, "", 0, 0, false, new BitSet(), false, false, false);
        }
    }

    /** Unknown identifiers return null, as required by PlaceholderAPI. */
    public static String format(String identifier, Snapshot snapshot, long now) {
        if (identifier == null) return null;
        Snapshot s = snapshot == null ? Snapshot.empty() : snapshot;
        String key = identifier.toLowerCase(Locale.ROOT);
        BitSet contents = s.progress;
        boolean active = s.cardId > 0 && s.cardId <= AlbumRules.CARD_COUNT
            && (s.bypass || s.effectExpires > now);
        long effectSeconds = active ? s.bypass ? -1 : seconds(s.effectExpires, now) : 0;
        long cooldownSeconds = s.bypass ? 0 : seconds(s.cooldownExpires, now);
        int count = AlbumRules.count(contents);
        int tier = AlbumRules.auraTier(contents);
        String value = switch (key) {
            case "effect_active" -> bool(active);
            case "effect_name", "card_name" -> active ? s.cardName : "无";
            case "effect_id", "card_id" -> Integer.toString(active ? s.cardId : 0);
            case "effect_level", "card_level" -> Integer.toString(active ? s.cardLevel : 0);
            case "effect_rarity", "card_rarity" -> active ? s.rarity : "无";
            case "effect_remaining", "effect_time" -> duration(effectSeconds);
            case "effect_remaining_seconds", "effect_seconds" -> Long.toString(effectSeconds);
            case "switch_cd", "cooldown" -> duration(cooldownSeconds);
            case "switch_cd_seconds", "cooldown_seconds" -> Long.toString(cooldownSeconds);
            case "bypass_cd" -> bool(s.bypass);
            case "album_count" -> Integer.toString(count);
            case "album_total" -> Integer.toString(AlbumRules.SLOT_COUNT);
            case "album_progress" -> count + "/" + AlbumRules.SLOT_COUNT;
            case "album_percent" -> percent(count, AlbumRules.SLOT_COUNT);
            case "album_percentage" -> percent(count, AlbumRules.SLOT_COUNT) + "%";
            case "album_level", "album_particle_level" -> Integer.toString(tier);
            case "album_carried" -> bool(s.carried);
            case "album_rewards_active" -> bool(s.carried && s.rewardsEligible && !AlbumRules.totals(contents).isEmpty());
            case "album_particles_enabled" -> bool(s.particlesEnabled);
            case "album_particles_active" -> bool(s.carried && s.rewardsEligible && s.particlesEnabled && tier > 0);
            case "album_completed_stars" -> AlbumRules.completedStars(contents).stream()
                .map(String::valueOf).collect(Collectors.joining(","));
            case "album_milestones" -> Integer.toString(AlbumRules.unlockedMilestones(contents).size());
            case "album_bonds" -> Long.toString(AlbumRules.BONDS.stream()
                .filter(b -> AlbumRules.bondRank(contents, b) > 0).count());
            default -> null;
        };
        if (value != null) return value;
        if (key.startsWith("album_star_")) {
            String[] parts = key.split("_", -1);
            if (parts.length != 4 || parts[2].length() != 1) return null;
            int star = parts[2].charAt(0) - '0';
            if (star < 1 || star > AlbumRules.STAR_COUNT) return null;
            int atStar = AlbumRules.countAtStar(contents, star);
            return switch (parts[3]) {
                case "count" -> Integer.toString(atStar);
                case "progress" -> atStar + "/" + AlbumRules.CARD_COUNT;
                case "percent" -> percent(atStar, AlbumRules.CARD_COUNT);
                case "complete" -> bool(atStar == AlbumRules.CARD_COUNT);
                default -> null;
            };
        }
        String bondKey = key.startsWith("album_bond_") ? key.substring(11)
            : key.startsWith("bond_") ? key.substring(5) : "";
        if (bondKey.endsWith("_level")) {
            String name = bondKey.substring(0, bondKey.length() - 6);
            return AlbumRules.BONDS.stream().filter(b -> b.key().equals(name)).findFirst()
                .map(b -> Integer.toString(AlbumRules.bondRank(contents, b))).orElse(null);
        }
        return null;
    }

    private static String bool(boolean value) { return Boolean.toString(value); }
    private static String percent(int count, int total) {
        return String.format(Locale.ROOT, "%.2f", count * 100.0 / total);
    }
    private static long seconds(long expires, long now) {
        if (expires <= now) return 0;
        long remaining = expires - now;
        if (remaining < 0) remaining = Long.MAX_VALUE;
        return remaining / 1000 + (remaining % 1000 == 0 ? 0 : 1);
    }
    private static String duration(long seconds) {
        if (seconds < 0) return "永久";
        return String.format(Locale.ROOT, "%02d:%02d:%02d", seconds / 3600,
            seconds / 60 % 60, seconds % 60);
    }
}

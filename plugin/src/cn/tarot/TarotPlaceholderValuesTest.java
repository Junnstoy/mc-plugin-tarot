package cn.tarot;

import java.util.BitSet;
import java.util.Locale;
import java.util.Objects;

public final class TarotPlaceholderValuesTest {
    private static int checks;
    private static final long NOW = 1_700_000_000_000L;
    public static void main(String[] args) {
        run();
        System.out.println("TAROT PLACEHOLDER VALUES SELFTEST PASS: " + checks + " checks.");
    }
    static void run() {
        checks = 0;
        var empty = TarotPlaceholderValues.Snapshot.empty();
        value(empty, "effect_name", "无"); value(empty, "effect_id", "0");
        value(empty, "effect_active", "false"); value(empty, "effect_remaining", "00:00:00");
        value(empty, "effect_remaining_seconds", "0"); value(empty, "switch_cd_seconds", "0");
        value(empty, "album_progress", "0/110"); value(empty, "album_total", "110");
        value(empty, "album_level", "0"); value(empty, "album_completed_stars", "");
        value(empty, "album_particles_active", "false"); value(empty, "album_rewards_active", "false");
        BitSet source = new BitSet();
        for (int card = 1; card <= 22; card++) AlbumRules.add(source, card, 3);
        var s = new TarotPlaceholderValues.Snapshot(22, "世界", 5, "UR", NOW + 86400000,
            NOW + 1800000, false, source, true, true, true);
        source.clear(); check(s.progress().cardinality() == 22, "constructor defensively copies contents");
        s.progress().clear(); check(s.progress().cardinality() == 22, "accessor defensively copies contents");
        value(s, "effect_name", "世界"); value(s, "effect_level", "5"); value(s, "effect_id", "22");
        value(s, "effect_rarity", "UR"); value(s, "effect_active", "true");
        value(s, "effect_remaining", "24:00:00"); value(s, "effect_remaining_seconds", "86400");
        value(s, "switch_cd", "00:30:00"); value(s, "switch_cd_seconds", "1800");
        value(s, "album_progress", "22/110"); value(s, "album_percent", "20.00");
        value(s, "album_percentage", "20.00%"); value(s, "album_level", "3");
        value(s, "album_completed_stars", "3"); value(s, "album_milestones", "2");
        value(s, "album_bonds", "6"); value(s, "album_carried", "true");
        value(s, "album_rewards_active", "true"); value(s, "album_particles_active", "true");
        for (int star = 1; star <= 5; star++) {
            value(s, "album_star_" + star + "_count", star == 3 ? "22" : "0");
            value(s, "album_star_" + star + "_progress", star == 3 ? "22/22" : "0/22");
            value(s, "album_star_" + star + "_percent", star == 3 ? "100.00" : "0.00");
            value(s, "album_star_" + star + "_complete", star == 3 ? "true" : "false");
        }
        for (var bond : AlbumRules.BONDS) {
            value(s, "album_bond_" + bond.key() + "_level", "3");
            value(s, "bond_" + bond.key() + "_level", "3");
        }
        check(Objects.equals(TarotPlaceholderValues.format("switch_cd_seconds", s, NOW + 1800000), "0"), "cooldown boundary");
        check(Objects.equals(TarotPlaceholderValues.format("effect_active", s, NOW + 86400000), "false"), "expiry boundary");
        check(Objects.equals(TarotPlaceholderValues.format("effect_name", s, NOW + 86400000), "无"), "expired card not exposed");
        check(Objects.equals(TarotPlaceholderValues.format("effect_remaining_seconds", s, NOW + 86399999), "1"), "ceil final millisecond");
        var bypass = new TarotPlaceholderValues.Snapshot(22, "世界", 5, "UR", NOW - 1, NOW + 999999, true, s.progress(), true, false, true);
        value(bypass, "effect_remaining", "永久"); value(bypass, "effect_remaining_seconds", "-1");
        value(bypass, "switch_cd", "00:00:00"); value(bypass, "bypass_cd", "true");
        value(bypass, "effect_active", "true"); value(bypass, "album_particles_active", "false");
        var noCardBypass = new TarotPlaceholderValues.Snapshot(0, "", 0, "", 0, 0, true, null, false, false, true);
        value(noCardBypass, "effect_remaining_seconds", "0"); value(noCardBypass, "effect_active", "false");
        var stored = new TarotPlaceholderValues.Snapshot(0, "", 0, "", 0, 0, false, s.progress(), false, true, true);
        value(stored, "album_level", "3"); value(stored, "album_rewards_active", "false");
        value(stored, "album_particles_active", "false");
        var spectator = new TarotPlaceholderValues.Snapshot(0, "", 0, "", 0, 0, false,
            s.progress(), true, true, false);
        value(spectator, "album_carried", "true"); value(spectator, "album_rewards_active", "false");
        value(spectator, "album_particles_active", "false");
        BitSet full = new BitSet(); full.set(0, 110);
        var complete = new TarotPlaceholderValues.Snapshot(0, "", 0, "", 0, 0, false, full, true, true, true);
        value(complete, "album_level", "6"); value(complete, "album_completed_stars", "1,2,3,4,5");
        value(complete, "album_milestones", "6"); value(complete, "album_percentage", "100.00%");
        for (String unknown : new String[]{"", "unknown", "album_star_0_count", "album_star_6_count", "album_star_1_nope", "album_star_11_count", "album_star_x_count", "album_bond_missing_level", "album_bond_heart", "album_star_1_count_extra"})
            value(s, unknown, null);
        check(TarotPlaceholderValues.format(null, s, NOW) == null, "null identifier");
        check(Objects.equals(TarotPlaceholderValues.format("album_progress", null, NOW), "0/110"), "null snapshot");
        Locale old = Locale.getDefault();
        try { Locale.setDefault(Locale.forLanguageTag("tr-TR")); value(s, "EFFECT_ID", "22"); value(s, "album_percent", "20.00"); }
        finally { Locale.setDefault(old); }
        BitSet invalid = new BitSet(); invalid.set(110);
        try { new TarotPlaceholderValues.Snapshot(0, "", 0, "", 0, 0, false, invalid, false, false, true); throw new AssertionError("invalid contents accepted"); }
        catch (IllegalArgumentException expected) { checks++; }
    }
    private static void value(TarotPlaceholderValues.Snapshot s, String key, String expected) {
        String actual = TarotPlaceholderValues.format(key, s, NOW);
        check(Objects.equals(actual, expected), key + ": expected=" + expected + ", actual=" + actual);
    }
    private static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
}

package cn.tarot;

import java.util.ArrayList;
import java.util.List;

/** Pure geometry regressions: actual cast lifecycle and cancellation are tested by the server integration. */
public final class TarotCastTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void invalid(Runnable action) {
        checks++;
        try { action.run(); throw new AssertionError("expected rejection"); }
        catch (IllegalArgumentException expected) { }
    }
    private static double radius(List<AlbumAura.Point> points) {
        return points.stream().filter(p -> p.up() == TarotCast.GROUND_HEIGHT)
            .mapToDouble(p -> Math.hypot(p.right(), p.back())).max().orElse(0);
    }
    public static void main(String[] args) {
        List<AlbumAura.Point> art = new ArrayList<>();
        for (int y = 0; y < 15; y++) for (int x = 0; x < 12; x++)
            art.add(new AlbumAura.Point(-.75 + 1.5 * x / 11, .65 + 2.95 * y / 14, 1.0,
                0x100000 + y * 0x100 + x, .6f));
        check(art.size() == TarotCast.MAX_CARD_POINTS, "test full card image");
        invalid(() -> TarotCast.frame(0, .5, art));
        invalid(() -> TarotCast.frame(6, .5, art));
        invalid(() -> TarotCast.frame(5, Double.NaN, art));
        invalid(() -> TarotCast.frame(5, Double.POSITIVE_INFINITY, art));
        check(TarotCast.CAST_TICKS == 100, "five seconds");
        check(TarotCast.frame(5, -1, art).isEmpty(), "negative progress clamped");
        check(TarotCast.frame(5, 2, art).equals(TarotCast.frame(5, 1, art)), "completed progress clamped");
        int previous = 0;
        for (int level = 1; level <= 5; level++) {
            var full = TarotCast.frame(level, 1, art);
            check(full.size() > previous, "higher stars add detail");
            previous = full.size();
            check(radius(TarotCast.frame(level, .1, art)) < radius(TarotCast.frame(level, .3, art)), "ground circle expands");
            check(Math.abs(radius(TarotCast.frame(level, .45, art)) - radius(full)) < 1e-9, "ground finished growing before midpoint");
            if (level < 5) check(full.stream().noneMatch(art::contains), "card image restricted to five stars");
            if (level < 4) check(full.stream().allMatch(p -> p.up() == TarotCast.GROUND_HEIGHT), "lower levels stay on ground");
            if (level >= 4) {
                check(TarotCast.frame(level, .20, art).stream().allMatch(p -> p.up() == TarotCast.GROUND_HEIGHT), "wings begin after ground draw");
                check(TarotCast.frame(level, .40, art).stream().anyMatch(p -> p.up() > 1), "wings unfold during cast");
            }
            for (int tick = 0; tick <= TarotCast.CAST_TICKS; tick += 2) {
                var points = TarotCast.frame(level, tick / (double) TarotCast.CAST_TICKS, art);
                check(points.size() <= TarotCast.MAX_POINTS, "cast particle cap");
                for (var point : points) {
                    check(Double.isFinite(point.right()) && Double.isFinite(point.up()) && Double.isFinite(point.back()), "finite cast coordinates");
                    check(Float.isFinite(point.size()) && point.size() > 0, "valid particle size");
                }
            }
        }
        check(TarotCast.frame(5, .50, art).stream().noneMatch(art::contains), "card not shown early");
        long midCount = TarotCast.frame(5, .75, art).stream().filter(art::contains).count();
        check(midCount == 90, "half of card revealed halfway through card phase");
        var finalFrame = TarotCast.frame(5, 1, art);
        check(finalFrame.containsAll(art), "full supplied card art visible at completion");
        check(finalFrame.size() == TarotCast.MAX_POINTS, "maximum composition exactly within budget");
        List<AlbumAura.Point> many = new ArrayList<>(art);
        many.addAll(art); many.addAll(art);
        var sampled = TarotCast.frame(5, 1, many);
        check(sampled.size() == TarotCast.MAX_POINTS, "oversized input art is sampled to budget");
        check(sampled.get(180).equals(many.getFirst()) && sampled.getLast().equals(many.getLast()), "sampling spans the complete source image");
        check(TarotCast.frame(5, 1, List.of()).size() == 180, "missing art still draws ground and wings");
        invalid(() -> TarotCast.frame(5, 1, List.of(new AlbumAura.Point(Double.NaN, 2, 1, 0xffffff, .6f))));
        try { finalFrame.clear(); throw new AssertionError("mutable frame"); }
        catch (UnsupportedOperationException expected) { checks++; }
        check(art.size() == 180, "input not modified");
        System.out.println("TarotCastTest PASS: " + checks + " checks");
    }
}

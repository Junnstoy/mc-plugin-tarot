package cn.tarot;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Pure geometry for a five-second tarot activation. All coordinates are relative to the player's feet. */
public final class TarotCast {
    private TarotCast() {}
    public static final int CAST_TICKS = 100;
    public static final int MAX_POINTS = 360;
    public static final int MAX_CARD_POINTS = 180;
    public static final double GROUND_HEIGHT = .08;
    private static final double TAU = Math.PI * 2;
    private static final int[] COLORS = {0, 0xff6464, 0xffa34c, 0xffdf62, 0x78e492, 0xc99cff};
    private static final int[] RAINBOW = {0xff5252, 0xff9b42, 0xffdf55, 0x62e875, 0x48dfdf, 0x588cff, 0xb46aff};
    private static final List<AlbumAura.Point> SMALL_WINGS = wings(4);
    private static final List<AlbumAura.Point> RAINBOW_WINGS = wings(5);

    private static List<AlbumAura.Point> wings(int tier) {
        return new AlbumAura.State(tier)
            .step(AlbumAura.IDLE_CYCLES * AlbumAura.CYCLE_TICKS + AlbumAura.UNFOLD_TICKS, false).points();
    }

    /**
     * cardArt contains the selected card's colored image and outline, already positioned behind the player.
     * Input art order defines the gradual reveal; up to MAX_CARD_POINTS are sampled uniformly from larger art.
     * Progress is clamped to [0,1], while NaN/infinity are rejected. Only five-star casts show card art.
     */
    public static List<AlbumAura.Point> frame(int level, double progress, List<AlbumAura.Point> cardArt) {
        if (level < 1 || level > 5) throw new IllegalArgumentException("card level must be 1..5");
        if (!Double.isFinite(progress)) throw new IllegalArgumentException("cast progress must be finite");
        Objects.requireNonNull(cardArt, "card art");
        double p = Math.max(0, Math.min(1, progress));
        if (p == 0) return List.of();
        List<AlbumAura.Point> points = new ArrayList<>(MAX_POINTS);
        double grow = smooth(p / .45);
        double radius = (.16 + .84 * grow) * (.65 + .07 * level);
        double phase = p * TAU;
        int color = COLORS[level];
        if (level == 1) {
            ring(points, radius, 24, phase, color, false);
        } else if (level == 2) {
            ring(points, radius, 24, phase, color, false);
            ring(points, radius * .72, 16, -phase, 0xffe0a3, false);
            for (int i = 0; i < 4; i++) {
                double angle = i * Math.PI / 2 + phase;
                double x = radius * Math.cos(angle), z = radius * Math.sin(angle);
                for (int corner = 0; corner < 4; corner++) {
                    double a = corner * Math.PI / 2 + angle;
                    add(points, x + Math.cos(a) * .07 * grow, GROUND_HEIGHT,
                        z + Math.sin(a) * .07 * grow, 0xfff2cb, .55f);
                }
            }
        } else {
            // Exactly 60 ground points leaves room for 120 wing points and 180 card points at level five.
            ring(points, radius, 30, phase, color, level == 5);
            for (int i = 0; i < 5; i++) {
                double a = phase + TAU * i / 5 - Math.PI / 2;
                double b = phase + TAU * ((i + 2) % 5) / 5 - Math.PI / 2;
                for (int j = 0; j < 6; j++) {
                    double t = j / 5.0;
                    add(points, radius * .80 * (Math.cos(a) * (1 - t) + Math.cos(b) * t), GROUND_HEIGHT,
                        radius * .80 * (Math.sin(a) * (1 - t) + Math.sin(b) * t),
                        level == 5 ? RAINBOW[(i + 1) % RAINBOW.length] : color, .52f);
                }
            }
        }
        if (level >= 4 && p > .20) {
            double spread = smooth((p - .20) / .55);
            List<AlbumAura.Point> wings = level == 5 ? RAINBOW_WINGS : SMALL_WINGS;
            for (AlbumAura.Point point : wings) {
                add(points, point.right() * (.08 + .92 * spread),
                    1.35 + (point.up() - 1.35) * (.45 + .55 * spread),
                    point.back() + Math.abs(point.right()) * (1 - spread) * .30,
                    point.color(), point.size());
            }
        }
        if (level == 5 && p > .50 && !cardArt.isEmpty()) {
            int artCount = Math.min(MAX_CARD_POINTS, cardArt.size());
            double reveal = smooth((p - .50) / .50);
            int visible = Math.min(artCount, (int) Math.ceil(reveal * artCount));
            for (int i = 0; i < visible; i++) {
                // Select from the whole image rather than truncating one corner of oversized art.
                int index = artCount <= 1 ? 0 : (int) ((long) i * (cardArt.size() - 1) / (artCount - 1));
                AlbumAura.Point point = Objects.requireNonNull(cardArt.get(index), "card art point");
                if (!Double.isFinite(point.right()) || !Double.isFinite(point.up()) || !Double.isFinite(point.back())
                    || !Float.isFinite(point.size()) || point.size() <= 0)
                    throw new IllegalArgumentException("card art point must have finite coordinates and positive size");
                points.add(point);
            }
        }
        if (points.size() > MAX_POINTS) throw new IllegalStateException("cast particle budget exceeded");
        return List.copyOf(points);
    }

    private static double smooth(double value) {
        double x = Math.max(0, Math.min(1, value));
        return x * x * (3 - 2 * x);
    }
    private static void ring(List<AlbumAura.Point> points, double radius, int count, double phase, int color, boolean rainbow) {
        for (int i = 0; i < count; i++) {
            double angle = TAU * i / count + phase;
            add(points, Math.cos(angle) * radius, GROUND_HEIGHT, Math.sin(angle) * radius,
                rainbow ? RAINBOW[i * RAINBOW.length / count] : color, .55f);
        }
    }
    private static void add(List<AlbumAura.Point> points, double right, double up, double back, int color, float size) {
        points.add(new AlbumAura.Point(right, up, back, color, size));
    }
}

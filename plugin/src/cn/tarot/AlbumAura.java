package cn.tarot;

import java.util.ArrayList;
import java.util.List;

/** Deterministic, server-independent collection aura animation. Coordinates are relative to the feet. */
public final class AlbumAura {
    private AlbumAura() {}
    public static final int SWEEP_TICKS = 36;
    public static final int DRAIN_TICKS = 12;
    public static final int PAUSE_TICKS = 20;
    public static final int CYCLE_TICKS = SWEEP_TICKS + DRAIN_TICKS + PAUSE_TICKS;
    public static final int IDLE_CYCLES = 7;
    public static final int UNFOLD_TICKS = 40;
    public static final int MAX_POINTS = 90;
    public static final double CIRCLE_HEIGHT = 2.10;
    public static final double MAX_ARC = Math.PI * 1.5;
    private static final double TAU = Math.PI * 2;
    private static final int[] RAINBOW = {0xff5252, 0xff9b42, 0xffdf55, 0x62e875, 0x48dfdf, 0x588cff, 0xb46aff};
    private static final int[] TIER_COLORS = {0, 0xff6464, 0xffa34c, 0xffdd62, 0x78e492};

    /** right/back are local horizontal axes; up is the height above the player's feet. */
    public record Point(double right, double up, double back, int color, float size) {}
    public record Frame(List<Point> points, boolean idle, int cycleTick, int colorIndex,
                        int completedStillCycles, double unfold) {
        public Frame { points = List.copyOf(points); }
    }

    /** Create a new state when its owner changes world or aura tier. Looking around does not reset it. */
    public static final class State {
        private final int tier;
        private int cycleTick;
        private int colorIndex;
        private int completedStillCycles;
        private boolean stillCycle = true;
        private boolean idle;
        private int idleTicks;
        private long ageTicks;

        public State(int tier) {
            if (tier < 1 || tier > 6) throw new IllegalArgumentException("aura tier must be 1..6");
            this.tier = tier;
        }
        public int tier() { return tier; }

        /** Movement resets the stationary streak, never the phase of an already running circle. */
        public Frame step(int elapsedTicks, boolean moved) {
            if (elapsedTicks < 0) throw new IllegalArgumentException("elapsed ticks must be nonnegative");
            ageTicks += elapsedTicks;
            if (moved) {
                completedStillCycles = 0;
                if (idle) {
                    idle = false;
                    idleTicks = 0;
                    cycleTick = 0;
                    colorIndex = 0;
                }
                long elapsed = (long) cycleTick + elapsedTicks;
                advanceColor(elapsed / CYCLE_TICKS);
                cycleTick = (int) (elapsed % CYCLE_TICKS);
                // If the movement ended exactly on a boundary, the next complete circle is eligible.
                stillCycle = cycleTick == 0;
                return frame();
            }
            if (idle) {
                idleTicks = (int) Math.min(UNFOLD_TICKS, (long) idleTicks + elapsedTicks);
                return frame();
            }
            int left = elapsedTicks;
            while (left > 0) {
                int segment = Math.min(left, CYCLE_TICKS - cycleTick);
                cycleTick += segment;
                left -= segment;
                if (cycleTick == CYCLE_TICKS) {
                    cycleTick = 0;
                    advanceColor(1);
                    if (stillCycle) completedStillCycles++;
                    stillCycle = true;
                    if (completedStillCycles == IDLE_CYCLES) {
                        idle = true;
                        idleTicks = Math.min(UNFOLD_TICKS, left);
                        break;
                    }
                }
            }
            return frame();
        }
        private void advanceColor(long cycles) {
            if (tier >= 5) colorIndex = (int) ((colorIndex + cycles) % RAINBOW.length);
        }
        public Frame frame() {
            List<Point> points = new ArrayList<>(MAX_POINTS);
            double unfold = idle ? smooth(idleTicks / (double) UNFOLD_TICKS) : 0;
            if (idle) idleShape(points, tier, unfold, ageTicks);
            else circle(points, tier, cycleTick, colorIndex);
            return new Frame(points, idle, cycleTick, colorIndex, completedStillCycles, unfold);
        }
    }

    private static double smooth(double value) {
        double x = Math.max(0, Math.min(1, value));
        return x * x * (3 - 2 * x);
    }
    private static void add(List<Point> points, double right, double up, double back, int color, float size) {
        if (points.size() >= MAX_POINTS) throw new IllegalStateException("aura particle budget exceeded");
        points.add(new Point(right, up, back, color, size));
    }
    private static int colorAt(double along) {
        double phase = ((along % 1) + 1) % 1 * RAINBOW.length;
        int first = (int) phase, second = (first + 1) % RAINBOW.length;
        double mix = phase - first;
        int a = RAINBOW[first], b = RAINBOW[second];
        int r = (int) Math.round(((a >> 16) & 255) * (1 - mix) + ((b >> 16) & 255) * mix);
        int g = (int) Math.round(((a >> 8) & 255) * (1 - mix) + ((b >> 8) & 255) * mix);
        int blue = (int) Math.round((a & 255) * (1 - mix) + (b & 255) * mix);
        return (r << 16) | (g << 8) | blue;
    }
    private static void circle(List<Point> points, int tier, int tick, int colorIndex) {
        if (tick >= SWEEP_TICKS + DRAIN_TICKS || tick == 0) return;
        double head = TAU * Math.min(tick, SWEEP_TICKS) / SWEEP_TICKS;
        double arc = tick <= SWEEP_TICKS ? Math.min(head, MAX_ARC)
            : MAX_ARC * (1 - (tick - SWEEP_TICKS) / (double) DRAIN_TICKS);
        double radius = .38 + tier * .018;
        // TRAIL points are visibly wider than dust. Leave space between their centers;
        // tightly sampled outlines merge into an opaque band on an actual client.
        int count = Math.max(2, (int) Math.ceil(arc * radius / .15) + 1);
        int color = tier >= 5 ? RAINBOW[colorIndex] : TIER_COLORS[tier];
        for (int i = 0; i < count; i++) {
            double angle = head - arc + arc * i / (count - 1) - Math.PI / 2;
            add(points, radius * Math.cos(angle), CIRCLE_HEIGHT, radius * Math.sin(angle), color, .55f);
        }
    }
    private static void idleShape(List<Point> points, int tier, double unfold, long age) {
        double turn = (age % 1200) * TAU / 1200;
        if (tier == 1) {
            ring(points, .42 * (.35 + .65 * unfold), 2.12, 0, 0, 16, TIER_COLORS[1], false, turn);
        } else if (tier == 2) {
            ring(points, .43 * (.35 + .65 * unfold), 2.12, 0, 0, 16, TIER_COLORS[2], false, turn);
            ring(points, .27 * (.35 + .65 * unfold), 2.30, 0, 0, 12, 0xffdf83, false, -turn);
        } else if (tier == 3) {
            double grow = .35 + .65 * unfold;
            ring(points, .51 * grow, 2.15, 0, 0, 18, TIER_COLORS[3], false, turn);
            // A five-point star in the horizontal plane, slowly rotating inside the halo.
            for (int i = 0; i < 5; i++) {
                double start = turn + TAU * i / 5 - Math.PI / 2;
                double end = turn + TAU * ((i + 2) % 5) / 5 - Math.PI / 2;
                line(points, Math.cos(start) * .42 * grow, 2.15, Math.sin(start) * .42 * grow,
                    Math.cos(end) * .42 * grow, 2.15, Math.sin(end) * .42 * grow, 4, 0xfff4ac, .48f);
            }
            ring(points, .16 * grow, 2.34, 0, 0, 8, 0xffc84f, false, -turn);
        } else {
            wings(points, tier, unfold);
            if (tier == 6) ring(points, .46, 2.18, 0, 0, 18, 0, true, turn);
        }
    }
    private static void ring(List<Point> points, double radius, double y, double z, double tilt,
                             int count, int color, boolean rainbow, double phase) {
        for (int i = 0; i < count; i++) {
            double angle = TAU * i / count + phase;
            add(points, Math.cos(angle) * radius, y + Math.sin(angle) * radius * Math.sin(tilt),
                z + Math.sin(angle) * radius * Math.cos(tilt), rainbow ? colorAt(i / (double) count) : color, .48f);
        }
    }
    private static void line(List<Point> points, double x1, double y1, double z1,
                             double x2, double y2, double z2, int count, int color, float size) {
        for (int i = 0; i < count; i++) {
            double t = i / (double) (count - 1);
            add(points, x1 + (x2 - x1) * t, y1 + (y2 - y1) * t, z1 + (z2 - z1) * t, color, size);
        }
    }
    // Each outline traces one continuous feathered wing, with an open central root at shoulder height.
    private static final double[][] WING = {
        {0, 0}, {.25, .40}, {.72, .92}, {1.25, 1.12}, {1.77, .86},
        {1.57, .55}, {1.39, .41}, {1.68, .17}, {1.39, .05}, {1.18, .10},
        {1.41, -.25}, {1.10, -.28}, {.87, -.13}, {1.00, -.60}, {.69, -.45},
        {.46, -.21}, {.45, -.67}, {.21, -.37}, {0, 0}
    };
    private static void wings(List<Point> points, int tier, double unfold) {
        boolean rainbow = tier >= 5;
        double scale = tier == 4 ? .72 : 1;
        double spread = .12 + .88 * unfold;
        int count = tier == 4 ? 26 : 32;
        double[] cumulative = new double[WING.length];
        for (int i = 1; i < WING.length; i++) cumulative[i] = cumulative[i - 1]
            + Math.hypot(WING[i][0] - WING[i - 1][0], WING[i][1] - WING[i - 1][1]);
        double length = cumulative[cumulative.length - 1];
        for (int side : new int[] {-1, 1}) {
            int segment = 1;
            for (int i = 0; i < count; i++) {
                double fraction = i / (double) (count - 1);
                double distance = fraction * length;
                while (segment < WING.length - 1 && cumulative[segment] < distance) segment++;
                double amount = (distance - cumulative[segment - 1]) / (cumulative[segment] - cumulative[segment - 1]);
                double x = WING[segment - 1][0] + amount * (WING[segment][0] - WING[segment - 1][0]);
                double y = WING[segment - 1][1] + amount * (WING[segment][1] - WING[segment - 1][1]);
                int color = rainbow ? colorAt(fraction) : (fraction < .5 ? 0x78e492 : 0xc6ffca);
                add(points, side * x * scale * spread, 1.35 + y * scale * (.55 + .45 * unfold),
                    .30 + x * scale * (1 - unfold) * .32, color, rainbow ? .50f : .46f);
            }
        }
    }
}

package cn.tarot;

import java.util.HashSet;
import java.util.Set;

/** State/geometry regression tests; deliberately requires no Bukkit server or particle lifetime. */
public final class AlbumAuraTest {
    private static int checks;
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static void close(double value, double expected, String message) {
        check(Math.abs(value - expected) < 1e-9, message + ": " + value);
    }
    private static void invalid(Runnable action) {
        checks++;
        try { action.run(); throw new AssertionError("expected argument rejection"); }
        catch (IllegalArgumentException expected) { }
    }
    public static void main(String[] args) {
        invalid(() -> new AlbumAura.State(0));
        invalid(() -> new AlbumAura.State(7));
        invalid(() -> new AlbumAura.State(1).step(-1, false));
        check(AlbumAura.SWEEP_TICKS == 36, "faster 1.8-second sweep");
        check(AlbumAura.DRAIN_TICKS == 12, "tail clears in 0.6 seconds");
        check(AlbumAura.PAUSE_TICKS == 20, "one second pause");
        close(AlbumAura.MAX_ARC, Math.PI * 1.5, "three-quarter circle cap");
        AlbumAura.State arc = new AlbumAura.State(1);
        check(arc.frame().points().isEmpty(), "cycle starts empty");
        int threeQuarterSweep = AlbumAura.SWEEP_TICKS * 3 / 4;
        var fullArc = arc.step(threeQuarterSweep, false);
        var first = fullArc.points().getFirst();
        var last = fullArc.points().getLast();
        double from = Math.atan2(first.back(), first.right());
        double to = Math.atan2(last.back(), last.right());
        close((to - from + Math.PI * 2) % (Math.PI * 2), AlbumAura.MAX_ARC, "drawn arc is exactly 75 percent");
        int fullCount = fullArc.points().size();
        check(arc.step(AlbumAura.SWEEP_TICKS - threeQuarterSweep, false).points().size() == fullCount, "length capped through complete sweep");
        check(arc.step(AlbumAura.DRAIN_TICKS / 2, false).points().size() < fullCount, "tail drains smoothly");
        check(arc.step(AlbumAura.DRAIN_TICKS / 2, false).points().isEmpty(), "pause begins only after tail is gone");
        check(arc.step(19, false).points().isEmpty(), "pause lasts full second");
        check(arc.step(1, false).cycleTick() == 0, "next boundary");
        check(!arc.step(1, false).points().isEmpty(), "next sweep starts");

        AlbumAura.State rainbow = new AlbumAura.State(5);
        Set<Integer> colors = new HashSet<>();
        for (int i = 0; i < 7; i++) {
            var frame = rainbow.step(2, true);
            check(frame.colorIndex() == i, "rainbow color order");
            check(frame.points().stream().map(AlbumAura.Point::color).distinct().count() == 1, "one color per circle");
            colors.add(frame.points().getFirst().color());
            rainbow.step(AlbumAura.CYCLE_TICKS - 2, true);
        }
        check(colors.size() == 7, "all seven colors");
        check(rainbow.frame().colorIndex() == 0, "red repeats after violet");
        check(!rainbow.frame().idle(), "walking never becomes idle");
        check(rainbow.step(30, true).cycleTick() == 30, "walking advances circle");
        check(rainbow.step(2, true).cycleTick() == 32, "continuous motion does not reset phase");
        rainbow.step(AlbumAura.CYCLE_TICKS - 32, false);
        check(rainbow.frame().completedStillCycles() == 0, "partial stationary cycle not counted");
        int idleStart = AlbumAura.IDLE_CYCLES * AlbumAura.CYCLE_TICKS;
        check(!rainbow.step(idleStart - 1, false).idle(), "seven full circles required after partial circle");
        check(rainbow.step(1, false).idle(), "seventh complete circle triggers wings");
        close(rainbow.frame().unfold(), 0, "wings begin folded");
        close(rainbow.step(20, false).unfold(), .5, "wings smoothly spread");
        close(rainbow.step(20, false).unfold(), 1, "wings fully spread after two seconds");
        check(rainbow.step(10000, false).idle(), "idle wings persist without flickering on/off");
        check(!rainbow.step(2, true).idle(), "movement immediately cancels wings");
        check(rainbow.frame().completedStillCycles() == 0 && rainbow.frame().cycleTick() == 2, "movement restarts stationary qualification and circle");

        int previousCount = 0;
        for (int tier = 1; tier <= 6; tier++) {
            AlbumAura.State state = new AlbumAura.State(tier);
            check(state.tier() == tier, "tier retained");
            check(!state.step(idleStart - 1, false).idle(), "tier does not become idle early");
            var transition = state.step(1, false);
            check(transition.idle() && transition.completedStillCycles() == 7, "every tier has seven-cycle idle trigger");
            var unfolded = state.step(40, false);
            close(unfolded.unfold(), 1, "every tier fully unfolds");
            check(unfolded.points().size() > previousCount, "higher-tier detail increases");
            previousCount = unfolded.points().size();
            if (tier >= 5) check(unfolded.points().stream().map(AlbumAura.Point::color).distinct().count() > 7,
                "rainbow wings have continuous multicolor border");
            if (tier >= 4) {
                double min = unfolded.points().stream().mapToDouble(AlbumAura.Point::right).min().orElseThrow();
                double max = unfolded.points().stream().mapToDouble(AlbumAura.Point::right).max().orElseThrow();
                close(min, -max, "symmetric wings");
            }
            state = new AlbumAura.State(tier);
            for (int tick = 0; tick < 1000; tick += 2) {
                var frame = state.step(2, false);
                check(frame.points().size() <= AlbumAura.MAX_POINTS, "bounded per-frame particle count");
                if (!frame.idle()) check(frame.points().size() <= 17, "sparse normal circle budget");
                for (var point : frame.points()) {
                    check(Double.isFinite(point.right()) && Double.isFinite(point.up()) && Double.isFinite(point.back()), "finite geometry");
                    check(point.size() > 0 && point.size() <= 1, "restrained particle size");
                    if (!frame.idle()) {
                        close(point.up(), AlbumAura.CIRCLE_HEIGHT, "normal animation is above head");
                        check(Math.hypot(point.right(), point.back()) <= .49, "head circle stays below half-block radius");
                    }
                }
            }
        }
        AlbumAura.State coarse = new AlbumAura.State(5), fine = new AlbumAura.State(5);
        int fullyUnfoldedAt = idleStart + AlbumAura.UNFOLD_TICKS;
        coarse.step(fullyUnfoldedAt, false);
        for (int i = 0; i < fullyUnfoldedAt / 2; i++) fine.step(2, false);
        check(coarse.frame().equals(fine.frame()), "batch advancement matches two-tick updates");
        var immutable = fine.frame();
        try { immutable.points().clear(); throw new AssertionError("mutable frame"); }
        catch (UnsupportedOperationException expected) { checks++; }
        System.out.println("AlbumAuraTest PASS: " + checks + " checks");
    }
}

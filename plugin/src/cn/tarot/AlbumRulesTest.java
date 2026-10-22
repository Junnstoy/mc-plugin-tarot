package cn.tarot;

import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** Run without Bukkit: javac AlbumRules.java AlbumRulesTest.java; java cn.tarot.AlbumRulesTest. */
public final class AlbumRulesTest {
    private static int checks;
    private static void check(boolean result, String message) {
        checks++;
        if (!result) throw new AssertionError(message);
    }
    private static void close(double actual, double expected, String message) {
        check(Math.abs(actual - expected) < 1e-10, message + ": " + actual + " != " + expected);
    }
    private static void invalid(Runnable action) {
        checks++;
        try { action.run(); throw new AssertionError("expected IllegalArgumentException"); }
        catch (IllegalArgumentException expected) { }
    }
    private static double amount(BitSet contents, String attribute, boolean multiply) {
        return AlbumRules.totals(contents).stream()
            .filter(b -> b.attribute().equals(attribute) && b.multiply() == multiply)
            .mapToDouble(AlbumRules.Bonus::amount).sum();
    }
    private static BitSet first(int count) {
        BitSet contents = new BitSet();
        contents.set(0, count);
        return contents;
    }
    public static void main(String[] args) {
        BitSet contents = new BitSet();
        check(AlbumRules.count(contents) == 0, "empty count");
        check(AlbumRules.auraTier(contents) == 0, "empty aura");
        check(AlbumRules.totals(contents).isEmpty(), "empty bonuses");
        Set<Integer> indices = new HashSet<>();
        for (int star = 1; star <= 5; star++) {
            for (int card = 1; card <= 22; card++) {
                int index = AlbumRules.index(card, star);
                check(index == (star - 1) * 22 + card - 1, "stable index");
                check(indices.add(index), "distinct indices");
                check(!AlbumRules.contains(contents, card, star), "not yet present");
                check(AlbumRules.add(contents, card, star), "first add");
                check(!AlbumRules.add(contents, card, star), "duplicate add");
                check(AlbumRules.contains(contents, card, star), "present after add");
                check(AlbumRules.count(contents) == index + 1, "one count per pair");
                check(AlbumRules.highestStar(contents, card) == star, "highest star");
            }
            check(AlbumRules.countAtStar(contents, star) == 22, "complete star");
            check(AlbumRules.completedStars(contents).size() == star, "independent star completions");
            check(AlbumRules.auraTier(contents) == (star == 5 ? 6 : star), "aura progression");
        }
        check(AlbumRules.count(contents) == 110, "full count");
        check(AlbumRules.completedStars(contents).equals(List.of(1, 2, 3, 4, 5)), "all complete stars");
        for (int bad : new int[] {-1, 0, 23, Integer.MAX_VALUE}) {
            invalid(() -> AlbumRules.index(bad, 1));
            invalid(() -> AlbumRules.highestStar(contents, bad));
            invalid(() -> AlbumRules.add(contents, bad, 1));
        }
        for (int bad : new int[] {-1, 0, 6, Integer.MAX_VALUE}) {
            invalid(() -> AlbumRules.index(1, bad));
            invalid(() -> AlbumRules.countAtStar(contents, bad));
            invalid(() -> AlbumRules.add(contents, 1, bad));
        }
        BitSet corrupt = (BitSet) contents.clone();
        corrupt.set(110);
        invalid(() -> AlbumRules.count(corrupt));
        invalid(() -> AlbumRules.totals(corrupt));

        int unlocked = 0;
        for (AlbumRules.Milestone m : AlbumRules.MILESTONES) {
            check(AlbumRules.unlockedMilestones(first(m.required() - 1)).size() == unlocked, "before milestone");
            unlocked++;
            check(AlbumRules.unlockedMilestones(first(m.required())).size() == unlocked, "at milestone");
            if (m.required() < 110)
                check(AlbumRules.unlockedMilestones(first(m.required() + 1)).size() == unlocked, "after milestone");
        }
        Set<Integer> bondCoverage = new HashSet<>();
        for (AlbumRules.Bond bond : AlbumRules.BONDS) {
            BitSet bondContents = new BitSet();
            check(AlbumRules.bondRank(bondContents, bond) == 0, "empty bond");
            for (int card : bond.cards()) {
                check(bondCoverage.add(card), "bond partition distinct cards");
                AlbumRules.add(bondContents, card, 5);
            }
            check(AlbumRules.bondRank(bondContents, bond) == 5, "only highest star needed for rank 5");
            int card = bond.cards().getFirst();
            AlbumRules.remove(bondContents, card, 5);
            check(AlbumRules.bondRank(bondContents, bond) == 0, "one missing card disables bond");
            check(AlbumRules.bondBonuses(bondContents, bond).isEmpty(), "disabled bond has no bonuses");
            for (int rank = 1; rank <= 5; rank++) {
                AlbumRules.add(bondContents, card, rank);
                check(AlbumRules.bondRank(bondContents, bond) == rank, "minimum highest star defines bond rank");
                List<AlbumRules.Bonus> bonuses = AlbumRules.bondBonuses(bondContents, bond);
                for (int n = 0; n < bonuses.size(); n++)
                    close(bonuses.get(n).amount(), bond.perStarBonuses().get(n).amount() * rank, "rank does not cumulatively stack");
            }
            AlbumRules.remove(bondContents, card, 5);
            check(AlbumRules.bondRank(bondContents, bond) == 4, "withdraw highest falls back to stored lower star");
            AlbumRules.remove(bondContents, card, 1);
            check(AlbumRules.bondRank(bondContents, bond) == 4, "withdraw lower preserves higher bond rank");
        }
        check(bondCoverage.size() == 22, "all cards represented in bonds");
        close(amount(contents, "max_health", false), 3, "full health balanced total");
        close(amount(contents, "armor", false), 1.5, "full armor balanced total");
        close(amount(contents, "attack_damage", false), 1, "full damage balanced total");
        close(amount(contents, "movement_speed", true), .045, "full speed balanced total");
        close(amount(contents, "luck", false), 1, "full luck balanced total");

        BitSet onlyFive = new BitSet();
        for (int card = 1; card <= 22; card++) AlbumRules.add(onlyFive, card, 5);
        check(AlbumRules.completedStars(onlyFive).equals(List.of(5)), "higher star never grants lower collection");
        check(AlbumRules.auraTier(onlyFive) == 5, "star 5 distinct from 110 completion");
        check(AlbumRules.remove(onlyFive, 22, 5), "withdraw present card");
        check(!AlbumRules.remove(onlyFive, 22, 5), "withdraw absent card");
        check(AlbumRules.auraTier(onlyFive) == 0, "incomplete star loses aura immediately");

        BitSet saved = (BitSet) contents.clone();
        AlbumRules.remove(contents, 22, 5);
        check(AlbumRules.count(contents) == 109, "withdraw loses count");
        check(AlbumRules.auraTier(contents) == 4, "withdraw loses full aura and incomplete 5 star");
        close(amount(contents, "luck", false), .5, "withdraw loses full milestone");
        close(amount(contents, "movement_speed", true), .04, "withdraw highest cosmos card downgrades bond");
        AlbumRules.add(contents, 22, 5);
        check(contents.equals(saved), "restore content restores state");
        check(AlbumRules.auraTier(contents) == 6, "restore full aura");
        AlbumRules.rewardLines(contents);
        AlbumRules.totals(contents);
        check(contents.equals(saved), "queries never mutate contents");
        check(AlbumRules.bonusText(new AlbumRules.Bonus("movement_speed", "移动速度", .005, true))
            .equals("移动速度 +0.5%"), "fraction formatting");
        check(AlbumRules.rewardLines(contents).stream().anyMatch(s -> s.contains("110/110")), "full progress visible");
        System.out.println("PASS: " + checks + " album rules assertions: 110 distinct slots, milestone boundaries, 6 bonds, withdrawals, 6 aura tiers");
    }
}

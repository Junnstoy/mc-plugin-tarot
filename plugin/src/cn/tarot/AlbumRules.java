package cn.tarot;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** Current album contents only: no permanent unlocks and no server API dependencies. */
public final class AlbumRules {
    private AlbumRules() {}
    public static final int CARD_COUNT = 22;
    public static final int STAR_COUNT = 5;
    public static final int SLOT_COUNT = CARD_COUNT * STAR_COUNT;

    /** multiply=true means an additive fraction of the base attribute (ADD_SCALAR). */
    public record Bonus(String attribute, String label, double amount, boolean multiply) {
        public Bonus {
            Objects.requireNonNull(attribute);
            Objects.requireNonNull(label);
            if (!Double.isFinite(amount) || amount < 0) throw new IllegalArgumentException("bonus amount");
        }
    }
    public record Milestone(int required, String name, List<Bonus> bonuses) {
        public Milestone {
            if (required < 1 || required > SLOT_COUNT) throw new IllegalArgumentException("milestone count");
            Objects.requireNonNull(name);
            bonuses = List.copyOf(bonuses);
        }
    }
    public record Bond(String key, String name, List<Integer> cards, List<Bonus> perStarBonuses) {
        public Bond {
            Objects.requireNonNull(key);
            Objects.requireNonNull(name);
            cards = List.copyOf(cards);
            perStarBonuses = List.copyOf(perStarBonuses);
            if (cards.isEmpty() || cards.stream().distinct().count() != cards.size())
                throw new IllegalArgumentException("bond cards");
            for (int card : cards) checkCard(card);
        }
    }

    public static final List<Milestone> MILESTONES = List.of(
        new Milestone(10, "初识塔罗", List.of(add("max_health", "生命上限（点）", 1))),
        new Milestone(22, "踏遍旅途", List.of(add("armor", "护甲", .5))),
        new Milestone(44, "双重回响", List.of(add("attack_damage", "攻击伤害", .5))),
        new Milestone(66, "三重祝福", List.of(add("max_health", "生命上限（点）", 1))),
        new Milestone(88, "四象流转", List.of(mult("movement_speed", "移动速度", .02))),
        new Milestone(110, "万象齐聚", List.of(add("luck", "幸运", .5)))
    );
    public static final List<Bond> BONDS = List.of(
        new Bond("heart", "心之守护", List.of(4, 5, 7),
            List.of(add("max_health", "生命上限（点）", .2))),
        new Bond("guidance", "旅途启示", List.of(1, 2, 3, 6),
            List.of(add("luck", "幸运", .1))),
        new Bond("resolve", "勇气与裁决", List.of(8, 9, 12),
            List.of(add("attack_damage", "攻击伤害", .1))),
        new Bond("balance", "静心修行", List.of(10, 13, 15),
            List.of(add("armor", "护甲", .1))),
        new Bond("change", "命运蜕变", List.of(11, 14, 16, 17),
            List.of(add("armor", "护甲", .1))),
        new Bond("cosmos", "星辰终章", List.of(18, 19, 20, 21, 22),
            List.of(mult("movement_speed", "移动速度", .005)))
    );

    private static Bonus add(String attribute, String label, double amount) {
        return new Bonus(attribute, label, amount, false);
    }
    private static Bonus mult(String attribute, String label, double amount) {
        return new Bonus(attribute, label, amount, true);
    }
    private static void checkCard(int card) {
        if (card < 1 || card > CARD_COUNT) throw new IllegalArgumentException("card must be 1..22");
    }
    private static void checkStar(int star) {
        if (star < 1 || star > STAR_COUNT) throw new IllegalArgumentException("star must be 1..5");
    }
    public static void validate(BitSet contents) {
        Objects.requireNonNull(contents, "contents");
        if (contents.nextSetBit(SLOT_COUNT) >= 0) throw new IllegalArgumentException("album index must be 0..109");
    }
    public static int index(int card, int star) {
        checkCard(card);
        checkStar(star);
        return (star - 1) * CARD_COUNT + card - 1;
    }
    public static boolean contains(BitSet contents, int card, int star) {
        validate(contents);
        return contents.get(index(card, star));
    }
    /** Mutates contents; returns false for an already present card/star pair. */
    public static boolean add(BitSet contents, int card, int star) {
        validate(contents);
        int index = index(card, star);
        boolean changed = !contents.get(index);
        contents.set(index);
        return changed;
    }
    /** Mutates contents; withdrawing a card immediately removes its contribution. */
    public static boolean remove(BitSet contents, int card, int star) {
        validate(contents);
        int index = index(card, star);
        boolean changed = contents.get(index);
        contents.clear(index);
        return changed;
    }
    public static int count(BitSet contents) {
        validate(contents);
        return contents.cardinality();
    }
    public static int countAtStar(BitSet contents, int star) {
        validate(contents);
        checkStar(star);
        return contents.get((star - 1) * CARD_COUNT, star * CARD_COUNT).cardinality();
    }
    public static int highestStar(BitSet contents, int card) {
        validate(contents);
        checkCard(card);
        for (int star = STAR_COUNT; star > 0; star--)
            if (contents.get(index(card, star))) return star;
        return 0;
    }
    public static List<Integer> completedStars(BitSet contents) {
        validate(contents);
        List<Integer> stars = new ArrayList<>();
        for (int star = 1; star <= STAR_COUNT; star++)
            if (countAtStar(contents, star) == CARD_COUNT) stars.add(star);
        return List.copyOf(stars);
    }
    /** 0=none, 1..5=highest complete star set, 6=all 110 distinct card/star pairs. */
    public static int auraTier(BitSet contents) {
        if (count(contents) == SLOT_COUNT) return 6;
        List<Integer> stars = completedStars(contents);
        return stars.isEmpty() ? 0 : stars.getLast();
    }
    /** A bond has one rank: minimum of each required card's highest stored star. */
    public static int bondRank(BitSet contents, Bond bond) {
        validate(contents);
        Objects.requireNonNull(bond);
        int rank = STAR_COUNT;
        for (int card : bond.cards()) rank = Math.min(rank, highestStar(contents, card));
        return rank;
    }
    public static List<Milestone> unlockedMilestones(BitSet contents) {
        int count = count(contents);
        return MILESTONES.stream().filter(m -> count >= m.required()).toList();
    }
    public static List<Bonus> bondBonuses(BitSet contents, Bond bond) {
        int rank = bondRank(contents, bond);
        if (rank == 0) return List.of();
        return bond.perStarBonuses().stream()
            .map(b -> new Bonus(b.attribute(), b.label(), b.amount() * rank, b.multiply())).toList();
    }
    /** Summed bonuses for current contents. Caller applies these only while carrying an album. */
    public static List<Bonus> totals(BitSet contents) {
        validate(contents);
        Map<String, Bonus> totals = new LinkedHashMap<>();
        for (Milestone milestone : unlockedMilestones(contents))
            for (Bonus bonus : milestone.bonuses()) accumulate(totals, bonus);
        for (Bond bond : BONDS)
            for (Bonus bonus : bondBonuses(contents, bond)) accumulate(totals, bonus);
        return List.copyOf(totals.values());
    }
    private static void accumulate(Map<String, Bonus> totals, Bonus bonus) {
        String key = bonus.attribute() + ":" + bonus.multiply();
        Bonus previous = totals.get(key);
        totals.put(key, previous == null ? bonus : new Bonus(bonus.attribute(), bonus.label(),
            previous.amount() + bonus.amount(), bonus.multiply()));
    }
    public static String bonusText(Bonus bonus) {
        DecimalFormat number = new DecimalFormat("0.##", DecimalFormatSymbols.getInstance(Locale.ROOT));
        return bonus.label() + " +" + number.format(bonus.amount() * (bonus.multiply() ? 100 : 1))
            + (bonus.multiply() ? "%" : "");
    }
    public static String bonusesText(List<Bonus> bonuses) {
        return String.join("、", bonuses.stream().map(AlbumRules::bonusText).toList());
    }
    /** Plain Chinese requirement/progress text; main plugin supplies actual card display names. */
    public static List<String> rewardLines(BitSet contents) {
        int count = count(contents);
        List<String> lines = new ArrayList<>();
        lines.add("收藏 " + count + "/110；同名不同星分别计数");
        lines.add("仅携带收集册时生效；取出卡牌后重新计算");
        for (Milestone m : MILESTONES)
            lines.add((count >= m.required() ? "已达成 " : "未达成 ") + m.name()
                + "（" + Math.min(count, m.required()) + "/" + m.required() + "）：" + bonusesText(m.bonuses()));
        lines.add("羁绊等级取所需卡牌的最高收藏星级中的最低值，只计一次");
        for (Bond b : BONDS) {
            int rank = bondRank(contents, b);
            String cards = String.join("、", b.cards().stream().map(id -> "#" + id).toList());
            lines.add(b.name() + " " + rank + "/5级（" + cards + "）；每级 " + bonusesText(b.perStarBonuses()));
        }
        for (int star = 1; star <= STAR_COUNT; star++)
            lines.add(star + "星全套 " + countAtStar(contents, star) + "/22：" + star + "阶跟随粒子");
        lines.add("全星全卡 " + count + "/110：六阶彩虹跟随粒子");
        lines.add("幸运不会增加塔罗卡包的稀有牌概率");
        return List.copyOf(lines);
    }
}

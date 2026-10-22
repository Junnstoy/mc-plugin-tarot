package cn.tarot;

import java.util.List;
import java.util.Locale;
import java.util.Set;

public final class TarotCommandTest {
    private static final List<String> NAMES = List.of("愚者", "魔术师", "女祭司", "女皇", "皇帝", "教皇",
        "恋人", "战车", "力量", "隐者", "命运之轮", "正义", "倒吊人", "死神", "节制", "恶魔", "高塔", "星星",
        "月亮", "太阳", "审判", "世界");
    private static int checks;
    private static void check(boolean condition) {
        checks++;
        if (!condition) throw new AssertionError("command check " + checks);
    }
    private static TarotCommand.Request parse(String command) {
        return TarotCommand.parse(command.isEmpty() ? new String[0] : command.split(" "), NAMES);
    }
    private static void invalid(String command, String messagePart) {
        try {
            parse(command);
            throw new AssertionError("accepted invalid command: " + command);
        } catch (IllegalArgumentException e) {
            check(e.getMessage().contains(messagePart));
        }
    }
    private static List<String> complete(boolean admin, String... args) {
        return TarotCommand.complete(args, NAMES, List.of("Alice", "Bob"), admin);
    }
    private static List<String> permitted(Set<String> grants, String... args) {
        return TarotCommand.complete(args, NAMES, List.of("Alice", "Bob"), grants::contains);
    }

    public static void main(String[] ignored) {
        check(parse("").kind() == TarotCommand.Kind.HELP);
        String[] simple = {"help", "status", "migrate", "album", "collection", "aura", "selftest"};
        for (String name : simple) {
            check(parse(name).kind() == TarotCommand.Kind.valueOf(name.toUpperCase(Locale.ROOT)));
            check(parse(name.toUpperCase(Locale.ROOT)).kind() == parse(name).kind());
            invalid(name + " extra", "额外参数");
        }
        for (int card = 1; card <= 22; card++) for (int level = 1; level <= 5; level++) {
            TarotCommand.Request byId = parse("give Alice card " + card + " " + level + " 2");
            TarotCommand.Request byName = parse("give Alice card " + NAMES.get(card - 1) + " " + level + " 2");
            check(byId.equals(byName));
            check(byId.kind() == TarotCommand.Kind.GIVE && byId.player().equals("Alice")
                && byId.item().equals("card") && byId.card() == card && byId.level() == level && byId.count() == 2);
        }
        TarotCommand.Request all = parse("GiVe Alice CaRd ALL 5 64");
        check(all.card() == 0 && all.level() == 5 && all.count() == 64 && all.player().equals("Alice"));
        for (String item : List.of("pack", "box", "case")) {
            TarotCommand.Request req = parse("give Bob " + item + " 64");
            check(req.kind() == TarotCommand.Kind.GIVE && req.item().equals(item) && req.count() == 64
                && req.card() == 0 && req.level() == 0);
            invalid("give Bob " + item, "用法");
            invalid("give Bob " + item + " 1 ignored", "用法");
        }
        check(parse("give Bob ALBUM 1").item().equals("album"));
        invalid("give Bob album 2", "唯一");
        check(parse("reset CD Alice").kind() == TarotCommand.Kind.RESET_CD);
        check(parse("RESET Buffer Bob").kind() == TarotCommand.Kind.RESET_BUFFER);
        check(parse("reset buffer Bob").player().equals("Bob"));
        invalid("reset cd", "用法");
        invalid("reset cd Bob extra", "用法");
        invalid("reset buff Bob", "cd 或 buffer");
        invalid("reset Bob cd", "cd 或 buffer");
        invalid("card Bob 1 1", "已移入 give");
        invalid("clear Bob", "reset cd");
        invalid("something", "未知指令");
        invalid("give", "用法");
        invalid("give Bob", "用法");
        invalid("give Bob unknown 1", "物品类型");
        invalid("give Bob card 1 1", "用法");
        invalid("give Bob card 1 1 1 extra", "用法");
        for (String bad : List.of("0", "23", "-1", "世界号", "2147483648", "99999999999999999", "1.0", "+1", "１"))
            invalid("give Bob card " + bad + " 1 1", "卡牌种类");
        for (String bad : List.of("0", "6", "-1", "NaN", "2147483648", "1.5", "+1", "１"))
            invalid("give Bob card 1 " + bad + " 1", "卡牌等级");
        for (String bad : List.of("0", "65", "-1", "NaN", "2147483648", "1.5", "+1", "１")) {
            invalid("give Bob card 1 1 " + bad, "数量");
            invalid("give Bob pack " + bad, "数量");
        }
        check(parse("give Bob card 01 01 01").card() == 1);
        try {
            TarotCommand.parse(new String[]{"give", "A B", "pack", "1"}, NAMES);
            throw new AssertionError("whitespace argument accepted");
        } catch (IllegalArgumentException e) { check(e.getMessage().contains("空格")); }
        try {
            TarotCommand.parse(new String[]{"give", "", "pack", "1"}, NAMES);
            throw new AssertionError("blank player accepted");
        } catch (IllegalArgumentException e) { check(e.getMessage().contains("为空")); }
        check(!complete(false, "").contains("give"));
        check(!complete(false, "").contains("reset"));
        check(complete(true, "").containsAll(List.of("help", "give", "reset", "selftest")));
        check(!complete(true, "").contains("card") && !complete(true, "").contains("clear"));
        check(complete(true, "give", "a").equals(List.of("Alice")));
        check(complete(true, "give", "Alice", "ca").equals(List.of("card", "case")));
        check(complete(true, "give", "Alice", "card", "世").equals(List.of("世界")));
        check(complete(true, "give", "Alice", "card", "AL").equals(List.of("all")));
        check(complete(true, "give", "Alice", "card", "22", "").equals(List.of("1", "2", "3", "4", "5")));
        check(complete(true, "give", "Alice", "card", "22", "5", "6").equals(List.of("64")));
        check(complete(true, "give", "Alice", "album", "").equals(List.of("1")));
        check(complete(true, "reset", "").equals(List.of("cd", "buffer")));
        check(complete(true, "reset", "buffer", "b").equals(List.of("Bob")));
        check(complete(true, "reset", "bad", "").isEmpty());
        check(complete(true, "album", "").isEmpty());
        check(complete(true, "help", "").isEmpty());
        check(complete(false, "give", "").isEmpty());
        check(complete(true, "give", "Alice", "card", "1", "1", "1", "").isEmpty());
        check(permitted(Set.of(), "").isEmpty());
        check(permitted(Set.of("tarot.command.album"), "").equals(List.of("album")));
        check(permitted(Set.of("tarot.command.give"), "").equals(List.of("give")));
        check(permitted(Set.of("tarot.command.give"), "give", "").equals(List.of("Alice", "Bob")));
        check(permitted(Set.of("tarot.command.status"), "give", "").isEmpty());
        check(permitted(Set.of("tarot.command.reset.cd"), "").equals(List.of("reset")));
        check(permitted(Set.of("tarot.command.reset.cd"), "reset", "").equals(List.of("cd")));
        check(permitted(Set.of("tarot.command.reset.cd"), "reset", "cd", "").equals(List.of("Alice", "Bob")));
        check(permitted(Set.of("tarot.command.reset.cd"), "reset", "buffer", "").isEmpty());
        check(permitted(Set.of("tarot.command.reset.buffer"), "reset", "").equals(List.of("buffer")));
        check(permitted(Set.of("tarot.command.reset.buffer"), "reset", "cd", "").isEmpty());
        check(permitted(Set.of("tarot.command.reset"), "reset", "").equals(List.of("cd", "buffer")));
        check(permitted(Set.of("tarot.admin"), "").equals(complete(true, "")));
        check(permitted(Set.of("tarot.bypass.cd"), "").isEmpty());
        check(permitted(Set.of("tarot.command.reset.cd"), "RESET", "CD", "a").equals(List.of("Alice")));
        Locale previous = Locale.getDefault();
        try {
            Locale.setDefault(Locale.forLanguageTag("tr-TR"));
            check(parse("GIVE Alice CARD ALL 1 1").kind() == TarotCommand.Kind.GIVE);
            check(complete(true, "G").equals(List.of("give")));
        } finally { Locale.setDefault(previous); }
        System.out.println("TAROT COMMAND TEST PASS: " + checks
            + " checks; 110 card/name variants, strict syntax, all quantities, migration hints and permission-aware completions");
    }
}

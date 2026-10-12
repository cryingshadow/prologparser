package prologparser;

import java.util.regex.*;

public enum TokenType {

    CLOSE_BRACKET(Pattern.compile("\\]")),
    CLOSE_COMMENT(Pattern.compile("\\*/")),
    CLOSE_CURLY(Pattern.compile("\\}")),
    CLOSE_PARANTHESIS(Pattern.compile("\\)")),
    COMMA(Pattern.compile(",")),
    COMMENT(null),
    CUT(Pattern.compile("!")),
    EMPTY_LIST(Pattern.compile("\\[(\\s|/\\*.*\\*/)*\\]")),
    FLOAT(Pattern.compile("")),
    FULLSTOP(Pattern.compile("\\.")),
    INFINITY(Pattern.compile("inf")),
    INT(Pattern.compile("")),
    NAME(Pattern.compile("\\{(\\s|/\\*.*\\*/)*\\}")),
    NAN(Pattern.compile("nan")),
    NEWLINE(Pattern.compile("\\n\\r?|\\r")),
    OPEN_BRACKET(Pattern.compile("\\[")),
    OPEN_COMMENT(Pattern.compile("/\\*")),
    OPEN_CURLY(Pattern.compile("\\{")),
    OPEN_PARANTHESIS(Pattern.compile("\\(")),
    PERCENT(Pattern.compile("%")),
    PIPE(Pattern.compile("\\|")),
    SEMICOLON(Pattern.compile(";")),
    STRING(Pattern.compile("\"([^\"\\\\]|\"\"|\\\\([abdefnrtv]|x(\\w|_)+\\\\|o\\d+\\\\|\\n|\\r|\\\\|\\^|`|'|\"))*\"")),
    VARIABLE(Pattern.compile("([A-Z]|_)\\w*")),
    WHITESPACE(Pattern.compile("(\\s&&[^\\n\\r])+"));

    public final Pattern regex;

    private TokenType(final Pattern regex) {
        this.regex = regex;
    }

}

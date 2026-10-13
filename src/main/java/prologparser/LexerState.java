package prologparser;

public enum LexerState {

    BACKQUOTE,
    BACKQUOTE_END,
    BASE_PREFIX,
    COMMENT,
    COMMENT_STAR,
    DIGIT,
    DIGITS,
    DIGITS_FULLSTOP,
    ESCAPE,
    FLOAT,
    I,
    IN,
    INF,
    INITIAL,
    INT,
    LINE_COMMENT,
    N,
    NA,
    NAME,
    NAN,
    OPEN_BRACKET,
    OPEN_CURLY,
    QUOTE,
    QUOTE_END,
    SLASH,
    STRING,
    STRING_END,
    VARIABLE,
    WHITESPACE

}

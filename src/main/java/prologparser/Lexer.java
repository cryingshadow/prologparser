package prologparser;

import java.io.*;
import java.util.*;
import java.util.function.*;

public class Lexer {

    private static final Set<Character> SYMBOLS =
            Set.of('+', '-', '^', '<', '>', '=', '~', ':', '?', '@', '#', '$', '&', '`', '*', '.', '/', '\\');

    private static boolean isAlpha(final char c) {
        return Character.isLetter(c) || Character.isDigit(c) || c == '_';
    }

    private static boolean isLF(final char c) {
        return c == '\n' || c == '\r';
    }

    private final char[] buffer;

    private int count;

    private int from;

    private int index;

    private final Reader reader;

    private LexerState state;

    private final List<PrologToken> tokens;

    public Lexer(final Reader reader) {
        this.reader = reader;
        this.state = LexerState.INITIAL;
        this.from = 0;
        this.index = 0;
        this.count = 0;
        this.buffer = new char[1024];
        this.tokens = new LinkedList<PrologToken>();
    }

    public List<PrologToken> tokenize() throws IOException {
        int read = this.reader.read(this.buffer, 0, this.buffer.length);
        while (read >= 0) {
            while (this.index < read) {
                switch (this.state) {
                case BACKQUOTE:
                    this.backquote();
                    break;
                case BACKQUOTE_END:
                    this.backquoteEnd();
                    break;
                case BACKQUOTE_ESCAPE:
                    this.backquoteEscape();
                    break;
                case BACKQUOTE_ESCAPE_DIGIT:
                    this.backquoteEscapeDigit();
                    break;
                case BACKQUOTE_ESCAPE_ALPHA:
                    this.backquoteEscapeAlpha();
                    break;
                case BASE_PREFIX:
                    this.basePrefix();
                    break;
                case COMMENT:
                    this.comment();
                    break;
                case COMMENT_STAR:
                    this.commentStar();
                    break;
                case DIGIT:
                    this.digit();
                    break;
                case DIGITS:
                    this.digits();
                    break;
                case DIGITS_FULLSTOP:
                    this.digitsFullstop();
                    break;
                case INT_ESCAPE_ALPHA:
                    this.intEscapeAlpha();
                    break;
                case INT_ESCAPE_DIGIT:
                    this.intEscapeDigit();
                    break;
                case FLOAT:
                    break;
                case I:
                    this.letterI();
                    break;
                case IN:
                    this.lettersIN();
                    break;
                case INF:
                    this.inf();
                    break;
                case INITIAL:
                    this.initial();
                    break;
                case INT:
                    this.integer();
                    break;
                case LINE_COMMENT:
                    this.lineComment();
                    break;
                case N:
                    this.letterN();
                    break;
                case NA:
                    this.lettersNA();
                    break;
                case NAME:
                    this.name();
                    break;
                case NAME_SLASH:
                    this.nameSlash();
                    break;
                case NAN:
                    this.nan();
                    break;
                case OPEN_BRACKET:
                    this.openBracket();
                    break;
                case OPEN_BRACKET_COMMENT:
                    this.openBracketComment();
                    break;
                case OPEN_BRACKET_LINE_COMMENT:
                    this.openBracketLineComment();
                    break;
                case OPEN_BRACKET_SLASH:
                    this.openBracketSlash();
                    break;
                case OPEN_BRACKET_WHITESPACE:
                    this.openBracketWhitespace();
                    break;
                case OPEN_CURLY:
                    this.openCurly();
                    break;
                case OPEN_CURLY_COMMENT:
                    this.openCurlyComment();
                    break;
                case OPEN_CURLY_LINE_COMMENT:
                    this.openCurlyLineComment();
                    break;
                case OPEN_CURLY_SLASH:
                    this.openCurlySlash();
                    break;
                case OPEN_CURLY_WHITESPACE:
                    this.openCurlyWhitespace();
                    break;
                case QUOTE:
                    this.quote();
                    break;
                case QUOTE_END:
                    this.quoteEnd();
                    break;
                case QUOTE_ESCAPE:
                    this.quoteEscape();
                    break;
                case QUOTE_ESCAPE_ALPHA:
                    this.quoteEscapeAlpha();
                    break;
                case QUOTE_ESCAPE_DIGIT:
                    this.quoteEscapeDigit();
                    break;
                case SLASH:
                    this.slash();
                    break;
                case STRING:
                    this.string();
                    break;
                case STRING_END:
                    this.stringEnd();
                    break;
                case STRING_ESCAPE:
                    this.stringEscape();
                    break;
                case STRING_ESCAPE_ALPHA:
                    this.stringEscapeAlpha();
                    break;
                case STRING_ESCAPE_DIGIT:
                    this.stringEscapeDigit();
                    break;
                case VARIABLE:
                    this.variable();
                    break;
                case WHITESPACE:
                    this.whitespace();
                    break;
                default:
                    throw new IllegalStateException("Lexer reached unknown state!");
                }
                this.index++;
                this.count++;
            }
            if (this.from < read) {
                final int diff = read - this.from;
                System.arraycopy(this.buffer, this.from, this.buffer, 0, diff);
                read = this.reader.read(this.buffer, diff, this.buffer.length - diff) + diff;
                this.index -= this.from;
            } else {
                read = this.reader.read(this.buffer, 0, this.buffer.length);
                this.index = 0;
            }
            this.from = 0;
        }
        if (this.index > 0) {
            final String remainingText = new String(this.buffer, 0, this.index);
            switch (this.state) {
            case BACKQUOTE_END:
            case I:
            case IN:
            case N:
            case NA:
            case NAME:
            case NAME_SLASH:
            case QUOTE_END:
            case SLASH:
                this.tokens.add(new PrologToken(remainingText, TokenType.NAME));
                break;
            case DIGIT:
            case DIGITS:
            case INT:
                this.tokens.add(new PrologToken(remainingText, TokenType.INT));
                break;
            case DIGITS_FULLSTOP:
                this.tokens.add(new PrologToken(new String(this.buffer, 0, this.index - 1), TokenType.INT));
                this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
                break;
            case FLOAT:
                this.tokens.add(new PrologToken(remainingText, TokenType.FLOAT));
                break;
            case INF:
                this.tokens.add(new PrologToken(remainingText, TokenType.INFINITY));
                break;
            case LINE_COMMENT:
                this.tokens.add(new PrologToken(remainingText, TokenType.COMMENT));
                break;
            case NAN:
                this.tokens.add(new PrologToken(remainingText, TokenType.NAN));
                break;
            case OPEN_BRACKET:
                this.tokens.add(new PrologToken(remainingText, TokenType.OPEN_BRACKET));
                break;
            case OPEN_BRACKET_LINE_COMMENT:
                this.tokens.add(new PrologToken("[", TokenType.OPEN_BRACKET));
                this.tokens.add(new PrologToken(new String(this.buffer, 1, this.index - 1), TokenType.COMMENT));
                break;
            case OPEN_BRACKET_SLASH:
                this.tokens.add(new PrologToken("[", TokenType.OPEN_BRACKET));
                this.tokens.add(new PrologToken("/", TokenType.NAME));
                break;
            case OPEN_BRACKET_WHITESPACE:
                this.tokens.add(new PrologToken("[", TokenType.OPEN_BRACKET));
                this.tokens.add(new PrologToken(new String(this.buffer, 1, this.index - 1), TokenType.WHITESPACE));
                break;
            case OPEN_CURLY:
                this.tokens.add(new PrologToken(remainingText, TokenType.OPEN_CURLY));
                break;
            case OPEN_CURLY_LINE_COMMENT:
                this.tokens.add(new PrologToken("{", TokenType.OPEN_CURLY));
                this.tokens.add(new PrologToken(new String(this.buffer, 1, this.index - 1), TokenType.COMMENT));
                break;
            case OPEN_CURLY_SLASH:
                this.tokens.add(new PrologToken("{", TokenType.OPEN_CURLY));
                this.tokens.add(new PrologToken("/", TokenType.NAME));
                break;
            case OPEN_CURLY_WHITESPACE:
                this.tokens.add(new PrologToken("{", TokenType.OPEN_CURLY));
                this.tokens.add(new PrologToken(new String(this.buffer, 1, this.index - 1), TokenType.WHITESPACE));
                break;
            case STRING_END:
                this.tokens.add(new PrologToken(remainingText, TokenType.STRING));
                break;
            case VARIABLE:
                this.tokens.add(new PrologToken(remainingText, TokenType.VARIABLE));
                break;
            case WHITESPACE:
                this.tokens.add(new PrologToken(remainingText, TokenType.WHITESPACE));
                break;
            default:
                throw new IOException("File ended unexpectedly!");
            }
        }
        return this.tokens;
    }

    private void backquote() {
        if (this.buffer[this.index] == '`') {
            this.state = LexerState.BACKQUOTE_END;
        } else if (this.buffer[this.index] == '\\') {
            this.state = LexerState.BACKQUOTE_ESCAPE;
        }
    }

    private void backquoteEnd() throws IOException {
        this.newToken(TokenType.NAME, c -> c == '`', _ -> LexerState.BACKQUOTE);
    }

    private void backquoteEscape() throws IOException {
        this.escape(LexerState.BACKQUOTE, LexerState.BACKQUOTE_ESCAPE_DIGIT, LexerState.BACKQUOTE_ESCAPE_ALPHA, null);
    }

    private void backquoteEscapeAlpha() throws IOException {
        this.escape(LexerState.BACKQUOTE, true);
    }

    private void backquoteEscapeDigit() throws IOException {
        this.escape(LexerState.BACKQUOTE, false);
    }

    private void basePrefix() throws IOException {
        switch (this.buffer[this.index]) {
        case '\\':
            this.escape(LexerState.INITIAL, LexerState.INT_ESCAPE_DIGIT, LexerState.INT_ESCAPE_ALPHA, TokenType.INT);
            break;
        default:
            if (
                Character.isLetter(this.buffer[this.index])
                || Character.isDigit(this.buffer[this.index])
                || this.buffer[this.index] == '_'
            ) {
                this.state = LexerState.INT;
            } else {
                throw new IOException(
                    "Unexpected character " + this.buffer[this.index] + " at position " + this.count + "!"
                );
            }
        }
    }

    private void comment() {
        if (this.buffer[this.index] == '*') {
            this.state = LexerState.COMMENT_STAR;
        }
    }

    private void commentStar() {
        switch (this.buffer[this.index]) {
        case '/':
            this.tokens.add(
                new PrologToken(
                    new String(this.buffer, this.from, this.index - this.from + 1),
                    TokenType.COMMENT
                )
            );
            this.updateFrom(1);
            this.state = LexerState.INITIAL;
            break;
        case '*':
            // do nothing
            break;
        default:
            this.state = LexerState.COMMENT;
        }
    }

    private void digit() throws IOException {
        this.newToken(
            TokenType.INT,
            c -> {
                switch (c) {
                case 'b':
                case 'o':
                case 'x':
                case '\'':
                case '.':
                    return true;
                default:
                    return Character.isDigit(c);
                }
            },
            c -> {
                switch (c) {
                case 'b':
                case 'o':
                case 'x':
                case '\'':
                    return LexerState.BASE_PREFIX;
                case '.':
                    return LexerState.DIGITS_FULLSTOP;
                default:
                    return LexerState.DIGITS;
                }
            }
        );
    }

    private void digits() throws IOException {
        this.newToken(
            TokenType.INT,
            c -> {
                switch (c) {
                case '\'':
                case '.':
                    return true;
                default:
                    return Character.isDigit(c);
                }
            },
            c -> {
                switch (c) {
                case '\'':
                    return LexerState.BASE_PREFIX;
                case '.':
                    return LexerState.DIGITS_FULLSTOP;
                default:
                    return LexerState.DIGITS;
                }
            }
        );
    }

    private void digitsFullstop() throws IOException {
        this.newToken(
            List.of(
                new PrologToken(
                    new String(this.buffer, this.from, this.index - this.from - 1),
                    TokenType.INT
                ),
                new PrologToken(".", TokenType.FULLSTOP)
            ),
            Character::isDigit,
            _ -> LexerState.FLOAT
        );
    }

    private void escape(final LexerState callback, final boolean letter) throws IOException {
        if (this.buffer[this.index] == '\\') {
            this.state = callback;
        } else if (
            (!letter || (!Character.isLetter(this.buffer[this.index]) && this.buffer[this.index] != '-'))
            && !Character.isDigit(this.buffer[this.index])
        ) {
            throw new IOException(
                "Unexpected character " + this.buffer[this.index] + " at position " + this.count + "!"
            );
        }
    }

    private void escape(
        final LexerState callback,
        final LexerState digit,
        final LexerState alpha,
        final TokenType type
    ) throws IOException {
        switch (this.buffer[this.index]) {
        case '\n':
        case '\r':
        case '\\':
        case '\'':
        case '\"':
        case '`':
        case '^':
        case 'b':
        case 't':
        case 'n':
        case 'v':
        case 'f':
        case 'r':
        case 'e':
        case 'd':
        case 'a':
            if (type != null) {
                this.tokens.add(new PrologToken(new String(this.buffer, this.from, this.index - this.from + 1), type));
            }
            this.state = callback;
            break;
        case 'o':
            this.state = digit;
            break;
        case 'x':
            this.state = alpha;
            break;
        default:
            throw new IOException(
                "Unexpected character " + this.buffer[this.index] + " at position " + this.count + "!"
            );
        }
    }

    private void inf() throws IOException {
        this.newToken(TokenType.INFINITY, Lexer::isAlpha, _ -> LexerState.NAME);
    }

    private void initial() throws IOException {
        this.newToken(List.of(), _ -> false, _ -> LexerState.INITIAL);
    }

    private void integer() throws IOException {
        this.newToken(TokenType.INT, Lexer::isAlpha, _ -> LexerState.INT);
    }

    private void intEscapeAlpha() throws IOException {
        this.escape(LexerState.INT, true);
    }

    private void intEscapeDigit() throws IOException {
        this.escape(LexerState.INT, false);
    }

    private void letterI() throws IOException {
        this.newToken(TokenType.NAME, Lexer::isAlpha, c -> c == 'n' ? LexerState.IN : LexerState.NAME);
    }

    private void letterN() throws IOException {
        this.newToken(TokenType.NAME, Lexer::isAlpha, c -> c == 'a' ? LexerState.NA : LexerState.NAME);
    }

    private void lettersIN() throws IOException {
        this.newToken(TokenType.NAME, Lexer::isAlpha, c -> c == 'f' ? LexerState.INF : LexerState.NAME);
    }

    private void lettersNA() throws IOException {
        this.newToken(TokenType.NAME, Lexer::isAlpha, c -> c == 'n' ? LexerState.NAN : LexerState.NAME);
    }

    private void lineComment() {
        if (Lexer.isLF(this.buffer[this.index])) {
            this.tokens.add(
                new PrologToken(
                    new String(this.buffer, this.from, this.index - this.from + 1),
                    TokenType.COMMENT
                )
            );
            this.updateFrom(1);
            this.state = LexerState.INITIAL;
        }
    }

    private void name() throws IOException {
        this.newToken(
            TokenType.NAME,
            c -> Lexer.isAlpha(c) || Lexer.SYMBOLS.contains(c),
            c -> c == '/' ? LexerState.NAME_SLASH : LexerState.NAME
        );
    }

    private void nameSlash() throws IOException {
        this.newToken(
            TokenType.NAME,
            c -> Lexer.isAlpha(c) || Lexer.SYMBOLS.contains(c),
            c -> {
                switch (c) {
                case '*':
                    this.tokens.add(
                        new PrologToken(new String(this.buffer, this.from, this.index - this.from - 1), TokenType.NAME)
                    );
                    this.updateFrom(-1);
                    this.state = LexerState.COMMENT;
                    break;
                case '/':
                    this.state = LexerState.NAME_SLASH;
                    break;
                default:
                    this.state = LexerState.NAME;
                }
            }
        );
    }

    private void nan() throws IOException {
        this.newToken(TokenType.NAN, Lexer::isAlpha, _ -> LexerState.NAME);
    }

    private void newToken(
        final List<PrologToken> tokens,
        final Predicate<Character> special,
        final Consumer<Character> specialCode
    ) throws IOException {
        if (special.test(this.buffer[this.index])) {
            specialCode.accept(this.buffer[this.index]);
            return;
        }
        switch (this.buffer[this.index]) {
        case '`':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.BACKQUOTE;
            break;
        case '\'':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.QUOTE;
            break;
        case '\"':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.STRING;
            break;
        case '/':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.SLASH;
            break;
        case 'i':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.I;
            break;
        case 'n':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.N;
            break;
        case '%':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.LINE_COMMENT;
            break;
        case '[':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.OPEN_BRACKET;
            break;
        case ']':
            this.tokens.addAll(tokens);
            this.tokens.add(new PrologToken("]", TokenType.CLOSE_BRACKET));
            this.updateFrom(1);
            this.state = LexerState.INITIAL;
            break;
        case '{':
            this.tokens.addAll(tokens);
            this.updateFrom();
            this.state = LexerState.OPEN_CURLY;
            break;
        case '}':
            this.tokens.addAll(tokens);
            this.tokens.add(new PrologToken("}", TokenType.CLOSE_CURLY));
            this.updateFrom(1);
            this.state = LexerState.INITIAL;
            break;
        case '(':
            this.tokens.addAll(tokens);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.addAll(tokens);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.addAll(tokens);
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.addAll(tokens);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ',':
            this.tokens.addAll(tokens);
            this.tokens.add(new PrologToken(",", TokenType.COMMA));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.addAll(tokens);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        default:
            this.tokens.addAll(tokens);
            this.updateFrom();
            if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                this.state = LexerState.VARIABLE;
            } else if (Character.isDigit(this.buffer[this.index])) {
                this.state = LexerState.DIGIT;
            } else if (Character.isWhitespace(this.buffer[this.index])) {
                this.state = LexerState.WHITESPACE;
            } else if (
                Character.isLowerCase(this.buffer[this.index])
                || Lexer.SYMBOLS.contains(this.buffer[this.index])
            ) {
                this.state = LexerState.NAME;
            } else {
                throw new IOException(
                    "Unexpected character " + this.buffer[this.index] + " at position " + this.count + "!"
                );
            }
        }
    }

    private void newToken(
        final List<PrologToken> tokens,
        final Predicate<Character> special,
        final Function<Character, LexerState> specialState
    ) throws IOException {
        this.newToken(tokens, special, c -> {this.state = specialState.apply(c);});
    }

    private void newToken(
        final TokenType type,
        final Predicate<Character> special,
        final Consumer<Character> specialCode
    ) throws IOException {
        this.newToken(
            List.of(new PrologToken(new String(this.buffer, this.from, this.index - this.from), type)),
            special,
            specialCode
        );
    }

    private void newToken(
        final TokenType type,
        final Predicate<Character> special,
        final Function<Character, LexerState> specialState
    ) throws IOException {
        this.newToken(
            List.of(new PrologToken(new String(this.buffer, this.from, this.index - this.from), type)),
            special,
            specialState
        );
    }

    private void openBracket() throws IOException {
        this.newToken(
            TokenType.OPEN_BRACKET,
            c -> {
                switch (c) {
                case ']':
                case '%':
                case '/':
                    return true;
                default:
                    return Character.isWhitespace(c);
                }
            },
            c -> {
                switch (c) {
                case ']':
                    this.tokens.add(
                        new PrologToken(
                            new String(this.buffer, this.from, this.index - this.from + 1),
                            TokenType.EMPTY_LIST
                        )
                    );
                    this.updateFrom(1);
                    this.state = LexerState.INITIAL;
                    break;
                case '%':
                    this.state = LexerState.OPEN_BRACKET_LINE_COMMENT;
                    break;
                case '/':
                    this.state = LexerState.OPEN_BRACKET_SLASH;
                    break;
                default:
                    this.state = LexerState.OPEN_BRACKET_WHITESPACE;
                }
            }
        );
    }

    private void openBracketComment() {
        // TODO Auto-generated method stub

    }

    private void openBracketLineComment() {
        // TODO Auto-generated method stub

    }

    private void openBracketSlash() {
        // TODO Auto-generated method stub

    }

    private void openBracketWhitespace() {
        // TODO Auto-generated method stub

    }

    private void openCurly() throws IOException {
        this.newToken(
            TokenType.OPEN_CURLY,
            c -> {
                switch (c) {
                case '}':
                case '%':
                case '/':
                    return true;
                default:
                    return Character.isWhitespace(c);
                }
            },
            c -> {
                switch (c) {
                case '}':
                    this.tokens.add(
                        new PrologToken(
                            new String(this.buffer, this.from, this.index - this.from + 1),
                            TokenType.NAME
                        )
                    );
                    this.updateFrom(1);
                    this.state = LexerState.INITIAL;
                    break;
                case '%':
                    this.state = LexerState.OPEN_CURLY_LINE_COMMENT;
                    break;
                case '/':
                    this.state = LexerState.OPEN_CURLY_SLASH;
                    break;
                default:
                    this.state = LexerState.OPEN_CURLY_WHITESPACE;
                }
            }
        );
    }

    private void openCurlyComment() {
        // TODO Auto-generated method stub

    }

    private void openCurlyLineComment() {
        // TODO Auto-generated method stub

    }

    private void openCurlySlash() {
        // TODO Auto-generated method stub

    }

    private void openCurlyWhitespace() {
        // TODO Auto-generated method stub

    }

    private void quote() {
        if (this.buffer[this.index] == '\'') {
            this.state = LexerState.QUOTE_END;
        } else if (this.buffer[this.index] == '\\') {
            this.state = LexerState.QUOTE_ESCAPE;
        }
    }

    private void quoteEnd() throws IOException {
        this.newToken(TokenType.NAME, c -> c == '\'', _ -> LexerState.QUOTE);
    }

    private void quoteEscape() {
        // TODO Auto-generated method stub

    }

    private void quoteEscapeAlpha() {
        // TODO Auto-generated method stub

    }

    private void quoteEscapeDigit() {
        // TODO Auto-generated method stub

    }

    private void slash() throws IOException {
        this.newToken(
            TokenType.NAME,
            c -> Lexer.SYMBOLS.contains(c),
            c -> c == '*' ? LexerState.COMMENT : LexerState.NAME
        );
    }

    private void string() {
        if (this.buffer[this.index] == '\"') {
            this.state = LexerState.STRING_END;
        } else if (this.buffer[this.index] == '\\') {
            this.state = LexerState.STRING_ESCAPE;
        }
    }

    private void stringEnd() throws IOException {
        this.newToken(TokenType.STRING, c -> c == '\"', _ -> LexerState.STRING);
    }

    private void stringEscape() {
        // TODO Auto-generated method stub

    }

    private void stringEscapeAlpha() {
        // TODO Auto-generated method stub

    }

    private void stringEscapeDigit() {
        // TODO Auto-generated method stub

    }

    private void updateFrom() {
        this.updateFrom(0);
    }

    private void updateFrom(final int offset) {
        this.from = this.index + offset;
    }

    private void variable() throws IOException {
        this.newToken(TokenType.VARIABLE, Lexer::isAlpha, _ -> LexerState.VARIABLE);
    }

    private void whitespace() throws IOException {
        this.newToken(TokenType.WHITESPACE, Character::isWhitespace, _ -> LexerState.WHITESPACE);
    }

}

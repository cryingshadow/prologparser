package prologparser;

import java.io.*;
import java.util.*;

public class Lexer {

    private static final Set<Character> SYMBOLS =
            Set.of('+', '-', '^', '<', '>', '=', '~', ':', '?', '@', '#', '$', '&', '`', '*', '.', '/', '\\');

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
            for (this.index = 0; this.index < read; this.index++, this.count++) {
                switch (this.state) {
                case BASE_PREFIX:
                    this.basePrefix();
                    break;
                case BACKQUOTE:
                    this.backquote();
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
                case ESCAPE:

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
                case NAN:
                    this.nan();
                    break;
                case OPEN_BRACKET:
                    this.openBracket();
                    break;
                case OPEN_CURLY:
                    this.openCurly();
                    break;
                case QUOTE:
                    this.quote();
                    break;
                case SLASH:
                    this.slash();
                    break;
                case STRING:
                    this.string();
                    break;
                case VARIABLE:
                    this.variable();
                    break;
                case WHITESPACE:
                    this.whitespace();
                    break;
                case BACKQUOTE_END:
                    this.backquoteEnd();
                    break;
                case QUOTE_END:
                    this.quoteEnd();
                    break;
                case STRING_END:
                    this.stringEnd();
                    break;
                default:
                    throw new IllegalStateException("Lexer reached unknown state!");
                }
            }
            if (this.from < read) {
                final int diff = read - this.from;
                System.arraycopy(this.buffer, this.from, this.buffer, 0, diff);
                read = this.reader.read(this.buffer, diff, this.buffer.length - diff) + diff;
            } else {
                read = this.reader.read(this.buffer, 0, this.buffer.length);
            }
            this.from = 0;
        }
        return this.tokens;
    }

    private void backquote() {
        switch (this.buffer[this.index]) {
            case '`':
                this.state = LexerState.BACKQUOTE_END;
                break;
            case '\\':
                //TODO
                this.state = LexerState.ESCAPE;
                break;
            default:
                // do nothing
        }
    }

    private void backquoteEnd() throws IOException {
        final PrologToken token =
            new PrologToken(
                new String(this.buffer, this.from, this.index - this.from),
                TokenType.NAME
            );
        switch (this.buffer[this.index]) {
        case '`':
            this.state = LexerState.BACKQUOTE;
            break;
        case '\'':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.QUOTE;
            break;
        case '\"':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.STRING;
            break;
        case '/':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.SLASH;
            break;
        case 'i':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.I;
            break;
        case 'n':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.N;
            break;
        case '%':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.LINE_COMMENT;
            break;
        case '[':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.OPEN_BRACKET;
            break;
        case '{':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.OPEN_CURLY;
            break;
        case '(':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        default:
            this.tokens.add(token);
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
            break;
        }
    }

    private void basePrefix() {
        // TODO Auto-generated method stub
        switch (this.buffer[this.index]) {
        case '\\':
        default:
            if (
                Character.isLetter(this.buffer[this.index])
                || Character.isDigit(this.buffer[this.index])
                || this.buffer[this.index] == '_'
            ) {
                this.state = LexerState.INT;
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
        final PrologToken token =
            new PrologToken(
                new String(this.buffer, this.from, this.index - this.from),
                TokenType.INT
            );
        switch (this.buffer[this.index]) {
        case 'b':
        case 'o':
        case 'x':
        case '\'':
            this.state = LexerState.BASE_PREFIX;
            break;
        case '.':
            this.state = LexerState.DIGITS_FULLSTOP;
            break;
        case '`':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.BACKQUOTE;
            break;
        case '\"':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.STRING;
            break;
        case '/':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.SLASH;
            break;
        case 'i':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.I;
            break;
        case 'n':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.N;
            break;
        case '%':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.LINE_COMMENT;
            break;
        case '[':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.OPEN_BRACKET;
            break;
        case '{':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.OPEN_CURLY;
            break;
        case '(':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        default:
            this.tokens.add(token);
            this.updateFrom();
            if (Character.isDigit(this.buffer[this.index])) {
                this.state = LexerState.DIGITS;
            } else if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                this.state = LexerState.VARIABLE;
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
            break;
        }
    }

    private void digits() throws IOException {
        final PrologToken token =
            new PrologToken(
                new String(this.buffer, this.from, this.index - this.from),
                TokenType.INT
            );
        switch (this.buffer[this.index]) {
        case '\'':
            this.state = LexerState.BASE_PREFIX;
            break;
        case '.':
            this.state = LexerState.DIGITS_FULLSTOP;
            break;
        case '`':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.BACKQUOTE;
            break;
        case '\"':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.STRING;
            break;
        case '/':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.SLASH;
            break;
        case 'i':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.I;
            break;
        case 'n':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.N;
            break;
        case '%':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.LINE_COMMENT;
            break;
        case '[':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.OPEN_BRACKET;
            break;
        case '{':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.OPEN_CURLY;
            break;
        case '(':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        default:
            this.tokens.add(token);
            this.updateFrom();
            if (Character.isDigit(this.buffer[this.index])) {
                this.state = LexerState.DIGITS;
            } else if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                this.state = LexerState.VARIABLE;
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
            break;
        }
    }

    private void digitsFullstop() throws IOException {
        final List<PrologToken> newTokens =
            List.of(
                new PrologToken(
                    new String(this.buffer, this.from, this.index - this.from - 1),
                    TokenType.INT
                ),
                new PrologToken(".", TokenType.FULLSTOP)
            );
        switch (this.buffer[this.index]) {
        case '`':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.BACKQUOTE;
            break;
        case '\'':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.QUOTE;
            break;
        case '\"':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.STRING;
            break;
        case '/':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.SLASH;
            break;
        case 'i':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.I;
            break;
        case 'n':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.N;
            break;
        case '%':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.LINE_COMMENT;
            break;
        case '[':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.OPEN_BRACKET;
            break;
        case '{':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state = LexerState.OPEN_CURLY;
            break;
        case '(':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        default:
            if (Character.isDigit(this.buffer[this.index])) {
                this.state = LexerState.FLOAT;
            } else {
                this.tokens.addAll(newTokens);
                this.updateFrom();
                if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                    this.state = LexerState.VARIABLE;
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
            break;
        }
    }

    private void inf() throws IOException {
        final PrologToken token =
            new PrologToken(
                new String(this.buffer, this.from, this.index - this.from),
                    TokenType.INFINITY
                );
        switch (this.buffer[this.index]) {
        case '`':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.BACKQUOTE;
            break;
        case '\'':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.QUOTE;
            break;
        case '\"':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.STRING;
            break;
        case '/':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.SLASH;
            break;
        case '%':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.LINE_COMMENT;
            break;
        case '[':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.OPEN_BRACKET;
            break;
        case '{':
            this.tokens.add(token);
            this.updateFrom();
            this.state = LexerState.OPEN_CURLY;
            break;
        case '(':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        default:
            if (
                Character.isLetter(this.buffer[this.index])
                || this.buffer[this.index] == '_'
                || Character.isDigit(this.buffer[this.index])
            ) {
                this.state = LexerState.NAME;
            } else {
                this.tokens.add(token);
                this.updateFrom();
                if (Character.isWhitespace(this.buffer[this.index])) {
                    this.state = LexerState.WHITESPACE;
                } else if (Lexer.SYMBOLS.contains(this.buffer[this.index])) {
                    this.state = LexerState.NAME;
                } else {
                    throw new IOException(
                        "Unexpected character " + this.buffer[this.index] + " at position " + this.count + "!"
                    );
                }
            }
            break;
        }
    }

    private void initial() throws IOException {
        switch (this.buffer[this.index]) {
        case '`':
            this.state = LexerState.BACKQUOTE;
            break;
        case '\'':
            this.state = LexerState.QUOTE;
            break;
        case '\"':
            this.state = LexerState.STRING;
            break;
        case '/':
            this.state = LexerState.SLASH;
            break;
        case 'i':
            this.state = LexerState.I;
            break;
        case 'n':
            this.state = LexerState.N;
            break;
        case '%':
            this.state = LexerState.LINE_COMMENT;
            break;
        case '[':
            this.state = LexerState.OPEN_BRACKET;
            break;
        case '{':
            this.state = LexerState.OPEN_CURLY;
            break;
        case '(':
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.state = LexerState.INITIAL;
            this.updateFrom(1);
            break;
        default:
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
            break;
        }
    }

    private void integer() {
        // TODO Auto-generated method stub

    }

    private void letterI() {
        // TODO Auto-generated method stub

    }

    private void letterN() {
        // TODO Auto-generated method stub

    }

    private void lettersIN() {
        // TODO Auto-generated method stub

    }

    private void lettersNA() {
        // TODO Auto-generated method stub

    }

    private void lineComment() {
        // TODO Auto-generated method stub

    }

    private void name() {
        // TODO Auto-generated method stub

    }

    private void nan() {
        // TODO Auto-generated method stub

    }

    private void openBracket() {
        // TODO Auto-generated method stub

    }

    private void openCurly() {
        // TODO Auto-generated method stub

    }

    private void quote() {
        // TODO Auto-generated method stub

    }

    private void quoteEnd() {
        // TODO Auto-generated method stub

    }

    private void slash() {
        // TODO Auto-generated method stub

    }

    private void string() {
        // TODO Auto-generated method stub

    }

    private void stringEnd() {
        // TODO Auto-generated method stub

    }

    private void updateFrom() {
        this.updateFrom(0);
    }

    private void updateFrom(final int offset) {
        this.from = this.index + offset;
    }

    private void variable() {
        // TODO Auto-generated method stub

    }

    private void whitespace() {
        // TODO Auto-generated method stub

    }

}

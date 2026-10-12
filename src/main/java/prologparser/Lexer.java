package prologparser;

import java.io.*;
import java.util.*;
import java.util.stream.*;

public class Lexer {

    private static final Set<Character> SYMBOLS =
            Set.of('+', '-', '^', '<', '>', '=', '~', ':', '?', '@', '#', '$', '&', '`', '*', '.', '/', '\\');

    private final char[] buffer;

    private int count;

    private Deque<Integer> from;

    private int index;

    private final Reader reader;

    private final Deque<LexerState> state;

    private final List<PrologToken> tokens;

    public Lexer(final Reader reader) {
        this.reader = reader;
        this.state = new LinkedList<LexerState>();
        this.state.push(LexerState.INITIAL);
        this.from = new LinkedList<Integer>();
        this.from.push(0);
        this.index = 0;
        this.count = 0;
        this.buffer = new char[1024];
        this.tokens = new LinkedList<PrologToken>();
    }

    public List<PrologToken> tokenize() throws IOException {
        int read = this.reader.read(this.buffer, 0, this.buffer.length);
        while (read >= 0) {
            for (this.index = 0; this.index < read; this.index++, this.count++) {
                switch (this.state.peek()) {
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
            final int firstFrom = this.from.peekFirst();
            if (firstFrom < read) {
                final int diff = read - firstFrom;
                System.arraycopy(this.buffer, firstFrom, this.buffer, 0, diff);
                read = this.reader.read(this.buffer, diff, this.buffer.length - diff) + diff;
            } else {
                read = this.reader.read(this.buffer, 0, this.buffer.length);
            }
            this.from = this.from.stream().map(i -> i - firstFrom).collect(Collectors.toCollection(LinkedList::new));
        }
        return this.tokens;
    }

    private void backquote() {
        switch (this.buffer[this.index]) {
            case '`':
                this.state.pop();
                this.state.push(LexerState.BACKQUOTE_END);
                break;
            case '\\':
                this.state.push(LexerState.ESCAPE);
                this.from.push(this.index);
                break;
            default:
                // do nothing
        }
    }

    private void backquoteEnd() throws IOException {
        final PrologToken token =
            new PrologToken(
                new String(this.buffer, this.from.peek(), this.index - this.from.peek()),
                TokenType.NAME
            );
        switch (this.buffer[this.index]) {
        case '`':
            this.state.pop();
            this.state.push(LexerState.BACKQUOTE);
            break;
        case '\'':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.QUOTE);
            break;
        case '\"':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.STRING);
            break;
        case '/':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.SLASH);
            break;
        case 'i':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.I);
            break;
        case 'n':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.N);
            break;
        case '%':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.LINE_COMMENT);
            break;
        case '[':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_BRACKET);
            break;
        case '{':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_CURLY);
            break;
        case '(':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.updateFrom(1);
            break;
        default:
            this.tokens.add(token);
            this.updateFrom();
            if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                this.state.pop();
                this.state.push(LexerState.VARIABLE);
            } else if (Character.isDigit(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.DIGIT);
            } else if (Character.isWhitespace(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.WHITESPACE);
            } else if (
                Character.isLowerCase(this.buffer[this.index])
                || Lexer.SYMBOLS.contains(this.buffer[this.index])
            ) {
                this.state.pop();
                this.state.push(LexerState.NAME);
            } else {
                throw new IOException(
                    "Unexpected character " + this.buffer[this.index] + " at position " + this.count + "!"
                );
            }
            break;
        }
    }

    private void comment() {
        if (this.buffer[this.index] == '*') {
            this.state.pop();
            this.state.push(LexerState.COMMENT_STAR);
        }
    }

    private void commentStar() {
        switch (this.buffer[this.index]) {
        case '/':
            this.tokens.add(
                new PrologToken(
                    new String(this.buffer, this.from.peek(), this.index - this.from.peek() + 1),
                    TokenType.COMMENT
                )
            );
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.INITIAL);
            break;
        case '*':
            // do nothing
            break;
        default:
            this.state.pop();
            this.state.push(LexerState.COMMENT);
        }
    }

    private void digit() throws IOException {
        final PrologToken token =
            new PrologToken(
                new String(this.buffer, this.from.peek(), this.index - this.from.peek()),
                TokenType.INT
            );
        switch (this.buffer[this.index]) {
        case 'b':
        case 'o':
        case 'x':
        case '\'':
            this.state.pop();
            this.state.push(LexerState.INT);
            break;
        case '.':
            this.state.pop();
            this.state.push(LexerState.DIGITS_FULLSTOP);
            break;
        case '`':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.BACKQUOTE);
            break;
        case '\"':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.STRING);
            break;
        case '/':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.SLASH);
            break;
        case 'i':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.I);
            break;
        case 'n':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.N);
            break;
        case '%':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.LINE_COMMENT);
            break;
        case '[':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_BRACKET);
            break;
        case '{':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_CURLY);
            break;
        case '(':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.updateFrom(1);
            break;
        default:
            this.tokens.add(token);
            this.updateFrom();
            if (Character.isDigit(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.DIGITS);
            } else if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                this.state.pop();
                this.state.push(LexerState.VARIABLE);
            } else if (Character.isWhitespace(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.WHITESPACE);
            } else if (
                Character.isLowerCase(this.buffer[this.index])
                || Lexer.SYMBOLS.contains(this.buffer[this.index])
            ) {
                this.state.pop();
                this.state.push(LexerState.NAME);
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
                new String(this.buffer, this.from.peek(), this.index - this.from.peek()),
                TokenType.INT
            );
        switch (this.buffer[this.index]) {
        case '\'':
            this.state.pop();
            this.state.push(LexerState.INT);
            break;
        case '.':
            this.state.pop();
            this.state.push(LexerState.DIGITS_FULLSTOP);
            break;
        case '`':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.BACKQUOTE);
            break;
        case '\"':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.STRING);
            break;
        case '/':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.SLASH);
            break;
        case 'i':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.I);
            break;
        case 'n':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.N);
            break;
        case '%':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.LINE_COMMENT);
            break;
        case '[':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_BRACKET);
            break;
        case '{':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_CURLY);
            break;
        case '(':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.updateFrom(1);
            break;
        default:
            this.tokens.add(token);
            this.updateFrom();
            if (Character.isDigit(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.DIGITS);
            } else if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                this.state.pop();
                this.state.push(LexerState.VARIABLE);
            } else if (Character.isWhitespace(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.WHITESPACE);
            } else if (
                Character.isLowerCase(this.buffer[this.index])
                || Lexer.SYMBOLS.contains(this.buffer[this.index])
            ) {
                this.state.pop();
                this.state.push(LexerState.NAME);
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
                    new String(this.buffer, this.from.peek(), this.index - this.from.peek() - 1),
                    TokenType.INT
                ),
                new PrologToken(".", TokenType.FULLSTOP)
            );
        switch (this.buffer[this.index]) {
        case '`':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.BACKQUOTE);
            break;
        case '\'':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.QUOTE);
            break;
        case '\"':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.STRING);
            break;
        case '/':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.SLASH);
            break;
        case 'i':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.I);
            break;
        case 'n':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.N);
            break;
        case '%':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.LINE_COMMENT);
            break;
        case '[':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_BRACKET);
            break;
        case '{':
            this.tokens.addAll(newTokens);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_CURLY);
            break;
        case '(':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.addAll(newTokens);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.updateFrom(1);
            break;
        default:
            if (Character.isDigit(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.FLOAT);
            } else {
                this.tokens.addAll(newTokens);
                this.updateFrom();
                if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                    this.state.pop();
                    this.state.push(LexerState.VARIABLE);
                } else if (Character.isWhitespace(this.buffer[this.index])) {
                    this.state.pop();
                    this.state.push(LexerState.WHITESPACE);
                } else if (
                    Character.isLowerCase(this.buffer[this.index])
                    || Lexer.SYMBOLS.contains(this.buffer[this.index])
                ) {
                    this.state.pop();
                    this.state.push(LexerState.NAME);
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
                new String(this.buffer, this.from.peek(), this.index - this.from.peek()),
                    TokenType.INFINITY
                );
        switch (this.buffer[this.index]) {
        case '`':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.BACKQUOTE);
            break;
        case '\'':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.QUOTE);
            break;
        case '\"':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.STRING);
            break;
        case '/':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.SLASH);
            break;
        case '%':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.LINE_COMMENT);
            break;
        case '[':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_BRACKET);
            break;
        case '{':
            this.tokens.add(token);
            this.updateFrom();
            this.state.pop();
            this.state.push(LexerState.OPEN_CURLY);
            break;
        case '(':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(token);
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(token);
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.updateFrom(1);
            break;
        default:
            if (
                Character.isLetter(this.buffer[this.index])
                || this.buffer[this.index] == '_'
                || Character.isDigit(this.buffer[this.index])
            ) {
                this.state.pop();
                this.state.push(LexerState.NAME);
            } else {
                this.tokens.add(token);
                this.updateFrom();
                if (Character.isWhitespace(this.buffer[this.index])) {
                    this.state.pop();
                    this.state.push(LexerState.WHITESPACE);
                } else if (Lexer.SYMBOLS.contains(this.buffer[this.index])) {
                    this.state.pop();
                    this.state.push(LexerState.NAME);
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
            this.state.pop();
            this.state.push(LexerState.BACKQUOTE);
            break;
        case '\'':
            this.state.pop();
            this.state.push(LexerState.QUOTE);
            break;
        case '\"':
            this.state.pop();
            this.state.push(LexerState.STRING);
            break;
        case '/':
            this.state.pop();
            this.state.push(LexerState.SLASH);
            break;
        case 'i':
            this.state.pop();
            this.state.push(LexerState.I);
            break;
        case 'n':
            this.state.pop();
            this.state.push(LexerState.N);
            break;
        case '%':
            this.state.pop();
            this.state.push(LexerState.LINE_COMMENT);
            break;
        case '[':
            this.state.pop();
            this.state.push(LexerState.OPEN_BRACKET);
            break;
        case '{':
            this.state.pop();
            this.state.push(LexerState.OPEN_CURLY);
            break;
        case '(':
            this.tokens.add(new PrologToken("(", TokenType.OPEN_PARANTHESIS));
            this.updateFrom(1);
            break;
        case ')':
            this.tokens.add(new PrologToken(")", TokenType.CLOSE_PARANTHESIS));
            this.updateFrom(1);
            break;
        case '.':
            this.tokens.add(new PrologToken(".", TokenType.FULLSTOP));
            this.updateFrom(1);
            break;
        case '!':
            this.tokens.add(new PrologToken("!", TokenType.CUT));
            this.updateFrom(1);
            break;
        case ';':
            this.tokens.add(new PrologToken(";", TokenType.SEMICOLON));
            this.updateFrom(1);
            break;
        default:
            if (Character.isUpperCase(this.buffer[this.index]) || this.buffer[this.index] == '_') {
                this.state.pop();
                this.state.push(LexerState.VARIABLE);
            } else if (Character.isDigit(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.DIGIT);
            } else if (Character.isWhitespace(this.buffer[this.index])) {
                this.state.pop();
                this.state.push(LexerState.WHITESPACE);
            } else if (
                Character.isLowerCase(this.buffer[this.index])
                || Lexer.SYMBOLS.contains(this.buffer[this.index])
            ) {
                this.state.pop();
                this.state.push(LexerState.NAME);
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
        this.from.pop();
        this.from.push(this.index + offset);
    }

    private void variable() {
        // TODO Auto-generated method stub

    }

    private void whitespace() {
        // TODO Auto-generated method stub

    }

}

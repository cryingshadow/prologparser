package prologparser;

import java.io.*;
import java.util.*;

public class PrologParser {

    private static final String CLAUSE_SYMBOL = ":-";
    private static final String QUERY_SYMBOL = "?-";

    private static List<List<PrologToken>> extractSentences(final List<PrologToken> tokens) throws IOException {
        final List<List<PrologToken>> sentences = new LinkedList<List<PrologToken>>();
        List<PrologToken> currentSentence = new LinkedList<PrologToken>();
        for (final PrologToken token : tokens) {
            switch (token.type()) {
            case FULLSTOP:
                if (currentSentence.isEmpty()) {
                    throw new IOException("Empty sentence detected!");
                }
                sentences.add(currentSentence);
                currentSentence = new LinkedList<PrologToken>();
                break;
            default:
                if (
                    !sentences.isEmpty()
                    && currentSentence.isEmpty()
                    && token.type() != TokenType.WHITESPACE
                    && token.type() != TokenType.COMMENT
                ) {
                    throw new IOException("Fullstop must be followed by layout text!");
                }
                currentSentence.add(token);
            }
        }
        if (!currentSentence.isEmpty()) {
            throw new IOException("Last sentence ended unexpectedly!");
        }
        return sentences;
    }

    private static boolean isDirective(final PrologTerm term) {
        return (PrologParser.CLAUSE_SYMBOL.equals(term.name()) || PrologParser.QUERY_SYMBOL.equals(term.name()))
            && term.arguments().size() == 1;
    }

    private static PrologTerm parseCurlyBraceExpression(
        final List<TokenListItem> brace,
        final OperatorSet operators
    ) throws IOException {
        final PrologTerm parsed = PrologParser.parseSequence(brace, operators);
        return new PrologTerm("{}", List.of(parsed));
    }

    private static PrologTerm parseListExpression(final List<TokenListItem> list, final OperatorSet operators) {
        // TODO Auto-generated method stub
        return null;
    }

    private static void parseOperatorDefinition(final List<PrologToken> sentence, final OperatorSet operators) {
        final Iterator<PrologToken> iterator = sentence.iterator();
        PrologToken current = iterator.next();
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (
            current.type() != TokenType.NAME
            || (!PrologParser.CLAUSE_SYMBOL.equals(current.text()) && !PrologParser.QUERY_SYMBOL.equals(current.text()))
            || !iterator.hasNext()
        ) {
            return;
        }
        current = iterator.next();
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (current.type() != TokenType.NAME || (!"op".equals(current.text())) || !iterator.hasNext()) {
            return;
        }
        current = iterator.next();
        if (current.type() != TokenType.OPEN_PARANTHESIS || !iterator.hasNext()) {
            return;
        }
        current = iterator.next();
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (current.type() != TokenType.INT || !iterator.hasNext()) {
            return;
        }
        final int precendence = Integer.parseInt(current.text());
        current = iterator.next();
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (current.type() != TokenType.COMMA || !iterator.hasNext()) {
            return;
        }
        current = iterator.next();
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (current.type() != TokenType.NAME || !iterator.hasNext()) {
            return;
        }
        final String fixity = current.text();
        current = iterator.next();
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (current.type() != TokenType.COMMA || !iterator.hasNext()) {
            return;
        }
        current = iterator.next();
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        final List<String> operatorNames = new LinkedList<String>();
        switch (current.type()) {
        case NAME:
        case SEMICOLON:
        case CUT:
        case EMPTY_LIST:
        case INFINITY:
        case NAN:
        case PIPE:
            if (!iterator.hasNext()) {
                return;
            }
            operatorNames.add(current.text());
            break;
        case OPEN_BRACKET:
            if (!iterator.hasNext()) {
                return;
            }
            current = iterator.next();
            while (current.isLayout() && iterator.hasNext()) {
                current = iterator.next();
            }
            switch (current.type()) {
            case NAME:
            case SEMICOLON:
            case CUT:
            case EMPTY_LIST:
            case INFINITY:
            case NAN:
            case PIPE:
                if (!iterator.hasNext()) {
                    return;
                }
                operatorNames.add(current.text());
                break;
            default:
                return;
            }
            current = iterator.next();
            while (current.isLayout() && iterator.hasNext()) {
                current = iterator.next();
            }
            while (current.type() != TokenType.CLOSE_BRACKET) {
                if (current.type() != TokenType.COMMA || !iterator.hasNext()) {
                    return;
                }
                current = iterator.next();
                while (current.isLayout() && iterator.hasNext()) {
                    current = iterator.next();
                }
                switch (current.type()) {
                case NAME:
                case SEMICOLON:
                case CUT:
                case EMPTY_LIST:
                case INFINITY:
                case NAN:
                case PIPE:
                    if (!iterator.hasNext()) {
                        return;
                    }
                    operatorNames.add(current.text());
                    break;
                default:
                    return;
                }
                current = iterator.next();
                while (current.isLayout() && iterator.hasNext()) {
                    current = iterator.next();
                }
            }
            if (!iterator.hasNext()) {
                return;
            }
            break;
        default:
            return;
        }
        current = iterator.next();
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (current.type() != TokenType.CLOSE_PARANTHESIS) {
            return;
        }
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (iterator.hasNext() || (current.type() != TokenType.CLOSE_PARANTHESIS && !current.isLayout())) {
            return;
        }
        for (final String operator : operatorNames) {
            operators.put(operator, new OperatorDefinition(OperatorFixity.valueOf(fixity.toUpperCase()), precendence));
        }
    }

    private static void parseOperatorDefinitions(final List<List<PrologToken>> sentences, final OperatorSet operators) {
        for (final List<PrologToken> sentence : sentences) {
            PrologParser.parseOperatorDefinition(sentence, operators);
        }
    }

    private static PrologTerm parseSequence(
        final List<? extends TokenListItem> items,
        final OperatorSet operators
    ) throws IOException {
        final Map<Integer, Integer> outerParentheses = new LinkedHashMap<Integer, Integer>();
        final Map<Integer, Integer> outerBrackets = new LinkedHashMap<Integer, Integer>();
        final Map<Integer, Integer> outerCurly = new LinkedHashMap<Integer, Integer>();
        int parentheses = 0;
        int brackets = 0;
        int curly = 0;
        int open = -1;
        for (int i = 0; i < items.size(); i++) {
            final TokenListItem item = items.get(i);
            switch (item.type()) {
            case OPEN_PARANTHESIS:
                if (parentheses == 0 && brackets == 0 && curly == 0) {
                    open = i;
                }
                parentheses++;
                break;
            case OPEN_BRACKET:
                if (parentheses == 0 && brackets == 0 && curly == 0) {
                    open = i;
                }
                brackets++;
                break;
            case OPEN_CURLY:
                if (parentheses == 0 && brackets == 0 && curly == 0) {
                    open = i;
                }
                curly++;
                break;
            case CLOSE_PARANTHESIS:
                parentheses--;
                if (parentheses == 0 && brackets == 0 && curly == 0) {
                    outerParentheses.put(open, i);
                }
                if (parentheses < 0) {
                    throw new IOException("Parenthesis closed, but not opened!");
                }
                break;
            case CLOSE_BRACKET:
                brackets--;
                if (parentheses == 0 && brackets == 0 && curly == 0) {
                    outerBrackets.put(open, i);
                }
                if (brackets < 0) {
                    throw new IOException("Brackets closed, but not opened!");
                }
                break;
            case CLOSE_CURLY:
                curly--;
                if (parentheses == 0 && brackets == 0 && curly == 0) {
                    outerParentheses.put(open, i);
                }
                if (parentheses < 0) {
                    throw new IOException("Curly brace closed, but not opened!");
                }
                break;
            default:
                // do nothing
            }
        }
        if (parentheses > 0 || brackets > 0 || curly > 0) {
            throw new IOException("Opened brace was not closed again!");
        }
        final List<TokenListItem> bracedItems = new ArrayList<TokenListItem>();
        for (int i = 0; i < items.size(); i++) {
            if (outerParentheses.containsKey(i)) {
                final List<TokenListItem> parenthesis = new ArrayList<TokenListItem>();
                final int to = outerParentheses.get(i);
                i++;
                while (i < to) {
                    parenthesis.add(items.get(i));
                    i++;
                }
                i++;
                bracedItems.add(PrologParser.parseSequence(parenthesis, operators));
            } else if (outerBrackets.containsKey(i)) {
                final List<TokenListItem> bracket = new ArrayList<TokenListItem>();
                final int to = outerBrackets.get(i);
                i++;
                while (i < to) {
                    bracket.add(items.get(i));
                    i++;
                }
                i++;
                bracedItems.add(PrologParser.parseListExpression(bracket, operators));
            } else if (outerCurly.containsKey(i)) {
                final List<TokenListItem> brace = new ArrayList<TokenListItem>();
                final int to = outerCurly.get(i);
                i++;
                while (i < to) {
                    brace.add(items.get(i));
                    i++;
                }
                i++;
                bracedItems.add(PrologParser.parseCurlyBraceExpression(brace, operators));
            } else {
                bracedItems.add(items.get(i));
            }
        }
        //TODO
        return null;
    }

    private static PrologClause toClause(final PrologTerm term) {
        if (PrologParser.CLAUSE_SYMBOL.equals(term.name()) && term.arguments().size() == 2) {
            return new PrologClause(term.arguments().getFirst(), Optional.of(term.arguments().getLast()));
        }
        return new PrologClause(term, Optional.empty());
    }

    private final Reader reader;

    public PrologParser(final Reader reader) {
        this.reader = reader;
    }

    public PrologProgram parse() throws IOException {
        final List<PrologClause> clauses = new LinkedList<PrologClause>();
        final List<PrologTerm> directives = new LinkedList<PrologTerm>();
        final OperatorSet operators = new OperatorSet();
        final List<PrologToken> tokens = new Lexer(this.reader).tokenize();
        final List<String> comments =
            tokens.stream().filter(t -> t.type() == TokenType.COMMENT).map(PrologToken::text).toList();
        final List<List<PrologToken>> sentences = PrologParser.extractSentences(tokens);
        PrologParser.parseOperatorDefinitions(sentences, operators);
        final List<PrologTerm> parsedSentences = new LinkedList<PrologTerm>();
        for (final List<PrologToken> sentence : sentences) {
            parsedSentences.add(PrologParser.parseSequence(sentence, operators));
        }
        for (final PrologTerm term : parsedSentences) {
            if (PrologParser.isDirective(term)) {
                directives.add(term);
            } else {
                clauses.add(PrologParser.toClause(term));
            }
        }
        return new PrologProgram(clauses, directives, operators, comments);
    }

}

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

    private static PrologTerm parseSentence(final List<PrologToken> tokens, final OperatorSet operators) {
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
        for (
            final PrologTerm term :
                sentences.stream().map(sentence -> PrologParser.parseSentence(sentence, operators)).toList()
        ) {
            if (PrologParser.isDirective(term)) {
                directives.add(term);
            } else {
                clauses.add(PrologParser.toClause(term));
            }
        }
        return new PrologProgram(clauses, directives, operators, comments);
    }

}

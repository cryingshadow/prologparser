package prologparser;

import java.io.*;
import java.util.*;

public class PrologParser {

    private static final String CLAUSE_SYMBOL = ":-";
    private static final String QUERY_SYMBOL = "?-";

    private static PrologTerm cleanLayoutAndParentheses(final List<TokenListItem> compounds) throws IOException {
        PrologTerm result = null;
        for (final TokenListItem item : compounds) {
            switch (item.type()) {
            case COMMENT:
            case WHITESPACE:
                break;
            case TERM:
                if (result != null) {
                    throw new IOException("Disconnected terms detected!");
                }
                result = PrologParser.cleanParentheses((PrologTerm)item);
                break;
            default:
                throw new IOException("Unparsed token detected: " + item.type());
            }
        }
        if (result == null) {
            throw new IOException("Empty term detected!");
        }
        return result;
    }

    private static PrologTerm cleanParentheses(final PrologTerm term) throws IOException {
        if ("()".equals(term.name())) {
            if (term.arguments().size() != 1) {
                throw new IOException("Disconnected terms detected!");
            }
            return PrologParser.cleanParentheses(term.arguments().getFirst());
        }
        final List<PrologTerm> arguments = new ArrayList<PrologTerm>();
        for (final PrologTerm argument : term.arguments()) {
            arguments.add(PrologParser.cleanParentheses(argument));
        }
        return new PrologTerm(term.name(), arguments);
    }

    private static List<PrologTerm> commaToList(final PrologTerm term) {
        if (",".equals(term.name())) {
            return term.arguments().stream().map(PrologParser::commaToList).flatMap(List::stream).toList();
        }
        return List.of(term);
    }

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

    private static boolean isLayout(final List<TokenListItem> bracedItems, final int from, final int to) {
        for (int i = from; i <= to; i++) {
            switch (bracedItems.get(i).type()) {
            case COMMENT:
            case WHITESPACE:
                break;
            default:
                return false;
            }
        }
        return true;
    }

    private static List<PrologTerm> parseArguments(final List<PrologTerm> arguments) {
        final List<PrologTerm> result = new ArrayList<PrologTerm>();
        for (final PrologTerm argument : arguments) {
            result.addAll(PrologParser.commaToList(argument));
        }
        return result;
    }

    private static PrologTerm parseCompounds(final List<TokenListItem> bracedItems) throws IOException {
        final List<TokenListItem> compounds = new ArrayList<TokenListItem>();
        for (int i = 0; i < bracedItems.size(); i++) {
            final TokenListItem item = bracedItems.get(i);
            if (item.type() == TokenType.NAME && i < bracedItems.size() - 1) {
                final TokenListItem nextItem = bracedItems.get(i + 1);
                if (nextItem instanceof final PrologTerm term && "()".equals(term.name())) {
                    compounds.add(new PrologTerm(item.name(), PrologParser.parseArguments(term.arguments())));
                    i++;
                    continue;
                }
            }
            compounds.add(item);
        }
        return PrologParser.cleanLayoutAndParentheses(compounds);
    }

    private static PrologTerm parseListExpression(
        final List<TokenListItem> list,
        final OperatorSet operators
    ) throws IOException {
        final List<TokenListItem> bracedItems = PrologParser.parseOuterBraces(list, operators);
        PrologTerm parsedLeft;
        PrologTerm parsedRight;
        for (int i = 0; i < bracedItems.size(); i++) {
            switch (bracedItems.get(i).type()) {
            case COMMA:
                parsedLeft = PrologParser.parseSequence(bracedItems.subList(0, i), operators, 999);
                if (parsedLeft == null) {
                    throw new IOException("Could not parse list expression!");
                }
                parsedRight =
                    PrologParser.parseListExpression(
                        bracedItems.subList(i + 1, bracedItems.size()),
                        operators
                    );
                if (parsedRight == null) {
                    throw new IOException("Could not parse list expression!");
                }
                return new PrologTerm("[]", List.of(parsedLeft, parsedRight));
            case PIPE:
                parsedLeft = PrologParser.parseSequence(bracedItems.subList(0, i), operators, 999);
                if (parsedLeft == null) {
                    throw new IOException("Could not parse list expression!");
                }
                parsedRight =
                    PrologParser.parseSequence(
                        bracedItems.subList(i + 1, bracedItems.size()),
                        operators,
                        999
                    );
                if (parsedRight == null) {
                    throw new IOException("Could not parse list expression!");
                }
                return new PrologTerm("[]", List.of(parsedLeft, parsedRight));
            default:
                //do nothing
            }
        }
        return new PrologTerm("[]", List.of(PrologParser.parseSequence(bracedItems, operators, 999)));
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
        if (current.type() != TokenType.OPEN_PARENTHESIS || !iterator.hasNext()) {
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
        if (current.type() != TokenType.CLOSE_PARENTHESIS) {
            return;
        }
        while (current.isLayout() && iterator.hasNext()) {
            current = iterator.next();
        }
        if (iterator.hasNext() || (current.type() != TokenType.CLOSE_PARENTHESIS && !current.isLayout())) {
            return;
        }
        for (final String operator : operatorNames) {
            operators.put(operator, new OperatorData(OperatorFixity.valueOf(fixity.toUpperCase()), precendence));
        }
    }

    private static void parseOperatorDefinitions(final List<List<PrologToken>> sentences, final OperatorSet operators) {
        for (final List<PrologToken> sentence : sentences) {
            PrologParser.parseOperatorDefinition(sentence, operators);
        }
    }

    private static PrologTerm parseOperatorTerm(
        final List<TokenListItem> bracedItems,
        final OperatorSet operators,
        final int precedence
    ) {
        for (final OperatorDefinition operator : operators.operatorsInDescendingPrecedence()) {
            if (precedence < operator.precedence()) {
                continue;
            }
            PrologTerm parsedLeft;
            PrologTerm parsedRight;
            for (int i = 0; i < bracedItems.size(); i++) {
                if (bracedItems.get(i).name().equals(operator.name())) {
                    switch (operator.fixity()) {
                    case FX:
                        if (!PrologParser.isLayout(bracedItems, 0, i - 1)) {
                            continue;
                        }
                        try {
                            parsedRight =
                                PrologParser.parseSequence(
                                    bracedItems.subList(i + 1, bracedItems.size()),
                                    operators,
                                    operator.precedence() - 1
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedRight == null) {
                            continue;
                        }
                        return new PrologTerm(operator.name(), List.of(parsedRight));
                    case FY:
                        if (!PrologParser.isLayout(bracedItems, 0, i - 1)) {
                            continue;
                        }
                        try {
                            parsedRight =
                                PrologParser.parseSequence(
                                    bracedItems.subList(i + 1, bracedItems.size()),
                                    operators,
                                    operator.precedence()
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedRight == null) {
                            continue;
                        }
                        return new PrologTerm(operator.name(), List.of(parsedRight));
                    case XF:
                        if (!PrologParser.isLayout(bracedItems, i + 1, bracedItems.size() - 1)) {
                            continue;
                        }
                        try {
                            parsedLeft =
                                PrologParser.parseSequence(
                                    bracedItems.subList(0, i),
                                    operators,
                                    operator.precedence() - 1
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedLeft == null) {
                            continue;
                        }
                        return new PrologTerm(operator.name(), List.of(parsedLeft));
                    case XFX:
                        try {
                            parsedLeft =
                                PrologParser.parseSequence(
                                    bracedItems.subList(0, i),
                                    operators,
                                    operator.precedence() - 1
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedLeft == null) {
                            continue;
                        }
                        try {
                            parsedRight =
                                PrologParser.parseSequence(
                                    bracedItems.subList(i + 1, bracedItems.size()),
                                    operators,
                                    operator.precedence() - 1
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedRight == null) {
                            continue;
                        }
                        return new PrologTerm(operator.name(), List.of(parsedLeft, parsedRight));
                    case XFY:
                        try {
                            parsedLeft =
                                PrologParser.parseSequence(
                                    bracedItems.subList(0, i),
                                    operators,
                                    operator.precedence() - 1
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedLeft == null) {
                            continue;
                        }
                        try {
                            parsedRight =
                                PrologParser.parseSequence(
                                    bracedItems.subList(i + 1, bracedItems.size()),
                                    operators,
                                    operator.precedence()
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedRight == null) {
                            continue;
                        }
                        return new PrologTerm(operator.name(), List.of(parsedLeft, parsedRight));
                    case YF:
                        if (!PrologParser.isLayout(bracedItems, i + 1, bracedItems.size() - 1)) {
                            continue;
                        }
                        try {
                            parsedLeft =
                                PrologParser.parseSequence(
                                    bracedItems.subList(0, i),
                                    operators,
                                    operator.precedence()
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedLeft == null) {
                            continue;
                        }
                        return new PrologTerm(operator.name(), List.of(parsedLeft));
                    case YFX:
                        try {
                            parsedLeft =
                                PrologParser.parseSequence(
                                    bracedItems.subList(0, i),
                                    operators,
                                    operator.precedence()
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedLeft == null) {
                            continue;
                        }
                        try {
                            parsedRight =
                                PrologParser.parseSequence(
                                    bracedItems.subList(i + 1, bracedItems.size()),
                                    operators,
                                    operator.precedence() - 1
                                );
                        } catch (final IOException e) {
                            continue;
                        }
                        if (parsedRight == null) {
                            continue;
                        }
                        return new PrologTerm(operator.name(), List.of(parsedLeft, parsedRight));
                    default:
                        throw new IllegalStateException("Unknown fixity detected!");
                    }
                }
            }
        }
        return null;
    }

    private static List<TokenListItem> parseOuterBraces(
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
            case OPEN_PARENTHESIS:
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
            case CLOSE_PARENTHESIS:
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
                bracedItems.add(
                    new PrologTerm("()", List.of(PrologParser.parseSequence(parenthesis, operators, 1200)))
                );
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
                bracedItems.add(new PrologTerm("{}", List.of(PrologParser.parseSequence(brace, operators, 1200))));
            } else {
                bracedItems.add(items.get(i));
            }
        }
        return bracedItems;
    }

    private static PrologTerm parseSequence(
        final List<? extends TokenListItem> items,
        final OperatorSet operators,
        final int precedence
    ) throws IOException {
        final List<TokenListItem> bracedItems = PrologParser.parseOuterBraces(items, operators);
        final PrologTerm parsed = PrologParser.parseOperatorTerm(bracedItems, operators, precedence);
        return parsed == null ? PrologParser.parseCompounds(bracedItems) : parsed;
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
            parsedSentences.add(PrologParser.parseSequence(sentence, operators, 1200));
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

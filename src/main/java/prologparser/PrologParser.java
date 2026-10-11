package prologparser;

import java.io.*;
import java.util.*;

public class PrologParser {

    private final Reader reader;

    public PrologParser(final Reader reader) {
        this.reader = reader;
    }

    public PrologProgram parse() throws IOException {
        final List<PrologClause> clauses = new LinkedList<PrologClause>();
        final List<PrologTerm> directives = new LinkedList<PrologTerm>();
        final OperatorSet operators = new OperatorSet();
        this.reader.read();
        return new PrologProgram(clauses, directives, operators);
    }

}

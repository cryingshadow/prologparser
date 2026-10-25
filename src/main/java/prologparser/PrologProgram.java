package prologparser;

import java.util.*;

public record PrologProgram(
    List<PrologClause> clauses,
    List<PrologTerm> directives,
    OperatorSet operators,
    List<String> comments
) {

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        for (final String comment : this.comments()) {
            result.append(comment);
            result.append("\n");
        }
        for (final PrologTerm query : this.directives()) {
            result.append(":- ");
            result.append(query.toString());
            result.append(".\n");
        }
        for (final PrologClause clause : this.clauses()) {
            result.append(clause.toString());
            result.append("\n");
        }
        return result.toString();
    }

}

package prologparser;

import java.util.*;

public record PrologProgram(List<PrologClause> clauses, List<PrologTerm> directives, OperatorSet operators) {

}

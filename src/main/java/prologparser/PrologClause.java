package prologparser;

import java.util.*;

public record PrologClause(PrologTerm head, Optional<PrologTerm> body) {

}

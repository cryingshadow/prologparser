package prologparser;

import java.util.*;

public record PrologClause(PrologTerm head, Optional<PrologTerm> body) {

    @Override
    public String toString() {
        if (this.body.isEmpty()) {
            return String.format("%s.", this.head.toString());
        }
        return String.format("%s :- %s.", this.head.toString(), this.body.get().toString());
    }

}

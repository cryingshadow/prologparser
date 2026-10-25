package prologparser;

import java.util.*;
import java.util.stream.*;

public class PrologTerm implements TokenListItem {

    public final List<PrologTerm> arguments;

    public final String name;

    public PrologTerm(final String name, final List<PrologTerm> arguments) {
        this.name = name;
        this.arguments = arguments;
    }

    @Override
    public boolean equals(final Object o) {
        if (o == null) {
            return false;
        }
        if (this.getClass() != o.getClass()) {
            return false;
        }
        final PrologTerm other = (PrologTerm)o;
        return this.name.equals(other.name) && this.arguments.equals(other.arguments);
    }

    @Override
    public int hashCode() {
        return this.name.hashCode() * 2 + this.arguments.hashCode() * 3;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.arguments.isEmpty() ?
            this.name :
                String.format(
                    "%s(%s)",
                    this.name,
                    this.arguments.stream().map(PrologTerm::toString).collect(Collectors.joining(", "))
                );
    }

    @Override
    public TokenType type() {
        return TokenType.TERM;
    }

}

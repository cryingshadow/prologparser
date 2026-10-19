package prologparser;

import java.util.*;

public record PrologTerm(String name, List<PrologTerm> arguments) implements TokenListItem {

    @Override
    public TokenType type() {
        return TokenType.TERM;
    }

}

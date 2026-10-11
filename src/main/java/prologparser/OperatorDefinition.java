package prologparser;

import java.util.*;

public class OperatorDefinition extends LinkedHashMap<OperatorFixity, Integer> {

    private static final long serialVersionUID = 1L;

    public OperatorDefinition(final OperatorFixity fixity, final int precedence) {
        this.put(fixity, precedence);
    }

    public OperatorDefinition(
        final OperatorFixity fixity1,
        final int precedence1,
        final OperatorFixity fixity2,
        final Integer precedence2
    ) {
        this.put(fixity1, precedence1);
        this.put(fixity2, precedence2);
    }

    @Override
    public Integer put(final OperatorFixity fixity, final Integer precedence) {
        if (precedence < 0 || precedence > 1200) {
            throw new IllegalArgumentException("Only precedences between 0 and 1200 are allowed!");
        }
        if (fixity == null) {
            throw new IllegalArgumentException("No operator fixity provided!");
        }
        return super.put(fixity, precedence);
    }

}

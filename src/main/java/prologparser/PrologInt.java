package prologparser;

import java.math.*;
import java.util.*;

public class PrologInt extends PrologTerm {

    public PrologInt(final String name) {
        super(name, List.of());
    }

    public BigInteger value() {
        return new BigInteger(this.name);
    }

}

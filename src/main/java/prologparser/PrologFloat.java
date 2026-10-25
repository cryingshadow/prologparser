package prologparser;

import java.util.*;

public class PrologFloat extends PrologTerm {

    public PrologFloat(final String name) {
        super(name, List.of());
    }

    public double value() {
        return Double.parseDouble(this.name);
    }

}

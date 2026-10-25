package prologparser;

public record OperatorDefinition(
    String name,
    OperatorFixity fixity,
    int precedence
) implements Comparable<OperatorDefinition> {

    @Override
    public int compareTo(final OperatorDefinition o) {
        int compare = -Integer.compare(this.precedence(), o.precedence());
        if (compare != 0) {
            return compare;
        }
        compare = -this.fixity().compareTo(o.fixity());
        if (compare != 0) {
            return compare;
        }
        return this.name().compareTo(o.name());
    }

}

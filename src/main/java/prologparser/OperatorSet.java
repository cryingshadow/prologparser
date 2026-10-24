package prologparser;

import java.util.*;
import java.util.stream.*;

public class OperatorSet extends LinkedHashMap<String, OperatorData> {

    private static final long serialVersionUID = 1L;

    public OperatorSet() {
        this.put(":-", new OperatorData(OperatorFixity.XFX, 1200, OperatorFixity.FX, 1200));
        this.put("-->", new OperatorData(OperatorFixity.XFX, 1200));
        this.put("?-", new OperatorData(OperatorFixity.FX, 1200));
        this.put("mode", new OperatorData(OperatorFixity.FX, 1150));
        this.put("public", new OperatorData(OperatorFixity.FX, 1150));
        this.put("dynamic", new OperatorData(OperatorFixity.FX, 1150));
        this.put("volatile", new OperatorData(OperatorFixity.FX, 1150));
        this.put("discontiguous", new OperatorData(OperatorFixity.FX, 1150));
        this.put("multifile", new OperatorData(OperatorFixity.FX, 1150));
        this.put("block", new OperatorData(OperatorFixity.FX, 1150));
        this.put("meta_predicate", new OperatorData(OperatorFixity.FX, 1150));
        this.put("initialization", new OperatorData(OperatorFixity.FX, 1150));
        this.put(";", new OperatorData(OperatorFixity.XFY, 1100));
        this.put("->", new OperatorData(OperatorFixity.XFY, 1050));
        this.put(",", new OperatorData(OperatorFixity.XFY, 1000));
        this.put("\\+", new OperatorData(OperatorFixity.FY, 900));
        this.put("spy", new OperatorData(OperatorFixity.FY, 900));
        this.put("nospy", new OperatorData(OperatorFixity.FY, 900));
        this.put("=", new OperatorData(OperatorFixity.XFX, 700));
        this.put("\\=", new OperatorData(OperatorFixity.XFX, 700));
        this.put("is", new OperatorData(OperatorFixity.XFX, 700));
        this.put("=..", new OperatorData(OperatorFixity.XFX, 700));
        this.put("==", new OperatorData(OperatorFixity.XFX, 700));
        this.put("\\==", new OperatorData(OperatorFixity.XFX, 700));
        this.put("@<", new OperatorData(OperatorFixity.XFX, 700));
        this.put("@>", new OperatorData(OperatorFixity.XFX, 700));
        this.put("@=<", new OperatorData(OperatorFixity.XFX, 700));
        this.put("@>=", new OperatorData(OperatorFixity.XFX, 700));
        this.put("=:=", new OperatorData(OperatorFixity.XFX, 700));
        this.put("=\\=", new OperatorData(OperatorFixity.XFX, 700));
        this.put("<", new OperatorData(OperatorFixity.XFX, 700));
        this.put(">", new OperatorData(OperatorFixity.XFX, 700));
        this.put("=<", new OperatorData(OperatorFixity.XFX, 700));
        this.put(">=", new OperatorData(OperatorFixity.XFX, 700));
        this.put(":", new OperatorData(OperatorFixity.XFY, 550));
        this.put("+", new OperatorData(OperatorFixity.YFX, 500, OperatorFixity.FY, 200));
        this.put("-", new OperatorData(OperatorFixity.YFX, 500, OperatorFixity.FY, 200));
        this.put("#", new OperatorData(OperatorFixity.YFX, 500));
        this.put("/\\", new OperatorData(OperatorFixity.YFX, 500));
        this.put("xor", new OperatorData(OperatorFixity.YFX, 500));
        this.put("\\/", new OperatorData(OperatorFixity.YFX, 500));
        this.put("*", new OperatorData(OperatorFixity.YFX, 400));
        this.put("/", new OperatorData(OperatorFixity.YFX, 400));
        this.put("//", new OperatorData(OperatorFixity.YFX, 400));
        this.put("mod", new OperatorData(OperatorFixity.YFX, 400));
        this.put("rem", new OperatorData(OperatorFixity.YFX, 400));
        this.put("<<", new OperatorData(OperatorFixity.YFX, 400));
        this.put(">>", new OperatorData(OperatorFixity.YFX, 400));
        this.put("**", new OperatorData(OperatorFixity.XFX, 200));
        this.put("^", new OperatorData(OperatorFixity.XFY, 200));
        this.put("\\", new OperatorData(OperatorFixity.FY, 200));
    }

    public TreeSet<OperatorDefinition> operatorsInDescendingPrecedence() {
        return
            this
            .entrySet()
            .stream()
            .flatMap(entry ->
                entry
                .getValue()
                .entrySet()
                .stream()
                .map(data -> new OperatorDefinition(entry.getKey(), data.getKey(), data.getValue()))
            ).collect(Collectors.toCollection(TreeSet<OperatorDefinition>::new));
    }

    @Override
    public OperatorData put(final String operator, final OperatorData definition) {
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("No operator name provided!");
        }
        if (this.containsKey(operator)) {
            return super.put(operator, this.get(operator).merge(definition));
        } else {
            return super.put(operator, definition);
        }
    }

}

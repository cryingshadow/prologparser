package prologparser;

import java.util.*;

public class OperatorSet extends LinkedHashMap<String, OperatorDefinition> {

    private static final long serialVersionUID = 1L;

    public OperatorSet() {
        this.put(":-", new OperatorDefinition(OperatorFixity.XFX, 1200, OperatorFixity.FX, 1200));
        this.put("-->", new OperatorDefinition(OperatorFixity.XFX, 1200));
        this.put("?-", new OperatorDefinition(OperatorFixity.FX, 1200));
        this.put("mode", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put("public", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put("dynamic", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put("volatile", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put("discontiguous", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put("multifile", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put("block", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put("meta_predicate", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put("initialization", new OperatorDefinition(OperatorFixity.FX, 1150));
        this.put(";", new OperatorDefinition(OperatorFixity.XFY, 1100));
        this.put("->", new OperatorDefinition(OperatorFixity.XFY, 1050));
        this.put(",", new OperatorDefinition(OperatorFixity.XFY, 1000));
        this.put("\\+", new OperatorDefinition(OperatorFixity.FY, 900));
        this.put("spy", new OperatorDefinition(OperatorFixity.FY, 900));
        this.put("nospy", new OperatorDefinition(OperatorFixity.FY, 900));
        this.put("=", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("\\=", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("is", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("=..", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("==", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("\\==", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("@<", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("@>", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("@=<", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("@>=", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("=:=", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("=\\=", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("<", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put(">", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put("=<", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put(">=", new OperatorDefinition(OperatorFixity.XFX, 700));
        this.put(":", new OperatorDefinition(OperatorFixity.XFY, 550));
        this.put("+", new OperatorDefinition(OperatorFixity.YFX, 500, OperatorFixity.FY, 200));
        this.put("-", new OperatorDefinition(OperatorFixity.YFX, 500, OperatorFixity.FY, 200));
        this.put("#", new OperatorDefinition(OperatorFixity.YFX, 500));
        this.put("/\\", new OperatorDefinition(OperatorFixity.YFX, 500));
        this.put("xor", new OperatorDefinition(OperatorFixity.YFX, 500));
        this.put("\\/", new OperatorDefinition(OperatorFixity.YFX, 500));
        this.put("*", new OperatorDefinition(OperatorFixity.YFX, 400));
        this.put("/", new OperatorDefinition(OperatorFixity.YFX, 400));
        this.put("//", new OperatorDefinition(OperatorFixity.YFX, 400));
        this.put("mod", new OperatorDefinition(OperatorFixity.YFX, 400));
        this.put("rem", new OperatorDefinition(OperatorFixity.YFX, 400));
        this.put("<<", new OperatorDefinition(OperatorFixity.YFX, 400));
        this.put(">>", new OperatorDefinition(OperatorFixity.YFX, 400));
        this.put("**", new OperatorDefinition(OperatorFixity.XFX, 200));
        this.put("^", new OperatorDefinition(OperatorFixity.XFY, 200));
        this.put("\\", new OperatorDefinition(OperatorFixity.FY, 200));
    }

    @Override
    public OperatorDefinition put(final String operator, final OperatorDefinition definition) {
        if (operator == null || operator.isBlank()) {
            throw new IllegalArgumentException("No operator name provided!");
        }
        return super.put(operator, definition);
    }

}

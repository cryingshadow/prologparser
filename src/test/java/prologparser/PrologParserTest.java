package prologparser;

import java.io.*;
import java.util.*;
import java.util.Optional;

import org.testng.*;
import org.testng.annotations.*;

public class PrologParserTest {

    @DataProvider
    public Object[][] parserData() {
        return new Object[][] {
            {
                new StringReader(""),
                new PrologProgram(List.of(), List.of(), new OperatorSet(), List.of())
            },
            {
                new StringReader("app([],YS,YS).\napp([X|XS],YS,[X|ZS]) :- app(XS,YS,ZS)."),
                new PrologProgram(
                    List.of(
                        new PrologClause(
                            new PrologTerm(
                                "app",
                                List.of(
                                    new PrologTerm("[]", List.of()),
                                    new PrologVariable("YS"),
                                    new PrologVariable("YS")
                                )
                            ),
                            Optional.empty()
                        ),
                        new PrologClause(
                            new PrologTerm(
                                "app",
                                List.of(
                                    new PrologTerm("[|]", List.of(new PrologVariable("X"), new PrologVariable("XS"))),
                                    new PrologVariable("YS"),
                                    new PrologTerm("[|]", List.of(new PrologVariable("X"), new PrologVariable("ZS")))
                                )
                            ),
                            Optional.of(
                                new PrologTerm(
                                    "app",
                                    List.of(
                                        new PrologVariable("XS"),
                                        new PrologVariable("YS"),
                                        new PrologVariable("ZS")
                                    )
                                )
                            )
                        )
                    ),
                    List.of(),
                    new OperatorSet(),
                    List.of()
                )
            },
            {
                new StringReader("%query: count(g).\ncount(X) :- X < 0, !.\ncount(X) :- Y is X - 1, count(Y)."),
                new PrologProgram(
                    List.of(
                        new PrologClause(
                            new PrologTerm("count", List.of(new PrologVariable("X"))),
                            Optional.of(
                                new PrologTerm(
                                    ",",
                                    List.of(
                                        new PrologTerm("<", List.of(new PrologVariable("X"), new PrologInt("0"))),
                                        new PrologTerm("!", List.of())
                                    )
                                )
                            )
                        ),
                        new PrologClause(
                            new PrologTerm("count", List.of(new PrologVariable("X"))),
                            Optional.of(
                                new PrologTerm(
                                    ",",
                                    List.of(
                                        new PrologTerm(
                                            "is",
                                            List.of(
                                                new PrologVariable("Y"),
                                                new PrologTerm(
                                                    "-",
                                                    List.of(new PrologVariable("X"), new PrologInt("1"))
                                                )
                                            )
                                        ),
                                        new PrologTerm("count", List.of(new PrologVariable("Y")))
                                    )
                                )
                            )
                        )
                    ),
                    List.of(),
                    new OperatorSet(),
                    List.of("%query: count(g).\n")
                )
            },
            {
                new StringReader(
                    "% Simple example for termination because of unification without occurs-check and cuts\n"
                    + "% Author: Thomas Stroeder\n\n"
                    + "%query: t.\n"
                    + "t :- eq(X,f(X)), !.\n"
                    + "t :- t.\n"
                    + "eq(X,X)."
                ),
                new PrologProgram(
                    List.of(
                        new PrologClause(
                            new PrologTerm("t", List.of()),
                            Optional.of(
                                new PrologTerm(
                                    ",",
                                    List.of(
                                        new PrologTerm(
                                            "eq",
                                            List.of(
                                                new PrologVariable("X"),
                                                new PrologTerm("f", List.of(new PrologVariable("X")))
                                            )
                                        ),
                                        new PrologTerm("!", List.of())
                                    )
                                )
                            )
                        ),
                        new PrologClause(
                            new PrologTerm("t", List.of()),
                            Optional.of(new PrologTerm("t", List.of()))
                        ),
                        new PrologClause(
                            new PrologTerm("eq", List.of(new PrologVariable("X"), new PrologVariable("X"))),
                            Optional.empty()
                        )
                    ),
                    List.of(),
                    new OperatorSet(),
                    List.of(
                        "% Simple example for termination because of unification without occurs-check and cuts\n",
                        "% Author: Thomas Stroeder\n",
                        "%query: t.\n"
                    )
                )
            }
        };
    }

    @Test(dataProvider="parserData")
    public void parserTest(final Reader reader, final PrologProgram expected) throws IOException {
        Assert.assertEquals(new PrologParser(reader).parse(), expected);
    }

}

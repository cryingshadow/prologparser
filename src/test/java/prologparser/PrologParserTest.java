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
                new StringReader("%query: count(g)\ncount(X) :- X < 0, !.\ncount(X) :- Y is X - 1, count(Y)."),
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
                    List.of("%query: count(g)\n")
                )
            }
        };
    }

    @Test(dataProvider="parserData")
    public void parserTest(final Reader reader, final PrologProgram expected) throws IOException {
        Assert.assertEquals(new PrologParser(reader).parse(), expected);
    }

}

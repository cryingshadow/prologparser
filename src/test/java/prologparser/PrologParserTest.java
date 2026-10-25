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
            }
        };
    }

    @Test(dataProvider="parserData")
    public void parserTest(final Reader reader, final PrologProgram expected) throws IOException {
        Assert.assertEquals(new PrologParser(reader).parse(), expected);
    }

}

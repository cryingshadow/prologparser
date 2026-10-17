package prologparser;

import java.io.*;
import java.util.*;

import org.testng.*;
import org.testng.annotations.*;

public class LexerTest {

    @DataProvider
    public Object[][] tokenizeData() {
        return new Object[][] {
            {
                "app([],YS,YS).\napp([X|XS],YS,[X|ZS]) :- app(XS,YS,ZS).",
                List.of(
                    new PrologToken("app", TokenType.NAME),
                    new PrologToken("(", TokenType.OPEN_PARANTHESIS),
                    new PrologToken("[]", TokenType.EMPTY_LIST),
                    new PrologToken(",", TokenType.COMMA),
                    new PrologToken("YS", TokenType.VARIABLE),
                    new PrologToken(",", TokenType.COMMA),
                    new PrologToken("YS", TokenType.VARIABLE),
                    new PrologToken(")", TokenType.CLOSE_PARANTHESIS),
                    new PrologToken(".", TokenType.FULLSTOP),
                    new PrologToken("\n", TokenType.WHITESPACE),
                    new PrologToken("app", TokenType.NAME),
                    new PrologToken("(", TokenType.OPEN_PARANTHESIS),
                    new PrologToken("[", TokenType.OPEN_BRACKET),
                    new PrologToken("X", TokenType.VARIABLE),
                    new PrologToken("|", TokenType.PIPE),
                    new PrologToken("XS", TokenType.VARIABLE),
                    new PrologToken("]", TokenType.CLOSE_BRACKET),
                    new PrologToken(",", TokenType.COMMA),
                    new PrologToken("YS", TokenType.VARIABLE),
                    new PrologToken(",", TokenType.COMMA),
                    new PrologToken("[", TokenType.OPEN_BRACKET),
                    new PrologToken("X", TokenType.VARIABLE),
                    new PrologToken("|", TokenType.PIPE),
                    new PrologToken("ZS", TokenType.VARIABLE),
                    new PrologToken("]", TokenType.CLOSE_BRACKET),
                    new PrologToken(")", TokenType.CLOSE_PARANTHESIS),
                    new PrologToken(" ", TokenType.WHITESPACE),
                    new PrologToken(":-", TokenType.NAME),
                    new PrologToken(" ", TokenType.WHITESPACE),
                    new PrologToken("app", TokenType.NAME),
                    new PrologToken("(", TokenType.OPEN_PARANTHESIS),
                    new PrologToken("XS", TokenType.VARIABLE),
                    new PrologToken(",", TokenType.COMMA),
                    new PrologToken("YS", TokenType.VARIABLE),
                    new PrologToken(",", TokenType.COMMA),
                    new PrologToken("ZS", TokenType.VARIABLE),
                    new PrologToken(")", TokenType.CLOSE_PARANTHESIS),
                    new PrologToken(".", TokenType.FULLSTOP)
                )
            }
        };
    }

    @Test(dataProvider="tokenizeData")
    public void tokenizeTest(final String input, final List<PrologToken> expected) throws IOException {
        Assert.assertEquals(new Lexer(new StringReader(input)).tokenize(), expected);
    }

}

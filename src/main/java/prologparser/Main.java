package prologparser;

import java.io.*;

public class Main {

    public static void main(final String[] args) throws IOException {
        IO.println(new PrologParser(new FileReader(args[0])).parse());
    }

}

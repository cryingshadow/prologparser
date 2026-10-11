package prologparser;

import java.io.*;

import com.google.gson.*;

public class Main {

    public static final Gson GSON = new Gson();

    public static void main(final String[] args) throws IOException {
        IO.println(Main.GSON.toJson(new PrologParser(new FileReader(args[0])).parse(), PrologProgram.class));
    }

}

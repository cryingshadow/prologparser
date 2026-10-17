package prologparser;


public record PrologToken(String text, TokenType type) {

    public boolean isLayout() {
        return this.type() == TokenType.COMMENT || this.type() == TokenType.WHITESPACE;
    }

}

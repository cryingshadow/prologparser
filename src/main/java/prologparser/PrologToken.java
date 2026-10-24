package prologparser;


public record PrologToken(String text, TokenType type) implements TokenListItem {

    public boolean isLayout() {
        return this.type() == TokenType.COMMENT || this.type() == TokenType.WHITESPACE;
    }

    @Override
    public String name() {
        return this.text();
    }

}

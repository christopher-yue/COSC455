/*
COURSE: COSC455003
Assignment: Program 1

Name: Yue, Christopher
*/

//  ************** REQUIRES JAVA 17 OR ABOVE! (https://adoptium.net/) ************** //
package compiler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public enum TokenSet {
    LET("let"),
    READ("read"),
    WRITE("write"),
    VAR("var"),
    OPEN("("),
    CLOSE(")"),
    EXPR_ASSGN("="),
    SUBR_ASSGN("<-"),
    COMMA(","),
    ADD_OP("-", "+"),
    MULT_OP("*", "/"),
    REL_OP(">", "<", "=="),
    UNTIL("until"),
    REPEAT("repeat"),
    IF("if"),
    ENDIF("endif"),
    ELSE("else"),
    THEN("then"),
    $$, // End of file
    ID,
    NUMBER;

    /**
     * A list of all lexemes for each token.
     */
    private final List<String> lexemeList;

    TokenSet(final String... tokenStrings) {
        this.lexemeList = new ArrayList<>(tokenStrings.length);
        this.lexemeList.addAll(Arrays.asList(tokenStrings));
    }

    /**
     * Get a TokenSet object from the Lexeme string.
     *
     * @param string The String (lexeme) to convert to a compiler.TokenSet
     * @return A compiler.TokenSet object based on the input String (lexeme)
     */
    static TokenSet getTokenFromLexeme(final String string) {
        // Just to be safe…
        final var lexeme = string.trim();

        // An empty string/lexeme should mean no more tokens to process.
        // Return the "end of input maker" if the string is empty.
        if (lexeme.isEmpty()) {
            return $$;
        }

        // Regex for one or more digits optionally followed by and more digits.
        // (doesn't handle "-", "+" etc., only digits)
        // Return the number token if the string represents a number.
        if (lexeme.matches(LexicalAnalyzer.NUMBER_REGEX)) {
            return NUMBER;
        }

        // Search through ALL lexemes looking for a match with early bailout.
        // Return the matching token if found.
        for (var token : TokenSet.values()) {
            if (token.lexemeList.contains(lexeme)) {
                // early bailout from the loop.
                return token;
            }
        }

        // Return "ID" if no match was found.
        return ID;
    }
}

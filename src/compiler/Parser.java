/*
COURSE: COSC455003
Assignment: Program 1

Name: Yue, Christopher
*/

//  ************** REQUIRES JAVA 17 or later! (https://adoptium.net/) ************** //
package compiler;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/*
* GRAMMAR FOR PROGRAM 1:
*
* <PROGRAM> ::= <STMT_LIST> $$
* <STMT_LIST> ::= <STMT> <STMT_LIST> | ε
*
* <STMT> ::= <READ_STMT> | <WRITE_STMT> | <VAR_DECL> | <SUBR_CALL> | <UNTIL_STMT> | <IF_STMT> | <LET> <ID> <ASSGN_STMT>
*
* <READ_STMT> ::= <READ> <ID>
* <WRITE_STMT> ::= <WRITE> <EXPR>
*
* <VAR_DECL> ::= <VAR> <ID>
* <SUBR_CALL> ::= <ID> <OPEN> <ARG_LIST> <CLOSE>
*
* <ASSGN_STMT> ::= <EQUALS> <EXPR> | <EQUALS> <SUBR_CALL>
*
* <ARG_LIST> ::= <EXPR> <ARGS_TAIL>
* <ARGS_TAIL> ::= <COMMA> <ARG_LIST> | ε
*
* <EXPR> ::= <TERM> <TERM_TAIL>
*
* <TERM> ::= <FACTOR> <FACTOR_TAIL>
* <TERM_TAIL> ::= <ADD_OP> <TERM> <TERM_TAIL> | ε
*
* <FACTOR> ::= <OPEN> <EXPR> <CLOSE> | <ID>
* <FACTOR_TAIL> ::= <MULT_OP> <FACTOR> <FACTOR_TAIL> | ε
*
* <CONDITION> ::= <EXPR> <REL_OPER> <EXPR>
*
* <UNTIL_STMT> ::= <UNTIL> <CONDITION> <STMT_LIST> <REPEAT>
*
* <IF_STMT> ::= <IF> <CONDITION> <THEN> <STMT_LIST> <ELSE_STMT> <ENDIF>
*
* <ELSE_STMT> ::= <ELSE> <STMT_LIST> | ε
*
* <ADD_OP> ::= '-' | '+'
* <MULT_OP> ::= '*' | '/'
* <REL_OPER> ::= '>' | '<' || '=='
*
* <COMMA> ::= ','
*
* <OPEN> ::= '('
* <CLOSE> ::= ')'
*
* <EXPR_ASSGN> ::= '='
* <SUBR_ASSGN> ::= '<-'
*
* <IF> ::= 'if'
* <THEN> ::= 'then'
* <ELSE> ::= 'else'
* <ENDIF> ::= 'endif'
*
* <UNTIL> ::= 'until'
* <REPEAT> ::= 'repeat'
*
*/

/**
 * This is the syntax analyzer for the compiler implemented as a recursive
 * descent parser.
 */
class Parser {

    // The lexer, which will provide the tokens
    private final LexicalAnalyzer lexer;

    // The "code generator"
    private final CodeGenerator codeGenerator;

    // List of declared variables
    private final List<String> declaredVars = new ArrayList<>();

    /**
     * This is the constructor for the Parser class which
     * accepts a LexicalAnalyzer, and a CodeGenerator object as parameters.
     *
     * @param lexer         The TokenSet Object
     * @param codeGenerator The CodeGenerator Object
     */
    Parser(LexicalAnalyzer lexer, CodeGenerator codeGenerator) {
        this.lexer = lexer;
        this.codeGenerator = codeGenerator;

        // Change this to automatically prompt to see the Open WebGraphViz dialog or not.
        MAIN.PROMPT_FOR_GRAPHVIZ = true;
    }

    /*
     * Since the "Compiler" portion of the code knows nothing about the start rule,
     * the "analyze" method must invoke the start rule.
     *
     * Begin analyzing...
     *
     * @param treeRoot The tree root.
     */
    public void analyze(TreeNode treeRoot) {
        try {
            // THIS IS OUR START RULE
            PROGRAM(treeRoot);
        } catch (ParseException ex) {
            final String msg = String.format("%s\n", ex.getMessage());
            Logger.getAnonymousLogger().severe(msg);
        }
    }

    // BASE GRAMMAR

    // <PROGRAM> :== <STMT_LIST> $$
    private void PROGRAM(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        // Invoke the first rule
        STMT_LIST(thisNode);

        // Test for the end of input ($$ meta token).
        if (lexer.getCurrentToken() != TokenSet.$$) {
            String currentLexeme = lexer.getCurrentLexeme();
            var errorMessage =
                    "SYNTAX ERROR: 'End of File' was expected but '%s' was found.".formatted(currentLexeme);
            codeGenerator.syntaxError(errorMessage, thisNode);
        }
    }

    // <STMT_LIST> ::= <STMT> <STMT_LIST> | ε
    private void STMT_LIST(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.READ || lexer.getCurrentToken() == TokenSet.WRITE || lexer.getCurrentToken() == TokenSet.VAR || lexer.getCurrentToken() == TokenSet.ID || lexer.getCurrentToken() == TokenSet.UNTIL || lexer.getCurrentToken() == TokenSet.IF || lexer.getCurrentToken() == TokenSet.LET) {
            STMT(thisNode);
            STMT_LIST(thisNode);
        } else{
            EMPTY(thisNode);
        }
    }

    // <STMT> ::= <READ_STMT> | <WRITE_STMT> | <VAR_DECL> | <SUBR_CALL> | <UNTIL_STMT> | <IF_STMT> | <LET> <ID> <ASSGN_STMT>
    private void STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.READ)  {
            READ_STMT(thisNode);
        } else if (lexer.getCurrentToken() == TokenSet.WRITE) {
            WRITE_STMT(thisNode);
        } else if (lexer.getCurrentToken() == TokenSet.VAR) {
            VAR_DECL(thisNode);
        } else if (lexer.getCurrentToken() == TokenSet.ID) {
            SUBR_CALL(thisNode);
        } else if (lexer.getCurrentToken() == TokenSet.UNTIL) {
            UNTIL_STMT(thisNode);
        } else if (lexer.getCurrentToken() == TokenSet.IF) {
            IF_STMT(thisNode);
        } else {
            MATCH(thisNode, TokenSet.LET);
            String currentLexeme = lexer.getCurrentLexeme();
            if (!declaredVars.contains(currentLexeme)) {
                final var errorMessage = "SYNTAX ERROR: Variable '%s' is used before declaration.".formatted(currentLexeme);
                codeGenerator.syntaxError(errorMessage, thisNode);
            } else {
                MATCH(thisNode, TokenSet.ID);
                ASSGN_STMT(thisNode);
            }
        }
    }

    // <READ_STMT> ::= <READ> <ID>
    private void READ_STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        MATCH(thisNode, TokenSet.READ);
        String currentLexeme = lexer.getCurrentLexeme();
        if (!declaredVars.contains(currentLexeme)) {
            final var errorMessage = "SYNTAX ERROR: Variable '%s' is used before declaration.".formatted(currentLexeme);
            codeGenerator.syntaxError(errorMessage, thisNode);
        } else {
            MATCH(thisNode, TokenSet.ID);
        }
    }

    // <WRITE_STMT> ::= <WRITE> <EXPR>
    private void WRITE_STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        MATCH(thisNode, TokenSet.WRITE);
        EXPR(thisNode);
    }

    // <VAR_DECL> ::= <VAR> <ID>
    private void VAR_DECL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        MATCH(thisNode, TokenSet.VAR);
        String currentLexeme = lexer.getCurrentLexeme();
        if (declaredVars.contains(currentLexeme)) {
            final var errorMessage = "SEMANTIC ERROR: Variable '%s' is already declared.".formatted(currentLexeme);
            codeGenerator.syntaxError(errorMessage, thisNode);
        } else {
            declaredVars.add(currentLexeme);
        }
        MATCH(thisNode, TokenSet.ID);
    }

    // <SUBR_CALL> ::= <ID> <OPEN> <ARG_LIST> <CLOSE>
    private void SUBR_CALL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        MATCH(thisNode, TokenSet.ID);
        MATCH(thisNode, TokenSet.OPEN);
        ARG_LIST(thisNode);
        MATCH(thisNode, TokenSet.CLOSE);
    }

    // <ASSGN_STMT> ::= <EQUALS> <EXPR> | <EQUALS> <SUBR_CALL>
    private void ASSGN_STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.EXPR_ASSGN) {
            MATCH(thisNode, TokenSet.EXPR_ASSGN);
            EXPR(thisNode);
        } else {
            MATCH(thisNode, TokenSet.SUBR_ASSGN);
            SUBR_CALL(thisNode);
        }
    }

    // <ARG_LIST> ::= <EXPR> <ARGS_TAIL>
    private void ARG_LIST(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        EXPR(thisNode);
        ARGS_TAIL(thisNode);
    }

    // <ARGS_TAIL> ::= <COMMA> <ARG_LIST> | ε
    private void ARGS_TAIL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.COMMA) {
            MATCH(thisNode, TokenSet.COMMA);
            ARG_LIST(thisNode);
        } else {
            EMPTY(thisNode);
        }
    }

    // <EXPR> ::= <TERM> <TERM_TAIL>
    private void EXPR(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        TERM(thisNode);
        TERM_TAIL(thisNode);
    }

    // <TERM> ::= <FACTOR> <FACTOR_TAIL>
    private void TERM(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        FACTOR(thisNode);
        FACTOR_TAIL(thisNode);
    }

    // <TERM_TAIL> ::= <ADD_OP> <TERM> <TERM_TAIL> | ε
    private void TERM_TAIL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.ADD_OP) {
            MATCH(thisNode, TokenSet.ADD_OP);
            TERM(thisNode);
            TERM_TAIL(thisNode);
        } else {
            EMPTY(thisNode);
        }
    }

    // <FACTOR> ::= <OPEN> <EXPR> <CLOSE> | <NUMBER> | <ID>
    private void FACTOR(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.OPEN) {
            MATCH(thisNode, TokenSet.OPEN);
            EXPR(thisNode);
            MATCH(thisNode, TokenSet.CLOSE);
        } else if (lexer.getCurrentToken() == TokenSet.NUMBER) {
            MATCH(thisNode, TokenSet.NUMBER);
        } else {
            String currentLexeme = lexer.getCurrentLexeme();
            if (!declaredVars.contains(currentLexeme)) {
                final var errorMessage = "ERROR: Variable '%s' is used before declaration.".formatted(currentLexeme);
                codeGenerator.syntaxError(errorMessage, thisNode);
            } else {
                MATCH(thisNode, TokenSet.ID);
            }
        }
    }

    // <FACTOR_TAIL> ::= <MULT_OP> <FACTOR> <FACTOR_TAIL> | ε
    private void FACTOR_TAIL(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if(lexer.getCurrentToken() == TokenSet.MULT_OP) {
            MATCH(thisNode, TokenSet.MULT_OP);
            FACTOR(thisNode);
            FACTOR_TAIL(thisNode);
        } else {
            EMPTY(thisNode);
        }
    }

    // <CONDITION> ::= <EXPR> <REL_OPER> <EXPR>
    private void CONDITION(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        EXPR(thisNode);
        MATCH(thisNode, TokenSet.REL_OP);
        EXPR(thisNode);
    }

    // UNTIL REPEAT STRUCTURE

    // <UNTIL_STMT> ::= <UNTIL> <CONDITION> <STMT_LIST> <REPEAT>
    private void UNTIL_STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        MATCH(thisNode, TokenSet.UNTIL);
        CONDITION(thisNode);
        STMT_LIST(thisNode);
        MATCH(thisNode, TokenSet.REPEAT);
    }

    // IF THEN ELSE STRUCTURE

    // <IF_STMT> ::= <IF> <CONDITION> <THEN> <STMT_LIST> <ELSE_STMT> <ENDIF>
    private void IF_STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        MATCH(thisNode, TokenSet.IF);
        CONDITION(thisNode);
        MATCH(thisNode, TokenSet.THEN);
        STMT_LIST(thisNode);
        ELSE_STMT(thisNode);
        MATCH(thisNode, TokenSet.ENDIF);
    }

    // <ELSE_STMT> ::= <ELSE> <STMT_LIST> | ε
    private void ELSE_STMT(final TreeNode parentNode) throws ParseException {
        final TreeNode thisNode = codeGenerator.addNonTerminalToTree(parentNode);

        if (lexer.getCurrentToken() == TokenSet.ELSE) {
            MATCH(thisNode, TokenSet.ELSE);
            STMT_LIST(thisNode);
        } else {
            EMPTY(thisNode);
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////

    /**
     * Add an EMPTY terminal node (result of an Epsilon Production) to the parse tree.
     * Mainly, this is just done for better visualizing the complete parse tree.
     *
     * @param parentNode The parent of the terminal node.
     */
    private void EMPTY(final TreeNode parentNode) {
        codeGenerator.addEmptyToTree(parentNode);
    }

    /**
     * Match the current token with the expected token.
     * If they match, add the token to the parse tree, otherwise throw an exception.
     *
     * @param currentNode     The current terminal node.
     * @param expectedToken   The token to be matched.
     * @throws ParseException Thrown if the token does not match the expected token.
     */
    private void MATCH(final TreeNode currentNode, final TokenSet expectedToken) throws ParseException {
        final var currentToken = lexer.getCurrentToken();
        final var currentLexeme = lexer.getCurrentLexeme();

        if (currentToken == expectedToken) {
            codeGenerator.addTerminalToTree(currentNode, currentToken, currentLexeme);
            lexer.advanceToken();
        } else {
            final var errorMessage = "SYNTAX ERROR: '%s' was expected\nbut '%s' was found (%s)."
                    .formatted(expectedToken, currentLexeme, currentToken);

            codeGenerator.syntaxError(errorMessage, currentNode);
        }
    }
}

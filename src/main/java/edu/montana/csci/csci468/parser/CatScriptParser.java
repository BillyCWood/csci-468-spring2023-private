package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.awt.image.MemoryImageSource;
import java.util.LinkedList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        if(tokens.match(PRINT)) {
            Statement printStmt = parsePrintStatement();
            return printStmt;
        }
        else if(tokens.match(FOR)) {
            Statement forStmt = parseForStatement();
            return forStmt;
        }
        else if (tokens.match(IF)){
            Statement ifStmt = parseIfStatement();
            return ifStmt;
        }
        else if (tokens.match(VAR)) {
            Statement varStmt = parseVarStatement();
            return varStmt;
        }
        else if (tokens.match(IDENTIFIER)) {
            Statement assignStmt = parseAssignmentStatement();
            return assignStmt;
        }

        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }


    private Statement parseForStatement(){
        if(tokens.match(FOR)){

            ForStatement forStatement = new ForStatement();
            List<Statement> stmtList = new LinkedList<>();
            forStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, forStatement);
            forStatement.setVariableName(tokens.consumeToken().getStringValue());
            require(IN, forStatement);
            forStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, forStatement);
            require(LEFT_BRACE, forStatement);
            while(tokens.hasMoreTokens()){
                if(tokens.match(RIGHT_BRACE)){
                    break;
                }
                else{stmtList.add(parseProgramStatement());}
            }
            forStatement.setBody(stmtList);
            forStatement.setEnd(require(RIGHT_BRACE, forStatement));
            return forStatement;
        }else{return null;}
    }



    private Statement parseIfStatement(){
        if(tokens.match(IF)){
            IfStatement ifStatement = new IfStatement();
            List<Statement> stmtList = new LinkedList<>();
            ifStatement.setStart(tokens.consumeToken());
            require(LEFT_PAREN, ifStatement);
            ifStatement.setExpression(parseExpression());
            require(RIGHT_PAREN, ifStatement);
            require(LEFT_BRACE, ifStatement);

            while(tokens.hasMoreTokens()){
                if(tokens.match(RIGHT_BRACE)){
                    break;
                }
                else{stmtList.add(parseProgramStatement());}
            }
            ifStatement.setTrueStatements(stmtList);
            ifStatement.setEnd(require(RIGHT_BRACE, ifStatement));

            return ifStatement;

        }else{return null;}
    }

/*
    private Statement parseIfElseStatement(){

    }


    private Statement parseElseStatement(){

    }


    private Statement parseVarStatement(){

    }


*/
    private Statement parseVarStatement(){
        if(tokens.match(VAR)){

            VariableStatement variableStatement = new VariableStatement();
            variableStatement.setStart(tokens.consumeToken());
            variableStatement.setVariableName(require(IDENTIFIER, variableStatement).getStringValue());

            if(tokens.match(COLON)){
                tokens.consumeToken();
                if(tokens.match("int")) {
                    tokens.consumeToken();
                    variableStatement.setExplicitType(CatscriptType.INT);
                }
                else if(tokens.match("bool")) {
                    tokens.consumeToken();
                    variableStatement.setExplicitType(CatscriptType.BOOLEAN);
                }
                else if(tokens.match("string")) {
                    tokens.consumeToken();
                    variableStatement.setExplicitType(CatscriptType.STRING);
                }
                else if(tokens.match("object")) {
                    tokens.consumeToken();
                    variableStatement.setExplicitType(CatscriptType.OBJECT);
                }
            }

            require(EQUAL, variableStatement);
            variableStatement.setExpression(parseExpression());
            variableStatement.setEnd(tokens.getCurrentToken());

            return variableStatement;

        }else{return null;}
    }


    private Statement parseAssignmentStatement(){
        if(tokens.match(IDENTIFIER)){
            AssignmentStatement assignmentStatement = new AssignmentStatement();
            assignmentStatement.setVariableName(tokens.getCurrentToken().getStringValue());
            assignmentStatement.setStart(require(IDENTIFIER, assignmentStatement));
            require(EQUAL, assignmentStatement);
            assignmentStatement.setExpression(parseExpression());
            assignmentStatement.setEnd(tokens.getCurrentToken());
            return assignmentStatement;
        }else {return null;}
    }



    private Statement parseFunctionCallStatement(){
        return null;
    }




    private Statement parseFunctionDefStatement(){
        return null;
    }


    private Statement parseReturnStatement(){
        return null;
    }



    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        //parseExpression -> parseEquality -> parseComparison
        //-> parseAdditive -> parseFactor -> parseUnary -> parsePrimary
        return parseEqualityExpression();
    }

    private Expression parseEqualityExpression(){
        Expression expression = parseComparisonExpression();
        while (tokens.match(EQUAL_EQUAL, BANG_EQUAL)){
            Token operator = tokens.consumeToken();
            final Expression rhs = parseUnaryExpression();
            EqualityExpression equalityExpression = new EqualityExpression(operator, expression, rhs);
            equalityExpression.setStart(expression.getStart());
            equalityExpression.setEnd(rhs.getEnd());
            expression = equalityExpression;

        }
        return expression;
    }




    private Expression parseComparisonExpression(){
        Expression expression = parseAdditiveExpression();
        while (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)){
            Token operator = tokens.consumeToken();
            final Expression rhs = parseUnaryExpression();
            ComparisonExpression comparisonExpression = new ComparisonExpression(operator, expression, rhs);
            comparisonExpression.setStart(expression.getStart());
            comparisonExpression.setEnd(rhs.getEnd());
            expression = comparisonExpression;

        }
        return expression;
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression(){
        Expression expression = parseUnaryExpression();
        while (tokens.match(STAR, SLASH)){
            Token operator = tokens.consumeToken();
            final Expression rhs = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rhs);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rhs.getEnd());
            expression = factorExpression;

        }
        return expression;
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        }
        else {
            return parsePrimaryExpression();
        }
    }

    private Expression parsePrimaryExpression() {
        //Identifier
        if(tokens.match(IDENTIFIER)){
            return parseIdentifier();
        }
        //Integer Literal
        else if (tokens.match(INTEGER)) {
            return parseIntegerLiteral();
        }
        //String Literal
        else if(tokens.match(STRING)){
            return parseStringLiteral();
        }
        //Boolean
        else if(tokens.match(TRUE) || tokens.match(FALSE)){
            return parseBooleanLiteral();
        }
        //Null
        else if(tokens.match(NULL)){
            return parseNullLiteral();
        }
        //List
        else if(tokens.match(LEFT_BRACKET)) {
           return parseListLiteral();
        }
        else if(tokens.match(LEFT_PAREN)){
            return parseAdditiveWithParentheses();
        }
        else {
            SyntaxErrorExpression syntaxErrorExpression = new SyntaxErrorExpression(tokens.consumeToken());
            return syntaxErrorExpression;
        }
    }

    private Expression parseIdentifier(){
        Token identifier = tokens.consumeToken();
        if(tokens.match(LEFT_PAREN)){
            return parseFunctionCall(identifier);
        }
        else{
            IdentifierExpression identifierExpression = new IdentifierExpression(identifier.getStringValue());
            identifierExpression.setToken(identifier);
            return identifierExpression;
        }
    }

    private Expression parseIntegerLiteral(){
        Token integerToken = tokens.consumeToken();
        IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(integerToken.getStringValue());
        integerExpression.setToken(integerToken);
        return integerExpression;
    }

    private Expression parseStringLiteral(){
        Token stringToken = tokens.consumeToken();
        StringLiteralExpression stringExpression = new StringLiteralExpression(stringToken.getStringValue());
        stringExpression.setToken(stringToken);
        return stringExpression;
    }

    private Expression parseBooleanLiteral(){
        Token booleanToken = tokens.consumeToken();
        BooleanLiteralExpression booleanExpression = new BooleanLiteralExpression(Boolean.valueOf(booleanToken.getStringValue()));
        booleanExpression.setToken(booleanToken);
        return booleanExpression;
    }

    private Expression parseNullLiteral(){
        Token nullToken = tokens.consumeToken();
        NullLiteralExpression nullExpression = new NullLiteralExpression();
        nullExpression.setToken(nullToken);
        return nullExpression;
    }

    private Expression parseFunctionCall(Token identifier) {

        tokens.consumeToken();
        //arguments list
        List<Expression> args = new LinkedList<>();

        //check if no args
        if(tokens.match(RIGHT_PAREN)){
            tokens.consumeToken();
        }

        //get arguments
        while (tokens.hasMoreTokens() && !tokens.match(RIGHT_PAREN)) {
            if(!tokens.match(COMMA)) {
                Expression arg = parsePrimaryExpression();
                args.add(arg);
            }
            else{tokens.matchAndConsume(COMMA);}
        }


        FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifier.getStringValue(), args);
        functionCallExpression.setToken(identifier);
        functionCallExpression.setEnd(require(RIGHT_PAREN, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST));

        return functionCallExpression;

    }

    private Expression parseListLiteral(){
        Token currentToken = tokens.consumeToken();
        List<Expression> elements = new LinkedList<>();

        if(tokens.match(LEFT_BRACKET)){
            currentToken = tokens.consumeToken();
        }
        else if(tokens.hasMoreTokens()) {
            while (tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACKET)) {
                if (!tokens.match(COMMA)) {
                    elements.add(parsePrimaryExpression());
                } else {
                    currentToken = tokens.consumeToken();
                }
            }
        }

        ListLiteralExpression listLiteralExpression = new ListLiteralExpression(elements);
        listLiteralExpression.setToken(currentToken);
        listLiteralExpression.setEnd(require(RIGHT_BRACKET,listLiteralExpression,ErrorType.UNTERMINATED_LIST));

        return listLiteralExpression;
    }

    private Expression parseAdditiveWithParentheses(){
        Token currentToken = tokens.consumeToken();
        Expression expression = parseAdditiveExpression();
        ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(expression);
        parenthesizedExpression.setToken(currentToken);
        parenthesizedExpression.setEnd(require(RIGHT_PAREN,parenthesizedExpression));
        return parenthesizedExpression;
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}

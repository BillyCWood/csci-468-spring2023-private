package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

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
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
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

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseAdditiveExpression();
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            expression = additiveExpression;
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
        //Multiply and Divide
        else if(tokens.match(STAR, SLASH)){
            Token token = tokens.consumeToken();
            Expression rhs = parseExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token,rhs);
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
        //Unterminated Function Call w/ No Args
        //TODO
        else if(tokens.hasMoreTokens() == false && tokens.match(RIGHT_PAREN) == false){

        }
        else {
            //get arguments
            while (tokens.hasMoreTokens() && !tokens.match(RIGHT_PAREN)) {
                if(!tokens.match(COMMA)) {
                    Expression arg = parsePrimaryExpression();
                    args.add(arg);
                }
                else{tokens.matchAndConsume(COMMA);}
            }
            if(tokens.match(RIGHT_PAREN)){
                tokens.consumeToken();
            }
            //Unterminated Function Call w/ Args
            //TODO
            else{

            }

        }
        FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifier.getStringValue(), args);
        functionCallExpression.setToken(identifier);
        return functionCallExpression;

    }

    private Expression parseListLiteral(){
        Token currentToken = tokens.consumeToken();
        List<Expression> elements = new LinkedList<>();

        if(tokens.match(RIGHT_BRACKET)){
            currentToken = tokens.consumeToken();
        }
        else if(tokens.hasMoreTokens()){
            while(tokens.hasMoreTokens() && !tokens.match(RIGHT_BRACKET)){
                if(!tokens.match(COMMA)){
                    elements.add(parsePrimaryExpression());
                }
                else{
                    currentToken=tokens.consumeToken();
                }
            }
            if(tokens.match(RIGHT_BRACKET)){
                currentToken = tokens.consumeToken();
            }
            //Unterminated List w/ Elements
            else{

            }
        }
        //Unterminated List w/ No Elements
        else{

        }

        ListLiteralExpression listLiteralExpression = new ListLiteralExpression(elements);
        listLiteralExpression.setToken(currentToken);
        return listLiteralExpression;
    }


    //private Expression parseListLiteral(){}


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

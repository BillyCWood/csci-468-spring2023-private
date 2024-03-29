package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.awt.image.MemoryImageSource;
import java.util.ArrayList;
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
        Statement stmt = parsePrintStatement();
        if (stmt != null) {
            return stmt;
        }

        stmt = parseForStatement();
        if (stmt != null) {
            return stmt;
        }

        stmt = parseIfStatement();
        if (stmt != null){
            return stmt;
        }

        stmt = parseVarStatement();
        if (stmt != null) {
            return stmt;
        }

        stmt = parseAssignmentOrFunctionCallStatement();
        if (stmt != null) {
            return stmt;
        }

        stmt = parseFunctionDefStatement();
        if (stmt != null){
            return stmt;
        }


        if(currentFunctionDefinition != null){
            stmt = parseReturnStatement();
            if (stmt != null) {
                return stmt;
            }
        }

        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parseAssignmentOrFunctionCallStatement() {
        if(tokens.match(IDENTIFIER)){
            Token name = tokens.consumeToken();
            if (tokens.match(EQUAL)) {
                Statement assignStmt = parseAssignmentStatement(name);
                return assignStmt;
            }else if (tokens.match(LEFT_PAREN)){
                Statement funcStmt = parseFunctionCallStatement(name);
                return funcStmt;
            }
        }
        return null;
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(require(PRINT, printStatement));

            require(LEFT_PAREN, printStatement);
            while (tokens.hasMoreTokens() && !tokens.match(RIGHT_PAREN)){
                Expression expression = parseExpression();
                printStatement.setExpression(expression);
            }
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
            require(RIGHT_BRACE, ifStatement);
            if(tokens.match(ELSE)){
                List<Statement> elseStmtList = new LinkedList<>();
                tokens.consumeToken();
                require(LEFT_BRACE, ifStatement);
                while(tokens.hasMoreTokens()){
                    if(tokens.match(RIGHT_BRACE)){
                        break;
                    }
                    else{elseStmtList.add(parseProgramStatement());}
                }
                ifStatement.setElseStatements(elseStmtList);
                require(RIGHT_BRACE, ifStatement);
            }

            ifStatement.setEnd(tokens.getCurrentToken());
            return ifStatement;

        }else{return null;}
    }


    private Statement parseVarStatement(){
        if(tokens.match(VAR)){

            VariableStatement variableStatement = new VariableStatement();
            variableStatement.setStart(tokens.consumeToken());
            variableStatement.setVariableName(require(IDENTIFIER, variableStatement).getStringValue());

            if(tokens.match(COLON)){
                tokens.consumeToken();
                variableStatement.setExplicitType(parseType());
            }

            require(EQUAL, variableStatement);
            variableStatement.setExpression(parseExpression());
            variableStatement.setEnd(tokens.getCurrentToken());

            return variableStatement;

        }else{return null;}
    }


    private Statement parseAssignmentStatement(Token identifier){
        if(tokens.match(EQUAL)){
            AssignmentStatement assignmentStatement = new AssignmentStatement();
            assignmentStatement.setVariableName(identifier.getStringValue());
            assignmentStatement.setStart(identifier);
            require(EQUAL, assignmentStatement);
            assignmentStatement.setExpression(parseExpression());
            assignmentStatement.setEnd(tokens.getCurrentToken());
            return assignmentStatement;
        }else {return null;}
    }



    private Statement parseFunctionCallStatement(Token identifier){
        if(tokens.match(LEFT_PAREN)){
            FunctionCallStatement functionCallStatement = new FunctionCallStatement(parseFunctionCall(identifier));
            return functionCallStatement;
        }
        else{return null;}
    }


    private CatscriptType parseType(){
        //tokens.consumeToken();
        CatscriptType type = CatscriptType.NULL;
        if(tokens.match("int")) {
            tokens.consumeToken();
            type = CatscriptType.INT;
        }
        else if(tokens.match("bool")) {
            tokens.consumeToken();
            type = CatscriptType.BOOLEAN;
        }
        else if(tokens.match("string")) {
            tokens.consumeToken();
            type = CatscriptType.STRING;
        }
        else if(tokens.match("object")) {
            tokens.consumeToken();
            type = CatscriptType.OBJECT;
        }
        else if(tokens.match("list")){
            tokens.consumeToken();//list
            tokens.consumeToken();//<
            type=parseType();
            type=CatscriptType.getListType(type);//type
            tokens.consumeToken();//>
        }
        return type;

    }


    private Statement parseFunctionDefStatement(){
        if(tokens.match(FUNCTION)){

            FunctionDefinitionStatement functionDefinitionStatement = new FunctionDefinitionStatement();
            LinkedList<Statement> stmts = new LinkedList<>();

            functionDefinitionStatement.setStart(require(FUNCTION, functionDefinitionStatement));
            functionDefinitionStatement.setName(require(IDENTIFIER, functionDefinitionStatement).getStringValue());
            require(LEFT_PAREN, functionDefinitionStatement);

            //PARAMS
            TypeLiteral type = new TypeLiteral();
            while (!tokens.match(RIGHT_PAREN) && tokens.hasMoreTokens()){

                //parse arg name
                Token argName = tokens.consumeToken();

                //check if type
                if(tokens.match(COLON)){
                    //parse type
                    tokens.consumeToken();
                    type.setType(parseType());
                    functionDefinitionStatement.addParameter(argName.getStringValue(), type);
                }else{functionDefinitionStatement.addParameter(argName.getStringValue(), null);}

                //parse comma
                if(tokens.match(COMMA)){
                    tokens.consumeToken();
                }
            }
            require(RIGHT_PAREN,functionDefinitionStatement);
            if(tokens.match(COLON)){
                type.setType(parseType());
                functionDefinitionStatement.setType(type);
            }else{
                type.setType(CatscriptType.NULL);
                functionDefinitionStatement.setType(type);
            }

            if (tokens.match(COLON)){
                tokens.consumeToken();
                type.setType(parseType());
                functionDefinitionStatement.setType(type);
            }



            //BODY
            require(LEFT_BRACE,functionDefinitionStatement);

            currentFunctionDefinition = functionDefinitionStatement;
            while(!tokens.match(RIGHT_BRACE) && tokens.hasMoreTokens()){
                stmts.add(parseProgramStatement());
            }

            currentFunctionDefinition = null;

            require(RIGHT_BRACE, functionDefinitionStatement);
            functionDefinitionStatement.setBody(stmts);
            functionDefinitionStatement.setEnd(tokens.getCurrentToken());
            return functionDefinitionStatement;
        }

        return null;
    }


    private Statement parseReturnStatement(){

        if(tokens.match(RETURN)){

            ReturnStatement returnStatement = new ReturnStatement();
            returnStatement.setStart(require(RETURN, returnStatement));
            returnStatement.setFunctionDefinition(currentFunctionDefinition);
            if (!tokens.match(RIGHT_BRACE)){
                returnStatement.setExpression(parseExpression());
            }
            else{returnStatement.setExpression(new NullLiteralExpression());}

            returnStatement.setEnd(tokens.getCurrentToken());
            return returnStatement;

        }

        else{return null;}
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

    private FunctionCallExpression parseFunctionCall(Token identifier) {

        tokens.consumeToken();
        //arguments list
        List<Expression> args = new LinkedList<>();

        //get arguments
        while (tokens.hasMoreTokens() && !tokens.match(RIGHT_PAREN)) {
            if(!tokens.match(COMMA)) {
                Expression arg = parseExpression();
                args.add(arg);
            }
            else{tokens.matchAndConsume(COMMA);}
        }


        FunctionCallExpression functionCallExpression = new FunctionCallExpression(identifier.getStringValue(), args);
        functionCallExpression.setToken(identifier);
        require(RIGHT_PAREN, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST);
        functionCallExpression.setEnd(tokens.getCurrentToken());

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

package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenType;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;

import java.util.Objects;

public class EqualityExpression extends Expression {

    private final Token operator;
    private final Expression leftHandSide;
    private final Expression rightHandSide;

    public EqualityExpression(Token operator, Expression leftHandSide, Expression rightHandSide) {
        this.leftHandSide = addChild(leftHandSide);
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }

    public Expression getLeftHandSide() {
        return leftHandSide;
    }

    public Expression getRightHandSide() {
        return rightHandSide;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    public boolean isEqual() {
        return operator.getType().equals(TokenType.EQUAL_EQUAL);
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        leftHandSide.validate(symbolTable);
        rightHandSide.validate(symbolTable);
    }

    @Override
    public CatscriptType getType() {
        return CatscriptType.BOOLEAN;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {

        if(isEqual()){
            return leftHandSide.evaluate(runtime) == rightHandSide.evaluate(runtime);
        }
        else{
            return leftHandSide.evaluate(runtime) != rightHandSide.evaluate(runtime);
        }

    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }
    @Override
    public void compile(ByteCodeGenerator code) {

        getLeftHandSide().compile(code);
        box(code, getLeftHandSide().getType());
        getRightHandSide().compile(code);
        box(code, getRightHandSide().getType());

        Label setTrue = new Label();
        Label end = new Label();

        if(isEqual()) {
            code.addMethodInstruction(Opcodes.INVOKESTATIC,
                    ByteCodeGenerator.internalNameFor(Objects.class),
                    "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
            code.addJumpInstruction(Opcodes.GOTO, end);
        }else{
            code.addMethodInstruction(Opcodes.INVOKESTATIC,
                    ByteCodeGenerator.internalNameFor(Objects.class),
                    "equals", "(Ljava/lang/Object;Ljava/lang/Object;)Z");
        }
        code.addLabel(setTrue);
        code.addInstruction(Opcodes.POP);
        code.pushConstantOntoStack(true);

        code.addLabel(end);

    }


}

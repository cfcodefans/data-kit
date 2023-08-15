// Generated from D:/workspace/study/data-kit/lang-ex-study/src/main/resources\arithmetic_v1.g4 by ANTLR 4.12.0
package tool;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link arithmetic_v1Parser}.
 */
public interface arithmetic_v1Listener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link arithmetic_v1Parser#input}.
	 * @param ctx the parse tree
	 */
	void enterInput(arithmetic_v1Parser.InputContext ctx);
	/**
	 * Exit a parse tree produced by {@link arithmetic_v1Parser#input}.
	 * @param ctx the parse tree
	 */
	void exitInput(arithmetic_v1Parser.InputContext ctx);
	/**
	 * Enter a parse tree produced by {@link arithmetic_v1Parser#sign}.
	 * @param ctx the parse tree
	 */
	void enterSign(arithmetic_v1Parser.SignContext ctx);
	/**
	 * Exit a parse tree produced by {@link arithmetic_v1Parser#sign}.
	 * @param ctx the parse tree
	 */
	void exitSign(arithmetic_v1Parser.SignContext ctx);
	/**
	 * Enter a parse tree produced by {@link arithmetic_v1Parser#expr}.
	 * @param ctx the parse tree
	 */
	void enterExpr(arithmetic_v1Parser.ExprContext ctx);
	/**
	 * Exit a parse tree produced by {@link arithmetic_v1Parser#expr}.
	 * @param ctx the parse tree
	 */
	void exitExpr(arithmetic_v1Parser.ExprContext ctx);
	/**
	 * Enter a parse tree produced by {@link arithmetic_v1Parser#relop}.
	 * @param ctx the parse tree
	 */
	void enterRelop(arithmetic_v1Parser.RelopContext ctx);
	/**
	 * Exit a parse tree produced by {@link arithmetic_v1Parser#relop}.
	 * @param ctx the parse tree
	 */
	void exitRelop(arithmetic_v1Parser.RelopContext ctx);
	/**
	 * Enter a parse tree produced by {@link arithmetic_v1Parser#primary}.
	 * @param ctx the parse tree
	 */
	void enterPrimary(arithmetic_v1Parser.PrimaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link arithmetic_v1Parser#primary}.
	 * @param ctx the parse tree
	 */
	void exitPrimary(arithmetic_v1Parser.PrimaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link arithmetic_v1Parser#unary}.
	 * @param ctx the parse tree
	 */
	void enterUnary(arithmetic_v1Parser.UnaryContext ctx);
	/**
	 * Exit a parse tree produced by {@link arithmetic_v1Parser#unary}.
	 * @param ctx the parse tree
	 */
	void exitUnary(arithmetic_v1Parser.UnaryContext ctx);
	/**
	 * Enter a parse tree produced by {@link arithmetic_v1Parser#variable}.
	 * @param ctx the parse tree
	 */
	void enterVariable(arithmetic_v1Parser.VariableContext ctx);
	/**
	 * Exit a parse tree produced by {@link arithmetic_v1Parser#variable}.
	 * @param ctx the parse tree
	 */
	void exitVariable(arithmetic_v1Parser.VariableContext ctx);
}
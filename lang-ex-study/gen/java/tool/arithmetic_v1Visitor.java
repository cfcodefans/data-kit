// Generated from D:/workspace/study/data-kit/lang-ex-study/src/main/resources\arithmetic_v1.g4 by ANTLR 4.12.0
package tool;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link arithmetic_v1Parser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface arithmetic_v1Visitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link arithmetic_v1Parser#input}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitInput(arithmetic_v1Parser.InputContext ctx);
	/**
	 * Visit a parse tree produced by {@link arithmetic_v1Parser#sign}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitSign(arithmetic_v1Parser.SignContext ctx);
	/**
	 * Visit a parse tree produced by {@link arithmetic_v1Parser#expr}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitExpr(arithmetic_v1Parser.ExprContext ctx);
	/**
	 * Visit a parse tree produced by {@link arithmetic_v1Parser#relop}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitRelop(arithmetic_v1Parser.RelopContext ctx);
	/**
	 * Visit a parse tree produced by {@link arithmetic_v1Parser#primary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitPrimary(arithmetic_v1Parser.PrimaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link arithmetic_v1Parser#unary}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitUnary(arithmetic_v1Parser.UnaryContext ctx);
	/**
	 * Visit a parse tree produced by {@link arithmetic_v1Parser#variable}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitVariable(arithmetic_v1Parser.VariableContext ctx);
}
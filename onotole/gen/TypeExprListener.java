// Generated from C:/Users/ericsson/IdeaProjects/research/onotole/src\TypeExpr.g4 by ANTLR 4.9.1
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TypeExprParser}.
 */
public interface TypeExprListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TypeExprParser#type}.
	 * @param ctx the parse tree
	 */
	void enterType(TypeExprParser.TypeContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeExprParser#type}.
	 * @param ctx the parse tree
	 */
	void exitType(TypeExprParser.TypeContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeExprParser#typeParamList}.
	 * @param ctx the parse tree
	 */
	void enterTypeParamList(TypeExprParser.TypeParamListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeExprParser#typeParamList}.
	 * @param ctx the parse tree
	 */
	void exitTypeParamList(TypeExprParser.TypeParamListContext ctx);
}
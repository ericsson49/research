// Generated from C:/Users/ericsson/IdeaProjects/research/onotole/src\TypeExpr.g4 by ANTLR 4.9.1
package antlr;
import org.antlr.v4.runtime.tree.ParseTreeListener;

/**
 * This interface defines a complete listener for a parse tree produced by
 * {@link TypeExprParser}.
 */
public interface TypeExprListener extends ParseTreeListener {
	/**
	 * Enter a parse tree produced by {@link TypeExprParser#clsName}.
	 * @param ctx the parse tree
	 */
	void enterClsName(TypeExprParser.ClsNameContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeExprParser#clsName}.
	 * @param ctx the parse tree
	 */
	void exitClsName(TypeExprParser.ClsNameContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeExprParser#funDecl}.
	 * @param ctx the parse tree
	 */
	void enterFunDecl(TypeExprParser.FunDeclContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeExprParser#funDecl}.
	 * @param ctx the parse tree
	 */
	void exitFunDecl(TypeExprParser.FunDeclContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeExprParser#arg}.
	 * @param ctx the parse tree
	 */
	void enterArg(TypeExprParser.ArgContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeExprParser#arg}.
	 * @param ctx the parse tree
	 */
	void exitArg(TypeExprParser.ArgContext ctx);
	/**
	 * Enter a parse tree produced by {@link TypeExprParser#argList}.
	 * @param ctx the parse tree
	 */
	void enterArgList(TypeExprParser.ArgListContext ctx);
	/**
	 * Exit a parse tree produced by {@link TypeExprParser#argList}.
	 * @param ctx the parse tree
	 */
	void exitArgList(TypeExprParser.ArgListContext ctx);
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
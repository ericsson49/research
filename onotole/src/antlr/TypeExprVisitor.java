// Generated from C:/Users/ericsson/IdeaProjects/research/onotole/src\TypeExpr.g4 by ANTLR 4.9.1
package antlr;
import org.antlr.v4.runtime.tree.ParseTreeVisitor;

/**
 * This interface defines a complete generic visitor for a parse tree produced
 * by {@link TypeExprParser}.
 *
 * @param <T> The return type of the visit operation. Use {@link Void} for
 * operations with no return type.
 */
public interface TypeExprVisitor<T> extends ParseTreeVisitor<T> {
	/**
	 * Visit a parse tree produced by {@link TypeExprParser#clsName}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitClsName(TypeExprParser.ClsNameContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeExprParser#funDecl}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitFunDecl(TypeExprParser.FunDeclContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeExprParser#arg}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArg(TypeExprParser.ArgContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeExprParser#argList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitArgList(TypeExprParser.ArgListContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeExprParser#type}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitType(TypeExprParser.TypeContext ctx);
	/**
	 * Visit a parse tree produced by {@link TypeExprParser#typeParamList}.
	 * @param ctx the parse tree
	 * @return the visitor result
	 */
	T visitTypeParamList(TypeExprParser.TypeParamListContext ctx);
}
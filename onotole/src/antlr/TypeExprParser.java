// Generated from C:/Users/ericsson/IdeaProjects/research/onotole/src\TypeExpr.g4 by ANTLR 4.9.1
package antlr;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.*;
import org.antlr.v4.runtime.tree.*;
import java.util.List;
import java.util.Iterator;
import java.util.ArrayList;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast"})
public class TypeExprParser extends Parser {
	static { RuntimeMetaData.checkVersion("4.9.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		T__0=1, DIGIT=2, WORD=3, LPAR=4, RPAR=5, LBR=6, RBR=7, COMMA=8, ARR=9, 
		COL=10, WS=11, NB_STRING_LIT=12, BSTRING_LIT=13;
	public static final int
		RULE_clsName = 0, RULE_funDecl = 1, RULE_arg = 2, RULE_argList = 3, RULE_type = 4, 
		RULE_typeParamList = 5;
	private static String[] makeRuleNames() {
		return new String[] {
			"clsName", "funDecl", "arg", "argList", "type", "typeParamList"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'?'", null, null, "'('", "')'", "'['", "']'", "','", "'->'", "':'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, "DIGIT", "WORD", "LPAR", "RPAR", "LBR", "RBR", "COMMA", "ARR", 
			"COL", "WS", "NB_STRING_LIT", "BSTRING_LIT"
		};
	}
	private static final String[] _SYMBOLIC_NAMES = makeSymbolicNames();
	public static final Vocabulary VOCABULARY = new VocabularyImpl(_LITERAL_NAMES, _SYMBOLIC_NAMES);

	/**
	 * @deprecated Use {@link #VOCABULARY} instead.
	 */
	@Deprecated
	public static final String[] tokenNames;
	static {
		tokenNames = new String[_SYMBOLIC_NAMES.length];
		for (int i = 0; i < tokenNames.length; i++) {
			tokenNames[i] = VOCABULARY.getLiteralName(i);
			if (tokenNames[i] == null) {
				tokenNames[i] = VOCABULARY.getSymbolicName(i);
			}

			if (tokenNames[i] == null) {
				tokenNames[i] = "<INVALID>";
			}
		}
	}

	@Override
	@Deprecated
	public String[] getTokenNames() {
		return tokenNames;
	}

	@Override

	public Vocabulary getVocabulary() {
		return VOCABULARY;
	}

	@Override
	public String getGrammarFileName() { return "TypeExpr.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public ATN getATN() { return _ATN; }

	public TypeExprParser(TokenStream input) {
		super(input);
		_interp = new ParserATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	public static class ClsNameContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(TypeExprParser.WORD, 0); }
		public ClsNameContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_clsName; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).enterClsName(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).exitClsName(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TypeExprVisitor ) return ((TypeExprVisitor<? extends T>)visitor).visitClsName(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClsNameContext clsName() throws RecognitionException {
		ClsNameContext _localctx = new ClsNameContext(_ctx, getState());
		enterRule(_localctx, 0, RULE_clsName);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(13);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(12);
				match(T__0);
				}
			}

			setState(15);
			match(WORD);
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class FunDeclContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(TypeExprParser.WORD, 0); }
		public TerminalNode LPAR() { return getToken(TypeExprParser.LPAR, 0); }
		public ArgListContext argList() {
			return getRuleContext(ArgListContext.class,0);
		}
		public TerminalNode RPAR() { return getToken(TypeExprParser.RPAR, 0); }
		public TerminalNode ARR() { return getToken(TypeExprParser.ARR, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public FunDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_funDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).enterFunDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).exitFunDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TypeExprVisitor ) return ((TypeExprVisitor<? extends T>)visitor).visitFunDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final FunDeclContext funDecl() throws RecognitionException {
		FunDeclContext _localctx = new FunDeclContext(_ctx, getState());
		enterRule(_localctx, 2, RULE_funDecl);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(17);
			match(WORD);
			setState(18);
			match(LPAR);
			setState(19);
			argList();
			setState(20);
			match(RPAR);
			setState(21);
			match(ARR);
			setState(22);
			type();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(TypeExprParser.WORD, 0); }
		public TerminalNode COL() { return getToken(TypeExprParser.COL, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ArgContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_arg; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).enterArg(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).exitArg(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TypeExprVisitor ) return ((TypeExprVisitor<? extends T>)visitor).visitArg(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgContext arg() throws RecognitionException {
		ArgContext _localctx = new ArgContext(_ctx, getState());
		enterRule(_localctx, 4, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(24);
			match(WORD);
			setState(25);
			match(COL);
			setState(26);
			type();
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class ArgListContext extends ParserRuleContext {
		public ArgContext arg;
		public List<ArgContext> args = new ArrayList<ArgContext>();
		public List<ArgContext> arg() {
			return getRuleContexts(ArgContext.class);
		}
		public ArgContext arg(int i) {
			return getRuleContext(ArgContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(TypeExprParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TypeExprParser.COMMA, i);
		}
		public ArgListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_argList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).enterArgList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).exitArgList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TypeExprVisitor ) return ((TypeExprVisitor<? extends T>)visitor).visitArgList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ArgListContext argList() throws RecognitionException {
		ArgListContext _localctx = new ArgListContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_argList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(36);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==WORD) {
				{
				setState(28);
				((ArgListContext)_localctx).arg = arg();
				((ArgListContext)_localctx).args.add(((ArgListContext)_localctx).arg);
				setState(33);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(29);
					match(COMMA);
					setState(30);
					((ArgListContext)_localctx).arg = arg();
					((ArgListContext)_localctx).args.add(((ArgListContext)_localctx).arg);
					}
					}
					setState(35);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeContext extends ParserRuleContext {
		public ClsNameContext clsName() {
			return getRuleContext(ClsNameContext.class,0);
		}
		public TerminalNode LBR() { return getToken(TypeExprParser.LBR, 0); }
		public TypeParamListContext typeParamList() {
			return getRuleContext(TypeParamListContext.class,0);
		}
		public TerminalNode RBR() { return getToken(TypeExprParser.RBR, 0); }
		public TerminalNode LPAR() { return getToken(TypeExprParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(TypeExprParser.RPAR, 0); }
		public TerminalNode ARR() { return getToken(TypeExprParser.ARR, 0); }
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TypeContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_type; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).enterType(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).exitType(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TypeExprVisitor ) return ((TypeExprVisitor<? extends T>)visitor).visitType(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeContext type() throws RecognitionException {
		TypeContext _localctx = new TypeContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_type);
		try {
			setState(50);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,3,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(38);
				clsName();
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(39);
				clsName();
				setState(40);
				match(LBR);
				setState(41);
				typeParamList();
				setState(42);
				match(RBR);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(44);
				match(LPAR);
				setState(45);
				typeParamList();
				setState(46);
				match(RPAR);
				setState(47);
				match(ARR);
				setState(48);
				type();
				}
				break;
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static class TypeParamListContext extends ParserRuleContext {
		public TypeContext type;
		public List<TypeContext> tparams = new ArrayList<TypeContext>();
		public List<TypeContext> type() {
			return getRuleContexts(TypeContext.class);
		}
		public TypeContext type(int i) {
			return getRuleContext(TypeContext.class,i);
		}
		public List<TerminalNode> COMMA() { return getTokens(TypeExprParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TypeExprParser.COMMA, i);
		}
		public TypeParamListContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_typeParamList; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).enterTypeParamList(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).exitTypeParamList(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TypeExprVisitor ) return ((TypeExprVisitor<? extends T>)visitor).visitTypeParamList(this);
			else return visitor.visitChildren(this);
		}
	}

	public final TypeParamListContext typeParamList() throws RecognitionException {
		TypeParamListContext _localctx = new TypeParamListContext(_ctx, getState());
		enterRule(_localctx, 10, RULE_typeParamList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(52);
			((TypeParamListContext)_localctx).type = type();
			((TypeParamListContext)_localctx).tparams.add(((TypeParamListContext)_localctx).type);
			setState(57);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(53);
				match(COMMA);
				setState(54);
				((TypeParamListContext)_localctx).type = type();
				((TypeParamListContext)_localctx).tparams.add(((TypeParamListContext)_localctx).type);
				}
				}
				setState(59);
				_errHandler.sync(this);
				_la = _input.LA(1);
			}
			}
		}
		catch (RecognitionException re) {
			_localctx.exception = re;
			_errHandler.reportError(this, re);
			_errHandler.recover(this, re);
		}
		finally {
			exitRule();
		}
		return _localctx;
	}

	public static final String _serializedATN =
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\17?\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\3\2\5\2\20\n\2\3\2\3\2\3\3\3\3\3\3"+
		"\3\3\3\3\3\3\3\3\3\4\3\4\3\4\3\4\3\5\3\5\3\5\7\5\"\n\5\f\5\16\5%\13\5"+
		"\5\5\'\n\5\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\3\6\5\6\65\n\6"+
		"\3\7\3\7\3\7\7\7:\n\7\f\7\16\7=\13\7\3\7\2\2\b\2\4\6\b\n\f\2\2\2>\2\17"+
		"\3\2\2\2\4\23\3\2\2\2\6\32\3\2\2\2\b&\3\2\2\2\n\64\3\2\2\2\f\66\3\2\2"+
		"\2\16\20\7\3\2\2\17\16\3\2\2\2\17\20\3\2\2\2\20\21\3\2\2\2\21\22\7\5\2"+
		"\2\22\3\3\2\2\2\23\24\7\5\2\2\24\25\7\6\2\2\25\26\5\b\5\2\26\27\7\7\2"+
		"\2\27\30\7\13\2\2\30\31\5\n\6\2\31\5\3\2\2\2\32\33\7\5\2\2\33\34\7\f\2"+
		"\2\34\35\5\n\6\2\35\7\3\2\2\2\36#\5\6\4\2\37 \7\n\2\2 \"\5\6\4\2!\37\3"+
		"\2\2\2\"%\3\2\2\2#!\3\2\2\2#$\3\2\2\2$\'\3\2\2\2%#\3\2\2\2&\36\3\2\2\2"+
		"&\'\3\2\2\2\'\t\3\2\2\2(\65\5\2\2\2)*\5\2\2\2*+\7\b\2\2+,\5\f\7\2,-\7"+
		"\t\2\2-\65\3\2\2\2./\7\6\2\2/\60\5\f\7\2\60\61\7\7\2\2\61\62\7\13\2\2"+
		"\62\63\5\n\6\2\63\65\3\2\2\2\64(\3\2\2\2\64)\3\2\2\2\64.\3\2\2\2\65\13"+
		"\3\2\2\2\66;\5\n\6\2\678\7\n\2\28:\5\n\6\29\67\3\2\2\2:=\3\2\2\2;9\3\2"+
		"\2\2;<\3\2\2\2<\r\3\2\2\2=;\3\2\2\2\7\17#&\64;";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
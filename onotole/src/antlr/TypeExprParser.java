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
		T__0=1, T__1=2, WORD=3, LPAR=4, RPAR=5, LBR=6, RBR=7, COMMA=8, ARR=9, 
		COL=10, WS=11;
	public static final int
		RULE_clsName = 0, RULE_type = 1, RULE_typeParamList = 2, RULE_clsHead = 3, 
		RULE_clsDecl = 4, RULE_arg = 5, RULE_funDecl = 6;
	private static String[] makeRuleNames() {
		return new String[] {
			"clsName", "type", "typeParamList", "clsHead", "clsDecl", "arg", "funDecl"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'?'", "'<:'", null, "'('", "')'", "'['", "']'", "','", "'->'", 
			"':'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, null, null, "WORD", "LPAR", "RPAR", "LBR", "RBR", "COMMA", "ARR", 
			"COL", "WS"
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
			setState(15);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__0) {
				{
				setState(14);
				match(T__0);
				}
			}

			setState(17);
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
		enterRule(_localctx, 2, RULE_type);
		int _la;
		try {
			setState(32);
			_errHandler.sync(this);
			switch (_input.LA(1)) {
			case T__0:
			case WORD:
				enterOuterAlt(_localctx, 1);
				{
				setState(19);
				clsName();
				setState(24);
				_errHandler.sync(this);
				_la = _input.LA(1);
				if (_la==LBR) {
					{
					setState(20);
					match(LBR);
					setState(21);
					typeParamList();
					setState(22);
					match(RBR);
					}
				}

				}
				break;
			case LPAR:
				enterOuterAlt(_localctx, 2);
				{
				setState(26);
				match(LPAR);
				setState(27);
				typeParamList();
				setState(28);
				match(RPAR);
				setState(29);
				match(ARR);
				setState(30);
				type();
				}
				break;
			default:
				throw new NoViableAltException(this);
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
		enterRule(_localctx, 4, RULE_typeParamList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(42);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << WORD) | (1L << LPAR))) != 0)) {
				{
				setState(34);
				((TypeParamListContext)_localctx).type = type();
				((TypeParamListContext)_localctx).tparams.add(((TypeParamListContext)_localctx).type);
				setState(39);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(35);
					match(COMMA);
					setState(36);
					((TypeParamListContext)_localctx).type = type();
					((TypeParamListContext)_localctx).tparams.add(((TypeParamListContext)_localctx).type);
					}
					}
					setState(41);
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

	public static class ClsHeadContext extends ParserRuleContext {
		public Token name;
		public Token WORD;
		public List<Token> tparams = new ArrayList<Token>();
		public List<TerminalNode> WORD() { return getTokens(TypeExprParser.WORD); }
		public TerminalNode WORD(int i) {
			return getToken(TypeExprParser.WORD, i);
		}
		public TerminalNode LBR() { return getToken(TypeExprParser.LBR, 0); }
		public TerminalNode RBR() { return getToken(TypeExprParser.RBR, 0); }
		public List<TerminalNode> COMMA() { return getTokens(TypeExprParser.COMMA); }
		public TerminalNode COMMA(int i) {
			return getToken(TypeExprParser.COMMA, i);
		}
		public ClsHeadContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_clsHead; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).enterClsHead(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).exitClsHead(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TypeExprVisitor ) return ((TypeExprVisitor<? extends T>)visitor).visitClsHead(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClsHeadContext clsHead() throws RecognitionException {
		ClsHeadContext _localctx = new ClsHeadContext(_ctx, getState());
		enterRule(_localctx, 6, RULE_clsHead);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(44);
			((ClsHeadContext)_localctx).name = match(WORD);
			setState(55);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBR) {
				{
				setState(45);
				match(LBR);
				setState(46);
				((ClsHeadContext)_localctx).WORD = match(WORD);
				((ClsHeadContext)_localctx).tparams.add(((ClsHeadContext)_localctx).WORD);
				setState(51);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(47);
					match(COMMA);
					setState(48);
					((ClsHeadContext)_localctx).WORD = match(WORD);
					((ClsHeadContext)_localctx).tparams.add(((ClsHeadContext)_localctx).WORD);
					}
					}
					setState(53);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(54);
				match(RBR);
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

	public static class ClsDeclContext extends ParserRuleContext {
		public TypeContext base;
		public ClsHeadContext clsHead() {
			return getRuleContext(ClsHeadContext.class,0);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public ClsDeclContext(ParserRuleContext parent, int invokingState) {
			super(parent, invokingState);
		}
		@Override public int getRuleIndex() { return RULE_clsDecl; }
		@Override
		public void enterRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).enterClsDecl(this);
		}
		@Override
		public void exitRule(ParseTreeListener listener) {
			if ( listener instanceof TypeExprListener ) ((TypeExprListener)listener).exitClsDecl(this);
		}
		@Override
		public <T> T accept(ParseTreeVisitor<? extends T> visitor) {
			if ( visitor instanceof TypeExprVisitor ) return ((TypeExprVisitor<? extends T>)visitor).visitClsDecl(this);
			else return visitor.visitChildren(this);
		}
	}

	public final ClsDeclContext clsDecl() throws RecognitionException {
		ClsDeclContext _localctx = new ClsDeclContext(_ctx, getState());
		enterRule(_localctx, 8, RULE_clsDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(57);
			clsHead();
			setState(60);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==T__1) {
				{
				setState(58);
				match(T__1);
				setState(59);
				((ClsDeclContext)_localctx).base = type();
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

	public static class ArgContext extends ParserRuleContext {
		public Token name;
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode COL() { return getToken(TypeExprParser.COL, 0); }
		public TerminalNode WORD() { return getToken(TypeExprParser.WORD, 0); }
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
		enterRule(_localctx, 10, RULE_arg);
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(64);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,8,_ctx) ) {
			case 1:
				{
				setState(62);
				((ArgContext)_localctx).name = match(WORD);
				setState(63);
				match(COL);
				}
				break;
			}
			setState(66);
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

	public static class FunDeclContext extends ParserRuleContext {
		public Token name;
		public Token WORD;
		public List<Token> targs = new ArrayList<Token>();
		public ArgContext arg;
		public List<ArgContext> args = new ArrayList<ArgContext>();
		public TypeContext resType;
		public TerminalNode LPAR() { return getToken(TypeExprParser.LPAR, 0); }
		public TerminalNode RPAR() { return getToken(TypeExprParser.RPAR, 0); }
		public TerminalNode ARR() { return getToken(TypeExprParser.ARR, 0); }
		public List<TerminalNode> WORD() { return getTokens(TypeExprParser.WORD); }
		public TerminalNode WORD(int i) {
			return getToken(TypeExprParser.WORD, i);
		}
		public TypeContext type() {
			return getRuleContext(TypeContext.class,0);
		}
		public TerminalNode LBR() { return getToken(TypeExprParser.LBR, 0); }
		public TerminalNode RBR() { return getToken(TypeExprParser.RBR, 0); }
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
		enterRule(_localctx, 12, RULE_funDecl);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(68);
			((FunDeclContext)_localctx).name = match(WORD);
			setState(79);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if (_la==LBR) {
				{
				setState(69);
				match(LBR);
				setState(70);
				((FunDeclContext)_localctx).WORD = match(WORD);
				((FunDeclContext)_localctx).targs.add(((FunDeclContext)_localctx).WORD);
				setState(75);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(71);
					match(COMMA);
					setState(72);
					((FunDeclContext)_localctx).WORD = match(WORD);
					((FunDeclContext)_localctx).targs.add(((FunDeclContext)_localctx).WORD);
					}
					}
					setState(77);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				setState(78);
				match(RBR);
				}
			}

			setState(81);
			match(LPAR);
			setState(90);
			_errHandler.sync(this);
			_la = _input.LA(1);
			if ((((_la) & ~0x3f) == 0 && ((1L << _la) & ((1L << T__0) | (1L << WORD) | (1L << LPAR))) != 0)) {
				{
				setState(82);
				((FunDeclContext)_localctx).arg = arg();
				((FunDeclContext)_localctx).args.add(((FunDeclContext)_localctx).arg);
				setState(87);
				_errHandler.sync(this);
				_la = _input.LA(1);
				while (_la==COMMA) {
					{
					{
					setState(83);
					match(COMMA);
					setState(84);
					((FunDeclContext)_localctx).arg = arg();
					((FunDeclContext)_localctx).args.add(((FunDeclContext)_localctx).arg);
					}
					}
					setState(89);
					_errHandler.sync(this);
					_la = _input.LA(1);
				}
				}
			}

			setState(92);
			match(RPAR);
			setState(93);
			match(ARR);
			setState(94);
			((FunDeclContext)_localctx).resType = type();
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\rc\4\2\t\2\4\3\t"+
		"\3\4\4\t\4\4\5\t\5\4\6\t\6\4\7\t\7\4\b\t\b\3\2\5\2\22\n\2\3\2\3\2\3\3"+
		"\3\3\3\3\3\3\3\3\5\3\33\n\3\3\3\3\3\3\3\3\3\3\3\3\3\5\3#\n\3\3\4\3\4\3"+
		"\4\7\4(\n\4\f\4\16\4+\13\4\5\4-\n\4\3\5\3\5\3\5\3\5\3\5\7\5\64\n\5\f\5"+
		"\16\5\67\13\5\3\5\5\5:\n\5\3\6\3\6\3\6\5\6?\n\6\3\7\3\7\5\7C\n\7\3\7\3"+
		"\7\3\b\3\b\3\b\3\b\3\b\7\bL\n\b\f\b\16\bO\13\b\3\b\5\bR\n\b\3\b\3\b\3"+
		"\b\3\b\7\bX\n\b\f\b\16\b[\13\b\5\b]\n\b\3\b\3\b\3\b\3\b\3\b\2\2\t\2\4"+
		"\6\b\n\f\16\2\2\2h\2\21\3\2\2\2\4\"\3\2\2\2\6,\3\2\2\2\b.\3\2\2\2\n;\3"+
		"\2\2\2\fB\3\2\2\2\16F\3\2\2\2\20\22\7\3\2\2\21\20\3\2\2\2\21\22\3\2\2"+
		"\2\22\23\3\2\2\2\23\24\7\5\2\2\24\3\3\2\2\2\25\32\5\2\2\2\26\27\7\b\2"+
		"\2\27\30\5\6\4\2\30\31\7\t\2\2\31\33\3\2\2\2\32\26\3\2\2\2\32\33\3\2\2"+
		"\2\33#\3\2\2\2\34\35\7\6\2\2\35\36\5\6\4\2\36\37\7\7\2\2\37 \7\13\2\2"+
		" !\5\4\3\2!#\3\2\2\2\"\25\3\2\2\2\"\34\3\2\2\2#\5\3\2\2\2$)\5\4\3\2%&"+
		"\7\n\2\2&(\5\4\3\2\'%\3\2\2\2(+\3\2\2\2)\'\3\2\2\2)*\3\2\2\2*-\3\2\2\2"+
		"+)\3\2\2\2,$\3\2\2\2,-\3\2\2\2-\7\3\2\2\2.9\7\5\2\2/\60\7\b\2\2\60\65"+
		"\7\5\2\2\61\62\7\n\2\2\62\64\7\5\2\2\63\61\3\2\2\2\64\67\3\2\2\2\65\63"+
		"\3\2\2\2\65\66\3\2\2\2\668\3\2\2\2\67\65\3\2\2\28:\7\t\2\29/\3\2\2\29"+
		":\3\2\2\2:\t\3\2\2\2;>\5\b\5\2<=\7\4\2\2=?\5\4\3\2><\3\2\2\2>?\3\2\2\2"+
		"?\13\3\2\2\2@A\7\5\2\2AC\7\f\2\2B@\3\2\2\2BC\3\2\2\2CD\3\2\2\2DE\5\4\3"+
		"\2E\r\3\2\2\2FQ\7\5\2\2GH\7\b\2\2HM\7\5\2\2IJ\7\n\2\2JL\7\5\2\2KI\3\2"+
		"\2\2LO\3\2\2\2MK\3\2\2\2MN\3\2\2\2NP\3\2\2\2OM\3\2\2\2PR\7\t\2\2QG\3\2"+
		"\2\2QR\3\2\2\2RS\3\2\2\2S\\\7\6\2\2TY\5\f\7\2UV\7\n\2\2VX\5\f\7\2WU\3"+
		"\2\2\2X[\3\2\2\2YW\3\2\2\2YZ\3\2\2\2Z]\3\2\2\2[Y\3\2\2\2\\T\3\2\2\2\\"+
		"]\3\2\2\2]^\3\2\2\2^_\7\7\2\2_`\7\13\2\2`a\5\4\3\2a\17\3\2\2\2\17\21\32"+
		"\"),\659>BMQY\\";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
// Generated from C:/Users/ericsson/IdeaProjects/research/onotole/src\TypeExpr.g4 by ANTLR 4.9.1
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
		NUM=1, WORD=2, LPAR=3, RPAR=4, LBR=5, RBR=6, COMMA=7, EQ=8, ARR=9, WS=10, 
		NB_STRING_LIT=11, BSTRING_LIT=12;
	public static final int
		RULE_type = 0, RULE_typeParamList = 1;
	private static String[] makeRuleNames() {
		return new String[] {
			"type", "typeParamList"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, "'('", "')'", "'['", "']'", "','", "'='", "'->'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "NUM", "WORD", "LPAR", "RPAR", "LBR", "RBR", "COMMA", "EQ", "ARR", 
			"WS", "NB_STRING_LIT", "BSTRING_LIT"
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

	public static class TypeContext extends ParserRuleContext {
		public TerminalNode WORD() { return getToken(TypeExprParser.WORD, 0); }
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
		enterRule(_localctx, 0, RULE_type);
		try {
			setState(16);
			_errHandler.sync(this);
			switch ( getInterpreter().adaptivePredict(_input,0,_ctx) ) {
			case 1:
				enterOuterAlt(_localctx, 1);
				{
				setState(4);
				match(WORD);
				}
				break;
			case 2:
				enterOuterAlt(_localctx, 2);
				{
				setState(5);
				match(WORD);
				setState(6);
				match(LBR);
				setState(7);
				typeParamList();
				setState(8);
				match(RBR);
				}
				break;
			case 3:
				enterOuterAlt(_localctx, 3);
				{
				setState(10);
				match(LPAR);
				setState(11);
				typeParamList();
				setState(12);
				match(RPAR);
				setState(13);
				match(ARR);
				setState(14);
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
		enterRule(_localctx, 2, RULE_typeParamList);
		int _la;
		try {
			enterOuterAlt(_localctx, 1);
			{
			setState(18);
			type();
			setState(23);
			_errHandler.sync(this);
			_la = _input.LA(1);
			while (_la==COMMA) {
				{
				{
				setState(19);
				match(COMMA);
				setState(20);
				type();
				}
				}
				setState(25);
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
		"\3\u608b\ua72a\u8133\ub9ed\u417c\u3be7\u7786\u5964\3\16\35\4\2\t\2\4\3"+
		"\t\3\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\3\2\5\2\23\n\2\3\3\3"+
		"\3\3\3\7\3\30\n\3\f\3\16\3\33\13\3\3\3\2\2\4\2\4\2\2\2\35\2\22\3\2\2\2"+
		"\4\24\3\2\2\2\6\23\7\4\2\2\7\b\7\4\2\2\b\t\7\7\2\2\t\n\5\4\3\2\n\13\7"+
		"\b\2\2\13\23\3\2\2\2\f\r\7\5\2\2\r\16\5\4\3\2\16\17\7\6\2\2\17\20\7\13"+
		"\2\2\20\21\5\2\2\2\21\23\3\2\2\2\22\6\3\2\2\2\22\7\3\2\2\2\22\f\3\2\2"+
		"\2\23\3\3\2\2\2\24\31\5\2\2\2\25\26\7\t\2\2\26\30\5\2\2\2\27\25\3\2\2"+
		"\2\30\33\3\2\2\2\31\27\3\2\2\2\31\32\3\2\2\2\32\5\3\2\2\2\33\31\3\2\2"+
		"\2\4\22\31";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
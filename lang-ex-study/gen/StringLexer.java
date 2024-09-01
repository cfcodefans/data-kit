// Generated from D:/workspace/study/data-kit/lang-ex-study/src/main/antlr4/StringLexer.g4 by ANTLR 4.13.1
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue", "this-escape"})
public class StringLexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.13.1", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		WS=1, ONE_LINE=2, ESC=3, EmbeddingEnd=4, Variable=5, BackTick=6, TemplateStringAtom=7, 
		EmbeddingStart=8;
	public static final int
		Template=1;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE", "Template"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"WS", "ONE_LINE", "NEWLINE", "ESC", "UNICODE", "HEX", "EmbeddingEnd", 
			"Variable", "BackTick", "TemplateStringAtom", "BackTickEnd", "EmbeddingStart"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, null, null, null, "'}'", null, null, null, "'${'"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "WS", "ONE_LINE", "ESC", "EmbeddingEnd", "Variable", "BackTick", 
			"TemplateStringAtom", "EmbeddingStart"
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


	public StringLexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "StringLexer.g4"; }

	@Override
	public String[] getRuleNames() { return ruleNames; }

	@Override
	public String getSerializedATN() { return _serializedATN; }

	@Override
	public String[] getChannelNames() { return channelNames; }

	@Override
	public String[] getModeNames() { return modeNames; }

	@Override
	public ATN getATN() { return _ATN; }

	public static final String _serializedATN =
		"\u0004\u0000\b_\u0006\uffff\uffff\u0006\uffff\uffff\u0002\u0000\u0007"+
		"\u0000\u0002\u0001\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007"+
		"\u0003\u0002\u0004\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007"+
		"\u0006\u0002\u0007\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n"+
		"\u0007\n\u0002\u000b\u0007\u000b\u0001\u0000\u0004\u0000\u001c\b\u0000"+
		"\u000b\u0000\f\u0000\u001d\u0001\u0000\u0001\u0000\u0001\u0001\u0001\u0001"+
		"\u0001\u0001\u0005\u0001%\b\u0001\n\u0001\f\u0001(\t\u0001\u0001\u0001"+
		"\u0001\u0001\u0001\u0002\u0003\u0002-\b\u0002\u0001\u0002\u0001\u0002"+
		"\u0004\u00021\b\u0002\u000b\u0002\f\u00022\u0001\u0003\u0001\u0003\u0001"+
		"\u0003\u0003\u00038\b\u0003\u0001\u0004\u0001\u0004\u0001\u0004\u0001"+
		"\u0004\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001\u0006\u0001"+
		"\u0006\u0001\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0005\u0007H\b"+
		"\u0007\n\u0007\f\u0007K\t\u0007\u0001\b\u0001\b\u0001\b\u0001\b\u0001"+
		"\t\u0004\tR\b\t\u000b\t\f\tS\u0001\n\u0001\n\u0001\n\u0001\n\u0001\n\u0001"+
		"\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0001\u000b\u0000\u0000\f\u0002"+
		"\u0001\u0004\u0002\u0006\u0000\b\u0003\n\u0000\f\u0000\u000e\u0004\u0010"+
		"\u0005\u0012\u0006\u0014\u0007\u0016\u0000\u0018\b\u0002\u0000\u0001\u0007"+
		"\u0003\u0000\t\n\r\r  \u0004\u0000\n\n\r\r\"\"\\\\\b\u0000\"\"//\\\\b"+
		"bffnnrrtt\u0003\u000009AFaf\u0003\u0000AZ__az\u0004\u000009AZ__az\u0004"+
		"\u0000$$\'\'``||c\u0000\u0002\u0001\u0000\u0000\u0000\u0000\u0004\u0001"+
		"\u0000\u0000\u0000\u0000\b\u0001\u0000\u0000\u0000\u0000\u000e\u0001\u0000"+
		"\u0000\u0000\u0000\u0010\u0001\u0000\u0000\u0000\u0000\u0012\u0001\u0000"+
		"\u0000\u0000\u0001\u0014\u0001\u0000\u0000\u0000\u0001\u0016\u0001\u0000"+
		"\u0000\u0000\u0001\u0018\u0001\u0000\u0000\u0000\u0002\u001b\u0001\u0000"+
		"\u0000\u0000\u0004!\u0001\u0000\u0000\u0000\u00060\u0001\u0000\u0000\u0000"+
		"\b4\u0001\u0000\u0000\u0000\n9\u0001\u0000\u0000\u0000\f?\u0001\u0000"+
		"\u0000\u0000\u000eA\u0001\u0000\u0000\u0000\u0010E\u0001\u0000\u0000\u0000"+
		"\u0012L\u0001\u0000\u0000\u0000\u0014Q\u0001\u0000\u0000\u0000\u0016U"+
		"\u0001\u0000\u0000\u0000\u0018Z\u0001\u0000\u0000\u0000\u001a\u001c\u0007"+
		"\u0000\u0000\u0000\u001b\u001a\u0001\u0000\u0000\u0000\u001c\u001d\u0001"+
		"\u0000\u0000\u0000\u001d\u001b\u0001\u0000\u0000\u0000\u001d\u001e\u0001"+
		"\u0000\u0000\u0000\u001e\u001f\u0001\u0000\u0000\u0000\u001f \u0006\u0000"+
		"\u0000\u0000 \u0003\u0001\u0000\u0000\u0000!&\u0005\"\u0000\u0000\"%\u0003"+
		"\b\u0003\u0000#%\b\u0001\u0000\u0000$\"\u0001\u0000\u0000\u0000$#\u0001"+
		"\u0000\u0000\u0000%(\u0001\u0000\u0000\u0000&$\u0001\u0000\u0000\u0000"+
		"&\'\u0001\u0000\u0000\u0000\')\u0001\u0000\u0000\u0000(&\u0001\u0000\u0000"+
		"\u0000)*\u0005\"\u0000\u0000*\u0005\u0001\u0000\u0000\u0000+-\u0005\r"+
		"\u0000\u0000,+\u0001\u0000\u0000\u0000,-\u0001\u0000\u0000\u0000-.\u0001"+
		"\u0000\u0000\u0000.1\u0005\n\u0000\u0000/1\u0005\r\u0000\u00000,\u0001"+
		"\u0000\u0000\u00000/\u0001\u0000\u0000\u000012\u0001\u0000\u0000\u0000"+
		"20\u0001\u0000\u0000\u000023\u0001\u0000\u0000\u00003\u0007\u0001\u0000"+
		"\u0000\u000047\u0005\\\u0000\u000058\u0007\u0002\u0000\u000068\u0003\n"+
		"\u0004\u000075\u0001\u0000\u0000\u000076\u0001\u0000\u0000\u00008\t\u0001"+
		"\u0000\u0000\u00009:\u0005u\u0000\u0000:;\u0003\f\u0005\u0000;<\u0003"+
		"\f\u0005\u0000<=\u0003\f\u0005\u0000=>\u0003\f\u0005\u0000>\u000b\u0001"+
		"\u0000\u0000\u0000?@\u0007\u0003\u0000\u0000@\r\u0001\u0000\u0000\u0000"+
		"AB\u0005}\u0000\u0000BC\u0001\u0000\u0000\u0000CD\u0006\u0006\u0001\u0000"+
		"D\u000f\u0001\u0000\u0000\u0000EI\u0007\u0004\u0000\u0000FH\u0007\u0005"+
		"\u0000\u0000GF\u0001\u0000\u0000\u0000HK\u0001\u0000\u0000\u0000IG\u0001"+
		"\u0000\u0000\u0000IJ\u0001\u0000\u0000\u0000J\u0011\u0001\u0000\u0000"+
		"\u0000KI\u0001\u0000\u0000\u0000LM\u0005`\u0000\u0000MN\u0001\u0000\u0000"+
		"\u0000NO\u0006\b\u0002\u0000O\u0013\u0001\u0000\u0000\u0000PR\b\u0006"+
		"\u0000\u0000QP\u0001\u0000\u0000\u0000RS\u0001\u0000\u0000\u0000SQ\u0001"+
		"\u0000\u0000\u0000ST\u0001\u0000\u0000\u0000T\u0015\u0001\u0000\u0000"+
		"\u0000UV\u0005`\u0000\u0000VW\u0001\u0000\u0000\u0000WX\u0006\n\u0003"+
		"\u0000XY\u0006\n\u0001\u0000Y\u0017\u0001\u0000\u0000\u0000Z[\u0005$\u0000"+
		"\u0000[\\\u0005{\u0000\u0000\\]\u0001\u0000\u0000\u0000]^\u0006\u000b"+
		"\u0004\u0000^\u0019\u0001\u0000\u0000\u0000\u000b\u0000\u0001\u001d$&"+
		",027IS\u0005\u0006\u0000\u0000\u0004\u0000\u0000\u0005\u0001\u0000\u0007"+
		"\u0006\u0000\u0005\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
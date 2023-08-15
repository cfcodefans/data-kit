// Generated from D:/workspace/study/data-kit/lang-ex-study/src/main/resources\arithmetic_v1.g4 by ANTLR 4.12.0
package tool;
import org.antlr.v4.runtime.Lexer;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStream;
import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.atn.*;
import org.antlr.v4.runtime.dfa.DFA;
import org.antlr.v4.runtime.misc.*;

@SuppressWarnings({"all", "warnings", "unchecked", "unused", "cast", "CheckReturnValue"})
public class arithmetic_v1Lexer extends Lexer {
	static { RuntimeMetaData.checkVersion("4.12.0", RuntimeMetaData.VERSION); }

	protected static final DFA[] _decisionToDFA;
	protected static final PredictionContextCache _sharedContextCache =
		new PredictionContextCache();
	public static final int
		PLUS=1, MINUS=2, TIMES=3, DIV=4, POW=5, POINT=6, MOD=7, OPPSITE=8, NUMBER=9, 
		WS=10, VARIABLE=11, LPAREN=12, RPAREN=13, EQ=14, GT=15, LT=16, GEQ=17, 
		LEQ=18;
	public static String[] channelNames = {
		"DEFAULT_TOKEN_CHANNEL", "HIDDEN"
	};

	public static String[] modeNames = {
		"DEFAULT_MODE"
	};

	private static String[] makeRuleNames() {
		return new String[] {
			"PLUS", "MINUS", "TIMES", "DIV", "POW", "POINT", "MOD", "OPPSITE", "NUMBER", 
			"WS", "VARIABLE", "LPAREN", "RPAREN", "EQ", "GT", "LT", "GEQ", "LEQ"
		};
	}
	public static final String[] ruleNames = makeRuleNames();

	private static String[] makeLiteralNames() {
		return new String[] {
			null, "'+'", "'-'", "'*'", "'/'", "'^'", "'.'", "'%'", "'!'", null, null, 
			null, "'('", "')'", "'=='", "'>'", "'<'", "'>='", "'<='"
		};
	}
	private static final String[] _LITERAL_NAMES = makeLiteralNames();
	private static String[] makeSymbolicNames() {
		return new String[] {
			null, "PLUS", "MINUS", "TIMES", "DIV", "POW", "POINT", "MOD", "OPPSITE", 
			"NUMBER", "WS", "VARIABLE", "LPAREN", "RPAREN", "EQ", "GT", "LT", "GEQ", 
			"LEQ"
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


	public arithmetic_v1Lexer(CharStream input) {
		super(input);
		_interp = new LexerATNSimulator(this,_ATN,_decisionToDFA,_sharedContextCache);
	}

	@Override
	public String getGrammarFileName() { return "arithmetic_v1.g4"; }

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
		"\u0004\u0000\u0012l\u0006\uffff\uffff\u0002\u0000\u0007\u0000\u0002\u0001"+
		"\u0007\u0001\u0002\u0002\u0007\u0002\u0002\u0003\u0007\u0003\u0002\u0004"+
		"\u0007\u0004\u0002\u0005\u0007\u0005\u0002\u0006\u0007\u0006\u0002\u0007"+
		"\u0007\u0007\u0002\b\u0007\b\u0002\t\u0007\t\u0002\n\u0007\n\u0002\u000b"+
		"\u0007\u000b\u0002\f\u0007\f\u0002\r\u0007\r\u0002\u000e\u0007\u000e\u0002"+
		"\u000f\u0007\u000f\u0002\u0010\u0007\u0010\u0002\u0011\u0007\u0011\u0001"+
		"\u0000\u0001\u0000\u0001\u0001\u0001\u0001\u0001\u0002\u0001\u0002\u0001"+
		"\u0003\u0001\u0003\u0001\u0004\u0001\u0004\u0001\u0005\u0001\u0005\u0001"+
		"\u0006\u0001\u0006\u0001\u0007\u0001\u0007\u0001\b\u0001\b\u0001\b\u0005"+
		"\b9\b\b\n\b\f\b<\t\b\u0003\b>\b\b\u0001\b\u0004\bA\b\b\u000b\b\f\bB\u0001"+
		"\b\u0001\b\u0005\bG\b\b\n\b\f\bJ\t\b\u0003\bL\b\b\u0001\t\u0004\tO\b\t"+
		"\u000b\t\f\tP\u0001\t\u0001\t\u0001\n\u0001\n\u0005\nW\b\n\n\n\f\nZ\t"+
		"\n\u0001\u000b\u0001\u000b\u0001\f\u0001\f\u0001\r\u0001\r\u0001\r\u0001"+
		"\u000e\u0001\u000e\u0001\u000f\u0001\u000f\u0001\u0010\u0001\u0010\u0001"+
		"\u0010\u0001\u0011\u0001\u0011\u0001\u0011\u0000\u0000\u0012\u0001\u0001"+
		"\u0003\u0002\u0005\u0003\u0007\u0004\t\u0005\u000b\u0006\r\u0007\u000f"+
		"\b\u0011\t\u0013\n\u0015\u000b\u0017\f\u0019\r\u001b\u000e\u001d\u000f"+
		"\u001f\u0010!\u0011#\u0012\u0001\u0000\u0003\u0003\u0000\t\n\r\r  \u0003"+
		"\u0000AZ__az\u0004\u000009AZ__azr\u0000\u0001\u0001\u0000\u0000\u0000"+
		"\u0000\u0003\u0001\u0000\u0000\u0000\u0000\u0005\u0001\u0000\u0000\u0000"+
		"\u0000\u0007\u0001\u0000\u0000\u0000\u0000\t\u0001\u0000\u0000\u0000\u0000"+
		"\u000b\u0001\u0000\u0000\u0000\u0000\r\u0001\u0000\u0000\u0000\u0000\u000f"+
		"\u0001\u0000\u0000\u0000\u0000\u0011\u0001\u0000\u0000\u0000\u0000\u0013"+
		"\u0001\u0000\u0000\u0000\u0000\u0015\u0001\u0000\u0000\u0000\u0000\u0017"+
		"\u0001\u0000\u0000\u0000\u0000\u0019\u0001\u0000\u0000\u0000\u0000\u001b"+
		"\u0001\u0000\u0000\u0000\u0000\u001d\u0001\u0000\u0000\u0000\u0000\u001f"+
		"\u0001\u0000\u0000\u0000\u0000!\u0001\u0000\u0000\u0000\u0000#\u0001\u0000"+
		"\u0000\u0000\u0001%\u0001\u0000\u0000\u0000\u0003\'\u0001\u0000\u0000"+
		"\u0000\u0005)\u0001\u0000\u0000\u0000\u0007+\u0001\u0000\u0000\u0000\t"+
		"-\u0001\u0000\u0000\u0000\u000b/\u0001\u0000\u0000\u0000\r1\u0001\u0000"+
		"\u0000\u0000\u000f3\u0001\u0000\u0000\u0000\u0011K\u0001\u0000\u0000\u0000"+
		"\u0013N\u0001\u0000\u0000\u0000\u0015T\u0001\u0000\u0000\u0000\u0017["+
		"\u0001\u0000\u0000\u0000\u0019]\u0001\u0000\u0000\u0000\u001b_\u0001\u0000"+
		"\u0000\u0000\u001db\u0001\u0000\u0000\u0000\u001fd\u0001\u0000\u0000\u0000"+
		"!f\u0001\u0000\u0000\u0000#i\u0001\u0000\u0000\u0000%&\u0005+\u0000\u0000"+
		"&\u0002\u0001\u0000\u0000\u0000\'(\u0005-\u0000\u0000(\u0004\u0001\u0000"+
		"\u0000\u0000)*\u0005*\u0000\u0000*\u0006\u0001\u0000\u0000\u0000+,\u0005"+
		"/\u0000\u0000,\b\u0001\u0000\u0000\u0000-.\u0005^\u0000\u0000.\n\u0001"+
		"\u0000\u0000\u0000/0\u0005.\u0000\u00000\f\u0001\u0000\u0000\u000012\u0005"+
		"%\u0000\u00002\u000e\u0001\u0000\u0000\u000034\u0005!\u0000\u00004\u0010"+
		"\u0001\u0000\u0000\u00005>\u00050\u0000\u00006:\u000219\u000079\u0002"+
		"09\u000087\u0001\u0000\u0000\u00009<\u0001\u0000\u0000\u0000:8\u0001\u0000"+
		"\u0000\u0000:;\u0001\u0000\u0000\u0000;>\u0001\u0000\u0000\u0000<:\u0001"+
		"\u0000\u0000\u0000=5\u0001\u0000\u0000\u0000=6\u0001\u0000\u0000\u0000"+
		">L\u0001\u0000\u0000\u0000?A\u000209\u0000@?\u0001\u0000\u0000\u0000A"+
		"B\u0001\u0000\u0000\u0000B@\u0001\u0000\u0000\u0000BC\u0001\u0000\u0000"+
		"\u0000CD\u0001\u0000\u0000\u0000DH\u0005.\u0000\u0000EG\u000209\u0000"+
		"FE\u0001\u0000\u0000\u0000GJ\u0001\u0000\u0000\u0000HF\u0001\u0000\u0000"+
		"\u0000HI\u0001\u0000\u0000\u0000IL\u0001\u0000\u0000\u0000JH\u0001\u0000"+
		"\u0000\u0000K=\u0001\u0000\u0000\u0000K@\u0001\u0000\u0000\u0000L\u0012"+
		"\u0001\u0000\u0000\u0000MO\u0007\u0000\u0000\u0000NM\u0001\u0000\u0000"+
		"\u0000OP\u0001\u0000\u0000\u0000PN\u0001\u0000\u0000\u0000PQ\u0001\u0000"+
		"\u0000\u0000QR\u0001\u0000\u0000\u0000RS\u0006\t\u0000\u0000S\u0014\u0001"+
		"\u0000\u0000\u0000TX\u0007\u0001\u0000\u0000UW\u0007\u0002\u0000\u0000"+
		"VU\u0001\u0000\u0000\u0000WZ\u0001\u0000\u0000\u0000XV\u0001\u0000\u0000"+
		"\u0000XY\u0001\u0000\u0000\u0000Y\u0016\u0001\u0000\u0000\u0000ZX\u0001"+
		"\u0000\u0000\u0000[\\\u0005(\u0000\u0000\\\u0018\u0001\u0000\u0000\u0000"+
		"]^\u0005)\u0000\u0000^\u001a\u0001\u0000\u0000\u0000_`\u0005=\u0000\u0000"+
		"`a\u0005=\u0000\u0000a\u001c\u0001\u0000\u0000\u0000bc\u0005>\u0000\u0000"+
		"c\u001e\u0001\u0000\u0000\u0000de\u0005<\u0000\u0000e \u0001\u0000\u0000"+
		"\u0000fg\u0005>\u0000\u0000gh\u0005=\u0000\u0000h\"\u0001\u0000\u0000"+
		"\u0000ij\u0005<\u0000\u0000jk\u0005=\u0000\u0000k$\u0001\u0000\u0000\u0000"+
		"\b\u0000:=BHKPX\u0001\u0006\u0000\u0000";
	public static final ATN _ATN =
		new ATNDeserializer().deserialize(_serializedATN.toCharArray());
	static {
		_decisionToDFA = new DFA[_ATN.getNumberOfDecisions()];
		for (int i = 0; i < _ATN.getNumberOfDecisions(); i++) {
			_decisionToDFA[i] = new DFA(_ATN.getDecisionState(i), i);
		}
	}
}
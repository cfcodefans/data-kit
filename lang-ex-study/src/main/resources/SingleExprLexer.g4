lexer grammar SingleExprLexer;

WS:[ \t\n\r]+ -> skip;

OpenBracket: '[';
CloseBracket: ']';
OpenParen:  '(';
CloseParen: ')';
Comma:  ',';
Colon:  ':';

Hashtag: '#';

OpenBrace:  '{' {this.ProcessOpenBrace();} -> pushMode(DEFAULT_MODE);
//TemplateCloseBrace: {this.IsInTempateStr()}? '}' -> popMode; //TODO
CloseBrace: {this.IsInTempateStr(_localctx)}? '}' {this.ProcessCloseBrace();} -> popMode;

QuestionMark: '?';
QuestionMarkDot: '?.';

NullCoalesce:   '??';

Dot: '.';

Instanceof: 'instanceof';
Typeof:     'typeof';
NullLiteral:    'null';
BooleanLiteral: 'true'|'false';

Identifier: IdentifierStart IdentifierPart*;

Ellipsis:   '...';

fragment IdentifierStart
    : [\p{L}]
    | [$_]
//    | '\\' UnicodeEscapeSequence
    ;

fragment IdentifierPart
    : IdentifierStart
    | [\p{Mn}]
    | [\p{Nd}]
    | [\p{Pc}]
    | '\u200C'
    | '\u200D'
    ;

ONE_LINE: '"' ( ESC | ~('\\'|'"'|'\r'|'\n') )* '"';
ESC:   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE: 'u' HEX HEX HEX HEX ;
fragment HEX: [0-9a-fA-F] ;

NUMBER: ('0' | '1'..'9' '0'..'9'*)
    | ('0'..'9')+ '.' ('0'..'9')*;

BackTick: '`' -> pushMode(TemplateStr);
mode TemplateStr;
BackTickEnd: '`' -> type(BackTick), popMode;

EmbeddingStart: '${' -> pushMode(DEFAULT_MODE);
TemplateStrAtom: ~[`];

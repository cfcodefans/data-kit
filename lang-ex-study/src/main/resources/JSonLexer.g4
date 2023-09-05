lexer grammar JSonLexer;

WS:[ \t\n\r]+ -> skip;

//Arithmetic
PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
POW: '^';
POINT: '.';
MOD: '%';
OPPSITE:'!';

NUMBER: ('0' | '1'..'9' '0'..'9'*)
    | ('0'..'9')+ '.' ('0'..'9')*;

VARIABLE : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9')* ;

LPAREN: '(';
RPAREN: ')';

EQ: '==';
GT: '>';
LT: '<';
GEQ: '>=';
LEQ: '<=';

//function call
COMMA: ',';


//String
fragment HEX: [0-9a-fA-F] ;
fragment UNICODE: 'u' HEX HEX HEX HEX ;
fragment NEWLINE: ('\r'? '\n' | '\r')+ ;
ESC:   '\\' (["\\/bfnrt] | UNICODE) ;
ONE_LINE: '"' ( ESC | ~('\\'|'"'|'\r'|'\n') )* '"';

EmbeddingEnd: '}' -> popMode;

BackTick: '`' -> pushMode(TemplateString);
mode TemplateString;
BackTickEnd: '`' -> type(BackTick), popMode;

EmbeddingStart: '${' -> pushMode(DEFAULT_MODE);
TemplateStringAtom: ~[`];
//mode Embedding;

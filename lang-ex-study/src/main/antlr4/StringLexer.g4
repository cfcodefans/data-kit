lexer grammar StringLexer;

WS:[ \t\n\r]+ -> skip;
ONE_LINE: '"' ( ESC | ~('\\'|'"'|'\r'|'\n') )* '"';


fragment NEWLINE: ('\r'? '\n' | '\r')+ ;

ESC:   '\\' (["\\/bfnrt] | UNICODE) ;
fragment UNICODE: 'u' HEX HEX HEX HEX ;
fragment HEX: [0-9a-fA-F] ;

EmbeddingEnd: '}' -> popMode; //be aware of the order
Variable : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9')* ;

BackTick: '`' -> pushMode(Template);
mode Template;
TemplateStringAtom: ~[`] ;
BackTickEnd: '`' -> type(BackTick), popMode;

EmbeddingStart: '${' -> pushMode(DEFAULT_MODE); //every token under the mode definition is only visible in the mode


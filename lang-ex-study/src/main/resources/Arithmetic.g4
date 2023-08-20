grammar Arithmetic;

input : expr EOF;

PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
POW: '^';
POINT: '.';
MOD: '%';
OPPSITE:'!';

sign: PLUS|MINUS|OPPSITE;

expr: unary
    | expr '^' expr
    | expr ('*'|'/'|'%') expr
    | expr ('+'|'-') expr
    | expr relop expr
    ;

NUMBER: ('0' | '1'..'9' '0'..'9'*)
    | ('0'..'9')+ '.' ('0'..'9')*;
WS: [ \r\n\t] + -> skip;

relop: EQ | GT | LT | GEQ | LEQ ;

primary:NUMBER | variable
    | LPAREN expr RPAREN;

unary: primary
    | sign unary;

variable : VARIABLE ;
VARIABLE : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9')* ;

LPAREN: '(';
RPAREN: ')';

EQ: '==';
GT: '>';
LT: '<';
GEQ: '>=';
LEQ: '<=';



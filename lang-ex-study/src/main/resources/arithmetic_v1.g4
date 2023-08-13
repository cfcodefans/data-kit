grammar arithmetic_v1;

input : expr EOF;

NUMBER: ('0' | '1'..'9' '0'..'9'*)
    | ('0'..'9')+ '.' ('0'..'9')*;
WS: [ \r\n\t] + -> skip;

expr: unary
    | expr ('^') expr
    | expr ('*'|'/') expr
    | expr ('+'|'-') expr
    | expr relop expr
    ;

relop: EQ | GT | LT | GEQ | LEQ ;

primary:NUMBER | variable
    | LPAREN expr RPAREN;

unary: primary
    | SIGN unary;

variable : VARIABLE ;
VARIABLE : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9')* ;

LPAREN: '(';
RPAREN: ')';
SIGN: '+' | '-' | '!';

EQ: '==';
GT: '>';
LT: '<';
GEQ: '>=';
LEQ: '<=';
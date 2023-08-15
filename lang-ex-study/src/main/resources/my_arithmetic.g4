grammar my_arithmetic;

input : expr EOF;

NUMBER: ('0' | '1'..'9' '0'..'9'*)
    | ('0'..'9')+ '.' ('0'..'9')*;
WS: [ \r\n\t] + -> skip;

expr:
    | add
    | expr relop expr
    ;

primary: NUMBER| variable
    | LPAREN expr RPAREN;

unary: primary
    | (SIGN) unary;

add: mul
    | add ('+'|'-') mul ;

mul: unary
    | pow ('*'|'/') unary ;

pow: unary
    | pow ('^') unary ;

relop: EQ | GT | LT | GEQ | LEQ ;

variable : VARIABLE ;
VARIABLE : ('a' .. 'z' | 'A' .. 'Z' | '_') ('a' .. 'z' | 'A' .. 'Z' | '_' | '0' .. '9')* ;


EQ: '==';
GT: '>';
LT: '<';
GEQ: '>=';
LEQ: '<=';

LPAREN: '(';
RPAREN: ')';
PLUS: '+';
MINUS: '-';
TIMES: '*';
DIV: '/';
POW: '^';
POINT: '.';

SIGN: '+' | '-';




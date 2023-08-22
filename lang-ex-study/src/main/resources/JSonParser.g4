parser grammar JSonParser;

options {tokenVocab = JSonLexer;}
input: expr EOF;

// parser for arithmetic
sign: PLUS|MINUS|OPPSITE;

expr: arithmetic
    | string
    | expr PLUS expr
    | expr relop expr
    ;

arithmetic: unary
    | arithmetic POW arithmetic
    | arithmetic (TIMES|DIV|MOD) arithmetic
    | arithmetic (PLUS|MINUS) arithmetic
    | arithmetic relop arithmetic
    ;

relop: EQ | GT | LT | GEQ | LEQ ;

primary: NUMBER
    | variable
    | LPAREN arithmetic RPAREN;

unary: primary
    | sign unary;

variable : VARIABLE ;

// parser for string
string: ONE_LINE
    |templateString;
templateString: BackTick templateStringAtom* BackTick;
templateStringAtom: embedded
    | TemplateStringAtom;

embedded: EmbeddingStart expr EmbeddingEnd;
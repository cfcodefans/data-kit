grammar SQL_EX;


WHITESPACE : ' ' -> skip ;

json:   object
    |   array
    ;

object: '{' pair (',' pair)* '}'
      | '{' '}'
      ;

pair: key ':' value
   ;

key: STRING
   | IDENTIFIER
   | LITERAL
   | NUMERIC_LITERAL
   ;

value
   : STRING
   | number
   | object
   | array
   | LITERAL
   | expr
   ;

array : '[' value (',' value)* ','? ']'
   | '[' ']'
   ;

number : SYMBOL?
      ( NUMERIC_LITERAL
      | NUMBER
      )
    ;

STRING
   : '"' DOUBLE_QUOTE_CHAR* '"'
   | '\'' SINGLE_QUOTE_CHAR* '\''
   ;

fragment DOUBLE_QUOTE_CHAR
   : ~["\\\r\n]
   | ESCAPE_SEQUENCE
   ;

fragment SINGLE_QUOTE_CHAR
   : ~['\\\r\n]
   | ESCAPE_SEQUENCE
   ;

fragment ESCAPE_SEQUENCE
   : '\\'
   ( NEWLINE
   | UNICODE_SEQUENCE       // \u1234
   | ['"\\/bfnrtv]          // single escape char
   | ~['"\\bfnrtv0-9xu\r\n] // non escape char
   | '0'                    // \0
   | 'x' HEX HEX            // \x3a
   )
   ;

NUMBER
   : INT ('.' [0-9]*)? EXP? // +1.e2, 1234, 1234.5
   | '.' [0-9]+ EXP?        // -.2e3
   | '0' [xX] HEX+          // 0x12345678
   ;

NUMERIC_LITERAL: 'Infinity'
   | 'NaN'
   ;

SYMBOL: '+' | '-'
   ;

fragment HEX
   : [0-9a-fA-F]
   ;

fragment INT
   : '0' | [1-9] [0-9]*
   ;

fragment EXP
   : [Ee] SYMBOL? [0-9]*
   ;

IDENTIFIER
   : IDENTIFIER_START IDENTIFIER_PART*
   ;

fragment IDENTIFIER_START
   : [\p{L}]
   | '$'
   | '_'
   | '\\' UNICODE_SEQUENCE
   ;

fragment IDENTIFIER_PART
   : IDENTIFIER_START
   | [\p{M}]
   | [\p{N}]
   | [\p{Pc}]
   | '\u200C'
   | '\u200D'
   ;

fragment UNICODE_SEQUENCE
   : 'u' HEX HEX HEX HEX
   ;

fragment NEWLINE
   : '\r\n'
   | [\r\n\u2028\u2029]
   ;

expr: number '*' number
    | number '/' number
    | number
    ;
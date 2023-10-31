parser grammar StringParser;

options {tokenVocab = StringLexer;}

//import Arithmetic;

input: (string|templateString) EOF;

string: ONE_LINE;

templateString: BackTick templateStringAtom* BackTick;

templateStringAtom: embedded
    | TemplateStringAtom
    ;

embedded: EmbeddingStart variable EmbeddingEnd ;
variable : Variable ;
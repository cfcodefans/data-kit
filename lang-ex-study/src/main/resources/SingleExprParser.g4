parser grammar SingleExprParser;

options {tokenVocab = SingleExprLexer;}

input: singleExpr EOF;

//exprSequence: singleExpr (Comma singleExpr)*;

identifierName: identifier;

identifier: Identifier;

singleExpr: singleExpr QuestionMarkDot singleExpr                       # OptionalChainExpression
    | singleExpr QuestionMarkDot? OpenBracket singleExpr CloseBracket   # MemberIndexExpression
    | singleExpr QuestionMark? Dot Hashtag? identifierName              # MemberDotExpression
    | Typeof singleExpr                                                 # TypeofExpression
    | singleExpr NullCoalesce singleExpr                                # CoalesceExpression
    | singleExpr Instanceof singleExpr                                  # InstanceofExpression
    | identifier                                                        # IdentifierExpression
    | literal                                                           # LiteralExpression
    | arrayLiteral                                                      # ArrayLiteralExpression
    | objectLiteral                                                     # ObjectLiteralExpression
//    | OpenParen exprSequence CloseParen # ParenthesizedExpression
    ;

literal: NullLiteral
    | BooleanLiteral
    | ONE_LINE
    | templateStrLiteral
    | numericLiteral
    ;


arrayLiteral: (OpenBracket elementList CloseBracket);
elementList
    : Comma* arrayElement? (Comma+ arrayElement)* Comma* // Yes, everything is optional
    ;
arrayElement: Ellipsis? singleExpr;

objectLiteral: OpenBrace (propertyAssignment (Comma propertyAssignment)* Comma?)? CloseBrace;
propertyAssignment
    : propertyName Colon singleExpr #PropertyExpressionAssignment
    | OpenBracket singleExpr CloseBracket Colon singleExpr #ComputedPropertyExpressionAssignment
    | Ellipsis? singleExpr  #PropertyShorthand
    ;

propertyName: identifierName
    | ONE_LINE
    | numericLiteral
    | OpenBracket singleExpr CloseBracket
    ;

numericLiteral:NUMBER;

templateStrLiteral: BackTick templateStrAtom* BackTick;

templateStrAtom: TemplateStrAtom
    | EmbeddingStart singleExpr CloseBrace;
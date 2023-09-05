// Header section
grammar StringTemplate; // The name of the grammar
options {output=JavaScript;} // The output language

// Token definitions
//tokens {
BACKTICK : '`'; // A token for the backtick character
DOLLAR : '$'; // A token for the dollar sign
LBRACE : '{'; // A token for the left brace
RBRACE : '}'; // A token for the right brace
//}

// Body section
// The start rule of the grammar
stringTemplate : BACKTICK (text | expression)* BACKTICK ; // A string template is a backtick followed by zero or more texts or expressions and then another backtick

// A rule for texts
text : ~ (BACKTICK | DOLLAR | LBRACE | RBRACE)+ ; // A text is one or more characters that are not backtick, dollar, left brace, or right brace

// A rule for expressions
expression : DOLLAR LBRACE variable RBRACE ; // An expression is a dollar sign followed by a left brace, a variable, and a right brace

// A rule for variables
variable : ID ; // A variable is an identifier

// A rule for identifiers
ID : LETTER (LETTER | DIGIT)* ; // An identifier is a letter followed by zero or more letters or digits

// A rule for letters
LETTER : 'a'..'z' | 'A'..'Z' ; // A letter is any character from a to z or A to Z

// A rule for digits
DIGIT : '0'..'9' ; // A digit is any character from 0 to 9
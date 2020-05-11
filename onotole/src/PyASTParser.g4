grammar PyASTParser;

value: BSTRING_LIT | NB_STRING_LIT | WORD | NUM | funcCall;
namedParam : WORD EQ value;
param : value | namedParam;
paramList : param (COMMA param)*;
funcCall: WORD LPAR (paramList | ) RPAR;

NUM : ('0' .. '9')+;
WORD : ('_' | 'A' .. 'Z' | 'a' .. 'z')+;
LPAR : '(';
RPAR : ')';
COMMA : ',';
EQ : '=';

WS  :   [ \t\r\n]+ -> skip;
NB_STRING_LIT : '"' ('\\"' | .)*? '"';
BSTRING_LIT : 'b' NB_STRING_LIT;
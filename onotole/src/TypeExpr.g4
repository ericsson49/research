grammar TypeExpr;

clsName : '?'? WORD;
funDecl : WORD LPAR argList RPAR ARR type;
arg : WORD COL type;
argList : (args+=arg (COMMA args+=arg)*)?;
type : clsName | clsName LBR typeParamList RBR | LPAR typeParamList RPAR ARR type;
typeParamList : tparams+=type (COMMA tparams+=type)*;

DIGIT : ('0' .. '9')+;
WORD : ('_' | 'A' .. 'Z' | 'a' .. 'z')+;
LPAR : '(';
RPAR : ')';
LBR : '[';
RBR : ']';
COMMA : ',';
ARR : '->';
COL : ':';

WS  :   [ \t\r\n]+ -> skip;
NB_STRING_LIT : '"' ('\\"' | .)*? '"';
BSTRING_LIT : 'b' NB_STRING_LIT;
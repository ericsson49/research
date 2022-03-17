grammar TypeExpr;

clsName : '?'? WORD;
//funDecl : WORD LPAR argList RPAR ARR type;
//arg : WORD COL type;
//argList : (args+=arg (COMMA args+=arg)*)?;
type : clsName (LBR typeParamList RBR)? | LPAR typeParamList RPAR ARR type;
typeParamList : (tparams+=type (COMMA tparams+=type)*)?;

clsHead : name=WORD (LBR tparams+=WORD (COMMA tparams+=WORD)* RBR)?;
clsDecl : clsHead ('<:' base=type)?;

arg : (name=WORD COL)? type;
funDecl : name=WORD (LBR targs+=WORD (COMMA targs+=WORD)* RBR)? LPAR (args+=arg (COMMA args+=arg)*)? RPAR ARR resType=type;

fragment LETTER : 'A' .. 'Z' | 'a' .. 'z';
fragment DIGIT : '0' .. '9';
WORD : ('_' | LETTER) ('_' | '.' | '#' | LETTER | DIGIT)*;
LPAR : '(';
RPAR : ')';
LBR : '[';
RBR : ']';
COMMA : ',';
ARR : '->';
COL : ':';

WS  :   [ \t\r\n]+ -> skip;
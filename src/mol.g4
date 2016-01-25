grammar mol;

@header{
    import java.util.ArrayList;
    import java.util.List;
}

options{
    language = Java;
}

@members{
    static Module currentModule = null;
    static Method currentMethod = null;
    static Block currentBlock = null;
    static int methodNumber = 0;
}

program : module*;
module : MODULE a=id[false] {Module m = Main.getModule($a.text, true); currentModule = m; currentBlock = m.blk;}
        ('includes' b=id[true] {if($b.text.equals("float") || $b.text.equals("bool") || $b.text.equals("int"))
            System.out.println("3: module "+$a.text+" includes primitive "+$b.text+"(warning)");
            else m.addInclude(Main.getModule($b.text, false));}
        (',' c=id[true] {if($c.text.equals("float") || $c.text.equals("bool") || $c.text.equals("int"))
            System.out.println("3: module "+$a.text+" includes primitive "+$b.text+"(warning)");
            else m.addInclude(Main.getModule($c.text, false));})* )? BEGIN (member)* END;

member locals[boolean isArray] :
        c=type b=id[false] {currentMethod = currentModule.addMethod($b.text, $c.t); currentMethod.methodNumber=methodNumber;methodNumber++;}
        {currentBlock = currentBlock.addBlock(); currentMethod.blk = currentBlock;} {currentMethod.AddArg("__self", new Type(currentModule, 1), false);}
        '(' ({$isArray = false;}d=type e=id[false]('[' ']' {$isArray = true;})? {currentMethod.AddArg($e.text, $d.t, $isArray);}
        (',' {$isArray = false;}f=type g=id[false]('[' ']' {$isArray = true;})? {currentMethod.AddArg($g.text, $f.t, $isArray);})* )? ')'
        {currentModule.checkSelfMethodDuplicate(currentMethod);}
        block {currentBlock=currentBlock.prev;}
        {if(!$block.hasReturn && !currentMethod.isVoid)
            System.out.println("2: method "+$b.text+" defined @"+$start.getLine()+" which is not void, is not returning(warning)");}
        {currentMethod=null;}
        | vardecl ';';
type returns [Type t] locals[int pCount = 0, Type.PRIMITIVE p = Type.PRIMITIVE.NONE, Module m = null] :
        (VOID {$p = Type.PRIMITIVE.VOID;} | (INT {$p = Type.PRIMITIVE.INT;} | FLOAT {$p = Type.PRIMITIVE.FLOAT;}
        | BOOL {$p = Type.PRIMITIVE.BOOL;} | a=id[false] {$m = Main.getModule($a.text, false);})
        ('*' {$pCount++;})*) {if($m == null) $t=new Type($p, $pCount); else $t=new Type($m, $pCount);};

vardecl locals[Type varT, int arrSize=0]: (type {$varT = $type.t; if($varT.whichPrimitive == Type.PRIMITIVE.VOID)
        {System.out.println("14: @"+$start.getLine()); System.exit(14);}}
        | cons {$varT = $cons.t;}) {$arrSize=0;} a=id[false] ('[' b=CONSTINT {$arrSize = Integer.parseInt($b.text);} ']')?
            {currentBlock.AddVar($a.text ,$varT, $arrSize, currentMethod == null, $start.getLine());}
        (',' {$arrSize=0;} c=id[false] ('[' d=CONSTINT {$arrSize = Integer.parseInt($d.text);} ']')?
            {currentBlock.AddVar($c.text ,$varT, $arrSize, currentMethod == null, $start.getLine());})*;
cons returns [Type t] : a=id[false] {$t=new Type(Main.getModule($a.text, false), 0);} '(' (expr (',' expr)* )? ')';

block returns [boolean hasReturn] : {$hasReturn = false;} BEGIN (statement {$hasReturn = $hasReturn || $statement.hasReturn;})* END;

statement returns [boolean hasReturn] : st {$hasReturn = $st.hasReturn;} | if_statement {$hasReturn = $if_statement.hasReturn;};

st returns [boolean hasReturn] :
    RETURN ';' {$hasReturn = false;}
    | RETURN expr ';' {$hasReturn = true; if(currentMethod.isVoid)
        System.out.println("2: method "+currentMethod.name+" which is void, is returning non void @"+$start.getLine()+"(warning)");}
    | {currentBlock = currentBlock.addBlock();} block {$hasReturn = $block.hasReturn; currentBlock=currentBlock.prev;}
    | FOR '(' (initexpr (',' initexpr)*)? ';' (expr)? ';' (loc '=' expr (',' loc '=' expr)*)? ')' a=st {$hasReturn = $a.hasReturn;}
    | OUTPUT expr ';' {$hasReturn = false;}
    | INPUT loc ';' {$hasReturn = false;}
    | vardecl ';' {$hasReturn = false;}
    | methodcall ';' {$hasReturn = false;}
    | loc '=' expr ';' {$hasReturn = false;};

if_statement returns [boolean hasReturn] : if_statement1 {$hasReturn = $if_statement1.hasReturn;} | if_statement2 {$hasReturn = $if_statement2.hasReturn;} ;
if_statement1 returns [boolean hasReturn] : IF '(' expr ')' a=if_statement1 ELSE b=if_statement1 {$hasReturn = $a.hasReturn && $b.hasReturn;}
    | st {$hasReturn = $st.hasReturn;};
if_statement2 returns [boolean hasReturn]: IF '(' expr ')' statement {$hasReturn = false;}
    | IF '(' expr ')' a=if_statement1 ELSE b=if_statement2 {$hasReturn = $a.hasReturn && $b.hasReturn;};


loc : THIS ('.' loc)?
    | id[false] ('[' expr ']')? ('.' | '->') loc
    | id[false] '(' (expr (',' expr)*)? ')' ('.' | '->') loc
    | id[false] ('[' expr ']')? ;

methodcall : (loc ('.' | '->'))? id[false] '(' (expr (',' expr)*)? ')';


expr	:	expror;
expror	:	exprand expror1| exprand;
expror1	:	OR exprand expror1
          | OR exprand;
exprand	:	exprnot exprand1| exprnot;
exprand1    :	AND exprnot exprand1
          | AND exprnot;
exprnot	:	NOT exprnot
          | expreq;
expreq	:	exprcom expreq1| exprcom;
expreq1	:	i=('=='|'=!') exprcom expreq1
          | ('=='|'=!') exprcom;
exprcom	:	exprsum exprcom1| exprsum;
exprcom1	:   ('<'|'>'|'<='|'>=') exprsum exprcom1
          | ('<'|'>'|'<='|'>=') exprsum;
exprsum	:	exprmul exprsum1 | exprmul;
exprsum1	:	('+'|'-') exprmul exprsum1
          | ('+'|'-') exprmul;
exprmul	:	expraddress exprmul1| expraddress;
exprmul1	:	('*'|'/') expraddress exprmul1
          | ('*'|'/') expraddress;
expraddress :	'&' expraddress |  exprref;
exprref :	'*' exprmi | exprmi;
exprmi	:	'-' exprmi | exprother;

exprother	:	(CONSTFLOAT|CONSTINT|TRUE|FALSE)
	|	methodcall
	|	cons
	|	'(' expr ')'
	|	loc;

initexpr : vardecl | loc '=' expr;

id[boolean inc] : a=(MODULE | INT | FLOAT | BOOL | TRUE | FALSE | AND | NOT | OR | THIS
                    | IF | ELSE | OUTPUT | INPUT | RETURN | FOR | VOID | BEGIN | END | INCLUDES | ID)
                    {if(!$inc && Main.reservedWords.contains($a.text)){
                        System.out.println("6: illegal use of "+$a.text+" @"+$start.getLine()+"(error)");
                        Main.hasFirstPassError = true;
                    }};

//Lexer
MODULE : 'module';
INT : 'int';
FLOAT : 'float';
BOOL : 'bool';
VOID : 'void';
TRUE : 'true';
FALSE : 'false';
AND : 'and';
NOT : 'not';
OR : 'or';
THIS : 'this';
IF : 'if';
ELSE : 'else';
OUTPUT : 'output';
INPUT : 'input';
RETURN : 'return';
FOR : 'for';
BEGIN : 'begin';
END : 'end';
INCLUDES : 'includes';
CONSTINT : (('-')? ([1-9][0-9]*)) | [0];
CONSTFLOAT : ('-')?(([1-9][0-9]*)|[0])['.'][0-9]+;
ID : ['_'a-zA-Z][a-zA-Z0-9'_']*;
WHITESPACE : [ ' '|'\r'|'\n'|'\t']+ ->skip ;
COMMENT : '%%%' .*? ('%%%'| EOF) ->  skip;
LINE_COMMENT  : '%%' .*? '\n' -> skip ;
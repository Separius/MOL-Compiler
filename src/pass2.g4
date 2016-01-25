grammar pass2;

//TODO arg=array, pass the pointer

@header{
    import java.util.ArrayList;
    import java.util.List;
    import java.util.Vector;
}

options{
    language = Java;
}

@members{
    static Module currentModule = null;
    static Method currentMethod = null;
    static Block currentBlock = null;
    static String currentReturnLabel = null;
}

program : module*;
module  : MODULE a=ID{currentModule = Main.getModule($a.text, false); currentBlock = currentModule.blk;}
        ('includes' b=ID (',' c=ID)* )? 'begin' (member)* 'end';

member locals[boolean isArray, List<Type> args = new ArrayList<Type>(), Variable newVar]: type a=ID {currentBlock=currentBlock.blocks.get(currentBlock.pass2Counter);}
        {$args.add(new Type(currentModule, 1));}
        '(' ({$isArray = false;}d=type e=ID('[' ']' {$isArray = true;})? {$newVar = new Variable($e.text, $d.t); if($isArray) $newVar.PromoteToArray(0); $args.add($newVar.type);}
        (',' {$isArray = false;}f=type g=ID('[' ']' {$isArray = true;})? {$newVar = new Variable($e.text, $d.t); if($isArray) $newVar.PromoteToArray(0); $args.add($newVar.type);})* )? ')'
        {currentMethod = currentModule.getMethod($a.text,$args);}
        {Main.EnterFunction(currentMethod);currentReturnLabel=Main.GenerateLabel(currentMethod.name+"Return");}block
        {Main.PutLabel(currentReturnLabel);Main.ExitFunction(currentMethod);currentBlock=currentBlock.prev;currentBlock.pass2Counter++;currentMethod=null;}
        | vardecl ';';

type returns [Type t] locals[int pCount = 0, Type.PRIMITIVE p = Type.PRIMITIVE.NONE, Module m = null] :
        ('void' {$p = Type.PRIMITIVE.VOID;} | ('int' {$p = Type.PRIMITIVE.INT;} | 'float' {$p = Type.PRIMITIVE.FLOAT;}
        | 'bool' {$p = Type.PRIMITIVE.BOOL;} | a=ID {$m = Main.getModule($a.text, false);})
        ('*' {$pCount++;})*) {if($m == null) $t=new Type($p, $pCount); else $t=new Type($m, $pCount);};

//TODO handle cons (copy from stack to ID.addr) => shit, self ? => must be side effect less
vardecl : (type | {Main.StackBeforeCall();}cons ) ID ('[' CONSTINT ']')? (',' ID('[' CONSTINT ']')? )*;
cons returns [Type t] locals[List<Type> args = new ArrayList<Type>(), Method theMethod] :
        a=ID {$args.add(new Type(Main.getModule($a.text, false), 1));} '(' (b=expr {$args.add($b.t);} (',' c=expr {$args.add($c.t);})* )? ')'
        {$theMethod = Main.checkCons($a.text, $args); $t=null; if($theMethod==null) System.out.println("cons does not exist @"+$start.getLine()); else $t=$theMethod.retType;
        Main.CallFunction($theMethod);};
block : 'begin' (statement)* 'end';

statement : st | if_statement ;

st locals[String l1, String l2, String l3, String l4]:
    {currentBlock=currentBlock.blocks.get(currentBlock.pass2Counter); Main.EnterBlock(currentBlock);} block
    {Main.ExitBlock(currentBlock);currentBlock=currentBlock.prev;currentBlock.pass2Counter++;}
    | vardecl ';'
    | methodcall ';' {Main.Flush($methodcall.t.correctSize);}
    | 'for' '(' (initexpr (',' initexpr)*)? ';'
        {$l1=Main.GenerateLabel("FOR_BE");$l2=Main.GenerateLabel("FOR_UPDATE");$l3=Main.GenerateLabel("FOR_BODY");$l4=Main.GenerateLabel("FOR_END");Main.PutLabel($l1);}(expr
        {if($expr.t.whichPrimitive != Type.PRIMITIVE.BOOL) {System.out.println("18: @"+$start.getLine());Main.hasSecondPassError=true;}Main.Flush(4);Main.BranchFalse($l4);})?
        {Main.Jump($l3);}';' {Main.PutLabel($l2);} (inithelper (',' inithelper)*)? {Main.Jump($l1);} ')'{Main.PutLabel($l3);}st{Main.Jump($l2);}{Main.PutLabel($l4);}
    | inithelper ';'
    | 'return' ';' {Main.Jump(currentReturnLabel);}
    | 'return' expr ';' {if(!$expr.t.lessThan(currentMethod.retType)) {System.out.println("23: @"+$start.getLine());Main.hasSecondPassError=true;}Main.ConvertIntToFloat($expr.t,currentMethod.retType);Main.Jump(currentReturnLabel);}
    | 'input' {Main.CallLOC();} loc ';' {if($loc.t.whichPrimitive==Type.PRIMITIVE.NONE) {System.out.println("17: @"+$start.getLine());Main.hasSecondPassError=true;}
    Main.GetInput($loc.t.whichPrimitive);Main.Flush(4);}
    | 'output' expr ';' {if($expr.t.whichPrimitive==Type.PRIMITIVE.NONE) {System.out.println("17: @"+$start.getLine());Main.hasSecondPassError=true;}
    Main.PutOutput($expr.t.whichPrimitive);Main.Flush(4);};

if_statement : if_statement1 | if_statement2;
if_statement1 locals[String l1, String l2] : {$l1=Main.GenerateLabel("IF1_ELSE");$l2=Main.GenerateLabel("IF1_END");}
        'if' '(' expr {if($expr.t.whichPrimitive != Type.PRIMITIVE.BOOL) {System.out.println("18: @"+$start.getLine());Main.hasSecondPassError=true;}} ')'
         {Main.Flush(4);Main.BranchFalse($l1);}if_statement1 {Main.Jump($l2);} 'else' {Main.PutLabel($l1);} if_statement1 {Main.PutLabel($l2);} | st;
if_statement2 locals[String l1, String l2] : {$l1=Main.GenerateLabel("IF2_FALSE");}'if' '(' expr {if($expr.t.whichPrimitive != Type.PRIMITIVE.BOOL) {System.out.println("18: @"+$start.getLine());Main.hasSecondPassError=true;}} ')'
        {Main.Flush(4);Main.BranchFalse($l1);}statement{Main.PutLabel($l1);} | {$l1=Main.GenerateLabel("IF2_ELSE");$l2=Main.GenerateLabel("IF2_END");}'if' '(' expr ')'
        {Main.Flush(4);Main.BranchFalse($l1);}if_statement1 {Main.Jump($l2);} 'else' {Main.PutLabel($l1);} if_statement2 {Main.PutLabel($l2);};

loc returns [Type t] locals[List<Type> args = new ArrayList<Type>(), int helper=0,
                    Module savedCurrentModule = currentModule, Block savedCurrentBlock = currentBlock] :
    (a=ID ('[' expr {if($expr.t.whichPrimitive != Type.PRIMITIVE.INT) {System.out.println("21: @"+$start.getLine()); System.exit(21);}} ']' {$helper=1;})?
    {$t=currentBlock.findTypeOfId($a.text); if($t == null) {System.out.println("13: @"+$start.getLine()); System.exit(13);Main.hasSecondPassError=true;}
    if($helper == 1 && $t.arrayOf == null) {System.out.println("13: @"+$start.getLine());System.exit(13);Main.hasSecondPassError=true;} else if($helper == 1) $t=$t.arrayOf;}
    {Main.FindStackAddr($a.text, $helper == 1, false, currentBlock);}
    | c=ID ('[' expr {if($expr.t.whichPrimitive != Type.PRIMITIVE.INT) {System.out.println("21: @"+$start.getLine());System.exit(21); Main.hasSecondPassError=true;}}']'
    {$helper=1;})? ('.' {$helper+=2;} | '->'{$helper+=4;}) {
        $t=currentBlock.findTypeOfId($c.text);
        if($t == null){
            System.out.println("13: @"+$start.getLine());
            System.exit(13);
            Main.hasSecondPassError=true;
        }
        if($helper == 2) //id.
            if($t.objFrom == null){
                System.out.println("13: @"+$start.getLine());
                System.exit(13);
                Main.hasSecondPassError=true;
            }
            else
                currentModule = $t.objFrom;
        else if($helper == 4) //id->
            if($t.pointerTo != null && $t.pointerTo.objFrom != null)
                currentModule = $t.pointerTo.objFrom;
            else{
                System.out.println("13: @"+$start.getLine());
                System.exit(13);
                Main.hasSecondPassError=true;
            }
        else if($helper == 3) //id[].
            if($t.arrayOf != null && $t.arrayOf.objFrom != null)
                currentModule = $t.arrayOf.objFrom;
            else{
                System.out.println("13: @"+$start.getLine());
                System.exit(13);
                Main.hasSecondPassError=true;
            }
        else{ //id[]->
            if($t.arrayOf != null && $t.arrayOf.pointerTo != null && $t.arrayOf.pointerTo.objFrom != null)
                currentModule = $t.arrayOf.pointerTo.objFrom;
            else{
                System.out.println("13: @"+$start.getLine());
                System.exit(13);
                Main.hasSecondPassError=true;
            }
        }
        Main.FindStackAddr($c.text, ($helper%2) == 1, ($helper>=4), currentBlock);
        currentBlock = currentModule.blk;
    } b=loc {$t = $b.t;}
    | a=ID {$args.add(new Type(currentModule, 1));Main.StackBeforeCall();} '(' (g=expr {$args.add($g.t);} (',' d=expr {$args.add($d.t);})*)? ')'
    ('.' {$helper = 1;} | '->' {$helper = 2;Main.DerefSelf();}){
    $t = currentModule.methodReturnType($a.text, $args);
    if($t == null){
        System.out.println("13: @"+$start.getLine());
        System.exit(13);
        Main.hasSecondPassError=true;
    }
    if($helper == 1)
        if($t.objFrom == null){
            System.out.println("13: @"+$start.getLine());
            System.exit(13);
            Main.hasSecondPassError=true;
        }
        else{
            currentModule = $t.objFrom;
            Main.LOConTheFlyA0($t);
        }
    else{
        if($t.pointerTo != null && $t.pointerTo.objFrom != null){
            currentModule = $t.pointerTo.objFrom;
            Main.StoreA0();
        }
        else{
            System.out.println("13: @"+$start.getLine());
            System.exit(13);
            Main.hasSecondPassError=true;
        }
    }
    Main.LOConTheFly($t,$helper==2);
    currentBlock = currentModule.blk;
    }  e=loc {Main.FlushUnsafe($t);$t = $e.t;}
    | 'this' {Main.DerefSelf();currentBlock = currentModule.blk;} {$t = new Type(currentModule, 0);} ('.' h=loc {$t = $h.t;})?)
    {currentModule = $savedCurrentModule; currentBlock = $savedCurrentBlock;};

methodcall returns[Type t = null] locals[int helper=0, List<Type> args = new ArrayList<Type>()] :
    ({Main.CallLOC();}loc ('.'{$helper=1;} | '->'{$helper=2;}) {$t=$loc.t;})?
    a=ID {if($helper == 0) {$args.add(new Type(currentModule, 1));Main.CallLOC();}else if($helper==1) $args.add(new Type($t.objFrom, 1)); else {$args.add($t);Main.DerefSelf();} Main.StackBeforeCall();}
    '(' (b=expr {$args.add($b.t);} (',' c=expr {$args.add($c.t);})*)? ')'{
    if($helper == 0){
        $t = currentModule.methodReturnType($a.text, $args);
        if($t == null){
            System.out.println("12: @"+$start.getLine());
            System.exit(12);
        }
        Main.CallFunction(currentModule.getMethod($a.text, $args));
    }else if($helper == 1){
        if($t.objFrom == null){
            System.out.println("12: @"+$start.getLine());
            System.exit(12);
        }
        Main.CallFunction($t.objFrom.getMethod($a.text, $args));
        $t = $t.objFrom.methodReturnType($a.text, $args);
    }else{
        if($t.pointerTo == null || $t.pointerTo.objFrom == null){
            System.out.println("12: @"+$start.getLine());
            System.exit(12);
        }
        Main.CallFunction($t.pointerTo.objFrom.getMethod($a.text, $args));
        $t = $t.pointerTo.objFrom.methodReturnType($a.text, $args);
    }
    };
expr returns[Type t]	:	expror {$t=$expror.t;};
expror	returns[Type t] :	exprand {$t=$exprand.t;} (expror1[$t] {$t=$expror1.t;})?;
expror1[Type lhs] returns[Type t] :	'or' exprand
        {if($lhs.whichPrimitive == Type.PRIMITIVE.BOOL && $exprand.t.whichPrimitive == Type.PRIMITIVE.BOOL)
            $t = new Type(Type.PRIMITIVE.BOOL, 0);
        else
            {System.out.println("16: @"+$start.getLine());Main.hasSecondPassError=true;System.exit(16);}
        Main.TwoOp(0, $lhs, $exprand.t);
        }(a=expror1[$t] {$t=$a.t;})?;
exprand	returns[Type t] :	exprnot {$t=$exprnot.t;} (exprand1[$t] {$t=$exprand1.t;})?;
exprand1[Type lhs] returns[Type t] : 'and' exprnot
                {if($lhs.whichPrimitive == Type.PRIMITIVE.BOOL && $exprnot.t.whichPrimitive == Type.PRIMITIVE.BOOL)
                    $t = new Type(Type.PRIMITIVE.BOOL, 0);
                else
                    {System.out.println("16: @"+$start.getLine());Main.hasSecondPassError=true;System.exit(16);}
                Main.TwoOp(1, $lhs, $exprnot.t);
                }(a=exprand1[$t] {$t = $a.t;})?;
exprnot	returns[Type t] : 'not' a=exprnot {if($a.t.whichPrimitive == Type.PRIMITIVE.BOOL) $t = new Type(Type.PRIMITIVE.BOOL, 0);
                            else {System.out.println("16: @"+$start.getLine());Main.hasSecondPassError=true;System.exit(16);}
                            Main.Not();} | expreq {$t = $expreq.t;};
expreq	returns[Type t] : exprcom {$t=$exprcom.t;} (expreq1[$t]{$t=$expreq1.t;})?;
expreq1[Type lhs] returns[Type t] locals[int which] : ('=='{$which=4;}|'=!'{$which=5;}) exprcom {
        if($exprcom.t.whichPrimitive == $lhs.whichPrimitive && $lhs.whichPrimitive != Type.PRIMITIVE.NONE)
            $t = new Type(Type.PRIMITIVE.BOOL, 0);
        else
            {System.out.println("16: @"+$start.getLine());Main.hasSecondPassError=true;System.exit(16);}
        Main.Comp($which, $lhs, $exprcom.t);
        } (b=expreq1[$t] {$t=$b.t;})?;
exprcom returns[Type t] : exprsum {$t=$exprsum.t;} (exprcom1[$t] {$t=$exprcom1.t;})?;
exprcom1[Type lhs] returns[Type t] locals[int which] : ('<'{$which=0;}|'>'{$which=1;}|'<='{$which=2;}|'>='{$which=3;}) exprsum
        {if(Main.fOrI($lhs) && Main.fOrI($exprsum.t))
            $t=new Type(Type.PRIMITIVE.BOOL, 0);
        else
            {System.out.println("15: @"+$start.getLine());Main.hasSecondPassError=true;System.exit(15);}
        Main.Comp($which, $lhs, $exprsum.t);
        } (exprcom1[$t])?;
exprsum	returns[Type t] : exprmul{$t=$exprmul.t;} (exprsum1[$t] {$t=$exprsum1.t;})?;
exprsum1[Type lhs] returns[Type t] locals[int which] : ('+'{$which=2;}|'-'{$which=3;}) exprmul {
        if(Main.fOrI($lhs) && Main.fOrI($exprmul.t)){
            if(($lhs.whichPrimitive == Type.PRIMITIVE.INT) && ($exprmul.t.whichPrimitive == Type.PRIMITIVE.INT))
                $t=new Type(Type.PRIMITIVE.INT, 0);
            else
                $t=new Type(Type.PRIMITIVE.FLOAT, 0);
        }else
            {System.out.println("15: @"+$start.getLine());Main.hasSecondPassError=true;System.exit(15);}
        Main.TwoOp($which, $lhs, $exprmul.t);
        } (a=exprsum1[$t] {$t=$a.t;})?;
exprmul	returns[Type t] : expraddress {$t=$expraddress.t;} (exprmul1[$t] {$t=$exprmul1.t;})?;
exprmul1[Type lhs] returns[Type t] locals[int which]: ('*'{$which=4;}|'/'{$which=5;}) expraddress{
        if(Main.fOrI($lhs) && Main.fOrI($expraddress.t)){
            if(($lhs.whichPrimitive == Type.PRIMITIVE.INT) && ($expraddress.t.whichPrimitive == Type.PRIMITIVE.INT))
                $t=new Type(Type.PRIMITIVE.INT, 0);
            else
                $t=new Type(Type.PRIMITIVE.FLOAT, 0);
        }else
            {System.out.println("15: @"+$start.getLine());Main.hasSecondPassError=true;System.exit(15);}
        Main.TwoOp($which, $lhs, $expraddress.t);
        } (a=exprmul1[$t] {$t=$a.t;})?;
expraddress returns[Type t] : '&' a=expraddress{if($a.t.virt) {System.out.println("25: @"+$start.getLine());System.exit(25);} Main.Ref($a.t); $t=new Type($a.t, true);} |  exprref {$t=$exprref.t;};
exprref returns[Type t] locals[boolean ref=false] : ('*' {$ref=true;})? exprmi{if(!$ref) $t=$exprmi.t; else{
            if($exprmi.t.pointerTo == null){
                System.out.println("26: @"+$start.getLine());
                System.exit(26);
            }
            $t=$exprmi.t.pointerTo;
            Main.Deref($t);
        }};
exprmi returns[Type t] : '-' a=exprmi {if(Main.fOrI($a.t)) $t=$a.t; else {System.out.println("15: @"+$start.getLine());
                                Main.hasSecondPassError=true;System.exit(15);}Main.Negate($a.t);} | exprother{$t=$exprother.t;};
exprother returns[Type t] :	{Main.CallLOC();}loc {$t=$loc.t;Main.T0Hack($t);}
	|	methodcall{$t=$methodcall.t;}
	//TODO handle properly
	|	{Main.StackBeforeCall();}cons{$t=$cons.t;}
	|	'(' expr ')' {$t=$expr.t;}
	|	(CONSTFLOAT {$t=new Type(Type.PRIMITIVE.FLOAT,0); Main.PushFloat($CONSTFLOAT.text);}|CONSTINT{$t=new Type(Type.PRIMITIVE.INT,0);Main.PushConst($CONSTINT.text);}
	|   'true'{$t=new Type(Type.PRIMITIVE.BOOL,0); Main.PushConst("1");}
	|   'false'{$t=new Type(Type.PRIMITIVE.BOOL,0); Main.PushConst("0");});

initexpr : vardecl | inithelper;
inithelper :
    {Main.CallLOC();}loc '=' expr {if(!$expr.t.lessThan($loc.t)) {System.out.println("19: @"+$start.getLine()); Main.hasSecondPassError=true;}Main.Assignment($loc.t, $expr.t);};

//Lexer
MODULE : 'module';
CONSTINT : ([1-9][0-9]*) | [0];
CONSTFLOAT : (([1-9][0-9]*)|[0])['.'][0-9]+;
ID : ['_'a-zA-Z][a-zA-Z0-9'_']*;
WHITESPACE : [ ' '|'\r'|'\n'|'\t']+ ->skip ;

COMMENT : '%%%' .*? ('%%%'| EOF) ->  skip;
LINE_COMMENT  : '%%' .*? '\n' -> skip ;
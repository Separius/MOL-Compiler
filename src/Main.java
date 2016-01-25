import org.antlr.v4.runtime.ANTLRFileStream;
import org.antlr.v4.runtime.CharStream;
import org.antlr.v4.runtime.CommonTokenStream;

import java.io.PrintWriter;
import java.util.*;

public class Main {
    public static List<Module> moduleList = new ArrayList<Module>();
    public static boolean hasFirstPassError = false;
    public static boolean hasSecondPassError = false;
    static int pc = 0;
    static boolean accIsFree = true;
    static Vector<String> programBlock=new Vector<String>();
    public static Module getModule(String name, boolean hasBody){
        for (Module mod:moduleList)
            if (mod.name.equals(name)) {
                if (mod.hasBody && hasBody) {
                    System.out.println("7: duplicated module " + mod.name + ", ignored(warning)");
                    hasFirstPassError = true;
                }
                else if (hasBody)
                    mod.hasBody = true;
                return mod;
            }
        Module returnValue = new Module(name, hasBody);
        moduleList.add(returnValue);
        return returnValue;
    }
    static boolean fOrI(Type t){
        return(t!=null && (t.whichPrimitive == Type.PRIMITIVE.INT || t.whichPrimitive == Type.PRIMITIVE.FLOAT));
    }
    public static Method checkCons(String name, List<Type> args){
        for (Module mod:moduleList)
            if (mod.name.equals(name)) {
                for(Method m : mod.methods){
                    if(m.args.size() == args.size()) {
                        boolean found = true;
                        for(int i=0;i<args.size() && found;++i)
                            if(!args.get(i).lessThan(m.args.get(i).type))
                                found = false;
                        if(found) {
                            return m;
                        }
                    }
                }
            }
        return null;
    }
    public static Set<String> reservedWords = new HashSet<String>();
    static {
        reservedWords.add("module");
        reservedWords.add("includes");
        reservedWords.add("begin");
        reservedWords.add("end");
        reservedWords.add("int");
        reservedWords.add("float");
        reservedWords.add("bool");
        reservedWords.add("void");
        reservedWords.add("for");
        reservedWords.add("return");
        reservedWords.add("input");
        reservedWords.add("output");
        reservedWords.add("if");
        reservedWords.add("else");
        reservedWords.add("this");
        reservedWords.add("or");
        reservedWords.add("and");
        reservedWords.add("not");
        reservedWords.add("true");
        reservedWords.add("false");
    }
    public static void LOConTheFly(Type locType,boolean isPointer){
        if(isPointer)
            Main.programBlock.add("lw $a0, 0($a0)");
        else{
            programBlock.add("sw $a0, 0($sp)");
            programBlock.add("addiu $sp, $sp, -4");
            programBlock.add("addiu $a0, $sp, "+locType.correctSize);
            pc+=2;
        }
        pc++;
    }
    public static void T0Hack(Type locType){
        programBlock.add("move $t0, $a0");
        programBlock.add("lw $a0, 0($t0)");
        for(int i=4; i < locType.correctSize; i+=4){
            programBlock.add("sw $a0, -"+(i-4)+"($sp)");
            programBlock.add("lw $a0, -"+i+"($t0)");
            pc+=2;
        }
        programBlock.add("addiu $sp, $sp, -"+(locType.correctSize-4));
        pc+=3;
    }
    public static void Assignment(Type lhsLOCType, Type rhsExprType){
        if((lhsLOCType.whichPrimitive == Type.PRIMITIVE.FLOAT) && (rhsExprType.whichPrimitive == Type.PRIMITIVE.INT)){
            programBlock.add("lw $t1, 4($sp)");
            programBlock.add("addiu $sp, $sp, 4");
            programBlock.add("mtc1 $a0, $f12");
            programBlock.add("cvt.w.s $f12, $f12");//convert int to single
            programBlock.add("mfc1 $a0, $f12");
            programBlock.add("sw $a0, 0($t1)");
            pc+=6;
        }else{
            programBlock.add("sw $a0, 0($sp)");
            programBlock.add("lw $t1, "+lhsLOCType.correctSize+"($sp)");
            for(int i = 0; i < lhsLOCType.correctSize; i+=4){
                programBlock.add("lw $a0, "+i+"($sp)");
                programBlock.add("sw $a0, "+(-lhsLOCType.correctSize+i+4)+"($t1)");
                pc+=2;
            }
            programBlock.add("addiu $sp, $sp, "+lhsLOCType.correctSize);
            pc+=4;
        }
        accIsFree = true;
    }
    public static void DerefSelf(){
        Main.programBlock.add("lw $a0, 0($a0)");
        pc++;
    }
    public static void FindStackAddr(String id, boolean isArray, boolean isPointer, Block currBlk){
        Block blockIterator = currBlk;
        while(blockIterator != null){
            if(blockIterator.prev == null && currBlk.prev != null) {
                Main.programBlock.add("lw $a0, 0($a0)");
                pc++;
            }
            for(Variable var : blockIterator.vars)
                if(var.name.equals(id)){
                    int ans = var.relAddr + blockIterator.baseAddr;
                    if(isArray) {
                        programBlock.add("li $t1, "+var.type.arrayOf.correctSize);
                        programBlock.add("mul $a0, $t1, $a0");
                        programBlock.add("lw $t1, 4($sp)"); //t1=fp, a0=offset
                        programBlock.add("addiu $a0, $a0, "+ans);
                        programBlock.add("sub $a0, $t1, $a0");
                        programBlock.add("addiu $sp, $sp, 4");
                        if(isPointer){
                            Main.programBlock.add("lw $a0, 0($a0)");
                            pc++;
                        }
                        pc+=6;
                        accIsFree = false;
                        return;
                    }
                    if(isPointer)
                        Main.programBlock.add("lw $a0, -"+ans+"($a0)");
                    else
                        Main.programBlock.add("addiu $a0, $a0, -"+ans);
                    pc++;
                    accIsFree = false;
                    return;
                }
            blockIterator = blockIterator.prev;
        }
    }
    public static void Negate(Type inType){
        if(inType.whichPrimitive == Type.PRIMITIVE.INT) {
            Main.programBlock.add("neg $a0, $a0");
            Main.pc++;
        }else{
            programBlock.add("mtc1 $a0, $f0");
            programBlock.add("neg.s $f0, $f0");
            programBlock.add("mfc1 $a0, $f0");
            Main.pc+=3;
        }
    }
    public static void Deref(Type inType){
        programBlock.add("move $t1, $a0");
        programBlock.add("lw $a0, 0($t1)");
        for(int i=4; i < inType.getCorrectSize(); i+=4){
            programBlock.add("sw $a0, "+(i-4)+"($sp)");
            programBlock.add("lw $a0, "+i+"($t1)");
            pc+=2;
        }
        programBlock.add("addiu $sp, $sp, -"+(inType.correctSize-4));
        Main.pc+=3;
    }
    public static void StackBeforeCall(){
        programBlock.add("addiu $sp, $sp, -8");
    }
    public static void LOConTheFlyA0(Type t){
        programBlock.add("sw $a0, 0($sp)");
        programBlock.add("addiu $sp, $sp, -4");
        programBlock.add("addiu $a0, $sp, "+t.correctSize);
        pc+=3;
    }
    public static void StoreA0(){
        programBlock.add("sw $a0, 0($sp)");
        programBlock.add("addiu $sp, $sp, -4");
        pc+=2;
    }
    public static void FlushUnsafe(Type t){
        programBlock.add("addiu $sp, $sp, "+t.correctSize);
        pc++;
    }
    public static void CallLOC(){
        if(!accIsFree){
            programBlock.add("sw $a0, 0($sp)");
            programBlock.add("addiu $sp, $sp, -4");
            pc+=2;
        }
        programBlock.add("move $a0, $fp");
        accIsFree = false;
        pc++;
    }
    public static void Jump(String inLabel){
        programBlock.add("j "+inLabel);
        pc++;
    }
    public static void BranchFalse(String inLabel){
        programBlock.add("beqz $a0, "+inLabel);
        pc++;
    }
    public static void Flush(int size){
        accIsFree = true;
        if(size == 4)
            return;
        programBlock.add("addiu $sp, $sp, "+(size-4));
        pc++;
    }
    public static void GetInput(Type.PRIMITIVE prim){
        if(prim == Type.PRIMITIVE.FLOAT) {
            programBlock.add("li $v0, 6");
            programBlock.add("syscall");
            programBlock.add("mfc1 $t1, $f0");
            programBlock.add("sw $t1, 0($a0)");
            pc+=4;
        }else if(prim == Type.PRIMITIVE.INT) {
            programBlock.add("li $v0, 5");
            programBlock.add("syscall");
            programBlock.add("sw $v0, 0($a0)");
            pc+=3;
        }
    }
    public static void PutOutput(Type.PRIMITIVE prim){
        if(prim == Type.PRIMITIVE.FLOAT) {
            programBlock.add("li $v0, 2");
            programBlock.add("mtc1 $a0, $f12");
            programBlock.add("syscall");
            pc+=3;
        }else if(prim == Type.PRIMITIVE.INT) {
            programBlock.add("li $v0, 1");
            programBlock.add("syscall");
            pc+=2;
        }
    }
    public static void Ref(Type inType){
        programBlock.add("move $a0, $t0");
        programBlock.add("addiu $sp, $sp, "+(inType.correctSize-4));
        accIsFree = false;
        pc+=3;
    }
    public static String GenerateLabel(String salt){
        return "Label"+Integer.toString(pc-1)+salt;
    }
    public static void PutLabel(String inLabel){
        programBlock.add(inLabel+" :");
        pc++;
    }
    public static void EnterFunction(Method meth){
        programBlock.add("METHOD"+meth.methodNumber+" :");
        programBlock.add("sw $a0, 0($sp)");
        programBlock.add("addiu $sp, $sp, -"+(4+meth.LocalsSize()));
        programBlock.add("sw $fp, "+(meth.blk.currentRelativeAddr+4)+"($sp)");
        programBlock.add("sw $ra, "+(meth.blk.currentRelativeAddr+8)+"($sp)");
        programBlock.add("addiu $fp, $sp, "+(meth.blk.currentRelativeAddr));
        pc+=6;
        accIsFree=true;
        /*layout : oldra | oldfp | self | other args | locals == stack , a0 = free*/
    }
    public static void ExitFunction(Method meth){
        programBlock.add("sw $a0, 0($sp)");
        programBlock.add("addiu $a0, $sp, "+(meth.retType.getCorrectSize()-4)); //a0 points to the begining of the return value
        programBlock.add("lw $ra, 8($fp)");
        programBlock.add("addiu $t1, $fp, 8");//t1 points to the begining of ans
        programBlock.add("lw $fp, 4($fp)");
        programBlock.add("move $sp, $a0");
        for(int i=0; i<meth.retType.correctSize; i+=4){
            programBlock.add("lw $a0, -"+i+"($sp)");
            programBlock.add("sw $a0, -"+i+"($t1)");
            pc+=2;
        }
        programBlock.add("addiu $sp, $t1, "+(-(meth.retType.correctSize-4)));
        programBlock.add("jr $ra");
        pc+=8;
        accIsFree = false;
    }
    public static void CallFunction(Method meth){
        programBlock.add("jal METHOD"+meth.methodNumber);
        pc++;
    }
    public static void EnterBlock(Block curr){
        accIsFree = true;
        programBlock.add("addiu $sp, $sp, -"+curr.currentRelativeAddr);
    }
    public static void ExitBlock(Block curr){
        accIsFree = true;
        programBlock.add("addiu $sp, $sp, "+curr.currentRelativeAddr);
    }
    public static void TwoOp(int which, Type t1Type, Type a0Type) {
        if((t1Type.whichPrimitive == Type.PRIMITIVE.BOOL) && (a0Type.whichPrimitive == Type.PRIMITIVE.BOOL)) {
        programBlock.add("lw $t1, 4($sp)");
        if (which == 0)
            programBlock.add("or $a0, $t1, $a0");
        else if (which == 1)
            programBlock.add("and $a0, $t1, $a0");
            pc+=2;
        }
        if((t1Type.whichPrimitive == Type.PRIMITIVE.INT) && (a0Type.whichPrimitive == Type.PRIMITIVE.INT)) {
            programBlock.add("lw $t1, 4($sp)");
            if (which == 0)
                programBlock.add("or $a0, $t1, $a0");
            else if (which == 1)
                programBlock.add("and $a0, $t1, $a0");
            else if (which == 2)
                programBlock.add("add $a0, $t1, $a0");
            else if (which == 3)
                programBlock.add("sub $a0, $t1, $a0");
            else if (which == 4)
                programBlock.add("mul $a0, $t1, $a0");
            else if (which == 5)
                programBlock.add("div $a0, $t1, $a0");
            programBlock.add("addiu $sp, $sp, 4");
            pc += 3;
        }else{
            programBlock.add("lw $t1, 4($sp)");
            programBlock.add("mtc1 $t1, $f12");
            programBlock.add("mtc1 $a0, $f11");
            if(t1Type.whichPrimitive == Type.PRIMITIVE.INT){
                programBlock.add("cvt.w.s $f12, $f12");//convert int to single
                pc++;
            }else if(a0Type.whichPrimitive == Type.PRIMITIVE.INT){
                programBlock.add("cvt.w.s $f11, $f11");//convert int to single
                pc++;
            }
            if (which == 2)
                programBlock.add("add.s $f11, $f12, $f11");
            else if (which == 3)
                programBlock.add("sub.s $f11, $f12, $f11");
            else if (which == 4)
                programBlock.add("mul.s $f11, $f12, $f11");
            else if (which == 5)
                programBlock.add("div.s $f11, $f12, $f11");
            programBlock.add("mfc1 $a0, $f11");
            programBlock.add("addiu $sp, $sp, 4");
            pc+=6;
        }
    }
    public static void Not(){
        programBlock.add("not $a0, $a0");
        pc++;
    }
    public static void ConvertIntToFloat(Type inType, Type expected){
        if((inType.whichPrimitive == Type.PRIMITIVE.INT) &&(expected.whichPrimitive == Type.PRIMITIVE.FLOAT)){
            programBlock.add("mtc1 $a0, $f11");
            programBlock.add("cvt.w.s $f11, $f11");
            programBlock.add("mfc1 $a0, $f11");
            pc+=3;
        }
    }
    public static void Comp(int which, Type t1Type, Type a0Type){
        programBlock.add("lw $t1, 4($sp)");
        if(t1Type.whichPrimitive == Type.PRIMITIVE.FLOAT){
            programBlock.add("mtc1 $t1, $f12");
            programBlock.add("cvt.s.w $f12, $f12");
            programBlock.add("mfc1 $t1, $f12");
            pc+=3;
        }
        if(a0Type.whichPrimitive == Type.PRIMITIVE.FLOAT){
            programBlock.add("mtc1 $a0, $f11");
            programBlock.add("cvt.s.w $f11, $f11");
            programBlock.add("mfc1 $a0, $f11");
            pc+=3;
        }
            if (which == 0)
                programBlock.add("slt $a0, $t1, $a0");
            else if (which == 1)
                programBlock.add("sgt $a0, $t1, $a0");
            else if (which == 2)
                programBlock.add("sle $a0, $t1, $a0");
            else if (which == 3)
                programBlock.add("sge $a0, $t1, $a0");
            else if (which == 4)
                programBlock.add("seq $a0, $t1, $a0");
            else if (which == 5)
                programBlock.add("sne $a0, $t1, $a0");
            programBlock.add("addiu $sp, $sp, 4");
            pc += 3;
    }
    public static void PushConst(String b){
        if(accIsFree){
            programBlock.add("li $a0, "+b);
            accIsFree = false;
            pc++;
        }else{
            programBlock.add("sw $a0, 0($sp)");
            programBlock.add("addiu $sp, $sp, -4");
            programBlock.add("li $a0, "+b);
            pc+=3;
        }
    }
    public static void PushFloat(String b){
        if(accIsFree){
            programBlock.add("li.s $f0, "+b);
            programBlock.add("mfc1 $a0, $f0");
            accIsFree = false;
            pc+=2;
        }else{
            programBlock.add("sw $a0, 0($sp)");
            programBlock.add("addiu $sp, $sp, -4");
            programBlock.add("li.s $f0, "+b);
            programBlock.add("mfc1 $a0, $f0");
            pc+=4;
        }
    }
    public static void main(String[] args) throws Exception {
        programBlock.add(".text");
        programBlock.add("main :");
        programBlock.add("");
        programBlock.add("move $fp, $sp");
        programBlock.add("move $a0, $fp");
        pc+=4;
        CharStream input = new ANTLRFileStream(args[0]);
        molLexer lexer = new molLexer(input);
        CommonTokenStream ts = new CommonTokenStream(lexer);
        molParser parser = new molParser(ts);
        parser.program();
        boolean hasProgram=false, hasMain=false;
        for(Module mod : moduleList){
            if(mod.name.equals("program")) {
                hasProgram = true;
                for(Method meth : mod.methods)
                    if(meth.name.equals("main")){
                        hasMain = true;
                        break;
                    }
                break;
            }
        }
        if(!hasProgram)
            System.out.println("1: lacks program module(warning)");
        if(!hasMain)
            System.out.println("1: program module lacks main method(warning)");
        Module.checkCyclicGraph(moduleList);
        for(Module mod : moduleList){
            if(mod.hasBody == false) {
                System.out.println("5: module "+mod.name+" has no body(error)");
                hasFirstPassError = true;
            }
            Module.addAllIncludes(mod);
            mod.checkIncludedMethods();
            for(Module inc : mod.includes)
                mod.blk.AddBlockContent(inc.blk);
        }
        if(hasFirstPassError == true) {
            System.out.println("Fatal errors for pass one");
            System.exit(0);
        }

        for(Module mod : moduleList){
            mod.correctify();
        }

        CharStream input2 = new ANTLRFileStream(args[0]);
        pass2Lexer lexer2 = new pass2Lexer(input2);
        CommonTokenStream ts2 = new CommonTokenStream(lexer2);
        pass2Parser parser2 = new pass2Parser(ts2);
        parser2.program();
        if(hasSecondPassError == true) {
            System.out.println("Fatal errors for pass two");
            System.exit(0);
        }
        PrintWriter writer = new PrintWriter("mol.asm", "UTF-8");
        for(String st : programBlock)
            writer.println(st);
        writer.close();
        for(Module mod : moduleList){
            System.out.println("Module Dump:");
            System.out.println("  Name: "+mod.name);
            System.out.println("  Includes: "+mod.includes);
            System.out.println("  Fields: "+mod.blk.toStringEasy());
            System.out.println("  Methods: ");
            mod.includes.add(mod);
            for(Module inc : mod.includes){
                for(Method meth : inc.methods)
                    System.out.println("    "+meth);
            }
            mod.includes.remove(mod);
        }
    }
}

import java.util.*;

public class Block {
    int baseAddr = 0;
    int currentRelativeAddr = 0;
    Block prev = null;
    Set<Variable> vars = new LinkedHashSet<Variable>();
    List<Block> blocks = new ArrayList<Block>();
    Block(int baseAddr){
        this.baseAddr = baseAddr;
    }
    int pass2Counter = 0;
    Block(){}
    Block addBlock(){
        Block retValue = new Block(currentRelativeAddr);
        retValue.prev = this;
        blocks.add(retValue);
        return retValue;
    }
    void AddBlockContent(Block incBlock){
        for(Variable var : incBlock.vars){
            if(vars.contains(var)){
                System.out.println("11: same variables " + var.name);
                Main.hasFirstPassError = true;
            }
            Variable varC = new Variable(var.name, var.type);
            varC.relAddr = currentRelativeAddr;
            currentRelativeAddr += varC.type.size;
            vars.add(varC);
        }
    }

    public String toStringEasy(){
        String result = new String();
        result = "base addr : "+baseAddr;
        for(Variable var : vars)
            result = result + "," + var.toStringExp();
        return result;
    }

    @Override
    public String toString() {
        String result = new String();
        result = "base addr : "+baseAddr;
        for(Variable var : vars)
            result = result + "," + var.toStringExp();
        for (Block blk : blocks)
            result = result + " +another block : " + blk.toString();
        return result;
    }

    Type findTypeOfId(String ID){
        for(Variable var : vars)
            if(var.name.equals(ID))
                return var.type;
        if(prev !=  null)
            return prev.findTypeOfId(ID);
        return null;
    }
    boolean AddVar(Variable newVar, String to){
        if(vars.contains(newVar)){
            System.out.println("8: duplicated argument "+newVar.name+" for method "+to+", ignored(warning)");
            Main.hasFirstPassError = true;
            return false;
        }
        newVar.relAddr = currentRelativeAddr;
        currentRelativeAddr += newVar.type.correctSize;
        vars.add(newVar);
        return true;
    }
    void AddVar(String name, Type t, int arrSize, boolean inModule, int lineNumber){
        for(Variable var : vars)
            if(var.name.equals(name)){
                if(inModule)
                    System.out.println("8: duplicated variable "+var.name+"@ "+lineNumber+", ignored(warning)");
                else
                    System.out.println("9: duplicated variable "+var.name+"@ "+lineNumber+", ignored(warning)");
                Main.hasFirstPassError = true;
                return;
            }
        Variable newVar = new Variable(name, t);
        newVar.relAddr = currentRelativeAddr;
        if(arrSize > 0) {
            newVar.PromoteToArray(arrSize);
            currentRelativeAddr += newVar.type.size*arrSize;
        }else
            currentRelativeAddr += newVar.type.size;
        vars.add(newVar);
    }

    void correctifyRelAddresses(int newBaseAddr, boolean isModule){
        baseAddr = newBaseAddr;
        currentRelativeAddr = 0;
        for(Variable var : vars){
            var.relAddr = currentRelativeAddr;
            currentRelativeAddr += var.type.getCorrectSize();
        }
        if(isModule)
            for(Block blk : blocks){
                blk.correctifyRelAddresses(0, false);
            }
        else
            for(Block blk : blocks){
                blk.correctifyRelAddresses(currentRelativeAddr+baseAddr, false);
            }
    }
}

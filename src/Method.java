import java.util.ArrayList;
import java.util.List;

public class Method {
    List<Variable> args = new ArrayList<Variable>();
    Type retType;
    String name;
    int methodNumber;
    Block blk;
    boolean isVoid;
    Method(String name, Type retType){
        this.name = name;
        this.retType = retType;
        isVoid = retType.IsVoid();
    }
    void AddArg(String name, Type type, boolean isArray){
        Variable newVar = new Variable(name, type);
        if(isArray)
            newVar.PromoteToArray(0);
        if(blk.AddVar(newVar, this.name))
            args.add(newVar);
    }

    int LocalsSize(){
        int sum = args.get(args.size()-1).relAddr+args.get(args.size()-1).type.correctSize;
        return blk.currentRelativeAddr-sum;
    }

    @Override
    public String toString() {
        String result = new String();
        result = name + " " + retType.toString() + "[";
        for(Variable var : args)
            result = result + var.name + ":" + var.type.toString()+",";
        result = result + "]";
        result = result + blk.toString();
        return result;
    }
}

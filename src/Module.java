import java.util.*;

public class Module {
    String name;
    boolean hasBody;
    boolean corrected = false;

    @Override
    public int hashCode() {
        return name.hashCode();
    }
    Set<Module> includes = new LinkedHashSet<Module>();
    List<Method> methods = new ArrayList<Method>();
    Block blk = new Block();
    public int correctify(){
        if(corrected == false)
            blk.correctifyRelAddresses(0,true);
        corrected = true;
        return blk.currentRelativeAddr;
    }
    Module(String name, boolean hasBody){
        this.name = name;
        this.hasBody = hasBody;
    }
    void addInclude(Module incMod){
        if(incMod.name.equals(name)){
            System.out.println("Warning: Module " + name + " is included in itself !");
            return;
        }
        if(includes.contains(incMod)){
            System.out.println("Warning: Multiple include " + incMod.name + " to " + name);
            return;
        }
        includes.add(incMod);
    }
    void checkSelfMethodDuplicate(Method newMethod){
        for(Method meth : methods)
            if(meth.name.equals(newMethod.name) && meth.args.size() == newMethod.args.size() && newMethod != meth){
                boolean same = true;
                for(int i = 0; i < meth.args.size() ; ++i){
                    Variable var = meth.args.get(i);
                    if(!var.type.equals(newMethod.args.get(i).type)) {
                        same = false;
                        break;
                    }
                }
                if(same) {
                    System.out.println("10: same methods " + newMethod.name+"(error)");
                    Main.hasFirstPassError = true;
                }
            }
    }
    Method getMethod(String name, List<Type> args){
        for(Method meth : methods)
            if(meth.name.equals(name) && meth.args.size() == args.size()){
                boolean same = true;
                for(int i = 0; i < meth.args.size() ; ++i){
                    Variable var = meth.args.get(i);
                    if(!var.type.equals(args.get(i))) {
                        same = false;
                        break;
                    }
                }
                if(same) {
                    return meth;
                }
            }
        return null;
    }
    void checkIncludedMethods(){
        for(Module m : includes){
            for(Method newMethod : m.methods){
                for(Method meth : methods)
                    if(meth.name.equals(newMethod.name) && meth.args.size() == newMethod.args.size()){
                        boolean same = true;
                        for(int i = 0; i < meth.args.size() ; ++i){
                            Variable var = meth.args.get(i);
                            if(!var.type.equals(newMethod.args.get(i).type)) {
                                same = false;
                                break;
                            }
                        } if(same) {
                            System.out.println("11: same methods " + meth.name);
                            Main.hasFirstPassError = true;
                        }
                    }
            }
        }
    }
    Method addMethod(String name, Type retType){
        Method retValue = new Method(name, retType);
        methods.add(retValue);
        return retValue;
    }
    static void addAllIncludes(Module m){
        for(Module mod : m.includes){
            Module.addAllIncludes(mod);
            m.includes.addAll(mod.includes);
        }
    }
    static boolean isCyclicUtil(Module v, Set<Module> visited, Set<Module> recStack){
        if(!visited.contains(v)){
            visited.add(v);
            recStack.add(v);
            for(Module i : v.includes){
                if(!visited.contains(i) && isCyclicUtil(i,visited,recStack))
                    return true;
                else if(recStack.contains(i))
                    return true;
            }
        }
        recStack.remove(v);
        return false;
    }
    static void checkCyclicGraph(List<Module> moduleList){
        Set<Module> visited = new HashSet<Module>();
        Set<Module> recStack = new HashSet<Module>();
        for(Module i : moduleList)
            if (isCyclicUtil(i, visited, recStack)){
                System.out.println("4: loop in includes for module " + i.name+"(error)");
                System.exit(4);
            }
    }
    Type methodReturnType(String name, List<Type> argsTypes){
        includes.add(this);
        for(Module inc : includes){
            for(Method m : inc.methods){
                if(m.name.equals(name) && m.args.size() == argsTypes.size()) {
                    boolean found = true;
                    for(int i=0;i<argsTypes.size() && found;++i)
                        if(!argsTypes.get(i).lessThan(m.args.get(i).type))
                            found = false;
                    if(found) {
                        includes.remove(this);
                        return m.retType;
                    }
                }
            }
        }
        includes.remove(this);
        return null;
    }

    @Override
    public String toString() {
        return name;
    }
}

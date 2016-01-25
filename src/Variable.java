public class Variable{
    Type type;
    String name;
    int relAddr;
    Variable(String name, Type type){
        this.name = name;
        this.type = type;
    }


    @Override
    public int hashCode() {
        return name.hashCode();
    }

    void PromoteToArray(int count){
        type.arrayOf = new Type(type);
        type.arrayLength = count;
        type.pointerTo = null;
        type.whichPrimitive = Type.PRIMITIVE.NONE;
        type.objFrom = null;
        type.pCount = 0;
        if(count == 0)
            type.size = 1;
        else
            type.size = count*type.arrayOf.size;
    }

    @Override
    public String toString(){
        return name;
    }


    public String toStringExp(){
        if(type.objFrom == null)
            return name+":"+type.toString()+"@"+relAddr+" for "+type.correctSize+" byte(s)";
        else
            return name+":"+type.toString()+"@"+relAddr+" for "+type.objFrom.blk.currentRelativeAddr+" byte(s)";
    }

    @Override
    public boolean equals(Object o) {
        Variable rhs = ((Variable) o);
        return name.equals(rhs.name);
    }
}

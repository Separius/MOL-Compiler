public class Type {
    int size = 1;
    int arrayLength = 0;
    int pCount = 0;
    int correctSize;
    Type arrayOf = null;
    Type pointerTo = null;
    Module objFrom = null;
    boolean virt = false;
    enum PRIMITIVE{
        NONE,
        VOID,
        BOOL,
        INT,
        FLOAT
    };
    PRIMITIVE whichPrimitive = PRIMITIVE.NONE;

    public int getCorrectSize(){
        correctSize = 0;
        if(pointerTo != null)
            correctSize = 4;
        else if(objFrom != null)
            correctSize = objFrom.correctify();
        else if(arrayOf != null){
            if(arrayLength == 0)
                correctSize = 4;
            else
                correctSize = arrayOf.getCorrectSize() * arrayLength;
        } else if(whichPrimitive == PRIMITIVE.BOOL)
            correctSize = 4;
        else if(whichPrimitive == PRIMITIVE.INT)
            correctSize = 4;
        else if(whichPrimitive == PRIMITIVE.FLOAT)
            correctSize = 4;
        else if(whichPrimitive == PRIMITIVE.VOID)
            correctSize = 0;
        else
            System.exit(100);
        return correctSize;
    }

    boolean IsVoid(){
        return (whichPrimitive == PRIMITIVE.VOID);
    }

    Type(Type t){
        size = t.size;
        pCount = t.pCount;
        arrayLength = t.arrayLength;
        arrayOf = t.arrayOf;
        pointerTo = t.pointerTo;
        objFrom = t.objFrom;
        whichPrimitive = t.whichPrimitive;
        virt = t.virt;
        if(whichPrimitive == PRIMITIVE.BOOL)
            correctSize = 4;
        else if(whichPrimitive == PRIMITIVE.INT)
            correctSize = 4;
        else if(whichPrimitive == PRIMITIVE.FLOAT)
            correctSize = 4;
        else if(whichPrimitive == PRIMITIVE.VOID)
            correctSize = 0;
    }

    Type(Type t, boolean virtual){
        size = 1;
        pCount = 1+t.pCount;
        arrayLength = 0;
        arrayOf = null;
        pointerTo = t;
        objFrom = null;
        whichPrimitive = PRIMITIVE.NONE;
        virt = virtual;
    }

    Type(Module m, int pCount){
        this.pCount = pCount;
        if(pCount > 0)
            pointerTo = new Type(m, pCount-1);
        else
            objFrom = m;
    }

    Type(PRIMITIVE m, int pCount){
        this.pCount = pCount;
        if(pCount > 0)
            pointerTo = new Type(m, pCount-1);
        else {
            whichPrimitive = m;
            if(whichPrimitive == PRIMITIVE.BOOL)
                correctSize = 4;
            else if(whichPrimitive == PRIMITIVE.INT)
                correctSize = 4;
            else if(whichPrimitive == PRIMITIVE.FLOAT)
                correctSize = 4;
            else if(whichPrimitive == PRIMITIVE.VOID)
                correctSize = 0;
        }
    }

    String toStr(){
        String result;
        if(pCount != 0)
            result = "*" + pointerTo.toStr();
        else{
            if(arrayOf != null)
                result = arrayOf.toStr() + "[]";
            else if(objFrom != null)
                result = objFrom.name;
            else if(whichPrimitive != PRIMITIVE.NONE)
                if(whichPrimitive == PRIMITIVE.BOOL)
                    result = "bool";
                else if(whichPrimitive == PRIMITIVE.VOID)
                    result = "void";
                else //mind the float
                    result = "int";
            else
                result = null;
        }
        return result;
    }

    @Override
    public String toString() {
        String result;
        if(pCount != 0)
            result = "*" + pointerTo.toString();
        else{
            if(arrayOf != null)
                result = arrayOf.toString() + "[]";
            else if(objFrom != null)
                result = objFrom.name;
            else if(whichPrimitive != PRIMITIVE.NONE)
                if(whichPrimitive == PRIMITIVE.BOOL)
                    result = "bool";
                else if(whichPrimitive == PRIMITIVE.VOID)
                    result = "void";
                else if(whichPrimitive == PRIMITIVE.INT)
                    result = "int";
                else
                    result = "float";
            else
                result = null;
        }
        return result;
    }

    @Override
    public boolean equals(Object o) {
        Type lhs = ((Type) o);
        if(this.toStr().equals(lhs.toStr()))
            return true;
        else
            return false;
    }

    public boolean lessThan(Type rhs){
        if(whichPrimitive == PRIMITIVE.NONE)
            return this.toStr().equals(rhs.toStr());
        else if(whichPrimitive == rhs.whichPrimitive)
            return true;
        //we can use int instead of float
        else if(whichPrimitive == PRIMITIVE.INT && rhs.whichPrimitive == PRIMITIVE.FLOAT)
            return true;
        else
            return false;
    }
}

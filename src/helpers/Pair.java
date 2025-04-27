package helpers;

public class Pair<A, B> {
    private A key;
    private B val;


    public Pair(A key, B val) {
        this.key = key;
        this.val = val;
    }

    public void setKey(A key) {
        this.key = key;
    }

    public void setVal(B val) {
        this.val = val;
    }

    public A Key() {
        return key;
    }

    public B Val() {
        return val;
    }
}

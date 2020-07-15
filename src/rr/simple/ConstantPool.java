package rr.simple;

import java.util.HashMap;

public class ConstantPool extends HashMap<String, Integer> {
    /**
     *
     */
    private static final long serialVersionUID = 2596562765576121168L;

    public ConstantPool() {
        //
        // Add default values
        //
        put("Mark.Monitor", 1);
        put("Mark.Delay", 2);
        put("Mark.Thread", 3);
    }

    public int encode(String s) {
        Integer uid = get(s);

        if (uid == null) {
            uid = size() + 1;
            put(s, uid);
        }

        return uid;
    }
}
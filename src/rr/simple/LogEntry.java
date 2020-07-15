package rr.simple;

import java.util.Comparator;
import java.util.HashMap;

public class LogEntry {
    public long tsc = 0;
    public int objId = 0;
    public String opType = null;
    public String operand = null;
    public String location = null;

    public LogEntry() {

    }

    public void setInfo(long tsc, Object obj, String opType, String operand, String location) {
        this.tsc = tsc;
        this.objId = System.identityHashCode(obj);
        this.opType = opType;
        this.operand = operand;
        this.location = location;
    }

    public boolean isValid() {
        if (tsc == 0 || opType == null || operand == null) {
            return false;
        } else {
            return true;
        }
    }

    public LogEntry(long tsc, Object obj, String opType, String operand, String location) {
        this.tsc = tsc;
        this.objId = System.identityHashCode(obj);
        this.opType = opType;
        this.operand = operand;
        this.location = location;
    }

    public long getTsc() {
        return tsc;
    }

    public int getObjId() {
        return objId;
    }

    public boolean isEnter() {
	/*
	boolean b = false;
	try{
	    b = opType.equals("Enter");
	}catch(Exception e){
	    System.err.println(this.toString());
	    throw e;
	}
	return b;
	*/
        return (opType!= null) && opType.equals("Enter");
    }

    public static LogEntry call(Object obj, String opType, String methodName, String location) {
        return new LogEntry($.getTsc(), obj, opType, methodName, location);
    }

    public static LogEntry access(Object obj, String opType, String fieldName, String location) {
        return new LogEntry($.getTsc(), obj, opType, fieldName, location);
    }

    public static LogEntry monitor(Object obj, String opType, String location) {
        return new LogEntry($.getTsc(), obj, opType, "Mark.Monitor", location);
    }

    public static LogEntry delay() {
        return new LogEntry($.getTsc(), null, "Enter", "Mark.Delay", null);
    }

    public String toString() {
        return String.format("%d|%d|%s|%d|", tsc, objId, opType, operand);
    }

    public String compactToString(HashMap<String, Integer> constantPool) {
        Integer operandUid = constantPool.get(operand);

        if (operandUid == null) {
            operandUid = constantPool.size() + 1;
            constantPool.put(operand, operandUid);
        }

        return String.format("%d|%d|%s|%d|", tsc, objId, opType, operandUid);
        // if (objId != 0) {
            // return String.format("%d|%d|%s|%d|", tsc, objId, opType, operandUid );
        // } else {
            // return String.format("%d|null|%s|%d|", tsc, opType, operandUid);
        // }
    }

    public static Comparator<LogEntry> getCmpByTsc() {
        return new Comparator<LogEntry>() {
            @Override
            public int compare(LogEntry l1, LogEntry l2) {
                return Long.compare(l1.tsc, l2.tsc);
            }
        };
    }
}


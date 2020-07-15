package rr.simple;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import acme.util.option.CommandLine;
import rr.annotations.Abbrev;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.FieldAccessEvent;
import rr.event.MethodEvent;
import rr.event.ReleaseEvent;
import rr.event.VolatileAccessEvent;
import rr.event.AccessEvent.Kind;
import rr.state.ShadowVar;
import rr.tool.Tool;

@Abbrev("EL")
final public class EventLogger extends Tool {

    private AtomicBoolean needLogging = new AtomicBoolean(true);
    private AtomicBoolean stop = new AtomicBoolean(false);

    private ConcurrentHashMap<Long, ArrayList<LogEntry>> threadLogBuffer =
            new ConcurrentHashMap<Long, ArrayList<LogEntry>>();

    private String logDir = "litelogs";

    public EventLogger(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
        this.logDir = "litelogs";
    }

    private ArrayList<LogEntry> getThreadLogBuffer() {
        Long tid = $.getTid();
        return threadLogBuffer.computeIfAbsent(tid, k -> new ArrayList<>());
    }

    protected void addThreadLogEntry(LogEntry entry) {
        getThreadLogBuffer().add(entry);
    }

    private ConstantPool constantPool = new ConstantPool();
    ConcurrentLinkedDeque<VarAccess> objFirstAccess = new ConcurrentLinkedDeque<>();

    private void saveAllThreadLog() {
        $.mkdir(logDir);
        $.info("#Threads: %d \n", threadLogBuffer.size());

        //
        // Merge all "first access" of object to threads
        //
        HashMap<Long, ArrayList<LogEntry>> threadObjFirstLog = new HashMap<>();
        for (VarAccess access: objFirstAccess) {
            long tid = access.getLastTid();
            LogEntry logEntry = access.getLastLogEntry();
            //
            // For some variables, we don't record the first access for it,
            // so its logEntry is invalid.
            //
            if (logEntry.isValid()) {
                threadObjFirstLog.computeIfAbsent(tid, k -> new ArrayList<>()).add(logEntry);
            }
        }

        //
        // Sorting
        //
        threadObjFirstLog.forEach((tid, threadLog) -> Collections.sort(threadLog, LogEntry.getCmpByTsc()));

        for (Map.Entry<Long, ArrayList<LogEntry>> e: threadLogBuffer.entrySet()) {
            long tid = e.getKey();
            ArrayList<LogEntry> threadLog = e.getValue();
            saveThreadLog(tid, threadLog, threadObjFirstLog.get(tid));
        }

        for (Map.Entry<Long, ArrayList<LogEntry>> e: threadObjFirstLog.entrySet()) {
            long tid = e.getKey();
            if (!threadLogBuffer.containsKey(tid)){
                saveThreadLog(tid, null, threadObjFirstLog.get(tid));
            }
        }

        Dumper.dumpMap($.pathJoin(logDir, "map.cp"), constantPool);
    }

    private void saveLogEntry(PrintWriter liteLogWriter,
                              PrintWriter tlLogWriter,
                              LogEntry logEntry) {
        liteLogWriter.println(logEntry.compactToString(constantPool));
    }

    private void saveThreadLog(long tid, ArrayList<LogEntry> threadLog1,
                                         ArrayList<LogEntry> threadLog2) {
        int logSize1  = threadLog1 != null? threadLog1.size():0;
        int logSize2  = threadLog2 != null? threadLog2.size():0;

        if (logSize1 == 0 && logSize2 == 0) {
            $.warn("[ LOGGER ]", "Thread: `%d` log empty", tid);
            return;
        } else {
            $.info("[ Thread %d log size %d ]\n", tid, logSize1 + logSize2);
        }

        String liteLogName = String.format("%d.litelog", tid);
        String liteLogPath = $.pathJoin(logDir, liteLogName);

        String tlLogName = String.format("%d.tl-litelog", tid);
        String tlLogPath = $.pathJoin(logDir, tlLogName);

        try {
            PrintWriter liteLogWriter = new PrintWriter(liteLogPath, "UTF-8");
            PrintWriter tlLogWriter = new PrintWriter(tlLogPath, "UTF-8");

            int pos1 = 0;
            int pos2 = 0;
            while (pos1 < logSize1 && pos2 < logSize2) {
                LogEntry logEntry1 = threadLog1.get(pos1);
                LogEntry logEntry2 = threadLog2.get(pos2);
                if (logEntry1.getTsc() < logEntry2.getTsc()) {
                    saveLogEntry(liteLogWriter, tlLogWriter, logEntry1);
                    pos1 += 1;
                } else {
                    saveLogEntry(liteLogWriter, tlLogWriter, logEntry2);
                    pos2 += 1;
                }
            }

            while (pos1 < logSize1) {
                saveLogEntry(liteLogWriter, tlLogWriter, threadLog1.get(pos1));
                pos1 += 1;
            }
            while (pos2 < logSize2) {
                saveLogEntry(liteLogWriter, tlLogWriter, threadLog2.get(pos2));
                pos2 += 1;
            }

            liteLogWriter.close();
            tlLogWriter.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    @Override
	public void fini() {
        System.err.println("VM EXIT");
        stop.set(true);
        needLogging.set(false);
        saveAllThreadLog();
    }

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		return new VarAccess();
	}

    // ===========================================

    @Override
	public void enter(MethodEvent me) {
        Object target = me.getTarget();

        getThreadLogBuffer().add(LogEntry.call(target,
                                               "Enter",
                                               me.getInfo().getName(),
                                               null));
    }

    @Override
	public void exit(MethodEvent me) {
        // Object target = me.getTarget();
	/*
        if (!needLogging.get()) {
            return;
        }
        if (isAccessedByMultiThread($.getTid(), target)) {
            getThreadLogBuffer().add(LogEntry.call(target, "Exit", methodName, location));
        }
	*/
    }

    private void read(AccessEvent ae) {
        FieldAccessEvent fae = (FieldAccessEvent)ae;
        Object target = ae.getTarget();
        VarAccess access = (VarAccess)ae.getOriginalShadow();
        if (access.accessBy($.getTid()) != VarAccess.SINGLE) {
            getThreadLogBuffer().add(
                LogEntry.access(target,
                                "R",
                                fae.getInfo().getField().getName(),
                                null)
            );
        }
    }

    private void write(AccessEvent ae) {
        FieldAccessEvent fae = (FieldAccessEvent)ae;
        Object target = ae.getTarget();
        VarAccess access = (VarAccess)ae.getOriginalShadow();

        int ret =  access.accessBy($.getTid());
        if (ret != VarAccess.SINGLE) {
            if (ret == VarAccess.FIRST_MULTI) {
                objFirstAccess.add(access);
            }
            getThreadLogBuffer().add(
                LogEntry.access(target,
                                "W",
                                fae.getInfo().getField().getName(),
                                null));
        } else {
            int objId = System.identityHashCode(target);
            access.setAccess($.getTsc(),
                             $.getTid(),
                             target,
                             "W",
                             fae.getInfo().getField().getName(),
                             null);
        }
    }

    @Override
	public final void access(AccessEvent ae) {
        if (ae.getKind() == Kind.FIELD || ae.getKind() == Kind.VOLATILE) {
            if (ae.isWrite()) {
                write(ae);
            } else {
                read(ae);
            }
        } else if (ae.getKind() == Kind.ARRAY) {

        }
    }

	@Override
	public void volatileAccess(VolatileAccessEvent ae) {
        access(ae);
    }

    @Override
	public void acquire(AcquireEvent ae) {
        getThreadLogBuffer().add(
            LogEntry.monitor(
                ae.getLock().getLock(),
                "Enter",
                null
            )
        );
    }

    @Override
	public void release(ReleaseEvent re) {
        getThreadLogBuffer().add(
            LogEntry.monitor(
                re.getLock().getLock(),
                "Exit",
                null
            )
        );
    }
}

package rr.simple;

import rr.annotations.Abbrev;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.InterruptEvent;
import rr.event.InterruptedEvent;
import rr.event.JoinEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.WaitEvent;
import rr.meta.InvokeInfo;
import rr.meta.SourceLocation;
import rr.event.MethodEvent;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import acme.util.StringMatchResult;
import acme.util.Util;
import acme.util.XLog;
import acme.util.decorations.Decoration;
import acme.util.decorations.DefaultValue;
import acme.util.decorations.DecorationFactory.Type;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * Like LastTool, but keeps ShadowVar set to thread, so that the fast path in the Instrumenter is
 * triggered.
 *
 * Use this only for performance tests.
 */

final class MethodCallInfo {
	public Instant tsc;
	public int tid;
	public InvokeInfo info;
	// public SourceLocation loc;
	public MethodCallInfo lastDelayedCall;
	public String methodInfo;

	public MethodCallInfo() {
		tsc = Instant.now();
		tid = -1;
		// loc = null;
		info = null;
		lastDelayedCall = null;
		methodInfo = null;
	}

	public MethodCallInfo(MethodEvent me) {
		this(me, null);
	}

	public MethodCallInfo(MethodEvent me, MethodCallInfo last) {
		tsc = Instant.now();
		tid = me.getThread().getTid();
		info = me.getInvokeInfo();
		lastDelayedCall = last;
		methodInfo = me.toString();
	}

	public String toString() {
		if (info != null) {
			return methodInfo + "@" + info.toString();
		} else {
			// return "Unknown";
			return methodInfo;
		}
	}

	public int getTid() {
		return tid;
	}
}

@Abbrev("DI")
final public class DelayInjectionTool extends Tool {

	static final Decoration<ShadowThread, MethodCallInfo> threadLastCalls =
			ShadowThread.makeDecoration("Local Access Map", Type.SINGLE,
					new DefaultValue<ShadowThread, MethodCallInfo>() {
						public MethodCallInfo get(ShadowThread thread) {
							return new MethodCallInfo();
						}
					});

	static AtomicReference<MethodCallInfo> lastDelayedCall = new AtomicReference<MethodCallInfo>();
	static AtomicInteger numberOfThreads = new AtomicInteger(1);

	MethodCallInfo getThreadLastCall(ShadowThread thread) {
		return threadLastCalls.get(thread);
	}

	void putThreadCall(ShadowThread thread, MethodEvent me) {
		MethodCallInfo lastCall = lastDelayedCall.get();

		if (lastCall == null || lastCall.getTid() == thread.getTid()) {
			threadLastCalls.set(thread, new MethodCallInfo(me, null));
		} else {
			threadLastCalls.set(thread, new MethodCallInfo(me, lastCall));
		}
	}

	boolean needTrap() {
		if (numberOfThreads.get() < 2) {
			return false;
		}
		Random rand = new Random();

		if (rand.nextInt(100) < 5) {
			// Util.printf("Yeah~~~~~~~~~~~~~~~~");
			return true;
		} else {
			// Util.printf("NO~~~~~~~~~~~~~~~~");
			return false;
		}
	}

	void threadTrap(MethodEvent me) {
		lastDelayedCall.set(new MethodCallInfo(me));
		try {
			// Util.printf("Trap %s", me.toString());
			Thread.sleep(100); // 0.1 ms
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		XLog.logf("End trap %s %s\n", me.toString(), me.getInvokeInfo().getKey());
	}

	void mbrInfer(MethodEvent me) {
		MethodCallInfo lastCall = getThreadLastCall(me.getThread());

		if (lastCall == null) {
			return;
		}

		Instant currentTsc = Instant.now();
		Duration duration = Duration.between(lastCall.tsc, currentTsc);

		if (duration.getSeconds() < 1) {
			return;
		}

		if (lastCall.lastDelayedCall != null) {
			XLog.logf("May-HB: %s -> %s\n", lastCall.lastDelayedCall.toString(),
					lastCall.toString());
		}
	}

	void onMethodEvent(MethodEvent me) {
		// if (me.getInvokeInfo() != null) {
			// Util.printf(">>> %s", me.getInvokeInfo().toString());
		// }
		if (needTrap()) {
			threadTrap(me);
		} else {
			mbrInfer(me);
		}
		putThreadCall(me.getThread(), me);
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		return fae.getThread();
	}

	@Override
	public String toString() {
		return "Delay Injection";
	}

	public DelayInjectionTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
	}

	// @Override
	// public final void access(AccessEvent fae) {
	// 	// Util.printf("%s\n", fae.toString());

	// 	onEvent(fae);
	// }

	// Does not handle enter/exit, so that the instrumentor won't instrument method
	// invocations.
	public void enter(MethodEvent me) {
		onMethodEvent(me);
	}

	// public void exit(MethodEvent me) {}

	@Override
	public void postStart(StartEvent se) {
		Util.printf(">> start %s\n", se.toString());
		numberOfThreads.incrementAndGet();
	}

	@Override
	public void postJoin(JoinEvent je) {
		Util.printf("JOIN %s\n", je.toString());
		numberOfThreads.decrementAndGet();
	}


	/*
	 * public static boolean readFastPath(ShadowVar vs, ShadowThread ts) { Util.printf("FAST R\n");
	 *
	 * return true; }
	 *
	 * public static boolean writeFastPath(ShadowVar vs, ShadowThread ts) { return true; }
	 */
}

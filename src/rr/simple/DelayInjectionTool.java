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
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;

import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import acme.util.StringMatchResult;
import acme.util.Util;
import acme.util.decorations.Decoration;
import acme.util.decorations.DefaultValue;
import acme.util.decorations.DecorationFactory.Type;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * Like LastTool, but keeps ShadowVar set to thread, so that the fast path in
 * the Instrumenter is triggered.
 *
 * Use this only for performance tests.
 */

final class TrapInfo implements ShadowVar {
	public AccessEvent access;
	public Instant startTime;
	public boolean inTrap;
	public ReadWriteLock lock;

	public TrapInfo(AccessEvent e) {
		this.access = e;
		this.startTime = Instant.now();
		this.inTrap = false;
		lock = new ReentrantReadWriteLock();
	}

	public ShadowThread getThread() {
		return access.getThread();
	}

	public void setTrap(AccessEvent e) {
		this.access = e;
		this.startTime = Instant.now();
		this.inTrap = true;
	}

	public void clearTrap() {
		this.inTrap = false;
	}
}

class LocalAccessSet {
	private HashMap<Object, Instant> accessMap = new HashMap<Object, Instant>();

	public Instant getAccessTime(Object target) {
		return accessMap.get(target);
	}

	public void putAccessTime(Object target, Instant instant) {
		accessMap.put(target, instant);
	}
}

@Abbrev("DI")
final public class DelayInjectionTool extends Tool {

	static final Decoration<ShadowThread, LocalAccessSet> threadAccessMap = ShadowThread.makeDecoration("Local Access Map", Type.SINGLE,
			new DefaultValue<ShadowThread, LocalAccessSet>() {
		public LocalAccessSet get(ShadowThread thread) {
			return new LocalAccessSet();
		}
	});

	Instant getThreadLocalAccess(ShadowThread thread, Object target) {
		return threadAccessMap.get(thread).getAccessTime(target);
	}

	void putThreadLocalAccess(ShadowThread thread, Object target, Instant instant) {
		threadAccessMap.get(thread).putAccessTime(target, instant);
	}

	boolean checkTrap(AccessEvent currentAccess, TrapInfo trapInfo) {

		trapInfo.lock.readLock().lock();

		if (currentAccess.getTarget() != trapInfo.access.getTarget() || trapInfo.inTrap == false) {
			trapInfo.lock.readLock().unlock();
			return true;
		}

		if (currentAccess.getThread() == trapInfo.getThread()) {
			Util.printf("BUG HERE");
		}

		if (currentAccess.isWrite() || trapInfo.access.isWrite()) {
			Util.printf("Data Race");
		}

		trapInfo.lock.readLock().unlock();
		return true;
	}

	boolean needTrap() {
		Random rand = new Random();

        if (rand.nextInt(100) < 20) {
			return true;
		} else {
			return false;
		}
	}

	void trapOnVariable(AccessEvent currentAccess, TrapInfo trapInfo) {
		trapInfo.lock.writeLock().lock();
		trapInfo.setTrap(currentAccess);
		trapInfo.lock.writeLock().unlock();

		try {
			TimeUnit.SECONDS.sleep(1);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		trapInfo.lock.writeLock().lock();
		trapInfo.clearTrap();
		trapInfo.lock.writeLock().unlock();
	}

	void mbrInfer(AccessEvent currentAccess, TrapInfo trapInfo) {
		trapInfo.lock.readLock().lock();

		Instant lastAccessTime = getThreadLocalAccess(currentAccess.getThread(),
													  currentAccess.getTarget());
		if (lastAccessTime == null) {
			trapInfo.lock.readLock().unlock();
			return;
		}

		Instant currentTime = Instant.now();
		Duration duration = Duration.between(lastAccessTime, currentTime);

		if (duration.getSeconds() < 1) {
			trapInfo.lock.readLock().unlock();
			return;
		}

		if (trapInfo.inTrap) {
			trapInfo.lock.readLock().unlock();
			return;
		}

		if (lastAccessTime.compareTo(trapInfo.startTime) < 0) {
			Util.printf("May-HB\n");
		}

		trapInfo.lock.readLock().unlock();
	}

	void onEvent(AccessEvent e) {
		TrapInfo trapInfo = (TrapInfo) e.getShadow();
		if (!checkTrap(e, trapInfo)) {
			putThreadLocalAccess(e.getThread(), e.getTarget(), Instant.now());
			return;
		}
		if (needTrap()) {
			trapOnVariable(e, trapInfo);
		} else {
			mbrInfer(e, trapInfo);
		}
		putThreadLocalAccess(e.getThread(), e.getTarget(), Instant.now());
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		return new TrapInfo(fae);
	}

	@Override
	public String toString() {
		return "Empty";
	}

	public DelayInjectionTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
	}

	@Override
	public final void access(AccessEvent fae) {
		Util.printf("%s\n", fae.toString());

		onEvent(fae);
	}

	// Does not handle enter/exit, so that the instrumentor won't instrument method
	// invocations.
	// public void enter(MethodEvent me) {}
	// public void exit(MethodEvent me) {}

	@Override
	public void acquire(AcquireEvent ae) {
	}

	@Override
	public void release(ReleaseEvent re) {
	}

	@Override
	public boolean testAcquire(AcquireEvent ae) {
		return true;
	}

	@Override
	public boolean testRelease(ReleaseEvent re) {
		return true;
	}

	@Override
	public void preWait(WaitEvent we) {
	}

	@Override
	public void postWait(WaitEvent we) {
	}

	@Override
	public void preNotify(NotifyEvent ne) {
	}

	@Override
	public void postNotify(NotifyEvent ne) {
	}

	@Override
	public void preSleep(SleepEvent e) {
	}

	@Override
	public void postSleep(SleepEvent e) {
	}

	@Override
	public void preJoin(JoinEvent je) {
	}

	@Override
	public void postJoin(JoinEvent je) {
	}

	@Override
	public void preStart(StartEvent se) {
		Util.printf("%s\n", ">> start");

	}

	@Override
	public void postStart(StartEvent se) {
	}

	@Override
	public void interrupted(InterruptedEvent e) {
	}

	@Override
	public void preInterrupt(InterruptEvent me) {
	}

	/*
	public static boolean readFastPath(ShadowVar vs, ShadowThread ts) {
		Util.printf("FAST R\n");

		return true;
	}

	public static boolean writeFastPath(ShadowVar vs, ShadowThread ts) {
		return true;
	}
	*/
}

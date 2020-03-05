/******************************************************************************
 *
 * Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz) and Stephen Freund
 * (Williams College)
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 *
 * Neither the names of the University of California, Santa Cruz and Williams College nor the names
 * of its contributors may be used to endorse or promote products derived from this software without
 * specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 ******************************************************************************/

package tools.hb;

import java.io.Serializable;
import java.util.concurrent.atomic.AtomicInteger;
import rr.annotations.Abbrev;
import rr.barrier.BarrierEvent;
import rr.barrier.BarrierListener;
import rr.barrier.BarrierMonitor;
import rr.error.ErrorMessage;
import rr.error.ErrorMessages;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.InterruptedEvent;
import rr.event.JoinEvent;
import rr.event.NewThreadEvent;
import rr.event.ReleaseEvent;
import rr.event.StartEvent;
import rr.event.VolatileAccessEvent;
import rr.event.WaitEvent;
import rr.meta.ArrayAccessInfo;
import rr.meta.FieldInfo;
import rr.state.ShadowLock;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import acme.util.decorations.Decoration;
import acme.util.decorations.DecorationFactory;
import acme.util.decorations.DefaultValue;
import acme.util.io.XMLWriter;
import acme.util.option.CommandLine;

/**
 * A simple VC-based HappensBefore Race Detector.
 *
 * This does not handle many special cases related to static initializers, etc.
 * and may report spurious warnings as a result.  The FastTrack implementations
 * do handles those items.
 */

@Abbrev("HBT")
public final class HBTrackTool extends Tool implements BarrierListener<HBBarrierState> {

	/* Reporters for field/array errors */
	public final ErrorMessage<FieldInfo> errors = ErrorMessages.makeFieldErrorMessage("HappensBefore");
	public final ErrorMessage<ArrayAccessInfo> arrayErrors = ErrorMessages.makeArrayErrorMessage("HappensBefore");

	public HBTrackTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);

		/*
		 * Create a barrier monitor that will notify this tool when barrier ops happen.
		 * The get(k) method returns the initial state to associate with each barrier
		 * when the barrier is created.
		 */
		new BarrierMonitor<HBBarrierState>(this, new DefaultValue<Object,HBBarrierState>() {
			public HBBarrierState get(Object k) {
				return new HBBarrierState(ShadowLock.get(k));
			}
		});

	}

	private AtomicInteger mustHB = new AtomicInteger(0);
	private AtomicInteger threadHB = new AtomicInteger(0);
	private AtomicInteger barrierHB = new AtomicInteger(0);
	private AtomicInteger signalHB = new AtomicInteger(0);
	private AtomicInteger mayHB = new AtomicInteger(0);
	private AtomicInteger volatileHB = new AtomicInteger(0);

	class Accessor implements Serializable, ShadowVar  {
		/**
		*
		*/
		private static final long serialVersionUID = 1L;
		public int tid;

		public Accessor() {
			tid = -1;
		}

		public void setAccessor(ShadowThread td) {
			tid = td.getTid();
		}

		public boolean isSame(ShadowThread td) {
			return tid == td.getTid();
		}
	}

	/*
	 * Attach an Accessor to each object used as a lock.
	 */
	Decoration<ShadowLock,Accessor> shadowLock = ShadowLock.decoratorFactory.make(
		"HBT:lock",
		DecorationFactory.Type.MULTIPLE,
		new DefaultValue<ShadowLock,Accessor>() {
			public Accessor get(ShadowLock ld) {
				return new Accessor();
			}
		});

	private Accessor get(ShadowLock td) {
		return shadowLock.get(td);
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		return new Accessor();
	}

	// ===============================================================

	@Override
	public void create(NewThreadEvent e) {
		threadHB.incrementAndGet();
		mustHB.incrementAndGet();
		super.create(e);
	}

	@Override
	public void acquire(AcquireEvent ae) {
		final ShadowThread currentThread = ae.getThread();
		final ShadowLock shadowLock = ae.getLock();

		synchronized(shadowLock) {
			Accessor acc = get(shadowLock);
			if (!acc.isSame(currentThread)) {
				mayHB.incrementAndGet();
			}
			acc.setAccessor(currentThread);
		}
		super.acquire(ae);
	}

	@Override
	public void volatileAccess(VolatileAccessEvent fae) {
		ShadowVar g = fae.getOriginalShadow();
		final ShadowThread currentThread = fae.getThread();

		if (g instanceof Accessor) {
			final ShadowVar orig = fae.getOriginalShadow();
			final ShadowThread td = fae.getThread();

			Accessor acc = (Accessor)g;
			if (!acc.isSame(currentThread)) {
				volatileHB.incrementAndGet();
			}
			acc.setAccessor(currentThread);
			// if (fae.isWrite()) {
			// } else {
			// }
		}
		super.volatileAccess(fae);

	}

	@Override
	public void preStart(final StartEvent se) {
		threadHB.incrementAndGet();
		mustHB.incrementAndGet();

		super.preStart(se);
	}

	@Override
	public void postWait(WaitEvent we) {
		signalHB.incrementAndGet();
		mustHB.incrementAndGet();
		super.postWait(we);
	}

	@Override
	public void postJoin(JoinEvent je) {
		threadHB.incrementAndGet();
		mustHB.incrementAndGet();
		super.postJoin(je);
	}

	public void postDoBarrier(BarrierEvent<HBBarrierState> be) {
		barrierHB.incrementAndGet();
		mustHB.incrementAndGet();
	}

	@Override
	public ShadowVar cloneState(ShadowVar shadowVar) {
		return null;
	}

	@Override
	public synchronized void interrupted(InterruptedEvent e) {
		mustHB.incrementAndGet();
		super.interrupted(e);
	}

	@Override
	public void preDoBarrier(BarrierEvent<HBBarrierState> be) {
		// TODO Auto-generated method stub

	}

	@Override
	public void printXML(XMLWriter xml) {
		xml.print("#MustHB", mustHB.get());
		xml.print("#MayHB", mayHB.get());
		xml.print("#VolatileHB", volatileHB.get());
	}

	@Override
	public void fini() {
		System.err.println("======= Happens-before Track =======");
		System.err.format("#ThreadHB: %d\n", threadHB.get());
		System.err.format("#SignalHB: %d\n", signalHB.get());
		System.err.format("#BarrierHB: %d\n", barrierHB.get());
		System.err.format("#MustHB: %d\n", mustHB.get());
		System.err.format("#MayHB: %d\n", mayHB.get());
		System.err.format("#VolatileHB: %d\n", volatileHB.get());
	}
}

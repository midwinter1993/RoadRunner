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

package rr.simple;

import rr.annotations.Abbrev;
import rr.event.AccessEvent;
import rr.event.AcquireEvent;
import rr.event.InterruptEvent;
import rr.event.InterruptedEvent;
import rr.event.JoinEvent;
import rr.event.MethodEvent;
import rr.event.NotifyEvent;
import rr.event.ReleaseEvent;
import rr.event.SleepEvent;
import rr.event.StartEvent;
import rr.event.WaitEvent;
import rr.state.ShadowThread;
import rr.state.ShadowVar;
import rr.tool.Tool;
import acme.util.StringMatchResult;
import acme.util.Util;
import acme.util.XLog;
import acme.util.option.CommandLine;
import acme.util.option.CommandLineOption;

/**
 * Like LastTool, but keeps ShadowVar set to thread, so that
 * the fast path in the Instrumenter is triggered.
 *
 * Use this only for performance tests.
 */

@Abbrev("ST")
final public class SyncTrackTool extends Tool {

	@Override
	public String toString() {
		return "Sync Track";
	}

	public SyncTrackTool(String name, Tool next, CommandLine commandLine) {
		super(name, next, commandLine);
	}

	@Override
	public ShadowVar makeShadowVar(AccessEvent fae) {
		return fae.getThread();
	}

	@Override
	public final void access(AccessEvent fae) {
	}

	// Does not handle enter/exit, so that the instrumentor won't instrument method invocations.
	public void enter(MethodEvent me) {
		Util.printf("Enter %s\n", me.toString());
	}

	public void exit(MethodEvent me) {
		// Util.printf("XX METHOD %s\n", me.toString());
	}

	@Override
	public void acquire(AcquireEvent ae) {
		XLog.logf("Acq %s @ %s", ae.toString(), ae.getInfo().getKey());
	}
	@Override
	public void release(ReleaseEvent re) {
		XLog.logf("Rel %s @ %s", re.toString(), re.getInfo().getKey());
	}
	@Override
	public boolean testAcquire(AcquireEvent ae) { return true; }
	@Override
	public boolean testRelease(ReleaseEvent re) { return true; }

	@Override
	public void preWait(WaitEvent we) {
		XLog.logf("Wait %s @ %s", we.toString(), we.getInfo().getKey());
	}

	@Override
	public void postWait(WaitEvent we) {}

	@Override
	public void preNotify(NotifyEvent ne) {
		XLog.logf("Notify %s", ne.toString());
	}
	@Override
	public void postNotify(NotifyEvent ne) {}
	@Override
	public void preSleep(SleepEvent e) {}
	@Override
	public void postSleep(SleepEvent e) {}
	@Override
	public void preJoin(JoinEvent je) {}

	@Override
	public void postJoin(JoinEvent je) {
		XLog.logf("Join %s @ %s", je.toString(), je.getInfo().getKey());
	}
	@Override
	public void preStart(StartEvent se) {
		XLog.logf("Start %s @ %s", se.toString(), se.getInfo().getKey());
	}
	@Override
	public void postStart(StartEvent se) {}

	@Override
	public void interrupted(InterruptedEvent e) { }

	@Override
	public void preInterrupt(InterruptEvent me) { }


	public static boolean readFastPath(ShadowVar vs, ShadowThread ts) {
		return true;
	}

	public static boolean writeFastPath(ShadowVar vs, ShadowThread ts) {
		return true;
	}
}

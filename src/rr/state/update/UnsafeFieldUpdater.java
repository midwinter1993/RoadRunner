/******************************************************************************

Copyright (c) 2010, Cormac Flanagan (University of California, Santa Cruz)
                    and Stephen Freund (Williams College) 

All rights reserved.  

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are
met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the names of the University of California, Santa Cruz
      and Williams College nor the names of its contributors may be
      used to endorse or promote products derived from this software
      without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

******************************************************************************/

package rr.state.update;

import rr.state.ShadowVar;
import acme.util.Assert;
import acme.util.Util;
import acme.util.Yikes;

public abstract class UnsafeFieldUpdater extends AbstractFieldUpdater {

	public UnsafeFieldUpdater() {

	}

	/**
	 * Return the current guard state for o's field that is
	 * associated with this object. 
	 */
	@Override
	public final ShadowVar getState(Object o) {
		return get(o);
	}

	/**
	 * Put newGS in to the shadow location for the associated field of object o. 
	 * If the expected guard state is not found there, don't do the update.  Return
	 * whatever the guard state is currently set to.
	 */
	@Override
	public boolean putState(Object o, ShadowVar expectedGS, ShadowVar newGS) {
		try {
			if (expectedGS == newGS) return true;
			ShadowVar current = get(o);
			if (current != expectedGS) {
				Yikes.yikes("UnsafeFieldUpdater: Concurrent update on %s: %s.  current=%s  expected=%s  new=%s", getClass(), Util.objectToIdentityString(o), current, expectedGS, newGS);
				return false;
			} else {
				set(o, newGS);
				return true;
			}
		} catch (ClassCastException e) {
			Util.log(this.getClass() + " " + o.getClass());
			Assert.panic(e);
			return true;
		}
	}

	/**
	 * Get the actual ShadowVar.  Okay if it is stale.
	 */
	protected abstract ShadowVar get(Object o);

	/**
	 * Set the actual ShadowVar.  Assume that exclusive access
	 * has been guaranteed prior to this call.
	 */
	protected abstract void set(Object target, ShadowVar g);

}

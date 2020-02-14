#!/bin/sh

rrrun -quiet -noxml -noTidGC -callSites -xout=X.log -tool=DI test.Test
# rrrun -quiet -noxml -noTidGC -callSites -xout=X.log -tool=ST test.Test

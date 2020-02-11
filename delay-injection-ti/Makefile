CXX=g++
CXXSUFFIX=cpp
CXXFLAGS = -fPIC -g -ggdb -std=c++11 -I./include -I$(JAVA_HOME)/include -I$(JAVA_HOME)/include/linux -I$(JAVA_HOME)/include/darwin
LDFLAGS=

SOURCES=$(shell find . -name "*.$(CXXSUFFIX)")

OBJECTS = $(SOURCES:.$(CXXSUFFIX)=.o)

PROG=x.so

$(PROG): $(OBJECTS)
	$(CXX) -fPIC -shared -o $@ $^ $(LDFLAGS)


.$(CXXSUFFIX).o:
	$(CXX) -MD -MP $(CXXFLAGS) -o $@ -c $<

DEPENDS = $(SOURCES:.$(CXXSUFFIX)=.d)

-include $(DEPENDS)

%.d:
	@touch $@

.PHONY: clean run

# ================================================

clean:
	$(RM) $(PROG)
	$(RM) *.o
	$(RM) *.d

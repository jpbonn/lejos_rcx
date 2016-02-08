#
# leJOS main make file
#

SHELL=/bin/sh

ANT=ant
TEMP=/usr/tmp

# OSTYPE is not set by default on Mac OS X
ifndef OSTYPE
  OSTYPE = $(shell uname -s|awk '{print tolower($$0)}')
endif


#------------------------------------------------------------------------------
#                         The adjust-path macro
# 
# If COMSPEC is defined, Make is launched from Windows through
# Cygwin.  This adjust-path macro will call 'cygpath -u' on all
# paths to ensure they are readable by Make.
#
# If COMSPEC is not defined, Make is launched from *nix, and no adjustment
# is necessary
#------------------------------------------------------------------------------
SPACE := $(empty) $(empty)
ifndef COMSPEC
ifdef ComSpec
COMSPEC = $(ComSpec)
endif # ComSpec
endif # !COMSPEC

ifdef COMSPEC
	OSTYPE=cygwin
	adjust-path = $(subst $(SPACE),\$(SPACE),$(shell cygpath -u "$1"))
	adjust-path-mixed = $(subst $(SPACE),\$(SPACE),$(shell cygpath -m "$1"))
else
	adjust-path = $(subst $(SPACE),\$(SPACE),$1)
	adjust-path-mixed = $(subst $(SPACE),\$(SPACE),$1)
endif


# determine LEJOS_HOME and path separators
PWD=$(shell pwd)
ifneq (,$(findstring cygwin,$(OSTYPE)))
  OSTYPE=cygwin
  LEJOS_HOME=$(call adjust-path-mixed,$(shell pwd))
#  LEJOS_HOME=$(shell cygpath -m "$(PWD)")
  PATH_SEP=;
else
  LEJOS_HOME=$(PWD)
  PATH_SEP=:
endif

MFLAGS = OSTYPE=$(OSTYPE) LEJOS_HOME=$(LEJOS_HOME) BIN_TARGET=$(LEJOS_HOME)/bin

REGRESSION_SRC="test/regression"
JVM_SRC="src/javavm"
EMU_SRC="src/tools/emu-lejos"
PLAT_RCX_SRC="src/platform/rcx"
PLAT_UNIX_SRC="src/platform/unix"
PLAT_GBOY_SRC="src/platform/gameboy"
IRTRCX_LIB_SRC="src/comms/libirtrcx"
JIRTRCX_LIB_SRC="src/comms/libjirtrcx"

export JAVA

default: props emulator irtrcx_libs all_java scripts tinyvm_emul

all: props default lejos_bin

props:
	@echo "LEJOS_HOME set to $(LEJOS_HOME)"
	@echo "Compiling for $(OSTYPE)"

all_java:
	${ANT} irctrcx.libs jirctrcx.libs lejos.libs

scripts:
	chmod 775 $(LEJOS_HOME)/bin/lejosjc
	chmod 775 $(LEJOS_HOME)/bin/lejoslink
	chmod 775 $(LEJOS_HOME)/bin/lejosdl
	chmod 775 $(LEJOS_HOME)/bin/firmdl

emulator:
	@echo ""
	@echo "====> Making Emulator emu-lejos"
	@echo ""
	chmod 775 $(LEJOS_HOME)/release/cctest.sh
	cd $(EMU_SRC); $(MAKE) $(MFLAGS)

irtrcx_libs:
	@echo ""
	@echo "====> Making IR RCX communication libraries"
	@echo ""
	cd $(IRTRCX_LIB_SRC); $(MAKE) $(MFLAGS) clean; $(MAKE) $(MFLAGS)
	cd $(JIRTRCX_LIB_SRC); $(MAKE) $(MFLAGS) clean; $(MAKE) $(MFLAGS)

lejos_bin:
	@echo ""
	@echo "====> Making leJOS RCX binary (lejos.srec)"
	@echo ""
	cd $(PLAT_RCX_SRC); $(MAKE) $(MFLAGS)

tinyvm_emul:
	@echo ""
	@echo "====> Making leJOS Unix binaries (lejos, for emulation)"
	@echo ""
	cd $(PLAT_UNIX_SRC); $(MAKE) $(MFLAGS)

clean:
	${ANT} clean
	rm -f `find . -name '*.o'`
	rm -f `find . -name '*~'`
	rm -f `find . -name '*.core'`
	rm -f `find . -name '*.tvm'`
	rm -f `find . -name '*.bin'`
	rm -f `find . -name '*.sig'`
	rm -f `find . -name '*.bak'`
	rm -f `find . -name '*.stackdump'`
	rm -f `find . -name '*.backtrace'`

distclean: clean
	rm -f `find . -name '.#*'`
	rm -f `find . -name '*.lst'`
	rm -f `find . -name '*.log'`

distclean_src: distclean
	rm -f bin/lejos bin/emu-dump bin/emu-lejos bin/emu-lejosrun
	rm -f src/java/tools/js/tinyvm/SpecialClassConstants.java src/java/tools/js/tinyvm/SpecialSignatureConstants.java src/javavm/specialclasses.h src/javavm/specialsignatures.h
	rm -f $(PLAT_UNIX_SRC)/dump_config $(PLAT_UNIX_SRC)/platform_config.h
	rm -f $(EMU_SRC)/mkimg
	rm -f `find . -name '*.so' -o -name '*.dylib' -o -name '*.jnilib' -o -name '*.dll'`
	rm -f `find . -name '.DS_Store'`	# Mac OS X Finder droppings
	rm -f `find . -name '*.exe'`

realclean: distclean_src
	rm -f `find bin -name '*.srec'`

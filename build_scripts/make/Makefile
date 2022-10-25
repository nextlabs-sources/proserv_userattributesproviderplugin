# Universal master makefile for all modules 
# by Dominic Lam

# Use defaults when build environment is not explicitly specified.

# Get the current directory of this Makefile
CURRENTDIR := $(dir $(lastword $(MAKEFILE_LIST)))

ifeq ($(P4ROOT), )
	P4ROOT = c:/dev/p4
endif

ifneq ($(EXTERNALSDIR), )
$(shell echo)
#$(shell echo "Externals directory already set to," ${EXTERNALSDIR} ", not resetting.")
$(shell echo)
else
	export EXTERNALSDIR=$(arg2)
endif

# Enforcers directory
# TODO - Remove all references to the Enforcers directory
ifeq ($(NLENFORCERSDIR), )
	ifeq ($(enforcersdir), )
		export NLENFORCERSDIR = $(P4ROOT)/$(P4CLIENT)/Enforcers
	else
		export NLENFORCERSDIR=$(enforcersdir)
	endif
endif

# External directory
ifeq ($(NLEXTERNALDIR), )
	ifeq ($(EXTERNALSDIR), )
		export NLEXTERNALDIR = $(P4ROOT)/$(P4CLIENT)/main/external
	else
		export NLEXTERNALDIR = $(EXTERNALSDIR)
	endif
endif

# TARGETENVOS and TARGETENVARCH defines
TARGETENVOS = $(shell echo ${TARGETENV} | cut -d_ -f1)

TARGETENVARCH = $(shell echo ${TARGETENV} | cut -d_ -f2)

# Including the OS dependent environment 
OS_NAME_MAP = ";Darwin=Darwin;CYGWIN_NT-5.1=CYGWIN_NT-5.1;CYGWIN_NT-6.1=CYGWIN_NT-5.1;CYGWIN_NT-6.1-WOW64=CYGWIN_NT-5.1;CYGWIN_NT-5.2=CYGWIN_NT-5.1;CYGWIN_NT-5.2-WOW64=CYGWIN_NT-5.2-WOW64;Linux=Linux;"
UNAME = $(shell uname -s)
OS = $(shell echo ${OS_NAME_MAP} | sed 's/.*;${UNAME}=\([^;]*\);.*/\1/')

# Default binary target version information
include $(CURRENTDIR)/config/version

# Default to not build static subtarget (library)	
SUBTARGET_STATIC = no

# Always include Makefile.inc, and first to take precedence
include Makefile.inc

include $(CURRENTDIR)/env.${OS}

MYHOST=${shell hostname}
sinclude c:/env.${MYHOST}

ifeq ($(BUILDTYPE), release)
  BINDIR    = release_${BASE}
  LIBDIR    = release_${BASE}
else
  BUILDTYPE = debug
  BINDIR    = debug_${BASE}
  LIBDIR    = debug_${BASE}
endif

  INCDIR    = include

# Phony target to make sure they will get built
.PHONY: subdirs $(SUBDIRS)
.PHONY: clean
.PHONY: $(TARGET)

# Go to subdirectory and do recursive make 
# ifneq ($(strip $(SUBDIRS)),)
# subdirs: $(SUBDIRS)
#   for dir in $(SUBDIRS); do $(MAKE) -C $$dir; done
# endif

 
OBJS = $(addprefix $(BINDIR)/, $(addsuffix .o, $(basename $(notdir $(SRC)))))

RCOBJS   = $(addprefix $(BINDIR)/, $(addsuffix .res, $(basename $(notdir $(RCSRC)))))

INCP     = $(addprefix -I, $(INCPATH))

TESTOBJS = $(addprefix $(TESTDIR)/$(BINDIR)/, $(addsuffix .o, $(basename $(notdir $(TESTSRC)))))

IDL_OBJS = $(subst .idl,,$(MIDL_SRC))

# Computing the files to be cleaned
CLEANFILES   += $(OBJS) $(TESTOBJS)


ifneq ($(TARGET), )
  CLEANFILES += $(BINDIR)
  TARGETS    += $(TARGET)
endif

ifneq ($(SUBTARGET), )
  CLEANFILES += $(LIBDIR)
  TARGETS    += $(SUBTARGET)
endif

ifneq ($(TESTTARGET), )
  CLEANFILES += $(TESTDIR)/$(BINDIR)
  TARGETS    += $(TESTTARGET)
endif

ifneq ($(TESTSUBTARGET), )
  CLEANFILES += $(TESTDIR)/$(BINDIR)
  TARGETS    += $(TESTSUBTARGET)
endif

ifneq ($(CUSTOM), )
  CUSTOMTARGETS  += $(CUSTOM)
endif


# Rule for building MIDL files
#$(subst .idl,,$(MIDL_SRC)): $(MIDL_SRCX) 
$(IDL_OBJS): $(MIDL_SRC)
	$(MIDL) -cpp_cmd $(CC) -iid $@_i.c -proxy $@_p.c -h $(subst $(SRCDIR),$(INCDIR),$@).h -tlb $@.tlb $(MIDL_FLAGS) $(INCP) $<


# Rule for building C files
$(BINDIR)/%.o: $(SRCDIR)/%.c
	@if [ ! -d $(BINDIR) ] ; then               \
	  mkdir -p $(BINDIR) ;                      \
	fi
	$(CC) $(CFLAGS) $(INCP) $(DFLAGS) -c $< $(OBJOUTPUTFLAGS)$@



# Rule for building C++ files
$(BINDIR)/%.o: $(SRCDIR)/%.cpp
	@if [ ! -d $(BINDIR) ] ; then               \
	  mkdir -p $(BINDIR) ;                      \
	fi
	$(CXX) $(CXXFLAGS) $(INCP) $(DFLAGS) -c $< $(OBJOUTPUTFLAGS)$@

# Rule for building the resources
$(BINDIR)/%.res: $(SRCDIR)/%.rc
	$(RC) $(RCFLAGS) $(INCP) $(OBJOUTPUTFLAGS)$@ $< 

# Rule for building test files
$(TESTDIR)/$(BINDIR)%.o: $(TESTDIR)/%.c
	@if [ ! -d $(TESTDIR)/$(BINDIR) ] ; then               \
	  mkdir -p $(TESTDIR)/$(BINDIR) ;                      \
	fi
	$(CC) $(CFLAGS) $(INCP) $(DFLAGS) -c $< $(OBJOUTPUTFLAGS)$@

$(TESTDIR)/$(BINDIR)%.o: $(TESTDIR)/%.cpp
	@if [ ! -d $(TESTDIR)/$(BINDIR) ] ; then               \
	  mkdir -p $(TESTDIR)/$(BINDIR) ;                      \
	fi
	$(CXX) $(CXXFLAGS) $(INCP) $(DFLAGS) -c $< $(OBJOUTPUTFLAGS)$@

all: build_target_resource $(TARGETS) $(CUSTOMTARGETS) 
 
# Rule for building C# files
ifneq ($(CSTARGET), )
all:$(CSTARGET)
$(CSTARGET): $(SRC)
	@if [ ! -d $(BINDIR) ] ; then               \
	  mkdir -p $(BINDIR) ;                      \
	fi
	$(CSC) $(TARGET_TARGET) $(CSTARGET_DEBUG) $(REFERENCE) $(CSFLAGS) $(CSLIB) $(CSOUTPUT)$(BINDIR)/$(CSTARGET)$(TARGET_EXT) $(subst /,\\,$(SRC))
	@if [ $(SIGNTOOL). != . ] ; then                      \
		${SIGNTOOL} ${SIGNTOOLARGS} $(BINDIR)/$(CSTARGET)$(TARGET_EXT) ;  \
	fi 
endif

ifneq ($(CSSUBTARGET), )
all: $(CSSUBTARGET)

$(CSSUBTARGET): $(SRC)
	@if [ ! -d $(LIBDIR) ] ; then               \
	  mkdir -p $(LIBDIR) ;                      \
	fi
	$(CSC) $(SUBTARGET_TARGET) $(CSTARGET_DEBUG) $(REFERENCE) $(CSFLAGS) $(CSLIB) $(CSOUTPUT)$(LIBDIR)/$(CSSUBTARGET).$(SUBTARGET_EXT) $(subst /,\\,$(SRC))
	@if [ $(BUILDTYPE) == "release" ] ; then            \
	  if [ $(STRIP). != . ] ; then                      \
	    $(STRIP) $(SFLAGS) $(LIBDIR)/$(SUBTARGET_PREFIX)$@.$(SUBTARGET_EXT) ; \
	  fi                                                \
	fi
	@if [ $(SIGNTOOL). != . ] ; then                      \
		${SIGNTOOL} ${SIGNTOOLARGS} $(LIBDIR)/$(SUBTARGET_PREFIX)$@.$(SUBTARGET_EXT) ;  \
	fi 
endif

# Rule for building Target executables
$(TARGET): $(OBJS) $(RCOBJS)
	@if [ ! -d $(BINDIR) ] ; then               \
	  mkdir -p $(BINDIR) ;                      \
	fi
	$(LD) $(LDFLAGS) $^ $(BINOUTPUTFLAGS)$(BINDIR)/$@$(TARGET_EXT) $(LIBP) $(RUNP) $(LIBRARIES) $(CURRENTDIR)/config/target.res
	@if [ $(BUILDTYPE) == "release" ] ; then    \
	  if [ $(STRIP). != . ] ; then              \
	    $(STRIP) $(SFLAGS) $(BINDIR)/$@ ;              \
	  fi                                        \
	fi
	@if [ ${MANIFEST}. != . ] ; then                      \
		$(MSRC_ROOT)/bin/mt.exe -manifest ${MANIFEST} -outputresource:$(BINDIR)/$@$(TARGET_EXT)\;1;   \
	fi
	@if [ $(SIGNTOOL). != . ] ; then                      \
		${SIGNTOOL} ${SIGNTOOLARGS} $(BINDIR)/$@$(TARGET_EXT) ;  \
	fi 

# Construct resource for binary target
build_target_resource:
	@cp --force $(CURRENTDIR)/config/template.rc $(CURRENTDIR)/config/target.rc
	@sed --in-place -e 's/VERSION_MAJOR/$(VERSION_MAJOR)/g'             $(CURRENTDIR)/config/target.rc
	@sed --in-place -e 's/VERSION_MINOR/$(VERSION_MINOR)/g'             $(CURRENTDIR)/config/target.rc
	@sed --in-place -e 's/VERSION_SUBMINOR/$(VERSION_SUBMINOR)/g'       $(CURRENTDIR)/config/target.rc
	@sed --in-place -e 's/VERSION_SUBSUBMINOR/$(VERSION_SUBSUBMINOR)/g' $(CURRENTDIR)/config/target.rc
	@sed --in-place -e 's/VERSION_PRODUCT/$(VERSION_PRODUCT)/g'         $(CURRENTDIR)/config/target.rc
	@sed --in-place -e 's/VERSION_BUILD/$(VERSION_BUILD)/g'             $(CURRENTDIR)/config/target.rc
	@$(RC) $(CURRENTDIR)/config/target.rc

# Rule for building subtarget share library
$(SUBTARGET): $(OBJS) $(IDL_OBJS) $(RC_OBJS)
	-@if [ -f $(SUBTARGET).doxygen ] ; then \
	  doxygen $(SUBTARGET).doxygen ;       \
	fi
	@if [ ! -d $(LIBDIR) ] ; then               \
	  mkdir -p $(LIBDIR) ;                      \
	fi

	$(LD) $(LDFLAGS) $(SUBTARGETFLAGS) $^ $(BINOUTPUTFLAGS)$(LIBDIR)/$(SUBTARGET_PREFIX)$@.$(SUBTARGET_EXT) $(LIBP) $(LIBRARIES) $(CURRENTDIR)/config/target.res

	@if [ $(SUBTARGET_STATIC) == "yes" ] ; then            \
	  $(STATIC_LD) $(STATIC_LDFLAGS) $(OBJS) $(BINOUTPUTFLAGS)$(LIBDIR)/$(SUBTARGET)_static.lib ; \
	fi

	@if [ $(BUILDTYPE) == "release" ] ; then            \
	  if [ $(STRIP). != . ] ; then                      \
	    $(STRIP) $(SFLAGS) $(BINDIR)/$(SUBTARGET_PREFIX)$@.$(SUBTARGET_EXT) ; \
	  fi                                                \
	fi
	@if [ ${MANIFEST}. != . ] ; then                      \
		$(MSRC_ROOT)/bin/mt.exe -manifest ${MANIFEST} -outputresource:$(LIBDIR)/$(SUBTARGET_PREFIX)$@.$(SUBTARGET_EXT)\;2;   \
	fi
	@if [ $(SIGNTOOL). != . ] ; then                      \
		${SIGNTOOL} ${SIGNTOOLARGS} $(LIBDIR)/$(SUBTARGET_PREFIX)$@.$(SUBTARGET_EXT) ;  \
	fi 

$(TESTTARGET): $(TESTOBJS)
	@if [ ! -d $(TESTDIR)/$(BINDIR) ] ; then               \
	  mkdir -p $(TESTDIR)/$(BINDIR) ;                      \
	fi
	$(LD) $(LDFLAGS) $^ $(BINOUTPUTFLAGS)$(TESTDIR)/$(BINDIR)/$@$(TARGET_EXT) $(TESTLIBP) $(LIBP) $(TESTRUNP) $(TESTLIBRARIES)

$(TESTSUBTARGET): $(TESTOBJS)
	@if [ ! -d $(TESTDIR)/$(BINDIR) ] ; then               \
	  mkdir -p $(TESTDIR)/$(BINDIR) ;                      \
	fi
	$(LD) $(LDFLAGS)  $(TESTSUBTARGETFLAGS) $^ $(BINOUTPUTFLAGS)$(TESTDIR)/$(BINDIR)/$@.$(SUBTARGET_EXT) $(TESTLIBP) $(LIBP) $(TESTLIBRARIES)

# Rule for cleaning up
clean:
	rm -fr $(CLEANFILES)

APP_NAME   := JUranometria
MAIN_CLASS := juranometria.app.JUranometriaMain
MAIN_JAR   := $(APP_NAME).jar

SRC_DIR      := src
TEST_DIR     := test
LIB_DIR      := lib
TEST_LIB_DIR := lib/test
BUILD_DIR    := build
CLASSES_DIR  := $(BUILD_DIR)/classes
TEST_CLASSES := $(BUILD_DIR)/test-classes
APP_DIR      := $(BUILD_DIR)/app

SOURCES      := $(shell find $(SRC_DIR) -name "*.java")
TEST_SOURCES := $(shell find $(TEST_DIR) -name "*.java" 2>/dev/null)

# The single authoritative dependency pin set, shared with the
# bootstrap script.
include scripts/lib-versions.env

# Java toolchain. The build selects its own JDK rather than trusting
# whichever one leads the shell PATH, which is commonly an older
# release (issue #136). Precedence: an explicit JAVA_HOME, then a
# local Homebrew openjdk@21, then the PATH tools.
#   make JAVA_HOME=/path/to/jdk21 test
REQUIRED_JDK := 21

ifneq ($(origin JAVA_HOME), undefined)
  # An explicit JAVA_HOME is authoritative: its tools are used as
  # given, and a missing or unusable JDK there stops the build with a
  # readable message - never a silent fallback to whatever leads the
  # PATH (PR #138 review).
  JDK_BIN := $(JAVA_HOME)/bin/
else
  BREW_JDK := $(shell brew --prefix openjdk@$(REQUIRED_JDK) 2>/dev/null)
  ifneq ($(BREW_JDK),)
    BREW_JDK_HOME := $(BREW_JDK)/libexec/openjdk.jdk/Contents/Home
    ifneq ($(wildcard $(BREW_JDK_HOME)/bin/javac),)
      JDK_BIN := $(BREW_JDK_HOME)/bin/
    endif
  endif
endif

JAVAC := $(JDK_BIN)javac
JAVA  := $(JDK_BIN)java
JAR   := $(JDK_BIN)jar

REQUIRED_LIBS := 	$(LIB_DIR)/flatlaf-$(FLATLAF_VERSION).jar 	$(LIB_DIR)/flatlaf-extras-$(FLATLAF_VERSION).jar 	$(LIB_DIR)/jsvg-$(JSVG_VERSION).jar
JUNIT_JAR := $(TEST_LIB_DIR)/junit-platform-console-standalone-$(JUNIT_VERSION).jar

.PHONY: all help clean classes jar app run test chart-image constellation-study check-libs check-jdk

all: app

help:
	@echo "Usage: make <target>"
	@echo ""
	@echo "  all    Build the app (default)"
	@echo "  run    Build and launch the app"
	@echo "  test         Compile and run unit tests"
	@echo "  chart-image  Write the deterministic reference chart image"
	@echo "  import-allsky     Regenerate the bright-sky all-sky pack from pinned inputs"
	@echo "  regional-study    Render the Sprint 6 regional-zoom candidate charts"
	@echo "  constellation-study  Render the Sprint 7 constellation-geography study"
	@echo "  import-constellations  Regenerate the bundled constellation-geography pack"
	@echo "  import-star-identities  Regenerate the bundled star-identity pack"
	@echo "  pan-study         Measure the Sprint 8 grab-to-pan geometry and costs"
	@echo "  chart-options-study  Render the Sprint 12 chart-options candidates"
	@echo "  star-identity-study  Measure and render the Sprint 13 star-identity candidates"
	@echo "  zoom-study        Measure the Sprint 14 pointer-centred zoom geometry"
	@echo "  grid-study        Measure and render the Sprint 15 coordinate-grid candidates"
	@echo "  clean        Delete build output"

clean:
	rm -rf $(BUILD_DIR)

# Stop with a readable instruction instead of compiler errors when the
# downloaded dependencies are missing (issue #80).
check-libs:
	@missing=0; \
	for jar in $(REQUIRED_LIBS) $(JUNIT_JAR); do \
		if [ ! -f "$$jar" ]; then echo "Missing dependency: $$jar"; missing=1; fi; \
	done; \
	if [ "$$missing" != "0" ]; then \
		echo "Run scripts/download-libs.sh to fetch the pinned dependencies."; \
		exit 1; \
	fi

# Stop with a readable message naming the required and detected
# versions instead of confusing compiler errors when the resolved
# toolchain is older than the recorded minimum (issue #136).
check-jdk:
	@if ! command -v $(JAVAC) >/dev/null 2>&1; then \
		echo "No javac found at: $(JAVAC)"; \
		echo "Install JDK $(REQUIRED_JDK) or later, or set JAVA_HOME."; \
		exit 1; \
	fi; \
	found=$$($(JAVAC) -version 2>&1 | sed -nE 's/^javac ([0-9]+).*/\1/p' | head -n 1); \
	case "$$found" in \
		''|*[!0-9]*) found=0 ;; \
	esac; \
	if [ "$$found" -lt $(REQUIRED_JDK) ]; then \
		echo "JUranometria needs JDK $(REQUIRED_JDK) or later."; \
		echo "  Using: $(JAVAC)"; \
		echo "  Found: $$($(JAVAC) -version 2>&1)"; \
		echo "Install it (brew install openjdk@$(REQUIRED_JDK)) or set JAVA_HOME."; \
		exit 1; \
	fi

classes: check-jdk check-libs
	rm -rf $(CLASSES_DIR)
	mkdir -p $(CLASSES_DIR)
	$(JAVAC) \
		--release 21 \
		-cp "$(LIB_DIR)/*" \
		-d $(CLASSES_DIR) \
		$(SOURCES)
	@if [ -d $(SRC_DIR)/resources ]; then cp -r $(SRC_DIR)/resources $(CLASSES_DIR)/; fi
	cp VERSION $(CLASSES_DIR)/

# FlatLaf loads a native library for platform window integration; on
# JDK 24+ (JEP 472) that is a restricted call needing explicit
# permission. The manifest attribute grants it for java -jar; the run
# target passes the launcher flag for the -cp launch. See
# docs/development.md, "Native access".
jar: classes
	mkdir -p $(APP_DIR)
	printf 'Enable-Native-Access: ALL-UNNAMED\nClass-Path: lib/flatlaf-$(FLATLAF_VERSION).jar \n lib/flatlaf-extras-$(FLATLAF_VERSION).jar \n lib/jsvg-$(JSVG_VERSION).jar\n' > $(BUILD_DIR)/manifest-extra.mf
	$(JAR) \
		--create \
		--file $(APP_DIR)/$(MAIN_JAR) \
		--manifest $(BUILD_DIR)/manifest-extra.mf \
		--main-class $(MAIN_CLASS) \
		-C $(CLASSES_DIR) .

app: jar
	rm -rf $(APP_DIR)/lib
	mkdir -p $(APP_DIR)/lib
	cp $(LIB_DIR)/*.jar $(APP_DIR)/lib/

run: app
	$(JAVA) \
		--enable-native-access=ALL-UNNAMED \
		-cp "$(APP_DIR)/$(MAIN_JAR):$(APP_DIR)/lib/*" \
		$(MAIN_CLASS)

chart-image: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.app.ChartImageMain

import-allsky: classes
	$(JAVA) -cp "$(CLASSES_DIR)" juranometria.tool.AllSkyPackMain

import-constellations: classes
	$(JAVA) -cp "$(CLASSES_DIR)" juranometria.tool.ConstellationPackMain

import-star-identities: classes
	$(JAVA) -cp "$(CLASSES_DIR)" juranometria.tool.StarIdentityPackMain

regional-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.RegionalStudyMain

constellation-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ConstellationStudyMain

pan-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.PanStudyMain

chart-options-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ChartOptionsStudyMain

star-identity-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.StarIdentityStudyMain

zoom-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ZoomStudyMain

grid-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.GridStudyMain

test: check-libs classes
	rm -rf $(TEST_CLASSES)
	mkdir -p $(TEST_CLASSES)
	$(JAVAC) \
		--release 21 \
		-cp "$(LIB_DIR)/*:$(JUNIT_JAR):$(CLASSES_DIR)" \
		-d $(TEST_CLASSES) \
		$(TEST_SOURCES)
	$(JAVA) -cp "$(CLASSES_DIR):$(TEST_CLASSES):$(LIB_DIR)/*:$(JUNIT_JAR)" \
		org.junit.platform.console.ConsoleLauncher execute \
		--scan-class-path "$(TEST_CLASSES)"

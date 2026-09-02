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

.PHONY: all help clean classes jar app run test chart-image constellation-study identify-study furniture-study deep-sky-study deep-sky-occlusion-study check-libs check-jdk dist app-image

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
	@echo "  bayer-study       Measure and render the Sprint 17 Bayer-Flamsteed candidates"
	@echo "  dist              Build and verify the portable fallback ZIP"
	@echo "  app-image         Build and verify this platform's native application image"
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
		--date=2026-01-01T00:00:00Z \
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

bayer-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.BayerStudyMain

# Point-and-identify (docs/decisions/point-and-identify.md, issue
# #168): the measurements go to the committed study document, the
# pictures beside them.
# Chart furniture (docs/decisions/chart-furniture.md, issue #179).
furniture-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.FurnitureStudyMain \
		> docs/studies/chart-furniture/measurements.md

identify-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.IdentifyStudyMain \
		> docs/studies/point-and-identify/measurements.md
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.IdentifyMockupMain

# The deep-sky symbol vocabulary (docs/decisions/deep-sky-vocabulary.md,
# issue #184): the catalogue census, the symbol measurements, and the
# tabbed-dialog mock-ups. The mock-ups need a display, and say so
# rather than drawing a headless imitation of a window.
deep-sky-study: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.DeepSkyVocabularyStudyMain \
		> docs/studies/deep-sky-vocabulary/measurements.md

# Overlapping deep-sky symbols (docs/decisions/deep-sky-stacking.md,
# issue #201): which symbols hide which in the bundled pack, what
# storage order buried, and what the stacking rule leaves. Measured
# through the renderer's own published placements.
deep-sky-occlusion-study: classes
	mkdir -p docs/studies/deep-sky-occlusion
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.DeepSkyOcclusionStudyMain \
		> docs/studies/deep-sky-occlusion/measurements.md

# The 1.0 release archive (docs/decisions/one-point-zero-contract.md,
# issue #144): one deterministic unpack-and-run zip built from checked
# source - application JAR, the manifest-referenced lib/ dependencies,
# launch helpers, and every licence and notice. Timestamps are
# normalized and entries sorted, so identical inputs produce an
# identical archive; scripts/verify-dist.sh asserts the exact contents.
DIST_NAME := JUranometria-$(shell cat VERSION)
DIST_DIR := $(BUILD_DIR)/dist
DIST_STAGE := $(DIST_DIR)/$(DIST_NAME)
DIST_ZIP := $(DIST_DIR)/$(DIST_NAME).zip

# The native application image for THIS platform (one of the four
# primary 1.0 artifacts; scripts/build-app-image.sh, issue #150).
app-image: app
	scripts/build-app-image.sh

dist: app
	rm -rf $(DIST_DIR)
	mkdir -p $(DIST_STAGE)/lib $(DIST_STAGE)/licenses
	cp $(APP_DIR)/$(MAIN_JAR) $(DIST_STAGE)/
	cp $(LIB_DIR)/flatlaf-$(FLATLAF_VERSION).jar 	   $(LIB_DIR)/flatlaf-extras-$(FLATLAF_VERSION).jar 	   $(LIB_DIR)/jsvg-$(JSVG_VERSION).jar $(DIST_STAGE)/lib/
	cp packaging/juranometria packaging/juranometria.bat 	   packaging/README.txt $(DIST_STAGE)/
	cp LICENSE $(DIST_STAGE)/
	cp packaging/LICENSING.md $(DIST_STAGE)/
	cp $(CLASSES_DIR)/resources/catalog/bright-sky/NOTICE-tycho2.md \
	   $(CLASSES_DIR)/resources/catalog/bright-sky/NOTICE-openngc.md \
	   $(CLASSES_DIR)/resources/catalog/bright-sky/LICENSE-CC-BY-SA-4.0.txt \
	   $(CLASSES_DIR)/resources/geo/constellations/NOTICE-constellations.md \
	   $(CLASSES_DIR)/resources/catalog/star-identities/NOTICE-star-identities.md \
	   $(CLASSES_DIR)/resources/catalog/star-identities/LICENSE-BSD-3-Clause.txt \
	   $(DIST_STAGE)/licenses/
	cp $(CLASSES_DIR)/resources/icons/LICENSE \
	   $(DIST_STAGE)/licenses/LICENSE-Tabler-MIT.txt
	cp packaging/licenses/LICENSE-Apache-2.0.txt 	   packaging/licenses/LICENSE-JSVG-MIT.txt 	   packaging/licenses/NOTICE-runtime-libraries.md 	   $(DIST_STAGE)/licenses/
	chmod +x $(DIST_STAGE)/juranometria
	find $(DIST_STAGE) -exec touch -t 202601010000 {} +
	cd $(DIST_DIR) && find $(DIST_NAME) | LC_ALL=C sort 		| zip -X -q $(DIST_NAME).zip -@
	@echo "dist: $(DIST_ZIP)"
	scripts/verify-dist.sh $(DIST_ZIP)

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

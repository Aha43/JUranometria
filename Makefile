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

.PHONY: all help clean classes jar app run test globe-study globe-frame-study chart-image constellation-study identify-study furniture-study deep-sky-study deep-sky-occlusion-study application-mark-study on-this-page-study wider-field-study chart-sheet-study overview-study overview-ink-study figure-anchor-study label-study released-text toggle-shortcut-study control-explanation-study evidence-contracts-ci evidence-provenance icons check-libs check-jdk dist app-image

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
	@echo "  overview-study    Measure and render the Sprint 30 overview-projection candidates"
	@echo "  overview-ink-study  Measure overview ink on pages the production renderer drew"
	@echo "  figure-anchor-study Measure constellation figures against the magnitude limit"
	@echo "  label-study  Measure how labels share a page"
	@echo "  globe-study       Draw the Sprint 32 celestial-globe candidate hemispheres"
	@echo "  globe-frame-study Compare how much of the page the globe disc fills"
	@echo "  released-text     List the text every released page draws"
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
	$(JAVA) -Djava.awt.headless=true \
		-cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.FurnitureStudyMain \
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

# The application mark's coded visual gate (issue #200): four
# candidates from one geometry, exported at every size a desktop
# asks for and measured at the ones that decide it.
application-mark-study: classes
	mkdir -p docs/studies/application-mark
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ApplicationMarkStudyMain \
		> docs/studies/application-mark/measurements.md

# The application mark's committed containers (issue #202): every
# PNG, the ICO and the ICNS, all written from the one geometry the
# gate chose, in Java, so they are the same bytes on any machine.
icons: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ApplicationIconMain

# What is on a page (docs/decisions/on-this-page.md, issue #214):
# inventory sizes, why present objects cannot be seen, the ordering,
# and what asking costs.
# The celestial-globe gate's candidate pages (Sprint 32, issue #301).
# Writes hemispheres to build/globe-study for a human to look at; runs
# no tests and no evidence generators.
globe-study: classes
	@echo "  hemispheres"
	@$(JAVA) -Xmx1g -Djava.awt.headless=true \
		-cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.globe.GlobeStudyMain

# The globe frame candidates (#301): the same crowded hemisphere at
# four disc sizes, three furniture states and three containers.
globe-frame-study: classes
	@echo "  globe frames"
	@$(JAVA) -Xmx1g -Djava.awt.headless=true \
		-cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.globe.GlobeFrameStudyMain

chart-sheet-study: classes
	@echo "  chart sheets"
	@$(JAVA) -Djava.awt.headless=true \
		-cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ChartSheetStudyMain > /dev/null
	@echo "written to docs/studies/chart-sheet"

# The Sprint 30 gate (issue #296): the overview candidates drawn over
# real scenes, and what each one costs in shape, scale, domain, curve
# form and page density. Writes docs/studies/overview-projection.
overview-study: classes
	@echo "  overview projections"
	@$(JAVA) -Djava.awt.headless=true -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.overview.OverviewStudyMain > /dev/null
	@echo "written to docs/studies/overview-projection"

# The confirmation the overview gate could not give and required of
# #299: the same ink measure over pages the production renderer drew,
# with labels and its own stroke policy.
overview-ink-study: classes
	@echo "  overview ink, on rendered pages"
	@$(JAVA) -Djava.awt.headless=true -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.OverviewInkStudyMain > docs/studies/overview-ink/measurements.md
	@echo "written to docs/studies/overview-ink/measurements.md"

# What the overview's magnitude limit was doing to constellation
# figures, and what keeping their stars costs (#307).
label-study: classes
	@echo "  how labels share a page"
	@mkdir -p docs/studies/label-placement
	@$(JAVA) -Djava.awt.headless=true -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.labels.LabelStudyMain > docs/studies/label-placement/measurements.md
	@echo "written to docs/studies/label-placement/measurements.md"

figure-anchor-study: classes
	@echo "  constellation figures against the magnitude limit"
	@$(JAVA) -Djava.awt.headless=true -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.FigureAnchorStudyMain > docs/studies/figure-anchors/measurements.md
	@echo "written to docs/studies/figure-anchors/measurements.md"

control-explanation-study: classes
	@echo "  what every control says about itself"
	@mkdir -p docs/studies/control-explanations
	@$(JAVA) -Djava.awt.headless=true -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ControlExplanationStudyMain > docs/studies/control-explanations/measurements.md

toggle-shortcut-study: classes
	@echo "  a keyboard route to what the chart shows"
	@mkdir -p docs/studies/toggle-shortcuts
	@$(JAVA) -Djava.awt.headless=true -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ToggleShortcutStudyMain > docs/studies/toggle-shortcuts/measurements.md
	@echo "written to docs/studies/toggle-shortcuts/measurements.md"

released-text: classes
	@echo "  the text the released pages draw"
	@$(JAVA) -Djava.awt.headless=true -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.ReleasedTextMain > docs/studies/wider-field/released-text.txt
	@echo "written to docs/studies/wider-field/released-text.txt"

wider-field-study: classes
	@echo "  the released pages, hashed"
	@$(JAVA) -Djava.awt.headless=true \
		-cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.WiderFieldStudyMain > docs/studies/wider-field/released-pages.txt
	@echo "written to docs/studies/wider-field/released-pages.txt"

on-this-page-study: classes
	mkdir -p docs/studies/on-this-page
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.OnThisPageStudyMain \
		> docs/studies/on-this-page/measurements.md
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.OnThisPageMockupMain

# The public gallery (docs/decisions/gallery.md, issue #252): the
# module slides composed by the production component, and the pages
# derived from the one manifest.
gallery-pages: classes
	mkdir -p docs/studies/gallery
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.GalleryPageMain

gallery: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.GalleryMain

# The publishable site (issue #253): self-contained under
# build/gallery-site, assembled from the manifest and committed
# assets alone. Compiles only the two JDK-pure tool sources it
# needs, so a clean checkout builds it with no dependency download
# - the Pages workflow runs exactly this target.
gallery-site:
	mkdir -p build/site-classes
	$(JAVAC) --release 21 -d build/site-classes \
		src/juranometria/tool/MiniJson.java \
		src/juranometria/tool/GalleryMain.java
	rm -rf build/gallery-site
	$(JAVA) -cp build/site-classes juranometria.tool.GalleryMain \
		site build/gallery-site

.PHONY: gallery gallery-pages gallery-site

# The working-selection gate (docs/decisions/working-selection.md,
# issue #258): the census of today's two models, the composed
# evidence, and the surface mock-ups.
working-selection-study: classes
	mkdir -p docs/studies/working-selection
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.WorkingSelectionStudyMain \
		> docs/studies/working-selection/measurements.md
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.WorkingSelectionMockupMain

.PHONY: working-selection-study

# The black-sky palette (docs/decisions/black-sky.md, issue #246):
# the derivation executed and verified against the pinned palette,
# the representative pages rendered in both palettes, every pixel
# accounted.
black-sky-study: classes
	mkdir -p docs/studies/black-sky
	$(JAVA) -Djava.awt.headless=true \
		-cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.BlackSkyStudyMain \
		> docs/studies/black-sky/measurements.md

.PHONY: evidence-contracts test-evidence-study place-and-time-study black-sky-study ecliptic-study printable-chart-study
# The heap is stated rather than inherited from whatever a machine's
# ergonomics chose for it: the CI runner's default quarter-of-RAM is
# not the same number as a developer's.
#
# 1 GiB, and the number is measured rather than guessed (#323). The
# label study dominates this run, and after its page caches stopped
# being held for the whole corpus it completes at every limit down to
# 256m - which is exactly why "the smallest that passes" is the wrong
# rule. What the limits cost it, on one machine:
#
#     4g  345.7 s   1g  348.8 s   768m 347.9 s   512m 353.5 s
#                 384m  358.3 s   320m 365.1 s   256m 588.0 s
#
# 320m is the last affordable value and 256m is a cliff - four extra
# minutes to save 60 MB. 1 GiB sits three times above that practical
# floor, costs under 1%, and leaves room for a runner whose garbage
# collector has a different number of threads than this laptop's.
# Three consecutive runs at this limit: 1.03, 1.03, 1.04 GiB peak.
evidence-contracts: classes
	$(JAVA) -Xmx1g -cp "$(CLASSES_DIR):$(LIB_DIR)/*" -Djava.awt.headless=true juranometria.tool.EvidenceContractMain

# What CI runs: the same instrument, forbidden to compare a rendering
# here with pixels recorded on another machine. See
# docs/decisions/test-evidence.md.
evidence-contracts-ci: classes
	$(JAVA) -Xmx1g -cp "$(CLASSES_DIR):$(LIB_DIR)/*" -Djava.awt.headless=true juranometria.tool.EvidenceContractMain ci

# Run on the machine that promotes reference images, after a reviewed
# regeneration: records when, on what, and from which generator each
# promoted rendering was agreed, and the hash of the agreed bytes.
evidence-provenance: classes
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" -Djava.awt.headless=true juranometria.tool.EvidenceProvenanceMain

test-evidence-study: classes
	mkdir -p docs/studies/test-evidence
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.TestEvidenceStudyMain \
		> docs/studies/test-evidence/measurements.md

place-and-time-study: classes
	mkdir -p docs/studies/place-and-time
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.PlaceAndTimeStudyMain \
		> docs/studies/place-and-time/measurements.md
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.PlaceAndTimeInkStudyMain
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.PlaceAndTimeControlsMockupMain
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.PlaceAndTimeDialogStudyMain

# The printable chart and the modest wider field
# (docs/decisions/printable-chart.md, issue #283): what each degree
# past 36 costs, the candidate pages drawn by the production
# renderer. The gate's own SVG, PDF and PNG prototypes were retired
# in #286, when production gained writers of its own; the sheets now
# live in docs/studies/chart-sheet, made by make chart-sheet-study.
printable-chart-study: classes
	mkdir -p docs/studies/printable-chart
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.PrintableChartStudyMain \
		> docs/studies/printable-chart/measurements.md
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.WiderFieldPageMain

ecliptic-study: classes
	mkdir -p docs/studies/ecliptic
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.EclipticStudyMain \
		> docs/studies/ecliptic/measurements.md
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.EclipticInkStudyMain
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.EclipticCandidateStudyMain
	$(JAVA) -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.tool.EclipticControlStudyMain

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

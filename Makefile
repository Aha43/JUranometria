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

JUNIT_JAR := $(TEST_LIB_DIR)/junit-platform-console-standalone-1.10.2.jar

.PHONY: all help clean classes jar app run test chart-image import-catalogue

all: app

help:
	@echo "Usage: make <target>"
	@echo ""
	@echo "  all    Build the app (default)"
	@echo "  run    Build and launch the app"
	@echo "  test         Compile and run unit tests"
	@echo "  chart-image  Write the deterministic reference chart image"
	@echo "  import-catalogue  Regenerate src/resources/catalog/m31 from pinned inputs"
	@echo "  import-allsky     Regenerate the bright-sky all-sky pack from pinned inputs"
	@echo "  clean        Delete build output"

clean:
	rm -rf $(BUILD_DIR)

classes:
	rm -rf $(CLASSES_DIR)
	mkdir -p $(CLASSES_DIR)
	javac \
		--release 21 \
		-cp "$(LIB_DIR)/*" \
		-d $(CLASSES_DIR) \
		$(SOURCES)
	@if [ -d $(SRC_DIR)/resources ]; then cp -r $(SRC_DIR)/resources $(CLASSES_DIR)/; fi
	cp VERSION $(CLASSES_DIR)/

jar: classes
	mkdir -p $(APP_DIR)
	jar \
		--create \
		--file $(APP_DIR)/$(MAIN_JAR) \
		--main-class $(MAIN_CLASS) \
		-C $(CLASSES_DIR) .

app: jar
	rm -rf $(APP_DIR)/lib
	mkdir -p $(APP_DIR)/lib
	cp $(LIB_DIR)/*.jar $(APP_DIR)/lib/

run: app
	java \
		-cp "$(APP_DIR)/$(MAIN_JAR):$(APP_DIR)/lib/*" \
		$(MAIN_CLASS)

chart-image: classes
	java -cp "$(CLASSES_DIR):$(LIB_DIR)/*" juranometria.app.ChartImageMain

import-catalogue: classes
	java -cp "$(CLASSES_DIR)" juranometria.tool.CatalogueImportMain

import-allsky: classes
	java -cp "$(CLASSES_DIR)" juranometria.tool.AllSkyPackMain

test: classes
	rm -rf $(TEST_CLASSES)
	mkdir -p $(TEST_CLASSES)
	javac \
		--release 21 \
		-cp "$(LIB_DIR)/*:$(JUNIT_JAR):$(CLASSES_DIR)" \
		-d $(TEST_CLASSES) \
		$(TEST_SOURCES)
	java -cp "$(CLASSES_DIR):$(TEST_CLASSES):$(LIB_DIR)/*:$(JUNIT_JAR)" \
		org.junit.platform.console.ConsoleLauncher execute \
		--scan-class-path "$(TEST_CLASSES)"

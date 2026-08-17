.PHONY: build test clean install run help

JAR := target/java-ai-quality-gate-0.1.0-SNAPSHOT.jar
INSTALL_DIR := /home/rodrigo/dev/tools/java-ai-quality-gate
INSTALL_JAR := $(INSTALL_DIR)/java-ai-quality-gate.jar

build:
	mvn clean package -DskipTests

test:
	mvn test

clean:
	mvn clean

install: build
	mkdir -p $(INSTALL_DIR)
	cp $(JAR) $(INSTALL_JAR)
	@echo "Installed to $(INSTALL_JAR)"

run: build
	java -jar $(JAR) review --project $(PROJECT)

run-installed:
	java -jar $(INSTALL_JAR) review --project $(PROJECT)

help:
	@echo "Targets:"
	@echo "  build           - Compile and package fat JAR (skip tests)"
	@echo "  test            - Run unit tests"
	@echo "  clean           - Clean target directory"
	@echo "  install         - Copy JAR to $(INSTALL_DIR) (no version in name)"
	@echo "  run             - Run quality gate from target (usage: make run PROJECT=/path)"
	@echo "  run-installed   - Run quality gate from installed JAR (usage: make run-installed PROJECT=/path)"
	@echo "  help            - Show this help"
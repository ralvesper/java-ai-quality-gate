.PHONY: build test clean install run help

JAR := target/java-ai-quality-gate-0.1.0-SNAPSHOT.jar

build:
	mvn clean package -DskipTests

test:
	mvn test

clean:
	mvn clean

install: build
	mkdir -p ~/.local/bin
	cp $(JAR) ~/.local/bin/java-ai-quality-gate.jar
	@echo "Installed to ~/.local/bin/java-ai-quality-gate.jar"

run: build
	java -jar $(JAR) review --project $(PROJECT)

help:
	@echo "Targets:"
	@echo "  build    - Compile and package fat JAR (skip tests)"
	@echo "  test     - Run unit tests"
	@echo "  clean    - Clean target directory"
	@echo "  install  - Copy JAR to ~/.local/bin"
	@echo "  run      - Run quality gate (usage: make run PROJECT=/path/to/project)"
	@echo "  help     - Show this help"
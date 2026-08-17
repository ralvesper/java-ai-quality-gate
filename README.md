# Java AI Quality Gate

CLI para avaliar a qualidade de projetos Java com gates determinísticos e, em etapas posteriores, revisão assistida por IA.

## Objetivo

Executar um conjunto de verificações sobre um projeto Java e retornar um resultado simples para uso local e em CI/CD:

```text
PASS  -> exit code 0
BLOCK -> exit code 1
```

## Roadmap

### v0.1 — Deterministic Maven Gate

- [x] Estrutura inicial da CLI
- [x] Detectar projeto Maven
- [x] Executar `mvn verify`
- [x] Capturar stdout/stderr e exit code
- [x] Gerar resultado estruturado
- [x] Retornar exit code 0/1
- [x] Unit tests (5 passing)
- [x] Fat JAR via maven-shade-plugin
- [x] Makefile com targets build/test/clean/install/run

### v0.2 — Coverage & Architecture

- [ ] JaCoCo
- [ ] Threshold de cobertura
- [ ] ArchUnit
- [ ] Regras arquiteturais configuráveis

### v0.3 — Git Awareness

- [ ] Detectar branch base
- [ ] Gerar `git diff`
- [ ] Identificar arquivos e linhas alteradas

### v0.4 — AI Reviewer

- [ ] Revisar apenas o diff e contexto relevante
- [ ] Correctness
- [ ] Security
- [ ] Architecture
- [ ] Tests
- [ ] Scope adherence
- [ ] Overengineering
- [ ] Structured output com severity e confidence

### v0.5 — CI/CD

- [ ] GitHub Actions
- [ ] GitLab CI
- [ ] Comentários em PR/MR
- [ ] Quality Gate PASS/BLOCK

## Stack

- Java 21
- Maven
- Picocli
- Jackson
- JUnit 5

Spring Boot não é necessário neste primeiro momento: o projeto é uma CLI.

## Instalação

```bash
# Clonar e instalar (copia JAR + cria wrapper executável em ~/.local/bin)
git clone https://github.com/ralvesper/java-ai-quality-gate.git
cd java-ai-quality-gate
make install
```

Isso copia:
- Fat JAR: `~/.local/bin/java-ai-quality-gate.jar`
- Wrapper: `~/.local/bin/java-ai-quality-gate` (executável)

Adicione ao PATH se ainda não estiver:

```bash
echo 'export PATH="$PATH:$HOME/.local/bin"' >> ~/.bashrc
source ~/.bashrc
```

## Uso

### Via Makefile (projeto local)

```bash
# Build + run
make run PROJECT=/caminho/do/projeto

# Apenas rodar (assume JAR já buildado)
make run-installed PROJECT=/caminho/do/projeto
```

### Via wrapper instalado (qualquer diretório, **recomendado**)

```bash
java-ai-quality-gate review --project /caminho/do/projeto
```

### Via JAR instalado (qualquer diretório)

```bash
java -jar ~/.local/bin/java-ai-quality-gate.jar review --project /caminho/do/projeto
```

Saída esperada:

```text
Java AI Quality Gate
--------------------
Project: /caminho/do/projeto
Maven verify: PASS
Message: mvn verify executado com sucesso

QUALITY GATE: PASS
```

Ou em caso de falha:

```text
Java AI Quality Gate
--------------------
Project: /caminho/do/projeto
Maven verify: FAIL
Message: mvn verify falhou com código 1

QUALITY GATE: BLOCK
```

## Makefile Targets

```bash
make help           # Mostra ajuda
make build          # Compila e empacota fat JAR (skip tests)
make test           # Roda testes unitários
make clean          # Limpa target/
make install        # Instala JAR + wrapper em ~/.local/bin
make run PROJECT=.. # Build + executa quality gate
make run-installed  # Executa quality gate usando JAR instalado
```

## Princípio

O gate deve combinar validações determinísticas com IA. A IA nunca substitui build, testes, análise estática ou regras arquiteturais executáveis.

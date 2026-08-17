# Java AI Quality Gate

CLI para avaliar a qualidade de projetos Java com gates determinísticos e, em etapas posteriores, revisão assistida por IA.

## Objetivo

Executar um conjunto de verificações sobre um projeto Java e retornar um resultado simples para uso local e em CI/CD:

```text
PASS  -> exit code 0
BLOCK -> exit code 1
```

Falhas de qualidade (`FAIL`) e erros de infraestrutura/configuração (`ERROR`) bloqueiam o gate. `WARNING` e `SKIPPED` não bloqueiam.

## Estado atual

### v0.1 — Deterministic Maven Gate

- [x] Estrutura inicial da CLI
- [x] Detectar projeto Maven
- [x] Executar `mvn verify`
- [x] Capturar stdout/stderr e exit code
- [x] Gerar resultado estruturado
- [x] Retornar exit code 0/1
- [x] Testes unitários
- [x] Fat JAR via `maven-shade-plugin`
- [x] Makefile com build/test/clean/install/run

### v0.1.1 — Core stabilization

- [x] `--project` opcional, usando o diretório atual por padrão
- [x] `QualityGateEngine` para orquestrar checks
- [x] `ProcessRunner` reutilizável
- [x] Timeout para processos externos
- [x] Status `PASS`, `WARNING`, `FAIL`, `ERROR` e `SKIPPED`
- [x] Saída desacoplada em `TextReportWriter` e `JsonReportWriter`
- [x] Duração de cada check no resultado
- [x] Configuração por `.quality-gate.yml`
- [x] Argumentos Maven repetíveis com `--mvn-arg`
- [x] Maven Wrapper executado sem alterar permissões do projeto
- [x] Testes E2E com projeto fixture
- [x] GitHub Actions para build, testes e smoke test da CLI
- [x] `--version`

### v0.2 — Coverage & Architecture

- [ ] JaCoCo
- [ ] Threshold de cobertura
- [ ] Cobertura do código alterado
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

### v0.5 — CI/CD integrations

- [ ] GitHub PR comments
- [ ] GitLab CI
- [ ] GitLab MR comments
- [ ] Quality Gate PASS/BLOCK publicado no PR/MR

## Stack

- Java 21
- Maven
- Picocli
- Jackson
- Jackson YAML
- JUnit 5

Spring Boot não é necessário: o projeto é uma CLI.

## Instalação

```bash
git clone https://github.com/ralvesper/java-ai-quality-gate.git
cd java-ai-quality-gate
make install
```

Isso copia:

- Fat JAR: `~/.local/bin/java-ai-quality-gate.jar`
- Wrapper: `~/.local/bin/java-ai-quality-gate`

Adicione ao `PATH` se necessário:

```bash
echo 'export PATH="$PATH:$HOME/.local/bin"' >> ~/.bashrc
source ~/.bashrc
```

## Uso

### Dentro do projeto

`--project` é opcional. Se não for informado, o diretório atual é analisado:

```bash
cd ~/dev/customer-service
java-ai-quality-gate review
```

### Fora do projeto

```bash
java-ai-quality-gate review --project ~/dev/customer-service
```

### Saída JSON

```bash
java-ai-quality-gate review \
  --project ~/dev/customer-service \
  --format json
```

Os logs do Maven são enviados para `stderr` quando `--format json` é usado, mantendo `stdout` com JSON válido para consumo por pipelines.

Exemplo:

```json
{
  "project" : "/home/user/dev/customer-service",
  "status" : "PASS",
  "checks" : [ {
    "gate" : "maven-verify",
    "status" : "PASS",
    "message" : "mvn verify executado com sucesso",
    "durationMs" : 18420
  } ]
}
```

### Timeout

O padrão é 600 segundos:

```bash
java-ai-quality-gate review --timeout-seconds 900
```

### Argumentos adicionais para Maven

Prefira argumentos repetíveis para não depender de parsing de shell:

```bash
java-ai-quality-gate review \
  --mvn-arg=-B \
  --mvn-arg=-Puat \
  --mvn-arg=-DskipITs=false
```

O antigo `--mvn-args` continua disponível apenas por compatibilidade.

## Configuração do projeto

O gate procura por `.quality-gate.yml` na raiz do projeto analisado.

Exemplo:

```yaml
maven:
  timeoutSeconds: 600
  args:
    - -B
    - -Puat
```

Precedência:

```text
CLI > .quality-gate.yml > defaults
```

A ferramenta não altera o projeto analisado. Se existir `mvnw` sem permissão de execução em Unix, ele é executado via `sh ./mvnw` em vez de aplicar `chmod`.

## Status dos checks

```text
PASS     verificação executada com sucesso
WARNING  atenção necessária, mas não bloqueia
FAIL     problema de qualidade; bloqueia
ERROR    falha de ferramenta/configuração/infraestrutura; bloqueia
SKIPPED  check não aplicável ou não configurado
```

## Arquitetura atual

```text
ReviewCommand
     |
     v
QualityGateEngine
     |
     +-- QualityCheck
            |
            +-- MavenBuildCheck
                    |
                    v
               ProcessRunner

QualityGateReport
     |
     +-- TextReportWriter
     +-- JsonReportWriter
```

Novos gates devem implementar `QualityCheck` e serem registrados no `QualityGateEngine`.

## Makefile

```bash
make help
make build
make test
make clean
make install
make run PROJECT=/caminho/do/projeto
make run-installed PROJECT=/caminho/do/projeto
```

## CI do projeto

O próprio `java-ai-quality-gate` possui GitHub Actions em:

```text
.github/workflows/ci.yml
```

O workflow executa:

```bash
mvn -B verify
java -jar target/java-ai-quality-gate-0.1.1-SNAPSHOT.jar --version
```

## Próximo passo

A v0.2 adicionará os primeiros gates de qualidade além do build:

```text
JaCoCo
+
Coverage threshold
+
ArchUnit
+
Architecture rules
```

O objetivo é manter as validações determinísticas como base. A IA será adicionada depois como mais um `QualityCheck`, sem substituir build, testes, cobertura, análise estática ou regras arquiteturais executáveis.

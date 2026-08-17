# Java AI Quality Gate

CLI para avaliar a qualidade de projetos Java com gates determinísticos e, em etapas posteriores, revisão assistida por IA.

## Objetivo

Executar verificações sobre um projeto Java e retornar um resultado simples para uso local e em CI/CD:

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
- [x] Resultado estruturado
- [x] Exit code 0/1
- [x] Testes unitários
- [x] Fat JAR
- [x] Makefile

### v0.1.1 — Core stabilization

- [x] `--project` opcional, usando o diretório atual por padrão
- [x] `QualityGateEngine`
- [x] `ProcessRunner` com timeout
- [x] Status `PASS`, `WARNING`, `FAIL`, `ERROR`, `SKIPPED`
- [x] `TextReportWriter` e `JsonReportWriter`
- [x] `.quality-gate.yml`
- [x] Maven Wrapper sem alterar permissões do projeto
- [x] Testes E2E
- [x] GitHub Actions
- [x] `--version`

### Architecture Context

- [x] `architecture detect`
- [x] Detecção de documentação arquitetural explícita
- [x] Inferência não bloqueante por evidências estruturais
- [x] Identificação automática do nome do projeto
- [x] `architecture init`
- [x] Geração de `ARCHITECTURE.md` em modo `INFERRED`
- [x] Proteção contra sobrescrita sem `--force`

Os modelos e heurísticas arquiteturais ficam no repositório `ai-skills`. O quality gate não impõe uma arquitetura universal.

### Git Awareness

- [x] Detectar branch atual
- [x] Detectar branch/base de comparação
- [x] Gerar diff a partir da merge-base
- [x] Identificar arquivos alterados
- [x] Considerar alterações ainda não commitadas

### Work Item Context

- [x] Modelo neutro `WorkItem`
- [x] Provider configurável
- [x] Provider GitHub Issues via `gh`
- [x] Repositório de issues configurável
- [x] Detecção do ID pela branch
- [x] Patterns configuráveis
- [x] Extração de contexto, objetivo, critérios de aceite e cenários de teste
- [x] `work-item --format text|json`
- [x] Work item opcional ou obrigatório

### Próximas etapas

- [ ] JaCoCo
- [ ] Threshold de cobertura
- [ ] Cobertura do código alterado
- [ ] AI Reviewer
- [ ] Comparação requirement -> diff
- [ ] Scope adherence / overengineering
- [ ] Architecture review usando `ARCHITECTURE.md`
- [ ] GitHub PR comments
- [ ] GitLab MR comments

## Stack

- Java 21
- Maven
- Picocli
- Jackson
- Jackson YAML
- JUnit 5
- Git CLI
- GitHub CLI (`gh`) para GitHub Issues

Spring Boot não é necessário: o projeto é uma CLI.

## Instalação

```bash
git clone https://github.com/ralvesper/java-ai-quality-gate.git
cd java-ai-quality-gate
make install
```

Isso instala:

- `~/.local/bin/java-ai-quality-gate.jar`
- `~/.local/bin/java-ai-quality-gate`

## Uso básico

Dentro do projeto:

```bash
cd ~/dev/customer-service
java-ai-quality-gate review
```

Fora do projeto:

```bash
java-ai-quality-gate review --project ~/dev/customer-service
```

Saída JSON:

```bash
java-ai-quality-gate review --format json
```

## Architecture Context

Detectar o contexto arquitetural:

```bash
java-ai-quality-gate architecture detect
```

Gerar uma proposta inicial de contrato:

```bash
java-ai-quality-gate architecture init
```

O arquivo gerado é:

```text
ARCHITECTURE.md
```

Quando a arquitetura for inferida apenas do código, o documento é marcado como `INFERRED`. Inferência arquitetural não deve virar regra bloqueante automaticamente.

Para substituir um contrato já existente explicitamente:

```bash
java-ai-quality-gate architecture init --force
```

## Git Awareness

O comando `git` expõe o contexto da mudança atual:

```bash
java-ai-quality-gate git
```

Esse contexto será usado pelos próximos gates para analisar apenas as mudanças relevantes.

## Work Item Context

O quality gate pode carregar requisitos de um repositório GitHub Issues configurável.

Exemplo usando `ralvesper/pluxee-issues`:

```yaml
workItem:
  enabled: true
  required: false
  provider: github

  github:
    repository: ralvesper/pluxee-issues

  detection:
    branchPatterns:
      - "(FS-\\d+)"
      - "(PLUX-\\d+)"
      - "(ENH-\\d+)"
      - "(GSI-\\d+)"
```

O nome `pluxee-issues` não é hardcoded. Outro projeto pode usar:

```yaml
workItem:
  enabled: true
  provider: github
  github:
    repository: minhaempresa/dev-issues
```

### Resolução automática pela branch

Com uma branch como:

```text
feature/FS-687-corrigir-boletos
```

o gate detecta:

```text
FS-687
```

e procura uma issue cujo título contenha o token:

```text
[FS-687]
```

Também suporta títulos de sub-issues, por exemplo:

```text
[FS-687][GSI-0001] Ajustar regra de apresentação
```

### Consultar o work item

```bash
java-ai-quality-gate work-item
```

Ou informar o ID explicitamente:

```bash
java-ai-quality-gate work-item --id FS-687
```

JSON:

```bash
java-ai-quality-gate work-item --id FS-687 --format json
```

O contexto normalizado contém:

```text
id
title
context
objective
acceptanceCriteria
testScenarios
source
url
```

O parser reconhece seções Markdown como:

```text
## Contexto
## Objetivo
## Critérios de aceite
## Cenários de teste
```

### Autenticação GitHub

O provider GitHub usa o GitHub CLI:

```bash
gh auth status
```

Se necessário:

```bash
gh auth login
```

Isso permite consultar também repositórios privados de issues sem colocar tokens no `.quality-gate.yml`.

### Work item obrigatório

Por padrão, a ausência de work item não bloqueia:

```yaml
workItem:
  required: false
```

Projetos que exigem rastreabilidade podem usar:

```yaml
workItem:
  required: true
```

Nesse caso, `work-item` retorna exit code `1` quando nenhum item é localizado.

## Configuração completa de exemplo

```yaml
maven:
  timeoutSeconds: 600
  args:
    - -B
    - -Puat

workItem:
  enabled: true
  required: false
  provider: github

  github:
    repository: ralvesper/pluxee-issues

  detection:
    branchPatterns:
      - "(FS-\\d+)"
      - "(PLUX-\\d+)"
      - "(ENH-\\d+)"
      - "(GSI-\\d+)"
```

Precedência geral:

```text
CLI > .quality-gate.yml > defaults
```

## Status dos checks

```text
PASS     verificação executada com sucesso
WARNING  atenção necessária, mas não bloqueia
FAIL     problema de qualidade; bloqueia
ERROR    falha de ferramenta/configuração/infraestrutura; bloqueia
SKIPPED  check não aplicável ou não configurado
```

## Contexto que será entregue ao AI Reviewer

A direção do projeto é consolidar:

```text
Git Diff
+
Work Item / Requirements
+
ARCHITECTURE.md
+
Código relevante
        |
        v
AI Reviewer
```

O reviewer poderá avaliar não apenas se o código está correto, mas também se a mudança atende ao requisito, respeita a arquitetura do projeto e não introduz alterações fora do escopo.

## CI do projeto

O próprio `java-ai-quality-gate` possui GitHub Actions em:

```text
.github/workflows/ci.yml
```

O workflow executa build, testes e smoke test da CLI.

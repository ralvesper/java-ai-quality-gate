# Java AI Quality Gate

CLI para avaliar a qualidade de projetos Java com gates determinísticos e revisão assistida por IA.

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
- [x] Considerar arquivos novos ainda não rastreados (`untracked`)

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

### Unified Review Context

- [x] `ReviewContext`
- [x] Git diff + Work Item + Architecture Context
- [x] Conteúdo de `ARCHITECTURE.md`
- [x] `context --format text|json`

### AI Reviewer v1

- [x] Interface `AiReviewProvider`
- [x] Findings estruturados
- [x] Categorias de correctness, security, architecture, tests, scope, overengineering e maintainability
- [x] Severity e confidence explícitos
- [x] Provider `command` desacoplado de fornecedor/modelo
- [x] Entrada via `ReviewContext` JSON no `stdin`
- [x] Saída JSON estruturada no `stdout`
- [x] Política de bloqueio inicial
- [x] `ai-review --format text|json`

Política inicial:

```text
CRITICAL + HIGH confidence -> BLOCK
HIGH     + HIGH confidence -> BLOCK
outros findings            -> não bloqueiam
```

### GitLab Merge Request Context

- [x] Aceitar URL completa de Merge Request
- [x] Extrair host, projeto e IID do MR
- [x] Buscar metadata via `glab mr view`
- [x] Buscar patch remoto via `glab mr diff`
- [x] Não exigir checkout da branch do MR apenas para obter o diff
- [x] Comando `gitlab-mr`
- [x] Integração com a skill `java-ai-quality-review`

### Próximas etapas

- [ ] JaCoCo
- [ ] Threshold de cobertura
- [ ] Cobertura do código alterado
- [ ] Provider OpenAI/OpenRouter opcional
- [ ] Comparação requirement -> diff refinada
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
- GitLab CLI (`glab`) para Merge Requests

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

Consultar:

```bash
java-ai-quality-gate work-item
java-ai-quality-gate work-item --id FS-687
java-ai-quality-gate work-item --id FS-687 --format json
```

O provider GitHub usa o GitHub CLI:

```bash
gh auth status
```

## Review Context

Inspecionar o contexto completo que será entregue ao reviewer:

```bash
java-ai-quality-gate context
```

Com base e work item explícitos:

```bash
java-ai-quality-gate context \
  --base origin/develop \
  --work-item FS-687 \
  --format json
```

O `ReviewContext` agrega:

```text
Git Diff
+
Work Item / Requirements
+
Architecture Context
+
ARCHITECTURE.md
```

## GitLab Merge Request por URL

O quality gate pode carregar metadata e diff de um Merge Request remoto a partir do link completo do GitLab.

Pré-requisito:

```bash
glab auth status
```

Exemplo:

```bash
java-ai-quality-gate gitlab-mr \
  'https://git.pluxee.com.br/core-backoffice/pedefacil1/ebs/-/merge_requests/734'
```

O comando usa o projeto e o IID extraídos da própria URL e consulta o GitLab via `glab`. Não é necessário trocar a branch local apenas para obter o patch do MR.

O contexto retornado contém, entre outros dados:

```text
provider
title
description
sourceBranch
targetBranch
state
author
url
projectPath
patch
```

Para revisão assistida pelo Codex, a skill correspondente pode ser chamada com a mesma URL:

```text
$java-ai-quality-review https://git.pluxee.com.br/core-backoffice/pedefacil1/ebs/-/merge_requests/734
```

Nesse modo, o patch remoto do MR é a fonte de verdade sobre as alterações. Quando existir um clone local correspondente, a revisão pode complementar o contexto com `ARCHITECTURE.md`, Work Item e arquivos relacionados.

## AI Reviewer

O reviewer é opcional e fica desabilitado por padrão.

Configuração:

```yaml
aiReview:
  enabled: true
  provider: command
  command:
    - /caminho/para/meu-ai-reviewer
```

O comando configurado recebe o `ReviewContext` serializado em JSON pelo `stdin` e deve escrever apenas JSON válido no `stdout`.

Executar:

```bash
java-ai-quality-gate ai-review
```

Com contexto explícito:

```bash
java-ai-quality-gate ai-review \
  --base origin/develop \
  --work-item FS-687
```

Saída JSON:

```bash
java-ai-quality-gate ai-review --format json
```

### Contrato de saída do provider

Exemplo:

```json
{
  "provider": "my-reviewer",
  "summary": "Mudança possui um problema arquitetural bloqueante.",
  "findings": [
    {
      "category": "ARCHITECTURE",
      "severity": "HIGH",
      "confidence": "HIGH",
      "file": "src/main/java/com/acme/CustomerController.java",
      "line": 43,
      "message": "Controller acessa repository diretamente.",
      "rationale": "ARCHITECTURE.md exige acesso à persistência através da camada application."
    }
  ]
}
```

Categorias suportadas:

```text
CORRECTNESS
SECURITY
ARCHITECTURE
TESTS
SCOPE
OVERENGINEERING
MAINTAINABILITY
```

Se existir pelo menos um finding `HIGH` ou `CRITICAL` com `HIGH` confidence, `ai-review` retorna exit code `1`.

A ferramenta não exige um fornecedor específico. O comando externo pode encapsular OpenAI, OpenRouter, Codex, Claude ou outro modelo, desde que respeite o contrato JSON.

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

aiReview:
  enabled: true
  provider: command
  command:
    - /caminho/para/meu-ai-reviewer
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

## CI do projeto

O próprio `java-ai-quality-gate` possui GitHub Actions em:

```text
.github/workflows/ci.yml
```

O workflow executa build, testes e smoke test da CLI.

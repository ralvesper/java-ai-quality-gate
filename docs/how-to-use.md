# How to Use - Java AI Quality Gate

Guia prático com exemplos reais de uso cotidiano.

## Instalação

```bash
git clone https://github.com/ralvesper/java-ai-quality-gate.git
cd java-ai-quality-gate
make install
```

Isso instala o JAR e o wrapper em `~/.local/bin/`. Garanta que `~/.local/bin` esteja no `PATH`.

Pré-requisitos:

- Java 21+
- Git
- Maven (ou Maven Wrapper no projeto)
- `gh` CLI (se for usar work items do GitHub)
- `glab` CLI (se for usar MRs do GitLab)

---

## Cenário 1: "Terminei uma feature, vou dar push"

Você está em `fix/FS-677-correcao-pedido` e quer garantir que está tudo antes de empurrar.

```bash
cd ~/dev/core-backoffice/pedefacil2/order-pedefacil-service

java-ai-quality-gate review
```

Saída esperada:

```
Quality Gate Report
===================

Project: ~/dev/core-backoffice/pedefacil2/order-pedefacil-service

  Maven Build ......... PASS (23.4s)

Final Status: PASS
```

Se o build falhar:

```
  Maven Build ......... FAIL (12.1s)

Final Status: BLOCK (exit code 1)
```

**O que acontece aqui:** o tool roda `mvn verify` no projeto e retorna PASS ou BLOCK. Nada de mágica — é o mesmo build que a CI vai rodar, rodado localmente antes de você perder tempo esperando pipeline.

---

## Cenário 2: "Quero ver o que mudei antes de commitar"

Você fez alterações mas não sabe mais o escopo exato. O `git` mostra o diff de forma estruturada:

```bash
java-ai-quality-gate git
```

Saída:

```
Git Context
===========

Branch:     fix/FS-677-correcao-pedido
Base:       origin/master
Merge-base: a1b2c3d

Changed files:
  M  src/main/java/com/example/OrderService.java    +15 -3
  A  src/test/java/com/example/OrderServiceTest.java +42 -0
  M  pom.xml                                          +2 -0
```

Isso é útil para:
- Confirmar que o escopo da sua mudança está dentro do esperado
- Verificar se esqueceu algum arquivo
- Ter certeza de que a base está correta antes de gerar o contexto

---

## Cenário 3: "Preciso revisar um MR do GitLab sem sair do terminal"

Alguém mandou o link de um MR e você quer entender o que mudou:

```bash
java-ai-quality-gate gitlab-mr \
  'https://git.pluxee.com.br/core-backoffice/pedefacil1/ebs/-/merge_requests/734'
```

Isso busca a metadata e o diff remoto via `glab` sem precisar fazer checkout da branch. Útil para:

- Review rápido antes de dar approve
- Entender o que o autor mudou sem clones temporários
- Alimentar a revisão assistida por IA com o patch remoto

---

## Cenário 4: "Quero que a IA revise meu código"

### Configuração mínima

No `.quality-gate.yml` do projeto:

```yaml
aiReview:
  enabled: true
  provider: command
  command:
    - /home/rodrigo/.local/bin/ai-reviewer
```

O comando configurado deve:
- Ler `ReviewContext` (JSON) pelo `stdin`
- Escrever um JSON válido de resultado no `stdout`

### Execução

```bash
java-ai-quality-gate ai-review
```

Com contexto explícito:

```bash
java-ai-quality-gate ai-review \
  --base origin/develop \
  --work-item FS-687
```

Saída (texto):

```
AI Review
=========

Provider: codex-skill
Summary:  Implementação correta, mas existe uma violação arquitetural.

Findings:
  [HIGH/HIGH] ARCHITECTURE
    src/main/java/com/example/CustomerController.java:43
    Controller acessa repository diretamente.
    Rationale: ARCHITECTURE.md exige acesso via camada application.

Final Status: BLOCK
```

### Por que bloqueou?

A política de bloqueio é:

| Severidade | Confiança | Resulta em |
|------------|-----------|------------|
| CRITICAL | HIGH | BLOCK |
| HIGH | HIGH | BLOCK |
| Qualquer outra | Qualquer | não bloqueia |

Se você ver `BLOCK` e achar que o finding não é válido, o problema pode estar no prompt do seu `ai-reviewer` — ajuste-o, não o quality gate.

### Providers de IA

O `ai-reviewer` suporta três providers. Selecione via variável de ambiente `AI_REVIEW_PROVIDER`:

#### Claude CLI (default)

```bash
# Usa o Claude instalado localmente
java-ai-quality-gate ai-review

# Ou explicitamente
AI_REVIEW_PROVIDER=claude java-ai-quality-gate ai-review

# Modelo específico
AI_REVIEW_MODEL=opus java-ai-quality-gate ai-review
```

Pré-requisito: `claude` CLI autenticado.

#### OpenRouter (API, qualquer modelo)

```bash
# Default: anthropic/claude-sonnet-4
AI_REVIEW_PROVIDER=openrouter java-ai-quality-gate ai-review

# Modelo específico
AI_REVIEW_PROVIDER=openrouter AI_REVIEW_MODEL=openai/gpt-4o java-ai-quality-gate ai-review

# Modelo gratuito
AI_REVIEW_PROVIDER=openrouter AI_REVIEW_MODEL=meta-llama/llama-3.1-8b-instruct:free java-ai-quality-gate ai-review
```

Pré-requisito: variável `OPENROUTER_API_KEY` (ou usa a key configurada em `~/.agents/skills/hostinger/`).

#### Ollama (Hostinger VPS)

```bash
# Default: qwen2.5-coder no VPS
AI_REVIEW_PROVIDER=ollama java-ai-quality-gate ai-review

# Modelo específico
AI_REVIEW_PROVIDER=ollama AI_REVIEW_MODEL=llama3.2:1b java-ai-quality-gate ai-review

# Host customizado
AI_REVIEW_PROVIDER=ollama OLLAMA_HOST=localhost:11434 java-ai-quality-gate ai-review
```

Pré-requisito: Ollama rodando no VPS (`69.62.100.245:11434`). Modelos disponíveis: `qwen2.5-coder:1.5b`, `llama3.2:1b`.

#### Resumo rápido

| Provider | Custo | Velocidade | Qualidade |
|----------|-------|------------|-----------|
| `claude` | Grátis (local) | Rápida | Alta |
| `openrouter` | Pago por token | Média | Alta (varia por modelo) |
| `ollama` | Grátis (self-hosted) | Lenta (VPS) | Média |

---

## Cenário 5: "Quero que o work item seja carregado automaticamente"

Configure no `.quality-gate.yml`:

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
      - "(GSI-\\d+)"
```

Agora, se sua branch se chama `fix/FS-677-correcao-pedido`, o tool automaticamente extrai `FS-677` e busca a issue correspondente.

```bash
java-ai-quality-gate context
```

Saída incluirá o work item:

```
Review Context
==============

Git:
  Branch: fix/FS-677-correcao-pedido
  Base:   origin/master
  Files:  3 changed

Work Item:
  ID:      FS-677
  Title:   Corrigir cálculo de pedido com gift card
  Context: O cálculo de pedido não está considerando gift cards...
  Objective: Garantir que pedidos com gift card sejam calculados corretamente
  Acceptance Criteria:
    - [x] Gift card é descontado do total
    - [ ] Testes unitários cobrem o cenário

Architecture:
  Style:  layered
  Mode:   EXPLICIT
  Confidence: HIGH
```

Se `required: true`, o tool retorna `BLOCK` quando não consegue resolver o work item (branch sem pattern, issue não encontrada, etc.).

---

## Cenário 6: "Quero gerar o contexto completo para debug"

Quando algo dá errado no `ai-review` e você quer entender o que está sendo enviado para a IA:

```bash
java-ai-quality-gate context --format json
```

Isso retorna o JSON completo com git diff, work item, arquitetura e documento arquitetural. É o que o `ai-review` usa internamente.

```bash
java-ai-quality-gate context --format json | jq .
```

---

## Cenário 7: "Estou em outro projeto, quero rodar daqui"

Você está no terminal mas o projeto está em outro lugar:

```bash
java-ai-quality-gate review --project ~/dev/core-backoffice/pedefacil2/billet-araras-service
```

Funciona para qualquer comando:

```bash
java-ai-quality-gate git --project ~/dev/core-backoffice/pedefacil2/order-pedefacil-service
java-ai-quality-gate context --project ~/dev/core-backoffice/pedefacil2/order-pedefacil-service
```

---

## Cenário 8: "Quero salvar o resultado em arquivo"

Use `-o` / `--output` em qualquer comando que gere relatório:

```bash
# Review em texto
java-ai-quality-gate review -o review-report.txt

# Review em JSON
java-ai-quality-gate review -f json -o review-report.json

# AI review em texto
AI_REVIEW_PROVIDER=ollama java-ai-quality-gate ai-review -o ai-review.txt

# AI review em JSON
AI_REVIEW_PROVIDER=ollama java-ai-quality-gate ai-review -f json -o ai-review.json
```

O arquivo é criado automaticamente (incluindo diretórios intermediários). Após salvar, a mensagem `Salvo em: <caminho>` é impressa no terminal.

---

## Cenário 9: "Quero saída JSON para integração"

Para consumir o resultado programaticamente (scripts, CI, dashboards):

```bash
java-ai-quality-gate review --format json
```

```json
{
  "project": "/home/rodrigo/dev/core-backoffice/pedefacil2/order-pedefacil-service",
  "status": "PASS",
  "checks": [
    {
      "gate": "Maven Build",
      "status": "PASS",
      "message": "mvn verify executado com sucesso",
      "durationMs": 23400
    }
  ]
}
```

---

## Cenário 10: "Setup de um projeto novo"

### 1. Crie o arquivo de configuração

```bash
cd ~/dev/novo-projeto-java
```

Crie `.quality-gate.yml` na raiz:

```yaml
maven:
  timeoutSeconds: 600
  args:
    - -B

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
      - "(GSI-\\d+)"

aiReview:
  enabled: true
  provider: command
  command:
    - /home/rodrigo/.local/bin/ai-reviewer
```

O provider de IA é selecionado via variável de ambiente (ver Cenário 4 — Providers de IA):

```bash
# Claude (default)
java-ai-quality-gate ai-review

# OpenRouter
AI_REVIEW_PROVIDER=openrouter java-ai-quality-gate ai-review

# Ollama no VPS
AI_REVIEW_PROVIDER=ollama java-ai-quality-gate ai-review
```

### 2. Gere o contrato arquitetural

```bash
java-ai-quality-gate architecture init
```

Isso gera `ARCHITECTURE.md` inferido da estrutura do projeto. Revise e ajuste manualmente — a inferência automática é um ponto de partida, não a verdade.

### 3. Teste

```bash
java-ai-quality-gate review
java-ai-quality-gate context --format json | jq .
```

---

## Cenário 11: "Revisar um commit específico"

Útil quando o código já foi commitado e pushado — o `--base` não funciona porque o diff está vazio.

```bash
# Ver o diff de um commit
java-ai-quality-gate git --commit bce5615d

# Ver o contexto completo de um commit
java-ai-quality-gate context --commit bce5615d

# Revisar um commit com IA
java-ai-quality-gate ai-review --commit bce5615d

# Salvar a revisão em arquivo
java-ai-quality-gate ai-review --commit bce5615d -o review.json
```

Funciona com qualquer ref: hash, tag, branch:

```bash
java-ai-quality-gate git --commit v1.2.0
java-ai-quality-gate git --commit fix/FS-708
java-ai-quality-gate git --commit HEAD~3
```

O `--commit` diffa o commit informado contra o pai (`git diff <commit>~1..<commit>`).

---

## Cenário 12: "Integrar com CI/CD"

### GitHub Actions

Adicione um step no seu workflow:

```yaml
- name: Quality Gate
  run: |
    java-ai-quality-gate review --format json > quality-gate-report.json
    java-ai-quality-gate review
```

O exit code 1 falha o step automaticamente.

### GitLab CI

```yaml
quality-gate:
  stage: test
  script:
    - java-ai-quality-gate review
  allow_failure: false
```

---

## Comandos rápidos de referência

| Comando | Quando usar |
|---------|-------------|
| `java-ai-quality-gate review` | "Vou dar push, quer saber se compila e passa nos testes" |
| `java-ai-quality-gate review -o report.txt` | "Quero salvar o resultado do review em arquivo" |
| `java-ai-quality-gate git` | "Quero ver o escopo do que mudei" |
| `java-ai-quality-gate git --commit abc123` | "Quero ver o diff de um commit específico" |
| `java-ai-quality-gate context` | "Quero ver tudo que seria enviado pro reviewer" |
| `java-ai-quality-gate context --commit abc123` | "Contexto completo de um commit específico" |
| `java-ai-quality-gate ai-review` | "Quero que a IA revise meu código" |
| `java-ai-quality-gate ai-review --commit abc123` | "Revisar um commit específico com IA" |
| `java-ai-quality-gate ai-review -o review.json` | "Quero salvar a revisão da IA em arquivo" |
| `java-ai-quality-gate work-item` | "Quero ver o work item da branch atual" |
| `java-ai-quality-gate architecture detect` | "Quero saber que arquitetura esse projeto tem" |
| `java-ai-quality-gate architecture init` | "Projeto novo, quero gerar o ARCHITECTURE.md" |
| `java-ai-quality-gate gitlab-mr <URL>` | "Quero revisar um MR do GitLab sem sair do terminal" |

## Opções gerais

| Flag | Comandos | Descrição |
|------|----------|-----------|
| `--project <path>` | todos | Diretório raiz do projeto (default: atual) |
| `-f, --format text\|json` | review, ai-review, context, work-item | Formato de saída |
| `-o, --output <file>` | review, ai-review | Salva resultado em arquivo |
| `--base <ref>` | git, context, ai-review | Branch/base para o diff |
| `--commit <ref>` | git, context, ai-review | Commit específico (diff contra pai) |
| `--work-item <ID>` | context, ai-review | Work item explícito |

## Precedência de configuração

```
CLI flags > .quality-gate.yml > defaults
```

Exemplo: se o YAML define `timeoutSeconds: 600` mas você passa `--timeout-seconds 120`, vale 120.

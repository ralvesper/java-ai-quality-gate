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
- [ ] Detectar projeto Maven
- [ ] Executar `mvn verify`
- [ ] Capturar stdout/stderr e exit code
- [ ] Gerar resultado estruturado
- [ ] Retornar exit code 0/1

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

## Uso desejado

```bash
java -jar java-ai-quality-gate.jar review --project /path/to/project
```

Saída esperada:

```text
Java AI Quality Gate
--------------------
Project: customer-service
Build: PASS
Tests: PASS

QUALITY GATE: PASS
```

## Princípio

O gate deve combinar validações determinísticas com IA. A IA nunca substitui build, testes, análise estática ou regras arquiteturais executáveis.

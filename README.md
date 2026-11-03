# 🎮 Gamificação - Prática 5 DevOps (Grupo 7)

Projeto desenvolvido como parte da disciplina de Prática 5 de DevOps. Trata-se de uma aplicação full stack com arquitetura monorepo composta por:
- **Backend (`api/`)**: API REST desenvolvida em **Java 21** com **Spring Boot 4**, **Spring Data JPA**, **Hibernate** e conexão com banco de dados relacional.
- **Frontend (`client/`)**: Aplicação web desenvolvida com **React 19**, **TypeScript** e **Vite**.
- **Banco de Dados (`postgres`)**: **PostgreSQL 16 (Alpine)** executado em container Docker.
- **Orquestração & DevOps**: Ambientes containerizados com **Docker** e **Docker Compose**, contando com suporte a multi-stage builds e **Docker Compose Watch** para sincronização em tempo real (hot-reload).

## 👥 Integrantes do Grupo

| Nome | RA |
|---|---|
| Enzo Yamaoca Borlini | 236774 |
| Giovanne Vieira de Queiroz | 235969 |
| Lucas Marino Tomazeli | 235798 |

---

## 📋 Sumário

- [Integrantes do Grupo](#-integrantes-do-grupo)
- [Visão Geral e Arquitetura](#-visão-geral-e-arquitetura)
- [Registro Arquitetural — B1 Architecture Lab (Desafio 1)](#-registro-arquitetural--b1-architecture-lab-desafio-1)
- [📊 Evidências — índice rápido](#-evidências--índice-rápido)
- [🧪 Teste de Carga (k6) — Runbook](tools/loadtest/README.md)
- [Mapeamento de Portas e Serviços](#-mapeamento-de-portas-e-serviços)
- [Pré-requisitos](#-pré-requisitos)
- [Como Clonar o Repositório](#-como-clonar-o-repositório)
- [Configuração de Variáveis de Ambiente](#-configuração-de-variáveis-de-ambiente)
- [Como Executar e Compilar via Docker](#-como-executar-e-compilar-via-docker-recomendado)
  - [Subindo o Ambiente Completo](#subindo-o-ambiente-completo)
  - [Desenvolvimento com Docker Watch (Hot-Reload)](#desenvolvimento-com-docker-watch-hot-reload)
  - [Compilação Manual das Imagens Docker (Multi-Stage Builds)](#compilação-manual-das-imagens-docker-multi-stage-builds)
  - [Executando Testes dentro dos Containers Docker](#executando-testes-dentro-dos-containers-docker)
  - [Comandos Úteis do Docker Compose](#comandos-úteis-do-docker-compose)
- [Como Compilar e Executar Normalmente (Local / Sem Docker)](#-como-compilar-e-executar-normalmente-local--sem-docker)
  - [Passo 1: Subir Apenas o Banco PostgreSQL](#passo-1-subir-apenas-o-banco-postgresql)
  - [Passo 2: Backend (Spring Boot)](#passo-2-backend-spring-boot)
  - [Passo 3: Frontend (React + Vite)](#passo-3-frontend-react--vite)
- [Como Conectar ao PostgreSQL pelo IntelliJ IDEA](#-como-conectar-ao-postgresql-pelo-intellij-idea)
- [Endpoints da API e Exemplos de Uso](#-endpoints-da-api-e-exemplos-de-uso)
  - [Documentação interativa (Swagger UI / OpenAPI)](#8-documentação-interativa-swagger-ui--openapi)
- [Metodologia ATDD — Passo a Passo dos TDDs](#-metodologia-atdd--passo-a-passo-dos-tdds)
- [Estrutura de Pastas do Repositório](#-estrutura-de-pastas-do-repositório)
- [Resolução de Problemas (Troubleshooting)](#-resolução-de-problemas-troubleshooting)

---

## 🏛 Visão Geral e Arquitetura

```mermaid
flowchart LR
    subgraph Host["Máquina Host / Navegador"]
        ClientBrowser["Navegador Web<br/>(localhost:5173)"]
        IDE["IntelliJ IDEA / DBeaver<br/>(localhost:5433)"]
        Curl["Requisições HTTP / Postman<br/>(localhost:8080)"]
    end

    subgraph DockerEnv["Docker Compose Network"]
        ClientContainer["Container Frontend: devops_client<br/>(Node 24 Alpine / Vite)"]
        ApiContainer["Container Backend: devops_api<br/>(Java 21 Eclipse Temurin / Spring Boot)"]
        DbContainer[("Container Banco: devops_postgres<br/>PostgreSQL 16 Alpine")]
    end

    ClientBrowser -->|"Acessa interface"| ClientContainer
    Curl -->|"Chamadas REST"| ApiContainer
    ClientContainer -->|"Comunicação com API"| ApiContainer
    ApiContainer -->|"Persistência JPA - porta 5432"| DbContainer
    IDE -->|"Conexão Externa - porta mapeada 5433"| DbContainer
```

---

## 🏛 Registro Arquitetural — B1 Architecture Lab (Desafio 1)

> Plataforma de Gamificação para Educação Continuada — evolução do MVP da AC1.
> Registro cumulativo exigido pelo B1: RF/RNF → ASR → RPC → alternativas/trade-offs → ADR → C4 → evidência.
> Regra de ouro: a decisão deve poder ser lida de trás para frente — o C4 é explicado pelo ADR; o ADR é sustentado pelos ASRs/RPCs; os ASRs derivam de RF/RNF; e a evidência demonstra que o raciocínio é plausível.
> **Índice de evidências**: no [índice de evidências](#-evidências--índice-rápido).

---

### Desafio 1 — Drivers e Evolução do Core (Monólito em Camadas → **Monólito Modular**)

#### 1. RF — o que o sistema precisa fazer

| RF | Descrição | Estado |
|---|---|---|
| **RF01 Ranking** | Top-N de alunos por moedas (desempate por cursos concluídos), cresce mais que as demais funcionalidades | ✅ `GET /ranking?top=` |
| **RF02 Conquistas** | Catálogo de conquistas por marcos (cursos/moedas/plano) com listagem e "como obter" | ✅ `GET /alunos/{id}/conquistas` |
| Evolução AC1 | Cursos, matrículas, recompensas (moedas/vouchers/cursos bônus) seguem evoluindo | preservada |
| Correção derivada da evidência | `GET /alunos` sem paginação (custo O(n) medido) | ✅ paginado (`?pagina=&tamanho=`, máx 100) |

#### 2. RNF (FURPS+) — qualidades em jogo

- **Performance**: leitura do ranking desproporcional às demais; p95 < 500 ms como meta local.
- **Escalabilidade**: **100.000 usuários simultâneos** (premissa confirmada em aula; modelo em §7.0).
- **Manutenibilidade**: 6 devs evoluindo ranking/cursos/recompensas simultaneamente.
- Testabilidade e complexidade operacional — analisadas, **não selecionadas como ASR** (consequência, não driver).

#### 3. ASR — requisitos arquiteturalmente significativos (máx. 3)

| # | ASR | Como altera estrutura/comunicação/dados/implantação |
|---|---|---|
| ASR-1.1 | **Desempenho do ranking** | Read model dedicado (`ranking_entries`) alimentado por eventos + índice `(moedas DESC, cursos_concluidos DESC)` — leitura pesada não compete com as escritas do Core |
| ASR-1.2 | **Escalabilidade evolutiva** (100k simultâneos) | Fronteiras por capacidade + leitura servida de read model escalável por réplicas; seams para extração seletiva futura sem big-bang |
| ASR-1.3 | **Manutenibilidade (6 devs)** | Módulos + eventos: RF02 entrou como módulo novo + listener, zero mudança em código existente |

#### 4. RPC — restrições e premissas

| RPC | Implicação |
|---|---|
| Core em **Java/Spring Boot 4.1** (Java 21) | Evolução interna, não reescrita |
| Equipe de **6 desenvolvedores** | Coordenação de serviços distribuídos proibitiva agora |
| **PostgreSQL 16** + legado AC1 | Reaproveitar entidades/regras; contrato HTTP da AC1 preservado |
| **100k usuários simultâneos** (confirmado em aula) | Modelo de carga em §7.0; leitura escala por réplicas; escrita tem teto por design |
| Sem deploy independente comprovado | Evidência orienta; não há demanda real |

#### 5. Alternativas e trade-offs

| | (A) Monólito em Camadas | (B) Monólito Modular ← **decisão** | (C) Decomposição seletiva |
|---|---|---|---|
| Desempenho (ASR-1.1) | read model possível, sem fronteira que o proteja | read model + evento; leitura pesada isolada por módulo | o melhor para ranking isolado |
| Escalabilidade (ASR-1.2) | bloco único; extração futura arriscada | bloco hoje, seams para extração | escala por capacidade |
| Manutenibilidade (ASR-1.3) | camadas transversais | fronteiras verificadas em build (`verify()`) | fronteiras físicas, custo de coordenação |
| Complexidade operacional | 1 deployável | 1 deployável | N deployáveis — desproporcional |
| **Dados medidos** | A/B medido: **±4% vs (B)** em todos os endpoints (§7.2) | — | fora do escopo do mini-lab |

**Decisão**: (B) — neutralidade de performance camadas×módulos **medida** (±4%); (C) injustificável para 6 devs sem demanda; (A) não protege os ASRs 1.2/1.3.

#### 6. ADR-001 — Evoluir o Core para Monólito Modular (Spring Modulith)

- **Contexto**: MVP em camadas crescendo para 100k simultâneos; ranking cresce mais; 6 devs; Spring Boot + PostgreSQL; legado AC1.
- **Decisão**: Spring Modulith 2.1 — módulos por capacidade (`aluno`, `curso`→`matricula`, `voucher`, `progressao`, `ranking`, `conquistas`, `shared`), `@ApplicationModule(allowedDependencies)` verificado em build, comunicação híbrida: recompensas **síncronas** (contrato AC1 preservado) + efeitos de leitura via **`CursoConcluidoEvent`** consumido por `ranking` e `conquistas` (`AFTER_COMMIT` + `REQUIRES_NEW`).
- **Consequências**:
  - ✅ RF02 = módulo novo + listener, **zero mudança** em código existente (open/closed por módulo).
  - ✅ Fronteiras quebram o build se violadas (antes do code review).
  - ⚠️ Eventual consistency mínima no read model — aceitável (visões, nunca bloqueiam conclusão).
  - ✅ **Paginação de `/alunos` executada** (da evidência: 60 rps/p95 3-5s/resposta 1,4MB → pós-fix medido ~1.3k rps — detalhe em §7.1).
  - ✅ **Virtual threads + pool 50 adotados e declarados no `application.properties`** (a aplicação é dona da decisão; apenas **verifica** no runtime — §7.1).
  - ⚠️ Escrita sustentada em escala é trabalho de **evento/fila (Desafio 4)** — a seam (`CursoConcluidoEvent`) já existe.

#### 7. Evidência — carga (k6) e antes/depois de estrutura

##### 7.0 Modelo de carga — 100k simultâneos traduzidos em engenharia

Usuário simultâneo não dispara 1 req/s; a agregação depende do *think time*:

| Cenário de uso | Think time | Carga agregada | Ranking (~50%) |
|---|---|---|---|
| A. passivo | 60s | ~1.700 rps | ~850 rps |
| B. ativo | 30s | ~3.300 rps | ~1.700 rps |
| C. intenso/polling | 10s | **~10.000 rps** | **~5.000 rps** |
| D. burst (resultado) | — | picos ≫ 10k | concentra no ranking |

**Dimensionamento por endpoint** (teto por instância medido na campanha final — §7.1; réplica = instância stateless):

| Endpoint | Teto/instância (medido) | Mix C | Caminho p/ além |
|---|---|---|---|
| `GET /ranking` (read model) | **~5.000 rps** | ~5.000 rps | **1 instância cobre**; além: réplicas, réplica de leitura PG, cache do top-20 |
| `POST /login` (BCrypt) | **~118/s** (CPU-bound, degrada graciosa: 0% erro) | burst matinal ~333/s | ~3 réplicas p/ burst |
| Dashboard (4 GETs/lote) | **~1.450 lotes/s** (~5,8k req/s) | — | réplicas |
| `GET /alunos` paginado | **~1.260 rps** (fila de pool: 50 conns × ~35ms) | share minoritário | pool/query |
| Write-flow (conclusão) | **~53 it/s** (31,9% timeouts sob demanda 300) | ~100-160 rps | **assincronia (Desafio 4)** |

##### 7.1 Campanha FINAL — arquitetura declarada, medida e verificada

**Configuração**: Modulith + **VT + pool 50 declarados no `application.properties`** e **verificados no runtime** (jcmd na JVM da aplicação: executor clássico `http-nio-exec` ausente + `VirtualThread-unparker` presente — `docs/arquitetura/evidencias/desafio1/final/vt-verificacao.txt`). **Executor**: `ramping-arrival-rate` (demanda explícita em rps; iterações *dropadas* = taxa não sustentada), 5 cenários por taxa-alvo, 75s cada, seed 10k alunos.

**Hardware da medição** (também em `ambiente.txt`): **AMD Ryzen 5 5600X (6 núcleos físicos / 12 threads), 32 GB RAM, NVMe, Ubuntu 24.04** — API, Postgres e k6 **na mesma máquina** (medição conservadora: CPU compartilhada; build de dev via Maven). ⚠️ **Caveat**: os números **absolutos** são 1:1 com este ferro (~833 rps/núcleo físico no teto do ranking) — em outro hardware, dimensione proporcionalmente ou re-rodando a bateria de testes. As **descobertas relativas** — neutralidade camadas×módulos (±4%), invariância do teto a threads/pool, classes de gargalo por endpoint — são as lições transferíveis.

| Cenário | Alvo | **Entregue** | p95 | Falhas | Drops | Diagnóstico |
|---|---|---|---|---|---|---|
| `ranking` | 10.000/s | **~5.000/s** | 575ms | 0% | 524k | **Platô CPU do stack** — invariante a pool 10→50 e plataforma→VT (§7.2): threads/conexões não são o limite |
| `dashboard` | 5.000 lotes/s | **~1.450/s** (5,8k req/s) | 810ms | 0% | 191k | Mesmo teto agregado do stack |
| `alunos-page` | 10.000/s | **~1.260/s** | 413ms | 0,5% | 505k | **Fila de pool** (50 × ~35ms: COUNT+fetch) — classe de gargalo distinta |
| `login` | 333/s | **~118/s** | 7.167ms | **0%** | 11k | **BCrypt CPU-bound**; degradação graciosa (fila 3,4s média, zero erro) |
| `write-flow` | 300 it/s | **~53 it/s** | 30.002ms | **31,9%** | 16k | Custo transacional (recompensas + 2 listeners) — resposta arquitetural é evento (D4) |

**Pinning**: **0 eventos `jdk.VirtualThreadPinned` (≥20ms)** com JFR na JVM correta — pgjdbc+JDK 21.0.3 não pinaram neste workload (`pinning-eventos.txt`). *Nota metodológica*: a primeira geração de dumps/JFR mirava o PID 1 (launcher Maven — a app roja como filho)

**Conclusões** (detalhe no [índice de evidências](#-evidências--índice-rápido)):
1. **Gargalos têm classes distintas por endpoint** — CPU-computacional (login, ranking) × fila-de-conexão (alunos-page) × custo-transacional (write). Tuning cego erraria o alvo 3×.
2. **Teto do ranking invariante** em todas as configurações ⇒ escala por **réplica/cache**, não por threads/pool.
3. **VT: par, não silver bullet** — sozinha (pool 10) colapsava por admissão; com pool 50 sustenta filas profundas com 0% de erro e sem pinning.
4. **Contra o mix C**: ranking (endpoint mais quente) coberto por 1 instância; login burst → ~3 réplicas; write → Desafio 4.

##### 7.2 Histórico das fases de decisão (dataset curado; brutos purgados)

Fases intermediárias do mini-lab (mesmo protocolo de saturação por VUs, worktree do commit pré-modularização para o baseline):

| Comparativo | Resultado | Leitura |
|---|---|---|
| Camadas × Módulos (A/B) | ranking 5.365↔5.358 rps · alunos 62,7↔64,9 · escrita 6,0↔5,7 — **±4%** | **Neutralidade estrutural medida**: a hipótese foi confirmada com dados |
| Pool 10 → **50** + plataforma → **VT** | teto do ranking 5,3k → **5,6k** (≈ruído) | threads/conexões não são o gargalo do ranking |
| VT + pool 10 (saturação antiga) | write 99% falhas rápidas; ranking 14% erros | admissão desbocada sem recurso real → **o par VT+pool é a decisão** |

Evidências das fases históricas: **números consolidados nas tabelas deste registro** (abaixo e na campanha final) — os artefatos brutos não são versionados; cada fase é **regenerável** (`tools/loadtest/`, com worktree do commit pré-modularização para o baseline). Os `resultado.json` brutos (3–4 GB/rodada) são excluídos por padrão (`.gitignore`).

##### 7.3 Antes/depois das dependências (evidência do mini-lab de estrutura)

- **Antes** (`baseline-estrutura/antes-camadas.puml`): pacotes por camada transversais; qualquer classe alcança qualquer outra.
- **Depois** (`pos-modulith-estrutura/components.puml` + `module-*.puml`, **gerados pelo Spring Modulith Documenter**): grafo acíclico verificado — `ranking → matricula (listens to)`, `conquistas → matricula (listens to)`, `matricula → {shared, aluno, voucher, progressao}`, `progressao → shared` (pura).
- **O verificador expôs um ciclo semântico** `matricula ↔ progressao` escondido nas camadas; quebra: regras puras em `progressao`, orquestração em `matricula` (commit `890db22`).
- **Encapsulamento**: raiz do módulo = API pública; subpacotes internos — `ApplicationModules.verify()` falha o build para acesso indevido.

##### 7.4 Diagnóstico de pool (fases históricas)

Nas fases com pool 10: `hikaricp.connections.active` **10/10 com até ~199 pending** em todos os cenários (`metricas-actuator.txt`) — o gargalo transversal que motivou o pool 50 adotado. Na campanha final o sidecar de métricas falhou silenciosamente (flake do Docker 29.x); a leitura de fila está preservada nas latências do k6 (aritmética: 50 conns × ~35ms ≈ 1,4k rps explicam o teto do alunos-page).

#### 8. C4 — Container (decisão do Desafio 1)

```mermaid
flowchart TB
    aluno(["👤 Aluno"])
    subgraph plataforma["Plataforma LevelUp"]
        spa["🖥️ SPA React [client]<br/>React 19 + Vite<br/>matrículas · recompensas · ranking · conquistas"]
        core["☕ Core — Monólito Modular<br/>Java 21 · Spring Boot 4.1 · Modulith 2.1<br/>VT + pool 50 declarados<br/>módulos: aluno · matricula · voucher · progressao · ranking · conquistas · shared"]
        pg[("🗄️ PostgreSQL 16<br/>alunos · matriculas · cursos · vouchers<br/>read models: ranking_entries · conquistas_concedidas<br/>event_publication")]
    end
    aluno -->|"HTTPS"| spa
    spa -->|"REST /api — matriculas · conclusao · ranking · conquistas · vouchers · login"| core
    core -->|"JPA · HikariCP (pool 50)"| pg
```

**Leitura**: a decisão do Desafio 1 é **interna ao Core** (um único deployável; escala por réplicas do mesmo artefato) — o que mudou foi a organização interna (módulos por capacidade + `CursoConcluidoEvent` materializado em `event_publication`), detalhada no Component do Desafio 2. O diagrama usa flowchart (render universal no GitHub) com a semântica C4-Container.

---

### Matriz cumulativa de ASRs (evolui a cada desafio)

| Desafio | ASR | RNF de origem | Driver da decisão |
|---|---|---|---|
| 1 | ASR-1.1 Desempenho do ranking | Performance | read model via eventos (medido: ~5k rps/instância) |
| 1 | ASR-1.2 Escalabilidade (100k simultâneos) | Escalabilidade | módulos verificáveis + escala por réplica |
| 1 | ASR-1.3 Manutenibilidade (6 devs) | Manutenibilidade | eventos desacoplam novas capacidades |

#
---

#### 📊 Evidências — índice rápido

| Evidência | O que contém |
|---|---|
| [`final/suite/dashboard.html`](docs/arquitetura/evidencias/desafio1/final/suite/dashboard.html) | **Dashboard interativo** da suíte (abre no navegador; gráficos por cenário) |
| [`final/suite/metricas-por-cenario.json`](docs/arquitetura/evidencias/desafio1/final/suite/metricas-por-cenario.json) | Análise completa extraída dos brutos (reqs/p95/falhas/drops por cenário) |
| [`final/suite/resumo.json`](docs/arquitetura/evidencias/desafio1/final/suite/resumo.json) | Sumário exportado da suíte |
| [`final/vt-verificacao.txt`](docs/arquitetura/evidencias/desafio1/final/vt-verificacao.txt) · [`pinning-eventos.txt`](docs/arquitetura/evidencias/desafio1/final/pinning-eventos.txt) · [`ambiente.txt`](docs/arquitetura/evidencias/desafio1/final/ambiente.txt) | VT verificada no runtime · pinning JFR · ambiente da medição |
| [`antes-camadas.puml`](docs/arquitetura/evidencias/desafio1/baseline-estrutura/antes-camadas.puml) · [`pos-modulith-estrutura/*.puml`](docs/arquitetura/evidencias/desafio1/pos-modulith-estrutura/) | **Antes/depois das dependências** (o "depois" gerado pelo Modulith Documenter) |

> **Fases históricas** (A/B camadas×módulos, VT+pool10): os números estão nas tabelas do [registro (§7.2)](#-72-histórico-das-fases-de-decisão-dataset-curado-brutos-purgados); os artefatos brutos não são versionados — **qualquer fase é regenerável** (`tools/loadtest/run-loadtest.sh`, incluindo o worktree do baseline pré-modularização). **Runbook do Teste de Carga** — pré-requisitos, cuidados com tamanho de arquivos, como dropar o ambiente e peculiaridades do Docker 29.x: [`tools/loadtest/README.md`](tools/loadtest/README.md).

### 🎯 Conclusões do Desafio 1

> **Em uma frase**: a decisão (Monólito Modular) suporta a meta confirmada de 100k simultâneos com réplicas de um único artefato — e a medição provou que nenhum dos "upgrades óbvios" (threads, conexões) moveria os tetos: os gargalos são **por endpoint, de classes diferentes, cada um com sua alavanca**.

**1. A pergunta central tem resposta medida: NÃO precisa de microserviços.**
O caminho mais quente do case (ranking, ~5k rps no mix C) é servido por **uma única instância dev** com o read model que a modularização introduziu; 2–3 réplicas cobrem burst e resiliência. A alternativa (C) não tem demanda real justificando — conclusão evidenciada, não opinião.

**2. "Escalar a plataforma" é frase vazia sem medir por endpoint — os gargalos têm classes distintas:**

| Classe | Endpoints | O que NÃO adianta | O que funciona |
|---|---|---|---|
| **CPU-computacional** | login (BCrypt ~118/s), ranking (stack ~5k/s) | threads, pool, VT — **provado pela invariância** do teto (5,3→5,6k) | réplicas; cache no top-20 |
| **Fila de conexão** | alunos-page (~1,3k/s = 50 conns × ~35ms) | threads, VT | pool, query mais leve |
| **Custo transacional** | write-flow (~53 it/s, 32% timeouts sob demanda 300) | tudo acima | **assincronia/eventos** — a seam (`CursoConcluidoEvent`) já existe |

Tuning cego erraria o alvo três vezes; o teste por endpoint acerta.

**3. Virtual threads: veredito nuançado e honesto.**
Não movimentou tetos (correto — VT não cria CPU), mas sustentou **degradação graciosa**: login sob 3× a capacidade com fila de 3,4s e **0% de erros**, e **zero pinning ≥20ms** medido com JFR na JVM correta. A lição do percurso: VT *sozinha* (pool 10) colapsava por admissão; VT *pareada* com pool 50 comporta. **Nenhuma tecnologia sozinha — o par é a decisão.**

**4. A evidência dirigiu três correções reais** (o ciclo que o laboratório pede):
60 rps sem paginação → **paginação implementada** (teto pós-fix ~1,3k rps, 21×) · fila de ~200 no Hikari → **pool 50** · colapso de admissão VT+pool10 → **par adotado e declarado no `application.properties`**. A regra de ouro do documento — *evidência sustentando plausibilidade* — aconteceu três vezes.

**5. Fronteiras conscientes** (trade-offs registrados, não buracos):
write sustentado em escala é trabalho de **Desafio 4** (o evento já está no lugar); **soak** de longa duração fica como futuro; o sidecar de métricas falhou na rodada final (a leitura de fila está nas latências do k6 — nota honesta no §7.4).

### Síntese para a Avaliação Final (AF)

- **Levar**: Monólito Modular + eventos (base dos Desafios 2/4); read model do ranking (seam de extração se a escala exigir); **VT + pool 50** (par medido: filas profundas com 0% erro e zero pinning); k6 por taxa de chegada (reutilizável em qualquer decisão de capacidade).
- **Não levar (ainda)**: decomposição em serviços (sem ASR que exija); cache do top-20 (próxima alavanca de ranking, 10-100×); soak de longa duração; assincronia da escrita (Desafio 4).

## 🔌 Mapeamento de Portas e Serviços

| Serviço | Nome do Container | Porta no Container | Porta no Host (Localhost) | Descrição |
| :--- | :--- | :---: | :---: | :--- |
| **API** | `devops_api` | `8080` | `8080` | Servidor REST Spring Boot |
| **API (Debug)** | `devops_api` | `5005` | `5005` | Porta para Remote Debugging JVM |
| **Client** | `devops_client` | `5173` | `5173` | Servidor de desenvolvimento Vite |
| **PostgreSQL** | `devops_postgres` | `5432` | **`5433`** | Banco de dados (*Atenção à porta 5433 no host*) |

> [!WARNING]
> **Atenção à porta do PostgreSQL**: No host, o PostgreSQL está exposto na porta **`5433`** (para não conflitar com possíveis instâncias locais do Postgres rodando na 5432). Dentro da rede Docker dos containers, a porta interna usada pela API é a **`5432`**.

---

## 🧰 Pré-requisitos

### Para rodar via Docker:
- [Docker](https://www.docker.com/) (versão 24 ou superior)
- [Docker Compose](https://docs.docker.com/compose/) (versão 2.20 ou superior com suporte a `develop.watch`)

### Para compilar e rodar localmente (sem Docker nos apps):
- [Java JDK 21](https://adoptium.net/) configurado no sistema (`JAVA_HOME`)
- [Node.js](https://nodejs.org/) versão 20 LTS ou 24 LTS e gerenciador `npm`
- [Maven](https://maven.apache.org/) (opcional, pois o projeto já inclui o Maven Wrapper `mvnw` e `mvnw.cmd`)
- Uma instância do PostgreSQL ativa (pode ser executada via container)
- [Git](https://git-scm.com/) instalado no sistema

---

## 📥 Como Clonar o Repositório

Para obter o código-fonte na sua máquina local:

```bash
# 1. Clonar o repositório via HTTPS:
git clone https://github.com/GiovanneVieira/Pratica5-DevOps.git

# Ou clonar via SSH (caso possua chaves SSH cadastradas no GitHub):
git clone git@github.com:GiovanneVieira/Pratica5-DevOps.git

# 2. Acessar o diretório do projeto:
cd Pratica5-DevOps

# 3. (Obrigatório) Criar uma branch para desenvolver:
git checkout -b nome-da-branch
```

---

## ⚙ Configuração de Variáveis de Ambiente

Antes de iniciar os containers ou a compilação, crie os arquivos de ambiente baseando-se nos exemplos:

### Linux / macOS / Git Bash:
```bash
cp .env.client.api.example .env.client.api
cp .env.client.client.example .env.client.client
```

### Windows (PowerShell):
```powershell
Copy-Item .env.api.example .env.api
Copy-Item .env.client.example .env.client
```

### Conteúdo padrão dos arquivos:

- **`.env.api`**:
  ```env
  POSTGRES_DB=devops
  POSTGRES_USER=postgres
  POSTGRES_PASSWORD=admin
  DATABASE_URL=jdbc:postgresql://postgres:5432/devops
  ```

- **`.env.client`**:
  ```env
  VITE_API_URL=http://localhost:8080
  ```

---

## 🐳 Como Executar e Compilar via Docker (Recomendado)

### Subindo o Ambiente Completo

Para construir as imagens e iniciar todos os serviços (Postgres, API e Client) em segundo plano:

```bash
docker compose up -d --build
```

Após a inicialização:
- **Frontend**: [http://localhost:5173](http://localhost:5173)
- **API Health Check**: [http://localhost:8080/health](http://localhost:8080/health)
- **PostgreSQL**: `localhost:5433`

---

### Desenvolvimento com Docker Watch (Hot-Reload)

O arquivo `docker-compose.yaml` está configurado com `develop.watch`, permitindo sincronização automática de arquivos e recarregamento sem precisar reconstruir manualmente os containers:

```bash
docker compose watch
```
*(ou `docker compose up --watch`)*

- **API (`api/`)**:
  - Alterações em `pom.xml` disparam rebuild automático.
  - Alterações no código-fonte (`api/src`) são sincronizadas e reiniciam a aplicação (`sync+restart`).
- **Client (`client/`)**:
  - Alterações em `client/src` são sincronizadas instantaneamente (`sync`).
  - Alterações em `package.json` ou `package-lock.json` acionam o rebuild do container.

---

### Compilação Manual das Imagens Docker (Multi-Stage Builds)

Tanto o backend quanto o frontend utilizam arquivos Docker multi-estágio (`dockerfile`), permitindo compilar tanto imagens focadas em desenvolvimento quanto imagens otimizadas para produção:

#### 1. Backend (`api/dockerfile`)
- **Estágio de Desenvolvimento (`dev`)**:
  ```bash
  docker build -t devops-api:dev --target dev ./api
  ```
- **Estágio de Produção (`prod`)** *(gera o `.jar`, cria usuário não-root `spring` e utiliza a JRE mínima `eclipse-temurin:21-jre-alpine`)*:
  ```bash
  docker build -t devops-api:prod --target prod ./api
  ```

#### 2. Frontend (`client/dockerfile`)
- **Estágio de Desenvolvimento (`dev`)**:
  ```bash
  docker build -t devops-client:dev --target dev ./client
  ```
- **Estágio de Produção (`production`)** *(roda compilação TypeScript, `vite build`, cria usuário não-root `nodejs` e empacota os arquivos na pasta `dist/`)*:
  ```bash
  docker build -t devops-client:prod --target production ./client
  ```

---

### Executando Testes dentro dos Containers Docker

Você pode executar toda a suíte de testes (testes unitários JUnit e testes de integração com Testcontainers) diretamente dentro dos containers Docker, sem precisar de Java ou Node instalados na máquina host:

#### 1. Com os containers em execução (`docker compose exec`)

Se o ambiente já estiver rodando (`docker compose up -d`):

- **Executar todos os testes do Backend (API)**:
  ```bash
  docker compose exec api mvn test
  ```

- **Executar uma classe de testes específica**:
  ```bash
  docker compose exec api mvn test -Dtest=ApplicationTests
  docker compose exec api mvn test -Dtest=AlunoTest
  ```

- **Executar linter e checagem de tipos do Frontend**:
  ```bash
  docker compose exec client npm run lint
  docker compose exec client npm run build
  ```

#### 2. Em container temporário sob demanda (`docker compose run`)

Caso o ambiente não esteja rodando, você pode disparar um container efêmero apenas para a execução dos testes:

- **Testes do Backend**:
  ```bash
  docker compose run --rm api mvn test
  ```

- **Linter do Frontend**:
  ```bash
  docker compose run --rm client npm run lint
  ```

#### 3. Testes durante o Build da Imagem (CI / CD)

O estágio `builder` do arquivo `api/dockerfile` executa `mvn package`, o que roda todos os testes automaticamente. Se qualquer teste falhar, o processo de build é interrompido garantindo a integridade da imagem:

```bash
docker build -t devops-api:builder --target builder ./api
```

---

### Comandos Úteis do Docker Compose

```bash
# Visualizar logs de todos os containers em tempo real
docker compose logs -f

# Visualizar logs apenas da API
docker compose logs -f api

# Visualizar logs apenas do banco de dados
docker compose logs -f postgres

# Parar todos os containers mantendo os dados do banco
docker compose down

# Parar todos os containers e REMOVER o volume com os dados do banco
docker compose down -v
```

---

## 💻 Como Compilar e Executar Normalmente (Local / Sem Docker)

Se preferir rodar e debugar as aplicações diretamente no seu sistema operacional:

### Passo 1: Subir Apenas o Banco PostgreSQL

Você pode subir apenas o container do banco de dados via Docker sem subir a API e o Client:

```bash
docker compose up postgres -d
```
O PostgreSQL estará disponível em `localhost:5433` (usuário: `postgres`, senha: `admin`, database: `devops`).

---

### Passo 2: Backend (Spring Boot)

> [!TIP]
> **Dificuldades com o IntelliSense / Autocomplete no IntelliJ IDEA?**
> Por se tratar de um monorepo com o backend em uma subpasta (`api/`), o IntelliJ IDEA pode não indexar o Maven de imediato, fazendo com que classes, imports ou anotações do Spring fiquem vermelhas ou sem autocompletar.
> 
> **Como resolver**:
> 1. No painel lateral esquerdo (**Project**), localize e expanda a pasta `api`.
> 2. Clique com o **botão direito** sobre o arquivo `pom.xml`.
> 3. Clique em **"Add as Maven Project"** (ou se já foi adicionado, selecione **Maven ➔ Reload Project**).
> 
> <p align="center">
>   <img src="docs/images/add_as_maven_project.png" alt="Adicionar como Projeto Maven no IntelliJ" width="400"/>
> </p>

1. Acesse o diretório `api`:
   ```bash
   cd api
   ```

2. **Configuração de conexão com o banco**:
   *(Nota: O arquivo `application.properties` já vem configurado por padrão para conectar em `localhost:5433`. Caso queira sobrescrever ou customizar portas/credenciais, defina as variáveis de ambiente abaixo)*:
   - **Linux / macOS**:
     ```bash
     export DATABASE_URL="jdbc:postgresql://localhost:5433/devops"
     export POSTGRES_USER="postgres"
     export POSTGRES_PASSWORD="admin"
     ```
   - **Windows (PowerShell)**:
     ```powershell
     $env:DATABASE_URL="jdbc:postgresql://localhost:5433/devops"
     $env:POSTGRES_USER="postgres"
     $env:POSTGRES_PASSWORD="admin"
     ```

3. **Compilar o projeto**:
   - Linux/macOS:
     ```bash
     ./mvnw clean compile
     ```
   - Windows:
     ```powershell
     .\mvnw.cmd clean compile
     ```

4. **Executar os testes automatizados (JUnit)**:
   - Linux/macOS:
     ```bash
     ./mvnw test
     ```
   - Windows:
     ```powershell
     .\mvnw.cmd test
     ```
   *Executa os testes de unidade JUnit e os testes de integração (que sobem um PostgreSQL efêmero via Testcontainers), cobrindo os cenários BDD especificados na planilha `Template_ATDD_Gamificacao.xlsx`.*


5. **Gerar o pacote de produção (JAR)**:
   - Linux/macOS:
     ```bash
     ./mvnw clean package
     ```
   - Windows:
     ```powershell
     .\mvnw.cmd clean package
     ```
   *O arquivo executável será gerado em: `api/target/gamificacao.grupo-7-0.0.1-SNAPSHOT.jar`.*

6. **Executar a API**:
   - Via plugin do Spring Boot:
     ```bash
     # Linux / macOS
     ./mvnw spring-boot:run

     # Windows
     .\mvnw.cmd spring-boot:run
     ```
   - Ou executando o arquivo JAR diretamente:
     ```bash
     java -jar target/gamificacao.grupo-7-0.0.1-SNAPSHOT.jar
     ```

---

### Passo 3: Frontend (React + Vite)

1. Acesse o diretório `client`:
   ```bash
   cd client
   ```

2. Instale as dependências:
   ```bash
   npm install
   ```

3. Inicie o servidor de desenvolvimento:
   ```bash
   npm run dev
   ```
   Acesse a aplicação no navegador em: `http://localhost:5173`.

4. **Compilar para produção**:
   ```bash
   npm run build
   ```
   *A compilação verifica a tipagem TypeScript (`tsc -b`) e cria a pasta `dist/` com o bundle final otimizado.*

5. **Executar a verificação de linter**:
   ```bash
   npm run lint
   ```

---

## 🐘 Como Conectar ao PostgreSQL pelo IntelliJ IDEA

Siga o passo a passo detalhado para gerenciar e visualizar as tabelas do banco de dados diretamente pelo IntelliJ IDEA:

### 1. Certifique-se de que o container do banco está ativo
Antes de tentar a conexão, verifique se o PostgreSQL está em execução:
```bash
docker compose up postgres -d
```

### 2. Abra a janela de Database no IntelliJ
- No canto direito da tela do IntelliJ IDEA, clique na aba vertical **Database**.
- *(Se não estiver visível, acesse o menu superior: **View** ➔ **Tool Windows** ➔ **Database**)*.

> [!NOTE]
> A ferramenta nativa de **Database** faz parte do **IntelliJ IDEA Ultimate**. Caso utilize o **IntelliJ IDEA Community**, você pode instalar o plugin gratuito **Database Navigator** pelo Marketplace ou utilizar um cliente externo como o [DBeaver](https://dbeaver.io/) ou [pgAdmin](https://www.pgadmin.org/) usando exatamente as mesmas configurações descritas abaixo.

### 3. Criar uma nova conexão (Data Source)
1. Na janela **Database**, clique no botão **`+`** (New) no topo.
2. Navegue até **Data Source** ➔ selecione **PostgreSQL**.

### 4. Preencher as configurações de conexão
Na janela de propriedades da fonte de dados (**Data Sources and Drivers**), preencha os campos com os valores a seguir:

| Campo | Valor a Preencher | Observações |
| :--- | :--- | :--- |
| **Name** | `devops_postgres` | Nome de identificação no IntelliJ |
| **Host** | `localhost` | Máquina local |
| **Port** | **`5433`** | ⚠️ **Importante**: Utilize `5433` (conforme mapeado no `docker-compose.yaml`) |
| **Authentication** | `User & Password` | |
| **User** | `postgres` | Usuário configurado |
| **Password** | `admin` | Senha configurada |
| **Database** | `devops` | Nome da base de dados |
| **URL (gerada)** | `jdbc:postgresql://localhost:5433/devops` | Confirme se a URL coincide |

### 5. Download dos drivers (caso necessário)
Se for a primeira vez configurando o PostgreSQL no IntelliJ, haverá uma mensagem de aviso no rodapé da janela: *"Driver files are missing"*. Basta clicar no link azul **Download** para que a IDE baixe o driver JDBC oficial automaticamente.

### 6. Testar a conexão
1. Clique no botão **Test Connection** (localizado no canto inferior esquerdo da janela).
2. Se os dados estiverem corretos e o container ativo, uma mensagem com ícone verde aparecerá:
   ```text
   Succeeded: PostgreSQL 16.x ...
   ```
3. Clique em **Apply** e em seguida em **OK**.

### 7. Explorando as tabelas geradas pelo Hibernate/JPA
1. Na aba **Database**, expanda a árvore:
   ```text
   devops_postgres
   └── @localhost
       └── devops (database)
           └── schemas
               └── public
                   └── tables
   ```
2. Você verá as entidades mapeadas:
   - `table_aluno`: Tabela contendo os registros de alunos (`aluno_id` [UUID], `name` [VARCHAR]).
   - `curso_table`: Tabela contendo os cursos vinculados (`id` [UUID], `status` [VARCHAR], `aluno_id` [UUID]).
3. **Para ver os dados**: Dê um duplo clique sobre o nome da tabela.
4. **Para executar consultas SQL**: Clique com o botão direito sobre a fonte de dados e escolha **New** ➔ **Query Console**.

---

## 📡 Endpoints da API e Exemplos de Uso

A API REST roda na porta **`8080`**. Abaixo estão os endpoints disponíveis e como testá-los:

### 1. Health Check
Verifica se a aplicação está online.
- **Método**: `GET`
- **URL**: `http://localhost:8080/health`
- **Exemplo via cURL**:
  ```bash
  curl -X GET http://localhost:8080/health
  ```
- **Resposta esperada**:
  ```json
  {
    "message": "OK"
  }
  ```

---

### 2. Cadastro de Aluno
Registra um novo aluno no banco de dados (plano inicial: `BASICO`, 0 moedas).
- **Método**: `POST`
- **URL**: `http://localhost:8080/alunos`
- **Header**: `Content-Type: application/json`
- **Body**:
  ```json
  {
    "name": "João da Silva",
    "email": "joao.silva@email.com",
    "password": "123456"
  }
  ```
- **Exemplo via cURL**:
  ```bash
  curl -X POST http://localhost:8080/alunos \
    -H "Content-Type: application/json" \
    -d "{\"name\": \"João da Silva\", \"email\": \"joao.silva@email.com\", \"password\": \"123456\"}"
  ```
- **Resposta esperada** (`201 Created`):
  ```json
  {
    "id": "e4f8a32b-9e0f-48d6-b0ad-5b43a9f5d1e2",
    "name": "João da Silva",
    "ra": "261234",
    "plano": "BASICO"
  }
  ```

---

### 3. Consulta de Alunos
- **Por ID**: `GET http://localhost:8080/alunos/{id}`
- **Por RA**: `GET http://localhost:8080/alunos/ra/{ra}`
- **Listar todos**: `GET http://localhost:8080/alunos`

---

### 4. Matrícula em Curso
Matricula um aluno em um curso (curso criado com status `INICIADO`).
- **Método**: `POST`
- **URL**: `http://localhost:8080/alunos/{alunoId}/matriculas`
- **Body**:
  ```json
  {
    "nomeCurso": "DevOps Essentials"
  }
  ```
- **Resposta esperada** (`201 Created`):
  ```json
  {
    "id": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "nomeCurso": "DevOps Essentials",
    "status": "INICIADO",
    "notaFinal": 0.0
  }
  ```
- **Listar matrículas do aluno**: `GET http://localhost:8080/alunos/{alunoId}/matriculas`

---

### 5. Conclusão de Curso (regras BDD de gamificação)
Conclui uma matrícula aplicando as regras de progressão da planilha `Template_ATDD_Gamificacao.xlsx`:
- **BDD1** — aluno básico com menos de 11 cursos concluídos e nota > 7.0: libera 3 novos cursos (`INICIADO`) e mantém o plano `BASICO`;
- **BDD2** — 12º curso concluído com nota > 7.0: plano vira `PREMIUM` e concede 3 cursos, 3 moedas e voucher para projetos reais;
- **BDD3** — nota ≤ 7.0: curso fica `REPROVADO`, sem liberar cursos e sem progresso no plano.
- **Método**: `PATCH`
- **URL**: `http://localhost:8080/alunos/{alunoId}/matriculas/{matriculaId}/conclusao`
- **Body**:
  ```json
  {
    "notaFinal": 8.5
  }
  ```
- **Exemplo via cURL**:
  ```bash
  curl -X PATCH http://localhost:8080/alunos/{alunoId}/matriculas/{matriculaId}/conclusao \
    -H "Content-Type: application/json" \
    -d "{\"notaFinal\": 8.5}"
  ```
- **Resposta esperada** (`200 OK`, exemplo do BDD1):
  ```json
  {
    "matriculaId": "3fa85f64-5717-4562-b3fc-2c963f66afa6",
    "nomeCurso": "DevOps Essentials",
    "status": "CONCLUIDO",
    "plano": "BASICO",
    "moedas": 0,
    "cursosLiberados": [
      {"id": "...", "nomeCurso": "Curso Liberado 1", "status": "INICIADO", "notaFinal": 0.0},
      {"id": "...", "nomeCurso": "Curso Liberado 2", "status": "INICIADO", "notaFinal": 0.0},
      {"id": "...", "nomeCurso": "Curso Liberado 3", "status": "INICIADO", "notaFinal": 0.0}
    ],
    "voucher": null
  }
  ```

---

### 6. Login
Valida credenciais comparando a senha do input com o hash **BCrypt** armazenado no cadastro (`passwordEncoder.matches`). Não usa sessão nem token — retorna os dados do aluno quando as credenciais são válidas e `401 Unauthorized` caso contrário (mensagem genérica, sem revelar se o erro foi no email ou na senha).
- **Método**: `POST`
- **URL**: `http://localhost:8080/login`
- **Body**:
  ```json
  {
    "email": "joao.silva@email.com",
    "password": "123456"
  }
  ```
- **Exemplo via cURL**:
  ```bash
  curl -X POST http://localhost:8080/login \
    -H "Content-Type: application/json" \
    -d "{\"email\": \"joao.silva@email.com\", \"password\": \"123456\"}"
  ```
- **Resposta esperada** (`200 OK`):
  ```json
  {
    "id": "e4f8a32b-9e0f-48d6-b0ad-5b43a9f5d1e2",
    "name": "João da Silva",
    "ra": "261234",
    "plano": "BASICO"
  }
  ```
- **Credenciais inválidas** (`401 Unauthorized`):
  ```json
  {
    "message": "Credenciais invalidas",
    "status": "UNAUTHORIZED",
    "path": "/login",
    "timestamp": "2026-09-15T23:59:00.000Z"
  }
  ```

---

### 7. Listar vouchers do aluno
Retorna os vouchers **persistidos** no banco (do upgrade Premium no 12º curso e das recorrências a cada 12 concluídos), para o painel "Seus vouchers" sobreviver ao refresh da página. Aluno inexistente devolve `404`.
- **Método**: `GET`
- **URL**: `http://localhost:8080/alunos/{alunoId}/vouchers`
- **Exemplo via cURL**:
  ```bash
  curl http://localhost:8080/alunos/{alunoId}/vouchers
  ```
- **Resposta esperada** (`200 OK`):
  ```json
  [
    {
      "id": "b1c2...",
      "nome": "Voucher Projetos Reais",
      "valorEmReais": 100.0,
      "descricao": "Voucher para participacao em projetos reais, concedido no upgrade para o plano Premium",
      "status": "VALIDO",
      "expiresAt": "2026-09-23T12:00:00"
    }
  ]
  ```

---

### 8. Documentação interativa (Swagger UI / OpenAPI)

A API gera automaticamente a documentação de todos os endpoints com **SpringDoc OpenAPI** (`springdoc-openapi-starter-webmvc-ui`). Com o backend em execução:

- **Swagger UI**: [http://localhost:8080/swagger](http://localhost:8080/swagger) — console interativo para explorar todos os endpoints (parâmetros, schemas e códigos de resposta) e testá-los diretamente do navegador, incluindo os fluxos de matrícula/conclusão das regras BDD e o login.
- **Especificação OpenAPI 3.1 (JSON)**: [http://localhost:8080/docs](http://localhost:8080/docs) — contrato machine-readable da API, importável em clientes como Postman/Insomnia.

Os caminhos são configurados em `api/src/main/resources/application.properties` (`springdoc.swagger-ui.path=/swagger` e `springdoc.api-docs.path=/docs`).
---

## 🧪 Metodologia ATDD — Passo a Passo dos TDDs

A aplicação foi desenvolvida com **ATDD (Acceptance Test-Driven Development)**: os cenários de aceitação da planilha `Template_ATDD_Gamificacao.xlsx` (aba `pb`) foram transformados em testes **antes** da implementação, e cada funcionalidade evoluiu no ciclo **RED → GREEN → BLUE**:

- **RED** — escreve-se o teste de aceitacao do cenario e ele falha (a regra ainda nao existe);
- **GREEN** — implementa-se o minimo para o teste passar;
- **BLUE (REFACTOR)** — refatora-se o codigo mantendo os testes verdes (extracao de camadas, SOLID, divisao de responsabilidades).

Cada teste carrega em javadoc o cenário Given/When/Then correspondente, e cada ponto do código onde a regra vive está marcado com comentários `TDD1 - GREEN`, `TDD2 - BLUE`, etc.

### TDD1 — Liberação de 3 novos cursos (nota > 7,0, aluno básico com menos de 11 concluídos)

**Cenário (planilha):** *Dado* um aluno com assinatura básica ativa *E* com menos de 11 cursos concluídos *Quando* o aluno conclui um curso *E* obtém nota final superior a 7,0 *Então* o sistema deve liberar o acesso a 3 novos cursos *E* manter a assinatura no plano básico.

1. **RED**: teste de aceitação criado para "concluir curso com nota 8,0 deve liberar 3 cursos e manter plano básico" — falhou porque não existia regra de progressão (nada era liberado na conclusão).
2. **GREEN**: implementada a decisão `concluidosAntes < 11 && nota > 7,0 → liberar 3 cursos com status INICIADO` e a transição `Curso.conclui()` (nota > 7,0 → `CONCLUIDO`).
3. **BLUE**: a regra saiu do orquestrador e virou a política `PoliticaProgressaoPadrao.avaliar()` (retorno `CURSO_BONUS_BASICO`), e a concessão dos 3 cursos ficou em `RecompensasService.liberarCursosBonus()` — o orquestrador `ConclusaoCursoService` apenas coordena.

**Onde está hoje:** `service/progressao/PoliticaProgressaoPadrao.java` (bloco `TDD1 - GREEN`), `service/RecompensasService.java` (`liberarCursosBonus`).
**Testes:** `PoliticaProgressaoPadraoTest.bdd1_...`, `RecompensasServiceTest.liberarCursosBonus...`, `MatriculaFlowIntegrationTest.bdd1_...` (HTTP de ponta a ponta).

### TDD2 — Upgrade para Premium no 12º curso (3 cursos + 3 moedas + voucher)

**Cenário (planilha):** *Dado* um aluno com plano básico e 11 cursos concluídos *E* com todas as avaliações validadas *Quando* o aluno conclui o seu 12º curso *E* obtém nota final superior a 7,0 *Então* a assinatura deve ser alterada para "Premium" *E* conceder 3 cursos, 3 moedas e voucher para projetos reais.

1. **RED**: teste de aceitação criado para "12º curso concluído com nota > 7,0 deve virar Premium com 3 cursos, 3 moedas e voucher" — falhou porque o upgrade não existia (plano permanecia básico, sem moedas e sem voucher).
2. **GREEN**: implementada a decisão `concluidosAntes >= 11 && nota > 7,0 → UPGRADE_PREMIUM` e o pacote de recompensas (plano `PREMIUM`, `moedas += 3`, voucher `VALIDO` com validade de 7 dias, 3 cursos `INICIADO`).
3. **BLUE**: decisões de limite isoladas em `PoliticaProgressaoPadrao` (constantes `CursosParametros.LIMITE_CURSOS_UPGRADE_PREMIUM`/`CURSOS_NECESSARIOS_PREMIUM`), montagem do pacote em `RecompensasService.concederRecompensasPremium()` reaproveitando o value object `RecompensasPremiumDTO` (voucher + cursos + moedas).

**Onde está hoje:** `service/progressao/PoliticaProgressaoPadrao.java` (bloco `TDD2 - GREEN`), `service/RecompensasService.java` (`concederRecompensasPremium`).
**Testes:** `PoliticaProgressaoPadraoTest.bdd2_...`, `RecompensasServiceTest.concederRecompensasPremium...`, `MatriculaFlowIntegrationTest.bdd2_...`.

### TDD3 — Nota ≤ 7,0 não libera nada nem progride o plano

**Cenário (planilha):** *Dado* um aluno com assinatura básica ativa *E* matriculado em um curso da grade *Quando* o aluno conclui o curso *E* obtém nota final igual ou inferior a 7,0 *Então* nenhum curso adicional deve ser liberado *E* o progresso para o plano Premium não deve ser incrementado.

1. **RED**: teste de aceitação criado para "nota 7,0 não libera curso adicional nem incrementa progresso" — falhou porque a primeira versão da conclusão marcava qualquer curso como concluído, contabilizando progresso indevidamente.
2. **GREEN**: a transição `Curso.conclui()` passou a reprovar com nota ≤ 7,0 (`REPROVADO`), o que naturalmente bloqueia bônus e progresso (a nota exatamente 7,0 reprova porque os cenários TDD1/TDD2 exigem nota "superior a 7,0").
3. **BLUE**: a reprovação flui como `ResultadoProgressao.REPROVADO_SEM_PROGRESSO` na política, e o orquestrador responde com recompensa vazia (`Recompensa.vazia()`) — sem cursos, sem moedas, sem voucher, sem tocar no plano.

**Onde está hoje:** `model/Curso.java` (`conclui()`), `service/progressao/PoliticaProgressaoPadrao.java` (bloco `TDD3 - GREEN`).
**Testes:** `CursoTest` (nota 7,0 e 6,5 → `REPROVADO`; 7,1 → `CONCLUIDO`), `PoliticaProgressaoPadraoTest.bdd3_...`, `MatriculaFlowIntegrationTest.bdd3_...` (notas 7,0 e 6,5).

### TDD4 — Apagar curso do histórico não apaga o progresso do aluno (ciclo de correção)

**Cenário (bug reportado, não presente na planilha):** *Dado* um aluno com cursos concluídos no progresso *Quando* ele apaga um curso concluído do histórico (desistência/remoção) *Então* o progresso acumulado para o plano Premium deve permanecer intacto.

1. **RED**: o progresso era **derivado** da contagem de matrículas `CONCLUIDO` no banco (`countByAlunoIdAndCursoStatus`) e do filtro da lista no front — apagar um curso concluído retrocedia o progresso. Testes criados para o comportamento esperado falharam: a política era consultada com o total errado e o contador do aluno não era incrementado na conclusão (`ConclusaoCursoServiceTest.deveIncrementarProgressoPersistido...`).
2. **GREEN**: o progresso virou **estado persistido do aluno** (`Aluno.cursosConcluidos`), incrementado a cada conclusão com nota > 7,0 em `ConclusaoCursoService.atualizaProgresso()`; a `PoliticaProgressao` passou a ser consultada com esse valor. O método de contagem derivada foi removido do repositório.
3. **BLUE**: `AlunoResponseDTO` expõe `cursosConcluidos` para a UI exibir o progresso oficial (o stat "CURSOS CONCLUÍDOS" do dashboard deixou de derivar do histórico); desistência/apagamento não tocam no contador, e a coluna nova é nullable-safe para linhas criadas antes da migração (backfill aplicado no banco de desenvolvimento).

**Onde está hoje:** `model/Aluno.java` (`cursosConcluidos`), `service/ConclusaoCursoService.java` (`atualizaProgresso`, bloco `TDD4 - GREEN`), `dto/aluno/AlunoResponseDTO.java`.
**Testes:** `ConclusaoCursoServiceTest` (incrementa na conclusão; não incrementa na reprovação), `MatriculaServiceTest.naoDeveReduzirProgressoPersistido...` (desistência mantém progresso), `AlunoMapperTest.deveExporProgressoPersistido...`, `MatriculaFlowIntegrationTest.tdd4_...` (aluno com 11 concluídos apaga um do histórico e vira Premium no 12º progresso).

### TDD5 — Recompensas recorrentes a cada 12 cursos concluídos (evolução do BDD2)

**Cenário (interpretação recorrente do BDD2):** *Dado* um aluno Premium que já recebeu o pacote do 12º curso *Quando* ele fecha cada novo ciclo de 12 concluídos com nota > 7,0 (24º, 36º...) *Então* recebe novamente **3 moedas e voucher** para projetos reais — o upgrade de plano e os cursos bônus continuam exclusivos do primeiro 12º (BDD2 original).

1. **RED**: a política devolvia `SEM_RECOMPENSA` para qualquer aluno Premium — moedas e voucher eram concedidos uma única vez na vida. Testes criados para a recorrência falharam (`PoliticaProgressaoPadraoTest.tdd5_...`).
2. **GREEN**: novo resultado `RECOMPENSA_RECURRENTE` na política (progresso `+1` múltiplo de 12 e plano Premium) e `RecompensasService.concederRecompensasRecorrentes()`: credita 3 moedas e emite novo voucher (VÁLIDO, 7 dias), sem tocar no plano nem matricular cursos bônus.
3. **BLUE**: `ConcluirCursoResponseDTO` passou a explicitar `upgradePremium` e `moedasRecebidas` (o que foi concedido *nesta* operação, distinguindo saldo de crédito) para a UI exibir o modal correto em cada caso.

**Onde está hoje:** `service/progressao/PoliticaProgressaoPadrao.java` (bloco `TDD5 - GREEN`), `service/RecompensasService.java` (`concederRecompensasRecorrentes`), `service/ConclusaoCursoService.java` (mapeamento do resultado).
**Testes:** `PoliticaProgressaoPadraoTest.tdd5_...` (12º/24º/36º de Premium), `RecompensasServiceTest.concederRecompensasRecorrentes...`, `ConclusaoCursoServiceTest.deveConcederRecompensasRecorrentes...`, `MatriculaFlowIntegrationTest.tdd5_...` (aluno vira Premium no 12º e recebe novamente no 24º: saldo 6, segundo voucher, sem upgrade).

### TDD6 — Consulta dos vouchers persistidos (ciclo de correção)

**Cenário:** *Dado* um aluno com vouchers conquistados *Quando* a página é recarregada *Então* o painel "Seus vouchers" deve exibi-los novamente — os vouchers sempre foram persistidos no banco, mas não existia endpoint de listagem e a UI só guardava o voucher do response de conclusão em estado de sessão (somia no refresh).

1. **RED**: teste de integração criado para `GET /alunos/{id}/vouchers` falhou com **404** — a rota não existia.
2. **GREEN**: `VoucherRepository.findAllByAlunoId` + `VoucherService.listarVouchers` (valida aluno → 404) + `VoucherController` com `GET /alunos/{alunoId}/vouchers`; o mapeamento `toResponseDTO` migrou do `MatriculaMapper` para o `VoucherMapper` (lugar da responsabilidade) e o `VoucherResponseDTO` para o pacote `dto/voucher/`.
3. **BLUE**: a UI passou a buscar os vouchers no carregamento do dashboard (`getVouchers` no `carregarDados` e no mount), e a adição manual do voucher pós-conclusão foi removida (o recarregamento já traz a lista completa, sem duplicatas).

**Onde está hoje:** `controller/VoucherController.java`, `service/VoucherService.java`, `repository/VoucherRepository.java`, `mapper/VoucherMapper.java` (`toResponseDTO`).
**Testes:** `VoucherServiceTest` (lista vouchers persistidos; aluno inexistente lança 404), `MatriculaFlowIntegrationTest.tdd6_...` (aluno com 2 vouchers conquistados → GET retorna os 2; aluno inexistente → 404).

### Mapa de cobertura dos cenários

| Camada | TDD1 | TDD2 | TDD3 | TDD4 | TDD5 | TDD6 |
|---|---|---|---|---|---|---|
| Domínio (`CursoTest`) | — | — | nota ≤ 7,0 reprova | — | — | — |
| Regra (`PoliticaProgressaoPadraoTest`) | `bdd1_...` | `bdd2_...` | `bdd3_...` | — | `tdd5_...` | — |
| Recompensas (`RecompensasServiceTest`) | 3 cursos INICIADO | Premium + moedas + voucher | — | — | recorrência: +3 moedas + voucher | — |
| Orquestração (`ConclusaoCursoServiceTest`) | chama `liberarCursosBonus` | chama `concederRecompensasPremium` | nenhuma recompensa | progresso persistido incrementa/retém | chama `concederRecompensasRecorrentes` | — |
| Desistência (`MatriculaServiceTest`) | — | — | — | progresso intacto ao apagar | — | — |
| Vouchers (`VoucherServiceTest`) | — | — | — | — | — | lista persistidos; 404 |
| Integração HTTP (`MatriculaFlowIntegrationTest`) | fluxo completo | fluxo completo | fluxo completo | apagar concluído mantém progresso | 24º curso re-premia | GET vouchers retorna os 2 |

> **Trajetória histórica:** os três ciclos nasceram na primeira entrega como testes de unidade diretos sobre a entidade `Aluno` (que acumulava as regras de propósito, documentado no próprio código da época). Na refatoração para camadas MVC a lógica migrou para os services, e na refatoração SOLID final foi dividida em `PoliticaProgressao` (decisão), `RecompensasService` (concessão) e `ConclusaoCursoService` (orquestração) — com os testes de aceitação mantidos verdes em cada etapa, como manda o ATDD.

---

## 📂 Estrutura de Pastas do Repositório

```text
Pratica5-DevOps/
├── .env.api.example             # Template de variáveis de ambiente da API
├── .env.client.example          # Template de variáveis de ambiente do Frontend
├── docker-compose.yaml          # Orquestração: api, client, postgres + perfil `loadtest` (k6)
├── README.md                    # Documentação principal + registro arquitetural do B1
│
├── api/                         # Backend em Spring Boot — Monólito Modular (Spring Modulith)
│   ├── dockerfile               # Multi-stage Dockerfile (dev, builder, prod)
│   ├── pom.xml                  # Gerenciador de dependências Maven
│   ├── mvnw / mvnw.cmd          # Maven Wrapper (Linux / Windows)
│   └── src/
│       ├── main/java/com/example/gamificacao/grupo_7/
│       │   ├── Application.java, SecurityConfig, GlobalExceptionHandler, HealthController
│       │   ├── aluno/           # Módulo: cadastro, consulta e login (RA na raiz = API pública)
│       │   ├── matricula/       # Módulo: matrícula/conclusão + CursoConcluidoEvent (API pública)
│       │   ├── voucher/         # Módulo: vouchers como recompensa
│       │   ├── progressao/      # Módulo: regras puras de progressão (sem infra)
│       │   ├── ranking/         # RF01: read model consumidor do evento
│       │   ├── conquistas/      # RF02: catálogo/concessão consumidora do evento
│       │   └── shared/          # Tipos compartilhados (Plano, CursoStatus, BusinessException)
│       │       # Cada módulo: service/ controller/ model/ repository/ dto/ mapper/ internos
│       ├── main/resources/application.properties  # VT + pool 50 declarados (arquitetura da API)
│       └── test/                # Unit (Mockito) + integração (Testcontainers) + ModularityTests
│
├── client/                      # Frontend em React + Vite + TypeScript
│   ├── dockerfile               # Multi-stage Dockerfile (deps, dev, build, production)
│   ├── package.json             # Dependências e scripts npm
│   ├── tsconfig.json / vite.config.ts
│   └── src/
│       ├── components/ui/       # Modal, ConfirmModal, RewardModal, Icon
│       ├── components/features/ # Home, Register, Login, Dashboard + painéis (Ranking, Conquistas...)
│       ├── hooks/               # useAuth, useMatriculas, useVouchers, useConquistas, useRanking, usePage
│       ├── context/AuthContext.tsx  # Sessão como fonte única de verdade
│       └── services/api.ts      # Camada de API tipada
│
├── tools/
│   └── loadtest/                # 🧪 Harness de carga k6 — ver README próprio (runbook completo)
│       ├── README.md            # Como rodar, higiene de disco, limpar ambiente, Docker 29.x
│       ├── suite.ts + config.ts # Suíte única (5 cenários por taxa-alvo, ramping-arrival-rate)
│       ├── login|dashboard|ranking|write-flow|alunos-page.ts
│       ├── seed.sql             # 10k alunos determinísticos (BCrypt real p/ login)
│       └── run-loadtest.sh      # Wrapper: verificação VT, JFR, coleta de evidências
│
└── docs/
    └── arquitetura/
        ├── README.md            # Stub → registro vive no README raiz
        └── evidencias/desafio1/ # Evidências finais + pumls antes/depois (índice no README raiz)
```

---

## 🔧 Resolução de Problemas (Troubleshooting)

| Problema | Causa Provável | Solução |
| :--- | :--- | :--- |
| **Erro ao conectar no Postgres pelo IntelliJ na porta 5432** | O container expõe a porta `5433` no host, e não `5432`. | Mude a porta na tela de conexão do IntelliJ para **`5433`**. |
| **A API local não conecta ao banco de dados** | A variável `DATABASE_URL` não está configurada e tenta conectar em `postgres:5432`. | Configure a variável de ambiente: `DATABASE_URL=jdbc:postgresql://localhost:5433/devops`. |
| **Docker Compose falha com aviso sobre `.env.api` ou `.env.client`** | Os arquivos `.env.api` e `.env.client` ainda não foram criados. | Execute `cp .env.api.example .env.api` e `cp .env.client.example .env.client`. |
| **Porta 8080 ou 5173 já em uso** | Outro serviço ou aplicação local está rodando nestas portas. | Finalize o processo conflitante ou altere a porta mapeada no `docker-compose.yaml`. |
| **IntelliSense/imports em vermelho no IntelliJ** | O arquivo `api/pom.xml` não foi importado como projeto Maven. | Clique com o botão direito em `api/pom.xml` e selecione **Add as Maven Project** (ou **Maven ➔ Reload Project**). |
| **Resetar completamente o banco de dados** | Deseja apagar todas as tabelas e dados armazenados no volume Docker. | Execute `docker compose down -v` e em seguida `docker compose up -d`. |

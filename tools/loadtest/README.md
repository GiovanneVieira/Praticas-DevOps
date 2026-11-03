# Teste de Carga — Runbook

Cobertura de carga por módulo para o Desafio 1 (B1). Suíte única k6 (TypeScript, `ramping-arrival-rate`) contra a arquitetura declarada na API (Modulith + VT + pool 50). Orquestrado pelo wrapper; topologia no perfil `loadtest` do `docker-compose.yaml`.

## Rodar

```bash
cd tools/loadtest && ./run-loadtest.sh
```

~12 min no total: build+boot (~2min) → verificação de VT (jcmd) → seed 10k alunos → suíte (~7min: 5 cenários × 75s por taxa-alvo) → coleta + resumo.

**Durante a campanha**:
- Dashboard estável: **http://localhost:5665** (cada cenário com rótulo e timeline)
- Terminal: `[loadtest] >>> CENARIO CORRENTE: <nome>` a cada troca + barras de progresso do k6
- JFR de pinning gravado na JVM da aplicação (`pinning-eventos.txt` na coleta)

**Resultado** em `docs/arquitetura/evidencias/desafio1/final/`:
- `suite/dashboard.html` — relatório interativo (abre no navegador)
- `suite/metricas-por-cenario.json` + `resumo.json` — números que sustentam o registro arquitetural
- `vt-verificacao.txt`, `pinning-eventos.txt`, `ambiente.txt`

## Cuidados antes de rodar

| Cuidado | Por quê |
|---|---|
| **Máquina quieta** (dev compose desligado) | O wrapper aborta se a porta 8080 estiver ocupada; medição exige CPU livre |
| `docker compose down` (sem `-v`) desliga o dev preservando o banco | O projeto de carga é isolado (`-p pratica5-loadtest`) — o dev nunca é tocado |
| ~5GB livres em disco | Ver [Higiene de disco](#higiene-de-disco-arquivos-que-crescem-sem-aviso) abaixo |

## Higiene de disco (arquivos que crescem sem aviso)

- **`resultado.json` cresce para GBs** (pontos de métrica por requisição). Ele é **`.gitignore`d** — nunca o commite (GitHub bloqueia arquivos >100MB e o histórico inflacionou para 441MB antes do purge). **Após cada campanha**: extraia a análise e delete o bruto:
  ```bash
  # o wrapper já extrai metricas-por-cenario.json; depois:
  rm docs/arquitetura/evidencias/desafio1/final/suite/resultado.json
  ```
- **Build cache do Docker** cresce ~1-2GB por ciclo de builds. Periodicamente: `docker builder prune -af`
- Console do k6 (`k6-console.txt`) pode passar de dezenas de MB em cenários de falha — trunque mantendo o final (onde está o sumário): `tail -c 2000000 f > f.tmp && mv f.tmp f`

## Limpar o ambiente de carga (dropar o compose)

O comando exato para derrubar **tudo** do ambiente de carga (containers + rede + volume do Postgres de carga — **o banco do dev não é tocado**, o projeto é isolado por `-p`):

```bash
docker compose -p pratica5-loadtest down -v --remove-orphans
```

Se sobrar algum container k6 zumbi (bug de ressurreição do Docker 29.x — ver seção abaixo):

```bash
docker ps -a --filter name=loadtest -q | xargs -r docker rm -f
```

Se a **network** `pratica5-loadtest_default` reclamar *"Resource is still in use"* mesmo depois do `down` (container fantasma anexado invisivelmente — mesmo bug):

```bash
docker ps -aq --filter label=com.docker.compose.project=pratica5-loadtest | xargs -r docker rm -f
docker network rm pratica5-loadtest_default
```

> O wrapper já executa **todas** essas limpezas automaticamente no início e no fim de cada campanha — o manual é para o caso de uma execução morta no meio (Ctrl+C, kill etc.).

## Peculiaridades do Docker 29.x (defesas já embutidas no wrapper)

- **Containers fantasma**: binds de porta que "falham" deixam alocação fantasma e o daemon às vezes ressuscita o container minutos depois. Defesas: porta do dashboard **efêmera**, cenários via `compose run --rm` (um container por vez), limpeza pre-emptiva de zumbis no início. Se ainda assim um bind falhar com "port is already allocated": `docker ps -a | grep loadtest` → `docker rm -f` no culpado.
- **Network presa ("Resource is still in use")**: containers de `compose run` ressuscitados ficam anexados à network mesmo após o `down`. Defesa: o wrapper remove todos os containers do projeto (por label) e a network no início/fim. Manual: ver [Limpar o ambiente](#limpar-o-ambiente-de-carga-dropar-o-compose).
- **Race "No such container"** na criação em rajada: o wrapper tem `retry` curto (3×5s) nas operações de compose.

## Detalhes internos (por que o wrapper faz o que faz)

- **PID da JVM**: no target dev, `mvn spring-boot:run` faz fork — o PID 1 é o **launcher Maven**; a app roda como filho. Todo `jcmd` resolve o PID real via `jcmd -l | grep gamificacao` (a primeira geração de medições JFR mirava o Maven — bug corrigido).
- **Verificação de VT**: o dump do JDK 21.0.3 omite VTs de request; o sinal canônico é do Tomcat — executor clássico `http-nio-exec` **ausente** + `VirtualThread-unparker` **presente**.
- **Seed**: 10k alunos determinísticos (distribuição coerente com as regras AC1), hash BCrypt real de `123456` (o cenário `login` exercita a verificação criptográfica de verdade), idempotente (TRUNCATE antes dos INSERTs).
- **`grep -c` retorna exit 1 quando conta 0** — no bash com `set -e`, isso mata o script em silêncio justamente no resultado de sucesso. Todos os contadores usam `|| true`.

## Taxas-alvo e parâmetros

Centralizados em `config.ts` (`CENARIOS[]`): login 333/s (burst matinal) · dashboard 5.000 lotes/s · ranking 10.000/s · write-flow 300 it/s · alunos-page 10.000/s. Durações: rampa 20s → platô 45s → descida 10s por cenário. Iterações **dropadas** = taxa não sustentada = achado de capacidade.

## ⚠️ Re-executar APAGA as evidências anteriores

O wrapper limpa `docs/arquitetura/evidencias/desafio1/final/` no início de cada campanha (evidência fresca a cada run, bruto de GBs não acumula). **Antes de re-rodar**, garanta que a análise anterior (`metricas-por-cenario.json`) já foi extraída/commitada — ou arquive a pasta: `mv docs/arquitetura/evidencias/desafio1/final docs/arquitetura/evidencias/desafio1/final-anterior`.

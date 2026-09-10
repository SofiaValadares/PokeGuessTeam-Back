# PokeTeamGuess — Backend (Spring Boot)

API REST do **PokeTeamGuess**: dedução de equipas secretas de 6 Pokémon, inventário, gacha e partidas.

Frontend: repositório React `pokeguessteam` (WebStorm / Vercel).

---

## Stack

- Java 17 · Spring Boot 3 · Maven
- PostgreSQL + JPA
- Sessão HTTP (`JSESSIONID`) + session binding (`User-Agent` + IP)
- E-mail transacional via Resend

---

## Como rodar

```bash
docker compose up -d db
./mvnw spring-boot:run
```

API: `http://localhost:8080`

---

## Arquitetura

### Fluxo de pedido

```
HTTP Request
  → SecurityFilterChain (sessão, CORS)
  → SessionBindingInterceptor
  → Controller (validação @Valid)
  → Service (@Transactional)
  → Repository (JPA) ou FriendMatchStore (memória)
  → DTO de resposta
```

Erros de negócio: `ApiBusinessException` → `GlobalExceptionHandler` → JSON com `code` + mensagem em `messages.properties` (pt_BR).

### Pacotes (`com.svc.pokeguessteam`)

```
src/main/java/com/svc/pokeguessteam/
├── controller/          REST — um ficheiro por área (auth, profile, game, …)
├── service/             Lógica de negócio e orquestração
├── repository/          Spring Data JPA, agrupado por domínio
│   ├── auth/
│   ├── game/
│   ├── pokemon/
│   └── user/
├── model/               Entidades JPA (*Model) + enums
│   ├── auth/, game/, pokemon/, user/, enums/
├── dto/                 Contratos HTTP (records), por domínio
├── config/              Security, CORS, properties, seed Pokémon, migrations ad-hoc
├── security/            Session binding, entry point 401
├── exception/           ApiBusinessException, ErrorCodes, handler global
├── messages/            MessageKeys (chaves i18n)
└── util/                Motor de jogo puro (MatchEngine, BotAiOpponent, recompensas)
```

### Onde mexer (guia rápido)

| Quero alterar… | Ficheiro / pasta |
|----------------|------------------|
| Nova rota REST | `controller/*Controller.java` |
| Regra de negócio | `service/*Service.java` |
| Query à BD | `repository/**/*Repository.java` |
| Tabela / coluna | `model/**/*Model.java` + migration em `config/*Migration*.java` |
| Contrato JSON | `dto/**/*Dto.java` ou `*Request.java` |
| Código de erro | `exception/ErrorCodes.java` + `messages.properties` |
| Regras de turno / palpite | `util/MatchEngine.java` |
| Partida amigo (online) | `service/FriendMatchService.java`, `FriendMatchStore.java` |
| Partida bot/local | `service/BotMatchService.java`, `LocalMatchService.java` |
| Histórico / recompensas | `GameHistoryService.java`, `MatchRewardService.java` |
| Autenticação | `service/AuthService.java`, `config/SecurityConfig.java` |
| Seed Gen 1–9 | `config/seed/Generation*Seed.java` |
| CORS / env | `config/AppCorsProperties.java`, `.env` |

### Domínios

| Domínio | Serviços principais | Persistência |
|---------|---------------------|--------------|
| **Auth** | `AuthService`, `AuthCodeService`, `TransactionalEmailService` | `UserModel`, `AuthCodeModel` |
| **Perfil** | `ProfileService`, `UserPokedexService`, `PokeballDrawService` | `ProfileModel`, inventário, Pokédex |
| **Pokémon** | `PokedexService`, `NationalPokedexCatalog` | `PokemonModel`, seed |
| **Bot** | `BotMatchService` | `ActiveMatchModel` na BD (setup/validação) |
| **Local** | `LocalMatchService` | `ActiveMatchModel` na BD |
| **Amigo** | `FriendMatchService` | **`FriendMatchStore` em memória** (não sobrevive a restart) |
| **Histórico** | `GameHistoryService` | `HistoryGameModel` |

### Partidas — modelos mentais

| Modo | Onde corre o motor | Onde está o estado ativo |
|------|-------------------|-------------------------|
| **Bot** | Frontend | BD só valida equipa e regista fim |
| **Local** | Frontend | BD só valida setup e regista fim |
| **Amigo** | **Backend** (`MatchEngine` no servidor) | `FriendMatchStore` (RAM) |

**Amigo:** sem WebSocket/Socket.io. O cliente faz polling manual (`GET /match`) e timer local; ao expirar o tempo chama `POST /skip` (palpite aleatório).

**Uma partida por conta:** `ActiveMatchConstraintService` bloqueia nova partida se já existir amigo ativo ou registo órfão bot/local na BD.

### Testes

```
src/test/java/.../service/   fluxos amigo, constraints
src/test/java/.../util/      MatchEngine, recompensas, conhecimento adversário
```

---

## Autenticação

Todos os pedidos autenticados: `credentials: 'include'` (cookie `JSESSIONID`).

E-mails via **Resend** (`RESEND_API_KEY`). Sem chave, o código aparece no log.

### Rotas públicas

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/auth/register` | Cadastro |
| POST | `/auth/login` | Login (exige e-mail verificado) |
| POST | `/auth/email/verification/confirm` | Confirma e-mail + cria sessão |
| POST | `/auth/password-reset/request` | Pedir código |
| POST | `/auth/password-reset/confirm` | Nova senha |
| GET | `/auth/session` | Estado da sessão |
| GET | `/api/meta` | Metadados do jogo (público) |

### Rotas autenticadas (resumo)

| Área | Prefixo |
|------|---------|
| Utilizador | `GET /api/me` |
| Perfil | `/api/profile/*` |
| Pokémon / gacha | `/api/pokemon/*` |
| Pokédex nacional | `/api/pokedex` |
| Partidas | `/api/game/*` |

---

## Partidas — API

### Bot (motor no cliente)

| Método | Rota |
|--------|------|
| PUT | `/api/game/bot/match/team` |
| POST | `/api/game/bot/match/finish` |

### Local (motor no cliente)

| Método | Rota |
|--------|------|
| PUT | `/api/game/local/match/setup` |
| POST | `/api/game/local/match/finish` |

### Amigo (motor no servidor)

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/api/game/friend/match` | Criar sala (`joinCode`) |
| POST | `/api/game/friend/match/join` | Entrar com código |
| GET | `/api/game/friend/match` | Estado ativo (204 se nenhum) |
| PUT | `/api/game/friend/match/team` | Confirmar equipa |
| POST | `/api/game/friend/match/guess` | Palpite |
| POST | `/api/game/friend/match/skip` | Tempo esgotado — palpite aleatório |
| POST | `/api/game/friend/match/surrender` | Desistir |
| DELETE | `/api/game/friend/match` | Abandonar sala / limpar órfãos |
| GET | `/api/game/friend/match/opponent-knowledge` | Pistas do turno atual |

Conflito com partida ativa: `409 GAME_MATCH_ALREADY_IN_PROGRESS`.

**Timer:** 50s por turno no **frontend**; últimos 10s mostram contagem; ao zero o cliente chama `/skip`.

### Histórico

| GET | `/api/game/history?page=0&size=20` |

---

## Meta, perfil, Pokédex

| GET | `/api/meta` | Regras globais |
| GET | `/api/profile/me` | Perfil |
| GET | `/api/profile/training-team` | Equipa de treino |
| PUT | `/api/profile/training-team` | Atualizar equipa |
| GET | `/api/profile/pokemon` | PC |
| POST | `/api/pokemon/draw` | Gacha |
| GET | `/api/pokemon/search?q=` | Autocomplete palpites |

---

## Recompensas pós-partida

Valores em `GET /api/meta` → `matchRewards`.

**Bot / local:** WIN 150 XP + 5 fragmentos; outros 75 XP.

**Amigo:** WIN 300 XP; DRAW/LOSE/DESISTENCE 150 XP + 5 fragmentos.

---

## Postman

`postman/pokeguessteam-passwordless.postman_collection.json`

---

## Deploy (produção)

### Variáveis essenciais

| Variável | Descrição |
|----------|-----------|
| `SPRING_DATASOURCE_URL` | JDBC PostgreSQL |
| `SPRING_DATASOURCE_USERNAME` / `PASSWORD` | Credenciais BD |
| `AUTH_CODE_SECRET` | Segredo códigos e-mail |
| `RESEND_API_KEY` | E-mail |
| `SESSION_COOKIE_SECURE` | `true` em HTTPS |
| `APP_CORS_ALLOWED_ORIGIN_PATTERNS` | URL do frontend |
| `DEV_TOOLS_ENABLED` | `false` em produção |

### Render

1. Web Service com Docker (`render.yaml` ou Dockerfile)
2. Health check: `GET /api/meta`
3. **Nota:** partidas amigo em memória perdem-se se o serviço reiniciar (plano free do Render).

```bash
./mvnw clean package -DskipTests
java -jar target/pokeguessteam-0.0.1-SNAPSHOT.jar
```

---

## Integração com o frontend React

1. `credentials: 'include'` em todos os pedidos.
2. **Bot/local:** motor em `lib/game/`; chamar setup/team + finish.
3. **Amigo:** estado via `FriendMatchProvider`; sincronização manual; timer + `/skip`.
4. Pesquisa de palpites: espécies do cache Redux ou `GET /api/pokemon/search`.

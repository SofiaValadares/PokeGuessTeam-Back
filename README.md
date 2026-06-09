# PokeTeamGuess — Backend (Spring Boot)

API REST do **PokeTeamGuess** ([GDD](https://github.com/SofiaValadares/PokeGuessTeam)): dedução de equipas secretas de 6 Pokémon, inventário, gacha e partidas (motor no **cliente** para bot/local; no **servidor** para amigo online).

Frontend de referência (AV1): [PokeGuessTeam](https://github.com/SofiaValadares/PokeGuessTeam) — [produção](https://poke-guess-team.vercel.app/).

## Stack

- Java 17, Spring Boot 3, Maven
- PostgreSQL + JPA
- Socket.io (porta `9092`, partidas amigo)
- Sessão HTTP (`JSESSIONID`) + session binding (`User-Agent` + IP)
- CORS para dev: `localhost:5173`, `3000`, `5500`

## Como rodar

```bash
docker compose up -d db
./mvn spring-boot:run
```

API: `http://localhost:8080`

## Autenticação

Todos os pedidos à API devem usar `credentials: 'include'` (cookie `JSESSIONID` + session binding por `User-Agent`).

E-mails transacionais via **Resend** (`RESEND_API_KEY` no `.env`). Sem chave configurada, o código aparece no **log do servidor**.

### Fluxos suportados

| Fluxo | Passos | Sessão ao final |
|-------|--------|-----------------|
| **Cadastro** | register → confirmar e-mail | Sim (confirm cria sessão) |
| **Login sem e-mail verificado** | login (`403`) → confirmar e-mail | Sim (confirm cria sessão) |
| **Login verificado** | login | Sim |
| **Troca de senha** | pedir código → confirmar nova senha | Não → redirecionar para login |

`POST /auth/login` e `POST /auth/email/verification/confirm` devolvem `AuthSessionResponse`:

```json
{
  "userId": "...",
  "email": "...",
  "username": "...",
  "emailVerified": true,
  "message": "..." 
}
```

(`message` só vem preenchido na confirmação de e-mail; no login é `null`.)

Login sem e-mail verificado → **403** `AUTH_EMAIL_NOT_VERIFIED`.

Prompt para implementação no frontend: `docs/frontend-auth-flows-prompt.md`.

### Rotas públicas (sem cookie inicial)

| Método | Rota | Body | Descrição |
|--------|------|------|-----------|
| POST | `/auth/register` | `{ username, email, password }` | Cadastro; envia código de verificação |
| POST | `/auth/email/verification/send` | `{ email }` | Reenvia código (cooldown 60s) |
| POST | `/auth/email/verification/confirm` | `{ email, code }` | Confirma e-mail e **cria sessão** |
| POST | `/auth/verification/resend` | `{ email }` | Alias de `.../send` |
| POST | `/auth/verification/confirm` | `{ email, code }` | Alias de `.../confirm` |
| POST | `/auth/password-reset/request` | `{ email }` | Pedir código de redefinição (só se e-mail já verificado) |
| POST | `/auth/password-reset/confirm` | `{ email, code, newPassword }` | Redefinir senha (**sem** sessão) |
| POST | `/auth/login` | `{ login, password }` | Login; exige e-mail verificado |
| GET | `/auth/session` | — | Estado da sessão (`authenticated`, `emailVerified`) |
| POST | `/auth/logout` | — | Logout |

### Rotas autenticadas (sessão + session binding)

| Método | Rota | Body | Descrição |
|--------|------|------|-----------|
| PATCH | `/auth/password` | `{ currentPassword, newPassword }` | Trocar senha |
| PATCH | `/auth/username` | `{ newUsername, password }` | Trocar username |
| GET | `/api/me` | — | Utilizador autenticado |

## Meta e Pokédex

| GET | `/api/meta` | Regras globais (tamanho de equipa, modos, resultados) — **público** |
| GET | `/api/pokedex` | Pokédex nacional (paginada ou completa) |
| GET | `/api/pokemon/search?q=` | Autocomplete de espécies para palpites |
| GET | `/api/pokemon/species/{dex}` | Detalhe de uma espécie |

## Perfil e progressão

| GET | `/api/profile/me` | Perfil |
| GET | `/api/profile/training-team` | Time de treino (6 linhas evolutivas do PC) |
| PUT ou POST | `/api/profile/training-team` | Atualizar time (`slots`: 6 `evolutionLineKey` ou `null`) |
| GET | `/api/profile/pokemon` | PC / inventário por linha evolutiva |
| GET | `/api/profile/collection` | Pokébolas e fragmentos |
| POST | `/api/pokemon/draw` | Gacha (consome Pokébola) |

## Partidas

Alinhado ao GDD e à [beta](https://poke-guess-team.vercel.app/): turnos, pistas (tipo, geração, cor, altura, peso), jogada extra, rodada de empate, histórico automático e **recompensas** (XP no time de treino + fragmentos de Pokébola).

### Arquitetura

| Modo | Motor | API |
|------|-------|-----|
| **Bot** | Cliente (frontend) | Valida equipa + regista resultado |
| **Local** | Cliente (frontend) | Valida setup + regista resultado |
| **Amigo** | Servidor | Fluxo completo + Socket.io |

Socket.io (`http://localhost:9092`, evento `match:event`) **só no modo amigo**. Cookie `JSESSIONID` na handshake.

### Bot (client-side)

| Método | Rota | Body |
|--------|------|------|
| PUT | `/api/game/bot/match/team` | `{ "team": [6 dex] }` → `{ hostTeam, opponentTeam }` |
| POST | `/api/game/bot/match/finish` | `{ userCorrectGuesses, opponentCorrectGuesses, result }` → `{ historyEntry, reward }` |

Requer ≥12 espécies registadas no PC. Limpa partidas bot antigas em `TB_ACTIVE_MATCH` ao validar ou terminar.

### Local (client-side, pass-and-play)

| Método | Rota | Body |
|--------|------|------|
| PUT | `/api/game/local/match/setup` | `{ opponentName, hostTeam, opponentTeam }` → `204` |
| POST | `/api/game/local/match/finish` | `{ opponentName, userCorrectGuesses, opponentCorrectGuesses, result }` |

### Amigo remoto (motor no servidor)

**Uma partida por conta:** com partida amigo não terminada (`SETUP` ou `ACTIVE`), não é possível iniciar outra até vitória, derrota, empate ou desistência (`409 GAME_MATCH_ALREADY_IN_PROGRESS`).

| Método | Rota |
|--------|------|
| POST | `/api/game/friend/match` — gera `joinCode` (6 caracteres) |
| POST | `/api/game/friend/match/join` — `{ "joinCode": "ABC123" }` |
| GET | `/api/game/friend/match` |
| PUT | `/api/game/friend/match/team` |
| POST | `/api/game/friend/match/guess` |
| POST | `/api/game/friend/match/surrender` |
| GET | `/api/game/friend/match/opponent-knowledge` |

### Socket.io — amigo

- Conectar: `http://localhost:9092` (porta `SOCKETIO_PORT`, default `9092`) com cookie `JSESSIONID`
- Entrar na sala: emit `match:join` `{ "mode": "friend", "matchId": "..." }` → room `match:friend:{matchId}:user:{userId}`
- Palpite: emit `match:friend:guess` `{ "pokedexNumber": 25 }` (alternativa ao HTTP)
- Sair: emit `match:leave` ou desconectar

Eventos recebidos (`match:event`, campo `type`):

| Tipo | Uso |
|------|-----|
| `PLAYER_GUESS` | Palpite + estado atualizado |
| `MATCH_STATE` / `MATCH_FINISHED` | Estado ou fim da partida |
| `TURN_TIMER` | Prazo de 50s |
| `TIMEOUT_PENALTY` | Palpite automático + penalidade |
| `OPPONENT_REPLACED_BY_BOT` | 3 penalidades → adversário vira bot |

**Amigo:** 50s por turno; timeout = palpite aleatório + penalidade (`turnTimeoutPenalties` no histórico). 3 penalidades na mesma partida = desistência desse jogador; o outro termina vs IA. 5 penalidades na última hora = ban de 3 dias do modo amigo (`403 GAME_FRIEND_ONLINE_BANNED`). Penalidades ficam no perfil por 7 dias (`TB_FRIEND_ONLINE_PENALTIES`).

### Histórico

| GET | `/api/game/history?page=0&size=20` |

### Utilitários

| GET | `/api/users/search?q=` | Pesquisar treinadores (mín. 2 caracteres) |

## Recompensas pós-partida (GDD)

Valores em `GET /api/meta` → `matchRewards`.

**Bot / local:**

| Resultado | XP (time de treino) | Fragmentos |
|-----------|---------------------|------------|
| WIN | 150 | 5 |
| DRAW, LOSE, DESISTENCE | 75 | 0 |

**Amigo** (cada jogador na sua perspetiva):

| Resultado | XP | Fragmentos |
|-----------|-----|------------|
| WIN | 300 | 0 |
| DRAW, LOSE, DESISTENCE | 150 | 5 |

## Postman

`postman/pokeguessteam-passwordless.postman_collection.json` — fluxos Auth, Bot, Local, Friend e histórico.

## Integração React (AV2)

1. `credentials: 'include'` em todos os pedidos à API.
2. **Bot/local:** motor no cliente; chamar `PUT .../team` ou `PUT .../setup` antes de jogar e `POST .../finish` ao terminar.
3. **Amigo:** polling ou Socket.io em `GET .../match` / eventos `match:event`.
4. Usar `GET /api/pokemon/search` no campo de palpite.

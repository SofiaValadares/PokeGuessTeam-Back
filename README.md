# PokeTeamGuess — Backend (Spring Boot)

API REST do **PokeTeamGuess** ([GDD](https://github.com/SofiaValadares/PokeGuessTeam)): dedução de equipas secretas de 6 Pokémon, inventário, gacha e partidas com **regras no servidor** (requisito AV2 de Frontend).

Frontend de referência (AV1): [PokeGuessTeam](https://github.com/SofiaValadares/PokeGuessTeam) — [produção](https://poke-guess-team.vercel.app/).

## Stack

- Java 17, Spring Boot 3, Maven
- PostgreSQL + JPA
- Sessão HTTP (`JSESSIONID`) + session binding (`User-Agent` + IP)
- CORS para dev: `localhost:5173`, `3000`, `5500`

## Como rodar

```bash
docker compose up -d db
./mvnw spring-boot:run
```

API: `http://localhost:8080`

## Autenticação

| Método | Rota | Descrição |
|--------|------|-----------|
| POST | `/auth/register` | Cadastro |
| POST | `/auth/login` | Login (cookie de sessão) |
| GET | `/auth/session` | Sessão atual |
| POST | `/auth/logout` | Logout |
| GET | `/api/me` | Utilizador autenticado |

## Meta e Pokédex

| GET | `/api/meta` | Regras globais (tamanho de equipa, modos, resultados) — **público** |
| GET | `/api/pokedex` | Pokédex nacional (paginada ou completa) |
| GET | `/api/pokemon/search?q=` | Autocomplete de espécies para palpites |
| GET | `/api/pokemon/species/{dex}` | Detalhe de uma espécie |

## Perfil e progressão

| GET | `/api/profile/me` | Perfil |
| GET | `/api/profile/training-team` | Time de treino (6 slots, foco de XP) |
| GET | `/api/profile/pokemon` | PC / inventário por linha evolutiva |
| GET | `/api/profile/collection` | Pokébolas e fragmentos |
| POST | `/api/pokemon/draw` | Gacha (consome Pokébola) |

## Partidas (motor no servidor)

Alinhado ao GDD e à [beta](https://poke-guess-team.vercel.app/): turnos, pistas (tipo, geração, cor, altura, peso), jogada extra, rodada de empate, histórico automático e **recompensas** (XP no time de treino + Pokébolas).

### Bot

| Método | Rota |
|--------|------|
| POST | `/api/game/bot/match` |
| GET | `/api/game/bot/match` |
| PUT | `/api/game/bot/match/team` |
| POST | `/api/game/bot/match/guess` |
| POST | `/api/game/bot/match/surrender` |
| DELETE | `/api/game/bot/match` |

### Local (pass-and-play)

| Método | Rota |
|--------|------|
| POST | `/api/game/local/match` — body: `{ "opponentName": "Ash" }` |
| PUT | `/api/game/local/match/team` — `{ "playerSide": "USER"|"BOT", "team": [6 dex] }` |
| POST | `/api/game/local/match/guess` |
| POST | `/api/game/local/match/surrender` |

### Amigo remoto

| Método | Rota |
|--------|------|
| POST | `/api/game/friend/match` — gera `joinCode` (6 caracteres) |
| POST | `/api/game/friend/match/join` — `{ "joinCode": "ABC123" }` |
| PUT | `/api/game/friend/match/team` |
| POST | `/api/game/friend/match/guess` |

### Histórico

| GET | `/api/game/history?page=0&size=20` |

### Utilitários

| GET | `/api/users/search?q=` | Pesquisar treinadores (mín. 2 caracteres) |

## Recompensas pós-partida (GDD)

Após terminar uma partida ativa:

| Resultado | XP (time de treino) | Pokébolas |
|-----------|---------------------|-----------|
| WIN | 40 | 2 |
| DRAW | 20 | 1 |
| LOSE | 12 | 0 |
| DESISTENCE | 5 | 0 |

Modo amigo: **ambos** os jogadores recebem recompensas na sua perspetiva.

## Postman

`postman/pokeguessteam-passwordless.postman_collection.json` — fluxos Auth, Bot, Local, Friend e histórico.

## Integração React (AV2)

1. `credentials: 'include'` em todos os pedidos à API.
2. Substituir `localStorage` / `MatchState` do cliente pelos endpoints `/api/game/*/match`.
3. Polling ou refresh em `GET .../match` no modo amigo enquanto espera o adversário.
4. Usar `GET /api/pokemon/search` no campo de palpite.

## Endpoints legados (finish manual)

Ainda disponíveis para migração gradual: `POST /api/game/local`, `/bot`, `/friend` (cliente envia placar). Preferir os fluxos `/match` acima.

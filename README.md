# Pokeguessteam - Login Tradicional com Session Binding

Projeto Spring Boot 3 (Java 17 + Maven) com autenticacao tradicional por e-mail/senha.

## Recursos

- Cadastro de usuario com hash de senha (`BCrypt`)
- Login stateful com `HttpSession` (`JSESSIONID`)
- Cookie com `HttpOnly` e `SameSite=Lax`
- Session binding por dispositivo (`User-Agent + IP`)
- Persistencia com PostgreSQL (Spring Data JPA)
- Pokedex seedada para testes

## Como rodar

1. Suba o banco:

```bash
docker compose up -d db
```

2. Suba a API:

```bash
./mvnw spring-boot:run
```

API: `http://localhost:8080`

## Endpoints

### `POST /auth/register`

```json
{
  "username": "user",
  "email": "user@example.com",
  "password": "123456"
}
```

### `POST /auth/login`

```json
{
  "email": "user@example.com",
  "password": "123456"
}
```

### `GET /api/me`

Rota protegida por sessão e session binding.

Resposta JSON (ordem estável):

| Campo | Significado |
|-------|-------------|
| `authenticatedAs` | Principal Spring Security (no login atual = **e-mail**) — mantido por compatibilidade |
| `userId` | ID do utilizador na sessão |
| `username` | Nome de utilizador (`TB_USERS`) para UI (“Conta”) |
| `email` | E-mail do utilizador |

Se o utilizador não existir na base de dados: **404** (`PROFILE_USER_NOT_FOUND`).

### `GET /api/pokedex`

Lista os Pokémon da Pokédex (número nacional).

### `GET /api/profile/me`

Retorna `profileId`, `userId`, favorito (`favoritePokemonId` / `favoritePokemonName`).

### `GET /api/profile/collection`

Retorna o inventário por linha evolutiva (`evolutionLineKey`, `members`, `rarity`, `level`, `totalXp`, `timesObtained`).

## Session Binding

Ao logar, a API grava na sessao um `DEVICE_ID` com hash de `User-Agent + IP`.
Em toda chamada de `/api/**`, o interceptor compara o `DEVICE_ID` atual com o da sessao.
Se divergir, a sessao e invalidada e a API responde `401`.

## Colecao Postman/Insomnia

Arquivo:

- `postman/pokeguessteam-passwordless.postman_collection.json`

Ordem de execucao (minimo):

1. `Auth - Register`
2. `Auth - Login`
3. `API - Me (rota protegida)` (esperado `200`)
4. `API - Me (simula outro dispositivo)` (esperado `401`)

Opcional: `Profile - Me` e `Profile - Collection` após o login.

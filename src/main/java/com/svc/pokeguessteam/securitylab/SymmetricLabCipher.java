package com.svc.pokeguessteam.securitylab;

import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Cipher didático (Unidade 2): XOR + quatro rodadas de substituição e difusão em blocos.
 * A estrutura inspira uma rede de substituição-permutação, mas não é AES nem cifra segura.
 * Não substitui AES-GCM de produção.
 */
@Component
public class SymmetricLabCipher {

    public static final int BLOCK_SIZE = 16;
    public static final int KEY_BYTES = 32;
    private static final int DIFFUSION_ROUNDS = 4;

    private final SecureRandom secureRandom = new SecureRandom();

    public record KeyMaterial(byte[] key, byte[] nonce, String fingerprint) {
        public String keyBase64() {
            return Base64.getEncoder().encodeToString(key);
        }

        public String nonceBase64() {
            return Base64.getEncoder().encodeToString(nonce);
        }
    }

    public record CipherResult(
            String ciphertextBase64,
            KeyMaterial keyMaterial,
            int blockCount,
            String pipeline
    ) {
    }

    public KeyMaterial deriveKey(String seed) {
        byte[] nonce = new byte[16];
        secureRandom.nextBytes(nonce);
        return materialFromSeedAndNonce(seed, nonce);
    }

    public KeyMaterial restoreKey(String seed, String nonceBase64) {
        byte[] nonce = Base64.getDecoder().decode(nonceBase64);
        return materialFromSeedAndNonce(seed, nonce);
    }

    private KeyMaterial materialFromSeedAndNonce(String seed, byte[] nonce) {
        String effectiveSeed = seed == null ? "" : seed.trim();
        String nonceB64 = Base64.getEncoder().encodeToString(nonce);
        byte[] key = sha256((effectiveSeed + "|" + nonceB64).getBytes(StandardCharsets.UTF_8));
        return new KeyMaterial(key, Arrays.copyOf(nonce, nonce.length),
                HexFormat.of().formatHex(Arrays.copyOf(key, 8)));
    }

    public CipherResult encrypt(String plaintext, String seed) {
        KeyMaterial km = deriveKey(seed);
        byte[] cipher = encryptBytes(plaintext.getBytes(StandardCharsets.UTF_8), km.key());
        return new CipherResult(
                Base64.getEncoder().encodeToString(cipher),
                km,
                cipher.length / BLOCK_SIZE,
                "XOR -> 4 rodadas de S-box/ShiftRows/MixColumns/subchave / blocos de 16 (didático)"
        );
    }

    public String decrypt(String ciphertextBase64, KeyMaterial km) {
        byte[] plain = decryptBytes(Base64.getDecoder().decode(ciphertextBase64), km.key());
        return new String(plain, StandardCharsets.UTF_8);
    }

    public byte[] encryptBytes(byte[] plain, byte[] key) {
        byte[] padded = pkcs7Pad(plain);
        byte[] sbox = buildSBox(key);
        byte[][] roundKeys = buildRoundKeys(key);
        byte[] keystream = expandKeystream(key, padded.length);
        byte[] out = new byte[padded.length];

        for (int offset = 0; offset < padded.length; offset += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(padded, offset, offset + BLOCK_SIZE);
            for (int i = 0; i < BLOCK_SIZE; i++) {
                block[i] ^= keystream[offset + i];
            }
            for (int round = 0; round < DIFFUSION_ROUNDS; round++) {
                substituteBytes(block, sbox);
                block = shiftRows(block, false);
                mixColumns(block, false);
                xorRoundKey(block, roundKeys[round]);
            }
            System.arraycopy(block, 0, out, offset, BLOCK_SIZE);
        }
        return out;
    }

    public byte[] decryptBytes(byte[] cipher, byte[] key) {
        if (cipher.length == 0 || cipher.length % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("Ciphertext com tamanho inválido");
        }
        byte[] sbox = buildSBox(key);
        byte[] invSbox = invertSBox(sbox);
        byte[][] roundKeys = buildRoundKeys(key);
        byte[] keystream = expandKeystream(key, cipher.length);
        byte[] out = new byte[cipher.length];

        for (int offset = 0; offset < cipher.length; offset += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(cipher, offset, offset + BLOCK_SIZE);
            for (int round = DIFFUSION_ROUNDS - 1; round >= 0; round--) {
                xorRoundKey(block, roundKeys[round]);
                mixColumns(block, true);
                block = shiftRows(block, true);
                substituteBytes(block, invSbox);
            }
            for (int i = 0; i < BLOCK_SIZE; i++) {
                block[i] ^= keystream[offset + i];
            }
            System.arraycopy(block, 0, out, offset, BLOCK_SIZE);
        }
        return pkcs7Unpad(out);
    }

    private static void substituteBytes(byte[] state, byte[] sbox) {
        for (int i = 0; i < state.length; i++) {
            state[i] = sbox[state[i] & 0xFF];
        }
    }

    /** Rotaciona cada linha do estado 4x4; o sentido inverso desfaz a transformação. */
    private static byte[] shiftRows(byte[] state, boolean inverse) {
        byte[] shifted = new byte[BLOCK_SIZE];
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                int sourceColumn = inverse
                        ? (column - row + 4) % 4
                        : (column + row) % 4;
                shifted[row + 4 * column] = state[row + 4 * sourceColumn];
            }
        }
        return shifted;
    }

    /** Mistura os quatro bytes de cada coluna sobre GF(2^8), como etapa didática de difusão. */
    private static void mixColumns(byte[] state, boolean inverse) {
        for (int column = 0; column < 4; column++) {
            int offset = column * 4;
            int a0 = state[offset] & 0xFF;
            int a1 = state[offset + 1] & 0xFF;
            int a2 = state[offset + 2] & 0xFF;
            int a3 = state[offset + 3] & 0xFF;

            if (inverse) {
                state[offset] = (byte) (gfMultiply(a0, 14) ^ gfMultiply(a1, 11)
                        ^ gfMultiply(a2, 13) ^ gfMultiply(a3, 9));
                state[offset + 1] = (byte) (gfMultiply(a0, 9) ^ gfMultiply(a1, 14)
                        ^ gfMultiply(a2, 11) ^ gfMultiply(a3, 13));
                state[offset + 2] = (byte) (gfMultiply(a0, 13) ^ gfMultiply(a1, 9)
                        ^ gfMultiply(a2, 14) ^ gfMultiply(a3, 11));
                state[offset + 3] = (byte) (gfMultiply(a0, 11) ^ gfMultiply(a1, 13)
                        ^ gfMultiply(a2, 9) ^ gfMultiply(a3, 14));
            } else {
                state[offset] = (byte) (gfMultiply(a0, 2) ^ gfMultiply(a1, 3) ^ a2 ^ a3);
                state[offset + 1] = (byte) (a0 ^ gfMultiply(a1, 2) ^ gfMultiply(a2, 3) ^ a3);
                state[offset + 2] = (byte) (a0 ^ a1 ^ gfMultiply(a2, 2) ^ gfMultiply(a3, 3));
                state[offset + 3] = (byte) (gfMultiply(a0, 3) ^ a1 ^ a2 ^ gfMultiply(a3, 2));
            }
        }
    }

    private static int gfMultiply(int value, int factor) {
        int product = 0;
        int multiplicand = value;
        int multiplier = factor;
        for (int bit = 0; bit < 8; bit++) {
            if ((multiplier & 1) != 0) product ^= multiplicand;
            boolean highBit = (multiplicand & 0x80) != 0;
            multiplicand = (multiplicand << 1) & 0xFF;
            if (highBit) multiplicand ^= 0x1B;
            multiplier >>>= 1;
        }
        return product & 0xFF;
    }

    private static void xorRoundKey(byte[] state, byte[] roundKey) {
        for (int i = 0; i < BLOCK_SIZE; i++) {
            state[i] ^= roundKey[i];
        }
    }

    private static byte[][] buildRoundKeys(byte[] key) {
        byte[][] roundKeys = new byte[DIFFUSION_ROUNDS][BLOCK_SIZE];
        byte[] domain = "lab-diffusion-round".getBytes(StandardCharsets.US_ASCII);
        for (int round = 0; round < DIFFUSION_ROUNDS; round++) {
            ByteBuffer material = ByteBuffer.allocate(key.length + domain.length + Integer.BYTES)
                    .put(key)
                    .put(domain)
                    .putInt(round);
            roundKeys[round] = Arrays.copyOf(sha256(material.array()), BLOCK_SIZE);
        }
        return roundKeys;
    }

    private static byte[] pkcs7Pad(byte[] input) {
        int pad = BLOCK_SIZE - (input.length % BLOCK_SIZE);
        if (pad == 0) pad = BLOCK_SIZE;
        byte[] out = Arrays.copyOf(input, input.length + pad);
        Arrays.fill(out, input.length, out.length, (byte) pad);
        return out;
    }

    private static byte[] pkcs7Unpad(byte[] input) {
        if (input.length == 0 || input.length % BLOCK_SIZE != 0) {
            throw new IllegalArgumentException("Padding inválido — chave ou ciphertext incorretos");
        }
        int pad = input[input.length - 1] & 0xFF;
        if (pad < 1 || pad > BLOCK_SIZE) {
            throw new IllegalArgumentException("Padding inválido — chave ou ciphertext incorretos");
        }
        for (int i = input.length - pad; i < input.length; i++) {
            if ((input[i] & 0xFF) != pad) {
                throw new IllegalArgumentException("Padding inválido — chave ou ciphertext incorretos");
            }
        }
        return Arrays.copyOf(input, input.length - pad);
    }

    private static byte[] buildSBox(byte[] key) {
        byte[] box = new byte[256];
        for (int i = 0; i < 256; i++) box[i] = (byte) i;
        int j = 0;
        for (int i = 0; i < 256; i++) {
            j = (j + (box[i] & 0xFF) + (key[i % key.length] & 0xFF)) & 0xFF;
            byte t = box[i];
            box[i] = box[j];
            box[j] = t;
        }
        return box;
    }

    private static byte[] invertSBox(byte[] sbox) {
        byte[] inv = new byte[256];
        for (int i = 0; i < 256; i++) {
            inv[sbox[i] & 0xFF] = (byte) i;
        }
        return inv;
    }

    private static byte[] expandKeystream(byte[] key, int length) {
        byte[] out = new byte[length];
        int produced = 0;
        int counter = 0;
        while (produced < length) {
            byte[] block = sha256(ByteBuffer.allocate(key.length + 4)
                    .put(key)
                    .putInt(counter++)
                    .array());
            int copy = Math.min(block.length, length - produced);
            System.arraycopy(block, 0, out, produced, copy);
            produced += copy;
        }
        return out;
    }

    private static byte[] sha256(byte[] data) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(data);
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }

    public static int hammingBits(byte[] a, byte[] b) {
        int n = Math.min(a.length, b.length);
        int bits = 0;
        for (int i = 0; i < n; i++) {
            bits += Integer.bitCount((a[i] ^ b[i]) & 0xFF);
        }
        bits += Math.abs(a.length - b.length) * 8;
        return bits;
    }
}

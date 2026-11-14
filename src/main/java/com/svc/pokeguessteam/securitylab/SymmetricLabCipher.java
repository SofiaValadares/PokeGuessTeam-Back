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
 * Cipher didático (Unidade 2): chave dinâmica + XOR + substituição + permutação em blocos.
 * Não substitui AES-GCM de produção.
 */
@Component
public class SymmetricLabCipher {

    public static final int BLOCK_SIZE = 16;
    public static final int KEY_BYTES = 32;

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
                "XOR -> Substituição(S-box) -> Permutação(P-box) / blocos de 16"
        );
    }

    public String decrypt(String ciphertextBase64, KeyMaterial km) {
        byte[] plain = decryptBytes(Base64.getDecoder().decode(ciphertextBase64), km.key());
        return new String(plain, StandardCharsets.UTF_8);
    }

    public byte[] encryptBytes(byte[] plain, byte[] key) {
        byte[] padded = pkcs7Pad(plain);
        byte[] sbox = buildSBox(key);
        int[] pbox = buildPBox(key);
        byte[] keystream = expandKeystream(key, padded.length);
        byte[] out = new byte[padded.length];

        for (int offset = 0; offset < padded.length; offset += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(padded, offset, offset + BLOCK_SIZE);
            for (int i = 0; i < BLOCK_SIZE; i++) {
                block[i] ^= keystream[offset + i];
            }
            for (int i = 0; i < BLOCK_SIZE; i++) {
                block[i] = sbox[block[i] & 0xFF];
            }
            block = applyPerm(block, pbox);
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
        int[] pbox = buildPBox(key);
        int[] invPbox = invertPBox(pbox);
        byte[] keystream = expandKeystream(key, cipher.length);
        byte[] out = new byte[cipher.length];

        for (int offset = 0; offset < cipher.length; offset += BLOCK_SIZE) {
            byte[] block = Arrays.copyOfRange(cipher, offset, offset + BLOCK_SIZE);
            block = applyPerm(block, invPbox);
            for (int i = 0; i < BLOCK_SIZE; i++) {
                block[i] = invSbox[block[i] & 0xFF];
            }
            for (int i = 0; i < BLOCK_SIZE; i++) {
                block[i] ^= keystream[offset + i];
            }
            System.arraycopy(block, 0, out, offset, BLOCK_SIZE);
        }
        return pkcs7Unpad(out);
    }

    /** out[i] = in[perm[i]] */
    private static byte[] applyPerm(byte[] in, int[] perm) {
        byte[] out = new byte[in.length];
        for (int i = 0; i < in.length; i++) {
            out[i] = in[perm[i]];
        }
        return out;
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

    private static int[] buildPBox(byte[] key) {
        int[] box = new int[BLOCK_SIZE];
        for (int i = 0; i < BLOCK_SIZE; i++) box[i] = i;
        // Fisher–Yates
        for (int i = BLOCK_SIZE - 1; i > 0; i--) {
            int j = (key[i % key.length] & 0xFF) % (i + 1);
            // misturar com mais bytes da chave
            j = (j + (key[(i * 3) % key.length] & 0xFF)) % (i + 1);
            int t = box[i];
            box[i] = box[j];
            box[j] = t;
        }
        return box;
    }

    private static int[] invertPBox(int[] pbox) {
        int[] inv = new int[pbox.length];
        for (int i = 0; i < pbox.length; i++) {
            inv[pbox[i]] = i;
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

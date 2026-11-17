package br.edu.unicid.saudeconecta.utils;

import android.util.Base64;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/** Hash de senha com PBKDF2-HMAC-SHA256 + salt aleatório (a senha nunca é salva em texto puro). */
public final class PasswordUtils {
    private static final int ITERACOES = 15000;
    private static final int TAMANHO_CHAVE = 256;
    private static final String ALGORITMO = "PBKDF2WithHmacSHA256";

    private PasswordUtils() { }

    public static String gerarSalt() {
        byte[] salt = new byte[16];
        new SecureRandom().nextBytes(salt);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }

    public static String gerarHash(String senha, String saltBase64) {
        PBEKeySpec spec = new PBEKeySpec(senha.toCharArray(),
                Base64.decode(saltBase64, Base64.NO_WRAP), ITERACOES, TAMANHO_CHAVE);
        try {
            byte[] hash = SecretKeyFactory.getInstance(ALGORITMO).generateSecret(spec).getEncoded();
            return Base64.encodeToString(hash, Base64.NO_WRAP);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Falha ao gerar hash de senha", e);
        } finally {
            spec.clearPassword();
        }
    }

    public static boolean conferir(String senha, String saltBase64, String hashEsperado) {
        String hash = gerarHash(senha, saltBase64);
        return MessageDigest.isEqual(hash.getBytes(StandardCharsets.UTF_8),
                hashEsperado.getBytes(StandardCharsets.UTF_8));
    }
}

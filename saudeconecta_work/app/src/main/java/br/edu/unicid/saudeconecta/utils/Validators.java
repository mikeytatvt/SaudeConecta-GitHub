package br.edu.unicid.saudeconecta.utils;

import android.util.Patterns;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.format.ResolverStyle;

/** Validações dos formulários. */
public final class Validators {
    private static final DateTimeFormatter DATA_BR =
            DateTimeFormatter.ofPattern("dd/MM/uuuu").withResolverStyle(ResolverStyle.STRICT);

    private Validators() { }

    public static String somenteDigitos(String texto) {
        return texto == null ? "" : texto.replaceAll("\\D", "");
    }

    /** Valida CPF pelos dígitos verificadores. */
    public static boolean isCpfValido(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11 || d.matches("(\\d)\\1{10}")) return false;
        int[] n = new int[11];
        for (int i = 0; i < 11; i++) n[i] = d.charAt(i) - '0';
        return calcularDigitoCpf(n, 9) == n[9] && calcularDigitoCpf(n, 10) == n[10];
    }

    /** Calcula o dígito verificador usando as "tamanho" primeiras posições. */
    public static int calcularDigitoCpf(int[] digitos, int tamanho) {
        int soma = 0;
        for (int i = 0; i < tamanho; i++) soma += digitos[i] * ((tamanho + 1) - i);
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /** LGPD: 11144477735 -> ***.444.777-** (exibição em listas e documentos). */
    public static String mascararCpf(String cpf) {
        String d = somenteDigitos(cpf);
        if (d.length() != 11) return "***";
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }

    public static boolean isEmailValido(String email) {
        return email != null && email.length() <= 120
                && Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches();
    }

    /** Aceita fixo (10 dígitos) ou celular (11 dígitos começando com 9). */
    public static boolean isTelefoneValido(String telefone) {
        String d = somenteDigitos(telefone);
        if (d.length() != 10 && d.length() != 11) return false;
        if (d.charAt(0) == '0') return false;
        return d.length() == 10 || d.charAt(2) == '9';
    }

    /** Nome e sobrenome, somente letras, espaços, hífen e apóstrofo. */
    public static boolean isNomeCompletoValido(String nome) {
        if (nome == null) return false;
        String n = nome.trim().replaceAll("\\s+", " ");
        if (n.length() < 5 || n.length() > 120) return false;
        if (!n.matches("[\\p{L}' .-]+")) return false;
        int partes = 0;
        for (String p : n.split(" ")) if (p.length() >= 2) partes++;
        return partes >= 2;
    }

    /** Mínimo 8 caracteres, com pelo menos uma letra e um número. */
    public static boolean isSenhaForte(String senha) {
        return senha != null && senha.length() >= 8 && senha.length() <= 64
                && senha.matches(".*\\p{L}.*") && senha.matches(".*\\d.*");
    }

    /** Converte dd/MM/aaaa em LocalDate; retorna null se a data não existir. */
    public static LocalDate parseDataBr(String data) {
        if (data == null || data.trim().length() != 10) return null;
        try {
            return LocalDate.parse(data.trim(), DATA_BR);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}

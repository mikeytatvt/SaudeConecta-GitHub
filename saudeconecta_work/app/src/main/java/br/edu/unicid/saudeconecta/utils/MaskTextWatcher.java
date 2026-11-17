package br.edu.unicid.saudeconecta.utils;

import android.text.Editable;
import android.text.TextWatcher;

/** Máscaras de digitação: CPF, telefone e data. */
public class MaskTextWatcher implements TextWatcher {
    public static final String MASCARA_CPF = "###.###.###-##";
    public static final String MASCARA_DATA = "##/##/####";
    public static final String MASCARA_TELEFONE = "(##) #####-####";
    private static final String MASCARA_TELEFONE_FIXO = "(##) ####-####";

    private final String mascara;
    private boolean atualizando;

    public MaskTextWatcher(String mascara) {
        this.mascara = mascara;
    }

    public static String aplicar(String digitos, String mascara) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (char c : mascara.toCharArray()) {
            if (i >= digitos.length()) break;
            if (c == '#') sb.append(digitos.charAt(i++));
            else sb.append(c);
        }
        return sb.toString();
    }

    public static String formatarCpf(String cpf) {
        return aplicar(Validators.somenteDigitos(cpf), MASCARA_CPF);
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        if (atualizando) return;
        String digitos = Validators.somenteDigitos(s.toString());
        String m = mascara;
        if (MASCARA_TELEFONE.equals(mascara) && digitos.length() <= 10) m = MASCARA_TELEFONE_FIXO;
        String formatado = aplicar(digitos, m);
        if (!formatado.contentEquals(s)) {
            atualizando = true;
            s.replace(0, s.length(), formatado);
            atualizando = false;
        }
    }
}

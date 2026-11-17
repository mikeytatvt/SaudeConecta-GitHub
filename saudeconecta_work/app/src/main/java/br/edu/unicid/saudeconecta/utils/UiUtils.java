package br.edu.unicid.saudeconecta.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import br.edu.unicid.saudeconecta.activities.LoginActivity;

/** Funções de interface reutilizadas pelas telas. */
public final class UiUtils {
    private UiUtils() { }

    public static String texto(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString().trim();
    }

    /** Senhas não recebem trim (espaço pode fazer parte da senha). */
    public static String textoSemTrim(EditText campo) {
        return campo.getText() == null ? "" : campo.getText().toString();
    }

    public static String primeiroNome(String nomeCompleto) {
        if (nomeCompleto == null || nomeCompleto.trim().isEmpty()) return "";
        return nomeCompleto.trim().split("\\s+")[0];
    }

    public static void esconderTeclado(Activity activity) {
        View foco = activity.getCurrentFocus();
        if (foco != null) {
            InputMethodManager imm = (InputMethodManager) activity.getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(foco.getWindowToken(), 0);
        }
    }

    /** Encerra a sessão e volta ao login limpando a pilha de telas. */
    public static void sairParaLogin(Activity activity) {
        new SessionManager(activity).encerrarSessao();
        irParaLogin(activity);
    }

    public static void irParaLogin(Activity activity) {
        Intent intent = new Intent(activity, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        activity.startActivity(intent);
        activity.finish();
    }
}

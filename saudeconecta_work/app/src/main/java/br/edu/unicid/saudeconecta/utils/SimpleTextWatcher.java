package br.edu.unicid.saudeconecta.utils;

import android.text.Editable;
import android.text.TextWatcher;

/** TextWatcher enxuto: executa uma ação a cada alteração (ex.: limpar mensagem de erro). */
public class SimpleTextWatcher implements TextWatcher {
    private final Runnable acao;

    public SimpleTextWatcher(Runnable acao) {
        this.acao = acao;
    }

    @Override
    public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

    @Override
    public void onTextChanged(CharSequence s, int start, int before, int count) { }

    @Override
    public void afterTextChanged(Editable s) {
        acao.run();
    }
}

package br.edu.unicid.saudeconecta.adapters;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.Filter;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;

/**
 * Adapter das listas suspensas (especialidade, unidade, profissional).
 * Não filtra o texto digitado: a lista completa aparece sempre que o campo é aberto.
 */
public class OpcoesAdapter extends ArrayAdapter<String> {
    private final List<String> opcoes;

    public OpcoesAdapter(@NonNull Context context, @NonNull List<String> opcoes) {
        super(context, R.layout.item_dropdown, new ArrayList<>(opcoes));
        this.opcoes = new ArrayList<>(opcoes);
    }

    @NonNull
    @Override
    public Filter getFilter() {
        return new Filter() {
            @Override
            protected FilterResults performFiltering(CharSequence texto) {
                FilterResults resultado = new FilterResults();
                resultado.values = opcoes;
                resultado.count = opcoes.size();
                return resultado;
            }

            @Override
            protected void publishResults(CharSequence texto, FilterResults resultado) {
                notifyDataSetChanged();
            }
        };
    }
}

package br.edu.unicid.saudeconecta.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ItemAdminBinding;
import br.edu.unicid.saudeconecta.models.ItemAdmin;

/** Adapter único das listas administrativas (unidades, profissionais, pacientes e agendamentos). */
public class ItemAdminAdapter extends RecyclerView.Adapter<ItemAdminAdapter.ViewHolder> {

    public interface Listener {
        void aoTocar(ItemAdmin item);

        void aoAcao1(ItemAdmin item);

        void aoAcao2(ItemAdmin item);
    }

    private final List<ItemAdmin> itens = new ArrayList<>();
    private final Listener listener;

    public ItemAdminAdapter(Listener listener) {
        this.listener = listener;
    }

    public void atualizar(List<ItemAdmin> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAdminBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ItemAdmin item = itens.get(position);
        ItemAdminBinding b = holder.binding;

        b.tvTitulo.setText(item.titulo);
        mostrar(b.tvSubtitulo, item.subtitulo);
        mostrar(b.tvDetalhe, item.detalhe);
        estilizarEtiqueta(b, item);

        b.btnAcao1.setVisibility(item.acao1 != null ? View.VISIBLE : View.GONE);
        b.btnAcao1.setText(item.acao1);
        b.btnAcao2.setVisibility(item.acao2 != null ? View.VISIBLE : View.GONE);
        b.btnAcao2.setText(item.acao2);
        b.layoutAcoes.setVisibility(item.acao1 != null || item.acao2 != null ? View.VISIBLE : View.GONE);

        b.getRoot().setOnClickListener(v -> listener.aoTocar(item));
        b.btnAcao1.setOnClickListener(v -> listener.aoAcao1(item));
        b.btnAcao2.setOnClickListener(v -> listener.aoAcao2(item));
    }

    private static void mostrar(android.widget.TextView campo, String texto) {
        campo.setVisibility(texto == null || texto.isEmpty() ? View.GONE : View.VISIBLE);
        campo.setText(texto);
    }

    private static void estilizarEtiqueta(ItemAdminBinding b, ItemAdmin item) {
        if (item.etiqueta == null || item.estiloEtiqueta == ItemAdmin.ETIQUETA_NENHUMA) {
            b.tvEtiqueta.setVisibility(View.GONE);
            return;
        }
        Context ctx = b.getRoot().getContext();
        int fundo;
        int cor;
        switch (item.estiloEtiqueta) {
            case ItemAdmin.ETIQUETA_AZUL:
                fundo = R.color.status_realizada_fundo;
                cor = R.color.status_realizada_texto;
                break;
            case ItemAdmin.ETIQUETA_VERMELHA:
                fundo = R.color.status_cancelada_fundo;
                cor = R.color.status_cancelada_texto;
                break;
            case ItemAdmin.ETIQUETA_AMARELA:
                fundo = R.color.status_pendente_fundo;
                cor = R.color.status_pendente_texto;
                break;
            default:
                fundo = R.color.status_agendada_fundo;
                cor = R.color.status_agendada_texto;
                break;
        }
        b.tvEtiqueta.setVisibility(View.VISIBLE);
        b.tvEtiqueta.setText(item.etiqueta);
        b.tvEtiqueta.setTextColor(ctx.getColor(cor));
        b.tvEtiqueta.setBackgroundTintList(ColorStateList.valueOf(ctx.getColor(fundo)));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemAdminBinding binding;

        ViewHolder(ItemAdminBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

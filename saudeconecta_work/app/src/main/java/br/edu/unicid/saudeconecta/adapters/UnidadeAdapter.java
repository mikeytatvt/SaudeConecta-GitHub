package br.edu.unicid.saudeconecta.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ItemUnidadeBinding;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.utils.HorarioUtils;

/** Lista de unidades de saúde. */
public class UnidadeAdapter extends RecyclerView.Adapter<UnidadeAdapter.ViewHolder> {

    public interface Listener {
        void aoSelecionar(UnidadeSaude unidade);

        void aoLigar(UnidadeSaude unidade);

        void aoComoChegar(UnidadeSaude unidade);
    }

    private final List<UnidadeSaude> itens = new ArrayList<>();
    private final Listener listener;
    private long unidadeReferenciaId;

    public UnidadeAdapter(Listener listener) {
        this.listener = listener;
    }

    public void atualizar(List<UnidadeSaude> unidades, long referenciaId) {
        itens.clear();
        itens.addAll(unidades);
        unidadeReferenciaId = referenciaId;
        notifyDataSetChanged();
    }

    /** Divide "A, B, C" em itens. */
    public static List<String> servicos(UnidadeSaude u) {
        List<String> lista = new ArrayList<>();
        if (u.servicos == null) return lista;
        for (String s : u.servicos.split(",")) {
            if (!s.trim().isEmpty()) lista.add(s.trim());
        }
        return lista;
    }

    /** Etiqueta "Aberta agora" (verde) ou "Fechada" (vermelha). */
    public static void aplicarSituacao(android.widget.TextView etiqueta, UnidadeSaude u) {
        Boolean aberta = HorarioUtils.estaAberta(u.horarioFuncionamento, LocalDateTime.now());
        if (aberta == null) {
            etiqueta.setVisibility(View.GONE);
            return;
        }
        Context ctx = etiqueta.getContext();
        etiqueta.setVisibility(View.VISIBLE);
        etiqueta.setText(aberta ? R.string.aberta_agora : R.string.fechada_agora);
        etiqueta.setTextColor(ctx.getColor(aberta ? R.color.status_agendada_texto : R.color.status_cancelada_texto));
        etiqueta.setBackgroundTintList(ColorStateList.valueOf(
                ctx.getColor(aberta ? R.color.status_agendada_fundo : R.color.status_cancelada_fundo)));
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemUnidadeBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        UnidadeSaude u = itens.get(position);
        ItemUnidadeBinding b = holder.binding;
        Context ctx = b.getRoot().getContext();

        b.tvNome.setText(u.nome);
        b.tvBairro.setText(u.bairro);
        b.tvEndereco.setText(u.endereco + (u.bairro != null ? " – " + u.bairro : ""));
        b.tvTelefone.setText(u.telefone);
        b.tvHorario.setText(u.horarioFuncionamento);
        b.tvReferencia.setVisibility(u.id == unidadeReferenciaId ? View.VISIBLE : View.GONE);
        aplicarSituacao(b.tvAberto, u);
        b.getRoot().setContentDescription(ctx.getString(R.string.cd_unidade, u.nome, u.endereco));

        b.cgServicos.removeAllViews();
        float densidade = ctx.getResources().getDisplayMetrics().density;
        for (String servico : servicos(u)) {
            Chip chip = new Chip(ctx);
            chip.setText(servico);
            chip.setClickable(false);
            chip.setFocusable(false);
            chip.setEnsureMinTouchTargetSize(false);
            chip.setChipMinHeight(28 * densidade);
            chip.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12);
            chip.setTextColor(ctx.getColor(R.color.verde_escuro));
            chip.setChipBackgroundColor(ColorStateList.valueOf(ctx.getColor(R.color.verde_claro)));
            b.cgServicos.addView(chip);
        }

        b.getRoot().setOnClickListener(v -> listener.aoSelecionar(u));
        b.btnLigar.setOnClickListener(v -> listener.aoLigar(u));
        b.btnComoChegar.setOnClickListener(v -> listener.aoComoChegar(u));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemUnidadeBinding binding;

        ViewHolder(ItemUnidadeBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

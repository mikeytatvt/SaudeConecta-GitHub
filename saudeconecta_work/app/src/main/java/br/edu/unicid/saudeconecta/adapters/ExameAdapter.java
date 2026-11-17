package br.edu.unicid.saudeconecta.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ItemExameBinding;
import br.edu.unicid.saudeconecta.models.ExameDetalhado;
import br.edu.unicid.saudeconecta.models.StatusExame;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/** Lista de exames do paciente. */
public class ExameAdapter extends RecyclerView.Adapter<ExameAdapter.ViewHolder> {

    public interface Listener {
        void aoVerResultado(ExameDetalhado exame);

        void aoAbrirPdf(ExameDetalhado exame);
    }

    private final List<ExameDetalhado> itens = new ArrayList<>();
    private final Listener listener;

    public ExameAdapter(Listener listener) {
        this.listener = listener;
    }

    public void atualizar(List<ExameDetalhado> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemExameBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ExameDetalhado item = itens.get(position);
        ItemExameBinding b = holder.binding;
        Context ctx = b.getRoot().getContext();
        String status = item.exame.status;

        b.tvNome.setText(item.exame.nome);
        aplicarEstiloStatus(b.tvStatus, status);
        b.tvTipo.setText(item.exame.tipo);

        String solicitado = DateUtils.isoParaBr(item.exame.dataSolicitacao);
        b.tvSolicitacao.setText(item.profissionalNome != null
                ? ctx.getString(R.string.solicitado_em_por, solicitado, item.profissionalNome)
                : ctx.getString(R.string.solicitado_em, solicitado));

        if (StatusExame.AGENDADO.equals(status) && item.exame.dataRealizacao != null) {
            b.tvRealizacao.setText(ctx.getString(R.string.agendado_para,
                    DateUtils.isoParaBrComDiaSemana(item.exame.dataRealizacao)));
        } else if (item.exame.dataRealizacao != null) {
            b.tvRealizacao.setText(ctx.getString(R.string.realizado_em, DateUtils.isoParaBr(item.exame.dataRealizacao)));
        } else {
            b.tvRealizacao.setText(R.string.aguardando_agendamento);
        }

        b.linhaTvUnidade.setVisibility(item.unidadeNome != null ? View.VISIBLE : View.GONE);
        b.tvUnidade.setText(item.unidadeNome);

        boolean mostrarPreparo = item.exame.preparo != null && !StatusExame.RESULTADO_DISPONIVEL.equals(status)
                && !StatusExame.REALIZADO.equals(status);
        b.tvPreparo.setVisibility(mostrarPreparo ? View.VISIBLE : View.GONE);
        if (mostrarPreparo) b.tvPreparo.setText(ctx.getString(R.string.preparo, item.exame.preparo));

        boolean temResultado = StatusExame.RESULTADO_DISPONIVEL.equals(status);
        b.layoutAcoes.setVisibility(temResultado ? View.VISIBLE : View.GONE);
        b.btnVerResultado.setOnClickListener(v -> listener.aoVerResultado(item));
        b.btnPdf.setOnClickListener(v -> listener.aoAbrirPdf(item));
        b.getRoot().setOnClickListener(temResultado ? v -> listener.aoVerResultado(item) : null);
        b.getRoot().setClickable(temResultado);
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    /** Etiqueta: amarela (solicitado/em análise), azul (agendado), verde (resultado disponível). */
    public static void aplicarEstiloStatus(TextView etiqueta, String status) {
        Context ctx = etiqueta.getContext();
        int texto;
        int fundo;
        int cor;
        if (StatusExame.RESULTADO_DISPONIVEL.equals(status)) {
            texto = R.string.exame_resultado_disponivel;
            fundo = R.color.status_agendada_fundo;
            cor = R.color.status_agendada_texto;
        } else if (StatusExame.AGENDADO.equals(status)) {
            texto = R.string.exame_agendado;
            fundo = R.color.status_realizada_fundo;
            cor = R.color.status_realizada_texto;
        } else if (StatusExame.REALIZADO.equals(status)) {
            texto = R.string.exame_em_analise;
            fundo = R.color.status_pendente_fundo;
            cor = R.color.status_pendente_texto;
        } else {
            texto = R.string.exame_solicitado;
            fundo = R.color.status_pendente_fundo;
            cor = R.color.status_pendente_texto;
        }
        etiqueta.setText(texto);
        etiqueta.setTextColor(ctx.getColor(cor));
        etiqueta.setBackgroundTintList(ColorStateList.valueOf(ctx.getColor(fundo)));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemExameBinding binding;

        ViewHolder(ItemExameBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

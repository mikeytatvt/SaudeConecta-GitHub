package br.edu.unicid.saudeconecta.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ItemAtendimentoBinding;
import br.edu.unicid.saudeconecta.models.ConsultaDetalhada;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/** Lista de atendimentos (agendados, realizados ou cancelados). */
public class AtendimentoAdapter extends RecyclerView.Adapter<AtendimentoAdapter.ViewHolder> {

    public interface Listener {
        void aoVerDetalhes(ConsultaDetalhada consulta);

        void aoCancelar(ConsultaDetalhada consulta);
    }

    private final List<ConsultaDetalhada> itens = new ArrayList<>();
    private final Listener listener;

    public AtendimentoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void atualizar(List<ConsultaDetalhada> novos) {
        itens.clear();
        itens.addAll(novos);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemAtendimentoBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConsultaDetalhada item = itens.get(position);
        ItemAtendimentoBinding b = holder.binding;
        Context ctx = b.getRoot().getContext();

        b.tvTitulo.setText(ctx.getString(R.string.consulta_de, item.especialidadeNome));
        b.tvDataHora.setText(ctx.getString(R.string.data_hora,
                DateUtils.isoParaBr(item.consulta.data), item.consulta.hora));
        b.tvUnidade.setText(item.unidadeNome);
        b.tvProfissional.setText(item.profissionalNome);
        aplicarEstiloStatus(b.tvStatus, item.consulta.status);

        String status = item.consulta.status;
        if (StatusConsulta.REALIZADA.equals(status) && item.consulta.tempoEsperaMin != null) {
            b.tvObservacao.setVisibility(View.VISIBLE);
            b.tvObservacao.setText(ctx.getString(R.string.tempo_espera, item.consulta.tempoEsperaMin));
        } else if (StatusConsulta.CANCELADA.equals(status) && item.consulta.motivoCancelamento != null) {
            b.tvObservacao.setVisibility(View.VISIBLE);
            b.tvObservacao.setText(ctx.getString(R.string.motivo_cancelamento, item.consulta.motivoCancelamento));
        } else {
            b.tvObservacao.setVisibility(View.GONE);
        }

        b.btnCancelar.setVisibility(podeCancelar(item) ? View.VISIBLE : View.GONE);
        b.btnCancelar.setOnClickListener(v -> listener.aoCancelar(item));
        b.btnDetalhes.setOnClickListener(v -> listener.aoVerDetalhes(item));
        b.getRoot().setOnClickListener(v -> listener.aoVerDetalhes(item));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    /** Somente consultas agendadas de hoje em diante podem ser canceladas. */
    public static boolean podeCancelar(ConsultaDetalhada c) {
        return StatusConsulta.AGENDADA.equals(c.consulta.status)
                && !LocalDate.parse(c.consulta.data).isBefore(LocalDate.now());
    }

    /** Etiqueta colorida: verde (agendada), azul (realizada), vermelha (cancelada). */
    public static void aplicarEstiloStatus(TextView etiqueta, String status) {
        Context ctx = etiqueta.getContext();
        int texto;
        int fundo;
        int cor;
        if (StatusConsulta.REALIZADA.equals(status)) {
            texto = R.string.status_realizada;
            fundo = R.color.status_realizada_fundo;
            cor = R.color.status_realizada_texto;
        } else if (StatusConsulta.CANCELADA.equals(status)) {
            texto = R.string.status_cancelada;
            fundo = R.color.status_cancelada_fundo;
            cor = R.color.status_cancelada_texto;
        } else {
            texto = R.string.status_agendada;
            fundo = R.color.status_agendada_fundo;
            cor = R.color.status_agendada_texto;
        }
        etiqueta.setText(texto);
        etiqueta.setTextColor(ctx.getColor(cor));
        etiqueta.setBackgroundTintList(ColorStateList.valueOf(ctx.getColor(fundo)));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemAtendimentoBinding binding;

        ViewHolder(ItemAtendimentoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

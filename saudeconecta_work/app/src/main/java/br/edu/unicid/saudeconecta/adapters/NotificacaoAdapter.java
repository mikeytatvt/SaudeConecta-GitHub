package br.edu.unicid.saudeconecta.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ItemNotificacaoBinding;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;

/** Lista de notificações, com ícone e cor por tipo e destaque para as não lidas. */
public class NotificacaoAdapter extends RecyclerView.Adapter<NotificacaoAdapter.ViewHolder> {

    public interface Listener {
        void aoAbrir(Notificacao notificacao);
    }

    private final List<Notificacao> itens = new ArrayList<>();
    private final Listener listener;

    public NotificacaoAdapter(Listener listener) {
        this.listener = listener;
    }

    public void atualizar(List<Notificacao> novas) {
        itens.clear();
        itens.addAll(novas);
        notifyDataSetChanged();
    }

    public Notificacao itemNa(int posicao) {
        return itens.get(posicao);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(ItemNotificacaoBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Notificacao n = itens.get(position);
        ItemNotificacaoBinding b = holder.binding;
        Context ctx = b.getRoot().getContext();

        b.tvTitulo.setText(n.titulo);
        b.tvTitulo.setTypeface(null, n.lida ? Typeface.NORMAL : Typeface.BOLD);
        b.tvMensagem.setText(n.mensagem);
        b.tvTempo.setText(tempoRelativo(ctx, n.dataHora));
        b.pontoNaoLida.setVisibility(n.lida ? View.GONE : View.VISIBLE);
        b.getRoot().setCardBackgroundColor(ctx.getColor(n.lida ? R.color.branco : R.color.cinza_claro));
        b.getRoot().setStrokeColor(ctx.getColor(n.lida ? R.color.borda : R.color.verde_claro));

        int icone;
        int fundo;
        int cor;
        switch (n.tipo) {
            case TipoNotificacao.CONFIRMACAO:
                icone = R.drawable.ic_check_circle;
                fundo = R.color.status_agendada_fundo;
                cor = R.color.status_agendada_texto;
                break;
            case TipoNotificacao.LEMBRETE:
                icone = R.drawable.ic_relogio;
                fundo = R.color.status_realizada_fundo;
                cor = R.color.status_realizada_texto;
                break;
            case TipoNotificacao.ALTERACAO:
                icone = R.drawable.ic_calendar;
                fundo = R.color.status_pendente_fundo;
                cor = R.color.status_pendente_texto;
                break;
            case TipoNotificacao.RESULTADO:
                icone = R.drawable.ic_exames;
                fundo = R.color.status_agendada_fundo;
                cor = R.color.status_agendada_texto;
                break;
            default:
                icone = R.drawable.ic_info;
                fundo = R.color.status_realizada_fundo;
                cor = R.color.status_realizada_texto;
                break;
        }
        b.ivIcone.setImageResource(icone);
        b.ivIcone.setImageTintList(ColorStateList.valueOf(ctx.getColor(cor)));
        b.fundoIcone.setBackgroundTintList(ColorStateList.valueOf(ctx.getColor(fundo)));

        String descricao = n.titulo + ". " + n.mensagem + (n.lida ? "" : ", " + ctx.getString(R.string.cd_nao_lida));
        b.getRoot().setContentDescription(descricao);
        b.getRoot().setOnClickListener(v -> listener.aoAbrir(n));
    }

    @Override
    public int getItemCount() {
        return itens.size();
    }

    /** "agora", "há 5 min", "há 3 h", "ontem às 14:20" ou "10/11/2026 às 09:00". */
    public static String tempoRelativo(Context ctx, long momento) {
        LocalDateTime quando = LocalDateTime.ofInstant(Instant.ofEpochMilli(momento), ZoneId.systemDefault());
        long minutos = (System.currentTimeMillis() - momento) / 60_000L;
        String hora = quando.format(DateTimeFormatter.ofPattern("HH:mm"));
        if (minutos < 1) return ctx.getString(R.string.agora);
        if (minutos < 60) return ctx.getString(R.string.ha_minutos, (int) minutos);
        if (quando.toLocalDate().equals(LocalDate.now())) return ctx.getString(R.string.ha_horas, (int) (minutos / 60));
        if (quando.toLocalDate().equals(LocalDate.now().minusDays(1))) return ctx.getString(R.string.ontem_as, hora);
        return ctx.getString(R.string.data_as, quando.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")), hora);
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final ItemNotificacaoBinding binding;

        ViewHolder(ItemNotificacaoBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
    }
}

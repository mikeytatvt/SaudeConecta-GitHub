package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.models.ConsultaDetalhada;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.DateUtils;
import br.edu.unicid.saudeconecta.utils.NotificacaoHelper;

/** Notificações do usuário: listagem, leitura, exclusão e lembretes de consulta. */
public class NotificacaoRepository {
    private final AppDatabase db;
    private final Context contexto;

    public NotificacaoRepository(Context context) {
        contexto = context.getApplicationContext();
        db = AppDatabase.getInstance(contexto);
    }

    public void contarNaoLidas(long usuarioId, RepositoryCallback<Integer> callback) {
        AppExecutors.disco().execute(() -> {
            int total = db.notificacaoDao().contarNaoLidas(usuarioId);
            AppExecutors.principal(() -> callback.onSuccess(total));
        });
    }

    public void listar(long usuarioId, RepositoryCallback<List<Notificacao>> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                List<Notificacao> lista = db.notificacaoDao().listarPorUsuario(usuarioId);
                AppExecutors.principal(() -> callback.onSuccess(lista));
            } catch (Exception e) {
                AppExecutors.principal(() -> callback.onError("Não foi possível carregar as notificações."));
            }
        });
    }

    public void marcarComoLida(long notificacaoId, long usuarioId) {
        AppExecutors.disco().execute(() -> db.notificacaoDao().marcarComoLida(notificacaoId, usuarioId));
    }

    public void marcarTodasComoLidas(long usuarioId, RepositoryCallback<Boolean> callback) {
        AppExecutors.disco().execute(() -> {
            db.notificacaoDao().marcarTodasComoLidas(usuarioId);
            AppExecutors.principal(() -> callback.onSuccess(true));
        });
    }

    public void excluir(Notificacao n, long usuarioId, RepositoryCallback<Boolean> callback) {
        AppExecutors.disco().execute(() -> {
            if (n.usuarioId == usuarioId) db.notificacaoDao().excluir(n);
            AppExecutors.principal(() -> callback.onSuccess(true));
        });
    }

    /** Desfaz a exclusão (reinsere com o mesmo id). */
    public void restaurar(Notificacao n, RepositoryCallback<Boolean> callback) {
        AppExecutors.disco().execute(() -> {
            db.notificacaoDao().inserir(n);
            AppExecutors.principal(() -> callback.onSuccess(true));
        });
    }

    /**
     * Verifica as consultas de hoje até depois de amanhã e cria os lembretes que ainda não existem.
     * Chamado ao abrir o app (complementa o alarme agendado pelo LembreteScheduler).
     */
    public void gerarLembretesProximos(long usuarioId, long pacienteId, RepositoryCallback<Integer> callback) {
        AppExecutors.disco().execute(() -> {
            int criados = 0;
            try {
                LocalDate hoje = LocalDate.now();
                List<ConsultaDetalhada> proximas = db.consultaDao()
                        .listarAgendadasNoPeriodo(pacienteId, hoje.toString(), hoje.plusDays(2).toString());
                for (ConsultaDetalhada c : proximas) {
                    if (criarLembreteSeNecessario(contexto, db, usuarioId, c.consulta.id)) criados++;
                }
            } catch (Exception ignorado) {
                // lembretes são complementares; uma falha aqui não impede o uso do app
            }
            int total = criados;
            AppExecutors.principal(() -> callback.onSuccess(total));
        });
    }

    /**
     * Cria o lembrete (no banco e na barra do Android) se a consulta ainda estiver agendada
     * e no futuro, e se ainda não houver lembrete para ela. Executar fora da thread principal.
     */
    public static boolean criarLembreteSeNecessario(Context contexto, AppDatabase db, long usuarioId, long consultaId) {
        ConsultaDetalhada c = db.consultaDao().buscarDetalhadaPorId(consultaId);
        if (c == null || !StatusConsulta.AGENDADA.equals(c.consulta.status)) return false;
        LocalDateTime quando = LocalDateTime.of(LocalDate.parse(c.consulta.data), LocalTime.parse(c.consulta.hora));
        if (!quando.isAfter(LocalDateTime.now())) return false;
        if (db.notificacaoDao().contarLembretes(usuarioId, consultaId) > 0) return false;

        LocalDate dia = quando.toLocalDate();
        LocalDate hoje = LocalDate.now();
        String quandoTexto = dia.equals(hoje) ? "hoje"
                : dia.equals(hoje.plusDays(1)) ? "amanhã (" + DateUtils.isoParaBr(c.consulta.data) + ")"
                : "em " + DateUtils.isoParaBr(c.consulta.data);
        String titulo = "Lembrete de atendimento";
        String mensagem = "Sua consulta de " + c.especialidadeNome + " com " + c.profissionalNome + " é "
                + quandoTexto + " às " + c.consulta.hora + " na " + c.unidadeNome
                + ". Chegue 15 minutos antes e leve documento com foto e o Cartão SUS.";

        db.notificacaoDao().inserir(Notificacao.criar(usuarioId, titulo, mensagem, TipoNotificacao.LEMBRETE,
                System.currentTimeMillis(), false, consultaId));
        NotificacaoHelper.mostrar(contexto, NotificacaoHelper.idLembrete(consultaId), titulo, mensagem,
                TipoNotificacao.LEMBRETE);
        return true;
    }
}

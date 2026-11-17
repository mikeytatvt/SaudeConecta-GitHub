package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.models.Consulta;
import br.edu.unicid.saudeconecta.models.ConsultaDetalhada;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/** Consultas do paciente: próximo atendimento, histórico, detalhes e cancelamento. */
public class ConsultaRepository {
    private final AppDatabase db;

    public ConsultaRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    /** Retorna null quando o paciente não tem consulta futura agendada. */
    public void buscarProximaConsulta(long pacienteId, RepositoryCallback<ConsultaDetalhada> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                LocalDateTime agora = LocalDateTime.now();
                String hoje = agora.toLocalDate().toString();
                String hora = String.format(Locale.ROOT, "%02d:%02d", agora.getHour(), agora.getMinute());
                ConsultaDetalhada c = db.consultaDao().buscarProximaAgendada(pacienteId, hoje, hora);
                AppExecutors.principal(() -> callback.onSuccess(c));
            } catch (Exception e) {
                erro(callback, "Não foi possível carregar a próxima consulta.");
            }
        });
    }

    public void listarAtendimentos(long pacienteId, RepositoryCallback<List<ConsultaDetalhada>> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                List<ConsultaDetalhada> lista = db.consultaDao().listarDetalhadasPorPaciente(pacienteId);
                AppExecutors.principal(() -> callback.onSuccess(lista));
            } catch (Exception e) {
                erro(callback, "Não foi possível carregar seus atendimentos.");
            }
        });
    }

    public void listarExamesDaConsulta(long consultaId, RepositoryCallback<List<String>> callback) {
        AppExecutors.disco().execute(() -> {
            List<String> nomes = db.exameDao().listarNomesPorConsulta(consultaId);
            AppExecutors.principal(() -> callback.onSuccess(nomes));
        });
    }

    /**
     * Cancela uma consulta futura do próprio paciente, libera o horário
     * e registra uma notificação de alteração de agendamento.
     */
    public void cancelarConsulta(long consultaId, long pacienteId, long usuarioId, String motivo,
                                 RepositoryCallback<Boolean> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                ConsultaDetalhada detalhe = db.consultaDao().buscarDetalhadaPorId(consultaId);
                if (detalhe == null || detalhe.consulta.pacienteId != pacienteId) {
                    erro(callback, "Consulta não encontrada.");
                    return;
                }
                Consulta c = detalhe.consulta;
                if (!StatusConsulta.AGENDADA.equals(c.status)) {
                    erro(callback, "Apenas consultas agendadas podem ser canceladas.");
                    return;
                }
                if (LocalDate.parse(c.data).isBefore(LocalDate.now())) {
                    erro(callback, "Não é possível cancelar uma consulta com data passada.");
                    return;
                }

                db.runInTransaction(() -> {
                    c.status = StatusConsulta.CANCELADA;
                    c.motivoCancelamento = motivo;
                    c.atualizadoEm = System.currentTimeMillis();
                    db.consultaDao().atualizar(c);
                    db.notificacaoDao().inserir(Notificacao.criar(usuarioId, "Consulta cancelada",
                            "Sua consulta de " + detalhe.especialidadeNome + " em "
                                    + DateUtils.isoParaBr(c.data) + " às " + c.hora
                                    + " foi cancelada. Você pode agendar uma nova data pelo aplicativo.",
                            TipoNotificacao.ALTERACAO, System.currentTimeMillis(), false, c.id));
                });
                AppExecutors.principal(() -> callback.onSuccess(true));
            } catch (Exception e) {
                erro(callback, "Não foi possível cancelar a consulta. Tente novamente.");
            }
        });
    }

    private static <T> void erro(RepositoryCallback<T> callback, String mensagem) {
        AppExecutors.principal(() -> callback.onError(mensagem));
    }
}

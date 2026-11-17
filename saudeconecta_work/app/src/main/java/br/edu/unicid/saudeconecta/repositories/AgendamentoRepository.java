package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.models.Consulta;
import br.edu.unicid.saudeconecta.models.ConsultaDetalhada;
import br.edu.unicid.saudeconecta.models.Especialidade;
import br.edu.unicid.saudeconecta.models.HorarioOcupado;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.Profissional;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.models.TipoAtendimento;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/**
 * Fluxo de agendamento: lista opções, verifica disponibilidade e registra a consulta.
 * A consulta gravada alimenta o "Próximo atendimento" do paciente e os indicadores do gestor.
 */
public class AgendamentoRepository {
    private final AppDatabase db;

    public AgendamentoRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void listarEspecialidades(RepositoryCallback<List<Especialidade>> callback) {
        AppExecutors.disco().execute(() -> {
            List<Especialidade> lista = db.especialidadeDao().listarTodas();
            AppExecutors.principal(() -> callback.onSuccess(lista));
        });
    }

    public void listarUnidades(long especialidadeId, RepositoryCallback<List<UnidadeSaude>> callback) {
        AppExecutors.disco().execute(() -> {
            List<UnidadeSaude> lista = db.unidadeSaudeDao().listarPorEspecialidade(especialidadeId);
            AppExecutors.principal(() -> callback.onSuccess(lista));
        });
    }

    public void listarProfissionais(long especialidadeId, long unidadeId,
                                    RepositoryCallback<List<Profissional>> callback) {
        AppExecutors.disco().execute(() -> {
            List<Profissional> lista = db.profissionalDao().listarPorEspecialidadeEUnidade(especialidadeId, unidadeId);
            AppExecutors.principal(() -> callback.onSuccess(lista));
        });
    }

    /** Mapa data (yyyy-MM-dd) -> horários já ocupados no período. */
    public void carregarAgenda(long profissionalId, LocalDate inicio, LocalDate fim,
                               RepositoryCallback<Map<String, Set<String>>> callback) {
        AppExecutors.disco().execute(() -> {
            List<HorarioOcupado> lista = db.consultaDao()
                    .horariosOcupadosNoPeriodo(profissionalId, inicio.toString(), fim.toString());
            Map<String, Set<String>> agenda = new HashMap<>();
            for (HorarioOcupado h : lista) {
                Set<String> horas = agenda.get(h.data);
                if (horas == null) {
                    horas = new HashSet<>();
                    agenda.put(h.data, horas);
                }
                horas.add(h.hora);
            }
            AppExecutors.principal(() -> callback.onSuccess(agenda));
        });
    }

    /** Valida as regras, grava a consulta e a notificação de confirmação. Retorna o id da consulta. */
    public void agendar(long pacienteId, long usuarioId, Profissional profissional, String especialidade,
                        String unidade, String data, String hora, RepositoryCallback<Long> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                ConsultaDetalhada existente = db.consultaDao().buscarAgendadaPorEspecialidade(
                        pacienteId, profissional.especialidadeId, LocalDate.now().toString());
                if (existente != null) {
                    erro(callback, "Você já tem uma consulta de " + especialidade + " agendada para "
                            + DateUtils.isoParaBr(existente.consulta.data) + " às " + existente.consulta.hora
                            + ". Para remarcar, cancele-a em Meus atendimentos.");
                    return;
                }
                if (db.consultaDao().contarConflitosPaciente(pacienteId, data, hora) > 0) {
                    erro(callback, "Você já tem outra consulta neste mesmo dia e horário.");
                    return;
                }

                final long[] consultaId = {0};
                db.runInTransaction(() -> {
                    // Nova verificação dentro da transação: o horário pode ter sido ocupado agora
                    if (db.consultaDao().contarOcupacao(profissional.id, data, hora) > 0) return;

                    String tipo = "Odontologia".equals(especialidade)
                            ? TipoAtendimento.ODONTOLOGICO : TipoAtendimento.CONSULTA_MEDICA;
                    Consulta c = Consulta.criar(pacienteId, profissional, data, hora, StatusConsulta.AGENDADA, tipo);
                    c.observacoes = "Agendada pelo aplicativo";
                    consultaId[0] = db.consultaDao().inserir(c);

                    db.notificacaoDao().inserir(Notificacao.criar(usuarioId, "Consulta confirmada",
                            "Sua consulta de " + especialidade + " com " + profissional.nome + " na " + unidade
                                    + " foi confirmada para " + DateUtils.isoParaBr(data) + " às " + hora + ".",
                            TipoNotificacao.CONFIRMACAO, System.currentTimeMillis(), false, consultaId[0]));
                });

                if (consultaId[0] == 0) {
                    erro(callback, "Este horário acabou de ser ocupado. Escolha outro horário.");
                    return;
                }
                long id = consultaId[0];
                AppExecutors.principal(() -> callback.onSuccess(id));
            } catch (Exception e) {
                erro(callback, "Não foi possível concluir o agendamento. Tente novamente.");
            }
        });
    }

    private static <T> void erro(RepositoryCallback<T> callback, String mensagem) {
        AppExecutors.principal(() -> callback.onError(mensagem));
    }
}

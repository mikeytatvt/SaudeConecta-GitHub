package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Locale;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.models.AgendamentoAdmin;
import br.edu.unicid.saudeconecta.models.Consulta;
import br.edu.unicid.saudeconecta.models.Especialidade;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.PacienteResumo;
import br.edu.unicid.saudeconecta.models.Profissional;
import br.edu.unicid.saudeconecta.models.ProfissionalDetalhado;
import br.edu.unicid.saudeconecta.models.StatusConsulta;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/** Operações administrativas do gestor: cadastros, pacientes e agendamentos. */
public class AdminRepository {
    private final AppDatabase db;

    public AdminRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    private interface Tarefa<T> {
        T executar() throws Exception;
    }

    /** Executa no banco (thread de disco) e devolve o resultado na thread principal. */
    private <T> void rodar(Tarefa<T> tarefa, RepositoryCallback<T> callback, String mensagemErro) {
        AppExecutors.disco().execute(() -> {
            try {
                T resultado = tarefa.executar();
                AppExecutors.principal(() -> callback.onSuccess(resultado));
            } catch (RegraException e) {
                AppExecutors.principal(() -> callback.onError(e.getMessage()));
            } catch (Exception e) {
                AppExecutors.principal(() -> callback.onError(mensagemErro));
            }
        });
    }

    /** Erro de regra de negócio com mensagem para o usuário. */
    private static class RegraException extends Exception {
        RegraException(String mensagem) {
            super(mensagem);
        }
    }

    private static String hoje() {
        return LocalDate.now().toString();
    }

    // ================================================================ Unidades

    public void listarUnidades(RepositoryCallback<List<UnidadeSaude>> cb) {
        rodar(() -> db.adminDao().listarUnidades(), cb, "Não foi possível carregar as unidades.");
    }

    public void buscarUnidade(long id, RepositoryCallback<UnidadeSaude> cb) {
        rodar(() -> db.unidadeSaudeDao().buscarPorId(id), cb, "Unidade não encontrada.");
    }

    public void salvarUnidade(UnidadeSaude u, RepositoryCallback<Long> cb) {
        rodar(() -> {
            if (db.adminDao().contarNomeUnidade(u.nome, u.id) > 0) {
                throw new RegraException("Já existe uma unidade com este nome.");
            }
            try {
                if (u.id == 0) return db.unidadeSaudeDao().inserir(u);
                db.unidadeSaudeDao().atualizar(u);
                return u.id;
            } catch (SQLiteConstraintException e) {
                throw new RegraException("Já existe uma unidade com este nome.");
            }
        }, cb, "Não foi possível salvar a unidade.");
    }

    /** Não permite desativar unidade com consultas futuras agendadas. */
    public void definirAtivoUnidade(long id, boolean ativo, RepositoryCallback<Boolean> cb) {
        rodar(() -> {
            if (!ativo) {
                int futuras = db.adminDao().contarAgendadasFuturasUnidade(id, hoje());
                if (futuras > 0) {
                    throw new RegraException("A unidade tem " + futuras + " consulta(s) agendada(s). "
                            + "Cancele ou transfira os agendamentos antes de desativá-la.");
                }
            }
            db.adminDao().definirAtivoUnidade(id, ativo);
            return true;
        }, cb, "Não foi possível alterar a unidade.");
    }

    // ================================================================ Profissionais

    public void listarProfissionais(RepositoryCallback<List<ProfissionalDetalhado>> cb) {
        rodar(() -> db.adminDao().listarProfissionais(hoje()), cb, "Não foi possível carregar os profissionais.");
    }

    public void buscarProfissional(long id, RepositoryCallback<Profissional> cb) {
        rodar(() -> db.profissionalDao().buscarPorId(id), cb, "Profissional não encontrado.");
    }

    public static class OpcoesProfissional {
        public List<Especialidade> especialidades;
        public List<UnidadeSaude> unidades;
    }

    public void carregarOpcoesProfissional(RepositoryCallback<OpcoesProfissional> cb) {
        rodar(() -> {
            OpcoesProfissional o = new OpcoesProfissional();
            o.especialidades = db.especialidadeDao().listarTodas();
            o.unidades = db.unidadeSaudeDao().listarAtivas();
            return o;
        }, cb, "Não foi possível carregar as opções.");
    }

    /** Salva o profissional e inclui a especialidade nos serviços da unidade, se faltar. */
    public void salvarProfissional(Profissional p, String especialidadeNome, RepositoryCallback<Long> cb) {
        rodar(() -> {
            if (db.adminDao().contarRegistro(p.registro, p.id) > 0) {
                throw new RegraException("Este registro profissional já está cadastrado.");
            }
            final long[] id = {p.id};
            db.runInTransaction(() -> {
                if (p.id == 0) id[0] = db.profissionalDao().inserir(p);
                else db.profissionalDao().atualizar(p);

                UnidadeSaude u = db.unidadeSaudeDao().buscarPorId(p.unidadeId);
                if (u != null && (u.servicos == null || !u.servicos.toLowerCase(Locale.ROOT)
                        .contains(especialidadeNome.toLowerCase(Locale.ROOT)))) {
                    u.servicos = (u.servicos == null || u.servicos.isEmpty())
                            ? especialidadeNome : especialidadeNome + ", " + u.servicos;
                    db.unidadeSaudeDao().atualizar(u);
                }
            });
            return id[0];
        }, cb, "Não foi possível salvar o profissional.");
    }

    public void definirAtivoProfissional(long id, boolean ativo, RepositoryCallback<Boolean> cb) {
        rodar(() -> {
            if (!ativo) {
                int futuras = db.adminDao().contarAgendadasFuturasProfissional(id, hoje());
                if (futuras > 0) {
                    throw new RegraException("O profissional tem " + futuras + " consulta(s) agendada(s). "
                            + "Cancele os agendamentos antes de desativá-lo.");
                }
            }
            db.adminDao().definirAtivoProfissional(id, ativo);
            return true;
        }, cb, "Não foi possível alterar o profissional.");
    }

    // ================================================================ Pacientes

    public void listarPacientes(RepositoryCallback<List<PacienteResumo>> cb) {
        rodar(() -> db.adminDao().listarPacientes(), cb, "Não foi possível carregar os pacientes.");
    }

    public void listarUnidadesAtivas(RepositoryCallback<List<UnidadeSaude>> cb) {
        rodar(() -> db.unidadeSaudeDao().listarAtivas(), cb, "Não foi possível carregar as unidades.");
    }

    public void alterarUnidadeReferencia(long pacienteId, long unidadeId, RepositoryCallback<Boolean> cb) {
        rodar(() -> {
            db.adminDao().alterarUnidadeReferencia(pacienteId, unidadeId);
            return true;
        }, cb, "Não foi possível alterar a unidade de referência.");
    }

    // ================================================================ Agendamentos

    public void listarAgendamentos(String status, LocalDate inicio, LocalDate fim,
                                   RepositoryCallback<List<AgendamentoAdmin>> cb) {
        rodar(() -> db.adminDao().listarAgendamentos(status, inicio.toString(), fim.toString()), cb,
                "Não foi possível carregar os agendamentos.");
    }

    /**
     * Registra o atendimento (consulta realizada) com o tempo de espera.
     * Consulta com data futura é registrada como encaixe de hoje.
     * O registro alimenta os indicadores do dashboard.
     */
    public void registrarAtendimento(long consultaId, int tempoEsperaMin, RepositoryCallback<Boolean> cb) {
        rodar(() -> {
            Consulta c = db.consultaDao().buscarPorId(consultaId);
            if (c == null || !StatusConsulta.AGENDADA.equals(c.status)) {
                throw new RegraException("Esta consulta não está mais agendada.");
            }
            LocalDate hoje = LocalDate.now();
            if (LocalDate.parse(c.data).isAfter(hoje)) {
                LocalTime agora = LocalTime.now();
                c.data = hoje.toString();
                c.hora = String.format(Locale.ROOT, "%02d:%02d", agora.getHour(), agora.getMinute());
                c.observacoes = (c.observacoes == null ? "" : c.observacoes + ". ") + "Atendimento antecipado (encaixe)";
            }
            c.status = StatusConsulta.REALIZADA;
            c.tempoEsperaMin = tempoEsperaMin;
            c.atualizadoEm = System.currentTimeMillis();
            db.consultaDao().atualizar(c);
            return true;
        }, cb, "Não foi possível registrar o atendimento.");
    }

    /** Cancela pela unidade e avisa o paciente (se ele tiver conta no app). */
    public void cancelarPelaUnidade(AgendamentoAdmin a, String motivo, RepositoryCallback<Boolean> cb) {
        rodar(() -> {
            Consulta c = db.consultaDao().buscarPorId(a.consulta.id);
            if (c == null || !StatusConsulta.AGENDADA.equals(c.status)) {
                throw new RegraException("Esta consulta não está mais agendada.");
            }
            db.runInTransaction(() -> {
                c.status = StatusConsulta.CANCELADA;
                c.motivoCancelamento = motivo + " (cancelada pela unidade)";
                c.atualizadoEm = System.currentTimeMillis();
                db.consultaDao().atualizar(c);
                if (a.pacienteUsuarioId != null) {
                    db.notificacaoDao().inserir(Notificacao.criar(a.pacienteUsuarioId, "Consulta cancelada pela unidade",
                            "Sua consulta de " + a.especialidadeNome + " em " + DateUtils.isoParaBr(c.data) + " às "
                                    + c.hora + " na " + a.unidadeNome + " foi cancelada. Motivo: " + motivo
                                    + ". Agende uma nova data pelo aplicativo.",
                            TipoNotificacao.ALTERACAO, System.currentTimeMillis(), false, c.id));
                }
            });
            return true;
        }, cb, "Não foi possível cancelar a consulta.");
    }
}

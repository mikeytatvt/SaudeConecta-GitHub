package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Query;

import java.util.List;

import br.edu.unicid.saudeconecta.models.AgendamentoAdmin;
import br.edu.unicid.saudeconecta.models.PacienteResumo;
import br.edu.unicid.saudeconecta.models.ProfissionalDetalhado;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;

/** Consultas e alterações da área administrativa (somente perfil GESTOR). */
@Dao
public interface AdminDao {

    // ---------------------------------------------------------------- Unidades

    @Query("SELECT * FROM unidades_saude ORDER BY ativo DESC, nome")
    List<UnidadeSaude> listarUnidades();

    @Query("SELECT COUNT(*) FROM unidades_saude WHERE nome = :nome COLLATE NOCASE AND id <> :ignorarId")
    int contarNomeUnidade(String nome, long ignorarId);

    @Query("SELECT COUNT(*) FROM consultas WHERE unidade_id = :unidadeId AND status = 'AGENDADA' AND data >= :hoje")
    int contarAgendadasFuturasUnidade(long unidadeId, String hoje);

    @Query("UPDATE unidades_saude SET ativo = :ativo WHERE id = :id")
    void definirAtivoUnidade(long id, boolean ativo);

    // ---------------------------------------------------------------- Profissionais

    @Query("SELECT p.*, e.nome AS especialidade_nome, u.nome AS unidade_nome, "
            + "(SELECT COUNT(*) FROM consultas c WHERE c.profissional_id = p.id AND c.status = 'AGENDADA' "
            + "AND c.data >= :hoje) AS agendadas_futuras "
            + "FROM profissionais p "
            + "INNER JOIN especialidades e ON e.id = p.especialidade_id "
            + "INNER JOIN unidades_saude u ON u.id = p.unidade_id "
            + "ORDER BY p.ativo DESC, p.nome")
    List<ProfissionalDetalhado> listarProfissionais(String hoje);

    @Query("SELECT COUNT(*) FROM profissionais WHERE registro = :registro COLLATE NOCASE AND id <> :ignorarId")
    int contarRegistro(String registro, long ignorarId);

    @Query("SELECT COUNT(*) FROM consultas WHERE profissional_id = :profissionalId AND status = 'AGENDADA' "
            + "AND data >= :hoje")
    int contarAgendadasFuturasProfissional(long profissionalId, String hoje);

    @Query("UPDATE profissionais SET ativo = :ativo WHERE id = :id")
    void definirAtivoProfissional(long id, boolean ativo);

    // ---------------------------------------------------------------- Pacientes

    @Query("SELECT pa.*, u.nome AS unidade_nome, "
            + "(SELECT COUNT(*) FROM consultas c WHERE c.paciente_id = pa.id AND c.status = 'REALIZADA') AS atendimentos, "
            + "(SELECT COUNT(*) FROM consultas c WHERE c.paciente_id = pa.id AND c.status = 'AGENDADA') AS agendadas "
            + "FROM pacientes pa LEFT JOIN unidades_saude u ON u.id = pa.unidade_referencia_id "
            + "ORDER BY pa.nome")
    List<PacienteResumo> listarPacientes();

    @Query("UPDATE pacientes SET unidade_referencia_id = :unidadeId WHERE id = :pacienteId")
    void alterarUnidadeReferencia(long pacienteId, long unidadeId);

    // ---------------------------------------------------------------- Agendamentos

    @Query("SELECT c.*, pa.nome AS paciente_nome, pa.usuario_id AS paciente_usuario_id, "
            + "p.nome AS profissional_nome, e.nome AS especialidade_nome, u.nome AS unidade_nome "
            + "FROM consultas c "
            + "INNER JOIN pacientes pa ON pa.id = c.paciente_id "
            + "INNER JOIN profissionais p ON p.id = c.profissional_id "
            + "INNER JOIN especialidades e ON e.id = c.especialidade_id "
            + "INNER JOIN unidades_saude u ON u.id = c.unidade_id "
            + "WHERE c.status = :status AND c.data BETWEEN :inicio AND :fim "
            + "ORDER BY c.data, c.hora")
    List<AgendamentoAdmin> listarAgendamentos(String status, String inicio, String fim);
}

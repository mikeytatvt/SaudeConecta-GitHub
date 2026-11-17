package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.Consulta;
import br.edu.unicid.saudeconecta.models.ConsultaDetalhada;
import br.edu.unicid.saudeconecta.models.HorarioOcupado;

@Dao
public interface ConsultaDao {

    /** Consulta + nomes do profissional, especialidade e unidade. */
    String SELECT_DETALHADA = "SELECT c.*, p.nome AS profissional_nome, e.nome AS especialidade_nome, "
            + "u.nome AS unidade_nome, u.endereco AS unidade_endereco, u.bairro AS unidade_bairro, "
            + "u.telefone AS unidade_telefone "
            + "FROM consultas c "
            + "INNER JOIN profissionais p ON p.id = c.profissional_id "
            + "INNER JOIN especialidades e ON e.id = c.especialidade_id "
            + "INNER JOIN unidades_saude u ON u.id = c.unidade_id ";

    @Insert
    long inserir(Consulta consulta);

    @Insert
    List<Long> inserirTodos(List<Consulta> consultas);

    @Update
    int atualizar(Consulta consulta);

    @Query("SELECT * FROM consultas WHERE id = :id")
    Consulta buscarPorId(long id);

    @Query("SELECT * FROM consultas WHERE paciente_id = :pacienteId ORDER BY data DESC, hora DESC")
    List<Consulta> listarPorPaciente(long pacienteId);

    /** Próxima consulta agendada a partir de agora (card "Próximo atendimento"). */
    @Query(SELECT_DETALHADA
            + "WHERE c.paciente_id = :pacienteId AND c.status = 'AGENDADA' "
            + "AND (c.data > :hoje OR (c.data = :hoje AND c.hora >= :horaAtual)) "
            + "ORDER BY c.data, c.hora LIMIT 1")
    ConsultaDetalhada buscarProximaAgendada(long pacienteId, String hoje, String horaAtual);

    /** Todos os atendimentos do paciente (tela Meus atendimentos). */
    @Query(SELECT_DETALHADA + "WHERE c.paciente_id = :pacienteId ORDER BY c.data DESC, c.hora DESC")
    List<ConsultaDetalhada> listarDetalhadasPorPaciente(long pacienteId);

    /** Consultas agendadas do paciente entre duas datas (geração de lembretes). */
    @Query(SELECT_DETALHADA + "WHERE c.paciente_id = :pacienteId AND c.status = 'AGENDADA' "
            + "AND c.data BETWEEN :inicio AND :fim ORDER BY c.data, c.hora")
    List<ConsultaDetalhada> listarAgendadasNoPeriodo(long pacienteId, String inicio, String fim);

    @Query(SELECT_DETALHADA + "WHERE c.id = :id")
    ConsultaDetalhada buscarDetalhadaPorId(long id);

    /** Horários já ocupados de um profissional em uma data (verificação de disponibilidade). */
    @Query("SELECT hora FROM consultas WHERE profissional_id = :profissionalId AND data = :data AND status <> 'CANCELADA'")
    List<String> horariosOcupados(long profissionalId, String data);

    /** Agenda do profissional em um período (para montar datas e horários livres). */
    @Query("SELECT data, hora FROM consultas WHERE profissional_id = :profissionalId "
            + "AND data BETWEEN :inicio AND :fim AND status <> 'CANCELADA'")
    List<HorarioOcupado> horariosOcupadosNoPeriodo(long profissionalId, String inicio, String fim);

    /** Verificação final de disponibilidade antes de gravar o agendamento. */
    @Query("SELECT COUNT(*) FROM consultas WHERE profissional_id = :profissionalId AND data = :data "
            + "AND hora = :hora AND status <> 'CANCELADA'")
    int contarOcupacao(long profissionalId, String data, String hora);

    /** Impede o paciente de ter duas consultas no mesmo dia e horário. */
    @Query("SELECT COUNT(*) FROM consultas WHERE paciente_id = :pacienteId AND data = :data "
            + "AND hora = :hora AND status = 'AGENDADA'")
    int contarConflitosPaciente(long pacienteId, String data, String hora);

    /** Consulta futura já agendada na mesma especialidade (evita agendamentos duplicados). */
    @Query(SELECT_DETALHADA + "WHERE c.paciente_id = :pacienteId AND c.especialidade_id = :especialidadeId "
            + "AND c.status = 'AGENDADA' AND c.data >= :hoje ORDER BY c.data, c.hora LIMIT 1")
    ConsultaDetalhada buscarAgendadaPorEspecialidade(long pacienteId, long especialidadeId, String hoje);

    @Query("SELECT COUNT(*) FROM consultas WHERE status = :status")
    int contarPorStatus(String status);
}

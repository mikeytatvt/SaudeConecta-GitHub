package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.Medicamento;
import br.edu.unicid.saudeconecta.models.MedicamentoDetalhado;

@Dao
public interface MedicamentoDao {
    @Insert
    List<Long> inserirTodos(List<Medicamento> medicamentos);

    /** Medicamentos ativos com o médico responsável. */
    @Query("SELECT m.*, p.nome AS profissional_nome, e.nome AS especialidade_nome FROM medicamentos m "
            + "LEFT JOIN profissionais p ON p.id = m.profissional_id "
            + "LEFT JOIN especialidades e ON e.id = p.especialidade_id "
            + "WHERE m.paciente_id = :pacienteId AND m.ativo = 1 ORDER BY m.data_prescricao DESC, m.nome")
    List<MedicamentoDetalhado> listarDetalhadosPorPaciente(long pacienteId);

    @Query("SELECT * FROM medicamentos WHERE paciente_id = :pacienteId ORDER BY data_prescricao DESC")
    List<Medicamento> listarPorPaciente(long pacienteId);
}

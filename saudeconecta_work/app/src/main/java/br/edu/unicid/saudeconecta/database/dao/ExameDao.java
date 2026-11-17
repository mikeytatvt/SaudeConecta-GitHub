package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.Exame;
import br.edu.unicid.saudeconecta.models.ExameDetalhado;

@Dao
public interface ExameDao {
    @Insert
    long inserir(Exame exame);

    @Insert
    List<Long> inserirTodos(List<Exame> exames);

    @Update
    int atualizar(Exame exame);

    /** Exames do paciente com unidade e profissional solicitante. */
    @Query("SELECT e.*, u.nome AS unidade_nome, p.nome AS profissional_nome FROM exames e "
            + "LEFT JOIN unidades_saude u ON u.id = e.unidade_id "
            + "LEFT JOIN profissionais p ON p.id = e.profissional_solicitante_id "
            + "WHERE e.paciente_id = :pacienteId ORDER BY e.data_solicitacao DESC, e.id DESC")
    List<ExameDetalhado> listarDetalhadosPorPaciente(long pacienteId);

    @Query("SELECT nome FROM exames WHERE consulta_id = :consultaId ORDER BY nome")
    List<String> listarNomesPorConsulta(long consultaId);

    @Query("SELECT * FROM exames WHERE paciente_id = :pacienteId ORDER BY data_solicitacao DESC")
    List<Exame> listarPorPaciente(long pacienteId);
}

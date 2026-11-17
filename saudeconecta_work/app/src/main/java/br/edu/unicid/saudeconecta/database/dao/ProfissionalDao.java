package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.Profissional;

@Dao
public interface ProfissionalDao {
    @Insert
    long inserir(Profissional profissional);

    @Insert
    List<Long> inserirTodos(List<Profissional> profissionais);

    @Update
    int atualizar(Profissional profissional);

    @Query("SELECT * FROM profissionais WHERE id = :id")
    Profissional buscarPorId(long id);

    @Query("SELECT * FROM profissionais WHERE ativo = 1 ORDER BY nome")
    List<Profissional> listarAtivos();

    /** "Especialidade: Profissional" de cada profissional ativo da unidade. */
    @Query("SELECT e.nome || ': ' || p.nome FROM profissionais p "
            + "INNER JOIN especialidades e ON e.id = p.especialidade_id "
            + "WHERE p.unidade_id = :unidadeId AND p.ativo = 1 ORDER BY e.nome, p.nome")
    List<String> listarDescricoesPorUnidade(long unidadeId);

    @Query("SELECT * FROM profissionais WHERE ativo = 1 AND especialidade_id = :especialidadeId AND unidade_id = :unidadeId ORDER BY nome")
    List<Profissional> listarPorEspecialidadeEUnidade(long especialidadeId, long unidadeId);
}

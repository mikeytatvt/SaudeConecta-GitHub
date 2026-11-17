package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.Especialidade;

@Dao
public interface EspecialidadeDao {
    @Insert
    List<Long> inserirTodos(List<Especialidade> especialidades);

    @Query("SELECT * FROM especialidades ORDER BY id")
    List<Especialidade> listarTodas();

    @Query("SELECT * FROM especialidades WHERE id = :id")
    Especialidade buscarPorId(long id);
}

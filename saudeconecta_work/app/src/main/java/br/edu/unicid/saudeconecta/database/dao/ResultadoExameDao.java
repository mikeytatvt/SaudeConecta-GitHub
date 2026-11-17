package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.ResultadoExame;

@Dao
public interface ResultadoExameDao {
    @Insert
    long inserir(ResultadoExame resultado);

    @Update
    int atualizar(ResultadoExame resultado);

    @Query("SELECT * FROM resultados_exames WHERE exame_id = :exameId LIMIT 1")
    ResultadoExame buscarPorExame(long exameId);
}

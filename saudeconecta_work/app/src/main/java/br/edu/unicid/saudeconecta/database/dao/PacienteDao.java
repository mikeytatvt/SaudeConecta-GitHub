package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.Paciente;

@Dao
public interface PacienteDao {
    @Insert
    long inserir(Paciente paciente);

    @Insert
    List<Long> inserirTodos(List<Paciente> pacientes);

    @Update
    int atualizar(Paciente paciente);

    @Query("SELECT * FROM pacientes WHERE id = :id")
    Paciente buscarPorId(long id);

    @Query("SELECT * FROM pacientes WHERE usuario_id = :usuarioId LIMIT 1")
    Paciente buscarPorUsuarioId(long usuarioId);

    @Query("SELECT * FROM pacientes WHERE cpf = :cpf LIMIT 1")
    Paciente buscarPorCpf(String cpf);

    @Query("SELECT COUNT(*) FROM pacientes")
    int contar();
}

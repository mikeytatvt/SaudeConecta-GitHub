package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.Usuario;

@Dao
public interface UsuarioDao {
    @Insert
    long inserir(Usuario usuario);

    @Update
    int atualizar(Usuario usuario);

    @Query("SELECT * FROM usuarios WHERE cpf = :cpf LIMIT 1")
    Usuario buscarPorCpf(String cpf);

    @Query("SELECT * FROM usuarios WHERE email = :email LIMIT 1")
    Usuario buscarPorEmail(String email);

    @Query("SELECT * FROM usuarios WHERE id = :id")
    Usuario buscarPorId(long id);

    @Query("SELECT * FROM usuarios ORDER BY nome")
    List<Usuario> listarTodos();

    @Query("UPDATE usuarios SET ultimo_acesso = :momento WHERE id = :id")
    void atualizarUltimoAcesso(long id, long momento);

    @Query("UPDATE usuarios SET senha_hash = :hash, senha_salt = :salt WHERE id = :id")
    void atualizarSenha(long id, String hash, String salt);

    @Query("SELECT COUNT(*) FROM usuarios")
    int contar();
}

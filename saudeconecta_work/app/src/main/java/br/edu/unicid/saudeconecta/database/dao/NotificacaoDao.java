package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.Notificacao;

@Dao
public interface NotificacaoDao {
    @Insert
    long inserir(Notificacao notificacao);

    @Insert
    List<Long> inserirTodos(List<Notificacao> notificacoes);

    @Update
    int atualizar(Notificacao notificacao);

    @Delete
    int excluir(Notificacao notificacao);

    @Query("SELECT * FROM notificacoes WHERE usuario_id = :usuarioId ORDER BY data_hora DESC")
    List<Notificacao> listarPorUsuario(long usuarioId);

    @Query("SELECT COUNT(*) FROM notificacoes WHERE usuario_id = :usuarioId AND lida = 0")
    int contarNaoLidas(long usuarioId);

    @Query("UPDATE notificacoes SET lida = 1 WHERE id = :id AND usuario_id = :usuarioId")
    void marcarComoLida(long id, long usuarioId);

    @Query("UPDATE notificacoes SET lida = 1 WHERE usuario_id = :usuarioId")
    void marcarTodasComoLidas(long usuarioId);

    /** Evita lembrete duplicado para a mesma consulta. */
    @Query("SELECT COUNT(*) FROM notificacoes WHERE usuario_id = :usuarioId AND tipo = 'LEMBRETE' "
            + "AND referencia_id = :consultaId")
    int contarLembretes(long usuarioId, long consultaId);
}

package br.edu.unicid.saudeconecta.database.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import br.edu.unicid.saudeconecta.models.UnidadeSaude;

@Dao
public interface UnidadeSaudeDao {
    @Insert
    long inserir(UnidadeSaude unidade);

    @Insert
    List<Long> inserirTodos(List<UnidadeSaude> unidades);

    @Update
    int atualizar(UnidadeSaude unidade);

    @Query("SELECT * FROM unidades_saude WHERE ativo = 1 ORDER BY nome")
    List<UnidadeSaude> listarAtivas();

    /** Unidades que têm profissional ativo na especialidade escolhida. */
    @Query("SELECT DISTINCT u.* FROM unidades_saude u "
            + "INNER JOIN profissionais p ON p.unidade_id = u.id "
            + "WHERE p.especialidade_id = :especialidadeId AND p.ativo = 1 AND u.ativo = 1 ORDER BY u.nome")
    List<UnidadeSaude> listarPorEspecialidade(long especialidadeId);

    /** UBS que oferecem farmácia básica (retirada de medicamentos). */
    @Query("SELECT nome FROM unidades_saude WHERE ativo = 1 AND servicos LIKE '%Farmácia%' ORDER BY nome")
    List<String> listarNomesComFarmacia();

    @Query("SELECT * FROM unidades_saude WHERE id = :id")
    UnidadeSaude buscarPorId(long id);
}

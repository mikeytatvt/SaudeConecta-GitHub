package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;

import java.util.List;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.models.Paciente;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.utils.AppExecutors;

/** Unidades de saúde cadastradas e seus profissionais. */
public class UnidadeRepository {

    public static class Dados {
        public final List<UnidadeSaude> unidades;
        /** Unidade de referência do paciente (0 = não definida). */
        public final long unidadeReferenciaId;

        Dados(List<UnidadeSaude> unidades, long unidadeReferenciaId) {
            this.unidades = unidades;
            this.unidadeReferenciaId = unidadeReferenciaId;
        }
    }

    private final AppDatabase db;

    public UnidadeRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void carregar(long pacienteId, RepositoryCallback<Dados> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                List<UnidadeSaude> unidades = db.unidadeSaudeDao().listarAtivas();
                Paciente p = pacienteId > 0 ? db.pacienteDao().buscarPorId(pacienteId) : null;
                long referencia = p != null && p.unidadeReferenciaId != null ? p.unidadeReferenciaId : 0;
                Dados dados = new Dados(unidades, referencia);
                AppExecutors.principal(() -> callback.onSuccess(dados));
            } catch (Exception e) {
                AppExecutors.principal(() -> callback.onError("Não foi possível carregar as unidades."));
            }
        });
    }

    public void listarProfissionais(long unidadeId, RepositoryCallback<List<String>> callback) {
        AppExecutors.disco().execute(() -> {
            List<String> lista = db.profissionalDao().listarDescricoesPorUnidade(unidadeId);
            AppExecutors.principal(() -> callback.onSuccess(lista));
        });
    }
}

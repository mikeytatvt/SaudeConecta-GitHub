package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;

import java.util.List;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.models.MedicamentoDetalhado;
import br.edu.unicid.saudeconecta.utils.AppExecutors;

/** Medicamentos prescritos ao paciente e locais de retirada. */
public class MedicamentoRepository {

    /** Lista de medicamentos + UBS com farmácia básica. */
    public static class Dados {
        public final List<MedicamentoDetalhado> medicamentos;
        public final List<String> farmacias;

        Dados(List<MedicamentoDetalhado> medicamentos, List<String> farmacias) {
            this.medicamentos = medicamentos;
            this.farmacias = farmacias;
        }
    }

    private final AppDatabase db;

    public MedicamentoRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void carregar(long pacienteId, RepositoryCallback<Dados> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                Dados dados = new Dados(db.medicamentoDao().listarDetalhadosPorPaciente(pacienteId),
                        db.unidadeSaudeDao().listarNomesComFarmacia());
                AppExecutors.principal(() -> callback.onSuccess(dados));
            } catch (Exception e) {
                AppExecutors.principal(() -> callback.onError("Não foi possível carregar seus medicamentos."));
            }
        });
    }
}

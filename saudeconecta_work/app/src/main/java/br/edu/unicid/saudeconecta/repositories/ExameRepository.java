package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;

import java.io.File;
import java.util.List;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.models.ExameDetalhado;
import br.edu.unicid.saudeconecta.models.Paciente;
import br.edu.unicid.saudeconecta.models.ResultadoExame;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.PdfResultadoGenerator;

/** Exames do paciente, resultados e geração do PDF. */
public class ExameRepository {
    private final AppDatabase db;
    private final Context contexto;

    public ExameRepository(Context context) {
        contexto = context.getApplicationContext();
        db = AppDatabase.getInstance(contexto);
    }

    public void listarExames(long pacienteId, RepositoryCallback<List<ExameDetalhado>> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                List<ExameDetalhado> lista = db.exameDao().listarDetalhadosPorPaciente(pacienteId);
                AppExecutors.principal(() -> callback.onSuccess(lista));
            } catch (Exception e) {
                erro(callback, "Não foi possível carregar seus exames.");
            }
        });
    }

    public void buscarResultado(long exameId, RepositoryCallback<ResultadoExame> callback) {
        AppExecutors.disco().execute(() -> {
            ResultadoExame r = db.resultadoExameDao().buscarPorExame(exameId);
            if (r == null) {
                erro(callback, "O resultado deste exame ainda não foi liberado.");
                return;
            }
            AppExecutors.principal(() -> callback.onSuccess(r));
        });
    }

    /** Gera (ou atualiza) o PDF do resultado e devolve o arquivo. */
    public void gerarPdf(long pacienteId, ExameDetalhado detalhe, RepositoryCallback<File> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                if (detalhe.exame.pacienteId != pacienteId) {
                    erro(callback, "Exame não encontrado.");
                    return;
                }
                ResultadoExame resultado = db.resultadoExameDao().buscarPorExame(detalhe.exame.id);
                Paciente paciente = db.pacienteDao().buscarPorId(pacienteId);
                if (resultado == null || paciente == null) {
                    erro(callback, "O resultado deste exame ainda não foi liberado.");
                    return;
                }
                File arquivo = PdfResultadoGenerator.gerar(contexto, paciente, detalhe, resultado);
                if (!arquivo.getName().equals(resultado.arquivoPdf)) {
                    resultado.arquivoPdf = arquivo.getName();
                    db.resultadoExameDao().atualizar(resultado);
                }
                AppExecutors.principal(() -> callback.onSuccess(arquivo));
            } catch (Exception e) {
                erro(callback, "Não foi possível gerar o PDF.");
            }
        });
    }

    private static <T> void erro(RepositoryCallback<T> callback, String mensagem) {
        AppExecutors.principal(() -> callback.onError(mensagem));
    }
}

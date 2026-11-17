package br.edu.unicid.saudeconecta;

import android.app.Application;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.database.DatabaseSeeder;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.NotificacaoHelper;

/** Inicializa o banco, os dados simulados e os canais de notificação. */
public class SaudeConectaApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        NotificacaoHelper.criarCanais(this);
        // Mesma fila do banco usada pelo login: os dados de demonstração ficam prontos antes do primeiro acesso
        AppExecutors.disco().execute(() -> DatabaseSeeder.popularSeNecessario(AppDatabase.getInstance(this)));
    }
}

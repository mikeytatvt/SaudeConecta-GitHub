package br.edu.unicid.saudeconecta.utils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.repositories.NotificacaoRepository;

/** Recebe o alarme agendado e gera o lembrete da consulta (mesmo com o app fechado). */
public class LembreteReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        long consultaId = intent.getLongExtra(LembreteScheduler.EXTRA_CONSULTA_ID, 0);
        long usuarioId = intent.getLongExtra(LembreteScheduler.EXTRA_USUARIO_ID, 0);
        if (consultaId <= 0 || usuarioId <= 0) return;

        PendingResult resultado = goAsync();
        Context app = context.getApplicationContext();
        AppExecutors.disco().execute(() -> {
            try {
                NotificacaoRepository.criarLembreteSeNecessario(app, AppDatabase.getInstance(app), usuarioId, consultaId);
            } finally {
                resultado.finish();
            }
        });
    }
}

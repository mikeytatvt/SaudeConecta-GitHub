package br.edu.unicid.saudeconecta.utils;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.activities.NotificacoesActivity;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;

/**
 * Notificações do sistema Android (barra de status).
 * LGPD: na tela bloqueada aparece só um texto genérico, sem dados de saúde.
 */
public final class NotificacaoHelper {

    public static final String CANAL_CONSULTAS = "consultas";
    public static final String CANAL_EXAMES = "exames";
    public static final String CANAL_AVISOS = "avisos";

    private NotificacaoHelper() { }

    /** Ids das notificações do sistema (separados por faixa para não colidirem). */
    public static int idConfirmacao(long consultaId) { return (int) (100_000 + consultaId); }
    public static int idLembrete(long consultaId) { return (int) (200_000 + consultaId); }
    public static int idGeral(long notificacaoId) { return (int) (300_000 + notificacaoId); }

    public static void criarCanais(Context context) {
        NotificationManager gerenciador = context.getSystemService(NotificationManager.class);
        if (gerenciador == null) return;
        gerenciador.createNotificationChannel(canal(context, CANAL_CONSULTAS, R.string.canal_consultas,
                R.string.canal_consultas_desc, NotificationManager.IMPORTANCE_HIGH));
        gerenciador.createNotificationChannel(canal(context, CANAL_EXAMES, R.string.canal_exames,
                R.string.canal_exames_desc, NotificationManager.IMPORTANCE_DEFAULT));
        gerenciador.createNotificationChannel(canal(context, CANAL_AVISOS, R.string.canal_avisos,
                R.string.canal_avisos_desc, NotificationManager.IMPORTANCE_DEFAULT));
    }

    private static NotificationChannel canal(Context c, String id, int nome, int descricao, int importancia) {
        NotificationChannel canal = new NotificationChannel(id, c.getString(nome), importancia);
        canal.setDescription(c.getString(descricao));
        canal.setLockscreenVisibility(android.app.Notification.VISIBILITY_PRIVATE);
        return canal;
    }

    /** No Android 13+ o usuário precisa autorizar notificações. */
    public static boolean podeNotificar(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
                && context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return NotificationManagerCompat.from(context).areNotificationsEnabled();
    }

    @SuppressLint("MissingPermission") // permissão verificada em podeNotificar()
    public static void mostrar(Context context, int id, String titulo, String mensagem, String tipo) {
        if (!podeNotificar(context)) return;

        String canal = TipoNotificacao.RESULTADO.equals(tipo) ? CANAL_EXAMES
                : TipoNotificacao.AVISO.equals(tipo) ? CANAL_AVISOS : CANAL_CONSULTAS;

        // Ao tocar: abre a lista de notificações, com o Início por baixo (botão voltar funciona)
        Intent abrir = new Intent(context, NotificacoesActivity.class);
        PendingIntent acao = TaskStackBuilder.create(context)
                .addNextIntentWithParentStack(abrir)
                .getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder publica = new NotificationCompat.Builder(context, canal)
                .setSmallIcon(R.drawable.ic_notifications)
                .setColor(context.getColor(R.color.verde_escuro))
                .setContentTitle(context.getString(R.string.app_name))
                .setContentText(context.getString(R.string.notificacao_publica));

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, canal)
                .setSmallIcon(R.drawable.ic_notifications)
                .setColor(context.getColor(R.color.verde_escuro))
                .setContentTitle(titulo)
                .setContentText(mensagem)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(mensagem))
                .setContentIntent(acao)
                .setAutoCancel(true)
                .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
                .setPublicVersion(publica.build())
                .setPriority(NotificationCompat.PRIORITY_DEFAULT);

        NotificationManagerCompat.from(context).notify(id, builder.build());
    }

    public static void remover(Context context, int id) {
        NotificationManagerCompat.from(context).cancel(id);
    }
}

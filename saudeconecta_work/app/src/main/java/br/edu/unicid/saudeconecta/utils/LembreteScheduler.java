package br.edu.unicid.saudeconecta.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;

/**
 * Agenda o lembrete de consulta para 24 horas antes do horário marcado.
 * Se a consulta for em menos de 24 horas, o lembrete sai em poucos segundos.
 */
public final class LembreteScheduler {

    public static final String EXTRA_CONSULTA_ID = "extra_consulta_id";
    public static final String EXTRA_USUARIO_ID = "extra_usuario_id";
    private static final long ANTECEDENCIA_HORAS = 24;

    private LembreteScheduler() { }

    public static void agendar(Context context, long consultaId, long usuarioId, String data, String hora) {
        LocalDateTime consulta = LocalDateTime.of(LocalDate.parse(data), LocalTime.parse(hora));
        LocalDateTime agora = LocalDateTime.now();
        if (!consulta.isAfter(agora)) return;

        LocalDateTime disparo = consulta.minusHours(ANTECEDENCIA_HORAS);
        if (disparo.isBefore(agora)) disparo = agora.plusSeconds(10);
        long quando = disparo.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();

        AlarmManager alarmes = context.getSystemService(AlarmManager.class);
        if (alarmes == null) return;
        // Alarme não exato: não exige permissão especial e economiza bateria
        alarmes.set(AlarmManager.RTC_WAKEUP, quando, intencao(context, consultaId, usuarioId, PendingIntent.FLAG_UPDATE_CURRENT));
    }

    public static void cancelar(Context context, long consultaId) {
        PendingIntent pendente = intencao(context, consultaId, 0, PendingIntent.FLAG_NO_CREATE);
        AlarmManager alarmes = context.getSystemService(AlarmManager.class);
        if (pendente != null && alarmes != null) {
            alarmes.cancel(pendente);
            pendente.cancel();
        }
        NotificacaoHelper.remover(context, NotificacaoHelper.idLembrete(consultaId));
    }

    private static PendingIntent intencao(Context context, long consultaId, long usuarioId, int flag) {
        Intent intent = new Intent(context, LembreteReceiver.class);
        intent.putExtra(EXTRA_CONSULTA_ID, consultaId);
        intent.putExtra(EXTRA_USUARIO_ID, usuarioId);
        return PendingIntent.getBroadcast(context, (int) consultaId, intent, flag | PendingIntent.FLAG_IMMUTABLE);
    }
}

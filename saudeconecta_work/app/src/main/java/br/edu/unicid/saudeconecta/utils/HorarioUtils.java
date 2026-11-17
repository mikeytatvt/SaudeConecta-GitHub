package br.edu.unicid.saudeconecta.utils;

import java.text.Normalizer;
import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Interpreta o horário de funcionamento ("Seg a Sex, 07h às 19h") e normaliza textos de busca. */
public final class HorarioUtils {
    private static final Pattern FAIXA = Pattern.compile("(\\d{1,2})h\\s*às\\s*(\\d{1,2})h");

    private HorarioUtils() { }

    /** @return TRUE aberta, FALSE fechada, null quando o horário não pôde ser interpretado. */
    public static Boolean estaAberta(String horario, LocalDateTime agora) {
        if (horario == null) return null;
        Matcher m = FAIXA.matcher(horario);
        if (!m.find()) return null;
        int abre = Integer.parseInt(m.group(1));
        int fecha = Integer.parseInt(m.group(2));
        DayOfWeek dia = agora.getDayOfWeek();
        boolean diaUtil = dia != DayOfWeek.SATURDAY && dia != DayOfWeek.SUNDAY;
        boolean atendeSabado = horario.toLowerCase(Locale.ROOT).contains("sáb");
        if (!diaUtil && !(dia == DayOfWeek.SATURDAY && atendeSabado)) return false;
        int hora = agora.getHour();
        return hora >= abre && hora < fecha;
    }

    /** "Farmácia Básica" -> "farmacia basica" (busca sem acentos e sem maiúsculas). */
    public static String normalizar(String texto) {
        if (texto == null) return "";
        String semAcento = Normalizer.normalize(texto, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        return semAcento.toLowerCase(Locale.ROOT).trim();
    }
}

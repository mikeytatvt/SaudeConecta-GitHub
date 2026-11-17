package br.edu.unicid.saudeconecta.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Datas no padrão brasileiro e regras de dias úteis das UBS. */
public final class DateUtils {
    public static final Locale PT_BR = new Locale("pt", "BR");
    private static final DateTimeFormatter BR = DateTimeFormatter.ofPattern("dd/MM/yyyy", PT_BR);

    private DateUtils() { }

    public static String formatarBr(LocalDate data) {
        return data.format(BR);
    }

    /** yyyy-MM-dd -> dd/MM/yyyy */
    public static String isoParaBr(String iso) {
        if (iso == null) return "";
        try {
            return LocalDate.parse(iso).format(BR);
        } catch (Exception e) {
            return iso;
        }
    }

    /** yyyy-MM-dd -> "segunda-feira, 16/11/2026" */
    public static String isoParaBrComDiaSemana(String iso) {
        try {
            return LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("EEEE, dd/MM/yyyy", PT_BR));
        } catch (Exception e) {
            return isoParaBr(iso);
        }
    }

    /** "Seg", "Ter", "Qua"... */
    public static String diaSemanaCurto(LocalDate data) {
        String nome = data.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        return nome.isEmpty() ? nome : nome.substring(0, 1).toUpperCase(PT_BR) + nome.substring(1);
    }

    /** "18/05" */
    public static String diaMes(LocalDate data) {
        return data.format(DateTimeFormatter.ofPattern("dd/MM", PT_BR));
    }

    public static boolean isDiaUtil(LocalDate data) {
        DayOfWeek d = data.getDayOfWeek();
        return d != DayOfWeek.SATURDAY && d != DayOfWeek.SUNDAY;
    }

    /** Retorna a própria data se for dia útil, senão o próximo dia útil. */
    public static LocalDate proximoDiaUtil(LocalDate data) {
        LocalDate d = data;
        while (!isDiaUtil(d)) d = d.plusDays(1);
        return d;
    }

    public static List<LocalDate> proximosDiasUteis(LocalDate inicio, int quantidade) {
        List<LocalDate> dias = new ArrayList<>();
        LocalDate d = inicio;
        while (dias.size() < quantidade) {
            if (isDiaUtil(d)) dias.add(d);
            d = d.plusDays(1);
        }
        return dias;
    }

    /** Dias úteis dentro dos últimos N dias corridos (ordem crescente, sem incluir hoje). */
    public static List<LocalDate> diasUteisAnteriores(LocalDate referencia, int diasCorridos) {
        List<LocalDate> dias = new ArrayList<>();
        for (int i = diasCorridos; i >= 1; i--) {
            LocalDate d = referencia.minusDays(i);
            if (isDiaUtil(d)) dias.add(d);
        }
        return dias;
    }
}

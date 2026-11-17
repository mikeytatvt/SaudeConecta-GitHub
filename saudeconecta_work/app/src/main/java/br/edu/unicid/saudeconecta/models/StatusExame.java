package br.edu.unicid.saudeconecta.models;

/** Situações possíveis de um exame. */
public final class StatusExame {
    public static final String SOLICITADO = "SOLICITADO";
    public static final String AGENDADO = "AGENDADO";
    public static final String REALIZADO = "REALIZADO";
    public static final String RESULTADO_DISPONIVEL = "RESULTADO_DISPONIVEL";
    private StatusExame() { }
}

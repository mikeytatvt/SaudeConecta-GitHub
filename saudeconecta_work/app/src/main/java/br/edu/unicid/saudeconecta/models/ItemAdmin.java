package br.edu.unicid.saudeconecta.models;

/** Linha genérica das listas administrativas (modelo de tela, não é tabela). */
public class ItemAdmin {
    public static final int ETIQUETA_NENHUMA = 0;
    public static final int ETIQUETA_VERDE = 1;
    public static final int ETIQUETA_AZUL = 2;
    public static final int ETIQUETA_VERMELHA = 3;
    public static final int ETIQUETA_AMARELA = 4;

    public long id;
    public String titulo;
    public String subtitulo;
    public String detalhe;
    public String etiqueta;
    public int estiloEtiqueta = ETIQUETA_NENHUMA;
    /** Texto do botão verde (null = sem botão). */
    public String acao1;
    /** Texto do botão vermelho (null = sem botão). */
    public String acao2;
    /** Objeto original (unidade, profissional, paciente ou consulta). */
    public Object origem;
}

package br.edu.unicid.saudeconecta.models;

/** Resultado de um login bem-sucedido (não é tabela). */
public class UsuarioAutenticado {
    public final long usuarioId;
    public final String nome;
    public final String perfil;
    public final long pacienteId;

    public UsuarioAutenticado(long usuarioId, String nome, String perfil, long pacienteId) {
        this.usuarioId = usuarioId;
        this.nome = nome;
        this.perfil = perfil;
        this.pacienteId = pacienteId;
    }
}

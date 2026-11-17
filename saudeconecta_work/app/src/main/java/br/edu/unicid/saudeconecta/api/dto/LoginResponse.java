package br.edu.unicid.saudeconecta.api.dto;

public class LoginResponse {
    public long usuarioId;
    public long pacienteId;
    public String nome;
    public String perfil;
    /** Token JWT emitido pela API. */
    public String token;
}

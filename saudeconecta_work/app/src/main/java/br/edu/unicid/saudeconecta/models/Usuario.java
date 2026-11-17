package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "usuarios": credenciais de acesso e perfil (PACIENTE ou GESTOR). */
@Entity(tableName = "usuarios",
        indices = {@Index(value = "cpf", unique = true), @Index(value = "email", unique = true)})
public class Usuario {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nome = "";

    /** Somente dígitos. */
    @NonNull
    public String cpf = "";

    @NonNull
    public String email = "";

    public String telefone;

    /** Senha nunca é salva em texto puro: PBKDF2 + salt. */
    @NonNull
    @ColumnInfo(name = "senha_hash")
    public String senhaHash = "";

    @NonNull
    @ColumnInfo(name = "senha_salt")
    public String senhaSalt = "";

    @NonNull
    public String perfil = Perfil.PACIENTE;

    public boolean ativo = true;

    @ColumnInfo(name = "criado_em")
    public long criadoEm;

    @ColumnInfo(name = "ultimo_acesso")
    public long ultimoAcesso;
}

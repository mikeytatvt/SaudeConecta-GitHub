package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/**
 * Tabela "pacientes". Um paciente pode existir sem conta no app (cadastrado na UBS);
 * quando cria a conta, o campo usuario_id é vinculado.
 */
@Entity(tableName = "pacientes",
        foreignKeys = {
                @ForeignKey(entity = Usuario.class, parentColumns = "id",
                        childColumns = "usuario_id", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = UnidadeSaude.class, parentColumns = "id",
                        childColumns = "unidade_referencia_id", onDelete = ForeignKey.SET_NULL)},
        indices = {
                @Index(value = "usuario_id", unique = true),
                @Index(value = "cpf", unique = true),
                @Index("unidade_referencia_id")})
public class Paciente {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "usuario_id")
    public Long usuarioId;

    @NonNull
    public String nome = "";

    @NonNull
    public String cpf = "";

    /** Formato ISO yyyy-MM-dd. */
    @ColumnInfo(name = "data_nascimento")
    public String dataNascimento;

    public String telefone;
    public String email;
    public String endereco;

    @ColumnInfo(name = "cartao_sus")
    public String cartaoSus;

    @ColumnInfo(name = "unidade_referencia_id")
    public Long unidadeReferenciaId;

    @ColumnInfo(name = "criado_em")
    public long criadoEm;
}

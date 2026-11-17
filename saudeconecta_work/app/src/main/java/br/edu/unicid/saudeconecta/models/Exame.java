package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "exames". */
@Entity(tableName = "exames",
        foreignKeys = {
                @ForeignKey(entity = Paciente.class, parentColumns = "id",
                        childColumns = "paciente_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Consulta.class, parentColumns = "id",
                        childColumns = "consulta_id", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = UnidadeSaude.class, parentColumns = "id",
                        childColumns = "unidade_id", onDelete = ForeignKey.SET_NULL),
                @ForeignKey(entity = Profissional.class, parentColumns = "id",
                        childColumns = "profissional_solicitante_id", onDelete = ForeignKey.SET_NULL)},
        indices = {
                @Index("paciente_id"),
                @Index("consulta_id"),
                @Index("unidade_id"),
                @Index("profissional_solicitante_id"),
                @Index("status")})
public class Exame {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "paciente_id")
    public long pacienteId;

    @ColumnInfo(name = "consulta_id")
    public Long consultaId;

    @ColumnInfo(name = "unidade_id")
    public Long unidadeId;

    @ColumnInfo(name = "profissional_solicitante_id")
    public Long profissionalSolicitanteId;

    @NonNull
    public String nome = "";

    /** Laboratorial, Imagem, Cardiológico... */
    public String tipo;

    @ColumnInfo(name = "data_solicitacao")
    public String dataSolicitacao;

    @ColumnInfo(name = "data_realizacao")
    public String dataRealizacao;

    @NonNull
    public String status = StatusExame.SOLICITADO;

    /** Orientações de preparo (jejum etc.). */
    public String preparo;

    @ColumnInfo(name = "criado_em")
    public long criadoEm;

    public static Exame criar(long pacienteId, Long consultaId, Long unidadeId, Long profissionalId,
                              String nome, String tipo, String dataSolicitacao, String dataRealizacao,
                              String status) {
        Exame e = new Exame();
        e.pacienteId = pacienteId;
        e.consultaId = consultaId;
        e.unidadeId = unidadeId;
        e.profissionalSolicitanteId = profissionalId;
        e.nome = nome;
        e.tipo = tipo;
        e.dataSolicitacao = dataSolicitacao;
        e.dataRealizacao = dataRealizacao;
        e.status = status;
        e.criadoEm = System.currentTimeMillis();
        return e;
    }
}

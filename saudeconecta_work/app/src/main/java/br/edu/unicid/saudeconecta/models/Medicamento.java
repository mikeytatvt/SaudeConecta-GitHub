package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "medicamentos" (prescrições do paciente). */
@Entity(tableName = "medicamentos",
        foreignKeys = {
                @ForeignKey(entity = Paciente.class, parentColumns = "id",
                        childColumns = "paciente_id", onDelete = ForeignKey.CASCADE),
                @ForeignKey(entity = Profissional.class, parentColumns = "id",
                        childColumns = "profissional_id", onDelete = ForeignKey.SET_NULL)},
        indices = {@Index("paciente_id"), @Index("profissional_id")})
public class Medicamento {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "paciente_id")
    public long pacienteId;

    @ColumnInfo(name = "profissional_id")
    public Long profissionalId;

    @NonNull
    public String nome = "";

    public String dosagem;
    public String orientacao;
    public String quantidade;

    @ColumnInfo(name = "data_prescricao")
    public String dataPrescricao;

    public boolean ativo = true;

    public static Medicamento criar(long pacienteId, Long profissionalId, String nome, String dosagem,
                                    String orientacao, String quantidade, String dataPrescricao) {
        Medicamento m = new Medicamento();
        m.pacienteId = pacienteId;
        m.profissionalId = profissionalId;
        m.nome = nome;
        m.dosagem = dosagem;
        m.orientacao = orientacao;
        m.quantidade = quantidade;
        m.dataPrescricao = dataPrescricao;
        return m;
    }
}

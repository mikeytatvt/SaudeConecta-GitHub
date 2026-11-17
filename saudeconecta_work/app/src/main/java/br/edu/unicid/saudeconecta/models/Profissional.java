package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "profissionais". */
@Entity(tableName = "profissionais",
        foreignKeys = {
                @ForeignKey(entity = Especialidade.class, parentColumns = "id", childColumns = "especialidade_id"),
                @ForeignKey(entity = UnidadeSaude.class, parentColumns = "id", childColumns = "unidade_id")},
        indices = {
                @Index("especialidade_id"),
                @Index("unidade_id"),
                @Index(value = "registro", unique = true)})
public class Profissional {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nome = "";

    /** CRM / CRO fictício. */
    @NonNull
    public String registro = "";

    @ColumnInfo(name = "especialidade_id")
    public long especialidadeId;

    @ColumnInfo(name = "unidade_id")
    public long unidadeId;

    public boolean ativo = true;

    public static Profissional criar(String nome, String registro, long especialidadeId, long unidadeId) {
        Profissional p = new Profissional();
        p.nome = nome;
        p.registro = registro;
        p.especialidadeId = especialidadeId;
        p.unidadeId = unidadeId;
        return p;
    }

    @NonNull
    @Override
    public String toString() {
        return nome;
    }
}

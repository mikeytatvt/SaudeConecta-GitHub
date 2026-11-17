package br.edu.unicid.saudeconecta.models;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "resultados_exames" (1 resultado por exame). */
@Entity(tableName = "resultados_exames",
        foreignKeys = @ForeignKey(entity = Exame.class, parentColumns = "id",
                childColumns = "exame_id", onDelete = ForeignKey.CASCADE),
        indices = @Index(value = "exame_id", unique = true))
public class ResultadoExame {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "exame_id")
    public long exameId;

    public String resumo;
    public String valores;
    public String observacao;

    /** Nome do arquivo PDF gerado no armazenamento interno do app. */
    @ColumnInfo(name = "arquivo_pdf")
    public String arquivoPdf;

    @ColumnInfo(name = "data_liberacao")
    public String dataLiberacao;

    public static ResultadoExame criar(long exameId, String resumo, String valores, String observacao,
                                       String arquivoPdf, String dataLiberacao) {
        ResultadoExame r = new ResultadoExame();
        r.exameId = exameId;
        r.resumo = resumo;
        r.valores = valores;
        r.observacao = observacao;
        r.arquivoPdf = arquivoPdf;
        r.dataLiberacao = dataLiberacao;
        return r;
    }
}

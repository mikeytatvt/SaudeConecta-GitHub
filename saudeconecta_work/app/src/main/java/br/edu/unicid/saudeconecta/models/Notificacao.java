package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "notificacoes". */
@Entity(tableName = "notificacoes",
        foreignKeys = @ForeignKey(entity = Usuario.class, parentColumns = "id",
                childColumns = "usuario_id", onDelete = ForeignKey.CASCADE),
        indices = @Index("usuario_id"))
public class Notificacao {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "usuario_id")
    public long usuarioId;

    @NonNull
    public String titulo = "";

    @NonNull
    public String mensagem = "";

    @NonNull
    public String tipo = TipoNotificacao.AVISO;

    @ColumnInfo(name = "data_hora")
    public long dataHora;

    public boolean lida;

    /** Id da consulta/exame relacionado (opcional). */
    @ColumnInfo(name = "referencia_id")
    public Long referenciaId;

    public static Notificacao criar(long usuarioId, String titulo, String mensagem, String tipo,
                                    long dataHora, boolean lida, Long referenciaId) {
        Notificacao n = new Notificacao();
        n.usuarioId = usuarioId;
        n.titulo = titulo;
        n.mensagem = mensagem;
        n.tipo = tipo;
        n.dataHora = dataHora;
        n.lida = lida;
        n.referenciaId = referenciaId;
        return n;
    }
}

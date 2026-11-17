package br.edu.unicid.saudeconecta.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/** Exame com o nome da unidade e do profissional solicitante (resultado de JOIN, não é tabela). */
public class ExameDetalhado {
    @Embedded
    public Exame exame;

    @ColumnInfo(name = "unidade_nome")
    public String unidadeNome;

    @ColumnInfo(name = "profissional_nome")
    public String profissionalNome;
}

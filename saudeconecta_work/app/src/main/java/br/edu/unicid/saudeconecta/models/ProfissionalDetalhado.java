package br.edu.unicid.saudeconecta.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/** Profissional com especialidade, unidade e consultas futuras (resultado de JOIN). */
public class ProfissionalDetalhado {
    @Embedded
    public Profissional profissional;

    @ColumnInfo(name = "especialidade_nome")
    public String especialidadeNome;

    @ColumnInfo(name = "unidade_nome")
    public String unidadeNome;

    @ColumnInfo(name = "agendadas_futuras")
    public int agendadasFuturas;
}

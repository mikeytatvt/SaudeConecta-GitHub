package br.edu.unicid.saudeconecta.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/** Paciente com unidade de referência e totais de atendimentos (resultado de JOIN). */
public class PacienteResumo {
    @Embedded
    public Paciente paciente;

    @ColumnInfo(name = "unidade_nome")
    public String unidadeNome;

    @ColumnInfo(name = "atendimentos")
    public int atendimentos;

    @ColumnInfo(name = "agendadas")
    public int agendadas;
}

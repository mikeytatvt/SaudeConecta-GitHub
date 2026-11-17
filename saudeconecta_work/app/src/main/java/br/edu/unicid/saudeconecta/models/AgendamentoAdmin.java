package br.edu.unicid.saudeconecta.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/** Consulta com nomes do paciente, profissional, especialidade e unidade (visão do gestor). */
public class AgendamentoAdmin {
    @Embedded
    public Consulta consulta;

    @ColumnInfo(name = "paciente_nome")
    public String pacienteNome;

    @ColumnInfo(name = "paciente_usuario_id")
    public Long pacienteUsuarioId;

    @ColumnInfo(name = "profissional_nome")
    public String profissionalNome;

    @ColumnInfo(name = "especialidade_nome")
    public String especialidadeNome;

    @ColumnInfo(name = "unidade_nome")
    public String unidadeNome;
}

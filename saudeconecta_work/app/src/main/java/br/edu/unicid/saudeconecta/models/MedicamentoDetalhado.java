package br.edu.unicid.saudeconecta.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/** Medicamento com o nome e a especialidade do médico que prescreveu (resultado de JOIN). */
public class MedicamentoDetalhado {
    @Embedded
    public Medicamento medicamento;

    @ColumnInfo(name = "profissional_nome")
    public String profissionalNome;

    @ColumnInfo(name = "especialidade_nome")
    public String especialidadeNome;
}

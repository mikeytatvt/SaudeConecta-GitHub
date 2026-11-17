package br.edu.unicid.saudeconecta.models;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;

/** Consulta com os nomes do profissional, especialidade e unidade (resultado de JOIN, não é tabela). */
public class ConsultaDetalhada {
    @Embedded
    public Consulta consulta;

    @ColumnInfo(name = "profissional_nome")
    public String profissionalNome;

    @ColumnInfo(name = "especialidade_nome")
    public String especialidadeNome;

    @ColumnInfo(name = "unidade_nome")
    public String unidadeNome;

    @ColumnInfo(name = "unidade_endereco")
    public String unidadeEndereco;

    @ColumnInfo(name = "unidade_bairro")
    public String unidadeBairro;

    @ColumnInfo(name = "unidade_telefone")
    public String unidadeTelefone;
}

package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "unidades_saude" (UBS). */
@Entity(tableName = "unidades_saude", indices = {@Index(value = "nome", unique = true)})
public class UnidadeSaude {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nome = "";

    @NonNull
    public String endereco = "";

    public String bairro;
    public String telefone;

    @ColumnInfo(name = "horario_funcionamento")
    public String horarioFuncionamento;

    /** Serviços separados por vírgula. */
    public String servicos;

    public boolean ativo = true;

    public static UnidadeSaude criar(String nome, String endereco, String bairro, String telefone,
                                     String horario, String servicos) {
        UnidadeSaude u = new UnidadeSaude();
        u.nome = nome;
        u.endereco = endereco;
        u.bairro = bairro;
        u.telefone = telefone;
        u.horarioFuncionamento = horario;
        u.servicos = servicos;
        return u;
    }
}

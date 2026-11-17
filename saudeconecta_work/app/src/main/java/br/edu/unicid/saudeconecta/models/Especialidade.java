package br.edu.unicid.saudeconecta.models;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.Index;
import androidx.room.PrimaryKey;

/** Tabela "especialidades". */
@Entity(tableName = "especialidades", indices = {@Index(value = "nome", unique = true)})
public class Especialidade {
    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String nome = "";

    public String descricao;

    public static Especialidade criar(String nome, String descricao) {
        Especialidade e = new Especialidade();
        e.nome = nome;
        e.descricao = descricao;
        return e;
    }

    @NonNull
    @Override
    public String toString() {
        return nome; // usado diretamente em listas/dropdowns
    }
}

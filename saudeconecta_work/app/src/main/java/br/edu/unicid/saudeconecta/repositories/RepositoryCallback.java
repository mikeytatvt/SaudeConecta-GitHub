package br.edu.unicid.saudeconecta.repositories;

/** Retorno assíncrono dos repositórios (sempre entregue na thread principal). */
public interface RepositoryCallback<T> {
    void onSuccess(T resultado);

    void onError(String mensagem);
}

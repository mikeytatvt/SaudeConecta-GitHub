package br.edu.unicid.saudeconecta.api;

import br.edu.unicid.saudeconecta.api.dto.CadastroRequest;
import br.edu.unicid.saudeconecta.api.dto.LoginRequest;
import br.edu.unicid.saudeconecta.api.dto.LoginResponse;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/** Endpoints da API REST (Spring Boot). Novos endpoints são adicionados a cada etapa. */
public interface ApiService {
    @POST("auth/login")
    Call<LoginResponse> login(@Body LoginRequest requisicao);

    @POST("auth/cadastro")
    Call<Void> cadastrar(@Body CadastroRequest requisicao);
}

package br.edu.unicid.saudeconecta.api;

import java.util.concurrent.TimeUnit;

import br.edu.unicid.saudeconecta.BuildConfig;
import br.edu.unicid.saudeconecta.utils.AppConfig;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

/** Cliente Retrofit para a API Spring Boot. */
public final class ApiClient {
    private static ApiService servico;

    private ApiClient() { }

    public static synchronized ApiService getServico() {
        if (servico == null) {
            HttpLoggingInterceptor log = new HttpLoggingInterceptor();
            // LGPD: nunca registrar o corpo (dados de saúde) nos logs
            log.setLevel(BuildConfig.DEBUG ? HttpLoggingInterceptor.Level.BASIC : HttpLoggingInterceptor.Level.NONE);

            OkHttpClient cliente = new OkHttpClient.Builder()
                    .addInterceptor(log)
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(20, TimeUnit.SECONDS)
                    .build();

            servico = new Retrofit.Builder()
                    .baseUrl(AppConfig.API_BASE_URL)
                    .client(cliente)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
                    .create(ApiService.class);
        }
        return servico;
    }
}

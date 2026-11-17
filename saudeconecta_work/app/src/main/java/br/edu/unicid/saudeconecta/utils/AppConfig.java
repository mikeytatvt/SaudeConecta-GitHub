package br.edu.unicid.saudeconecta.utils;

/** Configurações gerais. */
public final class AppConfig {
    /**
     * false = usa o banco local (Room/SQLite) com dados simulados.
     * true  = usa a API Spring Boot + MySQL (implementada em etapa posterior).
     */
    public static final boolean USAR_API_REMOTA = false;

    /** 10.0.2.2 é o "localhost" do computador quando acessado pelo emulador. */
    public static final String API_BASE_URL = "http://10.0.2.2:8080/api/";

    private AppConfig() { }
}

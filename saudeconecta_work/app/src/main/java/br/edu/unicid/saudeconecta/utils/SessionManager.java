package br.edu.unicid.saudeconecta.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.concurrent.TimeUnit;

import br.edu.unicid.saudeconecta.models.Perfil;
import br.edu.unicid.saudeconecta.models.UsuarioAutenticado;

/**
 * Sessão do usuário em SharedPreferences.
 * LGPD: guarda apenas identificadores mínimos (nunca senha, CPF ou dados clínicos).
 */
public class SessionManager {
    private static final String ARQUIVO = "saude_conecta_sessao";
    private static final String USUARIO_ID = "usuario_id";
    private static final String NOME = "nome";
    private static final String PERFIL = "perfil";
    private static final String PACIENTE_ID = "paciente_id";
    private static final String LOGIN_EM = "login_em";
    private static final String TENTATIVAS = "tentativas_falhas";
    private static final String BLOQUEADO_ATE = "bloqueado_ate";

    private static final long DURACAO_SESSAO = TimeUnit.HOURS.toMillis(8);
    private static final int MAX_TENTATIVAS = 5;
    private static final long TEMPO_BLOQUEIO = TimeUnit.SECONDS.toMillis(60);

    private final SharedPreferences prefs;

    public SessionManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE);
    }

    public void criarSessao(UsuarioAutenticado usuario) {
        prefs.edit()
                .putLong(USUARIO_ID, usuario.usuarioId)
                .putString(NOME, usuario.nome)
                .putString(PERFIL, usuario.perfil)
                .putLong(PACIENTE_ID, usuario.pacienteId)
                .putLong(LOGIN_EM, System.currentTimeMillis())
                .apply();
    }

    /** Sessão válida por 8 horas; depois disso exige novo login. */
    public boolean isLogado() {
        if (getUsuarioId() <= 0) return false;
        long loginEm = prefs.getLong(LOGIN_EM, 0);
        if (System.currentTimeMillis() - loginEm > DURACAO_SESSAO) {
            encerrarSessao();
            return false;
        }
        return true;
    }

    public long getUsuarioId() { return prefs.getLong(USUARIO_ID, 0); }
    public long getPacienteId() { return prefs.getLong(PACIENTE_ID, 0); }
    public String getNome() { return prefs.getString(NOME, ""); }
    public String getPerfil() { return prefs.getString(PERFIL, Perfil.PACIENTE); }
    public boolean isGestor() { return Perfil.GESTOR.equals(getPerfil()); }

    public void encerrarSessao() {
        prefs.edit()
                .remove(USUARIO_ID).remove(NOME).remove(PERFIL)
                .remove(PACIENTE_ID).remove(LOGIN_EM)
                .apply();
    }

    // ---------- Proteção contra tentativas repetidas de login ----------

    public boolean isLoginBloqueado() {
        return System.currentTimeMillis() < prefs.getLong(BLOQUEADO_ATE, 0);
    }

    public long segundosRestantesBloqueio() {
        long restante = prefs.getLong(BLOQUEADO_ATE, 0) - System.currentTimeMillis();
        return Math.max(1, TimeUnit.MILLISECONDS.toSeconds(restante));
    }

    /** @return tentativas restantes antes do bloqueio (0 = acabou de bloquear). */
    public int registrarFalhaLogin() {
        int tentativas = prefs.getInt(TENTATIVAS, 0) + 1;
        if (tentativas >= MAX_TENTATIVAS) {
            prefs.edit()
                    .putInt(TENTATIVAS, 0)
                    .putLong(BLOQUEADO_ATE, System.currentTimeMillis() + TEMPO_BLOQUEIO)
                    .apply();
            return 0;
        }
        prefs.edit().putInt(TENTATIVAS, tentativas).apply();
        return MAX_TENTATIVAS - tentativas;
    }

    public void limparTentativas() {
        prefs.edit().putInt(TENTATIVAS, 0).putLong(BLOQUEADO_ATE, 0).apply();
    }
}

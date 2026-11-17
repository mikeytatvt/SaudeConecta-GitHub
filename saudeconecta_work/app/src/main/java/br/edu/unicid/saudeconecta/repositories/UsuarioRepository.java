package br.edu.unicid.saudeconecta.repositories;

import android.content.Context;
import android.database.sqlite.SQLiteConstraintException;

import java.util.Locale;

import br.edu.unicid.saudeconecta.database.AppDatabase;
import br.edu.unicid.saudeconecta.models.Notificacao;
import br.edu.unicid.saudeconecta.models.Paciente;
import br.edu.unicid.saudeconecta.models.Perfil;
import br.edu.unicid.saudeconecta.models.TipoNotificacao;
import br.edu.unicid.saudeconecta.models.Usuario;
import br.edu.unicid.saudeconecta.models.UsuarioAutenticado;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.PasswordUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/**
 * Regras de login, cadastro e recuperação de senha.
 * Hoje usa o banco local; quando AppConfig.USAR_API_REMOTA = true, as chamadas passam
 * a usar o ApiService (Retrofit) sem mudar as telas.
 */
public class UsuarioRepository {
    private static final String MSG_CREDENCIAIS = "CPF ou senha incorretos.";
    private static final String SALT_FICTICIO = "AAAAAAAAAAAAAAAAAAAAAA==";

    private final AppDatabase db;

    public UsuarioRepository(Context context) {
        db = AppDatabase.getInstance(context);
    }

    public void autenticar(String cpf, String senha, RepositoryCallback<UsuarioAutenticado> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                Usuario u = db.usuarioDao().buscarPorCpf(Validators.somenteDigitos(cpf));
                if (u == null) {
                    PasswordUtils.gerarHash(senha, SALT_FICTICIO); // mesmo tempo de resposta: não revela se o CPF existe
                    erro(callback, MSG_CREDENCIAIS);
                    return;
                }
                if (!PasswordUtils.conferir(senha, u.senhaSalt, u.senhaHash)) {
                    erro(callback, MSG_CREDENCIAIS);
                    return;
                }
                if (!u.ativo) {
                    erro(callback, "Esta conta está desativada. Procure sua unidade de saúde.");
                    return;
                }
                db.usuarioDao().atualizarUltimoAcesso(u.id, System.currentTimeMillis());
                Paciente p = db.pacienteDao().buscarPorUsuarioId(u.id);
                UsuarioAutenticado resultado = new UsuarioAutenticado(u.id, u.nome, u.perfil, p != null ? p.id : 0);
                AppExecutors.principal(() -> callback.onSuccess(resultado));
            } catch (Exception e) {
                erro(callback, "Não foi possível entrar agora. Tente novamente.");
            }
        });
    }

    public void cadastrarPaciente(String nome, String cpf, String dataNascimentoIso, String telefone,
                                  String email, String endereco, String senha,
                                  RepositoryCallback<String> callback) {
        AppExecutors.disco().execute(() -> {
            String cpfDigitos = Validators.somenteDigitos(cpf);
            String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
            try {
                if (db.usuarioDao().buscarPorCpf(cpfDigitos) != null) {
                    erro(callback, "Já existe uma conta com este CPF. Use \"Esqueci minha senha\".");
                    return;
                }
                if (db.usuarioDao().buscarPorEmail(emailNormalizado) != null) {
                    erro(callback, "Este e-mail já está em uso por outra conta.");
                    return;
                }
                String salt = PasswordUtils.gerarSalt();
                String hash = PasswordUtils.gerarHash(senha, salt);
                long agora = System.currentTimeMillis();

                db.runInTransaction(() -> {
                    Usuario u = new Usuario();
                    u.nome = nome;
                    u.cpf = cpfDigitos;
                    u.email = emailNormalizado;
                    u.telefone = telefone;
                    u.senhaSalt = salt;
                    u.senhaHash = hash;
                    u.perfil = Perfil.PACIENTE;
                    u.criadoEm = agora;
                    long usuarioId = db.usuarioDao().inserir(u);

                    // Se o paciente já era atendido na UBS, apenas vincula a conta ao cadastro existente
                    Paciente p = db.pacienteDao().buscarPorCpf(cpfDigitos);
                    boolean novo = p == null;
                    if (novo) {
                        p = new Paciente();
                        p.cpf = cpfDigitos;
                        p.criadoEm = agora;
                    }
                    p.usuarioId = usuarioId;
                    p.nome = nome;
                    p.dataNascimento = dataNascimentoIso;
                    p.telefone = telefone;
                    p.email = emailNormalizado;
                    p.endereco = endereco;
                    if (novo) db.pacienteDao().inserir(p);
                    else db.pacienteDao().atualizar(p);

                    db.notificacaoDao().inserir(Notificacao.criar(usuarioId, "Conta criada",
                            "Boas-vindas ao Saúde Conecta! Agora você pode agendar consultas e acompanhar seus atendimentos.",
                            TipoNotificacao.AVISO, agora, false, null));
                });
                AppExecutors.principal(() -> callback.onSuccess(cpfDigitos));
            } catch (SQLiteConstraintException e) {
                erro(callback, "CPF ou e-mail já cadastrado.");
            } catch (Exception e) {
                erro(callback, "Não foi possível concluir o cadastro. Tente novamente.");
            }
        });
    }

    /** Confere CPF + data de nascimento + e-mail. Retorna o id do usuário. */
    public void verificarIdentidade(String cpf, String dataNascimentoIso, String email,
                                    RepositoryCallback<Long> callback) {
        AppExecutors.disco().execute(() -> {
            Usuario u = db.usuarioDao().buscarPorCpf(Validators.somenteDigitos(cpf));
            if (u != null && Perfil.GESTOR.equals(u.perfil)) {
                erro(callback, "Contas de gestor são redefinidas pelo administrador do sistema.");
                return;
            }
            Paciente p = u != null ? db.pacienteDao().buscarPorUsuarioId(u.id) : null;
            boolean confere = p != null
                    && dataNascimentoIso.equals(p.dataNascimento)
                    && u.email.equalsIgnoreCase(email.trim());
            if (!confere) {
                erro(callback, "Os dados informados não conferem com o cadastro.");
                return;
            }
            long id = u.id;
            AppExecutors.principal(() -> callback.onSuccess(id));
        });
    }

    public void redefinirSenha(long usuarioId, String novaSenha, RepositoryCallback<Boolean> callback) {
        AppExecutors.disco().execute(() -> {
            try {
                String salt = PasswordUtils.gerarSalt();
                db.usuarioDao().atualizarSenha(usuarioId, PasswordUtils.gerarHash(novaSenha, salt), salt);
                AppExecutors.principal(() -> callback.onSuccess(true));
            } catch (Exception e) {
                erro(callback, "Não foi possível redefinir a senha.");
            }
        });
    }

    private static <T> void erro(RepositoryCallback<T> callback, String mensagem) {
        AppExecutors.principal(() -> callback.onError(mensagem));
    }
}

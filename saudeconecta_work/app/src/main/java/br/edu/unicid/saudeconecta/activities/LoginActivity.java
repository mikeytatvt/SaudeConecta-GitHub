package br.edu.unicid.saudeconecta.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.inputmethod.EditorInfo;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.snackbar.Snackbar;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.database.DatabaseSeeder;
import br.edu.unicid.saudeconecta.databinding.ActivityLoginBinding;
import br.edu.unicid.saudeconecta.models.UsuarioAutenticado;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.repositories.UsuarioRepository;
import br.edu.unicid.saudeconecta.utils.MaskTextWatcher;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.SimpleTextWatcher;
import br.edu.unicid.saudeconecta.utils.UiUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/** TELA 1 – Login com CPF e senha. Direciona paciente e gestor para áreas diferentes. */
public class LoginActivity extends AppCompatActivity {

    public static final String EXTRA_CPF = "extra_cpf";

    private ActivityLoginBinding binding;
    private SessionManager sessao;
    private UsuarioRepository repositorio;

    private final ActivityResultLauncher<Intent> cadastroLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), resultado -> {
                if (resultado.getResultCode() == RESULT_OK && resultado.getData() != null) {
                    String cpf = resultado.getData().getStringExtra(EXTRA_CPF);
                    if (cpf != null) {
                        binding.etCpf.setText(cpf);
                        binding.etSenha.setText("");
                        binding.etSenha.requestFocus();
                    }
                    Snackbar.make(binding.getRoot(), R.string.msg_conta_criada_login, Snackbar.LENGTH_LONG).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sessao = new SessionManager(this);

        // Sessão ainda válida: entra direto
        if (sessao.isLogado()) {
            abrirAreaDoUsuario();
            return;
        }

        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repositorio = new UsuarioRepository(this);

        configurarCampos();
        configurarAcoes();
    }

    private void configurarCampos() {
        binding.etCpf.addTextChangedListener(new MaskTextWatcher(MaskTextWatcher.MASCARA_CPF));
        binding.etCpf.addTextChangedListener(new SimpleTextWatcher(() -> binding.tilCpf.setError(null)));
        binding.etSenha.addTextChangedListener(new SimpleTextWatcher(() -> binding.tilSenha.setError(null)));
        binding.etSenha.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                tentarLogin();
                return true;
            }
            return false;
        });
    }

    private void configurarAcoes() {
        binding.btnEntrar.setOnClickListener(v -> tentarLogin());
        binding.tvEsqueciSenha.setOnClickListener(v ->
                startActivity(new Intent(this, EsqueciSenhaActivity.class)));
        binding.btnCriarConta.setOnClickListener(v ->
                cadastroLauncher.launch(new Intent(this, CadastroActivity.class)));
        binding.chipDemoPaciente.setOnClickListener(v -> preencherDemo(DatabaseSeeder.CPF_PACIENTE_DEMO));
        binding.chipDemoGestor.setOnClickListener(v -> preencherDemo(DatabaseSeeder.CPF_GESTOR_DEMO));
    }

    private void preencherDemo(String cpf) {
        binding.etCpf.setText(cpf);
        binding.etSenha.setText(DatabaseSeeder.SENHA_DEMO);
        binding.etSenha.setSelection(binding.etSenha.length());
    }

    private void tentarLogin() {
        UiUtils.esconderTeclado(this);
        String cpf = UiUtils.texto(binding.etCpf);
        String senha = UiUtils.textoSemTrim(binding.etSenha);

        boolean valido = true;
        if (cpf.isEmpty()) {
            binding.tilCpf.setError(getString(R.string.erro_cpf_obrigatorio));
            valido = false;
        } else if (!Validators.isCpfValido(cpf)) {
            binding.tilCpf.setError(getString(R.string.erro_cpf_invalido));
            valido = false;
        }
        if (senha.isEmpty()) {
            binding.tilSenha.setError(getString(R.string.erro_senha_obrigatoria));
            valido = false;
        }
        if (!valido) return;

        if (sessao.isLoginBloqueado()) {
            binding.tilSenha.setError(getString(R.string.erro_login_bloqueado,
                    (int) sessao.segundosRestantesBloqueio()));
            return;
        }

        setCarregando(true);
        repositorio.autenticar(cpf, senha, new RepositoryCallback<UsuarioAutenticado>() {
            @Override
            public void onSuccess(UsuarioAutenticado usuario) {
                if (isFinishing()) return;
                sessao.limparTentativas();
                sessao.criarSessao(usuario);
                abrirAreaDoUsuario();
            }

            @Override
            public void onError(String mensagem) {
                if (isFinishing()) return;
                setCarregando(false);
                binding.tilSenha.setError(mensagem);
                int restantes = sessao.registrarFalhaLogin();
                if (restantes == 0) {
                    Snackbar.make(binding.getRoot(), R.string.aviso_bloqueio, Snackbar.LENGTH_LONG).show();
                } else if (restantes <= 2) {
                    Snackbar.make(binding.getRoot(),
                            getString(R.string.aviso_tentativas_restantes, restantes), Snackbar.LENGTH_LONG).show();
                }
            }
        });
    }

    private void setCarregando(boolean carregando) {
        binding.btnEntrar.setEnabled(!carregando);
        binding.btnCriarConta.setEnabled(!carregando);
        binding.btnEntrar.setText(carregando ? R.string.entrando : R.string.entrar);
    }

    /** Separação de permissões: gestor vai para o dashboard, paciente para a tela inicial. */
    private void abrirAreaDoUsuario() {
        Class<?> destino = sessao.isGestor() ? GestorDashboardActivity.class : HomeActivity.class;
        Intent intent = new Intent(this, destino);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}

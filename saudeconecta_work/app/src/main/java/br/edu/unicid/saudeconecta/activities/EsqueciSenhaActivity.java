package br.edu.unicid.saudeconecta.activities;

import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.time.LocalDate;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ActivityEsqueciSenhaBinding;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;
import br.edu.unicid.saudeconecta.repositories.UsuarioRepository;
import br.edu.unicid.saudeconecta.utils.MaskTextWatcher;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.SimpleTextWatcher;
import br.edu.unicid.saudeconecta.utils.UiUtils;
import br.edu.unicid.saudeconecta.utils.Validators;

/** TELA 1 – Esqueci minha senha: confirma CPF + nascimento + e-mail e permite criar nova senha. */
public class EsqueciSenhaActivity extends AppCompatActivity {

    private ActivityEsqueciSenhaBinding binding;
    private UsuarioRepository repositorio;
    private long usuarioVerificadoId = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEsqueciSenhaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        repositorio = new UsuarioRepository(this);

        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.etCpf.addTextChangedListener(new MaskTextWatcher(MaskTextWatcher.MASCARA_CPF));
        binding.etNascimento.addTextChangedListener(new MaskTextWatcher(MaskTextWatcher.MASCARA_DATA));
        binding.etCpf.addTextChangedListener(new SimpleTextWatcher(() -> binding.tilCpf.setError(null)));
        binding.etNascimento.addTextChangedListener(new SimpleTextWatcher(() -> binding.tilNascimento.setError(null)));
        binding.etEmail.addTextChangedListener(new SimpleTextWatcher(() -> binding.tilEmail.setError(null)));
        binding.etNovaSenha.addTextChangedListener(new SimpleTextWatcher(() -> binding.tilNovaSenha.setError(null)));
        binding.etConfirmarSenha.addTextChangedListener(new SimpleTextWatcher(() -> binding.tilConfirmarSenha.setError(null)));

        binding.btnVerificar.setOnClickListener(v -> verificar());
        binding.btnRedefinir.setOnClickListener(v -> redefinir());
    }

    private void verificar() {
        UiUtils.esconderTeclado(this);
        String cpf = UiUtils.texto(binding.etCpf);
        LocalDate nascimento = Validators.parseDataBr(UiUtils.texto(binding.etNascimento));
        String email = UiUtils.texto(binding.etEmail);

        boolean valido = true;
        if (!Validators.isCpfValido(cpf)) {
            binding.tilCpf.setError(getString(R.string.erro_cpf_invalido));
            valido = false;
        }
        if (nascimento == null) {
            binding.tilNascimento.setError(getString(R.string.erro_data_invalida));
            valido = false;
        }
        if (!Validators.isEmailValido(email)) {
            binding.tilEmail.setError(getString(R.string.erro_email_invalido));
            valido = false;
        }
        if (!valido) return;

        binding.btnVerificar.setEnabled(false);
        binding.btnVerificar.setText(R.string.verificando);
        repositorio.verificarIdentidade(cpf, nascimento.toString(), email, new RepositoryCallback<Long>() {
            @Override
            public void onSuccess(Long usuarioId) {
                if (isFinishing()) return;
                usuarioVerificadoId = usuarioId;
                binding.btnVerificar.setText(R.string.verificar_dados);
                binding.etCpf.setEnabled(false);
                binding.etNascimento.setEnabled(false);
                binding.etEmail.setEnabled(false);
                binding.cardNovaSenha.setVisibility(View.VISIBLE);
                binding.etNovaSenha.requestFocus();
            }

            @Override
            public void onError(String mensagem) {
                if (isFinishing()) return;
                binding.btnVerificar.setEnabled(true);
                binding.btnVerificar.setText(R.string.verificar_dados);
                Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }

    private void redefinir() {
        if (usuarioVerificadoId <= 0) return;
        UiUtils.esconderTeclado(this);
        String senha = UiUtils.textoSemTrim(binding.etNovaSenha);
        String confirmar = UiUtils.textoSemTrim(binding.etConfirmarSenha);

        if (!Validators.isSenhaForte(senha)) {
            binding.tilNovaSenha.setError(getString(R.string.erro_senha_fraca));
            return;
        }
        if (!senha.equals(confirmar)) {
            binding.tilConfirmarSenha.setError(getString(R.string.erro_senhas_diferentes));
            return;
        }

        binding.btnRedefinir.setEnabled(false);
        binding.btnRedefinir.setText(R.string.redefinindo);
        repositorio.redefinirSenha(usuarioVerificadoId, senha, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                if (isFinishing()) return;
                new SessionManager(EsqueciSenhaActivity.this).limparTentativas();
                new MaterialAlertDialogBuilder(EsqueciSenhaActivity.this)
                        .setIcon(R.drawable.ic_check_circle)
                        .setTitle(R.string.senha_redefinida_titulo)
                        .setMessage(R.string.senha_redefinida_msg)
                        .setCancelable(false)
                        .setPositiveButton(R.string.ir_para_login, (d, w) -> finish())
                        .show();
            }

            @Override
            public void onError(String mensagem) {
                if (isFinishing()) return;
                binding.btnRedefinir.setEnabled(true);
                binding.btnRedefinir.setText(R.string.redefinir_senha);
                Snackbar.make(binding.getRoot(), mensagem, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}

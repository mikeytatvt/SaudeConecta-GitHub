package br.edu.unicid.saudeconecta.activities;

import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.models.ItemAdmin;
import br.edu.unicid.saudeconecta.models.ProfissionalDetalhado;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;

/** GESTOR – Cadastro de profissionais (listar, incluir, editar, ativar/desativar). */
public class AdminProfissionaisActivity extends AdminListaBaseActivity {

    @Override
    protected int titulo() { return R.string.titulo_admin_profissionais; }

    @Override
    protected int dicaBusca() { return R.string.busca_profissionais; }

    @Override
    protected int textoBotaoNovo() { return R.string.novo_profissional; }

    @Override
    protected void aoNovo() {
        startActivity(new Intent(this, FormProfissionalActivity.class));
    }

    @Override
    protected void carregar() {
        repositorio.listarProfissionais(new RepositoryCallback<List<ProfissionalDetalhado>>() {
            @Override
            public void onSuccess(List<ProfissionalDetalhado> lista) {
                List<ItemAdmin> itens = new ArrayList<>();
                for (ProfissionalDetalhado p : lista) {
                    ItemAdmin i = new ItemAdmin();
                    i.id = p.profissional.id;
                    i.origem = p;
                    i.titulo = p.profissional.nome;
                    i.subtitulo = p.especialidadeNome + " – " + p.unidadeNome;
                    i.detalhe = p.profissional.registro + "\nConsultas agendadas: " + p.agendadasFuturas;
                    i.etiqueta = getString(p.profissional.ativo ? R.string.ativo : R.string.inativo);
                    i.estiloEtiqueta = p.profissional.ativo ? ItemAdmin.ETIQUETA_VERDE : ItemAdmin.ETIQUETA_VERMELHA;
                    i.acao1 = getString(R.string.editar);
                    i.acao2 = getString(p.profissional.ativo ? R.string.desativar : R.string.ativar);
                    itens.add(i);
                }
                exibir(itens);
            }

            @Override
            public void onError(String m) { mensagem(m); }
        });
    }

    @Override
    public void aoTocar(ItemAdmin item) { aoAcao1(item); }

    @Override
    public void aoAcao1(ItemAdmin item) {
        Intent intent = new Intent(this, FormProfissionalActivity.class);
        intent.putExtra(FormProfissionalActivity.EXTRA_ID, item.id);
        startActivity(intent);
    }

    @Override
    public void aoAcao2(ItemAdmin item) {
        ProfissionalDetalhado p = (ProfissionalDetalhado) item.origem;
        if (p.profissional.ativo) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirmar_desativar_titulo)
                    .setMessage(R.string.confirmar_desativar_profissional)
                    .setPositiveButton(R.string.desativar, (d, w) -> alterar(item.id, false))
                    .setNegativeButton(R.string.cancelar, null)
                    .show();
        } else {
            alterar(item.id, true);
        }
    }

    private void alterar(long id, boolean ativo) {
        repositorio.definirAtivoProfissional(id, ativo, new RepositoryCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean ok) {
                mensagem(R.string.salvo_sucesso);
                carregar();
            }

            @Override
            public void onError(String m) { alerta(m); }
        });
    }
}

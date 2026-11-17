package br.edu.unicid.saudeconecta.activities;

import android.content.Intent;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.models.ItemAdmin;
import br.edu.unicid.saudeconecta.models.UnidadeSaude;
import br.edu.unicid.saudeconecta.repositories.RepositoryCallback;

/** GESTOR – Cadastro de unidades de saúde (listar, incluir, editar, ativar/desativar). */
public class AdminUnidadesActivity extends AdminListaBaseActivity {

    @Override
    protected int titulo() { return R.string.titulo_admin_unidades; }

    @Override
    protected int dicaBusca() { return R.string.busca_unidades; }

    @Override
    protected int textoBotaoNovo() { return R.string.nova_unidade; }

    @Override
    protected void aoNovo() {
        startActivity(new Intent(this, FormUnidadeActivity.class));
    }

    @Override
    protected void carregar() {
        repositorio.listarUnidades(new RepositoryCallback<List<UnidadeSaude>>() {
            @Override
            public void onSuccess(List<UnidadeSaude> lista) {
                List<ItemAdmin> itens = new ArrayList<>();
                for (UnidadeSaude u : lista) {
                    ItemAdmin i = new ItemAdmin();
                    i.id = u.id;
                    i.origem = u;
                    i.titulo = u.nome;
                    i.subtitulo = u.endereco + (u.bairro != null ? " – " + u.bairro : "");
                    i.detalhe = u.telefone + "\n" + u.horarioFuncionamento + "\n" + u.servicos;
                    i.etiqueta = getString(u.ativo ? R.string.ativa : R.string.inativa);
                    i.estiloEtiqueta = u.ativo ? ItemAdmin.ETIQUETA_VERDE : ItemAdmin.ETIQUETA_VERMELHA;
                    i.acao1 = getString(R.string.editar);
                    i.acao2 = getString(u.ativo ? R.string.desativar : R.string.ativar);
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
        Intent intent = new Intent(this, FormUnidadeActivity.class);
        intent.putExtra(FormUnidadeActivity.EXTRA_ID, item.id);
        startActivity(intent);
    }

    @Override
    public void aoAcao2(ItemAdmin item) {
        UnidadeSaude u = (UnidadeSaude) item.origem;
        if (u.ativo) {
            new MaterialAlertDialogBuilder(this)
                    .setTitle(R.string.confirmar_desativar_titulo)
                    .setMessage(R.string.confirmar_desativar_unidade)
                    .setPositiveButton(R.string.desativar, (d, w) -> alterar(u, false))
                    .setNegativeButton(R.string.cancelar, null)
                    .show();
        } else {
            alterar(u, true);
        }
    }

    private void alterar(UnidadeSaude u, boolean ativo) {
        repositorio.definirAtivoUnidade(u.id, ativo, new RepositoryCallback<Boolean>() {
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

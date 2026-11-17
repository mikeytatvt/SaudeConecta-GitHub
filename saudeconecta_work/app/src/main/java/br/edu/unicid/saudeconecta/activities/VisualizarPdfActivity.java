package br.edu.unicid.saudeconecta.activities;

import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.databinding.ActivityVisualizarPdfBinding;
import br.edu.unicid.saudeconecta.utils.AppExecutors;
import br.edu.unicid.saudeconecta.utils.PdfResultadoGenerator;
import br.edu.unicid.saudeconecta.utils.SessionManager;
import br.edu.unicid.saudeconecta.utils.UiUtils;

/** Mostra o PDF do resultado dentro do app, com opções de baixar e compartilhar. */
public class VisualizarPdfActivity extends AppCompatActivity {

    public static final String EXTRA_CAMINHO = "extra_caminho_pdf";
    public static final String EXTRA_TITULO = "extra_titulo_pdf";

    private ActivityVisualizarPdfBinding binding;
    private File arquivo;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (!new SessionManager(this).isLogado()) {
            UiUtils.irParaLogin(this);
            return;
        }

        UiUtils.ativarTelaCheia(this);
        binding = ActivityVisualizarPdfBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        UiUtils.aplicarMargensDoSistema(binding.getRoot(), binding.toolbar);
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        binding.toolbar.setTitle(getIntent().getStringExtra(EXTRA_TITULO));

        arquivo = arquivoSeguro(getIntent().getStringExtra(EXTRA_CAMINHO));
        if (arquivo == null) {
            Snackbar.make(binding.getRoot(), R.string.erro_pdf, Snackbar.LENGTH_LONG).show();
            binding.progresso.setVisibility(View.GONE);
            return;
        }

        binding.toolbar.inflateMenu(R.menu.menu_pdf);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.acao_baixar) {
                confirmarDownload();
                return true;
            } else if (item.getItemId() == R.id.acao_compartilhar) {
                compartilhar();
                return true;
            }
            return false;
        });

        renderizar();
    }

    /** Aceita apenas arquivos da pasta interna de exames do próprio app. */
    private File arquivoSeguro(String caminho) {
        if (caminho == null) return null;
        try {
            File f = new File(caminho).getCanonicalFile();
            File pasta = PdfResultadoGenerator.pastaExames(this).getCanonicalFile();
            if (f.exists() && pasta.equals(f.getParentFile())) return f;
        } catch (IOException ignorado) {
            // caminho inválido
        }
        return null;
    }

    // ---------------------------------------------------------------- Visualização

    private void renderizar() {
        int larguraTela = getResources().getDisplayMetrics().widthPixels;
        int largura = Math.min(larguraTela * 2, 1600); // boa nitidez sem gastar memória demais

        AppExecutors.disco().execute(() -> {
            List<Bitmap> paginas = new ArrayList<>();
            try (ParcelFileDescriptor descritor = ParcelFileDescriptor.open(arquivo, ParcelFileDescriptor.MODE_READ_ONLY);
                 PdfRenderer renderizador = new PdfRenderer(descritor)) {
                for (int i = 0; i < renderizador.getPageCount(); i++) {
                    try (PdfRenderer.Page pagina = renderizador.openPage(i)) {
                        int altura = Math.round(largura * (pagina.getHeight() / (float) pagina.getWidth()));
                        Bitmap bitmap = Bitmap.createBitmap(largura, altura, Bitmap.Config.ARGB_8888);
                        bitmap.eraseColor(Color.WHITE);
                        pagina.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);
                        paginas.add(bitmap);
                    }
                }
                AppExecutors.principal(() -> exibirPaginas(paginas));
            } catch (Exception e) {
                AppExecutors.principal(() -> {
                    binding.progresso.setVisibility(View.GONE);
                    Snackbar.make(binding.getRoot(), R.string.erro_pdf, Snackbar.LENGTH_LONG).show();
                });
            }
        });
    }

    private void exibirPaginas(List<Bitmap> paginas) {
        if (isFinishing()) return;
        binding.progresso.setVisibility(View.GONE);
        int espaco = Math.round(12 * getResources().getDisplayMetrics().density);
        for (Bitmap pagina : paginas) {
            ImageView imagem = new ImageView(this);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.bottomMargin = espaco;
            imagem.setLayoutParams(lp);
            imagem.setAdjustViewBounds(true);
            imagem.setElevation(4 * getResources().getDisplayMetrics().density);
            imagem.setImageBitmap(pagina);
            imagem.setContentDescription(getIntent().getStringExtra(EXTRA_TITULO));
            binding.containerPaginas.addView(imagem);
        }
    }

    // ---------------------------------------------------------------- Baixar

    private void confirmarDownload() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
            Snackbar.make(binding.getRoot(), R.string.baixar_indisponivel, Snackbar.LENGTH_LONG).show();
            compartilhar();
            return;
        }
        // LGPD: o paciente decide conscientemente tirar o documento da área protegida do app
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.baixar_titulo)
                .setMessage(R.string.baixar_aviso_lgpd)
                .setPositiveButton(R.string.salvar, (d, w) -> salvarEmDownloads())
                .setNegativeButton(R.string.cancelar, null)
                .show();
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private void salvarEmDownloads() {
        AppExecutors.disco().execute(() -> {
            boolean ok = false;
            try {
                ContentValues valores = new ContentValues();
                valores.put(MediaStore.MediaColumns.DISPLAY_NAME, arquivo.getName());
                valores.put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf");
                valores.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/SaudeConecta");
                Uri destino = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, valores);
                if (destino != null) {
                    try (InputStream entrada = new FileInputStream(arquivo);
                         OutputStream saida = getContentResolver().openOutputStream(destino)) {
                        if (saida != null) {
                            byte[] buffer = new byte[8192];
                            int lidos;
                            while ((lidos = entrada.read(buffer)) != -1) saida.write(buffer, 0, lidos);
                            ok = true;
                        }
                    }
                }
            } catch (Exception e) {
                ok = false;
            }
            boolean sucesso = ok;
            AppExecutors.principal(() -> {
                if (!isFinishing()) {
                    Snackbar.make(binding.getRoot(), sucesso ? R.string.pdf_salvo : R.string.erro_salvar_pdf,
                            Snackbar.LENGTH_LONG).show();
                }
            });
        });
    }

    // ---------------------------------------------------------------- Compartilhar

    private void compartilhar() {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", arquivo);
        Intent envio = new Intent(Intent.ACTION_SEND);
        envio.setType("application/pdf");
        envio.putExtra(Intent.EXTRA_STREAM, uri);
        envio.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(envio, getString(R.string.compartilhar_pdf_titulo)));
    }
}

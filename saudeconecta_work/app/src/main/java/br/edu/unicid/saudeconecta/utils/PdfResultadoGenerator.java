package br.edu.unicid.saudeconecta.utils;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import br.edu.unicid.saudeconecta.models.ExameDetalhado;
import br.edu.unicid.saudeconecta.models.Paciente;
import br.edu.unicid.saudeconecta.models.ResultadoExame;

/**
 * Gera o PDF do resultado de exame (A4) na pasta interna do app (cache/exames).
 * LGPD: o CPF sai mascarado e o arquivo não fica em área pública, a menos que o paciente o salve.
 */
public final class PdfResultadoGenerator {

    private static final int LARGURA = 595;  // A4 em pontos
    private static final int ALTURA = 842;
    private static final float MARGEM = 40f;
    private static final float COLUNA_VALOR = 190f;
    private static final int VERDE = Color.rgb(0x17, 0x66, 0x38);
    private static final int VERDE_CLARO = Color.rgb(0xD9, 0xEB, 0xDD);
    private static final int CINZA = Color.rgb(0x5E, 0x6B, 0x63);
    private static final int TEXTO = Color.rgb(0x1F, 0x2A, 0x24);

    private PdfResultadoGenerator() { }

    public static File pastaExames(Context context) {
        File pasta = new File(context.getCacheDir(), "exames");
        if (!pasta.exists()) pasta.mkdirs();
        return pasta;
    }

    public static File gerar(Context context, Paciente paciente, ExameDetalhado detalhe,
                             ResultadoExame resultado) throws IOException {
        PdfDocument documento = new PdfDocument();
        PdfDocument.Page pagina = documento.startPage(
                new PdfDocument.PageInfo.Builder(LARGURA, ALTURA, 1).create());
        Canvas c = pagina.getCanvas();

        Paint fundo = new Paint();
        Paint titulo = texto(22, Color.WHITE, true);
        Paint subtitulo = texto(11, Color.WHITE, false);
        Paint secao = texto(12, VERDE, true);
        Paint rotulo = texto(10, CINZA, true);
        Paint valor = texto(11, TEXTO, false);
        Paint destaque = texto(11, VERDE, true);
        Paint rodape = texto(8.5f, CINZA, false);

        // Cabeçalho verde (identidade visual do banner)
        fundo.setColor(VERDE);
        c.drawRect(0, 0, LARGURA, 92, fundo);
        c.drawText("Saúde Conecta", MARGEM, 46, titulo);
        c.drawText("Resultado de exame – Atenção Básica", MARGEM, 68, subtitulo);
        Paint direita = texto(10, Color.WHITE, false);
        direita.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Exame nº " + detalhe.exame.id, LARGURA - MARGEM, 46, direita);
        c.drawText(valorOuTraco(detalhe.unidadeNome), LARGURA - MARGEM, 68, direita);

        float y = 128;
        y = tituloSecao(c, "Dados do paciente", y, secao);
        y = linha(c, "Nome", paciente.nome, y, rotulo, valor);
        y = linha(c, "CPF", mascararCpf(paciente.cpf), y, rotulo, valor);
        y = linha(c, "Nascimento", DateUtils.isoParaBr(paciente.dataNascimento), y, rotulo, valor);
        y = linha(c, "Cartão SUS", valorOuTraco(paciente.cartaoSus), y, rotulo, valor);

        y += 10;
        y = tituloSecao(c, "Exame", y, secao);
        y = linha(c, "Exame", detalhe.exame.nome, y, rotulo, valor);
        y = linha(c, "Tipo", valorOuTraco(detalhe.exame.tipo), y, rotulo, valor);
        y = linha(c, "Solicitado por", valorOuTraco(detalhe.profissionalNome), y, rotulo, valor);
        y = linha(c, "Data da solicitação", DateUtils.isoParaBr(detalhe.exame.dataSolicitacao), y, rotulo, valor);
        y = linha(c, "Data da realização", DateUtils.isoParaBr(detalhe.exame.dataRealizacao), y, rotulo, valor);
        y = linha(c, "Unidade", valorOuTraco(detalhe.unidadeNome), y, rotulo, valor);
        y = linha(c, "Liberado em", DateUtils.isoParaBr(resultado.dataLiberacao), y, rotulo, valor);

        y += 10;
        y = tituloSecao(c, "Resultado", y, secao);
        // Resumo em destaque com fundo verde claro
        float alturaResumo = alturaTexto(valorOuTraco(resultado.resumo), LARGURA - 2 * MARGEM - 20, destaque, 15);
        fundo.setColor(VERDE_CLARO);
        c.drawRoundRect(MARGEM, y - 4, LARGURA - MARGEM, y + alturaResumo + 8, 6, 6, fundo);
        y = paragrafo(c, valorOuTraco(resultado.resumo), MARGEM + 10, y + 10, LARGURA - 2 * MARGEM - 20, destaque, 15);
        y += 14;
        if (resultado.valores != null) {
            for (String linhaValor : resultado.valores.split("\n")) {
                y = paragrafo(c, "• " + linhaValor, MARGEM, y, LARGURA - 2 * MARGEM, valor, 16);
            }
        }

        if (resultado.observacao != null && !resultado.observacao.isEmpty()) {
            y += 12;
            y = tituloSecao(c, "Observação", y, secao);
            paragrafo(c, resultado.observacao, MARGEM, y, LARGURA - 2 * MARGEM, valor, 16);
        }

        // Rodapé
        Paint linhaRodape = new Paint();
        linhaRodape.setColor(Color.rgb(0xE1, 0xE6, 0xE2));
        linhaRodape.setStrokeWidth(1);
        c.drawLine(MARGEM, ALTURA - 70, LARGURA - MARGEM, ALTURA - 70, linhaRodape);
        String geradoEm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        c.drawText("Documento gerado pelo aplicativo Saúde Conecta em " + geradoEm
                + ". Dados simulados para fins acadêmicos.", MARGEM, ALTURA - 52, rodape);
        c.drawText("Contém dados pessoais sensíveis protegidos pela LGPD (Lei nº 13.709/2018). "
                + "Compartilhe apenas com profissionais de saúde.", MARGEM, ALTURA - 38, rodape);

        documento.finishPage(pagina);
        File arquivo = new File(pastaExames(context), "resultado_exame_" + detalhe.exame.id + ".pdf");
        try (FileOutputStream saida = new FileOutputStream(arquivo)) {
            documento.writeTo(saida);
        } finally {
            documento.close();
        }
        return arquivo;
    }

    // ------------------------------------------------------------------ apoio

    private static Paint texto(float tamanho, int cor, boolean negrito) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(tamanho);
        p.setColor(cor);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, negrito ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }

    private static float tituloSecao(Canvas c, String titulo, float y, Paint p) {
        c.drawText(titulo, MARGEM, y, p);
        Paint linha = new Paint();
        linha.setColor(VERDE);
        linha.setStrokeWidth(1.2f);
        c.drawLine(MARGEM, y + 6, LARGURA - MARGEM, y + 6, linha);
        return y + 24;
    }

    private static float linha(Canvas c, String rotulo, String valor, float y, Paint pRotulo, Paint pValor) {
        c.drawText(rotulo, MARGEM, y, pRotulo);
        float fim = paragrafo(c, valor, COLUNA_VALOR, y, LARGURA - MARGEM - COLUNA_VALOR, pValor, 15);
        return Math.max(fim, y + 17);
    }

    /** Desenha texto com quebra automática de linha; retorna o y da próxima linha. */
    private static float paragrafo(Canvas c, String texto, float x, float y, float largura, Paint p, float entrelinha) {
        String resto = texto;
        while (!resto.isEmpty()) {
            int qtd = Math.max(1, p.breakText(resto, true, largura, null));
            if (qtd < resto.length()) {
                int espaco = resto.lastIndexOf(' ', qtd);
                if (espaco > 0) qtd = espaco + 1;
            }
            c.drawText(resto.substring(0, qtd).trim(), x, y, p);
            resto = resto.substring(qtd);
            y += entrelinha;
        }
        return y;
    }

    private static float alturaTexto(String texto, float largura, Paint p, float entrelinha) {
        int linhas = 0;
        String resto = texto;
        while (!resto.isEmpty()) {
            int qtd = Math.max(1, p.breakText(resto, true, largura, null));
            if (qtd < resto.length()) {
                int espaco = resto.lastIndexOf(' ', qtd);
                if (espaco > 0) qtd = espaco + 1;
            }
            resto = resto.substring(qtd);
            linhas++;
        }
        return linhas * entrelinha;
    }

    /** 11144477735 -> ***.444.777-** */
    private static String mascararCpf(String cpf) {
        String d = Validators.somenteDigitos(cpf);
        if (d.length() != 11) return "***";
        return "***." + d.substring(3, 6) + "." + d.substring(6, 9) + "-**";
    }

    private static String valorOuTraco(String valor) {
        return valor == null || valor.trim().isEmpty() ? "—" : valor;
    }
}

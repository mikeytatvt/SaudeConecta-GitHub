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
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import br.edu.unicid.saudeconecta.models.ItemIndicador;
import br.edu.unicid.saudeconecta.repositories.DashboardRepository;

/**
 * Relatório de indicadores do gestor em PDF (A4, com quebra de página automática).
 * Contém apenas dados agregados, sem identificação de pacientes (LGPD).
 */
public final class RelatorioPdfGenerator {

    private static final int LARGURA = 595;
    private static final int ALTURA = 842;
    private static final float MARGEM = 40f;
    private static final float LIMITE_INFERIOR = ALTURA - 70f;
    private static final int VERDE = Color.rgb(0x17, 0x66, 0x38);
    private static final int VERDE_MEDIO = Color.rgb(0x25, 0x83, 0x4B);
    private static final int VERDE_CLARO = Color.rgb(0xD9, 0xEB, 0xDD);
    private static final int CINZA = Color.rgb(0x5E, 0x6B, 0x63);
    private static final int TEXTO = Color.rgb(0x1F, 0x2A, 0x24);

    private final PdfDocument documento = new PdfDocument();
    private final NumberFormat inteiro = NumberFormat.getIntegerInstance(DateUtils.PT_BR);
    private PdfDocument.Page pagina;
    private Canvas c;
    private int numeroPagina = 0;
    private float y;

    private final Paint secao = texto(13, VERDE, true);
    private final Paint normal = texto(11, TEXTO, false);
    private final Paint negrito = texto(11, TEXTO, true);
    private final Paint pequeno = texto(9, CINZA, false);

    private RelatorioPdfGenerator() { }

    public static File gerar(Context context, DashboardRepository.Indicadores r, String gestor) throws IOException {
        return new RelatorioPdfGenerator().montar(context, r, gestor);
    }

    private File montar(Context context, DashboardRepository.Indicadores r, String gestor) throws IOException {
        String periodo = DateUtils.formatarBr(r.inicio) + " a " + DateUtils.formatarBr(r.fim);
        novaPagina(periodo);

        // Indicadores principais em 4 caixas (como no dashboard)
        tituloSecao("Indicadores principais");
        String[][] kpis = {
                {inteiro.format(r.atendimentos), "Atendimentos realizados"},
                {inteiro.format(r.consultasMedicas), "Consultas médicas"},
                {inteiro.format(r.exames), "Exames realizados"},
                {inteiro.format(r.pacientesAtendidos), "Pacientes atendidos"}};
        float larguraCaixa = (LARGURA - 2 * MARGEM - 3 * 10) / 4f;
        Paint fundo = new Paint();
        fundo.setColor(VERDE_CLARO);
        Paint numero = texto(18, VERDE, true);
        numero.setTextAlign(Paint.Align.CENTER);
        Paint rotulo = texto(9, CINZA, false);
        rotulo.setTextAlign(Paint.Align.CENTER);
        for (int i = 0; i < 4; i++) {
            float x = MARGEM + i * (larguraCaixa + 10);
            c.drawRoundRect(x, y, x + larguraCaixa, y + 54, 6, 6, fundo);
            c.drawText(kpis[i][0], x + larguraCaixa / 2, y + 26, numero);
            c.drawText(kpis[i][1], x + larguraCaixa / 2, y + 44, rotulo);
        }
        y += 74;

        tabela("Atendimentos por unidade", r.porUnidade, r.atendimentos);
        tabela("Atendimentos por especialidade", r.porEspecialidade, r.atendimentos);

        tituloSecao("Tempo de espera");
        linha("Tempo médio de espera no período", String.format(DateUtils.PT_BR, "%.0f minutos", r.tempoMedioEspera));
        if (r.reducaoEspera != null) {
            linha("Redução do tempo médio de espera", String.format(DateUtils.PT_BR, "%.0f%%", r.reducaoEspera));
        }
        if (r.esperaPorDia != null && !r.esperaPorDia.isEmpty()) {
            ItemIndicador primeiro = r.esperaPorDia.get(0);
            ItemIndicador ultimo = r.esperaPorDia.get(r.esperaPorDia.size() - 1);
            linha("Média no primeiro dia (" + DateUtils.isoParaBr(primeiro.rotulo) + ")",
                    String.format(DateUtils.PT_BR, "%.0f min", primeiro.valor));
            linha("Média no último dia (" + DateUtils.isoParaBr(ultimo.rotulo) + ")",
                    String.format(DateUtils.PT_BR, "%.0f min", ultimo.valor));
        }
        y += 8;

        tituloSecao("Agenda e rede");
        linha("Taxa de cancelamento", String.format(DateUtils.PT_BR, "%.1f%%", r.taxaCancelamento));
        linha("Ocupação da agenda (próximos 10 dias úteis)", String.format(DateUtils.PT_BR, "%.0f%%", r.ocupacaoAgenda));
        linha("Consultas agendadas (próximos 10 dias úteis)", inteiro.format(r.agendadasProximas));
        linha("Pacientes cadastrados", inteiro.format(r.pacientesCadastrados));
        linha("Unidades de saúde integradas", inteiro.format(r.unidadesAtivas));
        linha("Profissionais ativos", inteiro.format(r.profissionaisAtivos));

        y += 14;
        garantirEspaco(30);
        String geradoEm = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy 'às' HH:mm"));
        c.drawText("Relatório gerado por " + gestor + " em " + geradoEm + ".", MARGEM, y, pequeno);

        finalizarPagina();
        File arquivo = new File(PdfResultadoGenerator.pastaExames(context),
                "relatorio_indicadores_" + r.fim.toString() + ".pdf");
        try (FileOutputStream saida = new FileOutputStream(arquivo)) {
            documento.writeTo(saida);
        } finally {
            documento.close();
        }
        return arquivo;
    }

    // ------------------------------------------------------------------ páginas

    private String periodoAtual;

    private void novaPagina(String periodo) {
        periodoAtual = periodo;
        numeroPagina++;
        pagina = documento.startPage(new PdfDocument.PageInfo.Builder(LARGURA, ALTURA, numeroPagina).create());
        c = pagina.getCanvas();

        Paint fundo = new Paint();
        fundo.setColor(VERDE);
        c.drawRect(0, 0, LARGURA, 92, fundo);
        c.drawText("Saúde Conecta", MARGEM, 44, texto(22, Color.WHITE, true));
        c.drawText("Relatório de indicadores da Atenção Básica", MARGEM, 66, texto(11, Color.WHITE, false));
        Paint direita = texto(10, Color.WHITE, false);
        direita.setTextAlign(Paint.Align.RIGHT);
        c.drawText("Período: " + periodo, LARGURA - MARGEM, 44, direita);
        c.drawText("Página " + numeroPagina, LARGURA - MARGEM, 66, direita);
        y = 126;
    }

    private void finalizarPagina() {
        Paint linha = new Paint();
        linha.setColor(Color.rgb(0xE1, 0xE6, 0xE2));
        c.drawLine(MARGEM, ALTURA - 50, LARGURA - MARGEM, ALTURA - 50, linha);
        c.drawText("Dados agregados, sem identificação de pacientes (LGPD – Lei nº 13.709/2018). "
                + "Dados simulados para fins acadêmicos.", MARGEM, ALTURA - 34, pequeno);
        documento.finishPage(pagina);
    }

    private void garantirEspaco(float altura) {
        if (y + altura > LIMITE_INFERIOR) {
            finalizarPagina();
            novaPagina(periodoAtual);
        }
    }

    // ------------------------------------------------------------------ blocos

    private void tituloSecao(String titulo) {
        garantirEspaco(50);
        c.drawText(titulo, MARGEM, y, secao);
        Paint linha = new Paint();
        linha.setColor(VERDE);
        linha.setStrokeWidth(1.2f);
        c.drawLine(MARGEM, y + 6, LARGURA - MARGEM, y + 6, linha);
        y += 24;
    }

    private void linha(String rotulo, String valor) {
        garantirEspaco(18);
        c.drawText(rotulo, MARGEM, y, normal);
        Paint direita = new Paint(negrito);
        direita.setTextAlign(Paint.Align.RIGHT);
        c.drawText(valor, LARGURA - MARGEM, y, direita);
        y += 18;
    }

    /** Tabela com nome, quantidade, percentual e barra proporcional. */
    private void tabela(String titulo, List<ItemIndicador> itens, int total) {
        tituloSecao(titulo);
        if (itens == null || itens.isEmpty()) {
            linha("Sem dados no período", "—");
            y += 8;
            return;
        }
        double maximo = 0;
        for (ItemIndicador i : itens) maximo = Math.max(maximo, i.valor);
        Paint barra = new Paint();
        barra.setColor(VERDE_MEDIO);
        Paint direita = new Paint(negrito);
        direita.setTextAlign(Paint.Align.RIGHT);
        Paint percentual = new Paint(pequeno);
        percentual.setTextAlign(Paint.Align.RIGHT);
        float inicioBarra = MARGEM + 170;
        float larguraBarra = LARGURA - 2 * MARGEM - 170 - 110;

        for (ItemIndicador i : itens) {
            garantirEspaco(20);
            c.drawText(i.rotulo, MARGEM, y, normal);
            float comprimento = maximo <= 0 ? 0 : (float) (larguraBarra * i.valor / maximo);
            c.drawRect(inicioBarra, y - 9, inicioBarra + comprimento, y + 1, barra);
            c.drawText(inteiro.format(Math.round(i.valor)), LARGURA - MARGEM - 50, y, direita);
            double pct = total == 0 ? 0 : i.valor * 100.0 / total;
            c.drawText(String.format(DateUtils.PT_BR, "%.1f%%", pct), LARGURA - MARGEM, y, percentual);
            y += 20;
        }
        y += 10;
    }

    private static Paint texto(float tamanho, int cor, boolean bold) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTextSize(tamanho);
        p.setColor(cor);
        p.setTypeface(Typeface.create(Typeface.SANS_SERIF, bold ? Typeface.BOLD : Typeface.NORMAL));
        return p;
    }
}

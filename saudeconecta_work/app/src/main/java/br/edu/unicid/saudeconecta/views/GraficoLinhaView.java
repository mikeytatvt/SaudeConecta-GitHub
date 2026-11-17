package br.edu.unicid.saudeconecta.views;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.models.ItemIndicador;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/** Gráfico de linha (Figura 3 do banner: "Tempo médio de espera" em minutos por dia). */
public class GraficoLinhaView extends View {

    private final List<ItemIndicador> pontos = new ArrayList<>();
    private final Paint pLinha = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pArea = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pPonto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pGrade = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTexto = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path caminho = new Path();
    private final Path area = new Path();
    private final float dp;

    public GraficoLinhaView(Context context) {
        this(context, null);
    }

    public GraficoLinhaView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        float sp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1, getResources().getDisplayMetrics());
        pLinha.setColor(context.getColor(R.color.verde_escuro));
        pLinha.setStyle(Paint.Style.STROKE);
        pLinha.setStrokeWidth(2.5f * dp);
        pLinha.setStrokeJoin(Paint.Join.ROUND);
        pLinha.setStrokeCap(Paint.Cap.ROUND);
        pArea.setColor(context.getColor(R.color.verde_claro));
        pArea.setAlpha(150);
        pPonto.setColor(context.getColor(R.color.verde_escuro));
        pGrade.setColor(context.getColor(R.color.borda));
        pGrade.setStrokeWidth(1 * dp);
        pTexto.setColor(context.getColor(R.color.texto_secundario));
        pTexto.setTextSize(11 * sp);
    }

    /** Rótulos no formato yyyy-MM-dd; valores em minutos. */
    public void setPontos(List<ItemIndicador> novos) {
        pontos.clear();
        if (novos != null) pontos.addAll(novos);
        if (!pontos.isEmpty()) {
            setContentDescription(String.format(Locale.ROOT, "%s: de %.0f para %.0f minutos",
                    getContext().getString(R.string.tempo_medio_espera),
                    pontos.get(0).valor, pontos.get(pontos.size() - 1).valor));
        }
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float largura = getWidth();
        float altura = getHeight();

        if (pontos.isEmpty()) {
            pTexto.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getContext().getString(R.string.sem_dados_periodo), largura / 2, altura / 2, pTexto);
            pTexto.setTextAlign(Paint.Align.LEFT);
            return;
        }

        double maximo = 0;
        for (ItemIndicador p : pontos) maximo = Math.max(maximo, p.valor);
        int passo = 20;
        int topoEscala = Math.max(passo, (int) Math.ceil(maximo / passo) * passo);
        while (topoEscala / passo > 4) passo += 20;
        topoEscala = Math.max(passo, (int) Math.ceil(maximo / passo) * passo);

        float margemEsquerda = pTexto.measureText(String.valueOf(topoEscala)) + 10 * dp;
        float margemBase = pTexto.getTextSize() + 10 * dp;
        float margemTopo = 8 * dp;
        float margemDireita = 8 * dp;
        float x0 = margemEsquerda;
        float x1 = largura - margemDireita;
        float y0 = altura - margemBase;
        float y1 = margemTopo;

        // Grade e escala do eixo Y (0, 20, 40, 60...)
        pTexto.setTextAlign(Paint.Align.RIGHT);
        for (int v = 0; v <= topoEscala; v += passo) {
            float y = y0 - (y0 - y1) * v / topoEscala;
            canvas.drawLine(x0, y, x1, y, pGrade);
            canvas.drawText(String.valueOf(v), x0 - 6 * dp, y + pTexto.getTextSize() / 3, pTexto);
        }

        // Linha e área preenchida
        int n = pontos.size();
        caminho.reset();
        area.reset();
        float[] xs = new float[n];
        float[] ys = new float[n];
        for (int i = 0; i < n; i++) {
            xs[i] = n == 1 ? (x0 + x1) / 2 : x0 + (x1 - x0) * i / (n - 1);
            ys[i] = (float) (y0 - (y0 - y1) * pontos.get(i).valor / topoEscala);
            if (i == 0) {
                caminho.moveTo(xs[i], ys[i]);
                area.moveTo(xs[i], y0);
                area.lineTo(xs[i], ys[i]);
            } else {
                caminho.lineTo(xs[i], ys[i]);
                area.lineTo(xs[i], ys[i]);
            }
        }
        area.lineTo(xs[n - 1], y0);
        area.close();
        canvas.drawPath(area, pArea);
        canvas.drawPath(caminho, pLinha);
        for (int i = 0; i < n; i++) canvas.drawCircle(xs[i], ys[i], 3 * dp, pPonto);

        // Datas do eixo X (até 4 rótulos, como no banner)
        Set<Integer> indices = new LinkedHashSet<>();
        indices.add(0);
        if (n > 2) {
            indices.add(n / 3);
            indices.add(2 * n / 3);
        }
        indices.add(n - 1);
        pTexto.setTextAlign(Paint.Align.CENTER);
        for (int i : indices) {
            String rotulo = rotuloData(pontos.get(i).rotulo);
            float x = Math.max(x0 + pTexto.measureText(rotulo) / 2, Math.min(xs[i], x1 - pTexto.measureText(rotulo) / 2));
            canvas.drawText(rotulo, x, altura - 2 * dp, pTexto);
        }
        pTexto.setTextAlign(Paint.Align.LEFT);
    }

    private static String rotuloData(String iso) {
        try {
            return DateUtils.diaMes(LocalDate.parse(iso));
        } catch (Exception e) {
            return iso;
        }
    }
}

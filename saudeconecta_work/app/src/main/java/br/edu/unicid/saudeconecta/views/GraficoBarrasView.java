package br.edu.unicid.saudeconecta.views;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.text.TextUtils;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

import androidx.annotation.Nullable;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

import br.edu.unicid.saudeconecta.R;
import br.edu.unicid.saudeconecta.models.ItemIndicador;
import br.edu.unicid.saudeconecta.utils.DateUtils;

/** Gráfico de barras horizontais (Figura 3 do banner: "Atendimentos por unidade"). */
public class GraficoBarrasView extends View {

    private final List<ItemIndicador> itens = new ArrayList<>();
    private final TextPaint pRotulo = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint pValor = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pBarra = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pTrilho = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final NumberFormat formato = NumberFormat.getIntegerInstance(DateUtils.PT_BR);
    private final RectF retangulo = new RectF();
    private final float alturaBarra;
    private final float espaco;
    private final float dp;
    private float progresso = 1f;

    public GraficoBarrasView(Context context) {
        this(context, null);
    }

    public GraficoBarrasView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        dp = getResources().getDisplayMetrics().density;
        alturaBarra = 16 * dp;
        espaco = 14 * dp;
        float sp = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 1, getResources().getDisplayMetrics());
        pRotulo.setTextSize(12 * sp);
        pRotulo.setColor(context.getColor(R.color.texto_primario));
        pValor.setTextSize(12 * sp);
        pValor.setFakeBoldText(true);
        pValor.setColor(context.getColor(R.color.texto_primario));
        pBarra.setColor(context.getColor(R.color.verde_medio));
        pTrilho.setColor(context.getColor(R.color.cinza_claro));
    }

    public void setItens(List<ItemIndicador> novos) {
        itens.clear();
        if (novos != null) itens.addAll(novos);
        StringBuilder descricao = new StringBuilder();
        for (ItemIndicador i : itens) {
            descricao.append(i.rotulo).append(": ").append(formato.format(Math.round(i.valor))).append(". ");
        }
        setContentDescription(descricao.toString());
        requestLayout();
        animar();
    }

    private void animar() {
        ValueAnimator animacao = ValueAnimator.ofFloat(0f, 1f);
        animacao.setDuration(700);
        animacao.setInterpolator(new DecelerateInterpolator());
        animacao.addUpdateListener(a -> {
            progresso = (float) a.getAnimatedValue();
            invalidate();
        });
        animacao.start();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int largura = MeasureSpec.getSize(widthMeasureSpec);
        int linhas = Math.max(1, itens.size());
        int altura = Math.round(getPaddingTop() + getPaddingBottom() + linhas * alturaBarra + (linhas - 1) * espaco);
        setMeasuredDimension(largura, resolveSize(altura, heightMeasureSpec));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float esquerda = getPaddingLeft();
        float largura = getWidth() - getPaddingLeft() - getPaddingRight();

        if (itens.isEmpty()) {
            pRotulo.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(getContext().getString(R.string.sem_dados_periodo),
                    esquerda + largura / 2, getPaddingTop() + alturaBarra * 0.8f, pRotulo);
            pRotulo.setTextAlign(Paint.Align.LEFT);
            return;
        }

        double maximo = 0;
        float maiorRotulo = 0;
        float maiorValor = 0;
        for (ItemIndicador i : itens) {
            maximo = Math.max(maximo, i.valor);
            maiorRotulo = Math.max(maiorRotulo, pRotulo.measureText(i.rotulo));
            maiorValor = Math.max(maiorValor, pValor.measureText(formato.format(Math.round(i.valor))));
        }
        float colunaRotulo = Math.min(maiorRotulo, largura * 0.4f) + 10 * dp;
        float colunaValor = maiorValor + 8 * dp;
        float areaBarra = Math.max(10 * dp, largura - colunaRotulo - colunaValor);
        float inicioBarra = esquerda + colunaRotulo;
        float ajusteTexto = (pRotulo.descent() + pRotulo.ascent()) / 2;

        for (int i = 0; i < itens.size(); i++) {
            ItemIndicador item = itens.get(i);
            float topo = getPaddingTop() + i * (alturaBarra + espaco);
            float centro = topo + alturaBarra / 2;

            CharSequence rotulo = TextUtils.ellipsize(item.rotulo, pRotulo, colunaRotulo - 10 * dp, TextUtils.TruncateAt.END);
            canvas.drawText(rotulo, 0, rotulo.length(), esquerda, centro - ajusteTexto, pRotulo);

            retangulo.set(inicioBarra, topo, inicioBarra + areaBarra, topo + alturaBarra);
            canvas.drawRoundRect(retangulo, 3 * dp, 3 * dp, pTrilho);

            float comprimento = maximo <= 0 ? 0 : (float) (areaBarra * (item.valor / maximo) * progresso);
            if (comprimento > 0) {
                retangulo.set(inicioBarra, topo, inicioBarra + comprimento, topo + alturaBarra);
                canvas.drawRoundRect(retangulo, 3 * dp, 3 * dp, pBarra);
            }
            canvas.drawText(formato.format(Math.round(item.valor * progresso)),
                    inicioBarra + comprimento + 6 * dp, centro - ajusteTexto, pValor);
        }
    }
}

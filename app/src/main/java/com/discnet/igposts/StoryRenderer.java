package com.discnet.igposts;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Desenha exatamente a mesma cena na prévia e na exportação 1080x1920. */
final class StoryRenderer {
    static final int WIDTH = 1080;
    static final int HEIGHT = 1920;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private static final int INK = Color.rgb(33, 34, 37);
    private static final int HASHTAG = Color.rgb(34, 82, 104);

    void draw(Canvas canvas, int width, int height, EditorState state,
              Bitmap artwork, Bitmap avatar, Bitmap blurred) {
        int checkpoint = canvas.save();
        canvas.scale(width / (float) WIDTH, height / (float) HEIGHT);
        background(canvas, state, blurred);
        card(canvas, state, artwork, avatar);
        canvas.restoreToCount(checkpoint);
    }

    private void background(Canvas canvas, EditorState state, Bitmap blurred) {
        p.reset();
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setColor(Color.rgb(42, 34, 35));
        canvas.drawColor(Color.rgb(37, 37, 40));
        if (blurred != null && !blurred.isRecycled()) {
            cover(canvas, blurred, new RectF(0, 0, WIDTH, HEIGHT), p);
        } else {
            p.setShader(new LinearGradient(0, 0, WIDTH, HEIGHT,
                    new int[]{0xFFE17C57, 0xFFAD897A, 0xFF35343A, 0xFF1E2026},
                    new float[]{0f, 0.30f, 0.70f, 1f}, Shader.TileMode.CLAMP));
            canvas.drawRect(0, 0, WIDTH, HEIGHT, p);
            p.setShader(null);
        }
        p.setColor(Color.argb(Math.round(state.darkness * 255 / 110f), 17, 18, 22));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, p);
        p.setShader(new LinearGradient(0, 0, 0, HEIGHT,
                new int[]{0x00FFFFFF, 0x00000000, 0xA9000000},
                new float[]{0f, 0.57f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawRect(0, 0, WIDTH, HEIGHT, p);
        p.setShader(null);
    }

    private void card(Canvas canvas, EditorState state, Bitmap artwork, Bitmap avatar) {
        float w = WIDTH * state.cardPercent / 100f;
        float x = (WIDTH - w) / 2f;
        float imageH = "1:1".equals(state.ratio) ? w : w * 4f / 3f;
        float headerH = 104f * w / 864f;
        float fontScale = w / 864f;
        float pad = 26f * fontScale;

        SpannableString footerText = footerText(state.profile().username, state.caption, state.hashtags);
        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG);
        textPaint.setColor(INK);
        textPaint.setTypeface(Typeface.create("sans-serif", Typeface.NORMAL));
        textPaint.setTextSize(29f * fontScale);
        int maxWidth = Math.max(20, (int) (w - pad * 2f));
        StaticLayout footerLayout = StaticLayout.Builder
                .obtain(footerText, 0, footerText.length(), textPaint, maxWidth)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setIncludePad(false)
                .setMaxLines(3)
                .setEllipsize(TextUtils.TruncateAt.END)
                .setBreakStrategy(Layout.BREAK_STRATEGY_SIMPLE)
                .build();
        float footerH = Math.max(108f * fontScale, footerLayout.getHeight() + 38f * fontScale);
        float total = headerH + imageH + footerH;
        float y = (HEIGHT - total) / 2f + state.verticalOffset;
        y = Math.max(48f, Math.min(y, HEIGHT - total - 48f));
        float radius = 30f * fontScale;

        // Sombra ambiente muito discreta: sem a borda preta dura ou offset evidente.
        // O mesmo renderizador é usado no preview e no arquivo exportado.
        p.reset();
        p.setAntiAlias(true);
        p.setFilterBitmap(true);
        p.setColor(Color.WHITE);
        p.setShadowLayer(14f * fontScale, 0f, 3f * fontScale, 0x23000000);
        canvas.drawRoundRect(x, y, x + w, y + total, radius, radius, p);
        p.clearShadowLayer();

        int clip = canvas.save();
        Path clipPath = new Path();
        clipPath.addRoundRect(new RectF(x, y, x + w, y + total), radius, radius, Path.Direction.CW);
        canvas.clipPath(clipPath);

        // Barra superior inspirada no compartilhamento de uma publicação.
        p.setColor(Color.WHITE);
        canvas.drawRect(x, y, x + w, y + headerH, p);
        float avatarRadius = 31f * fontScale;
        float avatarCx = x + 57f * fontScale;
        float avatarCy = y + headerH / 2f;
        drawAvatar(canvas, avatar, avatarCx, avatarCy, avatarRadius);
        p.setColor(INK);
        p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        p.setTextSize(37f * fontScale);
        p.setShader(null);
        p.setTextAlign(Paint.Align.LEFT);
        String username = state.profile().username.trim();
        if (username.isEmpty()) username = "usuario";
        String visibleUsername = ellipsize(username, p, w - 122f * fontScale);
        canvas.drawText(visibleUsername, x + 119f * fontScale, avatarCy + 13f * fontScale, p);

        // A imagem ocupa toda a largura do cartão, sem molduras artificiais.
        RectF imageBounds = new RectF(x, y + headerH, x + w, y + headerH + imageH);
        p.setColor(0xFF2E3035);
        canvas.drawRect(imageBounds, p);
        if (artwork != null && !artwork.isRecycled()) {
            p.setColor(Color.WHITE);
            if (state.cropToFill) {
                cover(canvas, artwork, imageBounds, p);
            } else {
                contain(canvas, artwork, imageBounds, p);
            }
        } else {
            p.setShader(new LinearGradient(x, imageBounds.top, x + w, imageBounds.bottom,
                    0xFFEB815D, 0xFF24252D, Shader.TileMode.CLAMP));
            canvas.drawRect(imageBounds, p);
            p.setShader(null);
            p.setColor(0x70FFFFFF);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(3f * fontScale);
            canvas.drawRoundRect(x + 65f * fontScale, imageBounds.top + imageH * .20f,
                    x + w - 65f * fontScale, imageBounds.bottom - imageH * .20f,
                    15f * fontScale, 15f * fontScale, p);
            p.setStyle(Paint.Style.FILL);
            p.setColor(Color.WHITE);
            p.setTextAlign(Paint.Align.CENTER);
            p.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
            p.setTextSize(38f * fontScale);
            canvas.drawText("SUA ARTE AQUI", x + w / 2, imageBounds.centerY(), p);
            p.setTextSize(21f * fontScale);
            p.setColor(0xDDFFFFFF);
            canvas.drawText("Toque em IMPORTAR ARTE", x + w / 2,
                    imageBounds.centerY() + 40f * fontScale, p);
            p.setTextAlign(Paint.Align.LEFT);
        }

        p.setColor(Color.WHITE);
        canvas.drawRect(x, imageBounds.bottom, x + w, y + total, p);
        int save = canvas.save();
        canvas.translate(x + pad, imageBounds.bottom + (footerH - footerLayout.getHeight()) / 2f);
        footerLayout.draw(canvas);
        canvas.restoreToCount(save);
        canvas.restoreToCount(clip);
    }

    private static String ellipsize(String value, Paint paint, float width) {
        if (paint.measureText(value) <= width) return value;
        String suffix = "…";
        while (!value.isEmpty() && paint.measureText(value + suffix) > width) {
            value = value.substring(0, value.length() - 1);
        }
        return value + suffix;
    }

    private static SpannableString footerText(String username, String caption, String tags) {
        username = username.trim().isEmpty() ? "usuario" : username.trim();
        String normalizedTags = normalizeHashtags(tags);
        StringBuilder builder = new StringBuilder(username);
        if (!caption.trim().isEmpty()) builder.append("  ").append(caption.trim());
        if (!normalizedTags.isEmpty()) builder.append("  ").append(normalizedTags);
        SpannableString rich = new SpannableString(builder.toString());
        rich.setSpan(new StyleSpan(Typeface.BOLD), 0, username.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        Matcher matcher = Pattern.compile("#[\\p{L}\\p{N}_]+").matcher(rich);
        while (matcher.find()) {
            rich.setSpan(new ForegroundColorSpan(HASHTAG),
                    matcher.start(), matcher.end(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        }
        return rich;
    }

    private static String normalizeHashtags(String value) {
        String[] parts = value.trim().split("\\s+");
        StringBuilder result = new StringBuilder();
        for (String part : parts) {
            String clean = part.trim();
            if (clean.isEmpty()) continue;
            if (!clean.startsWith("#")) clean = "#" + clean;
            if (result.length() > 0) result.append(' ');
            result.append(clean);
        }
        return result.toString();
    }

    private void drawAvatar(Canvas canvas, Bitmap avatar, float cx, float cy, float radius) {
        p.setShader(null);
        p.setColor(0xFF222329);
        canvas.drawCircle(cx, cy, radius, p);
        if (avatar != null && !avatar.isRecycled()) {
            int save = canvas.save();
            Path circle = new Path();
            circle.addCircle(cx, cy, radius, Path.Direction.CW);
            canvas.clipPath(circle);
            p.setColor(Color.WHITE);
            cover(canvas, avatar, new RectF(cx - radius, cy - radius, cx + radius, cy + radius), p);
            canvas.restoreToCount(save);
        } else {
            p.setColor(Color.WHITE);
            p.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
            p.setTextAlign(Paint.Align.CENTER);
            p.setTextSize(radius * 1.15f);
            canvas.drawText("@", cx, cy + radius * .36f, p);
            p.setTextAlign(Paint.Align.LEFT);
        }
    }

    private static void cover(Canvas canvas, Bitmap image, RectF bounds, Paint paint) {
        float factor = Math.max(bounds.width() / image.getWidth(), bounds.height() / image.getHeight());
        float sw = bounds.width() / factor;
        float sh = bounds.height() / factor;
        float left = (image.getWidth() - sw) / 2;
        float top = (image.getHeight() - sh) / 2;
        Rect src = new Rect(Math.round(left), Math.round(top),
                Math.round(left + sw), Math.round(top + sh));
        canvas.drawBitmap(image, src, bounds, paint);
    }

    private static void contain(Canvas canvas, Bitmap image, RectF bounds, Paint paint) {
        float factor = Math.min(bounds.width() / image.getWidth(), bounds.height() / image.getHeight());
        float width = image.getWidth() * factor;
        float height = image.getHeight() * factor;
        canvas.drawBitmap(image, null, new RectF(
                bounds.centerX() - width / 2, bounds.centerY() - height / 2,
                bounds.centerX() + width / 2, bounds.centerY() + height / 2), paint);
    }

    /** Desfoque em resolução reduzida: não depende de rede ou RenderScript. */
    static Bitmap blurBackdrop(Bitmap art) {
        if (art == null) return null;
        final int w = 100;
        final int h = 178;
        Bitmap small = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(small);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        cover(c, art, new RectF(0, 0, w, h), paint);
        int[] source = new int[w * h];
        int[] target = new int[w * h];
        small.getPixels(source, 0, w, 0, 0, w, h);
        for (int pass = 0; pass < 3; pass++) {
            blurPass(source, target, w, h, 7, true);
            blurPass(target, source, w, h, 7, false);
        }
        small.setPixels(source, 0, w, 0, 0, w, h);
        return small;
    }

    private static void blurPass(int[] src, int[] dst, int width, int height,
                                 int radius, boolean horizontal) {
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int r = 0, g = 0, b = 0, a = 0, count = 0;
                for (int delta = -radius; delta <= radius; delta++) {
                    int xx = horizontal ? Math.max(0, Math.min(width - 1, x + delta)) : x;
                    int yy = horizontal ? y : Math.max(0, Math.min(height - 1, y + delta));
                    int v = src[yy * width + xx];
                    a += Color.alpha(v);
                    r += Color.red(v);
                    g += Color.green(v);
                    b += Color.blue(v);
                    count++;
                }
                dst[y * width + x] = Color.argb(a / count, r / count, g / count, b / count);
            }
        }
    }
}

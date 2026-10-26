package com.conreo.couchytv;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;

public final class OwnIcons {
    public static Drawable drawable(Context c, String name) {
        Bitmap b = Bitmap.createBitmap(72, 72, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(b);
        draw(canvas, name, 72, 72);
        BitmapDrawable d = new BitmapDrawable(c.getResources(), b);
        int size = (int) (24 * c.getResources().getDisplayMetrics().density);
        d.setBounds(0, 0, size, size);
        return d;
    }

    public static void draw(Canvas c, String name, int width, int height) {
        c.save();
        c.scale(width / 72f, height / 72f);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setColor(0xFF8DD5FF);
        p.setStrokeWidth(4);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeCap(Paint.Cap.ROUND);
        p.setStrokeJoin(Paint.Join.ROUND);
        String n = name.toLowerCase(java.util.Locale.ROOT);
        if (n.contains("sobre") || n.contains("licen") || n.contains("crédito")) {
            c.drawCircle(36, 36, 25, p);
            c.drawCircle(36, 23, 1.5f, p);
            c.drawLine(36, 33, 36, 50, p);
        } else if (n.contains("notícia")
                || n.contains("manchete")
                || (n.contains("temas") || n.startsWith("tema"))) {
            c.drawRoundRect(9, 13, 63, 59, 4, 4, p);
            c.drawRect(17, 22, 33, 37, p);
            c.drawLine(41, 23, 55, 23, p);
            c.drawLine(41, 33, 55, 33, p);
            c.drawLine(17, 46, 55, 46, p);
        } else if (n.contains("favorito")
                || n.contains("ordem")
                || (n.contains("mover") && !n.contains("remover"))) {
            Path star = new Path();
            for (int i = 0; i < 10; i++) {
                double angle = -Math.PI / 2 + i * Math.PI / 5;
                float radius = i % 2 == 0 ? 26 : 12;
                float x = 36 + (float) Math.cos(angle) * radius,
                        y = 36 + (float) Math.sin(angle) * radius;
                if (i == 0) star.moveTo(x, y);
                else star.lineTo(x, y);
            }
            star.close();
            c.drawPath(star, p);
        } else if (n.contains("hdmi") || n.contains("entrada")) {
            Path port = new Path();
            port.moveTo(12, 22);
            port.lineTo(60, 22);
            port.lineTo(56, 47);
            port.lineTo(16, 47);
            port.close();
            c.drawPath(port, p);
            for (int i = 0; i < 5; i++) c.drawLine(23 + i * 7, 30, 23 + i * 7, 38, p);
            c.drawLine(36, 48, 36, 62, p);
        } else if (n.contains("estrela")) {
            Path star = new Path();
            for (int k = 0; k < 10; k++) {
                double a = -Math.PI / 2 + k * Math.PI / 5;
                float r = k % 2 == 0 ? 27 : 12;
                float x = 36 + (float) Math.cos(a) * r, y = 36 + (float) Math.sin(a) * r;
                if (k == 0) star.moveTo(x, y);
                else star.lineTo(x, y);
            }
            star.close();
            c.drawPath(star, p);
        } else if (n.contains("instagram")) {
            c.drawRoundRect(11, 11, 61, 61, 13, 13, p);
            c.drawCircle(36, 36, 12, p);
            c.drawCircle(51, 21, 2, p);
        } else if (n.contains("site") || n.contains("website") || n.contains("github")) {
            c.drawCircle(36, 36, 25, p);
            c.drawOval(24, 11, 48, 61, p);
            c.drawLine(12, 36, 60, 36, p);
            c.drawArc(13, 17, 59, 44, 0, 180, false, p);
        } else if (n.contains("vídeo") || n.contains("tela") || n.contains("picture")) {
            c.drawRoundRect(8, 14, 64, 51, 4, 4, p);
            c.drawLine(36, 52, 36, 61, p);
            c.drawLine(23, 62, 49, 62, p);
        } else if (n.contains("original") || n.contains("home") || n.contains("central")) {
            Path roof = new Path();
            roof.moveTo(9, 33);
            roof.lineTo(36, 11);
            roof.lineTo(63, 33);
            c.drawPath(roof, p);
            c.drawLine(16, 29, 16, 60, p);
            c.drawLine(56, 29, 56, 60, p);
            c.drawLine(16, 60, 56, 60, p);
            c.drawRect(29, 40, 43, 60, p);
        } else if (n.contains("parar") || n.contains("remover") || n.contains("desativ")) {
            c.drawRoundRect(15, 15, 57, 57, 5, 5, p);
            c.drawLine(27, 27, 45, 45, p);
            c.drawLine(45, 27, 27, 45, p);
        } else if (n.contains("atualizar") || n.contains("recomenda")) {
            c.drawArc(12, 12, 60, 60, 35, 290, false, p);
            c.drawLine(58, 17, 58, 34, p);
            c.drawLine(58, 34, 42, 31, p);
        } else if (n.contains("aplicativo")
                || n.contains("app")
                || n.contains("card")
                || n.contains("adicionar")) {
            for (int y = 0; y < 2; y++)
                for (int x = 0; x < 2; x++)
                    c.drawRoundRect(12 + x * 28, 12 + y * 28, 31 + x * 28, 31 + y * 28, 3, 3, p);
        } else if (n.contains("câm") || n.contains("cam") || n.contains("dvr")) {
            c.drawRoundRect(8, 16, 59, 49, 5, 5, p);
            c.drawCircle(34, 32, 10, p);
            c.drawLine(34, 50, 34, 61, p);
            c.drawLine(21, 62, 48, 62, p);
            c.drawLine(62, 24, 67, 20, p);
        } else if (n.contains("jog")
                || n.contains("game")
                || n.contains("controle")
                || n.contains("ps5")
                || n.contains("fortnite")) {
            c.drawRoundRect(8, 22, 64, 53, 12, 12, p);
            c.drawLine(18, 37, 34, 37, p);
            c.drawLine(26, 29, 26, 45, p);
            c.drawCircle(48, 32, 2, p);
            c.drawCircle(55, 41, 2, p);
        } else if (n.contains("voltar") || n.contains("anterior")) {
            c.drawLine(52, 36, 17, 36, p);
            c.drawLine(17, 36, 31, 22, p);
            c.drawLine(17, 36, 31, 50, p);
        } else if (n.contains("hora") || n.contains("fuso") || n.contains("data")) {
            c.drawCircle(36, 36, 25, p);
            c.drawLine(36, 36, 36, 20, p);
            c.drawLine(36, 36, 48, 42, p);
        } else if (n.contains("fundo")
                || n.contains("wall")
                || n.contains("paisage")
                || n.contains("foto")
                || n.contains("imagem")) {
            c.drawRoundRect(8, 12, 64, 59, 4, 4, p);
            Path x = new Path();
            x.moveTo(12, 52);
            x.lineTo(29, 33);
            x.lineTo(42, 46);
            x.lineTo(51, 37);
            x.lineTo(61, 51);
            c.drawPath(x, p);
            c.drawCircle(48, 25, 4, p);
        } else if (n.contains("youtube") || n.contains("inscri") || n.contains("play")) {
            c.drawRoundRect(7, 17, 65, 55, 9, 9, p);
            Path x = new Path();
            x.moveTo(31, 27);
            x.lineTo(46, 36);
            x.lineTo(31, 45);
            x.close();
            c.drawPath(x, p);
        } else if (n.contains("rede")
                || n.contains("internet")
                || n.contains("wifi")
                || n.contains("wi-fi")
                || n.contains("ssh")) {
            for (int i = 0; i < 3; i++)
                c.drawArc(10 + i * 8, 17 + i * 9, 62 - i * 8, 65 - i * 2, 215, 110, false, p);
            c.drawCircle(36, 57, 2, p);
        } else if (n.contains("salvar")) {
            c.drawRoundRect(12, 10, 60, 62, 4, 4, p);
            c.drawRect(23, 10, 48, 29, p);
            c.drawRect(23, 41, 48, 60, p);
        } else {
            c.drawRoundRect(12, 12, 60, 60, 10, 10, p);
            c.drawCircle(36, 36, 11, p);
            c.drawLine(36, 6, 36, 13, p);
            c.drawLine(36, 59, 36, 66, p);
            c.drawLine(6, 36, 13, 36, p);
            c.drawLine(59, 36, 66, 36, p);
        }
        c.restore();
    }
}

package com.conreo.couchytv;

import android.content.Context;
import org.json.*;

public final class OwnCards {
    public static JSONArray read(Context c, String group) {
        try {
            String raw = c.getSharedPreferences("own_cards", 0).getString(group, null);
            if (raw != null) return new JSONArray(raw);
        } catch (Exception ignored) {
        }
        JSONArray a = new JSONArray();
        try {
            if (group.equals("hero")) {
                a.put(
                        card(
                                "PlayStation 5",
                                "Fortnite · sua próxima partida",
                                "ep:ps5",
                                "fortnite",
                                .75));
                a.put(card("Câmeras", "Sua casa, ao vivo", "ep:cameras", "camera", .7));

            } else if (group.equals("games")) {
                a.put(
                        card(
                                "Jogar no PS5",
                                "Abrir a entrada do console",
                                "ep:hdmi",
                                "fortnite",
                                .7));
                a.put(
                        card(
                                "Game Master",
                                "Ajustes de imagem e desempenho",
                                "ep:game_mode",
                                "game",
                                .65));
                a.put(card("Entradas HDMI", "Escolher outra entrada", "ep:inputs", "game", .65));
            }
        } catch (Exception ignored) {
        }
        return a;
    }

    private static JSONObject card(
            String title, String sub, String action, String art, double opacity)
            throws JSONException {
        return new JSONObject()
                .put("title", title)
                .put("subtitle", sub)
                .put("action", action)
                .put("art", art)
                .put("opacity", opacity);
    }

    public static void save(Context c, String group, JSONArray a) {
        c.getSharedPreferences("own_cards", 0).edit().putString(group, a.toString()).apply();
    }
}

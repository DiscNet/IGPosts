package com.discnet.igposts;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.ArrayList;

final class EditorState {
    static final String PREFS = "igposts_editor_v1";

    String artworkPath = "";
    String caption = "";
    String hashtags = "#graphicdesign #designgrafico #editorialdesign #design";
    String ratio = "3:4";
    boolean cropToFill = true;
    boolean instagramCrop = true; // 4:5 share view, keeps original art intact
    boolean glassChrome = false;
    boolean glassImageBorder = false;
    int cardPercent = 80;
    int verticalOffset = 0;
    int darkness = 39;
    int selectedProfile = 0;
    final ArrayList<Profile> profiles = new ArrayList<>();

    static final class Profile {
        String username;
        String avatarPath;

        Profile(String username, String avatarPath) {
            this.username = username;
            this.avatarPath = avatarPath;
        }
    }

    EditorState() {
        profiles.add(new Profile("kxlynn666", ""));
    }

    Profile profile() {
        if (profiles.isEmpty()) profiles.add(new Profile("kxlynn666", ""));
        selectedProfile = Math.max(0, Math.min(selectedProfile, profiles.size() - 1));
        return profiles.get(selectedProfile);
    }

    void save(Context context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit().putString("state", toJson().toString()).apply();
    }

    JSONObject toJson() {
        JSONObject json = new JSONObject();
        try {
            json.put("artworkPath", artworkPath);
            json.put("caption", caption);
            json.put("hashtags", hashtags);
            json.put("ratio", ratio);
            json.put("cropToFill", cropToFill);
            json.put("instagramCrop", instagramCrop);
            json.put("glassChrome", glassChrome);
            json.put("glassImageBorder", glassImageBorder);
            json.put("cardPercent", cardPercent);
            json.put("verticalOffset", verticalOffset);
            json.put("darkness", darkness);
            json.put("selectedProfile", selectedProfile);
            JSONArray profilesJson = new JSONArray();
            for (Profile profile : profiles) {
                JSONObject item = new JSONObject();
                item.put("username", profile.username);
                item.put("avatarPath", profile.avatarPath);
                profilesJson.put(item);
            }
            json.put("profiles", profilesJson);
        } catch (Exception ignored) { }
        return json;
    }

    static EditorState load(Context context) {
        SharedPreferences preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        EditorState state = new EditorState();
        String raw = preferences.getString("state", "");
        if (raw == null || raw.isEmpty()) return state;
        try {
            JSONObject json = new JSONObject(raw);
            state.artworkPath = json.optString("artworkPath", "");
            state.caption = json.optString("caption", "");
            state.hashtags = json.optString("hashtags", "");
            state.ratio = "1:1".equals(json.optString("ratio")) ? "1:1" : "3:4";
            state.cropToFill = json.optBoolean("cropToFill", true);
            state.instagramCrop = json.optBoolean("instagramCrop", true);
            state.glassChrome = json.optBoolean("glassChrome", false);
            state.glassImageBorder = json.optBoolean("glassImageBorder", false);
            state.cardPercent = clamp(json.optInt("cardPercent", 80), 65, 94);
            state.verticalOffset = clamp(json.optInt("verticalOffset", 0), -350, 350);
            state.darkness = clamp(json.optInt("darkness", 39), 0, 85);
            state.selectedProfile = json.optInt("selectedProfile", 0);
            JSONArray array = json.optJSONArray("profiles");
            if (array != null && array.length() > 0) {
                state.profiles.clear();
                for (int i = 0; i < array.length(); i++) {
                    JSONObject item = array.optJSONObject(i);
                    if (item != null) {
                        state.profiles.add(new Profile(
                                item.optString("username", "usuario"),
                                item.optString("avatarPath", "")));
                    }
                }
            }
            state.profile();
        } catch (Exception ignored) {
            return new EditorState();
        }
        return state;
    }

    static int clamp(int v, int low, int high) {
        return Math.max(low, Math.min(high, v));
    }
}

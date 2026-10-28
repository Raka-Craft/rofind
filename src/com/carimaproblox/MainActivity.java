package com.carimaproblox;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;
import java.net.URLDecoder;

public class MainActivity extends Activity {
    private EditText input;
    private TextView status;
    private TextView processText;
    private ProgressBar processBar;
    private LinearLayout results;
    private Handler main = new Handler(Looper.getMainLooper());
    private List<Game> lastGames = new ArrayList<Game>();
    private String lastQuery = "";
    private int sortMode = 0; // 0 = paling banyak dimainin, 1 = paling populer, 2 = paling sedikit dimainin

    private static class Game {
        long universeId; long rootPlaceId; String title; int players; long visits; long upVotes; String thumb;
        Game(long u, long r, String t, int p) { universeId = u; rootPlaceId = r; title = t; players = p; }
    }

    private static final Map<String,String> DICT = new HashMap<String,String>();
    static {
        String[][] d = {
            {"mencuri","steal"},{"curi","steal"},{"telur","egg"},{"buah","fruit"},
            {"otak","brain"},{"brainrot","brainrot"},{"hewan","pet"},{"peliharaan","pet"},
            {"kucing","cat"},{"anjing","dog"},{"mobil","car"},{"kota","city"},
            {"rumah","house"},{"horor","horror"},{"hantu","horror"},{"menakutkan","horror"},
            {"perang","war"},{"tembak","shooting"},{"tembakan","shooting"},{"balap","race"},
            {"balapan","race"},{"racing","race"},{"lari","run"},{"parkour","obby"},{"rintangan","obby"},
            {"memasak","cooking"},{"masak","cooking"},{"restoran","restaurant"},
            {"sekolah","school"},{"penjara","prison"},{"kabur","escape"},{"melarikan","escape"},
            {"zombie","zombie"},{"naga","dragon"},{"dinosaurus","dinosaur"},{"laut","ocean"},
            {"angkasa","space"},{"pesawat","plane"},{"kereta","train"},{"bola","soccer"},
            {"sepak","soccer"},{"memancing","fishing"},{"pancing","fishing"},
            {"bertani","farming"},{"kebun","garden"},{"taman","garden"},{"berkebun","garden"},
            {"tower","tower"},{"menara","tower"},{"sihir","magic"},{"pedang","sword"},
            {"bertarung","fighting"},{"tinju","boxing"},{"olahraga","sport"},
            {"musik","music"},{"menari","dance"},{"pakaian","fashion"},{"kecantikan","beauty"},
            {"makanan","food"},{"pizza","pizza"},{"burger","burger"},{"donat","donut"},
            {"emas","gold"},{"uang","money"},{"kaya","rich"},{"miskin","poor"},
            {"bayi","baby"},{"keluarga","family"},{"roleplay","roleplay"},{"berperan","roleplay"},
            {"petualangan","adventure"},{"misteri","mystery"},{"puzzle","puzzle"},
            {"simulator","simulator"},{"tycoon","tycoon"},{"tambang","mining"},
            {"lautan","ocean"},{"pulau","island"},{"hutan","forest"},{"salju","snow"},
            {"robot","robot"},{"alien","alien"},{"vampire","vampire"},{"penyihir","witch"},
            {"pahlawan","hero"},{"penjahat","villain"},{"polisi","police"},{"pencuri","thief"},
            {"maling","thief"},{"toko","shop"},{"pasar","market"},{"bisnis","business"},
            {"bulan","moon"},{"pantai","beach"},{"gunung","mountain"},{"desa","village"},
            {"peternakan","farm"},{"konstruksi","building"},{"bangunan","building"},
        };
        for (String[] p : d) DICT.put(p[0], p[1]);
    }
    private static final String[] STOP = {"game","yang","bisa","mau","aku","mainin","cari","tentang","di","dan","untuk","dengan","itu","ini","adalah","ada","seperti","kaya","kayak","saya","tolong","carikan","map","roblox","nya","ke","dari","atau","si","dong"};

    private GradientDrawable rounded(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        return d;
    }

    private String translate(String q) {
        String low = q.toLowerCase().replaceAll("[^a-z0-9 ]", " ").trim();
        String[] words = low.split("\\s+");
        List<String> out = new ArrayList<String>();
        for (String w : words) {
            boolean stop = false;
            for (String s : STOP) if (s.equals(w)) { stop = true; break; }
            if (stop) continue;
            String t = DICT.get(w);
            out.add(t != null ? t : w);
        }
        if (out.isEmpty()) return low;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < out.size(); i++) { if (i > 0) sb.append(" "); sb.append(out.get(i)); }
        return sb.toString();
    }

    private String httpGet(String urlStr) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("GET");
        c.setConnectTimeout(20000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        c.setRequestProperty("Accept", "*/*");
        c.setRequestProperty("Accept-Language", "en-US,en;q=0.9");
        int code = c.getResponseCode();
        InputStream in = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
        if (in == null) throw new Exception("HTTP " + code);
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
        return sb.toString();
    }

    private String httpPostJson(String urlStr, String body) throws Exception {
        HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
        c.setRequestMethod("POST");
        c.setConnectTimeout(20000);
        c.setReadTimeout(20000);
        c.setRequestProperty("User-Agent", "Mozilla/5.0");
        c.setRequestProperty("Accept", "application/json");
        c.setRequestProperty("Content-Type", "application/json");
        c.setDoOutput(true);
        OutputStream os = c.getOutputStream();
        os.write(body.getBytes("UTF-8"));
        os.close();
        int code = c.getResponseCode();
        InputStream in = (code >= 200 && code < 300) ? c.getInputStream() : c.getErrorStream();
        if (in == null) throw new Exception("HTTP " + code);
        BufferedReader r = new BufferedReader(new InputStreamReader(in, "UTF-8"));
        StringBuilder sb = new StringBuilder(); String line;
        while ((line = r.readLine()) != null) sb.append(line);
        r.close();
        if (code < 200 || code >= 300) throw new Exception("HTTP " + code);
        return sb.toString();
    }

    // Parser umum: telusuri JSON apa pun, ambil objek yang punya nama + id game.
    private void walk(Object o, List<Game> out, Set<Long> seen) {
        try {
            if (o instanceof JSONObject) {
                JSONObject j = (JSONObject) o;
                long uid = 0;
                if (j.has("universeId")) uid = j.optLong("universeId", 0);
                if (uid == 0 && j.has("universe_id")) uid = j.optLong("universe_id", 0);
                String title = j.optString("title", "");
                if (title.isEmpty()) title = j.optString("name", "");
                if (uid != 0 && !title.isEmpty() && !seen.contains(uid)) {
                    int players = j.optInt("playerCount", 0);
                    if (players == 0) players = j.optInt("playing", 0);
                    if (players == 0) players = j.optInt("players", 0);
                    long visits = j.optLong("visits", 0);
                    if (visits == 0) visits = j.optLong("totalVisits", 0);
                    long up = j.optLong("totalUpVotes", 0);
                    if (up == 0) up = j.optLong("upVotes", 0);
                    long root = j.optLong("rootPlaceId", 0);
                    if (root == 0) root = j.optLong("placeId", 0);
                    Game g = new Game(uid, root, title, players);
                    g.visits = visits; g.upVotes = up;
                    out.add(g); seen.add(uid);
                }
                // Objek katalog (asset tempat): Id + Name, tanpa universeId
                if (uid == 0 && j.has("Id") && j.has("Name")) {
                    long aid = j.optLong("Id", 0);
                    String nm = j.optString("Name", "");
                    if (aid != 0 && !nm.isEmpty() && !seen.contains(-aid)) {
                        Game g = new Game(aid, aid, nm, 0);
                        out.add(g); seen.add(-aid);
                    }
                }
                Iterator<String> keys = j.keys();
                while (keys.hasNext()) walk(j.opt(keys.next()), out, seen);
            } else if (o instanceof JSONArray) {
                JSONArray a = (JSONArray) o;
                for (int i = 0; i < a.length(); i++) walk(a.opt(i), out, seen);
            }
        } catch (Exception e) { }
    }

    private List<Game> parseGamesGeneric(String json) {
        List<Game> list = new ArrayList<Game>();
        Set<Long> seen = new HashSet<Long>();
        try {
            if (json.trim().startsWith("[")) walk(new JSONArray(json), list, seen);
            else walk(new JSONObject(json), list, seen);
        } catch (Exception e) { }
        return list;
    }

    // Jalur mesin pencari: DuckDuckGo HTML, disaring ke roblox.com/games saja.
    // Ini yang menyaring berdasarkan kata kunci (bukan tile statis seperti Discover).
    private List<Game> parseDDG(String html) {
        List<Game> list = new ArrayList<Game>();
        Set<Long> seen = new HashSet<Long>();
        try {
            Pattern p = Pattern.compile("class=\"result__a\"[^>]*href=\"[^\"]*uddg=([^\"&]+)[^>]*>(.*?)</a>", Pattern.DOTALL);
            Matcher m = p.matcher(html);
            while (m.find()) {
                String decoded = "";
                try { decoded = URLDecoder.decode(m.group(1), "UTF-8"); } catch (Exception ignored) { continue; }
                if (!decoded.contains("roblox.com/") || !decoded.contains("/games/")) continue;
                Matcher gm = Pattern.compile("/games/(\\d+)/([^\\?#\\s/]+)").matcher(decoded);
                if (!gm.find()) continue;
                long placeId = Long.parseLong(gm.group(1));
                if (seen.contains(placeId)) continue;
                seen.add(placeId);
                String title = m.group(2).replaceAll("<[^>]+>", "").trim();
                title = title.replace("&amp;", "&").replace("&#x27;", "'").replace("&quot;", "\"");
                if (title.isEmpty()) {
                    try { title = URLDecoder.decode(gm.group(2), "UTF-8").replace('-', ' '); } catch (Exception ignored) { }
                }
                if (title.isEmpty()) continue;
                list.add(new Game(0, placeId, title, 0));
                if (list.size() >= 30) break;
            }
        } catch (Exception e) { }
        return list;
    }

    private List<Game> parseBing(String html) {
        List<Game> list = new ArrayList<Game>();
        Set<Long> seen = new HashSet<Long>();
        try {
            Pattern p = Pattern.compile("roblox\\.com/(?:[a-z]{2}/)?games/(\\d+)/([^\"'\\?#\\s]+)");
            Matcher m = p.matcher(html);
            while (m.find()) {
                long placeId = Long.parseLong(m.group(1));
                if (seen.contains(placeId)) continue;
                seen.add(placeId);
                String title = m.group(2);
                try { title = URLDecoder.decode(title, "UTF-8"); } catch (Exception ignored) { }
                title = title.replace('-', ' ').trim();
                if (title.isEmpty()) continue;
                list.add(new Game(0, placeId, title, 0));
                if (list.size() >= 30) break;
            }
        } catch (Exception e) { }
        return list;
    }

    // Hanya simpan game yang judulnya mengandung minimal satu kata kunci pencarian.
    // Ini yang mencegah game event/unggulan acak (mis. The Hollow) muncul untuk pencarian lain.
    private List<Game> filterByKeyword(List<Game> games, String keywords) {
        List<Game> out = new ArrayList<Game>();
        String[] keys = keywords.toLowerCase().split("\\s+");
        for (Game g : games) {
            String t = g.title.toLowerCase();
            for (String k : keys) {
                if (k.length() >= 3 && t.contains(k)) { out.add(g); break; }
            }
        }
        return out;
    }

    // Jalur Discover: buka halaman Discover Roblox, ambil tile game + ID-nya.
    // Ini jalur yang terbukti jalan tanpa login (diuji langsung 28 Okt 2026).
    private List<Game> parseDiscover(String html) {
        List<Game> list = new ArrayList<Game>();
        Map<Long,Game> byPlace = new HashMap<Long,Game>();
        try {
            Pattern p = Pattern.compile("/games/(\\d+)/([^\"'\\?#\\s]+)[^\"']*?universeId=(\\d+)");
            Matcher m = p.matcher(html);
            while (m.find()) {
                long placeId = Long.parseLong(m.group(1));
                String slug = m.group(2);
                long uid = Long.parseLong(m.group(3));
                if (byPlace.containsKey(placeId)) continue;
                String title = slug;
                try { title = URLDecoder.decode(slug, "UTF-8"); } catch (Exception ignored) { }
                title = title.replace('-', ' ').trim();
                if (title.isEmpty()) continue;
                Game g = new Game(uid, placeId, title, 0);
                list.add(g); byPlace.put(placeId, g);
                if (list.size() >= 40) break;
            }
            // Tile tanpa parameter universeId: ambil placeId-nya saja, universeId dicari belakangan
            if (list.isEmpty()) {
                Pattern p2 = Pattern.compile("/games/(\\d+)/([^\"'\\?#\\s]+)");
                Matcher m2 = p2.matcher(html);
                Set<Long> seenPlace = new HashSet<Long>();
                while (m2.find()) {
                    long placeId = Long.parseLong(m2.group(1));
                    if (seenPlace.contains(placeId)) continue;
                    seenPlace.add(placeId);
                    String slug = m2.group(2);
                    String title = slug;
                    try { title = URLDecoder.decode(slug, "UTF-8"); } catch (Exception ignored) { }
                    title = title.replace('-', ' ').trim();
                    if (title.isEmpty()) continue;
                    list.add(new Game(0, placeId, title, 0));
                    if (list.size() >= 40) break;
                }
            }
        } catch (Exception e) { }
        return list;
    }

    private void resolveUniverses(List<Game> games) {
        for (Game g : games) {
            if (g.universeId != 0 || g.rootPlaceId == 0) continue;
            try {
                String json = httpGet("https://apis.roblox.com/universes/v1/places/" + g.rootPlaceId + "/universe");
                JSONObject o = new JSONObject(json);
                g.universeId = o.optLong("universeId", 0);
            } catch (Exception ignored) { }
        }
    }

    private void enrichGames(List<Game> games) {
        try {
            List<Game> withUid = new ArrayList<Game>();
            for (Game g : games) if (g.universeId != 0) withUid.add(g);
            if (withUid.isEmpty()) return;
            StringBuilder ids = new StringBuilder();
            for (int i = 0; i < withUid.size() && i < 30; i++) { if (i > 0) ids.append(","); ids.append(withUid.get(i).universeId); }
            String json = httpGet("https://games.roblox.com/v1/games?universeIds=" + ids);
            JSONObject root = new JSONObject(json);
            JSONArray data = root.optJSONArray("data");
            if (data != null) {
                Map<Long,Game> byId = new HashMap<Long,Game>();
                for (Game g : withUid) byId.put(g.universeId, g);
                for (int i = 0; i < data.length(); i++) {
                    JSONObject d = data.optJSONObject(i);
                    if (d == null) continue;
                    Game g = byId.get(d.optLong("id", 0));
                    if (g != null) {
                        g.players = d.optInt("playing", g.players);
                        g.visits = d.optLong("visits", g.visits);
                        String nm = d.optString("name", "");
                        if (!nm.isEmpty()) g.title = nm;
                        if (g.rootPlaceId == 0) g.rootPlaceId = d.optLong("rootPlaceId", 0);
                    }
                }
            }
        } catch (Exception ignored) { }
    }

    private void fetchThumbs(final List<Game> games) {
        // 1) Thumbnail map terbaru per universe
        try {
            StringBuilder ids = new StringBuilder();
            for (int i = 0; i < games.size(); i++) { if (i > 0) ids.append(","); ids.append(games.get(i).universeId); }
            String url = "https://thumbnails.roblox.com/v1/games/multiget/thumbnails?universeIds=" + ids
                    + "&countPerUniverse=1&defaults=true&size=768x432&format=Png&isCircular=false";
            String json = httpGet(url);
            JSONObject root = new JSONObject(json);
            JSONArray data = root.optJSONArray("data");
            if (data != null) {
                Map<Long,String> map = new HashMap<Long,String>();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject d = data.optJSONObject(i);
                    if (d == null) continue;
                    JSONArray th = d.optJSONArray("thumbnails");
                    if (th != null && th.length() > 0) {
                        String u = th.optJSONObject(0).optString("imageUrl", "");
                        if (!u.isEmpty()) map.put(d.optLong("targetId", 0), u);
                    }
                }
                for (Game g : games) if (map.containsKey(g.universeId)) g.thumb = map.get(g.universeId);
            }
        } catch (Exception e) { }
        // 2) Fallback: ikon game terbaru
        try {
            boolean need = false;
            for (Game g : games) if (g.thumb == null) need = true;
            if (!need) return;
            StringBuilder ids = new StringBuilder();
            for (int i = 0; i < games.size(); i++) { if (i > 0) ids.append(","); ids.append(games.get(i).universeId); }
            String url = "https://thumbnails.roblox.com/v1/games/icons?universeIds=" + ids + "&size=512x512&format=Png&isCircular=false";
            String json = httpGet(url);
            JSONObject root = new JSONObject(json);
            JSONArray data = root.optJSONArray("data");
            if (data != null) {
                Map<Long,String> map = new HashMap<Long,String>();
                for (int i = 0; i < data.length(); i++) {
                    JSONObject d = data.optJSONObject(i);
                    if (d == null) continue;
                    String u = d.optString("imageUrl", "");
                    if (!u.isEmpty()) map.put(d.optLong("targetId", 0), u);
                }
                for (Game g : games) if (g.thumb == null && map.containsKey(g.universeId)) g.thumb = map.get(g.universeId);
            }
        } catch (Exception e) { }
        // 3) Fallback: thumbnail aset (untuk hasil katalog)
        try {
            for (Game g : games) {
                if (g.thumb != null) continue;
                try {
                    String url = "https://thumbnails.roblox.com/v1/assets?assetIds=" + g.universeId + "&size=768x432&format=Png&isCircular=false";
                    String json = httpGet(url);
                    JSONObject root = new JSONObject(json);
                    JSONArray data = root.optJSONArray("data");
                    if (data != null && data.length() > 0) {
                        String u = data.optJSONObject(0).optString("imageUrl", "");
                        if (!u.isEmpty()) g.thumb = u;
                    }
                } catch (Exception ignored) { }
            }
        } catch (Exception e) { }
    }

    private void setProcess(final int step, final String text) {
        main.post(new Runnable() {
            public void run() {
                processBar.setProgress(step);
                processText.setText(text);
            }
        });
    }

    private void applySort() {
        if (sortMode == 0) {
            Collections.sort(lastGames, new Comparator<Game>() {
                public int compare(Game a, Game b) { return Integer.compare(b.players, a.players); }
            });
        } else if (sortMode == 1) {
            Collections.sort(lastGames, new Comparator<Game>() {
                public int compare(Game a, Game b) {
                    long va = a.visits > 0 ? a.visits : a.upVotes;
                    long vb = b.visits > 0 ? b.visits : b.upVotes;
                    if (va != vb) return Long.compare(vb, va);
                    return Integer.compare(b.players, a.players);
                }
            });
        } else {
            Collections.sort(lastGames, new Comparator<Game>() {
                public int compare(Game a, Game b) { return Integer.compare(a.players, b.players); }
            });
        }
    }

    private String sortLabel() {
        if (sortMode == 0) return "Paling Banyak Dimainin";
        if (sortMode == 1) return "Paling Populer";
        return "Paling Sedikit Dimainin";
    }

    private List<String> expandQueries(String rawQuery, String q) {
        List<String> out = new ArrayList<String>();
        String lowRaw = rawQuery.toLowerCase();
        String lowQ = q.toLowerCase();
        if (lowRaw.contains("balap") || lowQ.contains("race")) {
            out.add("race"); out.add("racing car"); out.add("racing motorcycle");
            out.add("car race"); out.add("bike race");
            return out;
        }
        if (lowQ.contains("steal") && lowQ.contains("egg")) {
            out.add("steal egg"); out.add("steal an egg"); out.add("egg steal");
            return out;
        }
        if (lowQ.contains("brainrot")) {
            out.add("steal brainrot"); out.add("brainrot");
            return out;
        }
        if (lowQ.contains("obby")) {
            out.add("obby"); out.add("parkour obby");
            return out;
        }
        if (lowQ.contains("blox") || lowQ.contains("fruit")) {
            out.add("blox fruits"); out.add("blox fruit");
            return out;
        }
        out.add(q);
        return out;
    }

    private void addCurated(List<Game> all, Set<Long> seen, String rawQuery, String q) {
        String low = (rawQuery + " " + q).toLowerCase();
        if (low.contains("balap") || low.contains("race")) {
            if (!seen.contains(3351674303L)) { seen.add(3351674303L); all.add(new Game(1202096104L, 3351674303L, "Driving Empire", 0)); }
            if (!seen.contains(9285238704L)) { seen.add(9285238704L); all.add(new Game(0, 9285238704L, "Race Clicker", 0)); }
        }
        if (low.contains("blox") || low.contains("fruit")) {
            if (!seen.contains(2753915549L)) { seen.add(2753915549L); all.add(new Game(0, 2753915549L, "Blox Fruits", 0)); }
        }
    }

    private void renderResults(String query, String keywords) {
        results.removeAllViews();
        if (lastGames.isEmpty()) {
            status.setText("Tidak ketemu untuk \"" + query + "\". Coba kata lain atau pilih kategori di atas.");
            return;
        }
        status.setText("Ketemu " + lastGames.size() + " game untuk \"" + query + "\" - " + sortLabel());
        LinearLayout row = null;
        for (int i = 0; i < lastGames.size(); i++) {
            if (i % 2 == 0) {
                row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                results.addView(row);
            }
            row.addView(makeCard(lastGames.get(i)));
        }
    }

    private LinearLayout makeCard(final Game g) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(Color.parseColor("#1A1F2B"));
        bg.setCornerRadius(24);
        bg.setStroke(2, Color.parseColor("#262E40"));
        card.setBackground(bg);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        cp.setMargins(10, 10, 10, 10);
        card.setLayoutParams(cp);
        card.setPadding(0, 0, 0, 16);

        final ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 260));
        img.setBackgroundColor(Color.parseColor("#0E1117"));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(img);

        TextView title = new TextView(this);
        title.setText(g.title);
        title.setTextColor(Color.parseColor("#E5E7EB"));
        title.setTextSize(13);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        title.setPadding(16, 12, 16, 4);
        title.setMaxLines(2);
        card.addView(title);

        TextView meta = new TextView(this);
        meta.setText(g.players + " pemain");
        meta.setTextColor(Color.parseColor("#9CA3AF"));
        meta.setTextSize(11);
        meta.setPadding(16, 0, 16, 0);
        card.addView(meta);

        card.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String link;
                    if (g.rootPlaceId != 0) link = "https://www.roblox.com/games/" + g.rootPlaceId;
                    else link = "https://www.roblox.com/discover/?Keyword=" + URLEncoder.encode(g.title, "UTF-8");
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Tidak bisa membuka link", Toast.LENGTH_SHORT).show();
                }
            }
        });
        if (g.thumb != null) {
            new Thread(new Runnable() {
                public void run() {
                    final Bitmap b = loadBitmap(g.thumb);
                    if (b != null) main.post(new Runnable() { public void run() { img.setImageBitmap(b); } });
                }
            }).start();
        }
        return card;
    }

    private void doSearch(final String rawQuery) {
        status.setText("");
        results.removeAllViews();
        setProcess(5, "Langkah 1/4: Memahami kategori yang kamu mau...");
        new Thread(new Runnable() {
            public void run() {
                try {
                    final String q = translate(rawQuery);
                    final List<String> queries = expandQueries(rawQuery, q);
                    List<Game> games = new ArrayList<Game>();
                    Set<Long> seenPlace = new HashSet<Long>();
                    // Mekanisme v6: satu kategori dipecah jadi banyak kata kunci,
                    // tiap kata kunci dicari, hasilnya digabung dan dideduplikasi.
                    for (int qi = 0; qi < queries.size(); qi++) {
                        String eq = queries.get(qi);
                        setProcess(20 + (qi * 40 / queries.size()), "Langkah 2/4: Mencari '" + eq + "' (" + (qi + 1) + "/" + queries.size() + ")...");
                        List<Game> found = new ArrayList<Game>();
                        try {
                            String ddgUrl = "https://html.duckduckgo.com/html/?q=" + URLEncoder.encode(eq + " roblox", "UTF-8");
                            found = parseDDG(httpGet(ddgUrl));
                        } catch (Exception ignored) { }
                        if (found.isEmpty()) {
                            try {
                                String bingUrl = "https://www.bing.com/search?q=" + URLEncoder.encode(eq + " roblox game", "UTF-8");
                                found = parseBing(httpGet(bingUrl));
                            } catch (Exception ignored) { }
                        }
                        for (Game g : found) {
                            if (g.rootPlaceId != 0 && !seenPlace.contains(g.rootPlaceId)) {
                                seenPlace.add(g.rootPlaceId);
                                games.add(g);
                            }
                        }
                        if (games.size() >= 16) break;
                    }
                    // Cadangan Discover, disaring judulnya
                    if (games.isEmpty()) {
                        try {
                            setProcess(60, "Langkah 2/4: Mencoba halaman Discover Roblox...");
                            String pageUrl = "https://www.roblox.com/discover/?Keyword=" + URLEncoder.encode(q, "UTF-8");
                            List<Game> disc = filterByKeyword(parseDiscover(httpGet(pageUrl)), q);
                            for (Game g : disc) {
                                if (g.rootPlaceId != 0 && !seenPlace.contains(g.rootPlaceId)) {
                                    seenPlace.add(g.rootPlaceId);
                                    games.add(g);
                                }
                            }
                        } catch (Exception ignored) { }
                    }
                    // Daftar kategori terverifikasi (ID game asli dari Roblox)
                    addCurated(games, seenPlace, rawQuery, q);
                    if (games.isEmpty()) {
                        main.post(new Runnable() {
                            public void run() {
                                status.setText("Tidak ketemu untuk \"" + rawQuery + "\". Coba kata lain atau pilih kategori di atas.");
                                setProcess(0, "Pencarian selesai, tidak ada hasil.");
                            }
                        });
                        return;
                    }
                    setProcess(70, "Langkah 3/4: Mengambil angka pemain dan foto terbaru...");
                    resolveUniverses(games);
                    enrichGames(games);
                    if (games.size() > 16) games = new ArrayList<Game>(games.subList(0, 16));
                    fetchThumbs(games);
                    lastGames = games;
                    lastQuery = rawQuery;
                    applySort();
                    setProcess(100, "Langkah 4/4: Selesai. " + games.size() + " game ditemukan.");
                    main.post(new Runnable() {
                        public void run() { renderResults(lastQuery, q); }
                    });
                } catch (final Exception e) {
                    main.post(new Runnable() {
                        public void run() {
                            status.setText("Gagal mencari. Cek internet lalu coba lagi.");
                            setProcess(0, "Pencarian gagal.");
                        }
                    });
                }
            }
        }).start();
    }

    private Bitmap loadBitmap(String urlStr) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(urlStr).openConnection();
            c.setConnectTimeout(15000); c.setReadTimeout(15000);
            c.setRequestProperty("User-Agent", "Mozilla/5.0");
            InputStream in = c.getInputStream();
            Bitmap b = BitmapFactory.decodeStream(in);
            in.close();
            return b;
        } catch (Exception e) { return null; }
    }

    private void showGame(final Game g) {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setBackground(rounded(Color.parseColor("#1A2338"), 28));
        card.setPadding(24, 24, 24, 24);
        LinearLayout.LayoutParams cp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cp.setMargins(0, 0, 0, 24);
        card.setLayoutParams(cp);

        final ImageView img = new ImageView(this);
        img.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 360));
        img.setBackgroundColor(Color.parseColor("#0B0F1A"));
        img.setScaleType(ImageView.ScaleType.CENTER_CROP);
        card.addView(img);

        TextView title = new TextView(this);
        title.setText(g.title);
        title.setTextColor(Color.WHITE);
        title.setTextSize(16);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setPadding(0, 20, 0, 8);
        card.addView(title);

        TextView meta = new TextView(this);
        String m = g.players + " pemain lagi main";
        if (g.visits > 0) m += "  •  " + g.visits + " total kunjungan";
        meta.setText(m);
        meta.setTextColor(Color.parseColor("#94A3B8"));
        meta.setTextSize(12);
        card.addView(meta);

        Button open = new Button(this);
        open.setText("BUKA DI ROBLOX");
        open.setBackground(rounded(Color.parseColor("#2563EB"), 20));
        open.setTextColor(Color.WHITE);
        open.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    String link;
                    if (g.rootPlaceId != 0) link = "https://www.roblox.com/games/" + g.rootPlaceId;
                    else link = "https://www.roblox.com/discover/?Keyword=" + URLEncoder.encode(g.title, "UTF-8");
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link)));
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this, "Tidak bisa membuka link", Toast.LENGTH_SHORT).show();
                }
            }
        });
        card.addView(open);
        results.addView(card);

        if (g.thumb != null) {
            new Thread(new Runnable() {
                public void run() {
                    final Bitmap b = loadBitmap(g.thumb);
                    if (b != null) main.post(new Runnable() { public void run() { img.setImageBitmap(b); } });
                }
            }).start();
        }
    }

    private Button catButton(final String label, final int mode) {
        Button b = new Button(this);
        b.setText(label);
        b.setBackground(rounded(Color.parseColor("#1E293B"), 20));
        b.setTextColor(Color.WHITE);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1);
        p.setMargins(6, 6, 6, 6);
        b.setLayoutParams(p);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sortMode = mode;
                if (!lastGames.isEmpty()) {
                    applySort();
                    renderResults(lastQuery, "");
                } else {
                    Toast.makeText(MainActivity.this, "Cari game dulu, baru pilih kategori", Toast.LENGTH_SHORT).show();
                }
            }
        });
        return b;
    }

    private Button chip(String label, final String query) {
        Button b = new Button(this);
        b.setText(label);
        b.setBackground(rounded(Color.parseColor("#1E293B"), 20));
        b.setTextColor(Color.WHITE);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { input.setText(query); doSearch(query); }
        });
        return b;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#0B0F1A"));

        ScrollView scroll = new ScrollView(this);
        LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1);
        scroll.setLayoutParams(sp);
        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(36, 48, 36, 24);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.VERTICAL);
        header.setPadding(28, 28, 28, 28);
        GradientDrawable hg = new GradientDrawable(GradientDrawable.Orientation.TL_BR, new int[]{Color.parseColor("#111827"), Color.parseColor("#1F2937")});
        hg.setCornerRadius(32);
        header.setBackground(hg);
        TextView head = new TextView(this);
        head.setText("ROFIND");
        head.setTextColor(Color.WHITE);
        head.setTextSize(18);
        head.setLetterSpacing(0.08f);
        head.setTypeface(Typeface.DEFAULT_BOLD);
        header.addView(head);
        TextView sub = new TextView(this);
        sub.setText("Cari game Roblox dengan bahasa sendiri. Hasil diambil langsung dari Roblox, fotonya selalu yang terbaru.");
        sub.setTextColor(Color.parseColor("#E0E7FF"));
        sub.setTextSize(12);
        header.addView(sub);
        content.addView(header);

        input = new EditText(this);
        input.setHint("Contoh: game balapan, game yang bisa mencuri telur");
        input.setTextColor(Color.WHITE);
        input.setHintTextColor(Color.parseColor("#64748B"));
        input.setBackground(rounded(Color.parseColor("#1E293B"), 20));
        input.setPadding(28, 28, 28, 28);
        LinearLayout.LayoutParams ip = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        ip.setMargins(0, 28, 0, 16);
        input.setLayoutParams(ip);
        content.addView(input);

        Button cari = new Button(this);
        cari.setText("CARI GAME");
        cari.setBackground(rounded(Color.parseColor("#38BDF8"), 20));
        cari.setTextColor(Color.WHITE);
        cari.setTypeface(Typeface.DEFAULT_BOLD);
        cari.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                String q = input.getText().toString().trim();
                if (q.isEmpty()) { Toast.makeText(MainActivity.this, "Tulis dulu gamenya", Toast.LENGTH_SHORT).show(); return; }
                doSearch(q);
            }
        });
        content.addView(cari);

        TextView catLabel = new TextView(this);
        catLabel.setText("KATEGORI HASIL PENCARIAN");
        catLabel.setTextColor(Color.parseColor("#94A3B8"));
        catLabel.setTextSize(13);
        catLabel.setTypeface(Typeface.DEFAULT_BOLD);
        catLabel.setPadding(0, 28, 0, 8);
        content.addView(catLabel);
        LinearLayout cats = new LinearLayout(this);
        cats.setOrientation(LinearLayout.HORIZONTAL);
        cats.addView(catButton("TERBANYAK", 0));
        cats.addView(catButton("TERPOPULER", 1));
        cats.addView(catButton("TERSEDIKIT", 2));
        content.addView(cats);

        TextView ex = new TextView(this);
        ex.setText("COBA CEPAT");
        ex.setTextColor(Color.parseColor("#94A3B8"));
        ex.setTextSize(13);
        ex.setTypeface(Typeface.DEFAULT_BOLD);
        ex.setPadding(0, 20, 0, 8);
        content.addView(ex);
        LinearLayout chips = new LinearLayout(this);
        chips.setOrientation(LinearLayout.HORIZONTAL);
        chips.addView(chip("Mencuri Telur", "game yang bisa mencuri telur"));
        chips.addView(chip("Balapan", "game balapan"));
        chips.addView(chip("Brainrot", "steal brainrot"));
        content.addView(chips);

        status = new TextView(this);
        status.setTextColor(Color.WHITE);
        status.setTextSize(15);
        status.setPadding(0, 28, 0, 8);
        content.addView(status);

        results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        content.addView(results);

        scroll.addView(content);
        root.addView(scroll);

        LinearLayout bottom = new LinearLayout(this);
        bottom.setOrientation(LinearLayout.VERTICAL);
        bottom.setBackgroundColor(Color.parseColor("#111827"));
        bottom.setPadding(36, 20, 36, 20);
        processText = new TextView(this);
        processText.setText("Siap mencari. Tulis game yang kamu cari di atas.");
        processText.setTextColor(Color.parseColor("#E5E7EB"));
        processText.setTextSize(14);
        bottom.addView(processText);
        processBar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        processBar.setMax(100);
        processBar.setProgress(0);
        bottom.addView(processBar);
        root.addView(bottom);

        setContentView(root);
    }
}

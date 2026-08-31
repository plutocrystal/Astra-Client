package dev.astra.gui.tabs;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.astra.util.ItemUtil;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Ported from LiquidBounce legacy (net.ccbluex.liquidbounce.tabs.HeadsTab).
 * Loads player heads asynchronously from the LiquidBounce cloud head database.
 */
public class AstraHeadsTab extends CreativeTabs {

    private static final String HEADS_CONFIG_URL = "https://cloud.liquidbounce.net/LiquidBounce/heads.json";

    private volatile List<ItemStack> heads = new ArrayList<>();

    public AstraHeadsTab() {
        super(13, "Astra Heads");
        Thread loader = new Thread(this::loadHeads, "Astra-HeadsLoader");
        loader.setDaemon(true);
        loader.start();
    }

    private void loadHeads() {
        try {
            JsonObject headsConf = fetchJson(HEADS_CONFIG_URL);
            if (headsConf == null || !headsConf.get("enabled").getAsBoolean()) return;

            String url = headsConf.get("url").getAsString();
            JsonObject headsMap = fetchJson(url);
            if (headsMap == null) return;

            List<ItemStack> loaded = new ArrayList<>();
            for (Map.Entry<String, JsonElement> entry : headsMap.entrySet()) {
                JsonObject head = entry.getValue().getAsJsonObject();
                String name = head.get("name").getAsString().replace("\"", "");
                String uuid = head.get("uuid").getAsString();
                String value = head.get("value").getAsString();
                ItemStack stack = ItemUtil.createItem("skull 1 3 {display:{Name:\"" + name
                        + "\"},SkullOwner:{Id:\"" + uuid + "\",Properties:{textures:[{Value:\"" + value + "\"}]}}}");
                if (stack != null) {
                    loaded.add(stack);
                }
            }
            this.heads = loaded;
        } catch (Exception e) {
            System.out.println("Error while loading heads.");
            e.printStackTrace();
        }
    }

    private JsonObject fetchJson(String url) throws IOException {
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestProperty("User-Agent", "Astra");
        int code = connection.getResponseCode();
        InputStream stream = code >= 200 && code < 300 ? connection.getInputStream() : connection.getErrorStream();
        if (stream == null) return null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            JsonElement element = new JsonParser().parse(reader);
            return element.isJsonObject() ? element.getAsJsonObject() : null;
        } finally {
            connection.disconnect();
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public void displayAllReleventItems(List itemList) {
        itemList.addAll(heads);
    }

    @Override
    public Item getTabIconItem() {
        return Items.skull;
    }

    @Override
    public String getTranslatedTabLabel() {
        return "Astra Heads";
    }
}

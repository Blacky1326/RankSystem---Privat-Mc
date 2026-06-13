package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.List;

public class ConfigManager {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private FileConfiguration config;

    public ConfigManager(RankSystem plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
    }

    // ── TAB ──────────────────────────────────────────────────────────────────

    public String getTabHeader(int playerCount) {
        List<String> lines = config.getStringList("tab.header");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(line.replace("%players%", String.valueOf(playerCount)));
        }
        return sb.toString();
    }

    public String getTabFooter() {
        List<String> lines = config.getStringList("tab.footer");
        StringBuilder sb = new StringBuilder();
        for (String line : lines) {
            if (sb.length() > 0) sb.append("\n");
            sb.append(line);
        }
        return sb.toString();
    }

    // ── LOGIN SCREEN ─────────────────────────────────────────────────────────

    public Component getLoginTitle(String playerName) {
        String title = config.getString("login.title", "<white>Willkommen!</white>");
        return mm.deserialize(title.replace("%name%", playerName));
    }

    public Component getLoginSubtitle(String playerName, String rankDisplay) {
        String sub = config.getString("login.subtitle", "<gray>Schön dass du da bist!</gray>");
        return mm.deserialize(sub
                .replace("%name%", playerName)
                .replace("%rank%", rankDisplay));
    }

    public List<String> getLoginChat() {
        return config.getStringList("login.chat");
    }

    public int getLoginDuration() {
        return config.getInt("login.duration", 5);
    }

    // ── MESSAGES ─────────────────────────────────────────────────────────────

    public Component getMessage(String key, String... replacements) {
        String msg = config.getString("messages." + key, "<red>Message not found: " + key + "</red>");
        for (int i = 0; i < replacements.length - 1; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }
        return mm.deserialize(msg);
    }

    // ── RANKS ────────────────────────────────────────────────────────────────

    public String getRankPrefix(String rankKey) {
        return config.getString("ranks." + rankKey + ".prefix", "<gray>[?]</gray>");
    }

    public String getRankDisplay(String rankKey) {
        return config.getString("ranks." + rankKey + ".display", rankKey);
    }

    public int getRankWeight(String rankKey) {
        return config.getInt("ranks." + rankKey + ".weight", 99);
    }
}

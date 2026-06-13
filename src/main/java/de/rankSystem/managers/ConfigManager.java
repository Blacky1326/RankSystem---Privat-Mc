package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.*;

public class ConfigManager {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private FileConfiguration config;

    // 🔥 CACHE
    private List<String> actionbarCache = new ArrayList<>();
    private List<String> tabHeaderCache = new ArrayList<>();
    private List<String> tabFooterCache = new ArrayList<>();

    public ConfigManager(RankSystem plugin) {
        this.plugin = plugin;
        reload();
    }

    // ── RELOAD ─────────────────────────────

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadCache();
    }

    private void loadCache() {
        actionbarCache = config.getStringList("actionbar.lines");
        tabHeaderCache = config.getStringList("tab.header");
        tabFooterCache = config.getStringList("tab.footer");
    }

    // ── TAB ────────────────────────────────

    public Component getTabHeader(int playerCount) {
        String text = String.join("\n", tabHeaderCache)
                .replace("%players%", String.valueOf(playerCount));

        return mm.deserialize(text);
    }

    public Component getTabFooter() {
        String text = String.join("\n", tabFooterCache);
        return mm.deserialize(text);
    }

    // ── ACTIONBAR ──────────────────────────

    public List<String> getActionBarLines() {
        return actionbarCache;
    }

    public int getActionBarInterval() {
        return config.getInt("actionbar.interval", 4);
    }

    // ── MESSAGES ───────────────────────────

    public Component getMessage(String key, String... replacements) {
        String msg = config.getString("messages." + key,
                "<red>Message not found: " + key + "</red>");

        for (int i = 0; i + 1 < replacements.length; i += 2) {
            msg = msg.replace(replacements[i], replacements[i + 1]);
        }

        return mm.deserialize(msg);
    }

    // ── RANK SYSTEM (CLEAN ENUM STYLE) ────

    public String getRankPrefix(Rank rank) {
        return config.getString("ranks." + rank.name().toLowerCase() + ".prefix", "<gray>[?]</gray>");
    }

    public String getRankDisplay(Rank rank) {
        return config.getString("ranks." + rank.name().toLowerCase() + ".display", rank.name());
    }

    public int getRankWeight(Rank rank) {
        return config.getInt("ranks." + rank.name().toLowerCase() + ".weight", 99);
    }

    // ── MOTD ───────────────────────────────

    public boolean isMotdEnabled() {
        return config.getBoolean("motd.enabled", true);
    }

    public String getMotdLine1() {
        return config.getString("motd.line1", "<gradient:#FF0000:#FFD700>✦ Server ✦</gradient>");
    }

    public String getMotdLine2() {
        return config.getString("motd.line2", "<gray>Willkommen!</gray>");
    }

    public int getMotdFakeMax() {
        return config.getInt("motd.fake-max", 0);
    }

    // ── DISCORD ────────────────────────────

    public String getDiscordUrl() {
        return config.getString("discord.url", "discord.gg/deinserver");
    }
}

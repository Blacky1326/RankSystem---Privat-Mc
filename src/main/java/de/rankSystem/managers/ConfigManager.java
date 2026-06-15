package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class ConfigManager {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private FileConfiguration config;

    // Aktuelle Config-Version des Plugins – bei jedem Update erhöhen
    private static final int CURRENT_CONFIG_VERSION = 2;

    public ConfigManager(RankSystem plugin) {
        this.plugin = plugin;
        reload();
    }

    public void reload() {
        plugin.saveDefaultConfig();
        plugin.reloadConfig();
        config = plugin.getConfig();
        migrateConfig();
    }

    // ── CONFIG MIGRATION ─────────────────────────────────────────────────────

    /**
     * Vergleicht die Server-Config mit der Standard-Config aus dem Plugin.
     * Fehlende Keys werden automatisch ergänzt, vorhandene bleiben unberührt.
     */
    private void migrateConfig() {
        int serverVersion = config.getInt("config-version", 0);

        if (serverVersion >= CURRENT_CONFIG_VERSION) return;

        plugin.getLogger().info("╔══════════════════════════════════════╗");
        plugin.getLogger().info("║     Config-Migration wird gestartet  ║");
        plugin.getLogger().info("╠══════════════════════════════════════╣");
        plugin.getLogger().info("║  Version " + serverVersion + " → " + CURRENT_CONFIG_VERSION + " wird aktualisiert...   ║");
        plugin.getLogger().info("╚══════════════════════════════════════╝");

        // Standard-Config aus dem Plugin laden
        InputStream defaultStream = plugin.getResource("config.yml");
        if (defaultStream == null) return;

        FileConfiguration defaultConfig = YamlConfiguration.loadConfiguration(
                new InputStreamReader(defaultStream, StandardCharsets.UTF_8)
        );

        // Alle fehlenden Keys rekursiv ergänzen
        int addedKeys = addMissingKeys(defaultConfig, config, "");

        // Version aktualisieren
        config.set("config-version", CURRENT_CONFIG_VERSION);

        // Speichern
        try {
            config.save(new File(plugin.getDataFolder(), "config.yml"));
        } catch (IOException e) {
            plugin.getLogger().warning("Fehler beim Speichern der migrierten Config: " + e.getMessage());
            return;
        }

        if (addedKeys > 0) {
            plugin.getLogger().info("[RankSystem] Migration abgeschlossen: " + addedKeys + " neue Einträge hinzugefügt.");
        } else {
            plugin.getLogger().info("[RankSystem] Migration abgeschlossen: Keine neuen Einträge nötig.");
        }
    }

    /**
     * Geht rekursiv durch alle Keys der Standard-Config und fügt fehlende
     * Einträge in die Server-Config ein. Gibt die Anzahl der hinzugefügten Keys zurück.
     */
    private int addMissingKeys(ConfigurationSection defaultSection, ConfigurationSection serverSection, String path) {
        int count = 0;
        Set<String> keys = defaultSection.getKeys(false);

        for (String key : keys) {
            String fullPath = path.isEmpty() ? key : path + "." + key;

            // config-version überspringen (wird separat gesetzt)
            if (fullPath.equals("config-version")) continue;

            if (defaultSection.isConfigurationSection(key)) {
                // Unterabschnitt → rekursiv weiter
                ConfigurationSection defaultSub = defaultSection.getConfigurationSection(key);
                ConfigurationSection serverSub = serverSection.getConfigurationSection(key);

                if (serverSub == null) {
                    // Ganzer Abschnitt fehlt → komplett übernehmen
                    serverSection.set(key, defaultSection.get(key));
                    plugin.getLogger().info("[RankSystem] Neuer Abschnitt hinzugefügt: " + fullPath);
                    count++;
                } else {
                    // Abschnitt existiert → nur fehlende Keys darin ergänzen
                    count += addMissingKeys(defaultSub, serverSub, fullPath);
                }
            } else {
                // Einzelner Key
                if (!serverSection.contains(key)) {
                    serverSection.set(key, defaultSection.get(key));
                    plugin.getLogger().info("[RankSystem] Neuer Eintrag hinzugefügt: " + fullPath + " = " + defaultSection.get(key));
                    count++;
                }
            }
        }
        return count;
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

    // ── DISCORD ──────────────────────────────────────────────────────────────

    public String getDiscordUrl() {
        return config.getString("discord.url", "discord.gg/deinserver");
    }

    // ── ACTIONBAR ────────────────────────────────────────────────────────────

    public List<String> getActionBarLines() {
        return config.getStringList("actionbar.lines");
    }

    public int getActionBarInterval() {
        return config.getInt("actionbar.interval", 4);
    }

    // ── MOTD ─────────────────────────────────────────────────────────────────

    public boolean isMotdEnabled() {
        return config.getBoolean("motd.enabled", true);
    }

    public String getMotdLine1() {
        return config.getString("motd.line1", "<gradient:#FF0000:#FFD700><bold>✦ Dein Server ✦</bold></gradient>");
    }

    public String getMotdLine2() {
        return config.getString("motd.line2", "<gray>Willkommen!</gray>");
    }

    public int getMotdFakeMax() {
        return config.getInt("motd.fake-max", 0);
    }
    public String getVanishFakeJoin() {
        return config.getString("vanish.fake-join",
                "<gray>%name% ist dem Server beigetreten.</gray>");
    }
    public String getVanishFakeQuit() {
        return config.getString("vanish.fake-quit",
                "<gray>%name% hat den Server verlassen.</gray>");
    }
    // ── TELEPORT ─────────────────────────────────────────────────────────────

    public int getTpCooldown()        { return config.getInt("teleport.cooldown", 60); }
    public int getTpCountdown()       { return config.getInt("teleport.countdown", 5); }
    public int getTpRequestTimeout()  { return config.getInt("teleport.request-timeout", 30); }
    public double getTpMoveTolerance(){ return config.getDouble("teleport.move-tolerance", 0.22); }


}

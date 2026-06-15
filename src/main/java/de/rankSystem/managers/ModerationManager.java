package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class ModerationManager {

    private final RankSystem plugin;
    private final File dataFile;
    private YamlConfiguration data;

    // UUID -> List of warn reasons
    private final Map<UUID, List<String>> warnings = new ConcurrentHashMap<>();
    // UUID -> unmute timestamp (ms)
    private final Map<UUID, Long> mutes = new ConcurrentHashMap<>();
    // UUID -> unban timestamp (ms)
    private final Map<UUID, Long> tempBans = new ConcurrentHashMap<>();
    // UUID -> tempban reason
    private final Map<UUID, String> tempBanReasons = new ConcurrentHashMap<>();
    // UUID -> ip at time of ban
    private final Map<UUID, String> tempBanIps = new ConcurrentHashMap<>();
    // IP -> unban timestamp (ms)  – for alt-account blocking
    private final Map<String, Long> ipBans = new ConcurrentHashMap<>();
    // IP -> reason
    private final Map<String, String> ipBanReasons = new ConcurrentHashMap<>();

    public ModerationManager(RankSystem plugin) {
        this.plugin = plugin;
        this.dataFile = new File(plugin.getDataFolder(), "moderation.yml");
        load();
        startExpiryTask();
    }

    // ──────────────────────────────────────────────
    //  WARN
    // ──────────────────────────────────────────────

    public int addWarning(UUID uuid, String reason) {
        warnings.computeIfAbsent(uuid, k -> new ArrayList<>()).add(reason);
        save();
        return warnings.get(uuid).size();
    }

    public List<String> getWarnings(UUID uuid) {
        return warnings.getOrDefault(uuid, Collections.emptyList());
    }

    public void clearWarnings(UUID uuid) {
        warnings.remove(uuid);
        save();
    }

    // ──────────────────────────────────────────────
    //  MUTE
    // ──────────────────────────────────────────────

    public void mute(UUID uuid, long durationMs) {
        mutes.put(uuid, System.currentTimeMillis() + durationMs);
        save();
    }

    public void unmute(UUID uuid) {
        mutes.remove(uuid);
        save();
    }

    public boolean isMuted(UUID uuid) {
        Long until = mutes.get(uuid);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            mutes.remove(uuid);
            save();
            return false;
        }
        return true;
    }

    public long getMuteRemaining(UUID uuid) {
        Long until = mutes.get(uuid);
        if (until == null) return 0;
        return Math.max(0, until - System.currentTimeMillis());
    }

    // ──────────────────────────────────────────────
    //  TEMP-BAN  (UUID + IP)
    // ──────────────────────────────────────────────

    /** Ban by UUID and optionally also ban their IP */
    public void tempBan(UUID uuid, String ip, long durationMs, String reason) {
        long until = System.currentTimeMillis() + durationMs;

        tempBans.put(uuid, until);
        tempBanReasons.put(uuid, reason);

        if (ip != null && !ip.isBlank()) {
            tempBanIps.put(uuid, ip);
            ipBans.put(ip, until);
            ipBanReasons.put(ip, reason);
        }
        save();
    }

    public void unban(UUID uuid) {
        // Remove UUID ban
        tempBans.remove(uuid);
        tempBanReasons.remove(uuid);

        // Remove associated IP ban
        String ip = tempBanIps.remove(uuid);
        if (ip != null) {
            ipBans.remove(ip);
            ipBanReasons.remove(ip);
        }
        save();
    }

    public boolean isBanned(UUID uuid) {
        Long until = tempBans.get(uuid);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            String ip = tempBanIps.remove(uuid);
            if (ip != null) { ipBans.remove(ip); ipBanReasons.remove(ip); }
            tempBans.remove(uuid);
            tempBanReasons.remove(uuid);
            save();
            return false;
        }
        return true;
    }

    public boolean isIpBanned(String ip) {
        if (ip == null) return false;
        Long until = ipBans.get(ip);
        if (until == null) return false;
        if (System.currentTimeMillis() >= until) {
            ipBans.remove(ip);
            ipBanReasons.remove(ip);
            save();
            return false;
        }
        return true;
    }

    public long getBanRemaining(UUID uuid) {
        Long until = tempBans.get(uuid);
        if (until == null) return 0;
        return Math.max(0, until - System.currentTimeMillis());
    }

    public long getIpBanRemaining(String ip) {
        Long until = ipBans.get(ip);
        if (until == null) return 0;
        return Math.max(0, until - System.currentTimeMillis());
    }

    public String getBanReason(UUID uuid) {
        return tempBanReasons.getOrDefault(uuid, "Kein Grund angegeben");
    }

    public String getIpBanReason(String ip) {
        return ipBanReasons.getOrDefault(ip, "Kein Grund angegeben");
    }

    // ──────────────────────────────────────────────
    //  HELPERS
    // ──────────────────────────────────────────────

    public static long parseDuration(String input) {
        long total = 0;
        StringBuilder num = new StringBuilder();
        for (char c : input.toLowerCase().toCharArray()) {
            if (Character.isDigit(c)) {
                num.append(c);
            } else {
                if (num.length() == 0) continue;
                long val = Long.parseLong(num.toString());
                num.setLength(0);
                total += switch (c) {
                    case 'd' -> val * 86400_000L;
                    case 'h' -> val * 3600_000L;
                    case 'm' -> val * 60_000L;
                    case 's' -> val * 1000L;
                    default  -> 0L;
                };
            }
        }
        return total;
    }

    public static String formatDuration(long ms) {
        if (ms <= 0) return "0s";
        long s = ms / 1000;
        long d = s / 86400; s %= 86400;
        long h = s / 3600;  s %= 3600;
        long m = s / 60;    s %= 60;

        StringBuilder sb = new StringBuilder();
        if (d > 0) sb.append(d).append("d ");
        if (h > 0) sb.append(h).append("h ");
        if (m > 0) sb.append(m).append("m ");
        if (s > 0 || sb.length() == 0) sb.append(s).append("s");
        return sb.toString().trim();
    }

    // ──────────────────────────────────────────────
    //  PERSISTENCE
    // ──────────────────────────────────────────────

    private void load() {
        if (!dataFile.exists()) {
            try {
                plugin.getDataFolder().mkdirs();
                dataFile.createNewFile();
            } catch (IOException e) {
                plugin.getLogger().warning("Konnte moderation.yml nicht erstellen: " + e.getMessage());
            }
        }
        data = YamlConfiguration.loadConfiguration(dataFile);

        // Warnings
        if (data.isConfigurationSection("warnings")) {
            for (String key : data.getConfigurationSection("warnings").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                warnings.put(uuid, new ArrayList<>(data.getStringList("warnings." + key)));
            }
        }

        // Mutes
        if (data.isConfigurationSection("mutes")) {
            for (String key : data.getConfigurationSection("mutes").getKeys(false)) {
                long until = data.getLong("mutes." + key);
                if (System.currentTimeMillis() < until)
                    mutes.put(UUID.fromString(key), until);
            }
        }

        // TempBans (UUID)
        if (data.isConfigurationSection("tempbans")) {
            for (String key : data.getConfigurationSection("tempbans").getKeys(false)) {
                UUID uuid = UUID.fromString(key);
                long until  = data.getLong("tempbans." + key + ".until");
                String reason = data.getString("tempbans." + key + ".reason", "Kein Grund");
                String ip     = data.getString("tempbans." + key + ".ip", "");
                if (System.currentTimeMillis() < until) {
                    tempBans.put(uuid, until);
                    tempBanReasons.put(uuid, reason);
                    if (!ip.isBlank()) tempBanIps.put(uuid, ip);
                }
            }
        }

        // IP Bans
        if (data.isConfigurationSection("ipbans")) {
            for (String ip : data.getConfigurationSection("ipbans").getKeys(false)) {
                String safeKey = ip.replace(".", "_");
                long until  = data.getLong("ipbans." + safeKey + ".until");
                String reason = data.getString("ipbans." + safeKey + ".reason", "Kein Grund");
                if (System.currentTimeMillis() < until) {
                    ipBans.put(ip, until);
                    ipBanReasons.put(ip, reason);
                }
            }
        }
    }

    private void save() {
        // Warnings
        data.set("warnings", null);
        for (Map.Entry<UUID, List<String>> e : warnings.entrySet())
            data.set("warnings." + e.getKey(), e.getValue());

        // Mutes
        data.set("mutes", null);
        for (Map.Entry<UUID, Long> e : mutes.entrySet())
            data.set("mutes." + e.getKey(), e.getValue());

        // TempBans
        data.set("tempbans", null);
        for (Map.Entry<UUID, Long> e : tempBans.entrySet()) {
            String key = "tempbans." + e.getKey();
            data.set(key + ".until",  e.getValue());
            data.set(key + ".reason", tempBanReasons.getOrDefault(e.getKey(), ""));
            String ip = tempBanIps.get(e.getKey());
            data.set(key + ".ip", ip != null ? ip : "");
        }

        // IP Bans  (dots in YAML keys → replace with _)
        data.set("ipbans", null);
        for (Map.Entry<String, Long> e : ipBans.entrySet()) {
            String safeKey = "ipbans." + e.getKey().replace(".", "_");
            data.set(safeKey + ".until",  e.getValue());
            data.set(safeKey + ".reason", ipBanReasons.getOrDefault(e.getKey(), ""));
        }

        try {
            data.save(dataFile);
        } catch (IOException ex) {
            plugin.getLogger().warning("Fehler beim Speichern von moderation.yml: " + ex.getMessage());
        }
    }

    private void startExpiryTask() {
        Bukkit.getScheduler().runTaskTimerAsynchronously(plugin, () -> {
            long now = System.currentTimeMillis();
            mutes.entrySet().removeIf(e -> now >= e.getValue());
            ipBans.entrySet().removeIf(e -> {
                if (now >= e.getValue()) { ipBanReasons.remove(e.getKey()); return true; }
                return false;
            });
            tempBans.entrySet().removeIf(e -> {
                if (now >= e.getValue()) {
                    tempBanReasons.remove(e.getKey());
                    String ip = tempBanIps.remove(e.getKey());
                    if (ip != null) { ipBans.remove(ip); ipBanReasons.remove(ip); }
                    return true;
                }
                return false;
            });
        }, 600L, 600L);
    }
}

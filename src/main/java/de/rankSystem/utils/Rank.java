package de.rankSystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;

public enum Rank {

    // Name, LuckPerms-Gruppe, Tab-Prefix (MiniMessage), Chat-Prefix, Sortier-Gewicht (niedrig = oben)
    OWNER(
            "Owner",
            "owner",
            "<gradient:#FF0000:#FF6B00><bold>OWNER</bold></gradient>",
            "<gradient:#FF0000:#FF6B00><bold>[OWNER]</bold></gradient>",
            1,
            "ranksystem.rank.owner"
    ),
    ADMIN(
            "Admin",
            "admin",
            "<gradient:#FF4500:#FFD700><bold>ADMIN</bold></gradient>",
            "<gradient:#FF4500:#FFD700><bold>[ADMIN]</bold></gradient>",
            2,
            "ranksystem.rank.admin"
    ),
    MODERATOR(
            "Moderator",
            "moderator",
            "<gradient:#00BFFF:#7B68EE><bold>MOD</bold></gradient>",
            "<gradient:#00BFFF:#7B68EE><bold>[MOD]</bold></gradient>",
            3,
            "ranksystem.rank.moderator"
    ),
    SUPPORTER(
            "Supporter",
            "supporter",
            "<gradient:#00FF7F:#00CED1><bold>SUP</bold></gradient>",
            "<gradient:#00FF7F:#00CED1><bold>[SUP]</bold></gradient>",
            4,
            "ranksystem.rank.supporter"
    ),
    STREAMER(
            "Streamer",
            "streamer",
            "<gradient:#9400D3:#FF1493><bold>STREAM</bold></gradient>",
            "<gradient:#9400D3:#FF1493><bold>[STREAM]</bold></gradient>",
            5,
            "ranksystem.rank.streamer"
    ),
    VIP(
            "VIP",
            "vip",
            "<gradient:#FFD700:#FFA500><bold>VIP</bold></gradient>",
            "<gradient:#FFD700:#FFA500><bold>[VIP]</bold></gradient>",
            6,
            "ranksystem.rank.vip"
    ),
    MITGLIED(
            "Mitglied",
            "default",
            "<gray>Mitglied</gray>",
            "<gray>[Mitglied]</gray>",
            7,
            "ranksystem.rank.mitglied"
    );

    private final String displayName;
    private final String luckPermsGroup;
    private final String tabPrefixMini;
    private final String chatPrefixMini;
    private final int weight;
    private final String permission;

    Rank(String displayName, String luckPermsGroup, String tabPrefixMini,
         String chatPrefixMini, int weight, String permission) {
        this.displayName = displayName;
        this.luckPermsGroup = luckPermsGroup;
        this.tabPrefixMini = tabPrefixMini;
        this.chatPrefixMini = chatPrefixMini;
        this.weight = weight;
        this.permission = permission;
    }

    public Component getTabPrefix() {
        return MiniMessage.miniMessage().deserialize(tabPrefixMini + " ");
    }

    public Component getChatPrefix() {
        return MiniMessage.miniMessage().deserialize(chatPrefixMini);
    }

    public Component getTabHeader() {
        // Für die Tab-Liste: Rank-Kategorie als Header
        return MiniMessage.miniMessage().deserialize(tabPrefixMini);
    }

    // Tab-Sortierung: zwei Buchstaben für Scoreboard-Team-Namen (z.B. "01", "02"...)
    public String getTeamSortPrefix() {
        return String.format("%02d", weight);
    }

    public String getDisplayName() { return displayName; }
    public String getChatPrefixMini() { return chatPrefixMini; }
    public String getLuckPermsGroup() { return luckPermsGroup; }
    public int getWeight() { return weight; }
    public String getPermission() { return permission; }
    public String getLuckPermsGroupName() { return luckPermsGroup; }

    public static Rank fromGroup(String groupName) {
        for (Rank rank : values()) {
            if (rank.luckPermsGroup.equalsIgnoreCase(groupName)) {
                return rank;
            }
        }
        return MITGLIED;
    }

    public static Rank fromPermission(org.bukkit.entity.Player player) {
        for (Rank rank : values()) {
            if (rank == MITGLIED) continue;
            if (player.hasPermission(rank.permission)) return rank;
        }
        return MITGLIED;
    }
}

package de.rankSystem.utils;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;

public enum Rank {

    OWNER("Owner", "owner",
            "<gradient:#FF0000:#FF6B00><bold>OWNER</bold></gradient>",
            "<gradient:#FF0000:#FF6B00><bold>[OWNER]</bold></gradient>",
            1,
            "ranksystem.rank.owner"),

    ADMIN("Admin", "admin",
            "<gradient:#FF4500:#FFD700><bold>ADMIN</bold></gradient>",
            "<gradient:#FF4500:#FFD700><bold>[ADMIN]</bold></gradient>",
            2,
            "ranksystem.rank.admin"),

    MODERATOR("Moderator", "moderator",
            "<gradient:#00BFFF:#7B68EE><bold>MOD</bold></gradient>",
            "<gradient:#00BFFF:#7B68EE><bold>[MOD]</bold></gradient>",
            3,
            "ranksystem.rank.moderator"),

    SUPPORTER("Supporter", "supporter",
            "<gradient:#00FF7F:#00CED1><bold>SUP</bold></gradient>",
            "<gradient:#00FF7F:#00CED1><bold>[SUP]</bold></gradient>",
            4,
            "ranksystem.rank.supporter"),

    STREAMER("Streamer", "streamer",
            "<gradient:#9400D3:#FF1493><bold>STREAM</bold></gradient>",
            "<gradient:#9400D3:#FF1493><bold>[STREAM]</bold></gradient>",
            5,
            "ranksystem.rank.streamer"),

    VIP("VIP", "vip",
            "<gradient:#FFD700:#FFA500><bold>VIP</bold></gradient>",
            "<gradient:#FFD700:#FFA500><bold>[VIP]</bold></gradient>",
            6,
            "ranksystem.rank.vip"),

    MITGLIED("Mitglied", "default",
            "<gray>Mitglied</gray>",
            "<gray>[Mitglied]</gray>",
            7,
            "ranksystem.rank.mitglied");

    private static final MiniMessage MM = MiniMessage.miniMessage();

    private final String displayName;
    private final String luckPermsGroup;
    private final Component tabPrefix;
    private final Component chatPrefix;
    private final int weight;
    private final String permission;

    Rank(String displayName, String luckPermsGroup,
         String tabPrefixMini, String chatPrefixMini,
         int weight, String permission) {

        this.displayName = displayName;
        this.luckPermsGroup = luckPermsGroup;
        this.weight = weight;
        this.permission = permission;

        this.tabPrefix = MM.deserialize(tabPrefixMini + " ");
        this.chatPrefix = MM.deserialize(chatPrefixMini);
    }

    public Component getTabPrefix() {
        return tabPrefix;
    }

    public Component getChatPrefix() {
        return chatPrefix;
    }

    public String getTeamSortPrefix() {
        return String.format("%03d", weight);
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getLuckPermsGroup() {
        return luckPermsGroup;
    }

    public int getWeight() {
        return weight;
    }

    public String getPermission() {
        return permission;
    }

    public static Rank fromGroup(String groupName) {
        for (Rank rank : values()) {
            if (rank.luckPermsGroup.equalsIgnoreCase(groupName)) {
                return rank;
            }
        }
        return MITGLIED;
    }
}

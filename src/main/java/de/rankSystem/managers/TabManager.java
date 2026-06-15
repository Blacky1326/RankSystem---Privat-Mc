package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.HashMap;
import java.util.Map;

public class TabManager {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Map<String, String> playerTeams = new HashMap<>();

    public TabManager(RankSystem plugin) {
        this.plugin = plugin;
    }

    public void updatePlayer(Player player) {
        Rank rank = plugin.getRankManager().getPlayerRank(player);
        boolean vanished = plugin.getVanishManager() != null && plugin.getVanishManager().isVanished(player);

        setupScoreboardTeam(player, rank);
        updateTabHeaderFooter();

        Component tabName = buildTabName(player, rank, vanished);
        player.playerListName(tabName);
    }

    public void updateAllPlayers() {
        Bukkit.getOnlinePlayers().forEach(this::updatePlayer);
    }

    private Component buildTabName(Player player, Rank rank, boolean vanished) {
        String ping = getPingFormatted(player);

        String nameTag = switch (rank) {
            case OWNER     -> "<gradient:#FF6B6B:#FFD93D><bold>" + player.getName() + "</bold></gradient>";
            case ADMIN     -> "<gradient:#FF8C00:#FFD700><bold>" + player.getName() + "</bold></gradient>";
            case MODERATOR -> "<gradient:#00BFFF:#7B68EE>" + player.getName() + "</gradient>";
            case SUPPORTER -> "<gradient:#00FF7F:#00CED1>" + player.getName() + "</gradient>";
            case STREAMER  -> "<gradient:#DA70D6:#FF1493>" + player.getName() + "</gradient>";
            case VIP       -> "<gradient:#FFD700:#FFA500>" + player.getName() + "</gradient>";
            case MITGLIED  -> "<gray>" + player.getName() + "</gray>";
        };

        // 👻 Prefix für Staff die vanished Spieler sehen
        String vanishPrefix = vanished ? "<gradient:#9400D3:#FF1493>👻 </gradient>" : "";

        return mm.deserialize(vanishPrefix)
                .append(rank.getTabPrefix())
                .append(mm.deserialize(nameTag))
                .append(mm.deserialize(" <dark_gray>| " + ping + "</dark_gray>"));
    }

    private String getPingFormatted(Player player) {
        int ping = player.getPing();
        String color;
        if      (ping < 50)  color = "#00FF7F";
        else if (ping < 100) color = "#7FFF00";
        else if (ping < 150) color = "#FFD700";
        else if (ping < 200) color = "#FFA500";
        else                 color = "#FF4444";
        return "<color:" + color + ">" + ping + "ms</color>";
    }

    private void setupScoreboardTeam(Player player, Rank rank) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        String teamName = rank.getTeamSortPrefix() + player.getName();
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        String oldTeamName = playerTeams.get(player.getName());
        if (oldTeamName != null) {
            Team oldTeam = scoreboard.getTeam(oldTeamName);
            if (oldTeam != null) oldTeam.unregister();
        }

        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.registerNewTeam(teamName);
        team.prefix(rank.getTabPrefix());
        team.addPlayer(player);
        playerTeams.put(player.getName(), teamName);
    }

    public void updateTabHeaderFooter() {
        String header = plugin.getConfigManager().getTabHeader(Bukkit.getOnlinePlayers().size());
        String footer = plugin.getConfigManager().getTabFooter();

        Component headerComp = mm.deserialize(header);
        Component footerComp = mm.deserialize(footer);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendPlayerListHeaderAndFooter(headerComp, footerComp);
        }
    }

    public void removePlayer(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = playerTeams.remove(player.getName());
        if (teamName != null) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) team.unregister();
        }
    }
}

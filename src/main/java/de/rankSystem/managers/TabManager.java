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

    // Tab-Header & Footer
    private static final String TAB_HEADER =
            "\n<gradient:#FF0000:#FFD700><bold>✦ Dein Server ✦</bold></gradient>\n" +
            "<gray>Spieler online: <white>%players%</white></gray>\n";

    private static final String TAB_FOOTER =
            "\n<gradient:#00BFFF:#9400D3>discord.gg/deinserver</gradient>\n";

    // Team-Namen für Scoreboard-Sortierung (max 16 Zeichen!)
    private final Map<String, String> playerTeams = new HashMap<>();

    public TabManager(RankSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Aktualisiert Tab-Liste für einen Spieler (sein eigenes Aussehen für alle)
     */
    public void updatePlayer(Player player) {
        Rank rank = plugin.getRankManager().getPlayerRank(player);

        // Scoreboard-Team für Sortierung & Prefix
        setupScoreboardTeam(player, rank);

        // Tab Header/Footer für alle aktualisieren
        updateTabHeaderFooter();

        // Player-Name in Tab-Liste setzen
        Component tabName = buildTabName(player, rank);
        player.playerListName(tabName);
    }

    /**
     * Baut den Tab-Listen-Namen mit Prefix + Spielername
     */
    private Component buildTabName(Player player, Rank rank) {
        Component prefix = rank.getTabPrefix();
        Component name = getNameColor(player, rank);
        return prefix.append(name);
    }

    /**
     * Spielername-Farbe je nach Rang
     */
    private Component getNameColor(Player player, Rank rank) {
        String nameTag = switch (rank) {
            case OWNER -> "<gradient:#FF6B6B:#FFD93D><bold>" + player.getName() + "</bold></gradient>";
            case ADMIN -> "<gradient:#FF8C00:#FFD700><bold>" + player.getName() + "</bold></gradient>";
            case MODERATOR -> "<gradient:#00BFFF:#7B68EE>" + player.getName() + "</gradient>";
            case SUPPORTER -> "<gradient:#00FF7F:#00CED1>" + player.getName() + "</gradient>";
            case STREAMER -> "<gradient:#DA70D6:#FF1493>" + player.getName() + "</gradient>";
            case VIP -> "<gradient:#FFD700:#FFA500>" + player.getName() + "</gradient>";
            case MITGLIED -> "<gray>" + player.getName() + "</gray>";
        };
        return mm.deserialize(nameTag);
    }

    /**
     * Scoreboard-Team für Sortierung erstellen
     */
    private void setupScoreboardTeam(Player player, Rank rank) {
        // Jeder Spieler bekommt sein eigenes Scoreboard
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        // Team-Name: Sortiergewicht + Spielername (max 16 Zeichen)
        String teamName = rank.getTeamSortPrefix() + player.getName();
        if (teamName.length() > 16) teamName = teamName.substring(0, 16);

        // Altes Team entfernen falls vorhanden
        String oldTeamName = playerTeams.get(player.getName());
        if (oldTeamName != null) {
            Team oldTeam = scoreboard.getTeam(oldTeamName);
            if (oldTeam != null) oldTeam.unregister();
        }

        // Neues Team erstellen
        Team team = scoreboard.getTeam(teamName);
        if (team == null) team = scoreboard.registerNewTeam(teamName);

        // Prefix für Nametag über dem Kopf (im Spiel)
        team.prefix(rank.getTabPrefix());

        // Spieler hinzufügen
        team.addPlayer(player);
        playerTeams.put(player.getName(), teamName);
    }

    /**
     * Tab Header & Footer für alle Spieler aktualisieren
     */
    public void updateTabHeaderFooter() {
        String headerStr = TAB_HEADER.replace("%players%",
                String.valueOf(Bukkit.getOnlinePlayers().size()));

        Component header = mm.deserialize(headerStr);
        Component footer = mm.deserialize(TAB_FOOTER);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendPlayerListHeaderAndFooter(header, footer);
        }
    }

    /**
     * Entfernt einen Spieler aus seinem Team (beim Verlassen)
     */
    public void removePlayer(Player player) {
        Scoreboard scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();
        String teamName = playerTeams.remove(player.getName());
        if (teamName != null) {
            Team team = scoreboard.getTeam(teamName);
            if (team != null) team.unregister();
        }
    }
}

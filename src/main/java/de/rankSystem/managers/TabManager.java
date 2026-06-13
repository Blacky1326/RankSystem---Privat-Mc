package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.EnumMap;
import java.util.Map;

public class TabManager {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private Scoreboard scoreboard;
    private final Map<Rank, Team> rankTeams = new EnumMap<>(Rank.class);

    public TabManager(RankSystem plugin) {
        this.plugin = plugin;
        this.scoreboard = Bukkit.getScoreboardManager().getMainScoreboard();

        setupTeams();
    }

    // 🔥 1. Teams einmal erstellen (WICHTIG!)
    private void setupTeams() {
        for (Rank rank : Rank.values()) {

            String teamName = rank.name(); // stabiler Name

            Team team = scoreboard.getTeam(teamName);
            if (team == null) {
                team = scoreboard.registerNewTeam(teamName);
            }

            team.setPrefix(toLegacy(rank.getTabPrefix()));
            team.setOption(Team.Option.NAME_TAG_VISIBILITY, Team.OptionStatus.ALWAYS);

            rankTeams.put(rank, team);
        }
    }

    // 🔥 2. Player Update (nur verschieben, NICHT neu erstellen)
    public void updatePlayer(Player player) {

        Rank rank = plugin.getRankManager().getPlayerRank(player);

        // Spieler aus allen Teams entfernen
        for (Team team : rankTeams.values()) {
            team.removeEntry(player.getName());
        }

        // Neues Team zuweisen
        Team targetTeam = rankTeams.get(rank);
        if (targetTeam != null) {
            targetTeam.addEntry(player.getName());
        }

        // Tab Name setzen
        Component tabName = buildTabName(player, rank);
        player.playerListName(tabName);

        updateTabHeaderFooter();
    }

    // 🔥 3. Tab Name
    private Component buildTabName(Player player, Rank rank) {
        return rank.getTabPrefix().append(
                mm.deserialize("<white> " + player.getName())
        );
    }

    // 🔥 4. Header/Footer
    public void updateTabHeaderFooter() {
        String header = plugin.getConfigManager().getTabHeader(Bukkit.getOnlinePlayers().size());
        String footer = plugin.getConfigManager().getTabFooter();

        Component headerComp = mm.deserialize(header);
        Component footerComp = mm.deserialize(footer);

        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendPlayerListHeaderAndFooter(headerComp, footerComp);
        }
    }

    // 🔥 5. Cleanup
    public void removePlayer(Player player) {
        for (Team team : rankTeams.values()) {
            team.removeEntry(player.getName());
        }
    }

    // 🔥 MiniMessage → String (Scoreboard braucht legacy text)
    private String toLegacy(Component component) {
        return mm.serialize(component);
    }
}

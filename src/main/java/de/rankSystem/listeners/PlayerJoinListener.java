package de.rankSystem.listeners;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PlayerJoinListener(RankSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Tab mit kleiner Verzögerung aktualisieren (LuckPerms braucht kurz zum Laden)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getTabManager().updatePlayer(event.getPlayer());

            // Alle anderen Spieler auch aktualisieren (Header Player-Count)
            plugin.getTabManager().updateTabHeaderFooter();
        }, 10L); // 10 Ticks = 0.5 Sekunden

        // Custom Join-Message
        Rank rank = plugin.getRankManager().getPlayerRank(event.getPlayer());
        event.joinMessage(
                rank.getChatPrefix()
                        .append(mm.deserialize(" <gray>" + event.getPlayer().getName() + " ist dem Server beigetreten.</gray>"))
        );
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Rank rank = plugin.getRankManager().getPlayerRank(event.getPlayer());

        // Spieler aus Tab-Team entfernen
        plugin.getTabManager().removePlayer(event.getPlayer());

        // Header aktualisieren
        plugin.getTabManager().updateTabHeaderFooter();

        // Custom Quit-Message
        event.quitMessage(
                rank.getChatPrefix()
                        .append(mm.deserialize(" <gray>" + event.getPlayer().getName() + " hat den Server verlassen.</gray>"))
        );
    }
}

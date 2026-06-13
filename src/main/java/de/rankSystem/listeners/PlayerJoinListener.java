package de.rankSystem.listeners;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.time.Duration;
import java.util.List;

public class PlayerJoinListener implements Listener {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public PlayerJoinListener(RankSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Tab mit kleiner Verzögerung aktualisieren
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getTabManager().updatePlayer(player);
            plugin.getTabManager().updateTabHeaderFooter();
        }, 10L);

        // Login-Screen nach 20 Ticks (1 Sekunde) anzeigen
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            showLoginScreen(player);
        }, 20L);

        // Custom Join-Message
        Rank rank = plugin.getRankManager().getPlayerRank(player);
        String joinMsg = plugin.getConfigManager().getMessage("join",
                "%name%", player.getName(),
                "%prefix%", "").toString();

        event.joinMessage(
            rank.getChatPrefix()
                .append(mm.deserialize(" <gray>" + player.getName() + " ist dem Server beigetreten.</gray>"))
        );
    }

    private void showLoginScreen(Player player) {
        Rank rank = plugin.getRankManager().getPlayerRank(player);
        String rankDisplay = rank.getDisplayName();
        int duration = plugin.getConfigManager().getLoginDuration();

        // ── TITEL ANIMATION ──────────────────────────────────────────────────
        // FadeIn: 1s, Sichtbar: duration s, FadeOut: 1s
        Title.Times times = Title.Times.times(
                Duration.ofMillis(500),           // fade in
                Duration.ofSeconds(duration),      // stay
                Duration.ofMillis(800)             // fade out
        );

        Component title    = plugin.getConfigManager().getLoginTitle(player.getName());
        Component subtitle = plugin.getConfigManager().getLoginSubtitle(player.getName(), rankDisplay);

        player.showTitle(Title.title(title, subtitle, times));

        // Sound beim Login
        player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        // ── CHAT NACHRICHTEN ─────────────────────────────────────────────────
        List<String> chatLines = plugin.getConfigManager().getLoginChat();

        // Animiert: Zeile für Zeile mit kleiner Verzögerung einblenden
        for (int i = 0; i < chatLines.size(); i++) {
            final String line = chatLines.get(i)
                    .replace("%name%", player.getName())
                    .replace("%rank%", rankDisplay);
            final long delay = i * 3L; // 3 Ticks (0.15s) zwischen jeder Zeile

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    player.sendMessage(mm.deserialize(line));
                }
            }, 20L + delay); // Start nach 1s, dann animiert
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Rank rank = plugin.getRankManager().getPlayerRank(player);

        plugin.getTabManager().removePlayer(player);
        plugin.getTabManager().updateTabHeaderFooter();

        event.quitMessage(
            rank.getChatPrefix()
                .append(mm.deserialize(" <gray>" + player.getName() + " hat den Server verlassen.</gray>"))
        );
    }
}

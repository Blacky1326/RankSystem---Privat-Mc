package de.rankSystem.listeners;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
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

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getTabManager().updatePlayer(player);
            plugin.getTabManager().updateTabHeaderFooter();
        }, 10L);

        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            showLoginScreen(player);
        }, 20L);

        Rank rank = plugin.getRankManager().getPlayerRank(player);
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
        Title.Times times = Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(duration),
                Duration.ofMillis(800)
        );

        Component title    = plugin.getConfigManager().getLoginTitle(player.getName());
        Component subtitle = plugin.getConfigManager().getLoginSubtitle(player.getName(), rankDisplay);
        player.showTitle(Title.title(title, subtitle, times));
        player.playSound(player.getLocation(),
                org.bukkit.Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        // ── CHAT NACHRICHTEN animiert ─────────────────────────────────────────
        List<String> chatLines = plugin.getConfigManager().getLoginChat();
        String discordUrl = plugin.getConfigManager().getDiscordUrl();

        for (int i = 0; i < chatLines.size(); i++) {
            final String rawLine = chatLines.get(i)
                    .replace("%name%", player.getName())
                    .replace("%rank%", rankDisplay);
            final long delay = i * 3L;

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (!player.isOnline()) return;

                // Prüfe ob die Zeile den Discord-Link enthält
                if (rawLine.contains("%discord%") && discordUrl != null && !discordUrl.isEmpty()) {
                    // Klickbarer Discord Link
                    Component discordComponent = buildDiscordLink(rawLine, discordUrl);
                    player.sendMessage(discordComponent);
                } else {
                    player.sendMessage(mm.deserialize(rawLine));
                }
            }, 20L + delay);
        }
    }

    private Component buildDiscordLink(String rawLine, String discordUrl) {
        // Splitte die Zeile am %discord% Platzhalter
        String[] parts = rawLine.split("%discord%", 2);

        Component before = parts.length > 0 && !parts[0].isEmpty()
                ? mm.deserialize(parts[0]) : Component.empty();
        Component after  = parts.length > 1 && !parts[1].isEmpty()
                ? mm.deserialize(parts[1]) : Component.empty();

        // Klickbarer Discord-Link mit Hover
        Component discordLink = mm.deserialize("<gradient:#5865F2:#7289DA><bold>discord.gg/" + extractInvite(discordUrl) + "</bold></gradient>")
                .clickEvent(ClickEvent.openUrl("https://" + discordUrl))
                .hoverEvent(HoverEvent.showText(
                        mm.deserialize("<gray>Klicke um dem Discord beizutreten!\n<blue>» " + discordUrl + "</blue></gray>")
                ));

        return before.append(discordLink).append(after);
    }

    private String extractInvite(String url) {
        // discord.gg/xyz → xyz, oder komplette URL
        if (url.startsWith("discord.gg/")) return url.substring("discord.gg/".length());
        if (url.contains("discord.gg/")) return url.substring(url.indexOf("discord.gg/") + "discord.gg/".length());
        return url;
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

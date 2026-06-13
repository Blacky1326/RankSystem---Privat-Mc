package de.rankSystem.listeners;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.title.Title;
import org.bukkit.Sound;
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

        // ❌ Vanilla join message aus
        event.joinMessage(null);

        // TAB UPDATE leicht verzögert
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getTabManager().updatePlayer(player);
            plugin.getTabManager().updateTabHeaderFooter();
        }, 10L);

        // LOGIN SCREEN
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline()) {
                showLoginScreen(player);
            }
        }, 20L);

        // JOIN MESSAGE (nach kurzer Verzögerung für sichere Rank load)
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            Rank rank = plugin.getRankManager().getPlayerRank(player);

            Component msg = rank.getChatPrefix()
                    .append(mm.deserialize(" <gray>" + player.getName() + " ist gejoint.</gray>"));

            player.getServer().sendMessage(msg);
        }, 5L);
    }

    private void showLoginScreen(Player player) {

        Rank rank = plugin.getRankManager().getPlayerRank(player);
        String rankDisplay = rank.getDisplayName();
        int duration = plugin.getConfigManager().getLoginDuration();

        Title.Times times = Title.Times.times(
                Duration.ofMillis(500),
                Duration.ofSeconds(duration),
                Duration.ofMillis(800)
        );

        player.showTitle(Title.title(
                plugin.getConfigManager().getLoginTitle(player.getName()),
                plugin.getConfigManager().getLoginSubtitle(player.getName(), rankDisplay),
                times
        ));

        player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 0.7f, 1.2f);

        sendLoginChat(player, rankDisplay);
    }

    private void sendLoginChat(Player player, String rankDisplay) {

        List<String> lines = plugin.getConfigManager().getLoginChat();
        String discord = plugin.getConfigManager().getDiscordUrl();

        for (int i = 0; i < lines.size(); i++) {

            String line = lines.get(i)
                    .replace("%name%", player.getName())
                    .replace("%rank%", rankDisplay);

            long delay = i * 3L;

            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {

                if (!player.isOnline()) return;

                if (line.contains("%discord%")) {
                    player.sendMessage(buildDiscord(line, discord));
                } else {
                    player.sendMessage(mm.deserialize(line));
                }

            }, 20L + delay);
        }
    }

    private Component buildDiscord(String line, String url) {

        String[] split = line.split("%discord%", 2);

        Component before = mm.deserialize(split.length > 0 ? split[0] : "");
        Component after = mm.deserialize(split.length > 1 ? split[1] : "");

        String invite = url.replace("https://", "").replace("http://", "");

        Component link = mm.deserialize(
                        "<gradient:#5865F2:#7289DA><bold>discord</bold></gradient>")
                .clickEvent(ClickEvent.openUrl("https://" + invite))
                .hoverEvent(HoverEvent.showText(
                        mm.deserialize("<gray>Klick um beizutreten:\n<blue>" + url + "</blue></gray>")
                ));

        return before.append(link).append(after);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {

        Player player = event.getPlayer();

        event.quitMessage(null);

        plugin.getTabManager().removePlayer(player);
        plugin.getTabManager().updateTabHeaderFooter();

        Rank rank = plugin.getRankManager().getPlayerRank(player);

        Component msg = rank.getChatPrefix()
                .append(mm.deserialize(" <gray>" + player.getName() + " hat verlassen.</gray>"));

        player.getServer().sendMessage(msg);
    }
}

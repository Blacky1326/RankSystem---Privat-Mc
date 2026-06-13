package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.List;

public class ActionBarManager {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private BukkitTask task;
    private int currentIndex = 0;

    public ActionBarManager(RankSystem plugin) {
        this.plugin = plugin;
    }

    public void start() {
        stop();
        int intervalTicks = plugin.getConfigManager().getActionBarInterval() * 20;

        task = Bukkit.getScheduler().runTaskTimer(plugin, () -> {
            List<String> lines = plugin.getConfigManager().getActionBarLines();
            if (lines.isEmpty()) return;

            // Rotiere durch die Zeilen
            String line = lines.get(currentIndex % lines.size());
            currentIndex++;

            for (Player player : Bukkit.getOnlinePlayers()) {
                Rank rank = plugin.getRankManager().getPlayerRank(player);
                String formatted = line
                        .replace("%rank%",    rank.getDisplayName())
                        .replace("%prefix%",  getRankPrefixPlain(rank))
                        .replace("%name%",    player.getName())
                        .replace("%online%",  String.valueOf(Bukkit.getOnlinePlayers().size()))
                        .replace("%ping%",    String.valueOf(player.getPing()));

                Component actionBar = mm.deserialize(formatted);
                player.sendActionBar(actionBar);
            }
        }, 20L, intervalTicks);
    }

    public void stop() {
        if (task != null) {
            task.cancel();
            task = null;
        }
    }

    public void restart() {
        stop();
        start();
    }

    private String getRankPrefixPlain(Rank rank) {
        return switch (rank) {
            case OWNER     -> "OWNER";
            case ADMIN     -> "ADMIN";
            case MODERATOR -> "MOD";
            case SUPPORTER -> "SUP";
            case STREAMER  -> "STREAM";
            case VIP       -> "VIP";
            case MITGLIED  -> "Mitglied";
        };
    }
}

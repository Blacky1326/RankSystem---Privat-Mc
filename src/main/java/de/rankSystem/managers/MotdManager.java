package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import com.destroystokyo.paper.event.server.PaperServerListPingEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class MotdManager implements Listener {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MotdManager(RankSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onServerPing(PaperServerListPingEvent event) {
        if (!plugin.getConfigManager().isMotdEnabled()) return;

        String line1 = plugin.getConfigManager().getMotdLine1()
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max%",    String.valueOf(Bukkit.getMaxPlayers()));

        String line2 = plugin.getConfigManager().getMotdLine2()
                .replace("%online%", String.valueOf(Bukkit.getOnlinePlayers().size()))
                .replace("%max%",    String.valueOf(Bukkit.getMaxPlayers()));

        Component motd = mm.deserialize(line1 + "\n" + line2);
        event.motd(motd);

        // Custom Max-Player Anzeige
        int fakeMax = plugin.getConfigManager().getMotdFakeMax();
        if (fakeMax > 0) event.setMaxPlayers(fakeMax);
    }
}

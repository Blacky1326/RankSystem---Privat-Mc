package de.rankSystem.listeners;

import de.rankSystem.RankSystem;
import de.rankSystem.commands.StaffChatCommand;
import de.rankSystem.managers.ModerationManager;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;

import java.net.InetSocketAddress;

public class ModerationListener implements Listener {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ModerationListener(RankSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onLogin(PlayerLoginEvent event) {
        ModerationManager mod = plugin.getModerationManager();
        Player player = event.getPlayer();

        // Resolve IP
        String ip = null;
        InetSocketAddress addr = event.getAddress() != null
                ? new InetSocketAddress(event.getAddress(), 0) : null;
        if (event.getAddress() != null) ip = event.getAddress().getHostAddress();

        // 1) UUID ban check
        if (mod.isBanned(player.getUniqueId())) {
            long remaining = mod.getBanRemaining(player.getUniqueId());
            String reason  = mod.getBanReason(player.getUniqueId());
            event.disallow(Result.KICK_BANNED, mm.deserialize(buildBanMessage(reason, remaining)));
            return;
        }

        // 2) IP ban check (catches alt accounts)
        if (ip != null && mod.isIpBanned(ip)) {
            long remaining = mod.getIpBanRemaining(ip);
            String reason  = mod.getIpBanReason(ip);
            event.disallow(Result.KICK_BANNED, mm.deserialize(
                    buildBanMessage(reason, remaining) +
                    "\n<dark_gray>(IP-Ban – Alt-Accounts sind gesperrt)</dark_gray>"
            ));
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onChat(AsyncChatEvent event) {
        Player player = event.getPlayer();
        ModerationManager mod = plugin.getModerationManager();
        StaffChatCommand sc   = plugin.getStaffChatCommand();

        // Staff chat toggle
        if (sc != null && sc.isToggled(player.getUniqueId())) {
            event.setCancelled(true);
            String text = net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
                    .plainText().serialize(event.message());
            plugin.getServer().getScheduler().runTask(plugin, () ->
                    sc.sendStaffMessage(player.getName(), "", text)
            );
            return;
        }

        // Mute check
        if (mod.isMuted(player.getUniqueId())) {
            event.setCancelled(true);
            long remaining = mod.getMuteRemaining(player.getUniqueId());
            player.sendMessage(mm.deserialize(
                    "<red>🔇 Du bist gemutet! " +
                    "<gray>Noch <white>" + ModerationManager.formatDuration(remaining) + "</white> verbleibend.</gray></red>"
            ));
        }
    }

    private String buildBanMessage(String reason, long remaining) {
        return "<dark_red><bold>Du bist temporär gebannt!</bold></dark_red>\n" +
               "<gray>Grund: <white>" + reason + "</white></gray>\n" +
               "<gray>Verbleibend: <white>" + ModerationManager.formatDuration(remaining) + "</white></gray>";
    }
}

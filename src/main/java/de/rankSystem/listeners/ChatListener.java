package de.rankSystem.listeners;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;

public class ChatListener implements Listener {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public ChatListener(RankSystem plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onChat(AsyncChatEvent event) {

        Player player = event.getPlayer();
        Rank rank = plugin.getRankManager().getPlayerRank(player);

        Component message = event.message();

        Component formatted = Component.empty()
                .append(rank.getChatPrefix())
                .append(Component.text(" "))
                .append(getNameComponent(player, rank))
                .append(mm.deserialize("<dark_gray>: </dark_gray>"))
                .append(getMessageColor(rank))
                .append(message);

        event.renderer((source, sourceDisplayName, msg, viewer) -> formatted);
    }

    private Component getNameComponent(Player player, Rank rank) {

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

    private Component getMessageColor(Rank rank) {

        String color = switch (rank) {
            case OWNER, ADMIN -> "<white>";
            case MODERATOR, SUPPORTER -> "<gray>";
            case STREAMER -> "<#E8C4FF>";
            case VIP -> "<#FFF5CC>";
            case MITGLIED -> "<gray>";
        };

        return mm.deserialize(color);
    }
}

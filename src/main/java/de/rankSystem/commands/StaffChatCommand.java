package de.rankSystem.commands;

import de.rankSystem.RankSystem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class StaffChatCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    // Players with staff chat toggled on (every message goes to staff chat)
    private final Set<UUID> toggledOn = new HashSet<>();

    public StaffChatCommand(RankSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("ranksystem.mod")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du hast keine Berechtigung!</red>"));
            return true;
        }

        // /sc toggle  →  toggle staff chat mode
        if (args.length == 1 && args[0].equalsIgnoreCase("toggle")) {
            if (!(sender instanceof Player p)) {
                sender.sendMessage(mm.deserialize("<red>Nur Spieler können den Toggle nutzen.</red>"));
                return true;
            }
            if (toggledOn.remove(p.getUniqueId())) {
                p.sendMessage(mm.deserialize("<gray>[Staff-Chat] <red>Deaktiviert.</red> Nachrichten gehen wieder in den normalen Chat.</gray>"));
            } else {
                toggledOn.add(p.getUniqueId());
                p.sendMessage(mm.deserialize("<gray>[Staff-Chat] <green>Aktiviert.</green> Alle deine Nachrichten gehen nun in den Staff-Chat.</gray>"));
            }
            return true;
        }

        // /sc <message>
        if (args.length < 1) {
            sender.sendMessage(mm.deserialize(
                    "<gray>Nutzung: <white>/sc <Nachricht></white> – Einmalige Staff-Nachricht\n" +
                    "         <white>/sc toggle</white> – Staff-Chat dauerhaft ein/ausschalten</gray>"
            ));
            return true;
        }

        String message = String.join(" ", args);
        String senderName = sender instanceof Player ? sender.getName() : "Konsole";
        String rankPrefix = "";
        if (sender instanceof Player p) {
            rankPrefix = plugin.getRankManager().getPlayerRank(p).getChatPrefix()
                    .insertion() != null ? "" : "";
            // Use MiniMessage serialized rank prefix
            rankPrefix = plugin.getRankManager().getPlayerRank(p).getChatPrefixMini();
        }

        sendStaffMessage(senderName, rankPrefix, message);
        return true;
    }

    /** Called from ChatListener when a player has staff chat toggled on */
    public boolean isToggled(UUID uuid) {
        return toggledOn.contains(uuid);
    }

    public void sendStaffMessage(String senderName, String rankPrefix, String message) {
        // Format: [STAFF] [RANK] Name: message
        String formatted =
                "<dark_gray>[</dark_gray><gradient:#00BFFF:#7B68EE><bold>STAFF</bold></gradient><dark_gray>]</dark_gray> " +
                "<dark_gray>[</dark_gray><white>" + senderName + "</white><dark_gray>]</dark_gray>" +
                "<dark_gray>: </dark_gray><aqua>" + message + "</aqua>";

        // Send to all players with ranksystem.mod permission
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("ranksystem.mod"))
                .forEach(p -> p.sendMessage(mm.deserialize(formatted)));

        // Also log to console
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(formatted));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("ranksystem.mod")) return new ArrayList<>();
        if (args.length == 1) return List.of("toggle");
        return new ArrayList<>();
    }
}

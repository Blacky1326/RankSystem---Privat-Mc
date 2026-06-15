package de.rankSystem.commands;

import de.rankSystem.RankSystem;
import de.rankSystem.managers.ModerationManager;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class UnbanCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public UnbanCommand(RankSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ModerationManager mod = plugin.getModerationManager();

        if (!sender.hasPermission("ranksystem.admin")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du hast keine Berechtigung!</red>"));
            return true;
        }

        // /unban <player>
        if (args.length < 1) {
            sender.sendMessage(mm.deserialize("<red>Nutzung: /unban <Spieler></red>"));
            return true;
        }

        var offline = Bukkit.getOfflinePlayer(args[0]);
        UUID uuid = offline.getUniqueId();

        if (!offline.hasPlayedBefore() && !mod.isBanned(uuid)) {
            sender.sendMessage(mm.deserialize("<red>✗ Unbekannter Spieler!</red>"));
            return true;
        }

        if (!mod.isBanned(uuid)) {
            sender.sendMessage(mm.deserialize("<red>✗ " + args[0] + " ist nicht gebannt!</red>"));
            return true;
        }

        mod.unban(uuid);

        String staffName = sender instanceof Player ? sender.getName() : "Konsole";
        sender.sendMessage(mm.deserialize("<green>✓ " + args[0] + " wurde entbannt.</green>"));

        broadcastToStaff(
                "<dark_gray>[</dark_gray><green><bold>UNBAN</bold></green><dark_gray>]</dark_gray> " +
                "<white>" + args[0] + "</white> <gray>wurde entbannt von</gray> <white>" + staffName + "</white>"
        );

        return true;
    }

    private void broadcastToStaff(String miniMsg) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("ranksystem.mod"))
                .forEach(p -> p.sendMessage(mm.deserialize(miniMsg)));
        Bukkit.getConsoleSender().sendMessage(mm.deserialize(miniMsg));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("ranksystem.admin")) return new ArrayList<>();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        return new ArrayList<>();
    }
}

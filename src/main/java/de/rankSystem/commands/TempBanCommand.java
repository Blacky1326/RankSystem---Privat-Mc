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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;

public class TempBanCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public TempBanCommand(RankSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ModerationManager mod = plugin.getModerationManager();

        if (!sender.hasPermission("ranksystem.admin")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du hast keine Berechtigung!</red>"));
            return true;
        }

        // /tempban <player> <duration> [reason...]
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize(
                    "<red>Nutzung: /tempban <Spieler> <Dauer> [Grund]</red>\n" +
                    "<gray>Beispiel: /tempban Steve 1d Cheating</gray>"
            ));
            return true;
        }

        long duration = ModerationManager.parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(mm.deserialize("<red>✗ Ungültige Dauer! Beispiel: 1d, 12h, 30m</red>"));
            return true;
        }

        String reason = args.length >= 3
                ? String.join(" ", args).substring(args[0].length() + args[1].length() + 2).trim()
                : "Kein Grund angegeben";
        String durationStr = ModerationManager.formatDuration(duration);
        String staffName = sender instanceof Player ? sender.getName() : "Konsole";

        String kickMsg =
                "<dark_red><bold>Du wurdest gebannt!</bold></dark_red>\n" +
                "<gray>Grund: <white>" + reason + "</white></gray>\n" +
                "<gray>Dauer: <white>" + durationStr + "</white></gray>\n" +
                "<gray>Gebannt von: <white>" + staffName + "</white></gray>\n" +
                "<dark_gray>Alt-Accounts sind ebenfalls gesperrt.</dark_gray>";

        Player target = Bukkit.getPlayer(args[0]);

        if (target != null) {
            if (target.hasPermission("ranksystem.admin") && !sender.hasPermission("ranksystem.rank.owner")) {
                sender.sendMessage(mm.deserialize("<red>✗ Du kannst keine Admins bannen!</red>"));
                return true;
            }

            // Grab IP before kicking
            String ip = null;
            InetSocketAddress addr = target.getAddress();
            if (addr != null) ip = addr.getAddress().getHostAddress();

            mod.tempBan(target.getUniqueId(), ip, duration, reason);
            target.kick(mm.deserialize(kickMsg));

            String ipDisplay = ip != null ? ip : "unbekannt";
            broadcastToStaff(
                    "<dark_gray>[</dark_gray><dark_red><bold>TEMPBAN</bold></dark_red><dark_gray>]</dark_gray> " +
                    "<white>" + target.getName() + "</white> <gray>(" + ipDisplay + ") gebannt von</gray> <white>" + staffName + "</white>" +
                    " <dark_gray>|</dark_gray> <gray>Dauer: <white>" + durationStr + "</white> | Grund: <white>" + reason + "</white></gray>"
            );

        } else {
            // Offline player
            var offline = Bukkit.getOfflinePlayer(args[0]);
            if (!offline.hasPlayedBefore()) {
                sender.sendMessage(mm.deserialize("<red>✗ Unbekannter Spieler!</red>"));
                return true;
            }
            // No IP available for offline players
            mod.tempBan(offline.getUniqueId(), null, duration, reason);

            broadcastToStaff(
                    "<dark_gray>[</dark_gray><dark_red><bold>TEMPBAN</bold></dark_red><dark_gray>]</dark_gray> " +
                    "<white>" + args[0] + "</white> <gray>(offline) gebannt von</gray> <white>" + staffName + "</white>" +
                    " <dark_gray>|</dark_gray> <gray>Dauer: <white>" + durationStr + "</white> | Grund: <white>" + reason + "</white></gray>\n" +
                    "<yellow>⚠ Kein IP-Ban möglich – Spieler ist offline.</yellow>"
            );
        }

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
        if (args.length == 2) return List.of("1h", "6h", "12h", "1d", "3d", "7d", "14d", "30d");
        if (args.length == 3) return List.of("Cheating", "Spam", "Beleidigung", "Griefing", "Hacking");
        return new ArrayList<>();
    }
}

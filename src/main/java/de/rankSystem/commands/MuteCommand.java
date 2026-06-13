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

public class MuteCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public MuteCommand(RankSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ModerationManager mod = plugin.getModerationManager();
        boolean isUnmute = label.equalsIgnoreCase("unmute");

        if (!sender.hasPermission("ranksystem.mod")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du hast keine Berechtigung!</red>"));
            return true;
        }

        if (isUnmute) {
            return handleUnmute(sender, args, mod);
        }

        // /mute <player> <duration> [reason]
        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Nutzung: /mute <Spieler> <Dauer> [Grund]</red>\n<gray>Beispiel: /mute Steve 1h Spam</gray>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>✗ Spieler <white>" + args[0] + "</white> nicht gefunden!</red>"));
            return true;
        }

        if (target.hasPermission("ranksystem.mod") && !sender.hasPermission("ranksystem.admin")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du kannst keine Staff-Mitglieder muten!</red>"));
            return true;
        }

        long duration = ModerationManager.parseDuration(args[1]);
        if (duration <= 0) {
            sender.sendMessage(mm.deserialize("<red>✗ Ungültige Dauer! Beispiel: 30m, 1h, 2d</red>"));
            return true;
        }

        String reason = args.length >= 3
                ? String.join(" ", args).substring(args[0].length() + args[1].length() + 2).trim()
                : "Kein Grund angegeben";
        String staffName = sender instanceof Player ? sender.getName() : "Konsole";
        String durationStr = ModerationManager.formatDuration(duration);

        mod.mute(target.getUniqueId(), duration);

        // Notify staff
        broadcastToStaff(
                "<dark_gray>[</dark_gray><red><bold>MUTE</bold></red><dark_gray>]</dark_gray> " +
                "<white>" + target.getName() + "</white> <gray>wurde gemutet von</gray> <white>" + staffName + "</white>" +
                " <dark_gray>|</dark_gray> <gray>Dauer: <white>" + durationStr + "</white> | Grund: <white>" + reason + "</white></gray>"
        );

        // Notify target
        target.sendMessage(mm.deserialize(
                "<dark_gray>┌──── <red><bold>🔇 Du wurdest gemutet</bold></red> ────┐</dark_gray>\n" +
                "<dark_gray>│</dark_gray> <gray>Grund: <white>" + reason + "</white></gray>\n" +
                "<dark_gray>│</dark_gray> <gray>Dauer: <white>" + durationStr + "</white></gray>\n" +
                "<dark_gray>│</dark_gray> <gray>Gemutet von: <white>" + staffName + "</white></gray>\n" +
                "<dark_gray>└──────────────────────────────────┘</dark_gray>"
        ));

        return true;
    }

    private boolean handleUnmute(CommandSender sender, String[] args, ModerationManager mod) {
        if (args.length < 1) {
            sender.sendMessage(mm.deserialize("<red>Nutzung: /unmute <Spieler></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>✗ Spieler nicht gefunden!</red>"));
            return true;
        }

        if (!mod.isMuted(target.getUniqueId())) {
            sender.sendMessage(mm.deserialize("<yellow>⚠ <white>" + target.getName() + "</white> ist nicht gemutet.</yellow>"));
            return true;
        }

        mod.unmute(target.getUniqueId());
        String staffName = sender instanceof Player ? sender.getName() : "Konsole";

        broadcastToStaff(
                "<dark_gray>[</dark_gray><green><bold>UNMUTE</bold></green><dark_gray>]</dark_gray> " +
                "<white>" + target.getName() + "</white> <gray>wurde entmutet von</gray> <white>" + staffName + "</white>"
        );

        target.sendMessage(mm.deserialize(
                "<green>✔ Du wurdest von <white>" + staffName + "</white> entmutet!</green>"
        ));

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
        if (!sender.hasPermission("ranksystem.mod")) return new ArrayList<>();
        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> names.add(p.getName()));
            return names;
        }
        if (args.length == 2 && !alias.equalsIgnoreCase("unmute")) {
            return List.of("5m", "30m", "1h", "6h", "1d", "7d");
        }
        return new ArrayList<>();
    }
}

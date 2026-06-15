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

public class WarnCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public WarnCommand(RankSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        ModerationManager mod = plugin.getModerationManager();

        if (label.equalsIgnoreCase("warnings")) {
            return handleWarnings(sender, args, mod);
        }

        // /warn <player> <reason...>
        if (!sender.hasPermission("ranksystem.mod")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du hast keine Berechtigung!</red>"));
            return true;
        }

        if (args.length < 2) {
            sender.sendMessage(mm.deserialize("<red>Nutzung: /warn <Spieler> <Grund></red>"));
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(mm.deserialize("<red>✗ Spieler <white>" + args[0] + "</white> nicht gefunden!</red>"));
            return true;
        }

        if (target.hasPermission("ranksystem.mod") && !sender.hasPermission("ranksystem.admin")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du kannst keine Staff-Mitglieder verwarnen!</red>"));
            return true;
        }

        String reason = String.join(" ", args).substring(args[0].length()).trim();
        String staffName = sender instanceof Player ? sender.getName() : "Konsole";
        int count = mod.addWarning(target.getUniqueId(), reason);

        // Notify staff
        String staffMsg = "<dark_gray>[</dark_gray><yellow><bold>WARN</bold></yellow><dark_gray>]</dark_gray> " +
                "<white>" + target.getName() + "</white> <gray>wurde verwarnt von</gray> <white>" + staffName + "</white>" +
                " <dark_gray>|</dark_gray> <gray>Grund: <white>" + reason + "</white></gray>" +
                " <dark_gray>|</dark_gray> <yellow>Warnung #" + count + "</yellow>";
        broadcastToStaff(staffMsg);

        // Notify target
        target.sendMessage(mm.deserialize(
                "<dark_gray>┌──── <yellow><bold>⚠ Du wurdest verwarnt</bold></yellow> ────┐</dark_gray>\n" +
                "<dark_gray>│</dark_gray> <gray>Grund: <white>" + reason + "</white></gray>\n" +
                "<dark_gray>│</dark_gray> <gray>Verwarnt von: <white>" + staffName + "</white></gray>\n" +
                "<dark_gray>│</dark_gray> <yellow>Dies ist Verwarnung #" + count + "</yellow>\n" +
                "<dark_gray>└─────────────────────────────────┘</dark_gray>"
        ));

        return true;
    }

    private boolean handleWarnings(CommandSender sender, String[] args, ModerationManager mod) {
        if (!sender.hasPermission("ranksystem.mod")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du hast keine Berechtigung!</red>"));
            return true;
        }

        Player target;
        if (args.length >= 1) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                sender.sendMessage(mm.deserialize("<red>✗ Spieler nicht gefunden!</red>"));
                return true;
            }
        } else if (sender instanceof Player p) {
            target = p;
        } else {
            sender.sendMessage(mm.deserialize("<red>Bitte gib einen Spielernamen an!</red>"));
            return true;
        }

        List<String> warns = mod.getWarnings(target.getUniqueId());
        if (warns.isEmpty()) {
            sender.sendMessage(mm.deserialize("<green>✔ <white>" + target.getName() + "</white> hat keine Verwarnungen.</green>"));
            return true;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<dark_gray>┌──── Verwarnungen: <white>").append(target.getName()).append("</white> ────┐</dark_gray>\n");
        for (int i = 0; i < warns.size(); i++) {
            sb.append("<dark_gray>│</dark_gray> <yellow>#").append(i + 1).append("</yellow> <gray>").append(warns.get(i)).append("</gray>\n");
        }
        sb.append("<dark_gray>└──────────────────────────────────┘</dark_gray>");
        sender.sendMessage(mm.deserialize(sb.toString()));

        return true;
    }

    private void broadcastToStaff(String miniMsg) {
        Bukkit.getOnlinePlayers().stream()
                .filter(p -> p.hasPermission("ranksystem.mod"))
                .forEach(p -> p.sendMessage(mm.deserialize(miniMsg)));
        // Also print to console
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
        return new ArrayList<>();
    }
}

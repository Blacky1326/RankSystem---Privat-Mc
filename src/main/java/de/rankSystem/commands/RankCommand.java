package de.rankSystem.commands;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public RankCommand(RankSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("ranksystem.admin")) {
            sender.sendMessage(mm.deserialize("<red>✗ Du hast keine Berechtigung!</red>"));
            return true;
        }

        if (args.length < 1) {
            sendHelp(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "set" -> {
                if (args.length < 3) {
                    sender.sendMessage(mm.deserialize("<red>Nutzung: /rank set <Spieler> <Rang></red>"));
                    return true;
                }

                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(mm.deserialize("<red>✗ Spieler <white>" + args[1] + "</white> nicht gefunden!</red>"));
                    return true;
                }

                Rank newRank;
                try {
                    newRank = Rank.valueOf(args[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    sender.sendMessage(mm.deserialize("<red>✗ Ungültiger Rang! Verfügbar: <white>" + getRankList() + "</white></red>"));
                    return true;
                }

                // Owner-Rang nur von Konsolenbefehlen oder anderen Ownern vergeben
                if (newRank == Rank.OWNER && sender instanceof Player senderPlayer) {
                    if (!plugin.getRankManager().getPlayerRank(senderPlayer).equals(Rank.OWNER)) {
                        sender.sendMessage(mm.deserialize("<red>✗ Nur Owner können den Owner-Rang vergeben!</red>"));
                        return true;
                    }
                }

                Rank oldRank = plugin.getRankManager().getPlayerRank(target);
                UUID targetUUID = target.getUniqueId();

                plugin.getRankManager().setPlayerRank(targetUUID, newRank, oldRank)
                        .thenRun(() -> {
                            // Zurück auf Main-Thread für Bukkit-API
                            Bukkit.getScheduler().runTask(plugin, () -> {
                                plugin.getTabManager().updatePlayer(target);

                                sender.sendMessage(mm.deserialize(
                                        "<green>✔ Rang von <white>" + target.getName() +
                                        "</white> auf " + newRank.getChatPrefix().toString() +
                                        " <green>gesetzt!</green>"
                                ));

                                target.sendMessage(mm.deserialize(
                                        "<gray>Dein Rang wurde auf </gray>" +
                                        "<gradient:#00FF7F:#00CED1><bold>" + newRank.getDisplayName() + "</bold></gradient>" +
                                        "<gray> gesetzt!</gray>"
                                ));
                            });
                        });
            }

            case "info" -> {
                Player target;
                if (args.length >= 2) {
                    target = Bukkit.getPlayer(args[1]);
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

                Rank rank = plugin.getRankManager().getPlayerRank(target);
                sender.sendMessage(mm.deserialize(
                        "<dark_gray>┌──────────────────────────┐</dark_gray>\n" +
                        "<dark_gray>│</dark_gray> <gray>Spieler: <white>" + target.getName() + "</white></gray>\n" +
                        "<dark_gray>│</dark_gray> <gray>Rang: </gray>" + rank.getChatPrefixMini() + "\n" +
                        "<dark_gray>└──────────────────────────┘</dark_gray>"
                ));
            }

            case "reload" -> {
                if (!sender.hasPermission("ranksystem.admin")) {
                    sender.sendMessage(mm.deserialize("<red>✗ Kein Zugriff!</red>"));
                    return true;
                }
                plugin.getConfigManager().reload();
                plugin.getActionBarManager().restart();
                sender.sendMessage(mm.deserialize("<green>✔ Config erfolgreich neu geladen!</green>"));
            }

            case "list" -> {
                sender.sendMessage(mm.deserialize("<dark_gray>┌──── Alle Ränge ────┐</dark_gray>"));
                for (Rank rank : Rank.values()) {
                    sender.sendMessage(mm.deserialize(
                        "<dark_gray>│</dark_gray> " + rank.getChatPrefixMini()
                        + " <gray>(" + rank.getLuckPermsGroup() + ")</gray>"
                    ));
                }
                sender.sendMessage(mm.deserialize("<dark_gray>└───────────────────┘</dark_gray>"));
            }

            default -> sendHelp(sender);
        }
        return true;
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize(
                "<dark_gray>┌──── RankSystem Hilfe ────┐</dark_gray>\n" +
                "<dark_gray>│</dark_gray> <gradient:#00BFFF:#9400D3>/rank set <Spieler> <Rang></gradient>\n" +
                "<dark_gray>│</dark_gray> <gradient:#00BFFF:#9400D3>/rank info [Spieler]</gradient>\n" +
                "<dark_gray>│</dark_gray> <gradient:#00BFFF:#9400D3>/rank list</gradient>\n" +
                "<dark_gray>└─────────────────────────┘</dark_gray>"
        ));
    }


    private String getRankList() {
        StringBuilder sb = new StringBuilder();
        for (Rank rank : Rank.values()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(rank.name());
        }
        return sb.toString();
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!sender.hasPermission("ranksystem.admin")) return new ArrayList<>();

        if (args.length == 1) {
            return Arrays.asList("set", "info", "list", "reload");
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("set") || args[0].equalsIgnoreCase("info"))) {
            List<String> players = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> players.add(p.getName()));
            return players;
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            List<String> ranks = new ArrayList<>();
            for (Rank rank : Rank.values()) ranks.add(rank.name().toLowerCase());
            return ranks;
        }
        return new ArrayList<>();
    }
}

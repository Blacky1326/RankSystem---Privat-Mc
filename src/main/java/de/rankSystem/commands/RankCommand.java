package de.rankSystem.commands;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.*;

public class RankCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public RankCommand(RankSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (!sender.hasPermission("ranksystem.admin")) {
            sender.sendMessage(mm.deserialize("<red>✗ Keine Rechte!</red>"));
            return true;
        }

        if (args.length == 0) {
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
                    sender.sendMessage(mm.deserialize("<red>Spieler nicht online!</red>"));
                    return true;
                }

                Rank newRank;
                try {
                    newRank = Rank.valueOf(args[2].toUpperCase());
                } catch (Exception e) {
                    sender.sendMessage(mm.deserialize("<red>Ungültiger Rang!</red>"));
                    return true;
                }

                Rank oldRank = plugin.getRankManager().getPlayerRank(target);
                UUID uuid = target.getUniqueId();

                Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {

                    plugin.getRankManager().setPlayerRank(uuid, newRank, oldRank).thenRun(() -> {

                        Bukkit.getScheduler().runTask(plugin, () -> {

                            plugin.getTabManager().updatePlayer(target);

                            sender.sendMessage(mm.deserialize(
                                    "<green>✔ Rang von <white>" + target.getName() +
                                            "</white> geändert zu " +
                                            plugin.getConfigManager().getRankDisplay(newRank.name().toLowerCase()) +
                                            "</green>"
                            ));

                            target.sendMessage(mm.deserialize(
                                    "<gray>Dein Rang wurde geändert zu </gray>" +
                                            "<gradient:#00FF7F:#00CED1><bold>" +
                                            plugin.getConfigManager().getRankDisplay(newRank.name().toLowerCase()) +
                                            "</bold></gradient>"
                            ));
                        });
                    });
                });
            }

            case "info" -> {
                Player target = (args.length >= 2)
                        ? Bukkit.getPlayer(args[1])
                        : (sender instanceof Player p ? p : null);

                if (target == null) {
                    sender.sendMessage(mm.deserialize("<red>Spieler nicht gefunden!</red>"));
                    return true;
                }

                Rank rank = plugin.getRankManager().getPlayerRank(target);

                sender.sendMessage(mm.deserialize(
                        "<dark_gray>┌──── Info ────┐</dark_gray>\n" +
                                "<dark_gray>│</dark_gray> <white>" + target.getName() + "</white>\n" +
                                "<dark_gray>│</dark_gray> " + serialize(rank.getChatPrefix()) + "\n" +
                                "<dark_gray>└─────────────┘</dark_gray>"
                ));
            }

            case "list" -> {
                StringBuilder sb = new StringBuilder();
                sb.append("<dark_gray>┌──── Ränge ────┐</dark_gray>\n");

                for (Rank rank : Rank.values()) {
                    sb.append("<dark_gray>│</dark_gray> ")
                            .append(serialize(rank.getChatPrefix()))
                            .append("\n");
                }

                sb.append("<dark_gray>└──────────────┘</dark_gray>");
                sender.sendMessage(mm.deserialize(sb.toString()));
            }

            case "reload" -> {
                plugin.getConfigManager().reload();
                plugin.getActionBarManager().restart();
                sender.sendMessage(mm.deserialize("<green>Config neu geladen!</green>"));
            }

            default -> sendHelp(sender);
        }

        return true;
    }

    private String serialize(net.kyori.adventure.text.Component component) {
        return net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer
                .legacySection().serialize(component);
    }

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(mm.deserialize(
                "<dark_gray>/rank set <Spieler> <Rang>\n" +
                        "/rank info\n" +
                        "/rank list\n" +
                        "/rank reload</dark_gray>"
        ));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {

        if (!sender.hasPermission("ranksystem.admin")) return Collections.emptyList();

        if (args.length == 1) {
            return Arrays.asList("set", "info", "list", "reload");
        }

        if (args.length == 2) {
            return Bukkit.getOnlinePlayers()
                    .stream()
                    .map(Player::getName)
                    .toList();
        }

        if (args.length == 3 && args[0].equalsIgnoreCase("set")) {
            return Arrays.stream(Rank.values())
                    .map(r -> r.name().toLowerCase())
                    .toList();
        }

        return Collections.emptyList();
    }
}

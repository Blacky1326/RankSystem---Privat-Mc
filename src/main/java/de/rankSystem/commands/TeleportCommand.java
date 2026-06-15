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
import java.util.List;

public class TeleportCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final String commandType; // "tp", "tpa", "tpaccept", "tpdeny"

    public TeleportCommand(RankSystem plugin, String commandType) {
        this.plugin = plugin;
        this.commandType = commandType;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>✗ Nur Spieler können diesen Befehl nutzen!</red>"));
            return true;
        }

        switch (commandType) {
            case "tp"       -> handleTp(player, args);
            case "tpa"      -> handleTpa(player, args);
            case "tpaccept" -> plugin.getTeleportManager().acceptRequest(player);
            case "tpdeny"   -> plugin.getTeleportManager().denyRequest(player);
        }
        return true;
    }

    // ── /tp <Spieler> ── (nur Mod+, sofort) ─────────────────────────────────
    private void handleTp(Player player, String[] args) {
        Rank rank = plugin.getRankManager().getPlayerRank(player);

        if (rank.getWeight() > Rank.MODERATOR.getWeight()) {
            player.sendMessage(mm.deserialize(
                "<red>✗ Du hast keine Berechtigung! Nutze </red><white>/tpa</white><red> für Teleport-Anfragen.</red>"
            ));
            return;
        }

        if (args.length < 1) {
            player.sendMessage(mm.deserialize("<red>Nutzung: /tp <Spieler></red>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(mm.deserialize("<red>✗ Spieler <white>" + args[0] + "</white> nicht gefunden!</red>"));
            return;
        }

        if (target.equals(player)) {
            player.sendMessage(mm.deserialize("<red>✗ Du kannst dich nicht zu dir selbst teleportieren!</red>"));
            return;
        }

        plugin.getTeleportManager().staffTeleport(player, target);
    }

    // ── /tpa <Spieler> ── (alle Spieler, mit Anfrage + Countdown) ───────────
    private void handleTpa(Player player, String[] args) {
        if (args.length < 1) {
            player.sendMessage(mm.deserialize("<red>Nutzung: /tpa <Spieler></red>"));
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null || !target.isOnline()) {
            player.sendMessage(mm.deserialize("<red>✗ Spieler <white>" + args[0] + "</white> nicht gefunden!</red>"));
            return;
        }

        // Mod+ können auch /tpa nutzen (kein Cooldown für Staff)
        Rank rank = plugin.getRankManager().getPlayerRank(player);
        if (rank.getWeight() <= Rank.MODERATOR.getWeight()) {
            // Staff → sofort teleportieren statt Anfrage
            plugin.getTeleportManager().staffTeleport(player, target);
            return;
        }

        plugin.getTeleportManager().sendRequest(player, target);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player)) return new ArrayList<>();

        if ((commandType.equals("tp") || commandType.equals("tpa")) && args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (!p.equals(sender)) names.add(p.getName());
            });
            return names;
        }
        return new ArrayList<>();
    }
}

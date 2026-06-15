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

public class VanishCommand implements CommandExecutor, TabCompleter {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    public VanishCommand(RankSystem plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        // Nur Spieler (kein Konsolen-Vanish)
        if (!(sender instanceof Player player)) {
            sender.sendMessage(mm.deserialize("<red>✗ Nur Spieler können diesen Befehl nutzen!</red>"));
            return true;
        }

        // Rang prüfen: mindestens Moderator
        Rank rank = plugin.getRankManager().getPlayerRank(player);
        if (rank.getWeight() > Rank.MODERATOR.getWeight()) {
            player.sendMessage(mm.deserialize("<red>✗ Du hast keine Berechtigung! (Mindest-Rang: Moderator)</red>"));
            return true;
        }

        // /vanish [Spieler] → anderen Spieler vanishen (nur Admin+)
        if (args.length >= 1) {
            if (rank.getWeight() > Rank.ADMIN.getWeight()) {
                player.sendMessage(mm.deserialize("<red>✗ Nur Admins können andere Spieler vanishen!</red>"));
                return true;
            }

            Player target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                player.sendMessage(mm.deserialize("<red>✗ Spieler <white>" + args[0] + "</white> nicht gefunden!</red>"));
                return true;
            }

            Rank targetRank = plugin.getRankManager().getPlayerRank(target);
            if (targetRank.getWeight() <= rank.getWeight() && !target.equals(player)) {
                player.sendMessage(mm.deserialize("<red>✗ Du kannst keinen gleichrangigen oder höheren Staff vanishen!</red>"));
                return true;
            }

            plugin.getVanishManager().toggleVanish(target);

            boolean nowVanished = plugin.getVanishManager().isVanished(target);
            player.sendMessage(mm.deserialize(
                "<gray>» </gray><white>" + target.getName() + "</white>" +
                (nowVanished
                    ? " <gray>ist jetzt </gray><gradient:#9400D3:#FF1493><bold>vanished</bold></gradient> <gray>👻</gray>"
                    : " <gray>ist jetzt wieder </gray><green><bold>sichtbar</bold></green><gray>.</gray>")
            ));
            return true;
        }

        // /vanish → sich selbst vanishen/unvanishen
        plugin.getVanishManager().toggleVanish(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String alias, String[] args) {
        if (!(sender instanceof Player player)) return new ArrayList<>();

        Rank rank = plugin.getRankManager().getPlayerRank(player);
        if (rank.getWeight() > Rank.ADMIN.getWeight()) return new ArrayList<>();

        if (args.length == 1) {
            List<String> names = new ArrayList<>();
            Bukkit.getOnlinePlayers().forEach(p -> {
                if (!p.equals(player)) names.add(p.getName());
            });
            return names;
        }
        return new ArrayList<>();
    }
}

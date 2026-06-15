package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class VanishManager {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();
    private final Set<UUID> vanishedPlayers = new HashSet<>();

    public VanishManager(RankSystem plugin) {
        this.plugin = plugin;
    }

    public boolean isVanished(Player player) {
        return vanishedPlayers.contains(player.getUniqueId());
    }

    public Set<UUID> getVanishedPlayers() {
        return Collections.unmodifiableSet(vanishedPlayers);
    }

    /**
     * Vanish ein- oder ausschalten für einen Spieler.
     */
    public void toggleVanish(Player player) {
        if (isVanished(player)) {
            unvanish(player);
        } else {
            vanish(player);
        }
    }

    /**
     * Spieler verstecken:
     * - Für normale Spieler (unterhalb Mod) unsichtbar
     * - Fake-Quit Nachricht senden
     * - Aus Tab-Liste entfernen (für normale Spieler)
     * - Staff sehen 👻 Symbol
     */
    public void vanish(Player player) {
        vanishedPlayers.add(player.getUniqueId());
        Rank playerRank = plugin.getRankManager().getPlayerRank(player);

        // Fake Quit-Nachricht an alle senden
        String fakeQuit = plugin.getConfigManager().getVanishFakeQuit()
                .replace("%name%", player.getName())
                .replace("%rank%", playerRank.getDisplayName());
        Bukkit.broadcast(mm.deserialize(fakeQuit));

        // Spieler für normale Spieler verstecken
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            if (canSeeVanished(online)) {
                // Staff → Tab-Name mit 👻
                updateStaffTabName(player, online);
            } else {
                // Normaler Spieler → komplett verstecken
                online.hidePlayer(plugin, player);
            }
        }

        // Tab-Liste für nicht-Staff ausblenden
        player.setPlayerListName(null); // wird durch updateTabName überschrieben

        // Eigene Bestätigung
        player.sendMessage(mm.deserialize("<gray>Du bist jetzt </gray><gradient:#9400D3:#FF1493><bold>verschwunden</bold></gradient><gray> 👻</gray>"));

        // Tab für alle neu aufbauen
        plugin.getTabManager().updateAllPlayers();
    }

    /**
     * Spieler wieder sichtbar machen:
     * - Fake-Join Nachricht senden
     * - Wieder in Tab-Liste anzeigen
     */
    public void unvanish(Player player) {
        vanishedPlayers.remove(player.getUniqueId());
        Rank playerRank = plugin.getRankManager().getPlayerRank(player);

        // Für alle wieder sichtbar machen
        for (Player online : Bukkit.getOnlinePlayers()) {
            if (online.equals(player)) continue;
            online.showPlayer(plugin, player);
        }

        // Fake Join-Nachricht
        String fakeJoin = plugin.getConfigManager().getVanishFakeJoin()
                .replace("%name%", player.getName())
                .replace("%rank%", playerRank.getDisplayName());
        Bukkit.broadcast(mm.deserialize(fakeJoin));

        // Eigene Bestätigung
        player.sendMessage(mm.deserialize("<gray>Du bist jetzt wieder </gray><green><bold>sichtbar</bold></green><gray>!</gray>"));

        // Tab für alle neu aufbauen
        plugin.getTabManager().updateAllPlayers();
    }

    /**
     * Wenn ein neuer Spieler joint: Vanished-Spieler vor ihm verstecken
     * und umgekehrt Staff-Tag setzen.
     */
    public void applyVanishToNewPlayer(Player newPlayer) {
        for (UUID uuid : vanishedPlayers) {
            Player vanished = Bukkit.getPlayer(uuid);
            if (vanished == null) continue;

            if (canSeeVanished(newPlayer)) {
                updateStaffTabName(vanished, newPlayer);
            } else {
                newPlayer.hidePlayer(plugin, vanished);
            }
        }
    }

    /**
     * Beim Plugin-Reload / Neustart alle Vanish-States zurücksetzen.
     */
    public void removeAll() {
        for (UUID uuid : vanishedPlayers) {
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) unvanish(p);
        }
        vanishedPlayers.clear();
    }

    /**
     * Prüft ob ein Spieler vanished Spieler sehen darf (Mod+).
     */
    public boolean canSeeVanished(Player observer) {
        Rank rank = plugin.getRankManager().getPlayerRank(observer);
        return rank.getWeight() <= Rank.MODERATOR.getWeight();
    }

    /**
     * Setzt den Tab-Namen für Staff mit 👻 Prefix.
     */
    private void updateStaffTabName(Player vanished, Player staffViewer) {
        Rank rank = plugin.getRankManager().getPlayerRank(vanished);
        staffViewer.sendMessage(mm.deserialize(
            "<dark_gray>[</dark_gray><gradient:#9400D3:#FF1493>Vanish</gradient><dark_gray>]</dark_gray> " +
            "<gray>" + vanished.getName() + " ist im Vanish-Modus.</gray>"
        ));
        // Tab-Update läuft über TabManager der das 👻 hinzufügt
        plugin.getTabManager().updatePlayer(vanished);
    }
}

package de.rankSystem.listeners;

import de.rankSystem.RankSystem;
import org.bukkit.entity.Creature;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class VanishListener implements Listener {

    private final RankSystem plugin;

    public VanishListener(RankSystem plugin) {
        this.plugin = plugin;
    }

    /**
     * Wenn ein neuer Spieler joint: Vanished-Spieler vor ihm verstecken.
     */
    @EventHandler(priority = EventPriority.HIGH)
    public void onJoin(PlayerJoinEvent event) {
        Player newPlayer = event.getPlayer();

        // Kurz warten bis LuckPerms-Cache geladen ist
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            plugin.getVanishManager().applyVanishToNewPlayer(newPlayer);
        }, 5L);
    }

    /**
     * Wenn ein vanished Spieler disconnected: State aufräumen (kein Fake-Quit nötig,
     * der wurde schon beim /vanish gesendet).
     */
    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (plugin.getVanishManager().isVanished(player)) {
            // State entfernen ohne Fake-Quit (echter Quit soll keine Nachricht senden)
            plugin.getVanishManager().getVanishedPlayers()
                    .stream()
                    .filter(uuid -> uuid.equals(player.getUniqueId()))
                    .findFirst()
                    .ifPresent(uuid -> {
                        // Quit-Message unterdrücken — Spieler war für normale schon "weg"
                        event.quitMessage(null);
                    });
        }
    }

    /**
     * Verhindert, dass Mobs vanished Spieler anvisieren.
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onMobTarget(EntityTargetLivingEntityEvent event) {
        if (!(event.getTarget() instanceof Player target)) return;
        if (!(event.getEntity() instanceof Creature)) return;

        if (plugin.getVanishManager().isVanished(target)) {
            event.setCancelled(true);
        }
    }
}

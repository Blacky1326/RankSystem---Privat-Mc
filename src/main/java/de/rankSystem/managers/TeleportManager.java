package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class TeleportManager {

    private final RankSystem plugin;
    private final MiniMessage mm = MiniMessage.miniMessage();

    private final Map<UUID, UUID> pendingRequests  = new HashMap<>();
    private final Map<UUID, BukkitTask> countdownTasks = new HashMap<>();
    private final Map<UUID, Location> startLocations   = new HashMap<>();
    private final Map<UUID, Long> cooldowns            = new HashMap<>();

    public TeleportManager(RankSystem plugin) {
        this.plugin = plugin;
    }

    // ── Config-Werte (immer frisch lesen → /rank reload wirkt sofort) ────────

    private int getCooldown()        { return plugin.getConfigManager().getTpCooldown(); }
    private int getCountdown()       { return plugin.getConfigManager().getTpCountdown(); }
    private int getRequestTimeout()  { return plugin.getConfigManager().getTpRequestTimeout(); }
    private double getMoveTolerance(){ return plugin.getConfigManager().getTpMoveTolerance(); }

    // ── STAFF TELEPORT (/tp) ─────────────────────────────────────────────────

    public void staffTeleport(Player staff, Player target) {
        staff.teleport(target.getLocation());
        staff.sendMessage(mm.deserialize(
            "<dark_gray>» </dark_gray><gray>Du wurdest zu </gray><white>" + target.getName() + "</white><gray> teleportiert.</gray>"
        ));
        target.sendMessage(mm.deserialize(
            "<dark_gray>» </dark_gray><gray><white>" + staff.getName() + "</white> hat sich zu dir teleportiert.</gray>"
        ));
    }

    // ── TPA ANFRAGE (/tpa) ───────────────────────────────────────────────────

    public boolean sendRequest(Player requester, Player target) {
        if (isOnCooldown(requester)) {
            long remaining = getRemainingCooldown(requester);
            requester.sendMessage(mm.deserialize(
                "<red>✗ Du musst noch </red><white>" + remaining + " Sekunden</white><red> warten!</red>"
            ));
            return false;
        }

        if (requester.equals(target)) {
            requester.sendMessage(mm.deserialize("<red>✗ Du kannst keine Anfrage an dich selbst senden!</red>"));
            return false;
        }

        cancelRequest(requester.getUniqueId());
        pendingRequests.put(requester.getUniqueId(), target.getUniqueId());

        requester.sendMessage(mm.deserialize(
            "<dark_gray>┌──────────────────────────────┐</dark_gray>\n" +
            "<dark_gray>│</dark_gray> <gradient:#00BFFF:#9400D3>Teleport-Anfrage gesendet!</gradient>\n" +
            "<dark_gray>│</dark_gray> <gray>Ziel: <white>" + target.getName() + "</white></gray>\n" +
            "<dark_gray>│</dark_gray> <gray>Warte auf Bestätigung...</gray>\n" +
            "<dark_gray>└──────────────────────────────┘</dark_gray>"
        ));

        target.sendMessage(mm.deserialize(
            "<dark_gray>┌──────────────────────────────┐</dark_gray>\n" +
            "<dark_gray>│</dark_gray> <gradient:#00FF7F:#00CED1>Teleport-Anfrage erhalten!</gradient>\n" +
            "<dark_gray>│</dark_gray> <gray>Von: <white>" + requester.getName() + "</white></gray>\n" +
            "<dark_gray>│</dark_gray> " +
            "<click:run_command:/tpaccept><hover:show_text:'<green>Anfrage annehmen</green>'><green><bold>[✔ ANNEHMEN]</bold></green></hover></click>  " +
            "<click:run_command:/tpdeny><hover:show_text:'<red>Anfrage ablehnen</red>'><red><bold>[✗ ABLEHNEN]</bold></red></hover></click>\n" +
            "<dark_gray>└──────────────────────────────┘</dark_gray>"
        ));

        // Anfrage nach Timeout automatisch ablaufen lassen
        int timeout = getRequestTimeout();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (pendingRequests.containsKey(requester.getUniqueId())) {
                pendingRequests.remove(requester.getUniqueId());
                if (requester.isOnline())
                    requester.sendMessage(mm.deserialize(
                        "<gray>» Deine Teleport-Anfrage an <white>" + target.getName() + "</white> ist abgelaufen.</gray>"
                    ));
                if (target.isOnline())
                    target.sendMessage(mm.deserialize(
                        "<gray>» Die Teleport-Anfrage von <white>" + requester.getName() + "</white> ist abgelaufen.</gray>"
                    ));
            }
        }, timeout * 20L);

        return true;
    }

    public void acceptRequest(Player target) {
        UUID requesterUUID = null;
        for (Map.Entry<UUID, UUID> entry : pendingRequests.entrySet()) {
            if (entry.getValue().equals(target.getUniqueId())) {
                requesterUUID = entry.getKey();
                break;
            }
        }

        if (requesterUUID == null) {
            target.sendMessage(mm.deserialize("<red>✗ Du hast keine offene Teleport-Anfrage!</red>"));
            return;
        }

        Player requester = plugin.getServer().getPlayer(requesterUUID);
        if (requester == null || !requester.isOnline()) {
            pendingRequests.remove(requesterUUID);
            target.sendMessage(mm.deserialize("<red>✗ Der Spieler ist nicht mehr online!</red>"));
            return;
        }

        pendingRequests.remove(requesterUUID);
        startLocations.put(requesterUUID, requester.getLocation().clone());

        int countdown = getCountdown();

        requester.sendMessage(mm.deserialize(
            "<green>✔ <white>" + target.getName() + "</white> hat deine Anfrage angenommen!</green>\n" +
            "<gray>Teleport in <white>" + countdown + " Sekunden</white> – <red>Nicht bewegen!</red></gray>"
        ));
        target.sendMessage(mm.deserialize(
            "<green>✔ Du hast die Anfrage von <white>" + requester.getName() + "</white> angenommen.</green>"
        ));

        final UUID fUUID = requesterUUID;
        final int[] remaining = {countdown};

        BukkitTask task = plugin.getServer().getScheduler().runTaskTimer(plugin, () -> {
            Player req = plugin.getServer().getPlayer(fUUID);

            if (req == null || !req.isOnline()) {
                cancelRequest(fUUID);
                return;
            }

            Location savedLoc = startLocations.get(fUUID);
            if (savedLoc != null && hasMovedSignificantly(savedLoc, req.getLocation())) {
                cancelRequest(fUUID);
                req.sendMessage(mm.deserialize("<red>✗ Teleport abgebrochen – du hast dich bewegt!</red>"));
                if (target.isOnline())
                    target.sendMessage(mm.deserialize(
                        "<gray>» Teleport von <white>" + req.getName() + "</white> wurde abgebrochen (Bewegung).</gray>"
                    ));
                return;
            }

            remaining[0]--;

            if (remaining[0] <= 0) {
                cancelRequest(fUUID);
                setCooldown(fUUID);

                if (!target.isOnline()) {
                    req.sendMessage(mm.deserialize("<red>✗ Das Ziel ist nicht mehr online!</red>"));
                    return;
                }

                req.teleport(target.getLocation());
                req.sendMessage(mm.deserialize(
                    "<green>✔ Du wurdest zu <white>" + target.getName() + "</white> teleportiert!</green>"
                ));
                target.sendMessage(mm.deserialize(
                    "<gray>» <white>" + req.getName() + "</white> wurde zu dir teleportiert.</gray>"
                ));
            }
        }, 20L, 20L);

        countdownTasks.put(requesterUUID, task);
    }

    public void denyRequest(Player target) {
        UUID requesterUUID = null;
        for (Map.Entry<UUID, UUID> entry : pendingRequests.entrySet()) {
            if (entry.getValue().equals(target.getUniqueId())) {
                requesterUUID = entry.getKey();
                break;
            }
        }

        if (requesterUUID == null) {
            target.sendMessage(mm.deserialize("<red>✗ Du hast keine offene Teleport-Anfrage!</red>"));
            return;
        }

        Player requester = plugin.getServer().getPlayer(requesterUUID);
        pendingRequests.remove(requesterUUID);

        target.sendMessage(mm.deserialize(
            "<gray>» Du hast die Anfrage von <white>" +
            (requester != null ? requester.getName() : "Unbekannt") +
            "</white> abgelehnt.</gray>"
        ));

        if (requester != null && requester.isOnline())
            requester.sendMessage(mm.deserialize(
                "<red>✗ <white>" + target.getName() + "</white> hat deine Teleport-Anfrage abgelehnt.</red>"
            ));
    }

    // ── HILFSMETHODEN ────────────────────────────────────────────────────────

    private void cancelRequest(UUID uuid) {
        pendingRequests.remove(uuid);
        startLocations.remove(uuid);
        BukkitTask task = countdownTasks.remove(uuid);
        if (task != null) task.cancel();
    }

    private boolean hasMovedSignificantly(Location a, Location b) {
        if (!a.getWorld().equals(b.getWorld())) return true;
        double tol = getMoveTolerance();
        return a.distanceSquared(b) > (tol * tol);
    }

    private boolean isOnCooldown(Player player) {
        Long expires = cooldowns.get(player.getUniqueId());
        return expires != null && System.currentTimeMillis() < expires;
    }

    private long getRemainingCooldown(Player player) {
        Long expires = cooldowns.get(player.getUniqueId());
        if (expires == null) return 0;
        return Math.max(0, (expires - System.currentTimeMillis()) / 1000);
    }

    private void setCooldown(UUID uuid) {
        cooldowns.put(uuid, System.currentTimeMillis() + (getCooldown() * 1000L));
    }

    public void cleanup() {
        countdownTasks.values().forEach(BukkitTask::cancel);
        countdownTasks.clear();
        pendingRequests.clear();
        startLocations.clear();
        cooldowns.clear();
    }
}

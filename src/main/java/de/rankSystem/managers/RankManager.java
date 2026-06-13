package de.rankSystem.managers;

import de.rankSystem.RankSystem;
import de.rankSystem.utils.Rank;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.node.types.InheritanceNode;
import net.luckperms.api.node.types.PermissionNode;
import net.luckperms.api.query.QueryOptions;
import org.bukkit.entity.Player;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class RankManager {

    private final RankSystem plugin;
    private final LuckPerms luckPerms;

    public RankManager(RankSystem plugin) {
        this.plugin = plugin;
        this.luckPerms = plugin.getLuckPerms();
    }

    /**
     * Erstellt alle LuckPerms-Gruppen mit Permissions beim Start
     */
    public void setupLuckPermsGroups() {
        for (Rank rank : Rank.values()) {
            String groupName = rank.getLuckPermsGroup();
            if (groupName.equals("default")) continue; // default existiert immer

            luckPerms.getGroupManager().createAndLoadGroup(groupName).thenAccept(group -> {
                // Basis-Permission für die Gruppe
                group.data().add(PermissionNode.builder(rank.getPermission()).build());

                // Gruppen-Hierarchie: Niedrigere Ränge erben von höheren
                setupGroupInheritance(group, rank);

                luckPerms.getGroupManager().saveGroup(group);
                plugin.getLogger().info("Gruppe erstellt/aktualisiert: " + groupName);
            });
        }

        // Setze Gewichte für korrekte Sortierung
        setupGroupWeights();
    }

    private void setupGroupInheritance(Group group, Rank rank) {
        // Jede Gruppe erbt von der nächst-niedrigeren
        switch (rank) {
            case OWNER -> {
                addInheritance(group, "admin");
                // Owner hat alle Permissions
                group.data().add(PermissionNode.builder("*").build());
            }
            case ADMIN -> {
                addInheritance(group, "moderator");
                group.data().add(PermissionNode.builder("ranksystem.admin").build());
            }
            case MODERATOR -> {
                addInheritance(group, "supporter");
                group.data().add(PermissionNode.builder("ranksystem.mod").build());
            }
            case SUPPORTER -> {
                addInheritance(group, "default");
                group.data().add(PermissionNode.builder("ranksystem.support").build());
            }
            case STREAMER -> {
                addInheritance(group, "vip");
                group.data().add(PermissionNode.builder("ranksystem.streamer").build());
            }
            case VIP -> {
                addInheritance(group, "default");
                group.data().add(PermissionNode.builder("ranksystem.vip").build());
            }
        }
    }

    private void addInheritance(Group group, String parentName) {
        luckPerms.getGroupManager().loadGroup(parentName).thenAccept(optParent -> {
            optParent.ifPresent(parent ->
                    group.data().add(InheritanceNode.builder(parent).build())
            );
        });
    }

    private void setupGroupWeights() {
        for (Rank rank : Rank.values()) {
            String groupName = rank.getLuckPermsGroup();
            luckPerms.getGroupManager().loadGroup(groupName).thenAccept(optGroup -> {
                optGroup.ifPresent(group -> {
                    // Setze Prefix via Meta
                    group.data().add(
                            net.luckperms.api.node.types.PrefixNode.builder(
                                    "[" + rank.getDisplayName() + "] ", rank.getWeight()
                            ).build()
                    );
                    luckPerms.getGroupManager().saveGroup(group);
                });
            });
        }
    }

    /**
     * Gibt den Rang eines Spielers zurück (aus LuckPerms)
     */
    public Rank getPlayerRank(Player player) {
        User user = luckPerms.getUserManager().getUser(player.getUniqueId());
        if (user == null) return Rank.MITGLIED;

        String primaryGroup = user.getPrimaryGroup();
        return Rank.fromGroup(primaryGroup);
    }

    /**
     * Setzt den Rang eines Spielers (async)
     */
    public CompletableFuture<Void> setPlayerRank(UUID uuid, Rank newRank, Rank oldRank) {
        return luckPerms.getUserManager().loadUser(uuid).thenAccept(user -> {
            if (user == null) return;

            // Alle alten Rang-Gruppen entfernen
            for (Rank rank : Rank.values()) {
                if (rank == Rank.MITGLIED) continue;
                user.data().clear(node ->
                        node instanceof InheritanceNode &&
                        ((InheritanceNode) node).getGroupName().equalsIgnoreCase(rank.getLuckPermsGroup())
                );
            }

            // Neue Gruppe setzen
            if (newRank != Rank.MITGLIED) {
                luckPerms.getGroupManager().loadGroup(newRank.getLuckPermsGroup())
                        .thenAccept(optGroup -> optGroup.ifPresent(group -> {
                            user.data().add(InheritanceNode.builder(group).build());
                            user.setPrimaryGroup(newRank.getLuckPermsGroup());
                        })).join();
            } else {
                user.setPrimaryGroup("default");
            }

            luckPerms.getUserManager().saveUser(user);
        });
    }
}

package de.rankSystem;

import de.rankSystem.commands.RankCommand;
import de.rankSystem.listeners.ChatListener;
import de.rankSystem.listeners.PlayerJoinListener;
import de.rankSystem.managers.*;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.RegisteredServiceProvider;

public class RankSystem extends JavaPlugin {

    private static RankSystem instance;

    private LuckPerms luckPerms;
    private RankManager rankManager;
    private TabManager tabManager;
    private ConfigManager configManager;
    private ActionBarManager actionBarManager;

    @Override
    public void onEnable() {

        instance = this;

        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms nicht gefunden!");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        rankManager = new RankManager(this);
        tabManager = new TabManager(this);

        rankManager.setupLuckPermsGroups();

        Bukkit.getPluginManager().registerEvents(new MotdManager(this), this);
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        if (getCommand("rank") != null) {
            RankCommand cmd = new RankCommand(this);
            getCommand("rank").setExecutor(cmd);
            getCommand("rank").setTabCompleter(cmd);
        }

        Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p));

        luckPerms.getEventBus().subscribe(this, UserDataRecalculateEvent.class, event -> {
            Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
            if (player != null) {
                tabManager.updatePlayer(player);
            }
        });

        Bukkit.getScheduler().runTaskTimer(this, () ->
                Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p)),
        1200L, 1200L);

        Bukkit.getScheduler().runTaskLater(this, () -> {
            actionBarManager = new ActionBarManager(this);
            actionBarManager.start();
        }, 20L);

        getLogger().info("RankSystem gestartet!");
    }

    @Override
    public void onDisable() {
        getLogger().info("RankSystem wurde deaktiviert.");
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);

        if (provider == null) return false;

        luckPerms = provider.getProvider();
        return true;
    }

    public static RankSystem getInstance() { return instance; }
    public LuckPerms getLuckPerms() { return luckPerms; }
    public RankManager getRankManager() { return rankManager; }
    public TabManager getTabManager() { return tabManager; }
    public ConfigManager getConfigManager() { return configManager; }
    public ActionBarManager getActionBarManager() { return actionBarManager; }
}

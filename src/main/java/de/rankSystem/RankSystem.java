package de.rankSystem;

import de.rankSystem.commands.RankCommand;
import de.rankSystem.listeners.ChatListener;
import de.rankSystem.listeners.PlayerJoinListener;
import de.rankSystem.managers.ActionBarManager;
import de.rankSystem.managers.ConfigManager;
import de.rankSystem.managers.MotdManager;
import de.rankSystem.managers.RankManager;
import de.rankSystem.managers.TabManager;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.event.user.UserDataRecalculateEvent;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

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
            getLogger().severe("LuckPerms nicht gefunden! Plugin wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        configManager = new ConfigManager(this);
        rankManager   = new RankManager(this);
        tabManager    = new TabManager(this);

        rankManager.setupLuckPermsGroups();

        actionBarManager = new ActionBarManager(this);
        actionBarManager.start();

        MotdManager motdManager = new MotdManager(this);
        Bukkit.getPluginManager().registerEvents(motdManager, this);

        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        getCommand("rank").setExecutor(new RankCommand(this));
        getCommand("rank").setTabCompleter(new RankCommand(this));

        // Initial Tab Update
        Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p));

        // LuckPerms Event -> sofortiges Update bei Rank Änderung
        luckPerms.getEventBus().subscribe(this, UserDataRecalculateEvent.class, event -> {
            Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p));
        });

        // Backup: alle 2 Sekunden (falls irgendwas nicht triggert)
        Bukkit.getScheduler().runTaskTimer(this, () ->
                Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p)),
        40L, 40L);

        getLogger().info("RankSystem erfolgreich gestartet!");
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

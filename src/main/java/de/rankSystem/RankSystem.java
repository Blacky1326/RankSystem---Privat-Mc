package de.rankSystem;

import de.rankSystem.commands.RankCommand;
import de.rankSystem.listeners.ChatListener;
import de.rankSystem.listeners.PlayerJoinListener;
import de.rankSystem.managers.RankManager;
import de.rankSystem.managers.TabManager;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

public class RankSystem extends JavaPlugin {

    private static RankSystem instance;
    private LuckPerms luckPerms;
    private RankManager rankManager;
    private TabManager tabManager;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms nicht gefunden! Plugin wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        rankManager = new RankManager(this);
        tabManager = new TabManager(this);

        // Setup LuckPerms groups
        rankManager.setupLuckPermsGroups();

        // Register listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

        // Register commands
        getCommand("rank").setExecutor(new RankCommand(this));
        getCommand("rank").setTabCompleter(new RankCommand(this));

        // Update tab for all online players
        Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p));

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
}

package de.rankSystem;

import de.rankSystem.commands.*;
import de.rankSystem.listeners.ChatListener;
import de.rankSystem.listeners.ModerationListener;
import de.rankSystem.listeners.PlayerJoinListener;
import de.rankSystem.listeners.VanishListener;
import de.rankSystem.managers.*;
import net.luckperms.api.LuckPerms;
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
    private ModerationManager moderationManager;
    private VanishManager vanishManager;
    private TeleportManager teleportManager;
    private StaffChatCommand staffChatCommand;

    @Override
    public void onEnable() {
        instance = this;

        if (!setupLuckPerms()) {
            getLogger().severe("LuckPerms nicht gefunden! Plugin wird deaktiviert.");
            Bukkit.getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        configManager    = new ConfigManager(this);
        rankManager      = new RankManager(this);
        vanishManager    = new VanishManager(this);
        teleportManager  = new TeleportManager(this);
        tabManager       = new TabManager(this);
        moderationManager = new ModerationManager(this);

        rankManager.setupLuckPermsGroups();

        // ActionBar & MOTD
        actionBarManager = new ActionBarManager(this);
        actionBarManager.start();

        MotdManager motdManager = new MotdManager(this);
        Bukkit.getPluginManager().registerEvents(motdManager, this);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ModerationListener(this), this);
        Bukkit.getPluginManager().registerEvents(new VanishListener(this), this);

        // Commands
        getCommand("rank").setExecutor(new RankCommand(this));
        getCommand("rank").setTabCompleter(new RankCommand(this));

        WarnCommand warnCmd = new WarnCommand(this);
        getCommand("warn").setExecutor(warnCmd);
        getCommand("warn").setTabCompleter(warnCmd);
        getCommand("warnings").setExecutor(warnCmd);
        getCommand("warnings").setTabCompleter(warnCmd);

        MuteCommand muteCmd = new MuteCommand(this);
        getCommand("mute").setExecutor(muteCmd);
        getCommand("mute").setTabCompleter(muteCmd);
        getCommand("unmute").setExecutor(muteCmd);
        getCommand("unmute").setTabCompleter(muteCmd);

        TempBanCommand tempBanCmd = new TempBanCommand(this);
        getCommand("tempban").setExecutor(tempBanCmd);
        getCommand("tempban").setTabCompleter(tempBanCmd);

        UnbanCommand unbanCmd = new UnbanCommand(this);
        getCommand("unban").setExecutor(unbanCmd);
        getCommand("unban").setTabCompleter(unbanCmd);

        staffChatCommand = new StaffChatCommand(this);
        getCommand("sc").setExecutor(staffChatCommand);
        getCommand("sc").setTabCompleter(staffChatCommand);
        getCommand("staffchat").setExecutor(staffChatCommand);
        getCommand("staffchat").setTabCompleter(staffChatCommand);

        VanishCommand vanishCmd = new VanishCommand(this);
        getCommand("vanish").setExecutor(vanishCmd);
        getCommand("vanish").setTabCompleter(vanishCmd);

        TeleportCommand tpCmd     = new TeleportCommand(this, "tp");
        TeleportCommand tpaCmd    = new TeleportCommand(this, "tpa");
        TeleportCommand tpAccept  = new TeleportCommand(this, "tpaccept");
        TeleportCommand tpDeny    = new TeleportCommand(this, "tpdeny");

        getCommand("tp").setExecutor(tpCmd);
        getCommand("tp").setTabCompleter(tpCmd);
        getCommand("tpa").setExecutor(tpaCmd);
        getCommand("tpa").setTabCompleter(tpaCmd);
        getCommand("tpaccept").setExecutor(tpAccept);
        getCommand("tpdeny").setExecutor(tpDeny);

        Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p));

        // Auto-Update Tab alle 2 Sekunden
        Bukkit.getScheduler().runTaskTimer(this, () ->
                Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p)),
                40L, 40L);

        getLogger().info("RankSystem erfolgreich gestartet!");
    }

    @Override
    public void onDisable() {
        if (vanishManager != null)   vanishManager.removeAll();
        if (teleportManager != null) teleportManager.cleanup();
        getLogger().info("RankSystem wurde deaktiviert.");
    }

    private boolean setupLuckPerms() {
        RegisteredServiceProvider<LuckPerms> provider =
                Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        if (provider == null) return false;
        luckPerms = provider.getProvider();
        return true;
    }

    public static RankSystem getInstance()          { return instance; }
    public LuckPerms getLuckPerms()                 { return luckPerms; }
    public RankManager getRankManager()             { return rankManager; }
    public TabManager getTabManager()               { return tabManager; }
    public ConfigManager getConfigManager()         { return configManager; }
    public ActionBarManager getActionBarManager()   { return actionBarManager; }
    public ModerationManager getModerationManager() { return moderationManager; }
    public VanishManager getVanishManager()         { return vanishManager; }
    public TeleportManager getTeleportManager()     { return teleportManager; }
    public StaffChatCommand getStaffChatCommand()   { return staffChatCommand; }
}

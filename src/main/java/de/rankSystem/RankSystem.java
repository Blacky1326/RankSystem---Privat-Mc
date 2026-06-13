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
    rankManager   = new RankManager(this);
    tabManager    = new TabManager(this);

    rankManager.setupLuckPermsGroups();

    Bukkit.getPluginManager().registerEvents(new MotdManager(this), this);
    Bukkit.getPluginManager().registerEvents(new PlayerJoinListener(this), this);
    Bukkit.getPluginManager().registerEvents(new ChatListener(this), this);

    if (getCommand("rank") != null) {
        RankCommand cmd = new RankCommand(this);
        getCommand("rank").setExecutor(cmd);
        getCommand("rank").setTabCompleter(cmd);
    }

    // initial update
    Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p));

    // LuckPerms event (OPTIMIZED)
    luckPerms.getEventBus().subscribe(this, UserDataRecalculateEvent.class, event -> {

        Player player = Bukkit.getPlayer(event.getUser().getUniqueId());
        if (player != null) {
            tabManager.updatePlayer(player);
        }
    });

    // backup fallback (LOW FREQUENCY)
    Bukkit.getScheduler().runTaskTimer(this, () ->
            Bukkit.getOnlinePlayers().forEach(p -> tabManager.updatePlayer(p)),
    1200L, 1200L);

    // ActionBar delayed start
    Bukkit.getScheduler().runTaskLater(this, () -> {
        actionBarManager = new ActionBarManager(this);
        actionBarManager.start();
    }, 20L);

    getLogger().info("RankSystem gestartet!");
}

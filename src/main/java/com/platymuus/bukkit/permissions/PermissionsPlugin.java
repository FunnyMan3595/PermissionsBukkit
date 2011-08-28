package com.platymuus.bukkit.permissions;

import java.io.*;
import java.util.*;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Priority;
import org.bukkit.event.Event.Type;
import org.bukkit.permissions.PermissionAttachment;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.config.ConfigurationNode;

import com.platymuus.bukkit.permissions.data.DataAccessException;
import com.platymuus.bukkit.permissions.data.PermissionsData;
import com.platymuus.bukkit.permissions.data.SQLData;
import com.platymuus.bukkit.permissions.data.YMLData;

/**
 * Main class for PermissionsBukkit.
 */
public class PermissionsPlugin extends JavaPlugin {

    private BlockListener blockListener = new BlockListener(this);
    private PlayerListener playerListener = new PlayerListener(this);
    private PermissionsCommand commandExecutor = new PermissionsCommand(this);
    private HashMap<String, PermissionAttachment> permissions = new HashMap<String, PermissionAttachment>();
    private PermissionsData data;

    public final int SECONDS=20; // Server ticks per second.
    public final int MINUTES=SECONDS*60;

    // -- Basic stuff
    @Override
    public void onEnable() {
        // Write some default configuration
        if (!new File(getDataFolder(), "config.yml").exists()) {
            getServer().getLogger().info("Generating default configuration");
            writeDefaultConfiguration();
        }

        try {
            data = new SQLData(getConfiguration());
            int task_id = getServer().getScheduler().scheduleSyncRepeatingTask(
                                       this, data, 1 * MINUTES, 1 * MINUTES);

            if (task_id == -1) {
                throw new RuntimeException("Unable to schedule data heartbeat!");
            }
        } catch (DataAccessException e) {
            throw new RuntimeException("Unable to load PermissionsBukkit data!", e);
        }

        // Commands
        getCommand("permissions").setExecutor(commandExecutor);

        // Events
        PluginManager pm = getServer().getPluginManager();
        pm.registerEvent(Type.PLAYER_JOIN, playerListener, Priority.Lowest, this);
        pm.registerEvent(Type.PLAYER_QUIT, playerListener, Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_KICK, playerListener, Priority.Monitor, this);
        pm.registerEvent(Type.PLAYER_INTERACT, playerListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_BREAK, blockListener, Priority.Normal, this);
        pm.registerEvent(Type.BLOCK_PLACE, blockListener, Priority.Normal, this);

        // Register everyone online right now
        for (Player p : getServer().getOnlinePlayers()) {
            registerPlayer(p);
        }

        // How are you gentlemen
        getServer().getLogger().info(getDescription().getFullName() + " is now enabled");
    }

    @Override
    public void onDisable() {
        // Unregister everyone
        //for (Player p : getServer().getOnlinePlayers()) {
        //    unregisterPlayer(p);
        //}

        // Good day to you! I said good day!
        getServer().getLogger().info(getDescription().getFullName() + " is now disabled");
    }

    // -- External API
    /**
     * Get the group with the given name.
     * @param groupName The name of the group.
     * @return A Group if it exists or null otherwise.
     */
    public Group getGroup(String groupName) throws DataAccessException {
        if (data.groupExists(groupName)) {
            return new Group(this, groupName);
        }
        return null;
    }

    /**
     * Returns a list of groups a player is in.
     * @param playerName The name of the player.
     * @return The groups this player is in. May be empty.
     */
    public List<Group> getGroups(String playerName) throws DataAccessException {
        ArrayList<Group> result = new ArrayList<Group>();
        for (String group : data.getEffectiveGroupMembership(playerName)) {
            result.add(new Group(this, group));
        }
        return result;
    }

    /**
     * Returns permission info on the given player.
     * @param playerName The name of the player.
     * @return A PermissionsInfo about this player.
     */
    public PermissionInfo getPlayerInfo(String playerName) throws DataAccessException {
        if (!data.userExists(playerName)) {
            return null;
        } else {
            return new PermissionInfo(this, playerName, "user");
        }
    }

    /**
     * Returns a list of all defined groups.
     * @return The list of groups.
     */
    public List<Group> getAllGroups() throws DataAccessException {
        ArrayList<Group> result = new ArrayList<Group>();
        for (String group : data.getGroups()) {
            result.add(new Group(this, group));
        }
        return result;
    }

    public Map<String, Boolean> calculatePlayerPermissions(String player, String world) throws DataAccessException {
        Map<String, Boolean> perms = new HashMap<String, Boolean>();
        Set<String> groups = data.getEffectiveGroupMembership(player);


        // Note: We could simplify this as:
        // perms.putAll(data.getFullUserPermissions(player, ""));
        // perms.putAll(data.getFullUserPermissions(player, world));
        // BUT, this would cause world-specific group permissions to override
        // non-world-specific user permissions.
        // As user permissions exist to handle special cases not covered by
        // the groups, it makes more sense as written.

        // Least-priority: Non-world-specific group permissions.
        perms.putAll(data.getInheritedUserPermissions(player, ""));

        // Low-priority: World-specific group permissions.
        perms.putAll(data.getInheritedUserPermissions(player, world));

        // Medium-priority: Non-world-specific user permissions.
        perms.putAll(data.getUserPermissions(player, ""));

        // High-priority: World-specific user permissions.
        perms.putAll(data.getUserPermissions(player, world));

        return perms;
    }

    public PermissionsData getData() {
        return data;
    }

    public void refreshPermissions() {
        getConfiguration().save();
        for (String player : permissions.keySet()) {
            PermissionAttachment attachment = permissions.get(player);
            for (String key : attachment.getPermissions().keySet()) {
                attachment.unsetPermission(key);
            }

            try {
                calculateAttachment(getServer().getPlayer(player));
            } catch (DataAccessException e) {
                getServer().getLogger().warning("Unable to calculate permissions for " + player + ".  Using default permissions.");
            }
        }
    }

    // -- Plugin stuff
    protected void registerPlayer(Player player) {
        PermissionAttachment attachment = player.addAttachment(this);
        permissions.put(player.getName(), attachment);
        try {
            calculateAttachment(player);
        } catch (DataAccessException e) {
            getServer().getLogger().warning("Unable to calculate permissions for " + player + ".  Using default permissions.");
        }
    }

    protected void unregisterPlayer(Player player) {
        player.removeAttachment(permissions.get(player.getName()));
        permissions.remove(player.getName());
    }

    // Internal use

    protected void logError(Exception e) {
        StringWriter sw = new StringWriter();
        e.printStackTrace(new PrintWriter(sw));
        getServer().getLogger().warning(sw.toString());
    }

    // -- Private stuff
    
    private void calculateAttachment(Player player) throws DataAccessException {
        if (player == null) return;
        PermissionAttachment attachment = permissions.get(player.getName());

        for (Map.Entry<String, Boolean> entry : calculatePlayerPermissions(player.getName(), player.getWorld().getName()).entrySet()) {
            if (entry.getValue() != null) {
                attachment.setPermission(entry.getKey(), (Boolean) entry.getValue());
            } else {
                getServer().getLogger().warning("[PermissionsBukkit] Node " + entry.getKey() + " for player " + player.getName() + " is null.");
            }
        }

        player.recalculatePermissions();
    }

    private void writeDefaultConfiguration() {
        HashMap<String, Object> sql = new HashMap<String, Object>();
        HashMap<String, Object> messages = new HashMap<String, Object>();
        HashMap<String, Object> users = new HashMap<String, Object>();
        HashMap<String, Object> user = new HashMap<String, Object>();
        HashMap<String, Object> user_permissions = new HashMap<String, Object>();
        ArrayList<String> user_groups = new ArrayList<String>();

        HashMap<String, Object> groups = new HashMap<String, Object>();
        HashMap<String, Object> group_default = new HashMap<String, Object>();
        HashMap<String, Object> group_default_permissions = new HashMap<String, Object>();

        HashMap<String, Object> group_user = new HashMap<String, Object>();
        ArrayList<String> group_user_inheritance = new ArrayList<String>();
        HashMap<String, Object> group_user_permissions = new HashMap<String, Object>();
        HashMap<String, Object> group_user_worlds = new HashMap<String, Object>();
        HashMap<String, Object> group_user_worlds_creative = new HashMap<String, Object>();

        HashMap<String, Object> group_admin = new HashMap<String, Object>();
        ArrayList<String> group_admin_inheritance = new ArrayList<String>();
        HashMap<String, Object> group_admin_permissions = new HashMap<String, Object>();

        sql.put("enabled", false);
        sql.put("dbms", "MYSQL");
        sql.put("uri", "jdbc:mysql://localhost:3306/permissionsbukkit");
        sql.put("username", "exampleuser");
        sql.put("password", "examplepass");
        
        messages.put("build", "&cYou do not have permission to build here.");

        user_permissions.put("permissions.example", true);
        user_groups.add("admin");
        user.put("permissions", user_permissions);
        user.put("groups", user_groups);
        users.put("ConspiracyWizard", user);

        group_default_permissions.put("permissions.build", false);
        group_default.put("permissions", group_default_permissions);

        group_user_inheritance.add("default");
        group_user_permissions.put("permissions.build", true);
        group_user_worlds_creative.put("coolplugin.item", true);
        group_user_worlds.put("creative", group_user_worlds_creative);
        group_user.put("inheritance", group_user_inheritance);
        group_user.put("permissions", group_user_permissions);
        group_user.put("worlds", group_user_worlds);

        group_admin_inheritance.add("user");
        group_admin_permissions.put("permissions.*", true);
        group_admin.put("inheritance", group_admin_inheritance);
        group_admin.put("permissions", group_admin_permissions);

        groups.put("default", group_default);
        groups.put("user", group_user);
        groups.put("admin", group_admin);

        getConfiguration().setProperty("sql", sql);
        getConfiguration().setProperty("messages", messages);
        getConfiguration().setProperty("users", users);
        getConfiguration().setProperty("groups", groups);

        getConfiguration().setHeader(
            "# PermissionsBukkit configuration file",
            "# ",
            "# A permission node is a string like 'permissions.build', usually starting",
            "# with the name of the plugin. Refer to a plugin's documentation for what",
            "# permissions it cares about. Each node should be followed by true to grant",
            "# that permission or false to revoke it, as in 'permissions.build: true'.",
            "# Some plugins provide permission nodes that map to a group of permissions -",
            "# for example, PermissionsBukkit has 'permissions.*', which automatically",
            "# grants all admin permissions. You can also specify false for permissions",
            "# of this type.",
            "# ",
            "# Users inherit permissions from the groups they are a part of. If a user is",
            "# not specified here, or does not have a 'groups' node, they will be in the",
            "# group 'default'. Permissions for individual users may also be specified by",
            "# using a 'permissions' node with a list of permission nodes, which will",
            "# override their group permissions. World permissions may be assigned to",
            "# users with a 'worlds:' entry.",
            "# ",
            "# Groups can be assigned to players and all their permissions will also be",
            "# assigned to those players. Groups can also inherit permissions from other",
            "# groups. Like user permissions, groups may override the permissions of their",
            "# parent group(s). Unlike users, groups do NOT automatically inherit from",
            "# default. World permissions may be assigned to groups with a 'worlds:' entry.",
            "#",
            "# The cannot-build message is configurable. If it is left blank, no message",
            "# will be displayed to the player if PermissionsBukkit prevents them from",
            "# building, digging, or interacting with a block. Use '&' characters to",
            "# signify color codes.",
            "#",
            "# If you set the 'enabled:' property of 'sql:' to true, the 'users:' and",
            "# 'groups:' sections will be ignored, in favor of the SQL database specified.",
            "");
        getConfiguration().save();
    }

}

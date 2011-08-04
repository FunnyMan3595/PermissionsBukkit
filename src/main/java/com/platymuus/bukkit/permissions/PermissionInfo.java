package com.platymuus.bukkit.permissions;

import java.util.*;
import org.bukkit.util.config.ConfigurationNode;
import com.platymuus.bukkit.permissions.data.PermissionsData;
import com.platymuus.bukkit.permissions.data.DataAccessException;

/**
 * A class representing the global and world nodes attached to a player or group.
 */
public class PermissionInfo {
    
    private final PermissionsPlugin plugin;
    private final String name;
    private final String groupType;
    
    protected PermissionInfo(PermissionsPlugin plugin, String name, String groupType) {
        this.plugin = plugin;
        this.name = name;
        this.groupType = groupType;
    }
    
    /**
     * Gets the list of groups this group/player inherits permissions from.
     * @return The list of groups.
     */
    public List<Group> getGroups() throws DataAccessException {
        PermissionsData data = plugin.getData();

        HashSet<String> names;
        if (groupType.equals("user")) {
            names = data.getGroupMembership(name);
        } else {
            names = data.getGroupParents(name);
        }

        ArrayList<Group> result = new ArrayList<Group>();
        for (String name : names) {
            result.add(new Group(plugin, name));
        }

        return result;
    }
    
    /**
     * Gets a map of non-world-specific permission nodes to boolean values that this group/player defines.
     * @return The map of permissions.
     */
    public Map<String, Boolean> getPermissions() throws DataAccessException {
        PermissionsData data = plugin.getData();

        if (groupType.equals("user")) {
            return data.getUserPermissions(name, "");
        } else {
            return data.getGroupPermissions(name, "");
        }
    }
    
    /**
     * Gets a list of worlds this group/player defines world-specific permissions for.
     */
    public List<String> getWorlds() throws DataAccessException {
        PermissionsData data = plugin.getData();

        if (groupType.equals("user")) {
            return new ArrayList<String>(data.getUserWorlds(name));
        } else {
            return new ArrayList<String>(data.getGroupWorlds(name));
        }
    }
    
    /**
     * Gets a map of world-specific permission nodes to boolean values that this group/player defines.
     * @return The map of permissions.
     */
    public Map<String, Boolean> getWorldPermissions(String world) throws DataAccessException {
        PermissionsData data = plugin.getData();

        if (groupType.equals("user")) {
            return data.getUserPermissions(name, world);
        } else {
            return data.getGroupPermissions(name, world);
        }
    }
    
}

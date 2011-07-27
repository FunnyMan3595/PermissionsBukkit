package com.platymuus.bukkit.permissions.data;

import java.util.*;
import org.bukkit.util.config.Configuration;

public class YMLData {
    public YMLData(Configuration config) throws DataAccessException {
        super(config);
    }

    @SuppressWarnings("unchecked")
    protected HashMap<String, Object> getNodeMatches(String path) {
        HashMap<String, Object> nodes = new HashMap<String, Object>();
        nodes.put(null, config);

        for (String piece : path.split(".")) {
            HashMap<String, Object> new_nodes = new HashMap<String, Object>();
            if (piece.startsWith("%")) {
                String match = piece.substring(1);
                for (String key : nodes.keySet()) {
                    ConfigurationNode node = new ConfigurationNode((Map<String, Object>)nodes.get(key));
                    for (String childname : node.getKeys()) {
                        if (childname.equalsIgnoreCase(match)) {
                            new_nodes.put(key, node.getProperty(childname));
                            break;
                        }
                    }
                }
            } else if (piece.equals("?")) {
                ConfigurationNode node = new ConfigurationNode((Map<String, Object>)nodes.get(null));
                for (String childname : node.getKeys()) {
                    new_nodes.put(childname, node.getProperty(childname));
                }
            } else {
                for (String key : nodes.keySet()) {
                    ConfigurationNode node = new ConfigurationNode((Map<String, Object>)nodes.get(key));
                    new_nodes.put(key, node.getProperty(piece));
                }
            }

            nodes = new_nodes;
        }

        return nodes;
    }

    protected HashSet<String> getMatches(String path) {
        return new HashSet<String>(getNodeMatches(path).keySet());
    }

    @SuppressWarnings("unchecked")
    protected HashMap<String, Boolean> getMatchValues(String path) {
        HashMap<String, Boolean> values = new HashMap<String, Boolean>();

        HashMap<String, Object> raw_values = getNodeMatches(path);
        for (key : raw_values.keySet()) {
            values.put(key, (Boolean) raw_values.get(key));
        }

        return values;
    }

    public boolean userExists(String user) throws DataAccessException {
        return getNodeMatches("users.%" + user).size() > 0;
    }

    public boolean groupExists(String group) throws DataAccessException {
        return getNodeMatches("groups.%" + group).size() > 0;
    }

    public HashSet<String> getUsers() throws DataAccessException {
        return getMatches("users.?");
    }

    public HashSet<String> getGroups() throws DataAccessException {
        return getMatches("groups.?");
    }

    public String getDefaultGroup() throws DataAccessException {
        return "default";
    }

    // Note: For the default group, groupless users need not be included.
    public HashSet<String> getGroupMembers(String group) throws DataAccessException {
        return getMatches("users.?.groups.%" + group);
    }

    public HashSet<String> getGroupMembership(String user) throws DataAccessException {
        return getMatches("users.%" + user + ".groups.?");
    }

    public HashSet<String> getGroupParents(String group) throws DataAccessException {
        return getMatches("groups.%" + group + ".inheritance.?");
    }

    public HashSet<String> getGroupChildren(String group) throws DataAccessException {
        return getMatches("groups.?.inheritance.%" + group);
    }

    public HashMap<String,Boolean> getUserPermissions(String user, String world) throws DataAccessException {
        return getMatchValues("users.%" + user + ".permissions.?");
    }

    public HashMap<String,Boolean> getGroupPermissions(String group, String world) throws DataAccessException {
        return getMatchValues("groups.%" + group + ".permissions.?");
    }


    // These return true if a change was made.
    public boolean removeUser(String group) throws DataAccessException {
        HashMap<String> matches = getMatches("users.%" + user);
        for (match : matches) {
            config.removeProperty("users." + match);
        }
        return matches.size() > 0;
    }

    // Throws IllegalArgumentException if you try to remove the default group without even_if_default=true.
    public boolean removeGroup(String group, boolean even_if_default) throws DataAccessException {
        if (group.equalsIgnoreCase("default") && !even_if_default) {
            throw IllegalArgumentException("That's the default group!");
        }

        HashMap<String> matches = getMatches("groups.%" + group);
        for (match : matches) {
            config.removeProperty("groups." + match);
        }
        return matches.size() > 0;
    }

    public boolean joinGroup(String player, String group) throws DataAccessException {
    }

    public boolean leaveGroup(String player, String group) throws DataAccessException;
    public boolean addGroupParent(String child, String parent) throws DataAccessException;
    public boolean removeGroupParent(String child, String parent) throws DataAccessException;

    // These return the old value, null for not set.
    public Boolean addGroupPermission(String group, String world, String permission, Boolean value) throws DataAccessException;
    public Boolean removeGroupPermission(String group, String world, String permission) throws DataAccessException;
    public Boolean addUserPermission(String user, String world, String permission, Boolean value) throws DataAccessException;
    public Boolean removeUserPermission(String user, String world, String permission) throws DataAccessException;
}

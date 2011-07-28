package com.platymuus.bukkit.permissions.data;

import java.util.*;
import org.bukkit.util.config.Configuration;
import org.bukkit.util.config.ConfigurationNode;

public class YMLData extends PermissionsData {
    public YMLData(Configuration config) throws DataAccessException {
        super(config);
    }

    @SuppressWarnings("unchecked")
    protected HashMap<String, String> getNodeMatches(String path) {
        HashMap<String, String> nodes = new HashMap<String, String>();
        nodes.put(null, "");

        for (String piece : path.split(".")) {
            HashMap<String, String> new_nodes = new HashMap<String, String>();
            if (piece.startsWith("%")) {
                String match = piece.substring(1);
                for (String key : nodes.keySet()) {
                    String node_name = nodes.get(key);
                    ConfigurationNode node = config.getNode(node_name);
                    for (String childname : node.getKeys()) {
                        if (childname.equalsIgnoreCase(match)) {
                            new_nodes.put(key, node_name + "." + childname);
                            break;
                        }
                    }
                }
            } else if (piece.equals("?")) {
                String node_name = nodes.get(null);
                ConfigurationNode node = config.getNode(node_name);
                for (String childname : node.getKeys()) {
                    new_nodes.put(childname, node_name + "." + childname);
                }
            } else {
                for (String key : nodes.keySet()) {
                    String node_name = nodes.get(key);
                    ConfigurationNode node = config.getNode(node_name);
                    new_nodes.put(key, node_name + "." + key);
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

        HashMap<String, String> matches = getNodeMatches(path);
        for (String key : matches.keySet()) {
            values.put(key, config.getBoolean(matches.get(key), false));
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
    public boolean removeUser(String user) throws DataAccessException {
        HashSet<String> matches = getMatches("users.%" + user);
        for (String match : matches) {
            config.removeProperty("users." + match);
        }
        return matches.size() > 0;
    }

    // Throws IllegalArgumentException if you try to remove the default group without even_if_default=true.
    public boolean removeGroup(String group, boolean even_if_default) throws DataAccessException {
        if (group.equalsIgnoreCase("default") && !even_if_default) {
            throw new IllegalArgumentException("That's the default group!");
        }

        HashSet<String> matches = getMatches("groups.%" + group);
        for (String match : matches) {
            config.removeProperty("groups." + match);
        }
        return matches.size() > 0;
    }

    public boolean joinGroup(String player, String group) throws DataAccessException {
        throw new UnsupportedOperationException();
    }

    public boolean leaveGroup(String player, String group) throws DataAccessException {
        throw new UnsupportedOperationException();
    }
    public boolean addGroupParent(String child, String parent) throws DataAccessException {
        throw new UnsupportedOperationException();
    }
    public boolean removeGroupParent(String child, String parent) throws DataAccessException {
        throw new UnsupportedOperationException();
    }

    // These return the old value, null for not set.
    public Boolean addGroupPermission(String group, String world, String permission, Boolean value) throws DataAccessException {
        throw new UnsupportedOperationException();
    }
    public Boolean removeGroupPermission(String group, String world, String permission) throws DataAccessException {
        throw new UnsupportedOperationException();
    }
    public Boolean addUserPermission(String user, String world, String permission, Boolean value) throws DataAccessException {
        throw new UnsupportedOperationException();
    }
    public Boolean removeUserPermission(String user, String world, String permission) throws DataAccessException {
        throw new UnsupportedOperationException();
    }
}

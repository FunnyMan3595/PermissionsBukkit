package com.platymuus.bukkit.permissions.data;

import java.util.*;
import org.bukkit.util.config.Configuration;

public abstract class PermissionsData implements Runnable {
    protected Configuration config;

    public PermissionsData(Configuration config) throws DataAccessException {
        this.config = config;
    }

    public abstract boolean userExists(String user) throws DataAccessException;
    public abstract boolean groupExists(String group) throws DataAccessException;
    public abstract HashSet<String> getUsers() throws DataAccessException;
    public abstract HashSet<String> getGroups() throws DataAccessException;
    public abstract String getDefaultGroup() throws DataAccessException;
    // Note: For the default group, groupless users need not be included.
    public abstract HashSet<String> getGroupMembers(String group) throws DataAccessException;

    public HashSet<String> getIndirectGroupMembers(String group) throws DataAccessException {
        HashSet<String> users = new HashSet<String>();
        HashSet<String> subgroups = getGroupChildren(group);
        for (String subgroup : subgroups) {
            users.addAll(getFullGroupMembers(subgroup));
        }
        return users;
    }

    public HashSet<String> getFullGroupMembers(String group) throws DataAccessException {
        HashSet<String> users = getGroupMembers(group);
        users.addAll(getIndirectGroupMembers(group));
        return users;
    }

    public abstract HashSet<String> getGroupMembership(String user) throws DataAccessException;

    public HashSet<String> getEffectiveGroupMembership(String user) throws DataAccessException {
        HashSet<String> groups = getGroupMembership(user);
        if (groups.size() == 0) {
            String default_group = getDefaultGroup();
            if (default_group != null) {
                groups.add(default_group);
            }
        }
        return groups;
    }

    public abstract HashSet<String> getGroupParents(String group) throws DataAccessException;
    public abstract HashSet<String> getGroupChildren(String group) throws DataAccessException;
    public abstract HashMap<String,Boolean> getUserPermissions(String user, String world) throws DataAccessException;
    public abstract HashMap<String,Boolean> getGroupPermissions(String group, String world) throws DataAccessException;
    public abstract HashSet<String> getUserWorlds(String user) throws DataAccessException;
    public abstract HashSet<String> getGroupWorlds(String group) throws DataAccessException;

    public HashMap<String, Boolean> getInheritedUserPermissions(String user, String world) throws DataAccessException {
        HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();

        for (String group : getEffectiveGroupMembership(user)) {
            permissions.putAll(getFullGroupPermissions(group, world));
        }

        return permissions;
    }

    public HashMap<String, Boolean> getInheritedGroupPermissions(String group, String world) throws DataAccessException {
        HashMap<String, Boolean> permissions = new HashMap<String, Boolean>();

        for (String parent : getGroupParents(group)) {
            permissions.putAll(getFullGroupPermissions(parent, world));
        }

        return permissions;
    }

    public HashMap<String, Boolean> getFullUserPermissions(String user, String world) throws DataAccessException {
        HashMap<String, Boolean> permissions = getInheritedUserPermissions(user, world);
        permissions.putAll(getUserPermissions(user, world));
        return permissions;
    }

    public HashMap<String, Boolean> getFullGroupPermissions(String group, String world) throws DataAccessException {
        HashMap<String, Boolean> permissions = getInheritedGroupPermissions(group, world);
        permissions.putAll(getGroupPermissions(group, world));
        return permissions;
    }

    // These return true if a change was made.
    public abstract boolean removeUser(String group) throws DataAccessException;
    public abstract boolean removeGroup(String group, boolean even_if_default) throws DataAccessException; // Throws IllegalArgumentException if you try to remove the default group without even_if_default=true.
    public abstract boolean joinGroup(String player, String group) throws DataAccessException;
    public abstract boolean leaveGroup(String player, String group) throws DataAccessException;
    public abstract boolean addGroupParent(String child, String parent) throws DataAccessException;
    public abstract boolean removeGroupParent(String child, String parent) throws DataAccessException;

    // These return the old value, null for not set.
    public abstract Boolean addGroupPermission(String group, String world, String permission, Boolean value) throws DataAccessException;
    public abstract Boolean removeGroupPermission(String group, String world, String permission) throws DataAccessException;
    public abstract Boolean addUserPermission(String user, String world, String permission, Boolean value) throws DataAccessException;
    public abstract Boolean removeUserPermission(String user, String world, String permission) throws DataAccessException;


    // This is run once a minute, to allow the data object to maintain
    // and/or re-establish connections.
    public void run() {
    }
}

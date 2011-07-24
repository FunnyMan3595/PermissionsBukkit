package com.platymuus.bukkit.permissions.data;

import java.util.*;
import org.bukkit.util.config.Configuration;

public class YMLData {
    public YMLData(Configuration config) throws DataAccessException {
    }

    public String getDefaultGroup() throws DataAccessException;
    public HashSet<String> getGroupMembership(String user) throws DataAccessException;

    public HashSet<String> getGroupParents(String group) throws DataAccessException;
    public HashSet<String> getGroupChildren(String group) throws DataAccessException;
    public HashMap<String,Boolean> getUserPermissions(String user, String world) throws DataAccessException;
    public HashMap<String,Boolean> getGroupPermissions(String group, String world) throws DataAccessException;

    // These return true if a change was made.
    public boolean removeUser(String group) throws DataAccessException;
    public boolean removeGroup(String group, boolean even_if_default) throws DataAccessException; // Throws IllegalArgumentException if you try to remove the default group without even_if_default=true.
    public boolean joinGroup(String player, String group) throws DataAccessException;
    public boolean leaveGroup(String player, String group) throws DataAccessException;
    public boolean addGroupParent(String child, String parent) throws DataAccessException;
    public boolean removeGroupParent(String child, String parent) throws DataAccessException;

    // These return the old value, null for not set.
    public Boolean addGroupPermission(String group, String world, String permission, Boolean value) throws DataAccessException;
    public Boolean removeGroupPermission(String group, String world, String permission) throws DataAccessException;
    public Boolean addUserPermission(String user, String world, String permission, Boolean value) throws DataAccessException;
    public Boolean removeUserPermission(String user, String world, String permission) throws DataAccessException;
}

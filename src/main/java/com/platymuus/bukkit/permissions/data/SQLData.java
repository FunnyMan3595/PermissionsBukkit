package com.platymuus.bukkit.permissions.data;

import com.mysql.jdbc.jdbc2.optional.MysqlDataSource;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.util.logging.Logger;
import org.bukkit.util.config.Configuration;

public class SQLData extends PermissionsData {
    public Logger log = Logger.getLogger("Minecraft");

    private boolean debug;
    private String url = null;
    private String user = null;
    private String password = null;

    protected DAEWrapper db;

    protected PreparedStatement set_default_group;
    protected PreparedStatement add_group;
    protected PreparedStatement add_user;
    protected PreparedStatement add_group_permission;
    protected PreparedStatement add_user_permission;
    protected PreparedStatement add_group_membership;
    protected PreparedStatement add_group_inheritance;

    protected PreparedStatement remove_group;
    protected PreparedStatement remove_user;
    protected PreparedStatement remove_group_permission;
    protected PreparedStatement remove_all_group_permissions;
    protected PreparedStatement remove_user_permission;
    protected PreparedStatement remove_all_user_permissions;
    protected PreparedStatement remove_group_membership;
    protected PreparedStatement remove_group_inheritance;
    protected PreparedStatement remove_all_user_groups;
    protected PreparedStatement remove_all_group_members;
    protected PreparedStatement remove_all_group_inheritance;

    protected PreparedStatement get_users;
    protected PreparedStatement get_groups;
    protected PreparedStatement get_default_group;
    protected PreparedStatement get_group_members;
    protected PreparedStatement get_group_id;
    protected PreparedStatement get_user_id;
    protected PreparedStatement get_group_permission;
    protected PreparedStatement get_user_permission;
    protected PreparedStatement get_group_permissions;
    protected PreparedStatement get_user_permissions;
    protected PreparedStatement get_group_worlds;
    protected PreparedStatement get_user_worlds;
    protected PreparedStatement get_user_groups;
    protected PreparedStatement get_group_parents;
    protected PreparedStatement get_group_children;

    public void close() {
        try {
            db.close();
        } catch (DataAccessException e) {
            log.severe("Unable to close database: " + e.getMessage());
        }

        set_default_group = null;
        add_group = null;
        add_user = null;
        add_group_permission = null;
        add_user_permission = null;
        add_group_membership = null;
        add_group_inheritance = null;

        remove_group = null;
        remove_user = null;
        remove_group_permission = null;
        remove_all_group_permissions = null;
        remove_user_permission = null;
        remove_all_user_permissions = null;
        remove_group_membership = null;
        remove_group_inheritance = null;
        remove_all_user_groups = null;
        remove_all_group_members = null;
        remove_all_group_inheritance = null;

        get_groups = null;
        get_users = null;
        get_default_group = null;
        get_group_members = null;
        get_group_id = null;
        get_user_id = null;
        get_group_permission = null;
        get_user_permission = null;
        get_group_permissions = null;
        get_user_permissions = null;
        get_group_worlds = null;
        get_user_worlds = null;
        get_user_groups = null;
        get_group_parents = null;
        get_group_children = null;
    }

    public SQLData(Configuration config) throws DataAccessException {
        super(config);

        debug = config.getBoolean("sql.debug", false);
        url = config.getString("sql.uri");
        user = config.getString("sql.username");
        password = config.getString("sql.password");
        String dbms = config.getString("sql.dbms");

        if (dbms!=null && !dbms.equalsIgnoreCase("MYSQL")) {
            throw new DataAccessException("Only MYSQL dbms is supported for now.");
        }

        if (user==null) {
            throw new DataAccessException("sql.username is null!");
        } else if (password==null) {
            throw new DataAccessException("sql.password is null!");
        } else if (url==null) {
            throw new DataAccessException("sql.uri is null!");
        }

        init();
    }

    public void init() throws DataAccessException {
        MysqlDataSource server = new MysqlDataSource();
        server.setUrl(url);
        server.setUser(user);
        server.setPassword(password);
        server.setUseServerPrepStmts(true);
        server.setCachePreparedStatements(true);
        server.setPreparedStatementCacheSize(50);

        db = new DAEWrapper(server, log, debug);

        Vector<String> setup = new Vector<String>();
        setup.add("CREATE TABLE IF NOT EXISTS Users (userid INT NOT NULL AUTO_INCREMENT, username VARCHAR(64) NOT NULL, PRIMARY KEY(userid), INDEX (username))");
        setup.add("CREATE TABLE IF NOT EXISTS Groups (groupid INT NOT NULL AUTO_INCREMENT, groupname VARCHAR(64) NOT NULL, is_default BOOLEAN NOT NULL, PRIMARY KEY(groupid), INDEX (groupname))");
        setup.add("CREATE TABLE IF NOT EXISTS GroupPermissions (groupid INT NOT NULL, world VARCHAR(64) NOT NULL, permission VARCHAR(64) NOT NULL, value BOOLEAN NOT NULL, PRIMARY KEY(groupid, world, permission), INDEX(groupid))");
        setup.add("CREATE TABLE IF NOT EXISTS UserPermissions (userid INT NOT NULL, world VARCHAR(64) NOT NULL, permission VARCHAR(64) NOT NULL, value BOOLEAN NOT NULL, PRIMARY KEY(userid, world, permission), INDEX(userid))");
        setup.add("CREATE TABLE IF NOT EXISTS GroupMembership (groupid INT NOT NULL, userid INT NOT NULL, PRIMARY KEY(groupid,userid), INDEX(groupid), INDEX(userid))");
        setup.add("CREATE TABLE IF NOT EXISTS GroupInheritance (parent INT NOT NULL, child INT NOT NULL, PRIMARY KEY(parent,child), INDEX(parent), INDEX(child))");
        db.initializeDatabase(setup);

        set_default_group = db.prepareStatement("UPDATE Groups SET is_default=(groupname=?)");
        add_group = db.prepareStatement("INSERT INTO Groups SET groupname=?, is_default=false");
        add_user = db.prepareStatement("INSERT INTO Users SET username=?");
        add_group_permission = db.prepareStatement("INSERT INTO GroupPermissions  SET groupid=(SELECT groupid FROM Groups WHERE groupname=?), world=?, permission=?, value=?");
        add_user_permission = db.prepareStatement("INSERT INTO UserPermissions SET userid=(SELECT userid FROM Users WHERE username=?), world=?, permission=?, value=?");
        add_group_membership = db.prepareStatement("INSERT IGNORE INTO GroupMembership SET groupid=(SELECT groupid FROM Groups WHERE groupname=?), userid=(SELECT userid FROM Users WHERE username=?)");
        add_group_inheritance = db.prepareStatement("INSERT IGNORE INTO GroupInheritance SET parent=(SELECT groupid FROM Groups where groupname=?), child=(SELECT groupid FROM Groups WHERE groupname=?)");

        remove_group = db.prepareStatement("DELETE FROM Groups WHERE groupname=?");
        remove_user = db.prepareStatement("DELETE FROM Users WHERE username=?");
        remove_group_permission = db.prepareStatement("DELETE FROM GroupPermissions USING GroupPermissions NATURAL JOIN Groups WHERE groupname=? AND world=? AND permission=?");
        remove_all_group_permissions = db.prepareStatement("DELETE FROM GroupPermissions USING GroupPermissions NATURAL JOIN Groups WHERE groupname=?");
        remove_user_permission = db.prepareStatement("DELETE FROM UserPermissions USING UserPermissions NATURAL JOIN Users WHERE username=? AND world=? AND permission=?");
        remove_all_user_permissions = db.prepareStatement("DELETE FROM GroupPermissions USING GroupPermissions NATURAL JOIN Groups WHERE username=?");
        remove_group_membership = db.prepareStatement("DELETE FROM GroupMembership USING Groups NATURAL JOIN (GroupMembership NATURAL JOIN Users) WHERE groupname=? AND username=?");
        remove_group_inheritance = db.prepareStatement("DELETE FROM Rel USING Groups as p INNER JOIN (GroupInheritance as Rel INNER JOIN Groups as c ON Rel.child=c.groupid) ON Rel.parent=p.groupid WHERE p.groupname=? AND c.groupname=?");
        remove_all_user_groups = db.prepareStatement("DELETE FROM GroupMembership USING GroupMembership NATURAL JOIN Users WHERE username=?");
        remove_all_group_members = db.prepareStatement("DELETE FROM GroupMembership USING Groups NATURAL JOIN GroupMembership WHERE groupname=?");
        remove_all_group_inheritance = db.prepareStatement("DELETE FROM Rel USING Groups INNER JOIN ON Rel.parent=groupid OR Rel.child=groupid WHERE groupname=?");

        get_users = db.prepareStatement("SELECT username FROM Users");
        get_groups = db.prepareStatement("SELECT groupname FROM Groups");
        get_default_group = db.prepareStatement("SELECT groupname FROM Groups WHERE is_default=true");
        get_group_members = db.prepareStatement("SELECT username FROM Groups NATURAL JOIN (GroupMembership NATURAL JOIN Users) WHERE groupname=?");
        get_user_id = db.prepareStatement("SELECT userid FROM Users WHERE username=?");
        get_group_id = db.prepareStatement("SELECT groupid FROM Groups WHERE groupname=?");
        get_group_permission = db.prepareStatement("SELECT value FROM GroupPermissions NATURAL JOIN Groups WHERE groupname=? AND world=? AND permission=?");
        get_user_permission = db.prepareStatement("SELECT value FROM UserPermissions NATURAL JOIN Users WHERE username=? AND world=? AND permission=?");
        get_group_permissions = db.prepareStatement("SELECT permission, value FROM GroupPermissions NATURAL JOIN Groups WHERE groupname=? AND world=?");
        get_user_permissions = db.prepareStatement("SELECT permission, value FROM UserPermissions NATURAL JOIN Users WHERE username=? AND world=?");
        get_group_worlds = db.prepareStatement("SELECT DISTINCT world FROM GroupPermissions NATURAL JOIN Groups WHERE groupname=?");
        get_user_worlds = db.prepareStatement("SELECT DISTINCT world FROM UserPermissions NATURAL JOIN Users WHERE username=?");
        get_user_groups = db.prepareStatement("SELECT groupname FROM Groups NATURAL JOIN (GroupMembership NATURAL JOIN Users) WHERE username=?");
        get_group_parents = db.prepareStatement("SELECT p.groupname FROM Groups as p INNER JOIN (GroupInheritance as Rel INNER JOIN Groups as c ON Rel.child=c.groupid) ON Rel.parent=p.groupid WHERE c.groupname=?");
        get_group_children = db.prepareStatement("SELECT c.groupname FROM Groups as p INNER JOIN (GroupInheritance as Rel INNER JOIN Groups as c ON Rel.child=c.groupid) ON Rel.parent=p.groupid WHERE p.groupname=?");
    }

    public Boolean getUserPermission(String user, String world, String permission) throws DataAccessException {
        return db.get(get_user_permission, user, world, permission);
    }

    public Boolean getGroupPermission(String group, String world, String permission) throws DataAccessException {
        return db.get(get_group_permission, group, world, permission);
    }

    // Returns true if the user needed to be created.
    public synchronized boolean createUser(String user) throws DataAccessException {
        if (getUserID(user) == null) {
            db.execute(add_user, user);
            return true;
        }
        return false;
    }

    // Returns true if the group needed to be created.
    public synchronized boolean createGroup(String group) throws DataAccessException {
        if (getGroupID(group) == null) {
            db.execute(add_group, group);
            return true;
        }
        return false;
    }

    // Returns null if no such user.
    public Integer getUserID(String user) throws DataAccessException {
        return db.get(get_user_id, user);
    }

    // Returns null if no such group.
    public Integer getGroupID(String group) throws DataAccessException {
        return db.get(get_group_id, group);
    }


    /*
     *  Implementations of abstract methods from PermissionsData.
     */
    public boolean userExists(String user) throws DataAccessException {
        return getUserID(user) != null;
    }

    public boolean groupExists(String group) throws DataAccessException {
        return getGroupID(group) != null;
    }

    public HashSet<String> getUsers() throws DataAccessException {
        return db.getHashSet(get_users);
    }

    public HashSet<String> getGroups() throws DataAccessException {
        return db.getHashSet(get_groups);
    }

    public String getDefaultGroup() throws DataAccessException {
        return db.get(get_default_group);
    }

    // Note: For the default group, groupless users need not be included.
    public HashSet<String> getGroupMembers(String group) throws DataAccessException {
        return db.getHashSet(get_group_members, group);
    }

    public HashSet<String> getGroupMembership(String user) throws DataAccessException {
        return db.getHashSet(get_user_groups, user);
    }

    public HashSet<String> getGroupParents(String group) throws DataAccessException {
        return db.getHashSet(get_group_parents, group);
    }

    public HashSet<String> getGroupChildren(String group) throws DataAccessException {
        return db.getHashSet(get_group_children, group);
    }

    public HashMap<String,Boolean> getUserPermissions(String user, String world) throws DataAccessException {
        return db.getHashMap(get_user_permissions, user, world);
    }

    public HashSet<String> getGroupWorlds(String group) throws DataAccessException {
        return db.getHashSet(get_group_worlds, group);
    }

    public HashSet<String> getUserWorlds(String user) throws DataAccessException {
        return db.getHashSet(get_user_worlds, user);
    }

    public HashMap<String,Boolean> getGroupPermissions(String group, String world) throws DataAccessException {
        return db.getHashMap(get_group_permissions, group, world);
    }

    // These return true if a change was made.
    public synchronized boolean removeUser(String user) throws DataAccessException {
        if (getUserID(user) != null) {
            for (String group : getGroupMembership(user)) {
                leaveGroup(user, group);
            }
            db.execute(remove_all_user_permissions, user);
            db.execute(remove_all_user_groups, user);
            db.execute(remove_user, user);

            return true;
        } else {
            return false;
        }
    }

    // Throws IllegalArgumentException if you try to remove the default group without even_if_default=true.
    public synchronized boolean removeGroup(String group, boolean even_if_default) throws DataAccessException {
        String default_group = getDefaultGroup();
        if (default_group != null && !even_if_default) {
            if (group.equalsIgnoreCase(default_group)) {
                throw new IllegalArgumentException("That's the default group.");
            }
        }

        if (getGroupID(group) != null) {
            for (String parent : getGroupParents(group)) {
                removeGroupParent(group, parent);
            }
            db.execute(remove_all_group_members, group);
            db.execute(remove_all_group_inheritance, group);
            db.execute(remove_all_group_permissions, group);
            db.execute(remove_group, group);

            return true;
        } else {
            return false;
        }
    }

    public synchronized boolean joinGroup(String player, String group) throws DataAccessException {
        createUser(player);
        createGroup(group);
        return db.execute(add_group_membership, group, player) > 0;
    }

    public boolean leaveGroup(String player, String group) throws DataAccessException {
        return db.execute(remove_group_membership, group, player) > 0;
    }

    public synchronized boolean addGroupParent(String child, String parent) throws DataAccessException {
        createGroup(parent);
        createGroup(child);
        return db.execute(add_group_inheritance, parent, child) > 0;
    }

    public boolean removeGroupParent(String child, String parent) throws DataAccessException {
        return db.execute(remove_group_inheritance, parent, child) > 0;
    }

    // These return the old value, null for not set.
    public synchronized Boolean addGroupPermission(String group, String world, String permission, Boolean value) throws DataAccessException {
        createGroup(group);
        Boolean old_value = getGroupPermission(group, world, permission);
        if (old_value != null) {
            db.execute(remove_group_permission, group, world, permission);
        }
        if (value != null) {
            db.execute(add_group_permission, group, world, permission, value);
        }
        return old_value;
    }

    public synchronized Boolean removeGroupPermission(String group, String world, String permission) throws DataAccessException {
        Boolean old_value = getGroupPermission(group, world, permission);
        if (old_value != null) {
            db.execute(remove_group_permission, group, world, permission);
        }
        return old_value;
    }

    public synchronized Boolean addUserPermission(String user, String world, String permission, Boolean value) throws DataAccessException {
        createUser(user);
        Boolean old_value = getUserPermission(user, world, permission);
        if (old_value != null) {
            db.execute(remove_user_permission, user, world, permission);
        }
        if (value != null) {
            db.execute(add_user_permission, user, world, permission, value);
        }
        return old_value;
    }

    public synchronized Boolean removeUserPermission(String user, String world, String permission) throws DataAccessException {
        Boolean old_value = getUserPermission(user, world, permission);
        if (old_value != null) {
            db.execute(remove_user_permission, user, world, permission);
        }
        return old_value;
    }

    public synchronized void run() {
        try {
            getDefaultGroup();
        } catch (DataAccessException e) {
            // Connection to the database seems to be lost.  Reconnect.
            try {
                init();
            } catch (DataAccessException e2) {
                log.severe("Unable to connect to database: " + e2.getCause().getMessage());
            }
        }
    }
}

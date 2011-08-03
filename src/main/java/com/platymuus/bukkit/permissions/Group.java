package com.platymuus.bukkit.permissions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.util.config.ConfigurationNode;

import com.platymuus.bukkit.permissions.data.PermissionsData;
import com.platymuus.bukkit.permissions.data.DataAccessException;

/**
 * A class representing a permissions group.
 */
public class Group {

    private PermissionsPlugin plugin;
    private String name;

    protected Group(PermissionsPlugin plugin, String name) {
        this.plugin = plugin;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public List<String> getPlayers() throws DataAccessException {
        PermissionsData data = plugin.getData();
        return new ArrayList<String>(data.getGroupMembers(name));
    }

    public List<Player> getOnlinePlayers() throws DataAccessException {
        PermissionsData data = plugin.getData();

        ArrayList<Player> result = new ArrayList<Player>();
        for (String user : data.getGroupMembers(name)) {
            Player player = Bukkit.getServer().getPlayer(user);
            if (player != null && player.isOnline()) {
                result.add(player);
            }
        }
        return result;
    }

    public PermissionInfo getInfo() throws DataAccessException {
        if (!plugin.getData().groupExists(name)) {
            return null;
        }
        return new PermissionInfo(plugin, name, "group");
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || !(o instanceof Group)) {
            return false;
        }
        return name.equalsIgnoreCase(((Group) o).getName());
    }

    @Override
    public String toString() {
        return "Group{name=" + name + "}";
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

}

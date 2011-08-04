package com.platymuus.bukkit.permissions;

import com.platymuus.bukkit.permissions.data.DataAccessException;
import com.platymuus.bukkit.permissions.data.PermissionsData;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.bukkit.permissions.Permission;
import org.bukkit.permissions.PermissionAttachmentInfo;

/**
 * CommandExecutor for /permissions
 */
class PermissionsCommand implements CommandExecutor {
    
    private PermissionsPlugin plugin;

    public PermissionsCommand(PermissionsPlugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] split) {
        if (split.length < 1) {
            if (!checkPerm(sender, "help")) return true;
            return usage(sender, command);
        }
        
        String subcommand = split[0];
        if (subcommand.equals("reload")) {
            if (!checkPerm(sender, "reload")) return true;
            plugin.getConfiguration().load();
            plugin.refreshPermissions();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded.");
            return true;
        } if (subcommand.equals("check")) {
            if (!checkPerm(sender, "check")) return true;
            if (split.length != 2 && split.length != 3) return usage(sender, command, subcommand);
            
            String node = split[1];
            Permissible permissible;
            if (split.length == 2) {
                permissible = sender;
            } else {
                permissible = plugin.getServer().getPlayer(split[2]);
            }
            
            String name = (permissible instanceof Player) ? ((Player) permissible).getName() : (permissible instanceof ConsoleCommandSender) ? "Console" : "Unknown";
            
            if (permissible == null) {
                sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + split[2] + ChatColor.RED + " not found.");
            } else {
                boolean set = permissible.isPermissionSet(node), has = permissible.hasPermission(node);
                String sets = set ? " sets " : " defaults ";
                String perm = has ? " true" : " false";
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + name + ChatColor.GREEN + sets + ChatColor.WHITE + node + ChatColor.GREEN + " to " + ChatColor.WHITE + perm + ChatColor.GREEN + ".");
            }
            return true;
        } else if (subcommand.equals("info")) {
            if (!checkPerm(sender, "info")) return true;
            if (split.length != 2) return usage(sender, command, subcommand);
            
            String node = split[1];
            Permission perm = plugin.getServer().getPluginManager().getPermission(node);
            
            if (perm == null) {
                sender.sendMessage(ChatColor.RED + "Permission " + ChatColor.WHITE + node + ChatColor.RED + " not found.");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Info on permission " + ChatColor.WHITE + perm.getName() + ChatColor.GREEN + ":");
                sender.sendMessage(ChatColor.GREEN + "Default: " + ChatColor.WHITE + perm.getDefault());
                if (perm.getDescription() != null && perm.getDescription().length() > 0) {
                    sender.sendMessage(ChatColor.GREEN + "Description: " + ChatColor.WHITE + perm.getDescription());
                }
                if (perm.getChildren() != null && perm.getChildren().size() > 0) {
                    sender.sendMessage(ChatColor.GREEN + "Children: " + ChatColor.WHITE + perm.getChildren().size());
                }
            }
            return true;
        } else if (subcommand.equals("dump")) {
            if (!checkPerm(sender, "dump")) return true;
            if (split.length < 1 || split.length > 3) return usage(sender, command, subcommand);
            
            int page;
            Permissible permissible;
            if (split.length == 1) {
                permissible = sender;
                page = 1;
            } else if (split.length == 2) {
                try {
                    permissible = sender;
                    page = Integer.parseInt(split[1]);
                }
                catch (NumberFormatException ex) {
                    permissible = plugin.getServer().getPlayer(split[1]);
                    page = 1;
                }
            } else {
                permissible = plugin.getServer().getPlayer(split[1]);
                try {
                    page = Integer.parseInt(split[2]);
                }
                catch (NumberFormatException ex) {
                    page = 1;
                }
            }
            
            if (permissible == null) {
                sender.sendMessage(ChatColor.RED + "Player " + ChatColor.WHITE + split[1] + ChatColor.RED + " not found.");
            } else {
                ArrayList<PermissionAttachmentInfo> dump = new ArrayList<PermissionAttachmentInfo>(permissible.getEffectivePermissions());
                Collections.sort(dump, new Comparator<PermissionAttachmentInfo>() {
                    public int compare(PermissionAttachmentInfo a, PermissionAttachmentInfo b) {
                        return a.getPermission().compareTo(b.getPermission());
                    }
                });
                
                int numpages = 1 + (dump.size() - 1) / 8;
                if (page > numpages) {
                    page = numpages;
                } else if (page < 1) {
                    page = 1;
                }
                
                ChatColor g = ChatColor.GREEN, w = ChatColor.WHITE, r = ChatColor.RED;
                
                int start = 8 * (page - 1);
                sender.sendMessage(ChatColor.RED + "[==== " + ChatColor.GREEN + "Page " + page + " of " + numpages + ChatColor.RED + " ====]");
                for (int i = start; i < start + 8 && i < dump.size(); ++i) {
                    PermissionAttachmentInfo info = dump.get(i);
                    
                    if (info.getAttachment() == null) {
                        sender.sendMessage(g + "Node " + w + info.getPermission() + g + "=" + w + info.getValue() + g + " (" + r + "default" + g + ")");
                    } else {
                        sender.sendMessage(g + "Node " + w + info.getPermission() + g + "=" + w + info.getValue() + g + " (" + w + info.getAttachment().getPlugin().getDescription().getName() + g + ")");
                    }
                }
            }
            return true;
        } else if (subcommand.equals("group")) {
            if (split.length < 2) {
                if (!checkPerm(sender, "group.help")) return true;
                return usage(sender, command, subcommand);
            }
            try {
                groupCommand(sender, command, split);
            } catch (DataAccessException e) {
                plugin.logError(e);
                sender.sendMessage(ChatColor.RED + "An error occured while handling your request.");
            }
            return true;
        } else if (subcommand.equals("player")) {
            if (split.length < 2) {
                if (!checkPerm(sender, "player.help")) return true;
                return usage(sender, command, subcommand);
            }
            try {
                playerCommand(sender, command, split);
            } catch (DataAccessException e) {
                plugin.logError(e);
                sender.sendMessage(ChatColor.RED + "An error occured while handling your request.");
            }
            return true;
        } else {
            if (!checkPerm(sender, "help")) return true;
            return usage(sender, command);
        }
    }

    private boolean groupCommand(CommandSender sender, Command command, String[] split) throws DataAccessException {
        String subcommand = split[1];

        PermissionsData data = plugin.getData();
        
        if (subcommand.equals("list")) {
            if (!checkPerm(sender, "group.list")) return true;
            if (split.length != 2) return usage(sender, command, "group list");
            
            Vector<String> groups = new Vector<String>(data.getGroups());
            Collections.sort(groups);

            String result = "", sep = "";
            for (String key : groups) {
                result += sep + key;
                sep = ", ";
            }
            sender.sendMessage(ChatColor.GREEN + "Groups: " + ChatColor.WHITE + result);
            return true;
        } else if (subcommand.equals("players")) {
            if (!checkPerm(sender, "group.players")) return true;
            if (split.length != 3) return usage(sender, command, "group players");
            String group = split[2];
            
            if (!data.groupExists(group)) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }
            
            Vector<String> users = new Vector<String>(data.getGroupMembers(group));
            Collections.sort(users);

            int count = 0;
            String text = "", sep = "";
            for (String user : users) {
                ++count;
                text += sep + user;
                sep = ", ";
            }
            sender.sendMessage(ChatColor.GREEN + "Users in " + ChatColor.WHITE + group + ChatColor.GREEN + " (" + ChatColor.WHITE + count + ChatColor.GREEN + "): " + ChatColor.WHITE + text);
            return true;
        } else if (subcommand.equals("setperm")) {
            if (!checkPerm(sender, "group.setperm")) return true;
            if (split.length != 4 && split.length != 5) return usage(sender, command, "group setperm");
            String group = split[2];
            String perm = split[3];
            boolean value = (split.length == 5) ? Boolean.parseBoolean(split[4]) : true;
            
            if (!data.groupExists(group)) {
                sender.sendMessage(ChatColor.GRAY + "Creating new group " + ChatColor.WHITE + group + ChatColor.GRAY + ".");
            }
            
            String world = "";

            if (perm.contains(":")) {
                world = perm.substring(0, perm.indexOf(':'));
                perm = perm.substring(perm.indexOf(':') + 1);
            }

            Boolean oldvalue = data.addGroupPermission(group, world, perm, value);

            plugin.refreshPermissions();

            if (oldvalue == null) {
                sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " now has " + ChatColor.WHITE + perm + ChatColor.GREEN + " = " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
            } else if (oldvalue != value) {
                sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " changed to have " + ChatColor.WHITE + perm + ChatColor.GREEN + " = " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " already had " + ChatColor.WHITE + perm + ChatColor.GREEN + " = " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
            }
            return true;
        } else if (subcommand.equals("unsetperm")) {
            if (!checkPerm(sender, "group.unsetperm")) return true;
            if (split.length != 4) return usage(sender, command, "group unsetperm");
            String group = split[2].toLowerCase();
            String perm = split[3];
            
            if (!data.groupExists(group)) {
                sender.sendMessage(ChatColor.RED + "No such group " + ChatColor.WHITE + group + ChatColor.RED + ".");
                return true;
            }
            
            String world = "";

            if (perm.contains(":")) {
                world = perm.substring(0, perm.indexOf(':'));
                perm = perm.substring(perm.indexOf(':') + 1);
            }

            Boolean oldvalue = data.removeGroupPermission(group, world, perm);
            if (oldvalue == null) {
                sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " did not have " + ChatColor.WHITE + perm + ChatColor.GREEN + " set.");
                return true;
            }
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Group " + ChatColor.WHITE + group + ChatColor.GREEN + " no longer has " + ChatColor.WHITE + perm + ChatColor.GREEN + " set.");
            return true;
        } else {
            if (!checkPerm(sender, "group.help")) return true;
            return usage(sender, command);
        }
    }

    private boolean playerCommand(CommandSender sender, Command command, String[] split) throws DataAccessException {
        String subcommand = split[1];
        PermissionsData data = plugin.getData();
        
        if (subcommand.equals("groups")) {
            if (!checkPerm(sender, "player.groups")) return true;
            if (split.length != 3) return usage(sender, command, "player groups");
            String player = split[2].toLowerCase();
            
            Vector<String> groups = new Vector<String>(data.getGroupMembership(player));
            Collections.sort(groups);

            if (groups.size() == 0) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.RED + " is in the default group.");
                return true;
            }
            
            int count = 0;
            String text = "";
            String sep = "";
            for (String group : groups) {
                ++count;
                text += sep + group;
                sep = ", ";
            }
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is in groups (" + ChatColor.WHITE + count + ChatColor.GREEN + "): " + ChatColor.WHITE + text);
            return true;
        } else if (subcommand.equals("setgroup")) {
            if (!checkPerm(sender, "player.setgroup")) return true;
            if (split.length != 4) return usage(sender, command, "player setgroup");
            String player = split[2].toLowerCase();
            String[] groups = split[3].split(",");
            
            for (String group : data.getGroupMembership(player)) {
                data.leaveGroup(player, group);
            }

            if (!data.userExists(player)) {
                sender.sendMessage(ChatColor.GRAY + "Creating new player " + ChatColor.WHITE + player + ChatColor.GRAY + ".");
            }

            for (String group : groups) {
                if (!data.groupExists(group)) {
                    sender.sendMessage(ChatColor.GRAY + "Creating new group " + ChatColor.WHITE + group + ChatColor.GRAY + ".");
                }
                data.joinGroup(player, group);
            }
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is now in " + ChatColor.WHITE + split[3] + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("addgroup")) {
            if (!checkPerm(sender, "player.addgroup")) return true;
            if (split.length != 4) return usage(sender, command, "player addgroup");
            String player = split[2].toLowerCase();
            String group = split[3];
            
            if (!data.groupExists(group)) {
                sender.sendMessage(ChatColor.GRAY + "Creating new group " + ChatColor.WHITE + group + ChatColor.GRAY + ".");
            }
            if (!data.userExists(player)) {
                sender.sendMessage(ChatColor.GRAY + "Creating new player " + ChatColor.WHITE + player + ChatColor.GRAY + ".");
            }

            if (!data.joinGroup(player, group)) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " was already in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
                return true;
            }
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is now in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("removegroup")) {
            if (!checkPerm(sender, "player.removegroup")) return true;
            if (split.length != 4) return usage(sender, command, "player removegroup");
            String player = split[2].toLowerCase();
            String group = split[3];
            
            if (!data.leaveGroup(player, group)) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " was not in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
                return true;
            }
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " is no longer in " + ChatColor.WHITE + group + ChatColor.GREEN + ".");
            return true;
        } else if (subcommand.equals("setperm")) {
            if (!checkPerm(sender, "player.setperm")) return true;
            if (split.length != 4 && split.length != 5) return usage(sender, command, "player setperm");
            String player = split[2].toLowerCase();
            String perm = split[3];
            boolean value = (split.length == 5) ? Boolean.parseBoolean(split[4]) : true;
            
            if (!data.userExists(player)) {
                sender.sendMessage(ChatColor.GRAY + "Creating new player " + ChatColor.WHITE + player + ChatColor.GRAY + ".");
            }

            String world="";
            
            if (perm.contains(":")) {
                world = perm.substring(0, perm.indexOf(':'));
                perm = perm.substring(perm.indexOf(':') + 1);
            }
            
            Boolean oldvalue = data.addUserPermission(player, world, perm, value);
            
            plugin.refreshPermissions();
            
            if (oldvalue == null) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " now has " + ChatColor.WHITE + perm + ChatColor.GREEN + " = " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
            } else if (oldvalue != value) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " changed to have " + ChatColor.WHITE + perm + ChatColor.GREEN + " = " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
            } else {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " already had " + ChatColor.WHITE + perm + ChatColor.GREEN + " = " + ChatColor.WHITE + value + ChatColor.GREEN + ".");
            }
            return true;
        } else if (subcommand.equals("unsetperm")) {
            if (!checkPerm(sender, "player.unsetperm")) return true;
            if (split.length != 4) return usage(sender, command, "player unsetperm");
            String player = split[2].toLowerCase();
            String perm = split[3];
            
            String world = "";
            
            if (perm.contains(":")) {
                world = perm.substring(0, perm.indexOf(':'));
                perm = perm.substring(perm.indexOf(':') + 1);
            }
            
            Boolean oldvalue = data.removeUserPermission(player, world, perm);

            if (oldvalue == null) {
                sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " did not have " + ChatColor.WHITE + perm + ChatColor.GREEN + " set.");
                return true;
            }
            
            plugin.refreshPermissions();
            
            sender.sendMessage(ChatColor.GREEN + "Player " + ChatColor.WHITE + player + ChatColor.GREEN + " no longer has " + ChatColor.WHITE + perm + ChatColor.GREEN + " set.");
            return true;
        } else {
            if (!checkPerm(sender, "player.help")) return true;
            return usage(sender, command);
        }
    }

    
    // -- utilities --
    
    private boolean checkPerm(CommandSender sender, String subnode) {
        boolean ok = sender.hasPermission("permissions." + subnode);
        if (!ok) {
            sender.sendMessage(ChatColor.RED + "You do not have permissions to do that.");
        }
        return ok;
    }
    
    private boolean usage(CommandSender sender, Command command) {
        sender.sendMessage(ChatColor.RED + "[====" + ChatColor.GREEN + " /permissons " + ChatColor.RED + "====]");
        for (String line : command.getUsage().split("\\n")) {
            if ((line.startsWith("/<command> group") && !line.startsWith("/<command> group -")) ||
                (line.startsWith("/<command> player") && !line.startsWith("/<command> player -"))) {
                continue;
            }
            sender.sendMessage(formatLine(line));
        }
        return true;
    }
    
    private boolean usage(CommandSender sender, Command command, String subcommand) {
        sender.sendMessage(ChatColor.RED + "[====" + ChatColor.GREEN + " /permissons " + subcommand + " " + ChatColor.RED + "====]");
        for (String line : command.getUsage().split("\\n")) {
            if (line.startsWith("/<command> " + subcommand)) {
                sender.sendMessage(formatLine(line));
            }
        }
        return true;
    }
    
    private String formatLine(String line) {
        int i = line.indexOf(" - ");
        String usage = line.substring(0, i);
        String desc = line.substring(i + 3);

        usage = usage.replace("<command>", "permissions");
        usage = usage.replaceAll("\\[[^]:]+\\]", ChatColor.AQUA + "$0" + ChatColor.GREEN);
        usage = usage.replaceAll("\\[[^]]+:\\]", ChatColor.AQUA + "$0" + ChatColor.LIGHT_PURPLE);
        usage = usage.replaceAll("<[^>]+>", ChatColor.LIGHT_PURPLE + "$0" + ChatColor.GREEN);

        return ChatColor.GREEN + usage + " - " + ChatColor.WHITE + desc;
    }
    
}

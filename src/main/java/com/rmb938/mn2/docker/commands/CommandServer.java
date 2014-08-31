package com.rmb938.mn2.docker.commands;

import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.rmb938.mn2.docker.MN2Bungee;
import com.rmb938.mn2.docker.db.entity.MN2Server;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;
import org.bson.types.ObjectId;

import java.util.Collections;
import java.util.Map;

public class CommandServer extends Command implements TabExecutor {

    private final MN2Bungee plugin;

    public CommandServer(MN2Bungee plugin) {
        super("server", "bungeecord.command.server");
        this.plugin = plugin;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            return;
        }
        ProxiedPlayer player = (ProxiedPlayer) sender;
        Map<String, ServerInfo> servers = ProxyServer.getInstance().getServers();
        if (args.length == 0) {
            player.sendMessage(ProxyServer.getInstance().getTranslation("current_server") + player.getServer().getInfo().getName());
            TextComponent serverList = new TextComponent(ProxyServer.getInstance().getTranslation("server_list"));
            serverList.setColor(ChatColor.GOLD);
            boolean first = true;
            for (ServerInfo server : servers.values()) {
                if (server.canAccess(player)) {
                    TextComponent serverTextComponent;
                    int count;
                    try {
                        MN2Server mn2Server = plugin.getServerLoader().loadEntity(new ObjectId(server.getName()));
                        if (mn2Server == null) {
                            throw new Exception("");
                        }
                        serverTextComponent = new TextComponent(first ? mn2Server.getServerType().getName() + "." + mn2Server.getNumber() : ", " + mn2Server.getServerType().getName() + "." + mn2Server.getNumber());
                        count = mn2Server.getPlayers().size();
                    } catch (Exception ex) {
                        serverTextComponent = new TextComponent(first ? server.getName() : ", " + server.getName());
                        count = server.getPlayers().size();
                    }
                    serverTextComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                            new ComponentBuilder(count + (count == 1 ? " player" : " players") + "\n")
                                    .append("Click to connect to the server").italic(true)
                                    .create()));
                    serverTextComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/server " + server.getName()));
                    serverList.addExtra(serverTextComponent);
                    first = false;
                }
            }
            player.sendMessage(serverList);
        } else {
            ServerInfo server = servers.get(args[0]);
            if (server == null) {
                String[] info = args[0].split("\\.");
                MN2Server mn2Server = plugin.getServerLoader().getServer(plugin.getServerTypeLoader().getType(info[0]), Integer.parseInt(info[1]));
                if (mn2Server == null) {
                    player.sendMessage(ProxyServer.getInstance().getTranslation("no_server"));
                    return;
                }
                server = servers.get(mn2Server.get_id().toString());
                if (server == null) {
                    player.sendMessage(ProxyServer.getInstance().getTranslation("no_server"));
                    return;
                }
            }
            if (!server.canAccess(player)) {
                player.sendMessage(ProxyServer.getInstance().getTranslation("no_server_permission"));
                return;
            }
            player.connect(server);
        }
    }

    @Override
    public Iterable<String> onTabComplete(final CommandSender sender, String[] args) {
        return (args.length != 0) ? Collections.EMPTY_LIST : Iterables.transform(Iterables.filter(plugin.getServerLoader().getServers(), new Predicate<MN2Server>() {
            @Override
            public boolean apply(MN2Server input) {
                ServerInfo server = plugin.getProxy().getServerInfo(input.get_id().toString());
                if (server != null) {
                    return server.canAccess(sender);
                } else {
                    return false;
                }
            }
        }), new Function<MN2Server, String>() {
            @Override
            public String apply(MN2Server input) {
                if (input.getServerType() != null) {
                    return input.getServerType().getName()+"."+input.getNumber();
                } else {
                    return input.get_id().toString();
                }
            }
        });
    }
}
package com.rmb938.mn2.docker;

import com.rmb938.mn2.docker.db.entity.MN2Server;
import com.rmb938.mn2.docker.db.entity.MN2ServerType;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bson.types.ObjectId;

import java.util.ArrayList;

public class MN2ReconnectHandler extends AbstractReconnectHandler {

    private final MN2Bungee plugin;

    public MN2ReconnectHandler(MN2Bungee plugin) {
        this.plugin = plugin;
    }

    @Override
    public ServerInfo getServer(ProxiedPlayer player) {
        ServerInfo serverInfo = MN2ReconnectHandler.getForcedHost(player.getPendingConnection());
        plugin.getLogger().info("Forced Host "+serverInfo);
        if (serverInfo == null) {
            serverInfo = getStoredServer(player);
            plugin.getLogger().info("Stored Server "+serverInfo);
            if (serverInfo == null) {
                serverInfo = getDefault(player);
                plugin.getLogger().info("Default Host "+serverInfo);
            }
        }
        if (serverInfo == null) {
            player.disconnect(new TextComponent("Unable to find a server to connect to. Please report."));
        }
        return serverInfo;
    }

    private static MN2Server getEmptiestServer(MN2Bungee plugin, MN2ServerType serverType) {
        ArrayList<MN2Server> servers = plugin.getServerLoader().getTypeServers(serverType);
        if (servers.isEmpty()) {
            return null;
        }
        MN2Server emptiest = servers.remove(0);
        for (MN2Server server : servers) {
            if (server.getPlayers().size() < emptiest.getPlayers().size()) {
                emptiest = server;
            }
        }
        return emptiest;
    }

    public static ServerInfo getForcedHost(PendingConnection connection) {
        MN2Bungee plugin = (MN2Bungee) ProxyServer.getInstance().getPluginManager().getPlugin("MN2Bungee");
        if (connection.getVirtualHost() == null) {
            return null;
        }
        String forced = connection.getListener().getForcedHosts().get(connection.getVirtualHost().getHostString());

        if (forced == null && connection.getListener().isForceDefault()) {
            forced = connection.getListener().getDefaultServer();
        }
        MN2Server server = getEmptiestServer(plugin, plugin.getServerTypeLoader().loadEntity(new ObjectId(forced)));

        return plugin.getProxy().getServerInfo(server.get_id().toString());
    }

    @Override
    protected ServerInfo getStoredServer(ProxiedPlayer proxiedPlayer) {
        ServerInfo serverInfo = getDefault(proxiedPlayer);
        return serverInfo;
    }

    private ServerInfo getDefault(ProxiedPlayer proxiedPlayer) {
        String defaultServer = proxiedPlayer.getPendingConnection().getListener().getDefaultServer();
        MN2Server server = getEmptiestServer(plugin, plugin.getServerTypeLoader().loadEntity(new ObjectId(defaultServer)));
        if (server == null) {
            return null;
        }
        return plugin.getProxy().getServerInfo(server.get_id().toString());
    }

    @Override
    public void setServer(ProxiedPlayer proxiedPlayer) {

    }

    @Override
    public void save() {

    }

    @Override
    public void close() {

    }
}

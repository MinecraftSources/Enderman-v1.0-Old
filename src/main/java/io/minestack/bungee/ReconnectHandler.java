package io.minestack.bungee;

import io.minestack.db.DoubleChest;
import io.minestack.db.entity.*;
import net.md_5.bungee.api.AbstractReconnectHandler;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.PendingConnection;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import org.bson.types.ObjectId;

import java.util.ArrayList;

public class ReconnectHandler extends AbstractReconnectHandler {

    private final Enderman plugin;

    public ReconnectHandler(Enderman plugin) {
        this.plugin = plugin;
    }

    @Override
    public ServerInfo getServer(ProxiedPlayer player) {
        ServerInfo serverInfo = ReconnectHandler.getForcedHost(player.getPendingConnection());
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

    public ServerInfo getSimilarServer(ProxiedPlayer player, ServerInfo serverInfo) {
        if (serverInfo == null) {
            return getDefault(player);
        }
        DCServer server = DoubleChest.getServerLoader().loadEntity(new ObjectId(serverInfo.getName()));
        if (server == null) {
            return null;
        }
        DCServerType serverType = server.getServerType();
        if (serverType == null) {
            return null;
        }

        DCBungee bungee = plugin.getBungee();
        boolean allowRejoin = false;
        for (DCServerType serverType1 : bungee.getBungeeType().getServerTypes().keySet()) {
            if (serverType1.get_id().equals(serverType.get_id())) {
                allowRejoin = bungee.getBungeeType().getServerTypes().get(serverType1);
                break;
            }
        }

        if (allowRejoin) {
            server = getServerWithRoom(plugin, serverType, serverInfo);
        } else {
            return getDefault(player);
        }

        return plugin.getProxy().getServerInfo(server.get_id().toString());
    }

    private static DCServer getServerWithRoom(Enderman plugin, DCServerType serverType, ServerInfo lastServer) {
        ArrayList<DCServer> servers = DoubleChest.getServerLoader().getTypeServers(serverType);
        DCServer toRemove = null;
        for (DCServer server : servers) {
            if (server.get_id().toString().equals(lastServer.getName())) {
                toRemove = server;
                break;
            }
        }
        if (toRemove != null) {
            servers.remove(toRemove);
        }
        if (servers.isEmpty()) {
            plugin.getLogger().info("Emptiest Server Empty "+serverType.getName());
            return null;
        }
        ArrayList<DCServer> roomServers = new ArrayList<>();
        for (DCServer server : servers) {
            if (server.getPort() == -1) {
                plugin.getLogger().info("Port -1 Skipping "+server.get_id().toString());
                continue;
            }
            if (server.getLastUpdate() == 0) {
                plugin.getLogger().info("LAst Update 0 Skipping "+server.get_id().toString());
                continue;
            }
            if (plugin.getProxy().getServerInfo(server.get_id().toString()) != null) {
                if ((server.getServerType().getPlayers() - server.getPlayers().size()) > 0) {
                    roomServers.add(server);
                }
            }
        }
        if (roomServers.isEmpty()) {
            plugin.getLogger().info("Cannot find a empty server "+serverType.getName());
            return null;
        }
        int random = (int) (Math.random() * roomServers.size());
        return roomServers.get(random);
    }

    public static DCServer getServerWithRoom(Enderman plugin, DCServerType serverType) {
        ArrayList<DCServer> servers = DoubleChest.getServerLoader().getTypeServers(serverType);
        if (servers.isEmpty()) {
            plugin.getLogger().info("Emptiest Server Empty "+serverType.getName());
            return null;
        }
        ArrayList<DCServer> roomServers = new ArrayList<>();
        for (DCServer server : servers) {
            if (server.getPort() == -1) {
                plugin.getLogger().info("Port -1 Skipping "+server.get_id().toString());
                continue;
            }
            if (server.getLastUpdate() == 0) {
                plugin.getLogger().info("LAst Update 0 Skipping "+server.get_id().toString());
                continue;
            }
            if (plugin.getProxy().getServerInfo(server.get_id().toString()) != null) {
                if ((server.getServerType().getPlayers() - server.getPlayers().size()) > 0) {
                    roomServers.add(server);
                }
            }
        }
        if (roomServers.isEmpty()) {
            plugin.getLogger().info("Cannot find a empty server "+serverType.getName());
            return null;
        }
        int random = (int) (Math.random() * roomServers.size());
        return roomServers.get(random);
    }

    public static ServerInfo getForcedHost(PendingConnection connection) {
        Enderman plugin = (Enderman) ProxyServer.getInstance().getPluginManager().getPlugin("Enderman");
        if (connection.getVirtualHost() == null) {
            return null;
        }
        String forced = connection.getListener().getForcedHosts().get(connection.getVirtualHost().getHostString());

        if (forced == null && connection.getListener().isForceDefault()) {
            return null;
        }
        DCServer server = getServerWithRoom(plugin, DoubleChest.getServerTypeLoader().loadEntity(new ObjectId(forced)));

        return plugin.getProxy().getServerInfo(server.get_id().toString());
    }

    @Override
    protected ServerInfo getStoredServer(ProxiedPlayer proxiedPlayer) {
        DCPlayer player = DoubleChest.getPlayerLoader().loadPlayer(proxiedPlayer.getUniqueId());
        if (player == null) {
            player = new DCPlayer();
            player.setPlayerName(proxiedPlayer.getName());
            player.setUuid(proxiedPlayer.getUniqueId());
            ObjectId objectId = DoubleChest.getPlayerLoader().insertEntity(player);
            player = DoubleChest.getPlayerLoader().loadEntity(objectId);
        } else {
            if (player.getPlayerName().equals(proxiedPlayer.getName()) == false) {
                player.setPlayerName(proxiedPlayer.getName());
                DoubleChest.getPlayerLoader().saveEntity(player);
            }
        }
        ServerInfo serverInfo = null;

        DCBungeeType bungeeType = plugin.getBungee().getBungeeType();
        for (DCBungeeType bungeeType1 : player.getLastServerTypes().keySet()) {
            if (bungeeType1.get_id().equals(bungeeType.get_id())) {
                DCServerType serverType = player.getLastServerTypes().get(bungeeType1);
                if (serverType != null) {
                    boolean allowRejoin = false;
                    for (DCServerType serverType1 : bungeeType.getServerTypes().keySet()) {
                        if (serverType1.get_id().equals(serverType.get_id())) {
                            allowRejoin = bungeeType.getServerTypes().get(serverType1);
                            break;
                        }
                    }

                    if (allowRejoin == true) {
                        DCServer server = ReconnectHandler.getServerWithRoom(plugin, serverType);
                        if (server != null) {
                            serverInfo = plugin.getProxy().getServerInfo(server.get_id().toString());
                        }
                    }
                }
                break;
            }
        }

        if (serverInfo == null) {
            serverInfo = getDefault(proxiedPlayer);
        }

        return serverInfo;
    }

    private ServerInfo getDefault(ProxiedPlayer proxiedPlayer) {
        String defaultServer = proxiedPlayer.getPendingConnection().getListener().getDefaultServer();
        DCServer server = getServerWithRoom(plugin, DoubleChest.getServerTypeLoader().loadEntity(new ObjectId(defaultServer)));
        if (server == null) {
            plugin.getLogger().severe("Null emptiest server");
            return null;
        }
        ServerInfo serverInfo = plugin.getProxy().getServerInfo(server.get_id().toString());
        if (serverInfo == null) {
            plugin.getLogger().warning("Null server info");
        }
        return serverInfo;
    }

    @Override
    public void setServer(ProxiedPlayer proxiedPlayer) {
        DCPlayer player = DoubleChest.getPlayerLoader().loadPlayer(proxiedPlayer.getUniqueId());
        if (player == null) {
            return;
        }
        ServerInfo serverInfo = proxiedPlayer.getServer().getInfo();
        DCServer server = DoubleChest.getServerLoader().loadEntity(new ObjectId(serverInfo.getName()));
        if (server != null && server.getServerType() != null) {
            DCBungeeType bungeeType = plugin.getBungee().getBungeeType();

            for (DCBungeeType bungeeType1 : player.getLastServerTypes().keySet()) {
                if (bungeeType1.get_id().equals(bungeeType.get_id())) {
                    bungeeType = bungeeType1;
                }
            }
            player.getLastServerTypes().put(bungeeType, server.getServerType());
        } else {
            String defaultServer = proxiedPlayer.getPendingConnection().getListener().getDefaultServer();
            DCServerType serverType = DoubleChest.getServerTypeLoader().loadEntity(new ObjectId(defaultServer));
            if (serverType != null) {
                DCBungeeType bungeeType = plugin.getBungee().getBungeeType();

                for (DCBungeeType bungeeType1 : player.getLastServerTypes().keySet()) {
                    if (bungeeType1.get_id().equals(bungeeType.get_id())) {
                        bungeeType = bungeeType1;
                    }
                }
                player.getLastServerTypes().put(bungeeType, serverType);
            }
        }
        DoubleChest.getPlayerLoader().saveEntity(player);
    }

    @Override
    public void save() {

    }

    @Override
    public void close() {

    }
}

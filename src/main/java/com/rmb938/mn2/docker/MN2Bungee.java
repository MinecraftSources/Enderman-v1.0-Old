package com.rmb938.mn2.docker;

import com.mongodb.ServerAddress;
import com.rabbitmq.client.Address;
import com.rmb938.mn2.docker.db.database.*;
import com.rmb938.mn2.docker.db.entity.MN2Server;
import com.rmb938.mn2.docker.db.entity.MN2ServerType;
import com.rmb938.mn2.docker.db.mongo.MongoDatabase;
import com.rmb938.mn2.docker.db.rabbitmq.RabbitMQ;
import com.rmb938.mn2.docker.listeners.PlayerListener;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import org.bson.types.ObjectId;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MN2Bungee extends Plugin {

    //private com.rmb938.mn2.docker.db.entity.MN2Bungee bungee;
    private BungeeLoader bungeeLoader;
    private ServerLoader serverLoader;
    private ServerTypeLoader serverTypeLoader;
    private PlayerLoader playerLoader;

    public ServerLoader getServerLoader() {
        return serverLoader;
    }

    public ServerTypeLoader getServerTypeLoader() {
        return serverTypeLoader;
    }

    public PlayerLoader getPlayerLoader() {
        return playerLoader;
    }

    public com.rmb938.mn2.docker.db.entity.MN2Bungee getBungee() {
        return bungeeLoader.loadEntity(new ObjectId(System.getenv("MY_BUNGEE_ID")));
    }

    @Override
    public void onEnable() {
        final Plugin plugin = this;
        getProxy().getScheduler().runAsync(this, () -> {
            getLogger().info("Starting MN2 Bukkit");

            String hosts = System.getenv("MONGO_HOSTS");

            if (hosts == null) {
                getLogger().severe("MONGO_HOSTS is not set.");
                getProxy().stop();
                return;
            }
            List<ServerAddress> mongoAddresses = new ArrayList<ServerAddress>();
            for (String host : hosts.split(",")) {

                String[] info = host.split(":");
                try {
                    mongoAddresses.add(new ServerAddress(info[0], Integer.parseInt(info[1])));
                    getLogger().info("Added Mongo Address " + host);
                } catch (UnknownHostException e) {
                    getLogger().severe("Invalid Mongo Address " + host);
                }
            }

            if (mongoAddresses.isEmpty()) {
                getLogger().severe("No valid mongo addresses");
                getProxy().stop();
                return;
            }
            getLogger().info("Setting up mongo database mn2");
            MongoDatabase mongoDatabase = new MongoDatabase(mongoAddresses, "mn2");

            hosts = System.getenv("RABBITMQ_HOSTS");
            String username = System.getenv("RABBITMQ_USERNAME");
            String password = System.getenv("RABBITMQ_PASSWORD");

            List<Address> rabbitAddresses = new ArrayList<>();
            for (String host : hosts.split(",")) {
                String[] info = host.split(":");
                try {
                    rabbitAddresses.add(new Address(info[0], Integer.parseInt(info[1])));
                } catch (Exception e) {
                    getLogger().severe("Invalid RabbitMQ Address " + host);
                }
            }

            if (rabbitAddresses.isEmpty()) {
                getLogger().severe("No valid RabbitMQ addresses");
                return;
            }

            RabbitMQ rabbitMQ = null;
            try {
                getLogger().info("Setting up RabbitMQ");
                rabbitMQ = new RabbitMQ(rabbitAddresses, username, password);
            } catch (IOException e) {
                e.printStackTrace();
                getProxy().stop();
                return;
            }

            PluginLoader pluginLoader = new PluginLoader(mongoDatabase);
            WorldLoader worldLoader = new WorldLoader(mongoDatabase);
            serverTypeLoader = new ServerTypeLoader(mongoDatabase, pluginLoader, worldLoader);
            BungeeTypeLoader bungeeTypeLoader = new BungeeTypeLoader(mongoDatabase, pluginLoader, serverTypeLoader);
            NodeLoader nodeLoader = new NodeLoader(mongoDatabase, bungeeTypeLoader);
            bungeeLoader = new BungeeLoader(mongoDatabase, bungeeTypeLoader, nodeLoader);
            serverLoader = new ServerLoader(mongoDatabase, nodeLoader, serverTypeLoader);
            playerLoader = new PlayerLoader(mongoDatabase, serverTypeLoader, bungeeTypeLoader);

            if (getBungee() == null) {
                getLogger().severe("Could not find bungee data");
                getProxy().stop();
                return;
            }

            getProxy().getServers().clear();

            getProxy().setReconnectHandler(new MN2ReconnectHandler(this));

            new PlayerListener(this);

            getProxy().getScheduler().schedule(plugin, () -> {
                com.rmb938.mn2.docker.db.entity.MN2Bungee localBungee = getBungee();
                if (localBungee == null) {
                    getLogger().severe("Couldn't find bungee data stopping bungee");
                    getProxy().stop();
                    return;
                }
                if (localBungee.getNode() == null) {
                    getLogger().severe("Couldn't find node data stopping bungee");
                    getProxy().stop();
                    return;
                }
                if (localBungee.getBungeeType() == null) {
                    getLogger().severe("Couldn't find type data stopping bungee");
                    getProxy().stop();
                    return;
                }
                localBungee.setLastUpdate(System.currentTimeMillis());
                bungeeLoader.saveEntity(localBungee);

                ArrayList<ServerInfo> toRemove = new ArrayList<ServerInfo>();
                for (ServerInfo serverInfo : getProxy().getServers().values()) {
                    MN2Server server = serverLoader.loadEntity(new ObjectId(serverInfo.getName()));
                    if (server == null) {
                        getLogger().info("Removing "+serverInfo.getName());
                        toRemove.add(serverInfo);
                    } else if (server.getLastUpdate() <= System.currentTimeMillis()-60000 || server.getLastUpdate() == 0) {
                        getLogger().info("Removing "+server.getServerType().getName()+"."+server.getNumber());
                        toRemove.add(serverInfo);
                    }
                }

                for (ServerInfo serverInfo : toRemove) {
                    getProxy().getServers().remove(serverInfo.getName());
                }

                for (MN2ServerType serverType : localBungee.getBungeeType().getServerTypes().keySet()) {
                    ArrayList<MN2Server> servers = serverLoader.getTypeServers(serverType);
                    servers.stream().filter(server -> getProxy().getServers().containsKey(server.get_id().toString()) == false).forEach(server -> {
                        if (server.getPort() > 0 && server.getLastUpdate() > System.currentTimeMillis()-60000) {
                            getLogger().info("Adding "+server.getServerType().getName()+"."+server.getNumber());
                            ServerInfo serverInfo = getProxy().constructServerInfo(server.get_id().toString(), new InetSocketAddress(server.getNode().getAddress(), server.getPort()), "", false);
                            getProxy().getServers().put(serverInfo.getName(), serverInfo);
                        }
                    });
                }
            }, 10, 10, TimeUnit.SECONDS);
        });

    }

    @Override
    public void onDisable() {
        getLogger().info("Stopping MN2 Bungee");
        getProxy().getScheduler().cancel(this);

        com.rmb938.mn2.docker.db.entity.MN2Bungee localBungee = getBungee();

        localBungee.setLastUpdate(0);
        bungeeLoader.saveEntity(localBungee);
    }

}

package io.minestack.bungee;

import com.mongodb.ServerAddress;
import com.rabbitmq.client.Address;
import io.minestack.db.DoubleChest;
import io.minestack.db.entity.DCBungee;
import io.minestack.db.entity.DCServer;
import io.minestack.db.entity.DCServerType;
import io.minestack.bungee.commands.CommandList;
import io.minestack.bungee.commands.CommandServer;
import io.minestack.bungee.listeners.PlayerListener;
import io.minestack.bungee.listeners.PluginListener;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.plugin.Plugin;
import org.bson.types.ObjectId;

import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Enderman extends Plugin {

    public DCBungee getBungee() {
        return DoubleChest.getBungeeLoader().loadEntity(new ObjectId(System.getenv("MY_BUNGEE_ID")));
    }

    @Override
    public void onEnable() {
        final Plugin plugin = this;
        getProxy().getScheduler().runAsync(this, () -> {
            getLogger().info("Starting Enderman");

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

            try {
                DoubleChest.initDatabase(mongoAddresses, rabbitAddresses, username, password);
            } catch (Exception e) {
                e.printStackTrace();
                getProxy().stop();
                return;
            }

            if (getBungee() == null) {
                getLogger().severe("Could not find bungee data");
                getProxy().stop();
                return;
            }

            getProxy().getServers().clear();

            getProxy().setReconnectHandler(new ReconnectHandler(this));

            new PlayerListener(this);
            new PluginListener(this);

            getProxy().getPluginManager().registerCommand(this, new CommandList(this));
            getProxy().getPluginManager().registerCommand(this, new CommandServer(this));

            getProxy().getScheduler().schedule(plugin, () -> {
                DCBungee localBungee = getBungee();
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
                DoubleChest.getBungeeLoader().saveEntity(localBungee);

                ArrayList<ServerInfo> toRemove = new ArrayList<ServerInfo>();
                for (ServerInfo serverInfo : getProxy().getServers().values()) {
                    DCServer server = DoubleChest.getServerLoader().loadEntity(new ObjectId(serverInfo.getName()));
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

                for (DCServerType serverType : localBungee.getBungeeType().getServerTypes().keySet()) {
                    ArrayList<DCServer> servers = DoubleChest.getServerLoader().getTypeServers(serverType);
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
        getLogger().info("Stopping Enderman");
        getProxy().getScheduler().cancel(this);

        DCBungee localBungee = getBungee();

        localBungee.setLastUpdate(0);
        DoubleChest.getBungeeLoader().saveEntity(localBungee);
    }

}
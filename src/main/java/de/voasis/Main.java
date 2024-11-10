package de.voasis;

import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.AsyncPlayerConfigurationEvent;
import net.minestom.server.event.player.PlayerDeathEvent;
import net.minestom.server.event.player.PlayerSpawnEvent;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;

import java.nio.charset.StandardCharsets;

public class Main {
    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        InstanceContainer instanceContainer = instanceManager.createInstanceContainer();
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));

        // Enable Velocity Proxy if secret is provided
        if(System.getenv("PAPER_VELOCITY_SECRET") instanceof String vsecret) {
            VelocityProxy.enable(vsecret);
            System.out.println("v-secret: " + vsecret);
        }

        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();

        // Player spawn configuration
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            event.setSpawningInstance(instanceContainer);
            event.getPlayer().setRespawnPoint(new Pos(0, 41, 0));
        });

        // Initial inventory setup
        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> event.getPlayer().getInventory().addItemStack(ItemStack.builder(Material.IRON_AXE).build()));

        // Send players to lobby on death
        globalEventHandler.addListener(PlayerDeathEvent.class, event -> {
            for(Player p : instanceContainer.getPlayers()) {
                sendToLobby(p);
            }
        });

        // Enable PvP attack detection
        globalEventHandler.addListener(EntityAttackEvent.class, event -> {
            if (event.getEntity() instanceof Player attacker && event.getTarget() instanceof Player target) {
                handlePlayerAttack(attacker, target);
            }
        });

        // Set up lighting chunks
        instanceContainer.setChunkSupplier(LightingChunk::new);

        // Start server
        minecraftServer.start("0.0.0.0", 25565);
    }

    public static void sendToLobby(Player player) {
        String message = "lobby:" + player.getUsername();
        PluginMessagePacket packet = new PluginMessagePacket(
                "nebula:main",
                message.getBytes(StandardCharsets.UTF_8)
        );
        player.sendPacket(packet);
    }

    public static void handlePlayerAttack(Player attacker, Player target) {
        // Implement damage handling (e.g., reducing health)
        target.damage(Damage.fromPlayer(attacker, 4));  // Basic example damage value; customize as needed
        attacker.sendMessage("You hit " + target.getUsername());
        target.sendMessage("You were hit by " + attacker.getUsername());

        // Check if target player should be considered dead
        if (target.getHealth() <= 0) {
            target.kill();
        }
    }
}

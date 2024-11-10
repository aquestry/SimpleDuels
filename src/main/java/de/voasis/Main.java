package de.voasis;

import net.kyori.adventure.text.Component;
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
    private static InstanceContainer instanceContainer;
    private static final Pos SPAWN_POINT_1 = new Pos(-10, 41, 0, 90, 0);
    private static final Pos SPAWN_POINT_2 = new Pos(10, 41, 0, -90, 0);
    private static boolean firstPlayerJoined = false;
    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        instanceContainer = instanceManager.createInstanceContainer();
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 40, Block.GRASS_BLOCK));
        if(System.getenv("PAPER_VELOCITY_SECRET") instanceof String vsecret) {
            VelocityProxy.enable(vsecret);
            System.out.println("v-secret: " + vsecret);
        }
        GlobalEventHandler globalEventHandler = MinecraftServer.getGlobalEventHandler();
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            Pos spawnPosition = firstPlayerJoined ? SPAWN_POINT_2 : SPAWN_POINT_1;
            firstPlayerJoined = true;
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(spawnPosition);
        });
        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> event.getPlayer().getInventory().addItemStack(ItemStack.builder(Material.IRON_AXE).build()));
        globalEventHandler.addListener(PlayerDeathEvent.class, event -> {
            event.setDeathText(Component.empty());
            for(Player p : instanceContainer.getPlayers()) {
                sendToLobby(p);

            }
        });
        globalEventHandler.addListener(EntityAttackEvent.class, event -> {
            if (event.getEntity() instanceof Player attacker && event.getTarget() instanceof Player target) {
                handlePlayerAttack(attacker, target);
            }
        });
        instanceContainer.setChunkSupplier(LightingChunk::new);
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
        target.damage(Damage.fromPlayer(attacker, 4));
        target.setHealth(Math.max(target.getHealth() - 4, 0));
        Pos direction = target.getPosition().sub(attacker.getPosition()).mul(5);
        target.setVelocity(target.getVelocity().add(direction.x(), 0.5, direction.z()));
        if (target.getHealth() <= 0) {
            target.kill();
            for(Player p : instanceContainer.getPlayers()) {
                p.sendMessage(Component.text(attacker.getUsername() + " has won the game."));
            }
        }

    }
}

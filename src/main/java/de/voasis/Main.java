package de.voasis;

import net.kyori.adventure.text.Component;
import net.minestom.server.MinecraftServer;
import net.minestom.server.coordinate.Pos;
import net.minestom.server.entity.Player;
import net.minestom.server.entity.damage.Damage;
import net.minestom.server.event.GlobalEventHandler;
import net.minestom.server.event.entity.EntityAttackEvent;
import net.minestom.server.event.player.*;
import net.minestom.server.extras.velocity.VelocityProxy;
import net.minestom.server.instance.InstanceContainer;
import net.minestom.server.instance.InstanceManager;
import net.minestom.server.instance.LightingChunk;
import net.minestom.server.instance.block.Block;
import net.minestom.server.item.ItemStack;
import net.minestom.server.item.Material;
import net.minestom.server.network.packet.server.common.PluginMessagePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.nio.charset.StandardCharsets;
import java.util.Random;

public class Main {

    private static InstanceContainer instanceContainer;
    private static final Random random = new Random();
    public static GlobalEventHandler globalEventHandler;
    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        MinecraftServer minecraftServer = MinecraftServer.init();
        InstanceManager instanceManager = MinecraftServer.getInstanceManager();
        instanceContainer = instanceManager.createInstanceContainer();
        instanceContainer.setGenerator(unit -> unit.modifier().fillHeight(0, 1, Block.STONE_BRICKS));
        var vsecret = System.getenv("PAPER_VELOCITY_SECRET");
        if (vsecret != null) { VelocityProxy.enable(vsecret); }
        globalEventHandler = MinecraftServer.getGlobalEventHandler();
        new NebulaAPI(globalEventHandler, logger);
        globalEventHandler.addListener(AsyncPlayerConfigurationEvent.class, event -> {
            Player player = event.getPlayer();
            Pos randomSpawnPoint = new Pos(
                    -10 + random.nextInt(20),
                    1,
                    -10 + random.nextInt(20),
                    random.nextFloat() * 360,
                    0
            );
            event.setSpawningInstance(instanceContainer);
            player.setRespawnPoint(randomSpawnPoint);
            fill(new Pos(-22, 0, -22), new Pos(22, 10, -22), Block.STONE_BRICKS);
            fill(new Pos(-22, 0, 22), new Pos(22, 10, 22), Block.STONE_BRICKS);
            fill(new Pos(-22, 0, -22), new Pos(-22, 10, 22), Block.STONE_BRICKS);
            fill(new Pos(22, 0, -22), new Pos(22, 10, 22), Block.STONE_BRICKS);
        });
        globalEventHandler.addListener(PlayerSpawnEvent.class, event -> event.getPlayer().getInventory().addItemStack(ItemStack.builder(Material.IRON_AXE).build()));
        globalEventHandler.addListener(PlayerChatEvent.class, event -> event.setCancelled(true));
        globalEventHandler.addListener(PlayerBlockBreakEvent.class, event -> event.setCancelled(true));
        globalEventHandler.addListener(PlayerDisconnectEvent.class, event -> quitAll());
        globalEventHandler.addListener(EntityAttackEvent.class, event -> {
            if (event.getEntity() instanceof Player attacker && event.getTarget() instanceof Player target && attacker.getItemInMainHand().isSimilar(ItemStack.builder(Material.IRON_AXE).build())) {
                handlePlayerAttack(attacker, target);
            }
        });
        globalEventHandler.addListener(PlayerDeathEvent.class, event -> {
            event.setChatMessage(null);
            quitAll();
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
        Pos direction = target.getPosition().sub(attacker.getPosition()).mul(7);
        target.setVelocity(target.getVelocity().add(direction.x(), 1, direction.z()));
        if (target.getHealth() <= 0) {
            target.kill();
            for (Player p : instanceContainer.getPlayers()) {
                p.sendMessage(Component.text(attacker.getUsername() + " has won the game."));
            }
        }
    }

    public static void quitAll() {
        for (Player p : instanceContainer.getPlayers()) {
            sendToLobby(p);
        }
    }

    public static void fill(Pos pos1, Pos pos2, Block block) {
        int minX = Math.min(pos1.blockX(), pos2.blockX());
        int maxX = Math.max(pos1.blockX(), pos2.blockX());
        int minY = Math.min(pos1.blockY(), pos2.blockY());
        int maxY = Math.max(pos1.blockY(), pos2.blockY());
        int minZ = Math.min(pos1.blockZ(), pos2.blockZ());
        int maxZ = Math.max(pos1.blockZ(), pos2.blockZ());
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    instanceContainer.setBlock(x, y, z, block);
                }
            }
        }
    }
}
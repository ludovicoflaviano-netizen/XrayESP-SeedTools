package net.ludovicoflaviano.xrayespseedtools;

import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.WorldRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public final class XrayEspSeedToolsClient implements ClientModInitializer {
    private static boolean enabled;
    private static final List<BlockPos> ores = new ArrayList<>();
    private static long last;

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((d, r) -> d.register(
                ClientCommandManager.literal("xray")
                        .then(ClientCommandManager.literal("on").executes(c -> set(true)))
                        .then(ClientCommandManager.literal("off").executes(c -> set(false)))
                        .then(ClientCommandManager.literal("toggle").executes(c -> set(!enabled)))
        ));

        ClientCommandRegistrationCallback.EVENT.register((d, r) -> d.register(
                ClientCommandManager.literal("esp")
                        .then(ClientCommandManager.literal("on").executes(c -> set(true)))
                        .then(ClientCommandManager.literal("off").executes(c -> set(false)))
                        .then(ClientCommandManager.literal("toggle").executes(c -> set(!enabled)))
        ));

        ClientCommandRegistrationCallback.EVENT.register((d, r) -> d.register(
                ClientCommandManager.literal("seedinfo")
                        .then(ClientCommandManager.argument("seed", LongArgumentType.longArg())
                                .executes(c -> {
                                    long s = LongArgumentType.getLong(c, "seed");
                                    MinecraftClient.getInstance().player.sendMessage(Text.literal(SeedAnalyzer.describe(s)), false);
                                    return 1;
                                }))
                        .then(ClientCommandManager.literal("current").executes(c -> {
                            MinecraftClient mc = MinecraftClient.getInstance();
                            if (mc.getServer() == null) {
                                mc.player.sendMessage(Text.literal("Current multiplayer server seed is not exposed to the client."), false);
                                return 0;
                            }
                            long s = mc.getServer().getOverworld().getSeed();
                            mc.player.sendMessage(Text.literal(SeedAnalyzer.describe(s)), false);
                            return 1;
                        }))
        ));

        WorldRenderEvents.AFTER_ENTITIES.register(ctx -> {
            if (!enabled || ctx.matrices() == null || ctx.consumers() == null) return;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null || mc.world == null) return;

            if (System.currentTimeMillis() - last > 350) {
                scan(mc);
            }

            render(ctx.matrices(), ctx.consumers(), ctx.gameRenderer().getCamera().getCameraPos());
        });
    }

    private static int set(boolean value) {
        enabled = value;
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) {
            mc.player.sendMessage(Text.literal("ESP " + (value ? "enabled" : "disabled")), false);
        }
        return 1;
    }

    private static void scan(MinecraftClient mc) {
        ores.clear();
        BlockPos origin = mc.player.getBlockPos();

        for (int x = -24; x <= 24; x++) {
            for (int y = -24; y <= 24; y++) {
                for (int z = -24; z <= 24; z++) {
                    BlockPos p = origin.add(x, y, z);
                    var b = mc.world.getBlockState(p).getBlock();

                    if (b == Blocks.DIAMOND_ORE
                            || b == Blocks.DEEPSLATE_DIAMOND_ORE
                            || b == Blocks.EMERALD_ORE
                            || b == Blocks.DEEPSLATE_EMERALD_ORE
                            || b == Blocks.GOLD_ORE
                            || b == Blocks.DEEPSLATE_GOLD_ORE
                            || b == Blocks.IRON_ORE
                            || b == Blocks.DEEPSLATE_IRON_ORE
                            || b == Blocks.REDSTONE_ORE
                            || b == Blocks.DEEPSLATE_REDSTONE_ORE
                            || b == Blocks.LAPIS_ORE
                            || b == Blocks.DEEPSLATE_LAPIS_ORE
                            || b == Blocks.COPPER_ORE
                            || b == Blocks.DEEPSLATE_COPPER_ORE) {
                        ores.add(p.toImmutable());
                    }
                }
            }
        }

        last = System.currentTimeMillis();
    }

    private static void render(MatrixStack matrices, VertexConsumerProvider consumers, Vec3d camera) {
        // Minecraft 1.21.11 removed the old RenderLayer#getLines path.
        // Keep the scan and command functionality compiling while the renderer
        // is migrated to the new command-based world renderer.
        if (ores.isEmpty()) return;
    }
}

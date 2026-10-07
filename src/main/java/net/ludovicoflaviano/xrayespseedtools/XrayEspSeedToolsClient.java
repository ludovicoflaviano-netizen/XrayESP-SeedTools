package net.ludovicoflaviano.xrayespseedtools;
import com.mojang.brigadier.arguments.LongArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.client.render.WorldRenderer;
import java.util.ArrayList;
import java.util.List;
public final class XrayEspSeedToolsClient implements ClientModInitializer {
private static boolean enabled; private static final List<BlockPos> ores=new ArrayList<>(); private static long last;
public void onInitializeClient(){
ClientCommandRegistrationCallback.EVENT.register((d,r)->d.register(ClientCommandManager.literal("xray").then(ClientCommandManager.literal("on").executes(c->set(true))).then(ClientCommandManager.literal("off").executes(c->set(false))).then(ClientCommandManager.literal("toggle").executes(c->set(!enabled)))));
ClientCommandRegistrationCallback.EVENT.register((d,r)->d.register(ClientCommandManager.literal("esp").then(ClientCommandManager.literal("on").executes(c->set(true))).then(ClientCommandManager.literal("off").executes(c->set(false))).then(ClientCommandManager.literal("toggle").executes(c->set(!enabled)))));
ClientCommandRegistrationCallback.EVENT.register((d,r)->d.register(ClientCommandManager.literal("seedinfo").then(ClientCommandManager.argument("seed",LongArgumentType.longArg()).executes(c->{long s=LongArgumentType.getLong(c,"seed"); MinecraftClient.getInstance().player.sendMessage(Text.literal(SeedAnalyzer.describe(s)),false); return 1;})).then(ClientCommandManager.literal("current").executes(c->{MinecraftClient mc=MinecraftClient.getInstance(); if(mc.getServer()==null){mc.player.sendMessage(Text.literal("Current multiplayer server seed is not exposed to the client."),false);return 0;} long s=mc.getServer().getOverworld().getSeed(); mc.player.sendMessage(Text.literal(SeedAnalyzer.describe(s)),false);return 1;}))));
WorldRenderEvents.AFTER_ENTITIES.register(ctx->{if(!enabled||ctx.world()==null||ctx.matrices()==null||ctx.consumers()==null)return; MinecraftClient mc=MinecraftClient.getInstance(); if(mc.player==null)return; if(System.currentTimeMillis()-last>350)scan(mc); render(ctx.matrices(),ctx.consumers(),ctx.world().getBlockEntityRenderDispatcher()==null?mc.gameRenderer.getCamera().getPos():mc.gameRenderer.getCamera().getPos());});
}
private static int set(boolean v){enabled=v; MinecraftClient mc=MinecraftClient.getInstance(); if(mc.player!=null)mc.player.sendMessage(Text.literal("ESP "+(v?"enabled":"disabled")),false); return 1;}
private static void scan(MinecraftClient mc){ores.clear(); BlockPos o=mc.player.getBlockPos(); for(int x=-24;x<=24;x++)for(int y=-24;y<=24;y++)for(int z=-24;z<=24;z++){BlockPos p=o.add(x,y,z);var b=mc.world.getBlockState(p).getBlock();if(b==Blocks.DIAMOND_ORE||b==Blocks.DEEPSLATE_DIAMOND_ORE||b==Blocks.EMERALD_ORE||b==Blocks.DEEPSLATE_EMERALD_ORE||b==Blocks.GOLD_ORE||b==Blocks.DEEPSLATE_GOLD_ORE||b==Blocks.IRON_ORE||b==Blocks.DEEPSLATE_IRON_ORE||b==Blocks.REDSTONE_ORE||b==Blocks.DEEPSLATE_REDSTONE_ORE||b==Blocks.LAPIS_ORE||b==Blocks.DEEPSLATE_LAPIS_ORE||b==Blocks.COPPER_ORE||b==Blocks.DEEPSLATE_COPPER_ORE)ores.add(p.toImmutable());}last=System.currentTimeMillis();}
private static void render(MatrixStack m,net.minecraft.client.renderer.MultiBufferSource c,Vec3d cam){VertexConsumer v=c.getBuffer(RenderLayer.getLines());for(BlockPos p:ores){double x=p.getX()-cam.x,y=p.getY()-cam.y,z=p.getZ()-cam.z;WorldRenderer.drawBox(m,v,x,y,z,x+1,y+1,z+1,1f,1f,1f,1f);}}
}
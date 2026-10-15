package me.frankv.staaaaaaaaaaaack.verify;
import me.frankv.staaaaaaaaaaaack.Stxck;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
@Mod("stxck_verification")
public class MixedClientServer {
    private ServerPlayer player;
    private int ticks;
    public MixedClientServer() {
        if(Boolean.getBoolean("stxck.mixedClientServer")) {
            NeoForge.EVENT_BUS.addListener(this::login);
            NeoForge.EVENT_BUS.addListener(this::tick);
        }
    }
    private void login(PlayerEvent.PlayerLoggedInEvent event) {
        player=(ServerPlayer)event.getEntity();
        var level=player.level();
        for(int x=4;x<13;x++)for(int z=1;z<13;z++)level.setBlockAndUpdate(new BlockPos(x,79,z),Blocks.STONE.defaultBlockState());
        player.teleportTo(8,80,4);player.setYRot(0);player.setXRot(0);
        player.getInventory().clearContent();
        for(var item:java.util.List.of(Items.COBBLESTONE,Items.IRON_INGOT)) {
            var e=new ItemEntity(level,8,80,8,new ItemStack(item,64),0,0,0);
            e.setNoGravity(true);e.setNoPickUpDelay();Stxck.setExtraItemCount(e,436);level.addFreshEntity(e);
        }
        System.out.println("MIXED_CLIENT_SERVER fixture=1000");
    }
    private void tick(ServerTickEvent.Post event) {
        if(player==null)return;
        ticks++;
        if(ticks==300)player.teleportTo(8,80,8);
        if(ticks==380) {
            long n=0;for(int i=0;i<player.getInventory().getContainerSize();i++)n+=player.getInventory().getItem(i).getCount();
            System.out.println("MIXED_CLIENT_SERVER pickup="+n);
        }
        if(ticks==560)event.getServer().halt(false);
    }
}

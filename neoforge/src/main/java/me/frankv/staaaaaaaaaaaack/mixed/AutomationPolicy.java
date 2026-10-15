package me.frankv.staaaaaaaaaaaack.mixed;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
/** Conservative local fallback, including unknown block-entity automation. No chunk tickets. */
public final class AutomationPolicy {
 public static boolean nearAutomation(Entity entity){
  if(!(entity.level() instanceof ServerLevel level))return true;
  var pos=entity.blockPosition();int cx=pos.getX()>>4,cz=pos.getZ()>>4;
  // General machines: 4 blocks; Create fans can process items much further away.
  for(int x=cx-2;x<=cx+2;x++)for(int z=cz-2;z<=cz+2;z++){
   var chunk=level.getChunkSource().getChunkNow(x,z);if(chunk==null)continue;
   for(var be:chunk.getBlockEntities().values()){
    var id=BuiltInRegistries.BLOCK_ENTITY_TYPE.getKey(be.getType());
    int radius=id!=null&&id.toString().equals("create:encased_fan")?32:4;
    if(be.getBlockPos().distSqr(pos)<=radius*radius)return true;
   }
  }
  for(var living:level.getEntitiesOfClass(LivingEntity.class,entity.getBoundingBox().inflate(8)))
   if(!(living instanceof Player))return true;
  for(var player:level.players())if(player.distanceToSqr(entity)<32*32){
   for(int i=0;i<player.getInventory().getContainerSize();i++){
    var stack=player.getInventory().getItem(i);if(stack.isEmpty())continue;
    String id=BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    if(id.contains("magnet")||id.contains("attractor"))return true;
   }
  }
  return false;
 }
 public static boolean mustUnpack(MixedPileEntity entity){
  if(MixedConfig.excludeAutomation.get()&&nearAutomation(entity))return true;
  var level=entity.level();var pos=entity.blockPosition();
  if(!level.getFluidState(pos).isEmpty()||BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).getPath().contains("portal"))return true;
  boolean gravity=entity.entries.stream().anyMatch(e->!e.source.getBooleanOr("NoGravity",false));
  return gravity&&level.noCollision(entity,entity.getBoundingBox().move(0,-0.1,0));
 }
 private AutomationPolicy(){}
}

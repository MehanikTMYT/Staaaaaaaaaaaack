package me.frankv.staaaaaaaaaaaack.mixed;
import net.neoforged.neoforge.common.ModConfigSpec;
/** Independent optional policy; existing same-type config remains unchanged. */
public final class MixedConfig {
 public static ModConfigSpec.BooleanValue enabled, requireStablePosition, excludeAutomation, fallbackToVanilla;
 public static ModConfigSpec.DoubleValue radius,maxMergeMillisPerTick;
 public static ModConfigSpec.IntValue delayTicks,maxEntries,maxTotalItems,maxMergedEntriesPerTick;
 public static void configure(ModConfigSpec.Builder b){
  b.push("mixedAggregation");
  enabled=b.comment("Combine stable dropped items in a separate mixed-pile entity.").define("enabled",true);
  radius=b.defineInRange("radius",2.0,0.25,8.0);
  delayTicks=b.defineInRange("delayTicks",10,1,1200);
  maxEntries=b.defineInRange("maxEntries",64,2,256);
  maxTotalItems=b.defineInRange("maxTotalItems",4096,2,1048576);
  requireStablePosition=b.define("requireStablePosition",true);
  excludeAutomation=b.comment("Keep ordinary ItemEntity near block entities, NPCs and detectable magnets.").define("excludeAutomation",true);
  fallbackToVanilla=b.comment("Unpack near incompatible processing, fluids, unsupported terrain or when disabled. Disabling aggregation always unpacks saved piles.").define("fallbackToVanilla",true);
  maxMergeMillisPerTick=b.comment("Server-thread time budget for mixed aggregation per level per tick; pickup and machine actions are never queued.").defineInRange("maxMergeMillisPerTick",8.0,0.1,50.0);
  maxMergedEntriesPerTick=b.defineInRange("maxMergedEntriesPerTick",256,2,4096);
  b.pop();
 }
 private MixedConfig(){}
}

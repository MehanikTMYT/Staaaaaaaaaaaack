package me.frankv.staaaaaaaaaaaack.mixed;
import me.frankv.staaaaaaaaaaaack.Stxck;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.*;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import java.util.function.Supplier;
public final class MixedModule {
 private static final DeferredRegister<EntityType<?>> TYPES=DeferredRegister.create(Registries.ENTITY_TYPE,Stxck.MODID);
 public static final Supplier<EntityType<MixedPileEntity>> TYPE=TYPES.register("mixed_pile",()->EntityType.Builder.of(MixedPileEntity::new,MobCategory.MISC).sized(0.5f,0.25f).clientTrackingRange(8).updateInterval(10).build(ResourceKey.create(Registries.ENTITY_TYPE,Identifier.fromNamespaceAndPath(Stxck.MODID,"mixed_pile"))));
 public static void register(IEventBus bus){TYPES.register(bus);bus.addListener(MixedModule::capabilities);NeoForge.EVENT_BUS.addListener(MixedModule::joined);NeoForge.EVENT_BUS.addListener(MixedPileIndex::unload);NeoForge.EVENT_BUS.addListener(net.neoforged.bus.api.EventPriority.HIGH,MixedAggregation::drain);}
 private static void capabilities(RegisterCapabilitiesEvent e){e.registerEntity(Capabilities.Item.ENTITY_AUTOMATION,TYPE.get(),(pile,side)->pile.inventory);}
 private static void joined(EntityJoinLevelEvent e){if(e.getEntity() instanceof ItemEntity item)((MixedItemState)item).mixed$joined();}
 private MixedModule(){}
}

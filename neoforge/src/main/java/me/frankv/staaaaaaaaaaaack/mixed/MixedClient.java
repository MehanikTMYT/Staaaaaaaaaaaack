package me.frankv.staaaaaaaaaaaack.mixed;
import me.frankv.staaaaaaaaaaaack.Stxck;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.common.Mod;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
@Mod(value=Stxck.MODID,dist=Dist.CLIENT)
public final class MixedClient {
 public MixedClient(IEventBus bus){bus.addListener(MixedClient::renderers);}
 private static void renderers(EntityRenderersEvent.RegisterRenderers event){event.registerEntityRenderer(MixedModule.TYPE.get(),MixedPileRenderer::new);}
}

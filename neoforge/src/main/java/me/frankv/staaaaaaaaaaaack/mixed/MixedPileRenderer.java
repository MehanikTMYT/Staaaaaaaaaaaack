package me.frankv.staaaaaaaaaaaack.mixed;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.*;
import net.minecraft.client.renderer.entity.state.ItemClusterRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
public final class MixedPileRenderer extends EntityRenderer<MixedPileEntity,MixedPileRenderer.State> {
 public static final class State extends ItemClusterRenderState {String text;}
 private final ItemModelResolver resolver;
 private final RandomSource random=RandomSource.create();
 public MixedPileRenderer(EntityRendererProvider.Context context){super(context);resolver=context.getItemModelResolver();shadowRadius=0.2f;}
 public State createRenderState(){return new State();}
 public void extractRenderState(MixedPileEntity entity,State state,float partial){super.extractRenderState(entity,state,partial);state.extractItemGroupRenderState(entity,entity.sample(),resolver);state.count=Math.min(3,entity.entryCount());var mc=Minecraft.getInstance();state.text=state.distanceToCameraSq<=64?(mc.player!=null&&mc.player.isShiftKeyDown()?entity.summary():entity.total()+" items / "+entity.entryCount()+" entries"):null;}
 public void submit(State state,PoseStack pose,SubmitNodeCollector collector,CameraRenderState camera){
  pose.pushPose();pose.translate(0,0.25,0);pose.mulPose(Axis.YP.rotation(state.ageInTicks/20f));ItemEntityRenderer.submitMultipleFromCount(pose,collector,state.lightCoords,state,random);pose.popPose();
  if(state.text!=null){var mc=Minecraft.getInstance();pose.pushPose();pose.translate(0,0.8,0);pose.mulPose(camera.orientation);pose.scale(0.025f,-0.025f,0.025f);collector.submitText(pose,-mc.font.width(state.text)/2f,0,Component.literal(state.text).getVisualOrderText(),false,Font.DisplayMode.NORMAL,state.lightCoords,-1,0x40000000,0);pose.popPose();}
  super.submit(state,pose,collector,camera);
 }
}

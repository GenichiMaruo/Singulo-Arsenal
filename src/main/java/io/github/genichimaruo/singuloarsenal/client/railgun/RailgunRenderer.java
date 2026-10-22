package io.github.genichimaruo.singuloarsenal.client.railgun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import io.github.genichimaruo.singuloarsenal.SinguloArsenal;
import io.github.genichimaruo.singuloarsenal.weapon.RailgunItem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/** Player-sized version of Ciel's white chassis, open stock, split rails and three square accelerator coils. */
public final class RailgunRenderer extends BlockEntityWithoutLevelRenderer {
    private static final ResourceLocation WHITE=SinguloArsenal.id("textures/misc/white.png");
    private static final int SHELL=0xE6EAF0, DARK=0x28313E, METAL=0x8394A6, ACCENT=0x61F4D7;
    public RailgunRenderer() { super(Minecraft.getInstance().getBlockEntityRenderDispatcher(),Minecraft.getInstance().getEntityModels()); }

    @Override
    public void renderByItem(ItemStack stack,ItemDisplayContext context,PoseStack pose,MultiBufferSource buffers,int light,int overlay) {
        var mc=Minecraft.getInstance();
        float partial=mc.getTimer().getGameTimeDeltaPartialTick(true);
        Player owner=null;
        if(mc.level!=null && context!=ItemDisplayContext.GUI && context!=ItemDisplayContext.GROUND && context!=ItemDisplayContext.FIXED) {
            if(context.firstPerson()) owner=mc.player;
            else for(Player p:mc.level.players()) if(p.getMainHandItem()==stack) { owner=p;break; }
        }
        float charge=owner!=null && owner.isUsingItem() && owner.getUseItem().getItem() instanceof RailgunItem
                ? Math.min(1,(owner.getTicksUsingItem()+partial)/RailgunItem.CHARGE_TICKS):0;
        float age=owner==null?0:owner.tickCount+partial;
        float kick=owner==null?0:RailgunClient.recoil(owner.getId(),partial);
        pose.pushPose();
        pose.translate(0.5,0.5,0.5);
        pose.scale(1.28F,1.28F,1.28F);
        // Geometry uses the same +X barrel direction as the android's original model.
        pose.translate(-kick*0.12,0,0);
        pose.mulPose(Axis.ZP.rotationDegrees(kick*3));
        pose.scale(1F/16,1F/16,1F/16);
        pose.translate(2.7,0,0);
        VertexConsumer solid=buffers.getBuffer(RenderType.entityCutoutNoCull(WHITE));
        box(pose,solid,-4.6F,-1,-0.95F,1.5F,1,0.95F,DARK,light);
        box(pose,solid,-4.3F,0.25F,-1.3F,1.0F,1.4F,1.3F,SHELL,light);
        box(pose,solid,-3.9F,-1.25F,-1.1F,0.6F,-0.4F,1.1F,METAL,light);
        box(pose,solid,-3.4F,-2.7F,-0.45F,-2.45F,-0.8F,0.45F,DARK,light);
        // Open stock and recoil pad.
        box(pose,solid,-8,0.45F,-0.5F,-4.4F,1,0.5F,SHELL,light);
        box(pose,solid,-8,-0.95F,-0.4F,-4.4F,-0.5F,0.4F,DARK,light);
        box(pose,solid,-8.6F,-1.3F,-0.7F,-7.8F,1.3F,0.7F,DARK,light);
        // Underslung trigger guard, secondary grip, vent slats and sight.
        box(pose,solid,-2.6F,-2.5F,-0.35F,-0.7F,-2.15F,0.35F,METAL,light);
        box(pose,solid,-0.95F,-2.3F,-0.35F,-0.6F,-0.9F,0.35F,METAL,light);
        box(pose,solid,0.7F,-2,-0.65F,2.1F,-1,0.65F,DARK,light);
        box(pose,solid,-2.9F,1.4F,-0.45F,-0.3F,2.1F,0.45F,DARK,light);
        for(int i=0;i<3;i++) for(int side:new int[]{-1,1}) {
            float z=side*1.3F;
            box(pose,solid,-3.4F+i*0.9F,0,z-0.08F,-2.95F+i*0.9F,0.95F,z+0.08F,DARK,light);
        }
        // Both rails separate a little as the magnetic field builds.
        float open=charge*0.3F+kick*0.4F;
        for(int side:new int[]{-1,1}) {
            float y=side*(0.85F+open);
            box(pose,solid,1.2F,y-0.24F,-0.6F,8.5F,y+0.24F,0.6F,DARK,light);
            box(pose,solid,1.4F,y-0.12F,-0.65F,8,y+0.12F,-0.52F,METAL,light);
        }
        // Three rigid square coils rotate around the barrel when charged.
        for(int i=0;i<3;i++) {
            pose.pushPose();
            pose.mulPose(Axis.XP.rotationDegrees(charge*charge*(age*3+i*60)*(i%2==0?1:-1)));
            float x=2.3F+i*1.85F;
            box(pose,solid,x,-1.55F,-1.1F,x+0.85F,-0.95F,1.1F,SHELL,light);
            box(pose,solid,x,0.95F,-1.1F,x+0.85F,1.55F,1.1F,SHELL,light);
            box(pose,solid,x,-1.05F,-1.1F,x+0.85F,1.05F,-0.6F,SHELL,light);
            box(pose,solid,x,-1.05F,0.6F,x+0.85F,1.05F,1.1F,SHELL,light);
            pose.popPose();
        }
        box(pose,solid,8.1F,-1.3F,-0.95F,9.0F,1.3F,0.95F,DARK,light);
        box(pose,solid,8.3F,-1.45F,-1.05F,8.6F,1.45F,1.05F,METAL,light);
        VertexConsumer emissive=buffers.getBuffer(RenderType.entityTranslucentEmissive(WHITE));
        int core=charge>0.95F?0xEAFFF8:ACCENT;
        box(pose,emissive,1.5F,-0.16F,-0.16F,8.8F,0.16F,0.16F,core,LightTexture.FULL_BRIGHT);
        for(int side:new int[]{-1,1}) {
            float z=side*1.32F;
            box(pose,emissive,-3.7F,-0.1F,z-0.025F,0.4F,0.08F,z+0.025F,ACCENT,LightTexture.FULL_BRIGHT);
        }
        box(pose,emissive,-0.3F,1.55F,-0.3F,-0.23F,1.95F,0.3F,ACCENT,LightTexture.FULL_BRIGHT);
        box(pose,emissive,9.01F,-0.4F,-0.4F,9.09F,0.4F,0.4F,core,LightTexture.FULL_BRIGHT);
        // These item-local rings remain attached to the rotating coil assemblies in every view.
        VertexConsumer glow=buffers.getBuffer(RailRenderTypes.GLOW);
        for(int i=0;i<3;i++) {
            float pulse=0.45F+0.55F*(float)Math.pow(Math.max(0,Math.sin(age*0.3-i*1.5)),2);
            RailMeshes.ring(pose,glow,new Vec3(2.7+i*1.85,0,0),new Vec3(1,0,0),1.65+charge*0.25,0.12,ACCENT,0.15F+charge*pulse*0.6F,0);
        }
        if(charge>0) {
            for(int i=0;i<2;i++) RailMeshes.ring(pose,glow,new Vec3(9.25+i*1.5,0,0),new Vec3(1,0,0),
                    (1.7-i*0.4)*(1.1-charge*0.2),0.12,core,charge*0.55F,age*0.05);
        }
        pose.popPose();
    }
    private static void box(PoseStack p,VertexConsumer v,float x,float y,float z,float xx,float yy,float zz,int rgb,int light) {
        RailMeshes.box(p,v,x,y,z,xx,yy,zz,rgb,light);
    }
}

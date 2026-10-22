package io.github.genichimaruo.singuloarsenal.client.railgun;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.genichimaruo.singuloarsenal.network.RailShotPayload;
import io.github.genichimaruo.singuloarsenal.weapon.RailgunItem;
import io.github.genichimaruo.singuloarsenal.weapon.WeaponContent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.BlockEntityWithoutLevelRenderer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.ComputeFovModifierEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ViewportEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class RailgunClient {
    private record Shot(RailShotPayload data, long born) {}
    private static final ArrayList<Shot> SHOTS=new ArrayList<>();
    private static final Map<Integer,ChargeSound> HUMS=new HashMap<>();
    private static ClientLevel scene;
    private RailgunClient() {}

    public static void register(IEventBus bus) {
        bus.addListener(RailgunClient::extensions);
        RailShotPayload.setClientHandler(RailgunClient::shot);
        NeoForge.EVENT_BUS.addListener(RailgunClient::tick);
        NeoForge.EVENT_BUS.addListener(RailgunClient::world);
        NeoForge.EVENT_BUS.addListener(RailgunClient::hud);
        NeoForge.EVENT_BUS.addListener(RailgunClient::fov);
        NeoForge.EVENT_BUS.addListener(RailgunClient::camera);
    }
    private static void extensions(RegisterClientExtensionsEvent event) {
        event.registerItem(new IClientItemExtensions() {
            private RailgunRenderer renderer;
            @Override public BlockEntityWithoutLevelRenderer getCustomRenderer() {
                if(renderer==null) renderer=new RailgunRenderer();
                return renderer;
            }
            @Override public HumanoidModel.ArmPose getArmPose(LivingEntity entity,InteractionHand hand,ItemStack stack) {
                return hand==InteractionHand.MAIN_HAND ? HumanoidModel.ArmPose.CROSSBOW_HOLD : null;
            }
        },WeaponContent.RAILGUN.get());
    }
    private static boolean charging(Player p) { return p.isUsingItem() && p.getUseItem().getItem() instanceof RailgunItem; }
    private static float charge(Player p,float partial) { return charging(p)?Math.min(1,(p.getTicksUsingItem()+partial)/RailgunItem.CHARGE_TICKS):0; }
    private static void shot(RailShotPayload data) {
        var mc=Minecraft.getInstance();
        if(mc.level==null || !finite(data.from()) || !finite(data.to()) || data.from().distanceToSqr(data.to())>100*100) return;
        if(scene!=mc.level) clear(mc.level);
        if(SHOTS.size()>=32) SHOTS.removeFirst();
        SHOTS.add(new Shot(data,mc.level.getGameTime()));
    }
    private static boolean finite(Vec3 v) { return Double.isFinite(v.x)&&Double.isFinite(v.y)&&Double.isFinite(v.z); }
    private static void clear(ClientLevel next) {
        SHOTS.clear();
        for(ChargeSound sound:HUMS.values()) sound.finish();
        HUMS.clear();scene=next;
    }
    private static void tick(ClientTickEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(scene!=mc.level) clear(mc.level);
        if(mc.level==null) return;
        SHOTS.removeIf(shot->mc.level.getGameTime()-shot.born()>14);
        HUMS.entrySet().removeIf(entry -> {
            ChargeSound sound=entry.getValue();
            if(sound.isStopped() || sound.player.isRemoved() || !charging(sound.player)) {
                sound.finish();
                return true;
            }
            return false;
        });
        for(Player p:mc.level.players()) {
            if(charging(p) && !HUMS.containsKey(p.getId())) {
                var sound=new ChargeSound(p);HUMS.put(p.getId(),sound);mc.getSoundManager().play(sound);
            }
        }
    }
    static float recoil(int id,float partial) {
        if(scene==null) return 0;
        for(int i=SHOTS.size()-1;i>=0;i--) {
            Shot s=SHOTS.get(i);
            if(s.data().shooter()==id) {
                double t=scene.getGameTime()-s.born()+partial;
                return t<10?(float)(Math.exp(-t*0.4)*Math.min(1,t*3+0.4)):0;
            }
        }
        return 0;
    }
    private static void camera(ViewportEvent.ComputeCameraAngles event) {
        var mc=Minecraft.getInstance();
        if(mc.player!=null && mc.options.getCameraType().isFirstPerson())
            event.setPitch(event.getPitch()-1.4F*recoil(mc.player.getId(),(float)event.getPartialTick()));
    }
    private static void fov(ComputeFovModifierEvent event) {
        event.setNewFovModifier(event.getNewFovModifier()*(1-0.10F*charge(event.getPlayer(),0)));
    }
    private static void world(RenderLevelStageEvent event) {
        if(event.getStage()!=RenderLevelStageEvent.Stage.AFTER_LEVEL || SHOTS.isEmpty()) return;
        var mc=Minecraft.getInstance();
        if(mc.level==null) return;
        float partial=event.getPartialTick().getGameTimeDeltaPartialTick(true);
        PoseStack pose=event.getPoseStack();
        Vec3 camera=event.getCamera().getPosition();
        pose.pushPose();
        // AFTER_LEVEL supplies an identity pose; the terrain renderer has already restored its view stack.
        pose.mulPose(event.getModelViewMatrix());
        pose.translate(-camera.x,-camera.y,-camera.z);
        var buffers=mc.renderBuffers().bufferSource();
        var light=buffers.getBuffer(RailRenderTypes.GLOW);
        for(Shot shot:SHOTS) {
            double age=mc.level.getGameTime()-shot.born()+partial;
            if(age<0 || age>14) continue;
            RailShotPayload s=shot.data();
            Vec3 delta=s.to().subtract(s.from());
            if(delta.lengthSqr()<0.001) continue;
            Vec3 axis=delta.normalize();
            float fade=(float)Math.pow(Math.max(0,1-age/14),1.6);
            float flash=(float)Math.max(0,1-age/3);
            // A narrow white core inside two coherent magnetic light sheaths.
            RailMeshes.beam(pose,light,s.from(),s.to(),0.012+0.018*flash,0xF2FFF9,fade);
            RailMeshes.beam(pose,light,s.from(),s.to(),0.04+0.04*flash,0x62FFDF,fade*0.30F);
            RailMeshes.beam(pose,light,s.from(),s.to(),0.08+0.06*flash,0x20BFAE,fade*0.09F);
            if(age<10) RailMeshes.helix(pose,light,s.from(),s.to(),0.11+age*0.008,age*0.6,fade*0.48F);
            // Muzzle pressure front and a travelling accelerator ring along the beam.
            if(age<9) RailMeshes.ring(pose,light,s.from().add(axis.scale(0.12)),axis,0.12+age*0.04,0.025,
                    0x9BFFEA,(float)Math.pow(1-age/9,2)*0.45F,0);
            if(age<5) RailMeshes.ring(pose,light,s.from().lerp(s.to(),age/5),axis,0.16+age*0.03,0.04,0xD0FFF5,fade*0.6F,0);
            if(s.hitBlock()) {
                Vec3 normal=Vec3.atLowerCornerOf(s.normal().getNormal());
                Vec3 at=s.to().add(normal.scale(0.035));
                // Surface-aligned expanding rings, not billboards or particle clouds.
                RailMeshes.ring(pose,light,at,normal,0.12+age*0.115,0.07,0x67F5E2,fade*0.8F,0);
                RailMeshes.ring(pose,light,at.add(normal.scale(0.015)),normal,0.08+age*0.075,0.035,0xE8FFF8,fade*0.7F,0);
                Vec3 u=RailMeshes.perpendicular(normal),v=normal.cross(u);
                for(int i=0;i<6;i++) {
                    double angle=i*Math.PI/3;
                    Vec3 ray=u.scale(Math.cos(angle)).add(v.scale(Math.sin(angle)));
                    RailMeshes.beam(pose,light,at.add(ray.scale(0.12)),at.add(ray.scale(0.15+age*0.06)).add(normal.scale(age*0.04)),0.012,0xA9FFEB,fade*0.5F);
                }
            }
        }
        buffers.endBatch(RailRenderTypes.GLOW);pose.popPose();
    }
    private static void hud(RenderGuiEvent.Post event) {
        var mc=Minecraft.getInstance();
        if(mc.player==null || mc.options.hideGui || mc.screen!=null || !(mc.player.getMainHandItem().getItem() instanceof RailgunItem)) return;
        int x=mc.getWindow().getGuiScaledWidth()/2,y=mc.getWindow().getGuiScaledHeight()/2;
        float charge=charge(mc.player,mc.getTimer().getGameTimeDeltaPartialTick(true));
        float cooldown=mc.player.getCooldowns().getCooldownPercent(WeaponContent.RAILGUN.get(),0);
        int color=charge>=1?0xFFD9FFF1:0xFF54CDBD;
        var gui=event.getGuiGraphics();
        // Confirm real server damage, including penetration, rather than predicting a hit locally.
        for (int i=SHOTS.size()-1;i>=0;i--) {
            Shot shot=SHOTS.get(i);
            if(shot.data().shooter()!=mc.player.getId()) continue;
            if(shot.data().hits()>0 && mc.level.getGameTime()-shot.born()<8) {
                for(int dx:new int[]{-1,1}) for(int dy:new int[]{-1,1}) for(int d=5;d<=7;d++)
                    gui.fill(x+dx*d,y+dy*d,x+dx*d+1,y+dy*d+1,0xFFEAFFF8);
            }
            break;
        }
        // Open brackets leave the target visible; the lower capacitor segments fill during charge.
        for(int side:new int[]{-1,1}) {
            gui.fill(x+side*12-1,y-4,x+side*12+1,y+5,color);
            gui.fill(x+(side<0?-12:8),y-5,x+(side<0?-7:13),y-3,color);
            gui.fill(x+(side<0?-12:8),y+3,x+(side<0?-7:13),y+5,color);
        }
        for(int i=0;i<8;i++) gui.fill(x-19+i*5,y+15,x-16+i*5,y+17,
                i<charge*8?color:cooldown>0?0xFF6E7F8F:0x66304749);
        if(charge>0 || cooldown>0) {
            Component label=Component.translatable(cooldown>0?"railgun.singulo_arsenal.cooling":charge>=1?"railgun.singulo_arsenal.locked":"railgun.singulo_arsenal.charging");
            gui.drawCenteredString(mc.font,label,x,y+23,color);
        }
    }
    private static final class ChargeSound extends AbstractTickableSoundInstance {
        private final Player player;
        ChargeSound(Player p) {
            super(WeaponContent.CHARGE.get(),SoundSource.PLAYERS,RandomSource.create());
            player=p;looping=true;delay=0;volume=0.1F;attenuation=Attenuation.LINEAR;
        }
        @Override public void tick() {
            if(player.isRemoved() || !charging(player) || Minecraft.getInstance().level!=player.level()) { stop();return; }
            x=player.getX();y=player.getY()+1;z=player.getZ();
            float charge=charge(player,0);volume=0.12F+charge*0.38F;pitch=0.7F+charge*0.8F;
        }
        void finish() { stop(); }
    }
}

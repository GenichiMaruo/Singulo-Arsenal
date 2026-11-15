package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.genichimaruo.singuloarsenal.armor.SuitItem;
import io.github.genichimaruo.singuloarsenal.client.art.Canvas;
import io.github.genichimaruo.singuloarsenal.client.art.MachineArt;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt;
import io.github.genichimaruo.singuloarsenal.client.art.SuitArt.Piece;
import io.github.genichimaruo.singuloarsenal.client.art.ToolArt;
import io.github.genichimaruo.singuloarsenal.client.art.WeaponArt;
import io.github.genichimaruo.singuloarsenal.core.ArsenalData;
import io.github.genichimaruo.singuloarsenal.module.ModuleContent;
import io.github.genichimaruo.singuloarsenal.core.Fe;
import io.github.genichimaruo.singuloarsenal.core.Rules;
import io.github.genichimaruo.singuloarsenal.tool.DegeneracyBreakerItem;
import io.github.genichimaruo.singuloarsenal.tool.PoweredTool;
import io.github.genichimaruo.singuloarsenal.tool.ShearSawItem;
import io.github.genichimaruo.singuloarsenal.tool.SquidPickItem;
import io.github.genichimaruo.singuloarsenal.weapon.CasimirBladeItem;
import io.github.genichimaruo.singuloarsenal.weapon.JetCannonItem;
import io.github.genichimaruo.singuloarsenal.weapon.QuenchSaberItem;
import io.github.genichimaruo.singuloarsenal.weapon.TidalReaverItem;
import io.github.genichimaruo.singuloarsenal.weapon.Weapons;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import com.mojang.serialization.MapCodec;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import java.util.function.Consumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Renders gear as items, including inventory icons, from the same geometry that is worn, so an icon can never disagree with the model. */
public final class GearRenderer implements SpecialModelRenderer<GearRenderer.Frame> {
    private static final java.util.Map<Integer, Long> LAST_WORK = new java.util.HashMap<>();

    private final ItemDisplayContext context;
    public GearRenderer(ItemDisplayContext context) { this.context = context; }
    record Owner(int id, boolean rightHanded, boolean mainCannon, boolean using, int ticks, boolean running,
                 net.minecraft.world.phys.Vec3 aimTarget, net.minecraft.world.phys.Vec3 beamTarget,
                 net.minecraft.world.phys.Vec3 camera, boolean worldView, FirstPersonHands.Hands hands) {}
    public record Frame(ItemStack stack, long now, float partial, Owner owner) {}
    @Override public Frame extractArgument(ItemStack stack) {
        var mc = Minecraft.getInstance();
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        long now = mc.level == null ? 0 : mc.level.getGameTime();
        Player player = owner(mc, stack, context);
        Owner state = null;
        if (player != null) {
            boolean using = player.isUsingItem() && player.getUseItem().getItem() == stack.getItem();
            boolean working = player.swinging || (player == mc.player && mc.gameMode != null && mc.gameMode.isDestroying());
            if (working) LAST_WORK.put(player.getId(), now);
            boolean running = Fe.stored(stack) > 0 && now - LAST_WORK.getOrDefault(player.getId(), Long.MIN_VALUE / 2) <= 8;
            var eye = player.getEyePosition(partial);
            var hit = FxClient.aim(player, partial).getLocation();
            var camera = mc.gameRenderer.getMainCamera().position();
            boolean firing = JetCannonItem.jetAge(stack, now, partial) >= 0;
            double distance = firing ? Math.max(3, eye.distanceTo(hit)) : 40;
            state = new Owner(player.getId(), player.getMainArm() == net.minecraft.world.entity.HumanoidArm.RIGHT,
                    player.getMainHandItem().getItem() instanceof JetCannonItem, using, using ? player.getTicksUsingItem() : 0, running,
                    eye.add(player.getViewVector(partial).scale(distance)).subtract(camera), hit.subtract(camera), camera,
                    context.firstPerson() || !(mc.screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen),
                    context.firstPerson() && stack.getItem() instanceof JetCannonItem ? FirstPersonHands.capture(player) : null);
        }
        return new Frame(stack.copy(), now, partial, state);
    }
    @Override public void submit(Frame frame, PoseStack pose, SubmitNodeCollector collector,
                                 int light, int overlay, boolean foil, int outline) {
        if (frame == null) return;
        // Final item transforms are available only at submission. Solve aim and hands here from extracted data,
        // then freeze vertices before handing callbacks to the deferred geometry renderer.
        GeometrySnapshot geometry = new GeometrySnapshot();
        renderFrame(frame, context, pose, geometry, light, overlay);
        geometry.submit(new PoseStack(), collector);
    }    @Override public void getExtents(Consumer<Vector3fc> output) {
        output.accept(new Vector3f(-1, -1, -1)); output.accept(new Vector3f(2, 2, 2));
    }
    public record Unbaked(ItemDisplayContext context) implements SpecialModelRenderer.Unbaked<GearRenderer.Frame> {
        public static final MapCodec<Unbaked> CODEC = ItemDisplayContext.CODEC.fieldOf("context").xmap(Unbaked::new, Unbaked::context);
        @Override public SpecialModelRenderer<GearRenderer.Frame> bake(SpecialModelRenderer.BakingContext context) { return new GearRenderer(this.context); }
        @Override public MapCodec<Unbaked> type() { return CODEC; }
    }
    private void renderFrame(Frame frame, ItemDisplayContext context, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack stack = frame.stack;
        float partial = frame.partial;
        float t = frame.now + partial;
        pose.pushPose();
        pose.translate(0.5, 0.5, 0.5);
        if (stack.getItem() instanceof SuitItem suit) {
            drawSuit(pose, buffers, t, light, overlay, suit);
        } else if (stack.is(ModuleContent.INTEGRATOR_ITEM.get())) {
            pose.scale(1F / 16, 1F / 16, 1F / 16);
            Canvas.drawFine(pose, buffers, t, light, overlay, context != ItemDisplayContext.GUI, c -> MachineArt.integrator(c, -1, t));
        } else {
            pose.scale(1F / 16, 1F / 16, 1F / 16);
            Owner owner = frame.owner;
            drawWeapon(pose, buffers, t, light, overlay, stack, owner, frame.now, partial, context);
        }
        pose.popPose();
    }

    private static Player owner(Minecraft mc, ItemStack stack, ItemDisplayContext context) {
        if (mc.level == null || context == ItemDisplayContext.GUI || context == ItemDisplayContext.GROUND || context == ItemDisplayContext.FIXED) return null;
        if (context.firstPerson()) return mc.player;
        for (Player player : mc.level.players()) if (player.getMainHandItem() == stack) return player;
        return null;
    }

    /**
     * Swords use vanilla's handheld transforms, which centre a sprite whose blade lies on its diagonal. This centres the blade the
     * same way, and turns the cutting edge to face away from the wielder, which a flat sprite never had to decide.
     */
    private static void seatInHand(PoseStack pose, ItemStack stack) {
        pose.mulPose(Axis.YP.rotationDegrees(180));
        pose.translate(0, -(stack.getItem() instanceof TidalReaverItem ? 1.5F : stack.getItem() instanceof CasimirBladeItem ? 1.75F : 2.25F), 0);
    }

    private static void drawWeapon(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, ItemStack stack, Owner owner, long now, float partial, ItemDisplayContext context) {
        boolean using = owner != null && owner.using;
        int ticks = using ? owner.ticks : 0;
        boolean world = context != ItemDisplayContext.GUI;
        if (owner != null && !(stack.getItem() instanceof JetCannonItem)) seatInHand(pose, stack);
        float reload = world ? Weapons.reloadProgress(stack, now, partial) : -1;
        if (stack.getItem() instanceof QuenchSaberItem) {
            float current = ArsenalData.get(stack, ArsenalData.CURRENT) / (float) (Rules.QUENCH_MAX_CURRENT - 1);
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> WeaponArt.quench(c, 0, Math.min(1, current), reload));
        } else if (stack.getItem() instanceof CasimirBladeItem) {
            float use = using ? Math.min(1, (ticks + partial) / Rules.CASIMIR_CLOSE_TICKS) : 0;
            boolean armed = owner != null && CasimirBladeItem.armed(stack, now);
            // Armed mirrors stay shut; once spent they part again over the rest of the cooldown.
            float closing = armed ? 1 : Math.max(use, reload >= 0 ? 1 - reload : 0);
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> WeaponArt.casimir(c, closing, armed, t));
        } else if (stack.getItem() instanceof TidalReaverItem) {
            float use = Math.max(0, owner != null ? TidalReaverItem.focusProgress(stack, now, partial) : 0);
            if (context.firstPerson()) {
                // Drawn back while the focus builds, and snapped through on the shear.
                float strike = reload >= 0 && reload < 0.1F ? 1 - reload / 0.1F : 0;
                pose.mulPose(Axis.XP.rotationDegrees(-22 * use + 34 * strike));
            }
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> WeaponArt.reaver(c, use, reload, t));
            if (owner != null && world) FxClient.heldHole(owner, pose, context, 0, 6, 0, (0.55F + use * 0.25F) * (reload >= 0 ? Math.min(1, reload * 1.15F) : 1));
        } else if (stack.getItem() instanceof PoweredTool) {
            boolean running = owner != null && world && owner.running;
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> {
                if (stack.getItem() instanceof SquidPickItem) ToolArt.squidPick(c);
                else if (stack.getItem() instanceof DegeneracyBreakerItem) ToolArt.degeneracyBreaker(c);
                else if (stack.getItem() instanceof ShearSawItem) ToolArt.shearSaw(c, running, t);
                else ToolArt.accretionDrill(c, running, t);
            });
        } else if (stack.getItem() instanceof JetCannonItem) {
            float jet = owner != null && world ? JetCannonItem.jetAge(stack, now, partial) : -1;
            float charge = owner != null && world ? JetCannonItem.chargeProgress(stack, now, partial) : -1;
            float ignition = jet >= 0 ? 1 : Math.max(0, charge);
            boolean leftHanded = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND || context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND;
            boolean mainHand = mainHand(owner, context);
            if (mainHand && world) {
                FxClient.aimCannon(owner, pose);
                boolean firstPerson = context.firstPerson();
                // Recoil runs straight back along the barrel, so the beam stays on its axis.
                pose.translate(0, -(firstPerson ? 1.6F : 0.8F) * Rules.cannonKick(jet), 0);
                if (reload >= 0 && !firstPerson) {
                    // Drawn in toward the chest while the support hand works on it: at arm's length the magazine is out of reach.
                    pose.translate(0, -9 * Rules.reloadReach(reload), 0);
                }
                if (reload >= 0) {
                    // Muzzle up and rolled so the magazine well faces the support hand.
                    float tilt = Rules.reloadTilt(reload) * (firstPerson ? 1 : 0.55F);
                    pose.mulPose(Axis.XP.rotationDegrees(22 * tilt));
                    pose.mulPose(Axis.YP.rotationDegrees((owner.rightHanded ? 30 : -30) * tilt));
                }
            }
            Canvas.drawFine(pose, buffers, t, light, overlay, world, c -> WeaponArt.cannon(c, ignition, reload, t, leftHanded ? 1 : -1));
            if (mainHand) {
                FxClient.cannonRendered(owner, pose, buffers, context, jet);
                FirstPersonHands.drawCannon(owner.hands, pose, buffers, light, context, reload);
            }
        }
    }

    /** Display contexts describe the physical arm, not MAIN_HAND/OFF_HAND. */
    private static boolean mainHand(Owner owner, ItemDisplayContext context) {
        if (owner == null || !owner.mainCannon) return false;
        boolean right = context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND;
        return right == (owner.rightHanded);
    }

    private static void drawSuit(PoseStack pose, MultiBufferSource buffers, float t, int light, int overlay, SuitItem suit) {
        ArmorType type = suit.getType();
        // Worn art is Y-down and faces -Z; flip it upright and toward the viewer.
        pose.mulPose(Axis.XP.rotationDegrees(180));
        float fit = type == ArmorType.CHESTPLATE ? 0.044F : type == ArmorType.HELMET ? 0.062F : 0.056F;
        pose.scale(fit, fit, fit);
        SuitArt.State idle = SuitArt.State.IDLE;
        switch (type) {
            case HELMET -> {
                pose.translate(0, 4.8, 0);
                Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(suit.set), false, c -> SuitArt.draw(c, suit.set, Piece.HELMET, idle));
            }
            case CHESTPLATE -> {
                pose.translate(0, -4.5, 0);
                Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(suit.set), false, c -> {
                    SuitArt.draw(c, suit.set, Piece.TORSO, idle);
                    c.pose.pushPose();
                    c.pose.translate(-7, 2, 0);
                    SuitArt.draw(c, suit.set, Piece.ARM_R, idle);
                    c.pose.translate(14, 0, 0);
                    SuitArt.draw(c, suit.set, Piece.ARM_L, idle);
                    c.pose.popPose();
                });
            }
            case LEGGINGS -> {
                pose.translate(0, -16, 0);
                Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(suit.set), false, c -> {
                    SuitArt.draw(c, suit.set, Piece.WAIST, idle);
                    c.pose.pushPose();
                    c.pose.translate(-2.3, 12, 0);
                    SuitArt.draw(c, suit.set, Piece.LEG_R, idle);
                    c.pose.translate(4.6, 0, 0);
                    SuitArt.draw(c, suit.set, Piece.LEG_L, idle);
                    c.pose.popPose();
                });
            }
            default -> {
                pose.translate(0, -10, 0);
                Canvas.drawMaterial(pose, buffers, t, light, overlay, SuitArt.material(suit.set), false, c -> {
                    c.pose.pushPose();
                    c.pose.translate(-3.4, 0, 0);
                    SuitArt.draw(c, suit.set, Piece.BOOT_R, idle);
                    c.pose.translate(6.8, 0, 0);
                    SuitArt.draw(c, suit.set, Piece.BOOT_L, idle);
                    c.pose.popPose();
                });
            }
        }
    }
}

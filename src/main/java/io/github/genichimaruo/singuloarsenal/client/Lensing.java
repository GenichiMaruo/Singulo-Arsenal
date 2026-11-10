package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import net.minecraft.world.phys.Vec3;

/**
 * Singulo's own gravitational lens, so a black hole here bends the view exactly as one there does and obeys the same client setting.
 * This addon does not compile against Singulo, so the two public entry points are found at runtime; if they ever move, holes simply stop
 * bending light. Lenses must be added before Singulo's AFTER_LEVEL handler runs.
 */
final class Lensing {
    private static final MethodHandle HOLE, DISTORTION;

    static {
        MethodHandle hole = null, distortion = null;
        try {
            Class<?> lens = Class.forName("io.github.genichimaruo.singulo.client.GravitationalLensing");
            MethodType type = MethodType.methodType(void.class, Vec3.class, float.class, float.class, float.class);
            hole = MethodHandles.publicLookup().findStatic(lens, "add", type);
            distortion = MethodHandles.publicLookup().findStatic(lens, "addDistortion", type);
        } catch (ReflectiveOperationException | LinkageError e) {
            LogUtils.getLogger().warn("Singulo's gravitational lens is unavailable; held black holes will not bend light", e);
        }
        HOLE = hole;
        DISTORTION = distortion;
    }

    private Lensing() {}

    /** A black hole's lens. {@code shadow} is the radius of the black disc it shows, in blocks. */
    static void hole(Vec3 at, float shadow, float region) {
        // Singulo's lens takes the Einstein radius, 2.2 horizons; its shadow is 2.598 horizons.
        call(HOLE, at, shadow / 2.598F * 2.2F, 1F, region);
    }

    /** Bends what is behind it, with no black centre: for a hole sealed inside a weapon. */
    static void distortion(Vec3 at, float radius, float strength, float region) {
        call(DISTORTION, at, radius, strength, region);
    }

    private static void call(MethodHandle handle, Vec3 at, float radius, float strength, float region) {
        if (handle == null) return;
        try {
            handle.invokeExact(at, radius, strength, region);
        } catch (Throwable e) {
            LogUtils.getLogger().warn("Could not add a gravitational lens", e);
        }
    }
}

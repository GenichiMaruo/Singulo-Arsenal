package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/** Optional Iris public API. No hard loader or production dependency. */
final class ShaderCompat {
    private static MethodHandle active, shadow;
    static { discover(); }
    private ShaderCompat() {}

    private static void discover() {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            active = MethodHandles.publicLookup().unreflect(api.getMethod("isShaderPackInUse")).bindTo(instance);
            shadow = MethodHandles.publicLookup().unreflect(api.getMethod("isRenderingShadowPass")).bindTo(instance);
        } catch (ClassNotFoundException absent) {
            // Iris is optional.
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            active = shadow = null;
            LogUtils.getLogger().warn("Iris API unavailable; using ordinary jet rendering", incompatible);
        }
    }

    static boolean active() { return query(active); }
    static boolean shadowPass() { return query(shadow); }

    private static boolean query(MethodHandle method) {
        if (method == null) return false;
        try {
            return (boolean) method.invokeExact();
        } catch (VirtualMachineError | ThreadDeath fatal) {
            throw fatal;
        } catch (Throwable incompatible) {
            active = shadow = null; // Do not fail every frame on an incompatible optional API.
            LogUtils.getLogger().warn("Iris API failed; using ordinary jet rendering", incompatible);
            return false;
        }
    }
}

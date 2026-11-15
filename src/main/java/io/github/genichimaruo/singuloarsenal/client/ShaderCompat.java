package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.logging.LogUtils;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/** Optional Iris public API. No hard loader or production dependency. */
public final class ShaderCompat {
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

    /** Public Iris 26 pipeline API; preserve our additive blend and depth policy. */
    public static void registerPositionColorPipeline(com.mojang.blaze3d.pipeline.RenderPipeline pipeline) {
        try {
            Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
            Object instance = api.getMethod("getInstance").invoke(null);
            Class<?> program = Class.forName("net.irisshaders.iris.api.v0.IrisProgram");
            Class<?> shadowProgram = Class.forName("net.irisshaders.iris.api.v0.IrisShadowProgram");
            api.getMethod("assignPipeline", com.mojang.blaze3d.pipeline.RenderPipeline.class, program)
                    .invoke(instance, pipeline, program.getField("BASIC").get(null));
            api.getMethod("assignPipelineShadow", com.mojang.blaze3d.pipeline.RenderPipeline.class, shadowProgram)
                    .invoke(instance, pipeline, shadowProgram.getField("SHADOW_LIGHTNING").get(null));
        } catch (ClassNotFoundException absent) {
            // Optional dependency: ordinary vanilla pipelines remain fully functional.
        } catch (ReflectiveOperationException | LinkageError incompatible) {
            LogUtils.getLogger().warn("Iris custom pipeline registration unavailable for {}", pipeline.getLocation(), incompatible);
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

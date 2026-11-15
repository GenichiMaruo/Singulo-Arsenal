package io.github.genichimaruo.singuloarsenal.client.art;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.ColorTargetState;
import com.mojang.blaze3d.pipeline.DepthStencilState;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.CompareOp;
import com.mojang.blaze3d.shaders.UniformType;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderSetup;
import net.neoforged.neoforge.client.event.RegisterRenderPipelinesEvent;

/** Additive light; no depth writes. Only the SQUID markers ignore depth. */
public final class ArsenalRenderTypes {
    private static final RenderPipeline GLOW_PIPELINE = pipeline("glow", CompareOp.LESS_THAN_OR_EQUAL);
    private static final RenderPipeline XRAY_PIPELINE = pipeline("xray", CompareOp.ALWAYS_PASS);
    public static final RenderType GLOW = RenderType.create("singulo_arsenal_glow", RenderSetup.builder(GLOW_PIPELINE).createRenderSetup());
    public static final RenderType XRAY = RenderType.create("singulo_arsenal_xray", RenderSetup.builder(XRAY_PIPELINE).createRenderSetup());
    private ArsenalRenderTypes() {}
    private static RenderPipeline pipeline(String name, CompareOp depth) {
        return RenderPipeline.builder()
                .withLocation(net.minecraft.resources.Identifier.parse("singulo_arsenal:pipeline/" + name))
                .withVertexShader("core/rendertype_lightning").withFragmentShader("core/rendertype_lightning")
                .withUniform("DynamicTransforms", UniformType.UNIFORM_BUFFER)
                .withUniform("Projection", UniformType.UNIFORM_BUFFER)
                .withUniform("Fog", UniformType.UNIFORM_BUFFER)
                .withColorTargetState(new ColorTargetState(BlendFunction.LIGHTNING))
                .withDepthStencilState(new DepthStencilState(depth, false)).withCull(false)
                .withVertexFormat(DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS).build();
    }
    public static void register(RegisterRenderPipelinesEvent event) {
        event.registerPipeline(GLOW_PIPELINE);
        event.registerPipeline(XRAY_PIPELINE);
        io.github.genichimaruo.singuloarsenal.client.ShaderCompat.registerPositionColorPipeline(GLOW_PIPELINE);
        io.github.genichimaruo.singuloarsenal.client.ShaderCompat.registerPositionColorPipeline(XRAY_PIPELINE);
    }
}

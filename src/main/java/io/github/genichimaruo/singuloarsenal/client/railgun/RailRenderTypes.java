package io.github.genichimaruo.singuloarsenal.client.railgun;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/** Additive light with depth testing but no depth writes: halos cannot erase nearby geometry. */
final class RailRenderTypes extends RenderType {
    static final RenderType GLOW = create("singulo_arsenal_rail_light", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 65536, false, false,
            CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));
    private RailRenderTypes() { super("unused",DefaultVertexFormat.POSITION_COLOR,VertexFormat.Mode.QUADS,256,false,false,()->{},()->{}); }
}

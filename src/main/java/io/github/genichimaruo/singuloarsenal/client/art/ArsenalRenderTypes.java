package io.github.genichimaruo.singuloarsenal.client.art;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.RenderType;

/** Additive light with depth tests but no depth writes, so halos cannot erase nearby geometry. */
public final class ArsenalRenderTypes extends RenderType {
    public static final RenderType GLOW = create("singulo_arsenal_glow", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 65536, false, false,
            CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL).setDepthTestState(LEQUAL_DEPTH_TEST).createCompositeState(false));

    /** Used only for the Meissner helmet's SQUID markers, which are meant to show through walls. */
    public static final RenderType XRAY = create("singulo_arsenal_xray", DefaultVertexFormat.POSITION_COLOR,
            VertexFormat.Mode.QUADS, 8192, false, false,
            CompositeState.builder().setShaderState(RENDERTYPE_LIGHTNING_SHADER)
                    .setTransparencyState(LIGHTNING_TRANSPARENCY).setWriteMaskState(COLOR_WRITE)
                    .setCullState(NO_CULL).setDepthTestState(NO_DEPTH_TEST).createCompositeState(false));

    private ArsenalRenderTypes() {
        super("unused", DefaultVertexFormat.POSITION_COLOR, VertexFormat.Mode.QUADS, 256, false, false, () -> {}, () -> {});
    }
}

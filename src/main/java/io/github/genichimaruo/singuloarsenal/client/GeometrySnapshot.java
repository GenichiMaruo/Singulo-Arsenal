package io.github.genichimaruo.singuloarsenal.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;

/** Copies procedural geometry during extraction; deferred rendering never reads live game objects. */
final class GeometrySnapshot implements MultiBufferSource {
    private final Map<RenderType, Recorder> batches = new LinkedHashMap<>();
    @Override public VertexConsumer getBuffer(RenderType type) { return batches.computeIfAbsent(type, ignored -> new Recorder()); }
    void submit(PoseStack pose, SubmitNodeCollector collector) {
        submit(pose, collector, 0, 0);
    }
    void submit(PoseStack pose, SubmitNodeCollector collector, int light, int overlay) {
        batches.forEach((type, recorder) -> {
            List<Vertex> vertices = List.copyOf(recorder.vertices);
            collector.submitCustomGeometry(pose, type, (transform, out) -> {
                for (Vertex v : vertices) {
                    out.addVertex(transform, v.x, v.y, v.z).setColor(v.color).setUv(v.u, v.v)
                            .setOverlay(v.overlay == -1 ? overlay : v.overlay).setLight(v.light == -1 ? light : v.light).setNormal(transform, v.nx, v.ny, v.nz).setLineWidth(v.width);
                }
            });
        });
    }
    void replay(PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        batches.forEach((type, recorder) -> {
            VertexConsumer out = buffers.getBuffer(type);
            for (Vertex v : recorder.vertices) {
                out.addVertex(pose.last(), v.x, v.y, v.z).setColor(v.color).setUv(v.u, v.v)
                    .setOverlay(v.overlay == -1 ? overlay : v.overlay).setLight(v.light == -1 ? light : v.light)
                    .setNormal(pose.last(), v.nx, v.ny, v.nz).setLineWidth(v.width);
            }
        });
    }    private static final class Vertex {
        float x,y,z,u,v,nx,ny,nz,width = 1;
        int color = -1, overlay, light;
        Vertex(float x,float y,float z) { this.x=x;this.y=y;this.z=z; }
    }
    private static final class Recorder implements VertexConsumer {
        final List<Vertex> vertices = new ArrayList<>();
        Vertex current;
        @Override public VertexConsumer addVertex(float x,float y,float z) { current=new Vertex(x,y,z);vertices.add(current);return this; }
        @Override public VertexConsumer setColor(int r,int g,int b,int a) { return setColor(a<<24|r<<16|g<<8|b); }
        @Override public VertexConsumer setColor(int color) { current.color=color;return this; }
        @Override public VertexConsumer setUv(float u,float v) { current.u=u;current.v=v;return this; }
        @Override public VertexConsumer setUv1(int u,int v) { current.overlay=u|v<<16;return this; }
        @Override public VertexConsumer setUv2(int u,int v) { current.light=u|v<<16;return this; }
        @Override public VertexConsumer setNormal(float x,float y,float z) { current.nx=x;current.ny=y;current.nz=z;return this; }
        @Override public VertexConsumer setLineWidth(float width) { current.width=width;return this; }
    }
}

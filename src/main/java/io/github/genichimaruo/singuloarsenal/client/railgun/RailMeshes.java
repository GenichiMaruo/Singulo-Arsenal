package io.github.genichimaruo.singuloarsenal.client.railgun;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.phys.Vec3;

/** Actual surface geometry: shaded gun panels, cylindrical beam cores and expanding annular shock fronts. */
final class RailMeshes {
    private RailMeshes() {}
    static Vec3 perpendicular(Vec3 axis) {
        return axis.cross(Math.abs(axis.y) > 0.95 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0)).normalize();
    }
    static void vertex(PoseStack pose, VertexConsumer out, Vec3 p, int rgb, float alpha) {
        out.addVertex(pose.last().pose(), (float)p.x, (float)p.y, (float)p.z)
                .setColor((rgb >> 16) & 255, (rgb >> 8) & 255, rgb & 255, Math.max(0, Math.min(255, (int)(alpha * 255))));
    }
    static void quad(PoseStack pose, VertexConsumer out, Vec3 a, Vec3 b, Vec3 c, Vec3 d, int rgb, float alpha) {
        vertex(pose,out,a,rgb,alpha); vertex(pose,out,b,rgb,alpha); vertex(pose,out,c,rgb,alpha); vertex(pose,out,d,rgb,alpha);
    }
    static void beam(PoseStack pose, VertexConsumer out, Vec3 start, Vec3 end, double radius, int rgb, float alpha) {
        Vec3 delta = end.subtract(start);
        if (delta.lengthSqr() < 0.00001 || radius <= 0 || alpha <= 0) return;
        Vec3 axis=delta.normalize(), u=perpendicular(axis), v=axis.cross(u);
        for(int i=0;i<12;i++) {
            double a=i*Math.PI/6,b=(i+1)*Math.PI/6;
            Vec3 r1=u.scale(Math.cos(a)*radius).add(v.scale(Math.sin(a)*radius));
            Vec3 r2=u.scale(Math.cos(b)*radius).add(v.scale(Math.sin(b)*radius));
            quad(pose,out,start.add(r1),end.add(r1),end.add(r2),start.add(r2),rgb,alpha);
        }
    }
    /** Soft edge is geometry with alpha interpolation; no screen-space particle sprites. */
    static void ring(PoseStack pose, VertexConsumer out, Vec3 center, Vec3 axis, double radius, double width, int rgb, float alpha, double phase) {
        Vec3 u=perpendicular(axis),v=axis.cross(u).normalize();
        for(int i=0;i<64;i++) {
            double a=i*Math.PI/32+phase,b=(i+1)*Math.PI/32+phase;
            Vec3 p=u.scale(Math.cos(a)).add(v.scale(Math.sin(a))),q=u.scale(Math.cos(b)).add(v.scale(Math.sin(b)));
            vertex(pose,out,center.add(p.scale(Math.max(0,radius-width))),rgb,0);
            vertex(pose,out,center.add(q.scale(Math.max(0,radius-width))),rgb,0);
            vertex(pose,out,center.add(q.scale(radius)),rgb,alpha);
            vertex(pose,out,center.add(p.scale(radius)),rgb,alpha);
            vertex(pose,out,center.add(p.scale(radius)),rgb,alpha);
            vertex(pose,out,center.add(q.scale(radius)),rgb,alpha);
            vertex(pose,out,center.add(q.scale(radius+width)),rgb,0);
            vertex(pose,out,center.add(p.scale(radius+width)),rgb,0);
        }
    }
    static void helix(PoseStack pose, VertexConsumer out, Vec3 start, Vec3 end, double radius, double phase, float alpha) {
        Vec3 delta=end.subtract(start),axis=delta.normalize(),u=perpendicular(axis),v=axis.cross(u);
        int segments=Math.min(256,Math.max(8,(int)(delta.length()*6)));
        Vec3 previous=null;
        for(int i=0;i<=segments;i++) {
            double t=i/(double)segments,angle=t*delta.length()*2.8+phase;
            Vec3 at=start.add(delta.scale(t)).add(u.scale(Math.cos(angle)*radius)).add(v.scale(Math.sin(angle)*radius));
            if(previous!=null) beam(pose,out,previous,at,0.009,0xA1FFF1,alpha);
            previous=at;
        }
    }
    static void box(PoseStack pose, VertexConsumer out, float x0,float y0,float z0,float x1,float y1,float z1,int rgb,int light) {
        face(pose,out,new float[][]{{x0,y0,z0},{x0,y1,z0},{x1,y1,z0},{x1,y0,z0}},0,0,-1,rgb,light);
        face(pose,out,new float[][]{{x1,y0,z1},{x1,y1,z1},{x0,y1,z1},{x0,y0,z1}},0,0,1,rgb,light);
        face(pose,out,new float[][]{{x0,y0,z1},{x0,y1,z1},{x0,y1,z0},{x0,y0,z0}},-1,0,0,rgb,light);
        face(pose,out,new float[][]{{x1,y0,z0},{x1,y1,z0},{x1,y1,z1},{x1,y0,z1}},1,0,0,rgb,light);
        face(pose,out,new float[][]{{x0,y1,z0},{x0,y1,z1},{x1,y1,z1},{x1,y1,z0}},0,1,0,rgb,light);
        face(pose,out,new float[][]{{x0,y0,z1},{x0,y0,z0},{x1,y0,z0},{x1,y0,z1}},0,-1,0,rgb,light);
    }
    private static void face(PoseStack pose,VertexConsumer out,float[][] corners,float nx,float ny,float nz,int rgb,int light) {
        for(int i=0;i<4;i++) out.addVertex(pose.last().pose(),corners[i][0],corners[i][1],corners[i][2])
                .setColor((rgb>>16)&255,(rgb>>8)&255,rgb&255,255).setUv(i==0||i==3?0:1,i<2?0:1)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(pose.last(),nx,ny,nz);
    }
}

package io.github.genichimaruo.singuloarsenal.client;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/** No game instance required: the same world point must project identically in
 * vanilla and shader hand pipelines, including non-identity bob transforms. */
public final class RenderSpaceTest {
    @org.junit.jupiter.api.Test
    void projectionIsIndependentOfPipelineBasis() {
        int checks = 0;
        for (int yaw = -180; yaw <= 180; yaw += 30) {
            for (int pitch = -80; pitch <= 80; pitch += 20) {
                Quaternionf camera = new Quaternionf().rotationYXZ(
                        (float)Math.toRadians(yaw), (float)Math.toRadians(pitch), 0);
                Matrix4f view = new Matrix4f().rotation(new Quaternionf(camera).conjugate());
                for (Matrix4f modelView : new Matrix4f[] {view, new Matrix4f(),
                        new Matrix4f().translate(.03F, -.02F, .01F).rotateZ(.08F)}) {
                    Matrix4f basis = RenderSpace.worldToPose(modelView, camera);
                    for (Vector3f world : new Vector3f[] {new Vector3f(0,0,-40),
                            new Vector3f(18,3,-6), new Vector3f(-9,-5,12)}) {
                        Vector3f expected = view.transformPosition(new Vector3f(world));
                        Vector3f actual = modelView.transformPosition(basis.transformPosition(new Vector3f(world)));
                        if (expected.distance(actual) > .0001F) throw new AssertionError("basis mismatch");
                        Vector3f roundTrip = new Matrix4f(basis).invert().transformPosition(
                                basis.transformPosition(new Vector3f(world)));
                        if (roundTrip.distance(world) > .0001F) throw new AssertionError("muzzle round trip");
                        checks += 2;
                    }
                }
            }
        }
        System.out.println("Render-space checks passed: " + checks);
    }
}

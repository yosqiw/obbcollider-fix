package com.yowils.obbcolliderfix.mixin;

import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;

@Mixin(ContinuousOBBCollider.class)
public class ContinuousOBBColliderMixin {

    private static final MethodHandle AXIS_GETTER = createFieldGetter("axis");
    private static final MethodHandle NORMAL_AXIS_GETTER = createFieldGetter("normalAxis");

    private static MethodHandle createFieldGetter(String fieldName) {
        try {
            Class<?> manifoldClass = Class.forName(
                "com.simibubi.create.foundation.collision.ContinuousOBBCollider$ContinuousSeparationManifold"
            );
            Field field = manifoldClass.getDeclaredField(fieldName);
            field.setAccessible(true);
            return MethodHandles.lookup().unreflectGetter(field);
        } catch (Throwable t) {
            throw new RuntimeException(
                "obbcollider_fix: failed to bootstrap accessor for field '" + fieldName + "'", t
            );
        }
    }

    /**
     * Substitutes Vec3.ZERO for null when collideMany reads mf.axis. Without
     * this fix, a degenerate rotation matrix (typically from a Sable sub-level
     * transform) leaves mf.axis unpopulated even though the manifold is in a
     * "discrete collision" state, and the unconditional dereference NPEs.
     */
    @Redirect(
        method = "collideMany",
        at = @At(
            value = "FIELD",
            target = "Lcom/simibubi/create/foundation/collision/ContinuousOBBCollider$ContinuousSeparationManifold;axis:Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private static Vec3 obbcolliderfix$nullSafeAxis(@Coerce Object mf) {
        try {
            Vec3 axis = (Vec3) AXIS_GETTER.invoke(mf);
            return axis == null ? Vec3.ZERO : axis;
        } catch (Throwable t) {
            return Vec3.ZERO;
        }
    }

    /**
     * Same fix applied to mf.normalAxis. Both fields are populated by the
     * same SAT separation logic and can be left null in the same degenerate
     * cases.
     */
    @Redirect(
        method = "collideMany",
        at = @At(
            value = "FIELD",
            target = "Lcom/simibubi/create/foundation/collision/ContinuousOBBCollider$ContinuousSeparationManifold;normalAxis:Lnet/minecraft/world/phys/Vec3;"
        )
    )
    private static Vec3 obbcolliderfix$nullSafeNormalAxis(@Coerce Object mf) {
        try {
            Vec3 axis = (Vec3) NORMAL_AXIS_GETTER.invoke(mf);
            return axis == null ? Vec3.ZERO : axis;
        } catch (Throwable t) {
            return Vec3.ZERO;
        }
    }
}
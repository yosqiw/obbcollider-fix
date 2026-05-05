package com.yourname.obbcolliderfix.mixin;

import com.simibubi.create.foundation.collision.ContinuousOBBCollider;
import com.simibubi.create.foundation.collision.ContinuousSeparationManifold;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(ContinuousOBBCollider.class)
public class ContinuousOBBColliderMixin {

    /**
     * Redirects the read of {@code mf.axis} on the line that NPEs. If axis is null
     * (manifold reached discrete-collision state without primary axis being populated,
     * typically due to a degenerate rotation matrix from a Sable sub-level transform),
     * we substitute Vec3.ZERO. The downstream multiplications then contribute nothing
     * to the collision response, which is the correct behaviour for a manifold with
     * no resolvable axis.
     *
     * The bug: in collideMany, the discrete-collision-but-too-large-step branch reads
     * mf.axis unconditionally; if mf.isDiscreteCollision was set without populating
     * mf.axis (the parallel field stepSeparationAxis was populated instead), this NPEs.
     */
    @Redirect(
        method = "collideMany",
        at = @At(
            value = "FIELD",
            target = "Lcom/simibubi/create/foundation/collision/ContinuousSeparationManifold;axis:Lnet/minecraft/world/phys/Vec3;",
            opcode = org.objectweb.asm.Opcodes.GETFIELD
        )
    )
    private static net.minecraft.world.phys.Vec3 obbcolliderfix$nullSafeAxis(ContinuousSeparationManifold mf) {
        return mf.axis == null ? net.minecraft.world.phys.Vec3.ZERO : mf.axis;
    }
}
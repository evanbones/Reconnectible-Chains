package com.evandev.connectiblechains.compat.sable;

//? if <26.1 {
/*import dev.ryanhcode.sable.companion.ClientSubLevelAccess;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import dev.ryanhcode.sable.companion.math.BoundingBox3d;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

public final class SableHelper {

    private SableHelper() {
    }

    public static double distanceToCameraSqr(Entity entity, EntityRenderDispatcher dispatcher) {
        Vec3 camPos = dispatcher.camera.getPosition();
        return SableCompanion.INSTANCE.distanceSquaredWithSubLevels(entity.level(), camPos, entity.position());
    }

    @Nullable
    public static SubLevelAccess getSubLevel(Level level, @Nullable Entity entity, @Nullable Vec3 fallbackPos) {
        SubLevelAccess subLevel = entity != null ? SableCompanion.INSTANCE.getContaining(entity) : null;
        return subLevel != null ? subLevel : (fallbackPos != null ? SableCompanion.INSTANCE.getContaining(level, fallbackPos) : null);
    }

    public static Vec3 transformHolderPosForRenderer(Level level, Entity entity, Entity chainHolder, Vec3 dstPos, float tickDelta) {
        SubLevelAccess entitySubLevel = getSubLevel(level, entity, null);
        SubLevelAccess holderSubLevel = getSubLevel(level, chainHolder, dstPos);
        if (entitySubLevel == holderSubLevel) {
            return dstPos;
        }

        Vec3 globalPos = dstPos;
        if (holderSubLevel instanceof ClientSubLevelAccess clientHolder) {
            globalPos = clientHolder.renderPose(tickDelta).transformPosition(globalPos);
        } else if (holderSubLevel != null) {
            globalPos = holderSubLevel.logicalPose().transformPosition(globalPos);
        }

        if (entitySubLevel instanceof ClientSubLevelAccess clientEntity) {
            return clientEntity.renderPose(tickDelta).transformPositionInverse(globalPos);
        } else if (entitySubLevel != null) {
            return entitySubLevel.logicalPose().transformPositionInverse(globalPos);
        }

        return globalPos;
    }

    public static Vec3 getHolderPosInEntitySpace(Level level, Entity entity, Entity chainHolder) {
        SubLevelAccess entitySubLevel = getSubLevel(level, entity, null);
        SubLevelAccess holderSubLevel = getSubLevel(level, chainHolder, chainHolder != null ? chainHolder.position() : null);
        if (entitySubLevel == holderSubLevel) {
            return chainHolder != null ? chainHolder.position() : Vec3.ZERO;
        }

        Vec3 globalPos = chainHolder != null ? chainHolder.position() : Vec3.ZERO;
        if (holderSubLevel != null) {
            globalPos = holderSubLevel.logicalPose().transformPosition(globalPos);
        }

        if (entitySubLevel != null) {
            return entitySubLevel.logicalPose().transformPositionInverse(globalPos);
        }

        return globalPos;
    }

    public static Iterable<? extends SubLevelAccess> getAllIntersecting(Level level, AABB aabb) {
        return SableCompanion.INSTANCE.getAllIntersecting(level, new BoundingBox3d(aabb));
    }

    public static AABB toLocalAABB(SubLevelAccess subLevel, AABB aabb) {
        return new BoundingBox3d(aabb).transformInverse(subLevel.logicalPose()).toMojang();
    }

    public static AABB toGlobalAABB(SubLevelAccess subLevel, AABB aabb) {
        return new BoundingBox3d(aabb).transform(subLevel.logicalPose()).toMojang();
    }

    @Nullable
    public static SubLevelAccess getContaining(Level level, Vec3 pos) {
        return SableCompanion.INSTANCE.getContaining(level, pos);
    }

    public static Vec3 projectOutOfSubLevel(Level level, Vec3 pos) {
        return SableCompanion.INSTANCE.projectOutOfSubLevel(level, (net.minecraft.core.Position) pos);
    }
}
*///?}

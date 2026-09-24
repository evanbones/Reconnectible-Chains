package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.CommonClass;
import com.evandev.connectiblechains.compat.sable.SableHelper;
import com.evandev.connectiblechains.entity.ChainKnotEntity;
import com.evandev.connectiblechains.entity.Chainable;
import dev.ryanhcode.sable.companion.SubLevelAccess;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public final class ChainCollisionIndex {

    private static final Map<Level, ChainCollisionIndex> INDICES = Collections.synchronizedMap(new WeakHashMap<>());
    private static final double POSITION_EPSILON = 1.0E-8;
    private static final long LEASE_TICKS = 3;

    private final Map<Integer, Map<Integer, Entry>> byOwner = new ConcurrentHashMap<>();
    private final Map<Long, CopyOnWriteArrayList<Entry>> buckets = new ConcurrentHashMap<>();

    private ChainCollisionIndex() {
    }

    private static ChainCollisionIndex of(Level level) {
        return INDICES.computeIfAbsent(level, k -> new ChainCollisionIndex());
    }

    public static Vec3 chainAnchor(Entity entity) {
        if (entity instanceof ChainKnotEntity knot) return knot.getChainPos(1.0f);
        return entity.getLeashOffset(1.0f).add(entity.position());
    }

    public static void ensure(Level level, Entity owner, Entity holder, Chainable.ChainData chainData) {
        if (!CommonClass.runtimeConfig.isCollisionsEnabled()) return;

        ChainCollisionIndex index = of(level);
        long now = level.getGameTime();
        int ownerId = owner.getId();
        int holderId = holder.getId();

        Vec3 src = chainAnchor(owner);
        Vec3 dst = chainAnchor(holder);

        SubLevelAccess srcSubLevel = SableHelper.getContaining(level, src);
        SubLevelAccess dstSubLevel = SableHelper.getContaining(level, dst);
        if (srcSubLevel != dstSubLevel) {
            src = SableHelper.projectOutOfSubLevel(level, src);
            dst = SableHelper.projectOutOfSubLevel(level, dst);
        }

        if (!Chainable.isValidChainDistance(src, dst)) {
            return;
        }
        float slack = chainData.getSlack();
        int hangingsHash = hangingsHash(chainData);

        Map<Integer, Entry> owned = index.byOwner.get(ownerId);
        Entry existing = owned == null ? null : owned.get(holderId);
        if (existing != null
                && existing.slack == slack
                && existing.hangingsHash == hangingsHash
                && existing.src.distanceToSqr(src) < POSITION_EPSILON
                && existing.dst.distanceToSqr(dst) < POSITION_EPSILON) {
            existing.lastSeenTick = now;
            return;
        }

        index.unlink(ownerId, holderId);

        ChainShapeBaker.ChainShape shape = ChainShapeBaker.bake(src, dst, slack, hangingBoxes(level, src, dst, slack, chainData));
        if (shape == null) return;

        long[] bucketKeys = bucketKeysFor(shape.bounds());
        Entry entry = new Entry(owner, holder, shape, src, dst, slack, hangingsHash, bucketKeys, now);
        index.byOwner.computeIfAbsent(ownerId, k -> new ConcurrentHashMap<>()).put(holderId, entry);
        for (long bucketKey : bucketKeys) {
            index.buckets.computeIfAbsent(bucketKey, k -> new CopyOnWriteArrayList<>()).add(entry);
        }
    }

    public static void removeAllOwnedBy(Level level, Entity owner) {
        ChainCollisionIndex index = INDICES.get(level);
        if (index == null) return;

        Map<Integer, Entry> owned = index.byOwner.remove(owner.getId());
        if (owned == null) return;
        owned.values().forEach(index::detachFromBuckets);
    }

    public static void clearAll() {
        synchronized (INDICES) {
            INDICES.values().forEach(index -> {
                index.byOwner.clear();
                index.buckets.clear();
            });
        }
    }

    @Nullable
    public static List<VoxelShape> collect(Level level, AABB area) {
        ChainCollisionIndex index = INDICES.get(level);
        if (index == null || index.buckets.isEmpty()) return null;

        long now = level.getGameTime();
        List<VoxelShape> collected = null;

        int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(area.minX));
        int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(area.maxX));
        int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(area.minZ));
        int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(area.maxZ));

        long diffX = (long) maxChunkX - minChunkX + 1;
        long diffZ = (long) maxChunkZ - minChunkZ + 1;

        if (diffX > 0 && diffZ > 0 && diffX <= 32 && diffZ <= 32 && diffX * diffZ <= 512) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    CopyOnWriteArrayList<Entry> bucket = index.buckets.get(ChunkPos.asLong(chunkX, chunkZ));
                    if (bucket == null) continue;

                    for (Entry entry : bucket) {
                        if (entry.isStale(now)) {
                            index.discard(entry);
                            continue;
                        }

                        ChainShapeBaker.ChainShape shape = entry.shape;
                        if (!shape.bounds().intersects(area)) continue;

                        AABB[] boxes = shape.boxes();
                        for (int i = 0; i < boxes.length; i++) {
                            if (!boxes[i].intersects(area)) continue;
                            if (collected == null) collected = new ArrayList<>();
                            collected.add(shape.shapes()[i]);
                        }
                    }
                }
            }
        }

        for (SubLevelAccess subLevel : SableHelper.getAllIntersecting(level, area)) {
            AABB localArea = SableHelper.toLocalAABB(subLevel, area);
            int subMinChunkX = SectionPos.blockToSectionCoord(Mth.floor(localArea.minX));
            int subMaxChunkX = SectionPos.blockToSectionCoord(Mth.floor(localArea.maxX));
            int subMinChunkZ = SectionPos.blockToSectionCoord(Mth.floor(localArea.minZ));
            int subMaxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(localArea.maxZ));

            long subDiffX = (long) subMaxChunkX - subMinChunkX + 1;
            long subDiffZ = (long) subMaxChunkZ - subMinChunkZ + 1;
            if (subDiffX <= 0 || subDiffZ <= 0 || subDiffX > 32 || subDiffZ > 32 || subDiffX * subDiffZ > 512) {
                continue;
            }

            for (int chunkX = subMinChunkX; chunkX <= subMaxChunkX; chunkX++) {
                for (int chunkZ = subMinChunkZ; chunkZ <= subMaxChunkZ; chunkZ++) {
                    CopyOnWriteArrayList<Entry> bucket = index.buckets.get(ChunkPos.asLong(chunkX, chunkZ));
                    if (bucket == null) continue;

                    for (Entry entry : bucket) {
                        if (entry.isStale(now)) {
                            index.discard(entry);
                            continue;
                        }

                        ChainShapeBaker.ChainShape shape = entry.shape;
                        if (!shape.bounds().intersects(localArea)) continue;

                        AABB[] boxes = shape.boxes();
                        for (AABB box : boxes) {
                            if (!box.intersects(localArea)) continue;
                            AABB globalBox = SableHelper.toGlobalAABB(subLevel, box);
                            if (globalBox.intersects(area)) {
                                if (collected == null) collected = new ArrayList<>();
                                collected.add(Shapes.create(globalBox));
                            }
                        }
                    }
                }
            }
        }

        return collected;
    }

    public static boolean intersects(Level level, AABB area) {
        return scan(level, area, false);
    }

    public static boolean intersectsHangingBlock(Level level, AABB area) {
        if (!CommonClass.runtimeConfig.isHangingBlockCollisionsEnabled()) return false;
        return scan(level, area, true);
    }

    private static boolean scan(Level level, AABB area, boolean hangingOnly) {
        ChainCollisionIndex index = INDICES.get(level);
        if (index == null || index.buckets.isEmpty()) return false;

        long now = level.getGameTime();

        int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(area.minX));
        int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(area.maxX));
        int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(area.minZ));
        int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(area.maxZ));

        long diffX = (long) maxChunkX - minChunkX + 1;
        long diffZ = (long) maxChunkZ - minChunkZ + 1;

        if (diffX > 0 && diffZ > 0 && diffX <= 32 && diffZ <= 32 && diffX * diffZ <= 512) {
            for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
                for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                    CopyOnWriteArrayList<Entry> bucket = index.buckets.get(ChunkPos.asLong(chunkX, chunkZ));
                    if (bucket == null) continue;

                    for (Entry entry : bucket) {
                        if (entry.isStale(now)) {
                            index.discard(entry);
                            continue;
                        }

                        ChainShapeBaker.ChainShape shape = entry.shape;
                        if (!shape.bounds().intersects(area)) continue;

                        AABB[] boxes = shape.boxes();
                        for (int i = hangingOnly ? shape.hangingFrom() : 0; i < boxes.length; i++) {
                            if (boxes[i].intersects(area)) return true;
                        }
                    }
                }
            }
        }

        for (SubLevelAccess subLevel : SableHelper.getAllIntersecting(level, area)) {
            AABB localArea = SableHelper.toLocalAABB(subLevel, area);
            int subMinChunkX = SectionPos.blockToSectionCoord(Mth.floor(localArea.minX));
            int subMaxChunkX = SectionPos.blockToSectionCoord(Mth.floor(localArea.maxX));
            int subMinChunkZ = SectionPos.blockToSectionCoord(Mth.floor(localArea.minZ));
            int subMaxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(localArea.maxZ));

            long subDiffX = (long) subMaxChunkX - subMinChunkX + 1;
            long subDiffZ = (long) subMaxChunkZ - subMinChunkZ + 1;
            if (subDiffX <= 0 || subDiffZ <= 0 || subDiffX > 32 || subDiffZ > 32 || subDiffX * subDiffZ > 512) {
                continue;
            }

            for (int chunkX = subMinChunkX; chunkX <= subMaxChunkX; chunkX++) {
                for (int chunkZ = subMinChunkZ; chunkZ <= subMaxChunkZ; chunkZ++) {
                    CopyOnWriteArrayList<Entry> bucket = index.buckets.get(ChunkPos.asLong(chunkX, chunkZ));
                    if (bucket == null) continue;

                    for (Entry entry : bucket) {
                        if (entry.isStale(now)) {
                            index.discard(entry);
                            continue;
                        }

                        ChainShapeBaker.ChainShape shape = entry.shape;
                        if (!shape.bounds().intersects(localArea)) continue;

                        AABB[] boxes = shape.boxes();
                        for (int i = hangingOnly ? shape.hangingFrom() : 0; i < boxes.length; i++) {
                            if (!boxes[i].intersects(localArea)) continue;
                            AABB globalBox = SableHelper.toGlobalAABB(subLevel, boxes[i]);
                            if (globalBox.intersects(area)) return true;
                        }
                    }
                }
            }
        }

        return false;
    }

    private static int hangingsHash(Chainable.ChainData chainData) {
        if (!CommonClass.runtimeConfig.isHangingBlockCollisionsEnabled()) return 0;
        int hash = 1;
        for (Chainable.ChainData.HangingEntry entry : chainData.hangings) {
            hash = hash * 31 + Float.floatToIntBits(entry.t());
            hash = hash * 31 + entry.blockId().hashCode();
        }
        return hash;
    }

    @Nullable
    private static List<AABB> hangingBoxes(Level level, Vec3 src, Vec3 dst, float slack, Chainable.ChainData chainData) {
        if (!CommonClass.runtimeConfig.isHangingBlockCollisionsEnabled()) return null;
        if (chainData.hangings.isEmpty()) return null;

        List<AABB> boxes = new ArrayList<>(chainData.hangings.size());
        for (Chainable.ChainData.HangingEntry entry : chainData.hangings) {
            BlockState state = HangingBlockPlacement.hangingState(entry.blockId());
            if (state == null) continue;

            Vec3 anchor = HangingBlockPlacement.anchor(src, dst, entry.t(), slack);
            AABB box = HangingBlockPlacement.occupiedBox(level, state, anchor);
            if (box != null) boxes.add(box);
        }
        return boxes.isEmpty() ? null : boxes;
    }

    private static long[] bucketKeysFor(AABB bounds) {
        int minChunkX = SectionPos.blockToSectionCoord(Mth.floor(bounds.minX));
        int maxChunkX = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxX));
        int minChunkZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.minZ));
        int maxChunkZ = SectionPos.blockToSectionCoord(Mth.floor(bounds.maxZ));

        long diffX = (long) maxChunkX - minChunkX + 1;
        long diffZ = (long) maxChunkZ - minChunkZ + 1;
        if (diffX <= 0 || diffZ <= 0 || diffX > 32 || diffZ > 32 || diffX * diffZ > 512) {
            return new long[0];
        }

        long[] keys = new long[(int) (diffX * diffZ)];
        int i = 0;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                keys[i++] = ChunkPos.asLong(chunkX, chunkZ);
            }
        }
        return keys;
    }

    private void unlink(int ownerId, int holderId) {
        Map<Integer, Entry> owned = byOwner.get(ownerId);
        if (owned == null) return;

        Entry previous = owned.remove(holderId);
        if (previous != null) detachFromBuckets(previous);
        byOwner.remove(ownerId, Collections.emptyMap());
    }

    private void discard(Entry entry) {
        Map<Integer, Entry> owned = byOwner.get(entry.owner.getId());
        if (owned != null) {
            owned.remove(entry.holder.getId(), entry);
            byOwner.remove(entry.owner.getId(), Collections.emptyMap());
        }
        detachFromBuckets(entry);
    }

    private void detachFromBuckets(Entry entry) {
        for (long bucketKey : entry.bucketKeys) {
            CopyOnWriteArrayList<Entry> bucket = buckets.get(bucketKey);
            if (bucket == null) continue;
            bucket.remove(entry);
            if (bucket.isEmpty()) buckets.remove(bucketKey, bucket);
        }
    }

    private static final class Entry {
        final Entity owner;
        final Entity holder;
        final ChainShapeBaker.ChainShape shape;
        final Vec3 src;
        final Vec3 dst;
        final float slack;
        final int hangingsHash;
        final long[] bucketKeys;
        volatile long lastSeenTick;

        Entry(Entity owner, Entity holder, ChainShapeBaker.ChainShape shape, Vec3 src, Vec3 dst, float slack,
              int hangingsHash, long[] bucketKeys, long lastSeenTick) {
            this.owner = owner;
            this.holder = holder;
            this.shape = shape;
            this.src = src;
            this.dst = dst;
            this.slack = slack;
            this.hangingsHash = hangingsHash;
            this.bucketKeys = bucketKeys;
            this.lastSeenTick = lastSeenTick;
        }

        boolean isStale(long now) {
            return owner.isRemoved() || holder.isRemoved() || now - lastSeenTick > LEASE_TICKS;
        }
    }
}

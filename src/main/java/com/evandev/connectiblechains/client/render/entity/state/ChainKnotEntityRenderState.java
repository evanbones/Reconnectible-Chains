package com.evandev.connectiblechains.client.render.entity.state;

import com.evandev.connectiblechains.entity.Chainable;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class ChainKnotEntityRenderState extends EntityRenderState {

    public final List<ChainData> chainDataSet = new ArrayList<>();
    private final List<ChainData> pool = new ArrayList<>();
    public Item sourceItem;
    public Direction attachedFace = Direction.UP;
    public int knotTintColor = 0xFFFFFFFF;
    public float knotScaleXZ = 5 / 6f;

    public void reset() {
        chainDataSet.clear();
    }

    public ChainData claim() {
        ChainData data;
        if (pool.size() > chainDataSet.size()) {
            data = pool.get(chainDataSet.size());
        } else {
            data = new ChainData();
            pool.add(data);
        }
        chainDataSet.add(data);
        return data;
    }

    public static class ChainData {
        public boolean useBaked;
        public Item sourceItem;
        public float slack;
        public Vec3 offset = Vec3.ZERO;
        public Vec3 startPos = Vec3.ZERO;
        public Vec3 endPos = Vec3.ZERO;
        public int chainedEntityBlockLight = 0;
        public int chainHolderBlockLight = 0;
        public int chainedEntitySkyLight = 15;
        public int chainHolderSkyLight = 15;
        public int tintColor = 0xFFCCCCCC;
        public List<Chainable.ChainData.BuntingEntry> buntings = List.of();
        public List<Chainable.ChainData.BannerEntry> banners = List.of();
        public List<Chainable.ChainData.HangingEntry> hangings = List.of();
    }
}

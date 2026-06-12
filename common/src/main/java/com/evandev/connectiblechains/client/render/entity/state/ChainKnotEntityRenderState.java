package com.evandev.connectiblechains.client.render.entity.state;

import com.evandev.connectiblechains.entity.Chainable;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class ChainKnotEntityRenderState {
    public HashSet<ChainData> chainDataSet = new HashSet<>();
    public Item sourceItem;

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
        public List<Chainable.ChainData.BuntingEntry> buntings = new ArrayList<>();
    }
}

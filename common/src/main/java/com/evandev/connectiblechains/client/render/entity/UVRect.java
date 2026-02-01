package com.evandev.connectiblechains.client.render.entity;

import com.mojang.serialization.Codec;
import com.mojang.datafixers.util.Pair;

import java.util.List;

public record UVRect(float x0, float x1) {
    public static final Codec<Pair<UVRect, UVRect>> CODEC = Codec.FLOAT.listOf().xmap(UVRect::to, UVRect::from);

    public static final UVRect DEFAULT_SIDE_A = new UVRect(0, 3);
    public static final UVRect DEFAULT_SIDE_B = new UVRect(3, 6);

    private static Pair<UVRect, UVRect> to(List<Float> floats) {
        return new Pair<>(new UVRect(floats.get(0), floats.get(1)), new UVRect(floats.get(2), floats.get(3)));
    }

    private static List<Float> from(Pair<UVRect, UVRect> uvRect) {
        return List.of(uvRect.getFirst().x0(), uvRect.getFirst().x1(), uvRect.getSecond().x0(), uvRect.getSecond().x1());
    }
}
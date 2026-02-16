package com.evandev.connectiblechains.util;

import com.evandev.connectiblechains.CommonClass;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

public class MathHelper {

    public static ResourceLocation identifier(String name) {
        return new ResourceLocation(CommonClass.MODID, name);
    }

    @Deprecated
    public static double drip(double x, double d) {
        double c = CommonClass.runtimeConfig.getChainHangAmount();
        double b = -c / d;
        double a = c / (d * d);
        return (a * (x * x) + b * x);
    }

    public static double drip2(double x, double d, double h, double slack) {
        double a = slack;
        a = a + (d * 0.3);
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        double p2 = -a * Math.cosh((2D * p1 - d) / (2D * a));
        return p2 + a * Math.cosh((((2D * x) + (2D * p1)) - d) / (2D * a));
    }

    private static double asinh(double x) {
        return Math.log(x + Math.sqrt(x * x + 1.0));
    }

    public static double drip2prime(double x, double d, double h, double slack) {
        double a = slack;
        double p1 = a * asinh((h / (2D * a)) * (1D / Math.sinh(d / (2D * a))));
        return Math.sinh((2 * x + 2 * p1 - d) / (2 * a));
    }

    public static Vec3 middleOf(Vec3 a, Vec3 b) {
        double x = (a.x() - b.x()) / 2d + b.x();
        double y = (a.y() - b.y()) / 2d + b.y();
        double z = (a.z() - b.z()) / 2d + b.z();
        return new Vec3(x, y, z);
    }

    @Deprecated
    public static Vec3 getChainOffset(Vec3 start, Vec3 end) {
        Vector3f offset = end.subtract(start).toVector3f();
        offset.set(offset.x(), 0, offset.z());
        offset.normalize();
        offset.normalize(2 / 16f);
        return new Vec3(offset);
    }
}
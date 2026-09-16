package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

/** Small, short-lived sparks kept close to inventory equipment icons. */
public class EquipmentSparkle extends PixelParticle {
    public static Emitter.Factory factory(final int[] colors) {
        return new Emitter.Factory() {
            @Override public void emit(Emitter emitter, int index, float x, float y) {
                EquipmentSparkle p = (EquipmentSparkle)emitter.recycle(EquipmentSparkle.class);
                p.revive(); p.x = x; p.y = y;
                p.color(colors[index % colors.length]);
                p.left = p.lifespan = .5f;
                p.speed.set(Random.Float(-2, 2), Random.Float(-5, -2));
                p.acc.set(0, 0); p.size(1);
            }
            @Override public boolean lightMode() { return true; }
        };
    }
    @Override public void update() {
        super.update();
        am = (float)Math.sin(Math.PI * left / lifespan);
        size(am > .7f ? 2 : 1);
    }
}

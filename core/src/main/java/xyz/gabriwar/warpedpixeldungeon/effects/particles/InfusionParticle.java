package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

/** Expanding colored sparks with a small spiral, used by magical coatings. */
public class InfusionParticle extends PixelParticle {
    private float spin;

    public static Emitter.Factory factory(final int color) {
        return new Emitter.Factory() {
            @Override public void emit(Emitter emitter, int index, float x, float y) {
                InfusionParticle p = (InfusionParticle) emitter.recycle(InfusionParticle.class);
                p.revive();
                p.x = x;
                p.y = y;
                p.color(color);
                p.lifespan = p.left = Random.Float(0.3f, 0.6f);
                p.speed.polar(index * PointF.PI2 / 12f, Random.Float(12f, 25f));
                p.spin = (index % 2 == 0 ? 1 : -1) * 30f;
                p.acc.set(-p.speed.y * 3, p.speed.x * 3);
                p.size(2f);
            }
            @Override public boolean lightMode() { return true; }
        };
    }

    @Override public void update() {
        super.update();
        am = left / lifespan;
        angularSpeed = spin;
        size(1f + 2f * am);
    }
}

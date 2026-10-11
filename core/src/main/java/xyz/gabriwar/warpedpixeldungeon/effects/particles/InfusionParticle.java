package xyz.gabriwar.warpedpixeldungeon.effects.particles;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import com.badlogic.gdx.utils.IntMap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

/** Expanding colored sparks with a small spiral, used by magical coatings; or (spiralIn) sparks
 *  spiralling into a point, an enchantment taking hold. White-hot at first, then their colour;
 *  whole pixels, 3, 2, then 1; visual randomness from FxRandom. */
public class InfusionParticle extends PixelParticle {
    private int color;
    //spiralling in: the point, the start's angle and radius
    private boolean in;
    private float cx, cy, a0, r0;

    /** The spiral's life and turns (radians over it), and the first share of life it is white. */
    public static final float IN_LIFE = 0.6f, IN_TURN = 3.2f, HOT_FOR = 0.15f;

    private static final IntMap<Emitter.Factory> outs = new IntMap<>(), ins = new IntMap<>();

    /** Sparks thrown out in a small spiral (one factory a colour, the same object each time). */
    public static synchronized Emitter.Factory factory(final int color) {
        Emitter.Factory f = outs.get(color);
        if (f != null) return f;
        f = new Emitter.Factory() {
            @Override public void emit(Emitter emitter, int index, float x, float y) {
                InfusionParticle p = (InfusionParticle) emitter.recycle(InfusionParticle.class);
                p.revive();
                p.in = false;
                p.x = x;
                p.y = y;
                p.color = color;
                p.color(color);
                p.lifespan = p.left = FxRandom.Float(0.3f, 0.6f);
                float a = index * (float)(Math.PI * 2) / 12f, s = FxRandom.Float(12f, 25f);
                p.speed.set((float)Math.cos(a) * s, (float)Math.sin(a) * s);
                p.acc.set(-p.speed.y * 3, p.speed.x * 3);
                p.angle = 0;
                p.size(3f);
            }
            @Override public boolean lightMode() { return true; }
        };
        outs.put(color, f);
        return f;
    }

    /** Sparks spiralling into the point they are emitted at, from 9-12 px out, over IN_LIFE s. */
    public static synchronized Emitter.Factory spiralIn(final int color) {
        Emitter.Factory f = ins.get(color);
        if (f != null) return f;
        f = new Emitter.Factory() {
            @Override public void emit(Emitter emitter, int index, float x, float y) {
                InfusionParticle p = (InfusionParticle) emitter.recycle(InfusionParticle.class);
                p.revive();
                p.in = true;
                p.cx = x;
                p.cy = y;
                p.a0 = index * (float)(Math.PI * 2) / 12f + FxRandom.Float(-0.2f, 0.2f);
                p.r0 = FxRandom.Float(9f, 12f);
                p.color = color;
                p.lifespan = p.left = IN_LIFE * FxRandom.Float(0.85f, 1f);
                p.speed.set(0);
                p.acc.set(0);
                p.angle = 0;
                p.place(0f);
                p.size(1f);
            }
            @Override public boolean lightMode() { return true; }
        };
        ins.put(color, f);
        return f;
    }

    //on the spiral, a share t of the way in (a 2:1 ellipse: it lies round the body, on the ground)
    private void place(float t) {
        float a = a0 + IN_TURN * t, r = r0 * (1f - t);
        x = Math.round(cx + (float)Math.cos(a) * r);
        y = Math.round(cy + (float)Math.sin(a) * r * 0.5f);
    }

    @Override public void update() {
        super.update();
        float k = left / lifespan, p = 1f - k;
        if (in) {
            place(p);
            am = p < 0.2f ? p / 0.2f : 1f;
            size(p < 0.5f ? 1f : 2f);
        } else {
            am = k;
            size(k > 0.66f ? 3f : (k > 0.33f ? 2f : 1f));
        }
        color(p < HOT_FOR ? Ramp.lerp(0xFFFFFF, color, p / HOT_FOR) : color);
    }
}

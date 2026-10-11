package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.utils.Callback;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ElementalOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * Orbs of fire and ice circling their caster on the kit's ELEMENTAL ellipse, each three painted
 * layers at x1 (a halo of the light sheet one size larger for each step of heat, the element's
 * shell, a white-hot core), drawn as light; fire shells flicker by mirroring every FLICKER s and
 * shed an ember every EMBER_EVERY s, ice shells glint and drop frost motes; orbs on the far side
 * dimmer. A launched orb flies with a trail of its element and lands in a flash, a scorch or
 * rime, and sparks; its impact callback (the game waits on it) fires on the same frame as ever:
 * clamp(distance / 160, 0.15, 0.5) s after its first frame. Nothing allocated per spark: every
 * particle comes from the kit's pools. At most 5 orbs x 6 quads.
 */
public class ElementalOrbitFX extends Group {
    public static final float FLICKER=.1f, EMBER_EVERY=.15f, FROST_EVERY=.3f, GLINT_EVERY=.8f;
    /** Far-side orbs at this share of their brightness. */
    public static final float FAR=.6f;
    private static final int[] ORBS_SHEET={0,0,12,12};
    private final Char owner;
    private final ElementalOrbit state;
    private final Orb[] orbs = new Orb[ElementalOrbit.MAX_ORBS];
    private float time;
    private int flare;
    public ElementalOrbitFX(Char owner, ElementalOrbit state){
        this.owner = owner; this.state = state;
        flare = state.flareSerial;
        for (int i = 0; i < orbs.length; i++) add(orbs[i] = new Orb());
    }
    public static int color(int element, int heat){
        int[] fire = {0xFF4522, 0xFF792A, 0xFF9C32, 0xFFB642};
        int[] ice = {0x388AFF, 0x55BEFF, 0x57CFFF, 0x79DDFF};
        return (element == 0 ? fire : ice)[Math.max(0, Math.min(3, heat))];
    }
    /** The halo's glow size at a step of heat. */
    public static int haloSize(int heat){
        return heat >= 3 ? 15 : heat >= 1 ? 11 : 7;
    }
    /** Onto the light layer (render thread; posted there from another). */
    public static void show(final ElementalOrbitFX fx){
        Fx.post(() -> GameScene.light(fx));
    }
    @Override public void update(){
        super.update();
        if (owner.sprite == null || !owner.sprite.exists){ Fx.dispose(this); return; }
        visible = owner.sprite.visible;
        float dt = Game.elapsed;
        time += dt;
        float cx = SkillVfx.midX(owner.sprite), cy = SkillVfx.midY(owner.sprite) + FxOrbit.Slot.ELEMENTAL.h;
        //the orbs of one moment: the actor thread may swap in fewer while this frame draws
        int[][] view = state.view();
        int[] element = view[0], heat = view[1];
        int n = Math.min(element.length, heat.length);
        for (int i = 0; i < orbs.length; i++){
            Orb orb = orbs[i]; orb.visible = visible && i < n;
            if (i >= n) continue;
            float a = time * 1.8f + i * (float)Math.PI * 2 / n;
            float s = (float)Math.sin(a);
            float x = cx + (float)Math.cos(a) * (FxOrbit.Slot.ELEMENTAL.rx - i % 2);
            float y = cy + s * FxOrbit.Slot.ELEMENTAL.ry;
            orb.place(x, y, element[i], heat[i], time, s < 0 ? FAR : 1f);
            if (visible && s >= 0) orb.shed(dt, element[i], heat[i]);
        }
        if (flare != state.flareSerial){
            flare = state.flareSerial;
            for (int i = 0; i < n && visible; i++)
                burst(orbs[i].px, orbs[i].py, element[i], 3, 4);
        }
    }
    private static FxPic layer(String sheet, int[] frame){
        FxPic p = new FxPic(sheet); p.additive = true; p.show(frame); p.origin.set(frame[2] / 2f, frame[3] / 2f); return p;
    }
    private static int[] orbFrame(int i){
        return new int[]{ORBS_SHEET[0] + i * 12, 0, 12, 12};
    }
    private static final int[] FIRE_SHELL = orbFrame(1), ICE_SHELL = orbFrame(2), CORE = orbFrame(3);
    private static class Orb extends Group {
        final FxPic halo = layer(SkillVfx.LIGHT_SHEET, FxFrames.Light.GLOW_11);
        final FxPic shell = layer("effects/elemental_orbs.png", FIRE_SHELL);
        final FxPic core = layer("effects/elemental_orbs.png", CORE);
        float px, py, ember, frost, glint;
        int haloShown = 11;
        Orb(){ add(halo); add(shell); add(core); ember = FxRandom.Float(EMBER_EVERY); frost = FxRandom.Float(FROST_EVERY); glint = FxRandom.Float(GLINT_EVERY); }
        void place(float x, float y, int element, int heat, float time, float depth){
            px = x; py = y;
            int c = color(element, heat);
            int size = haloSize(heat);
            if (size != haloShown){ haloShown = size; halo.show(FxFrames.Light.glow(size)); halo.origin.set(size / 2f, size / 2f); }
            halo.lit(c);
            //the halo breathes within 8%
            halo.am = (.25f + .06f * heat) * (1f + .08f * (float)Math.sin(time * 4f + px * .1f)) * depth;
            shell.show(element == 0 ? FIRE_SHELL : ICE_SHELL);
            shell.lit(c);
            //fire flickers: its shell mirrored and back every FLICKER s
            shell.flipHorizontal = element == 0 && ((int)(time / FLICKER)) % 2 == 1;
            shell.am = .85f * depth;
            core.lit(0xFFFFFF);
            core.am = (.65f + heat * .1f) * depth;
            halo.place(x, y); shell.place(x, y); core.place(x, y);
        }
        //what an orb on the near side sheds: embers off fire, glints and frost off ice
        void shed(float dt, int element, int heat){
            if (element == 0){
                ember -= dt;
                if (ember <= 0){
                    ember += EMBER_EVERY;
                    FxParticle p = SkillVfx.glow(px + FxRandom.Float(-2f, 2f), py - 2, FxBudget.P3);
                    if (p != null) p.look(FxRandom.chance(.6f) ? FxFrames.EMBER_1 : FxFrames.EMBER_2).ramp(Element.FIRE.ember, true)
                            .vel(FxRandom.Float(-4f, 4f), FxRandom.Float(-18f, -10f)).sway(2f, 6f).life(.6f).alpha(1f, 0, .25f);
                }
            } else {
                frost -= dt;
                if (frost <= 0){
                    frost += FROST_EVERY;
                    FxParticle p = SkillVfx.matter(px + FxRandom.Float(-3f, 3f), py + 2, FxBudget.P3);
                    if (p != null) p.look(FxFrames.MOTE_1).paint(Element.FROST.ramp.stop(1))
                            .vel(FxRandom.Float(-2f, 2f), FxRandom.Float(6f, 10f)).life(.6f).alpha(.9f, 0, .3f);
                }
                glint -= dt;
                if (glint <= 0){
                    glint += GLINT_EVERY * FxRandom.Float(.8f, 1.2f);
                    FxParticle p = SkillVfx.glow(px + FxRandom.Float(-3f, 3f), py + FxRandom.Float(-3f, 3f), FxBudget.P3);
                    if (p != null) p.look(FxFrames.PLUS_3).paint(Element.FROST.light).life(.2f).alpha(1f, .04f, .12f).snap();
                }
            }
        }
    }
    /** Sparks of an orb's element thrown from (x, y). Render thread. */
    public static void burst(float x, float y, int element, int heat, int count){
        FxRecipes.sparks(x, y, element == 0 ? Element.FIRE : Element.FROST, count);
    }
    /** How long a launched orb flies to a cell `distance` px off, s. */
    public static float flight(float distance){
        return Math.max(.15f, Math.min(.5f, distance / 160f));
    }
    public static void launch(Char owner,int cell,int element,int heat,Callback impact){
        if(owner.sprite==null || owner.sprite.parent==null){ impact.call(); return; }
        final float fx=SkillVfx.midX(owner.sprite), fy=SkillVfx.midY(owner.sprite), tx=SkillVfx.cx(cell), ty=SkillVfx.cy(cell);
        final float duration=flight((float)Math.sqrt((tx-fx)*(tx-fx)+(ty-fy)*(ty-fy)));
        final Launch shot=new Launch(fx,fy,tx,ty,element,heat,duration,impact);
        Fx.post(()->{
            if(GameScene.layer(GameScene.Layer.LIGHTS)==null){ impact.call(); return; }
            GameScene.light(shot);
        });
    }
    /** A launched orb in flight: its impact called on the frame it arrives. */
    static class Launch extends Group {
        final Orb orb=new Orb();
        final float fx,fy,tx,ty,duration;
        final int element,heat;
        final Callback impact;
        float elapsed, trail;
        Launch(float fx,float fy,float tx,float ty,int element,int heat,float duration,Callback impact){
            this.fx=fx;this.fy=fy;this.tx=tx;this.ty=ty;this.element=element;this.heat=heat;this.duration=duration;this.impact=impact;
            add(orb);
        }
        @Override public void update(){
            super.update(); elapsed+=Game.elapsed; trail+=Game.elapsed;
            float t=Math.min(1,elapsed/duration), x=fx+(tx-fx)*t, y=fy+(ty-fy)*t;
            orb.place(x,y,element,heat,elapsed,1f);
            if(trail>.025f){
                trail=0;
                FxParticle p=SkillVfx.glow(x+FxRandom.Float(-1.5f,1.5f),y+FxRandom.Float(-1.5f,1.5f),FxBudget.P2);
                if(p!=null)p.look(element==0?FxFrames.EMBER_2:FxFrames.MOTE_2)
                        .ramp(element==0?Element.FIRE.ember:Element.FROST.ember,true).vel(FxRandom.Float(-6f,6f),FxRandom.Float(-6f,6f))
                        .drag(3f).life(.25f).alpha(.9f,0,.15f);
            }
            if(t>=1){ arrive(); Fx.dispose(this); impact.call(); }
        }
        //the landing: a flash of the orb's light, a scorch or rime, sparks
        void arrive(){
            if(!Fx.seen(tx,ty))return;
            int c=color(element,heat);
            //0.8 for its first moment (a core), then down within the glows' cap by 0.2 s
            FxLight.glow(tx,ty,23,c,.8f).core().life(0,.03f,.03f);
            FxLight.glow(tx,ty,23,c,1f).life(0,.04f,.16f);
            if(element==0)FxRecipes.scorch(tx,ty+4,FxRecipes.SMALL);
            else{
                FxDecal rime=FxDecal.at(tx,ty+4,FxFrames.RIME[FxRandom.Int(FxFrames.RIME.length)],Element.FROST.ramp.stop(2),.7f,1.5f,1f,FxRandom.chance(.5f));
                if(rime!=null)rime.revealCrop(FxDecal.FROM_MIDDLE,.15f);
            }
            burst(tx,ty,element,heat,4+heat*2);
        }
    }
}

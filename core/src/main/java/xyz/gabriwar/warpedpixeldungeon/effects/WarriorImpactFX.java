package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * Stone pressure and a hammer's blow, anchored to the actual struck tile, on the floor under
 * everyone: the ground cracks open (CRACK_17 revealed from its middle over OPEN s), a ring of
 * dust spreads 13 to 31 px over RING_FOR s, puffs of dust, pebbles bouncing once, a one-frame
 * star of the blow; on water a splash (a crown, droplets and its ring) instead of the crack. A
 * heavy blow (hammer: a leap's, a slam's landing) adds a second, wider ring a moment after, a warm
 * white core of light under it and holds its star two frames (no picture of a hammer standing in
 * the crack: nothing lies there). A ring of them (Earthshatter) draws every layer on its first RICH
 * cells a frame and only the crack and the ring (on water the ring alone) past them.
 */
public final class WarriorImpactFX {

    public static final float OPEN=.1f, RING_FOR=.2f;
    public static final int RICH=8, PEBBLES_MIN=2, PEBBLES_MAX=4, PUFFS=3;
    /** A heavy blow's second ring: from 9 to HEAVY_RING px, HEAVY_AFTER s after the first; its core. */
    public static final int HEAVY_RING=41, HEAVY_CORE=0xFFF4D8;
    public static final float HEAVY_AFTER=.05f;

    private WarriorImpactFX(){}

    public static void show(int cell){show(cell,false);}
    public static void show(int cell,boolean hammer){
        if(Dungeon.hero==null||Dungeon.hero.sprite==null||Dungeon.hero.sprite.parent==null)return;
        final int depth=Dungeon.depth,branch=Dungeon.branch;
        Fx.post(()->{
            if(Dungeon.level!=null&&depth==Dungeon.depth&&branch==Dungeon.branch
                    &&Dungeon.hero!=null&&Dungeon.hero.sprite!=null&&Dungeon.hero.sprite.parent!=null
                    &&cell>=0&&cell<Dungeon.level.length()&&Dungeon.level.heroFOV[cell])
                strike(cell,hammer);
        });
    }

    //render thread: everything the blow leaves
    private static void strike(int cell,boolean hammer){
        Group floor=GameScene.layer(GameScene.Layer.FLOOR);
        if(floor==null)return;
        float x=SkillVfx.cx(cell),y=SkillVfx.cy(cell)+3;
        boolean rich=SkillVfx.richThisFrame(RICH);
        boolean water=WaterFX.onWater(x,y);
        if(water){
            //a splash brings its own ring: one ring a blow, never two on one spot
            if(rich)WaterFX.splash(x,y,WaterFX.M,Element.NONE);
            else WaterFX.ring(x,y,WaterFX.M);
        }else{
            FxDecal crack=FxDecal.at(x,y,FxFrames.CRACK_17[FxRandom.Int(2)],Element.EARTH.ramp.stop(4),.75f,1.2f,.8f,FxRandom.chance(.5f));
            if(crack!=null)crack.revealCrop(FxDecal.FROM_MIDDLE,OPEN);
            FxRing.ground(floor,x,y,13,31,RING_FOR,Element.EARTH.ramp.stop(1),.5f,false);
        }
        if(hammer){
            //a heavier blow: a second, wider ring a moment after, a warm white core under it
            if(water){
                WaterFX.ring(x,y,WaterFX.L);
            }else{
                FxRing second=FxRing.ground(floor,x,y,9,HEAVY_RING,RING_FOR*1.6f,Element.EARTH.ramp.stop(0),.55f,false);
                if(second!=null)second.delay(HEAVY_AFTER);
            }
            FxLight.pool(x,y,31,HEAVY_CORE,1f).core().life(0,.05f,.05f);
        }
        if(!rich)return;
        //the blow's star, one frame (a heavy blow's two)
        FxParticle star=SkillVfx.glow(x,y-4,FxBudget.P0);
        if(star!=null)star.look(FxFrames.IMPACT_STAR[FxRandom.Int(2)]).paint(HEAVY_CORE).life(hammer?.07f:.04f).alpha(1f,0,.01f).snap();
        if(water)return;
        for(int i=0;i<PUFFS;i++){
            float side=i-1;
            FxParticle p=SkillVfx.matter(x+side*4f,y,FxBudget.P2);
            if(p!=null)p.look(FxFrames.PUFF_S[FxRandom.Int(2)]).paint(Element.EARTH.smoke.step(.3f))
                    .vel(side*FxRandom.Float(10f,18f),FxRandom.Float(-10f,-5f)).drag(3f).life(.4f).alpha(.6f,0,.25f);
        }
        int n=FxRandom.IntRange(PEBBLES_MIN,PEBBLES_MAX);
        for(int i=0;i<n;i++){
            FxParticle p=SkillVfx.matter(x+FxRandom.Float(-3f,3f),y-2,FxBudget.P2);
            if(p!=null)p.look(FxRandom.chance(.5f)?FxFrames.PEBBLE_2:FxFrames.PEBBLE_3).paint(Element.EARTH.ember.step(FxRandom.Float()))
                    .vel(FxRandom.Float(-28f,28f),FxRandom.Float(-50f,-30f)).gravity(220f)
                    .ground(y+3,FxParticle.BOUNCE,.3f).life(.8f).alpha(1f,0,.25f);
        }
    }

}

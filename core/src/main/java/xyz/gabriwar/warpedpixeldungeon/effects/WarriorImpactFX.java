package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * Stone pressure and a hammer's blow, anchored to the actual struck tile, on the floor under
 * everyone: the ground cracks open (CRACK_17 revealed from its middle over OPEN s), a ring of
 * dust spreads 13 to 31 px over RING_FOR s, puffs of dust, pebbles bouncing once, a one-frame
 * star of the blow; on water a splash (a crown, droplets and its ring) instead of the crack. A
 * hammer's own silhouette fades over it. A ring of them (Earthshatter) draws every layer on its
 * first RICH cells a frame and only the crack and the ring (on water the ring alone) past them.
 */
public class WarriorImpactFX extends FxPic {

    public static final float OPEN=.1f, RING_FOR=.2f, LIFE=.45f;
    public static final int RICH=8, PEBBLES_MIN=2, PEBBLES_MAX=4, PUFFS=3;

    private float left=LIFE;
    private final int depth=Dungeon.depth, branch=Dungeon.branch;

    private WarriorImpactFX(int cell){
        super("effects/warrior_impact.png");
        show(new int[]{16,0,16,16});
        place(SkillVfx.cx(cell),SkillVfx.cy(cell));
    }
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
        if(hammer)floor.add(new WarriorImpactFX(cell));
        if(!rich)return;
        //the blow's star, one frame
        FxParticle star=SkillVfx.glow(x,y-4,FxBudget.P0);
        if(star!=null)star.look(FxFrames.IMPACT_STAR[FxRandom.Int(2)]).paint(0xFFF4D8).life(.04f).alpha(1f,0,.01f).snap();
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

    @Override public void update(){
        super.update();left-=Game.elapsed;
        if(left<=0||depth!=Dungeon.depth||branch!=Dungeon.branch){Fx.dispose(this);return;}
        takeSlide();
        visible=SkillVfx.seen(Fx.cellAt(midX(),midY()));am=Math.min(1,left/.25f);
    }
}

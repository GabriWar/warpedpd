package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * A cut on the exact cell resolved by combat, on the floor under everyone: the whirl's own mark
 * (its inner 14 x 14, without the box round it) fading over LIFE s, a crescent sweeping three
 * directions in SWEEP s over it and two or three sparks off the steel. A whirl through many cells
 * draws every layer on its first RICH cells a frame, only the mark past them. About 10 quads a
 * cell.
 */
public class WhirlHitFX extends FxPic {

    public static final float LIFE=.45f, SWEEP=.12f;
    public static final int RICH=8;
    //the mark inside its box (effects/whirl_hit.png is 16 x 16 with a 1 px border)
    private static final int[] SHEET={0,0,16,16};

    private float left=LIFE;
    private final int depth=Dungeon.depth, branch=Dungeon.branch;

    private WhirlHitFX(int cell){
        super("effects/whirl_hit.png");
        crop(SHEET,1,1,14,14);
        place(SkillVfx.cx(cell),SkillVfx.cy(cell));
    }
    public static void show(int cell){
        if(Dungeon.hero==null||Dungeon.hero.sprite==null||Dungeon.hero.sprite.parent==null)return;
        final int depth=Dungeon.depth,branch=Dungeon.branch;
        Fx.post(()->{
            if(Dungeon.level==null||depth!=Dungeon.depth||branch!=Dungeon.branch
                    ||Dungeon.hero==null||Dungeon.hero.sprite==null||Dungeon.hero.sprite.parent==null
                    ||cell<0||cell>=Dungeon.level.length()||!Dungeon.level.heroFOV[cell])return;
            Group floor=GameScene.layer(GameScene.Layer.FLOOR);
            if(floor==null)return;
            floor.add(new WhirlHitFX(cell));
            if(!SkillVfx.richThisFrame(RICH))return;
            //three directions, a step each SWEEP / 3 s
            ArcSpinFX.at(cell,0xFFFFFF,9,.3f,FxRandom.Int(8)*45,FxRandom.sign()*45f/(SWEEP/3f),SWEEP);
            FxRecipes.sparks(SkillVfx.cx(cell),SkillVfx.cy(cell),Element.METAL,FxRandom.IntRange(2,3));
        });
    }
    @Override public void update(){
        super.update();left-=Game.elapsed;
        if(left<=0||depth!=Dungeon.depth||branch!=Dungeon.branch){Fx.dispose(this);return;}
        takeSlide();
        visible=SkillVfx.seen(Fx.cellAt(midX(),midY()));am=Math.min(1,left/.25f);
    }
}

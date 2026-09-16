package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** Stationary tile-sized cut: always marks the exact cell resolved by combat. */
public class WhirlHitFX extends Image {
    private float left=.45f;
    private final int depth=Dungeon.depth, branch=Dungeon.branch, cell;
    private WhirlHitFX(int cell){
        super("effects/whirl_hit.png");this.cell=cell;
        point(DungeonTilemap.tileToWorld(cell));
    }
    public static void show(int cell){
        if(Dungeon.hero==null||Dungeon.hero.sprite==null||Dungeon.hero.sprite.parent==null)return;
        final int depth=Dungeon.depth,branch=Dungeon.branch;
        Game.runOnRenderThread(()->{
            if(Dungeon.level!=null&&depth==Dungeon.depth&&branch==Dungeon.branch
                    &&Dungeon.hero!=null&&Dungeon.hero.sprite!=null&&Dungeon.hero.sprite.parent!=null
                    &&cell>=0&&cell<Dungeon.level.length()&&Dungeon.level.heroFOV[cell])
                Dungeon.hero.sprite.parent.add(new WhirlHitFX(cell));
        });
    }
    @Override public void update(){
        super.update();left-=Game.elapsed;
        if(left<=0||depth!=Dungeon.depth||branch!=Dungeon.branch){killAndErase();destroy();return;}
        visible=Dungeon.level.heroFOV[cell];alpha(Math.min(1,left/.25f));
    }
}

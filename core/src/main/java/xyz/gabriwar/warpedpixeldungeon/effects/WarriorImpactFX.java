package xyz.gabriwar.warpedpixeldungeon.effects;

import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/** Stone pressure ring and hammer impact, anchored to the actual struck tile. */
public class WarriorImpactFX extends Image {
    private float left=.45f;
    private final int depth=Dungeon.depth, branch=Dungeon.branch, cell;
    private WarriorImpactFX(int cell,boolean hammer){
        super("effects/warrior_impact.png");this.cell=cell;
        frame(new com.watabou.noosa.TextureFilm(texture,16,16).get(hammer?1:0));
        point(DungeonTilemap.tileToWorld(cell));
    }
    public static void show(int cell){show(cell,false);}
    public static void show(int cell,boolean hammer){
        if(Dungeon.hero==null||Dungeon.hero.sprite==null||Dungeon.hero.sprite.parent==null)return;
        final int depth=Dungeon.depth,branch=Dungeon.branch;
        Game.runOnRenderThread(()->{
            if(Dungeon.level!=null&&depth==Dungeon.depth&&branch==Dungeon.branch
                    &&Dungeon.hero!=null&&Dungeon.hero.sprite!=null&&Dungeon.hero.sprite.parent!=null
                    &&cell>=0&&cell<Dungeon.level.length()&&Dungeon.level.heroFOV[cell])
                Dungeon.hero.sprite.parent.add(new WarriorImpactFX(cell,hammer));
        });
    }
    @Override public void update(){
        super.update();left-=Game.elapsed;
        if(left<=0||depth!=Dungeon.depth||branch!=Dungeon.branch){killAndErase();destroy();return;}
        visible=Dungeon.level.heroFOV[cell];alpha(Math.min(1,left/.25f));
    }
}

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.*;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

public class PhantomEcho extends Buff {
    private int cell,enemy,rank,damage,left,depth,branch;
    private boolean struck;
    private Image image;
    public void set(int cell,Char enemy,int rank,int damage){
        this.cell=cell;this.enemy=enemy.id();this.rank=rank;this.damage=damage;left=rank+1;
        depth=Dungeon.depth;branch=Dungeon.branch;spend(TICK);fx(true);
    }
    public boolean swap(Hero hero){
        if(depth!=Dungeon.depth||branch!=Dungeon.branch||!SkillInteractions.valid(cell)
                || !Dungeon.level.passable[cell]||Actor.findChar(cell)!=null)return false;
        detach();ScrollOfTeleportation.appear(hero,cell);Dungeon.level.occupyCell(hero);Dungeon.observe();
        hero.spendAndNext(TICK);return true;
    }
    @Override public boolean act(){
        if(depth!=Dungeon.depth||branch!=Dungeon.branch){detach();return true;}
        Actor actor=Actor.findById(enemy);
        if(!struck){
            struck=true;
            if(actor instanceof Char && ((Char)actor).isAlive() && Dungeon.level.distance(cell,((Char)actor).pos)<=1+rank/2
                    && SkillInteractions.clear(cell,((Char)actor).pos)){
                Char victim=(Char)actor;Hero hero=(Hero)target;
                int hit=Math.max(1,damage*(25+10*rank)/100);
                if(rank>=3 && hero.belongings.weapon()!=null)hit=hero.belongings.weapon().proc(hero,victim,hit);
                victim.damage(hit,this);SkillInteractions.flare(victim.pos,0xC1A4F0);
            }
        }
        if(--left<=0)detach();else spend(TICK);return true;
    }
    @Override public void fx(boolean on){
        if(image!=null){image.killAndErase();image=null;}
        if(on && target!=null && target.sprite!=null&&target.sprite.parent!=null){
            image=new Image();image.copy(target.sprite);image.point(DungeonTilemap.tileToWorld(cell));
            image.hardlight(0x9E88D8);image.alpha(.45f);target.sprite.parent.add(image);
        }
    }
    @Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("cell",cell);b.put("enemy",enemy);b.put("rank",rank);b.put("damage",damage);b.put("left",left);b.put("depth",depth);b.put("branch",branch);b.put("struck",struck);}
    @Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);cell=b.getInt("cell");enemy=b.getInt("enemy");rank=b.getInt("rank");damage=b.getInt("damage");left=b.getInt("left");depth=b.getInt("depth");branch=b.getInt("branch");struck=b.getBoolean("struck");}
}

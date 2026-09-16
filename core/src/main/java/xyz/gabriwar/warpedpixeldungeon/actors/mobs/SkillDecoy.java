package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.sprites.MirrorSprite;

/** One disposable distraction; it never attacks or awards loot/experience. */
public class SkillDecoy extends Mob {
    public int rank=1,left=4;
    public boolean blinding=true;
    {spriteClass=MirrorSprite.class;alignment=Alignment.ALLY;HP=HT=1;EXP=0;lootChance=0;properties.add(Property.IMMOVABLE);}
    @Override public boolean act(){
        if(--left<=0){die(this);return true;}
        SkillInteractions.lure(pos,rank);SkillInteractions.flare(pos,0xB8A3DD);spend(TICK);return true;
    }
    @Override public void die(Object source){
        if(blinding&&rank>=2)for(int cell:SkillInteractions.area(pos,1)){
            Char ch=Actor.findChar(cell);if(ch!=null&&ch.alignment==Alignment.ENEMY)Buff.prolong(ch,Blindness.class,2f);
        }
        super.die(source);
    }
    @Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("blinding",blinding);b.put("rank",rank);b.put("left",left);}
    @Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);blinding=!b.contains("blinding")||b.getBoolean("blinding");rank=b.getInt("rank");left=b.getInt("left");}
}

package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.RogueShadowSprite;

/** Rogue's independent scouting and fighting companion; inherited summon save/follow AI. */
public class RogueShadow extends SummonedPet {
    public int rank=1;
    public RogueShadow(){super(RogueShadowSprite.class);viewDistance=3;}
    public void setRank(int rank){
        this.rank=rank;setLevel(rank*2);setHealthShare(new float[]{.15f,.30f,.40f}[Math.max(0,Math.min(2,rank-1))]);setDamageShare(new float[]{.30f,.40f,.50f}[Math.max(0,Math.min(2,rank-1))]);
        setStats(2+rank,4+3*rank,rank);viewDistance=2+rank;
    }
    public void shareVision(boolean[] vision){
        if(!isAlive()||alignment!=Alignment.ALLY||Dungeon.level==null||!Dungeon.level.mobs.contains(this))return;
        int radius=2+rank,w=Dungeon.level.width(),h=Dungeon.level.height();
        for(int y=Math.max(0,pos/w-radius);y<=Math.min(h-1,pos/w+radius);y++)
            for(int x=Math.max(0,pos%w-radius);x<=Math.min(w-1,pos%w+radius);x++)vision[x+y*w]=true;
    }
    @Override public String name(){return Messages.get(this,"name");}
    @Override public String description(){return Messages.get(this,"desc",2+rank);}
    @Override protected boolean act(){
        boolean result=super.act();
        if(isAlive()&&Dungeon.level.mobs.contains(this)){
            Dungeon.observe();
            xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog(pos,viewDistance+1+(int)Math.ceil(speed()));
        }
        return result;
    }
    @Override public int attackProc(Char enemy,int damage){
        SkillSpectacleFX.fly(SkillSpectacleFX.SHADOW,pos,enemy.pos,0,.35f);
        return super.attackProc(enemy,damage);
    }
    @Override public void die(Object cause){
        SkillSpectacleFX.show(SkillSpectacleFX.SHADOW,pos);super.die(cause);Dungeon.observe();xyz.gabriwar.warpedpixeldungeon.scenes.GameScene.updateFog();
    }
    @Override public void storeInBundle(Bundle b){super.storeInBundle(b);b.put("shadow_rank",rank);}
    @Override public void restoreFromBundle(Bundle b){super.restoreFromBundle(b);rank=Math.max(1,b.getInt("shadow_rank"));viewDistance=2+rank;spriteClass=RogueShadowSprite.class;}
}

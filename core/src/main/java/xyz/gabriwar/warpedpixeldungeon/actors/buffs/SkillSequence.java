package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import java.util.*;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

/** Turn-based, saveable follow-throughs. Visual timelines never own actor scheduling. */
public class SkillSequence extends Buff {
    public static final int QUAKE=0,LANDSLIDE=1,SMOKE_BLADES=2,BRIAR=3,BLADESTORM=4,LANCES=5,DAWN=6,CATHEDRAL=7,CROSSCUT=8;
    public int kind,rank,center,age,remaining,damage;
    private int depth,branch;
    private int[] cells=new int[0];
    private com.watabou.noosa.Gizmo aura;
    public static SkillSequence start(Hero hero,int kind,int rank,int center,int damage,int turns,Collection<Integer> cells){
        // Refresh mobile auras instead of multiplying them on every cast.
        ArrayList<SkillSequence> old=new ArrayList<>();
        for(SkillSequence s:hero.buffs(SkillSequence.class))if(s.kind==kind)old.add(s);
        old.sort(Comparator.comparingInt(Actor::id));
        int limit=(kind==DAWN||kind==BLADESTORM)?1:4;
        while(old.size()>=limit)old.remove(0).detach();
        SkillSequence s=Buff.append(hero,SkillSequence.class);
        s.kind=kind;s.rank=Math.max(1,Math.min(3,rank));s.center=center;s.damage=Math.max(1,damage);s.remaining=turns;
        s.cells=cells.stream().distinct().mapToInt(Integer::intValue).toArray();s.depth=Dungeon.depth;s.branch=Dungeon.branch;s.spend(TICK);s.fx(true);
        return s;
    }
    public boolean sameFloor(){return Dungeon.level!=null&&depth==Dungeon.depth&&branch==Dungeon.branch;}
    public static int color(int kind){return kind<=1?0xE6AE78:kind==SMOKE_BLADES?0xB999EC:kind==BRIAR?0xABD08A:kind==DAWN||kind==CATHEDRAL?0xFFE6A1:0xBFE5FF;}
    private void hit(int cell,int amount,int sprite){
        if(!SkillInteractions.valid(cell))return;
        if(sprite==SkillSpectacleFX.ROCK)xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX.show(cell);
        else SkillSpectacleFX.show(sprite,cell);
        Char enemy=Actor.findChar(cell);
        if(enemy!=null&&enemy.isAlive()&&enemy.alignment==Char.Alignment.ENEMY)enemy.damage(amount,this);
    }
    // A spectral lance: a ghost thrust over the cell at chest height, rippling out along the lane
    // (each cell 0.04 s after the one before it); the damage as hit's.
    private void lance(int cell,int amount,int index){
        if(!SkillInteractions.valid(cell))return;
        SkillSpectacleFX.fly(SkillSpectacleFX.LANCE,center,cell,.04f*index,.3f);
        Char enemy=Actor.findChar(cell);
        if(enemy!=null&&enemy.isAlive()&&enemy.alignment==Char.Alignment.ENEMY)enemy.damage(amount,this);
    }
    @Override public boolean act(){
        if(!sameFloor()||!(target instanceof Hero)||!target.isAlive()){detach();return true;}
        Hero hero=(Hero)target;age++;
        switch(kind){
            case QUAKE:
            case LANDSLIDE: {
                int radius=age+(kind==LANDSLIDE?1:0);
                for(int c:SkillInteractions.area(center,radius))if(Dungeon.level.distance(center,c)==radius){
                    hit(c,damage+rank*(age-1),SkillSpectacleFX.ROCK);
                    Char enemy=Actor.findChar(c);
                    if(enemy!=null&&enemy.isAlive()&&enemy.alignment==Char.Alignment.ENEMY){
                        SkillInteractions.push(enemy,center,1,2+rank);
                        if(rank>=3)Buff.prolong(enemy,Cripple.class,2f);
                    }
                }
                break;
            }
            case SMOKE_BLADES: {
                ArrayList<Char> victims=new ArrayList<>();
                for(Char ch:Actor.chars())if(ch.alignment==Char.Alignment.ENEMY&&ch.isAlive()
                        &&Dungeon.level.distance(center,ch.pos)<=1+rank&&SkillInteractions.clear(center,ch.pos))victims.add(ch);
                victims.sort(Comparator.comparingInt(ch->Dungeon.level.distance(center,ch.pos)));
                for(int i=0;i<Math.min(rank+1,victims.size());i++){
                    Char enemy=victims.get(i);SkillSpectacleFX.fly(SkillSpectacleFX.SHURIKEN,center,enemy.pos,i*.06f,.4f);
                    enemy.damage(damage,this);if(enemy.isAlive()&&rank>=2)Buff.prolong(enemy,Blindness.class,2f);
                }
                break;
            }
            case BRIAR:
                for(int c:SkillInteractions.area(center,rank>=3?2:1)){
                    Char enemy=Actor.findChar(c);
                    if(enemy==null||enemy.alignment!=Char.Alignment.ENEMY||!enemy.isAlive())continue;
                    SkillSpectacleFX.fly(SkillSpectacleFX.THORN,center,c,0,.45f);
                    enemy.damage(damage,this);
                    if(enemy.isAlive())Buff.prolong(enemy,Cripple.class,2f);
                }
                break;
            case BLADESTORM:
                center=hero.pos;
                for(int c:SkillInteractions.area(center,rank>=3&&remaining==1?2:1))if(c!=center){
                    xyz.gabriwar.warpedpixeldungeon.effects.WhirlHitFX.show(c);
                    Char enemy=Actor.findChar(c);
                    if(enemy!=null&&enemy.isAlive()&&enemy.alignment==Char.Alignment.ENEMY){
                        enemy.damage(damage,this);
                        if(remaining==1&&rank>=2)SkillInteractions.push(enemy,center,1,rank+2);
                    }
                }
                break;
            case LANCES:
                // Alternate odd/even tiles, then finish the entire lane at rank three.
                for(int i=0;i<cells.length;i++)if(rank==3&&remaining==1||(i%2)==((age-1)%2))
                    if(SkillInteractions.clear(center,cells[i]))lance(cells[i],damage,i);
                break;
            case CROSSCUT:
                for(int c:cells)if(SkillInteractions.clear(center,c)){
                    hit(c,damage,SkillSpectacleFX.SLASH);
                    if(rank>=2){
                        Char enemy=Actor.findChar(c);if(enemy!=null&&enemy.isAlive()&&enemy.alignment==Char.Alignment.ENEMY)Buff.affect(enemy,Bleeding.class).set(rank);
                    }
                }
                break;
            case DAWN:
                center=hero.pos;SkillSpectacleFX.show(SkillSpectacleFX.WINGS,center);
                for(int c:SkillInteractions.area(center,rank>=2?2:1)){
                    Char ally=Actor.findChar(c);
                    if(ally==null||!ally.isAlive())continue;
                    if(ally.alignment==Char.Alignment.ALLY){
                        int heal=Math.min(1+rank,ally.HT-ally.HP);ally.HP+=heal;
                        // the dawn's mend seen: the heal's red crosses and green light on whoever it really mended
                        if(heal>0)mended(ally);
                        int excess=1+rank-heal;
                        if(excess>0){SkillInteractions.Mark shard=SkillInteractions.mark(ally,SkillInteractions.Mark.SHARDS,rank,6);shard.power=Math.min(6+4*rank,shard.power+excess);}
                    }else if(remaining==1&&ally.alignment==Char.Alignment.ENEMY){
                        hit(c,damage,SkillSpectacleFX.SPIRE);
                    }
                }
                break;
            case CATHEDRAL:
                for(int c:SkillInteractions.area(center,Math.min(3,age))){
                    int w=Dungeon.level.width(),dx=c%w-center%w,dy=c/w-center/w;
                    // Cardinal arms unfold first; rank 2 adds diagonals, rank 3 a final crown.
                    if((dx==0||dy==0)&&Math.max(Math.abs(dx),Math.abs(dy))==age
                            ||rank>=2&&Math.abs(dx)==age&&Math.abs(dy)==age){
                        hit(c,damage,SkillSpectacleFX.SPIRE);
                        Char ally=Actor.findChar(c);if(ally!=null&&ally.alignment==Char.Alignment.ALLY)Buff.prolong(ally,Bless.class,2f);
                    }
                }
                if(rank==3&&remaining==1)for(int c:SkillInteractions.area(center,1))hit(c,damage,SkillSpectacleFX.SPIRE);
                break;
        }
        if(--remaining<=0)detach();else spend(TICK);return true;
    }
    /** The heal's picture on one the dawn mended (in sight): MENDED_CROSSES red crosses and
     *  MENDED_GREENS motes of green light (MoteFX, posted; none of the game's Random). */
    public static final int MENDED_CROSSES=2,MENDED_GREENS=2;
    public static void mended(Char ally){
        if(ally.sprite==null||Dungeon.level==null||!Dungeon.level.heroFOV[ally.pos])return;
        xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX.crosses(ally.sprite,MENDED_CROSSES);
        xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX.greens(ally.sprite,MENDED_GREENS);
    }
    private volatile int fxRevision;
    @Override public void fx(boolean on){
        final int revision=++fxRevision;
        if(aura==null&&(!on||target==null||target.sprite==null||target.sprite.parent==null))return;
        xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.post(()->{if(revision==fxRevision)updateAura(on);});
    }
    private void updateAura(boolean on){
        if(aura!=null){xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.dispose(aura);aura=null;}
        if(!on||target==null||target.sprite==null||target.sprite.parent==null||kind!=DAWN&&kind!=BLADESTORM)return;
        final boolean dawn=kind==DAWN;
        final int motes=dawn?2:2+rank/2;
        // The kit's SEQUENCE orbit: dawn's two warm motes breathing, a bladestorm's steel glints
        aura=new xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillOrbit(target,xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit.Slot.SEQUENCE){
            @Override protected int count(){return sameFloor()&&target.isAlive()?motes:0;}
            @Override protected void look(xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit.Ring ring,int n){
                if(dawn)ring.motes(n,xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames.MOTE_2,0xF1D99C,true).trail(2).breath(.35f,.55f,.5f).speed(3f);
                else ring.motes(n,xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames.PLUS_3,0xB9DEEE,true).trail(n<=2?2:1).breath(.8f,1f,1.5f).speed(4f);
            }
        };
        target.sprite.parent.add(aura);
    }
    @Override public String name(){return Messages.get(SkillSequence.class,"name_"+kind);}
    @Override public String desc(){return Messages.get(SkillSequence.class,"desc",name(),remaining);}
    @Override public void storeInBundle(Bundle b){
        super.storeInBundle(b);b.put("kind",kind);b.put("rank",rank);b.put("center",center);b.put("age",age);b.put("remaining",remaining);
        b.put("damage",damage);b.put("cells",cells);b.put("depth",depth);b.put("branch",branch);
    }
    @Override public void restoreFromBundle(Bundle b){
        super.restoreFromBundle(b);kind=b.getInt("kind");rank=b.getInt("rank");center=b.getInt("center");age=b.getInt("age");remaining=b.getInt("remaining");
        damage=b.getInt("damage");cells=b.getIntArray("cells");depth=b.getInt("depth");branch=b.getInt("branch");
    }
}

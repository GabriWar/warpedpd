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
    private com.watabou.noosa.Group aura;
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
                SkillSpectacleFX.show(SkillSpectacleFX.JAW,center);
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
                    if(SkillInteractions.clear(center,cells[i]))hit(cells[i],damage,SkillSpectacleFX.LANCE);
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
    private volatile int fxRevision;
    @Override public void fx(boolean on){
        final int revision=++fxRevision;
        if(aura==null&&(!on||target==null||target.sprite==null||target.sprite.parent==null))return;
        com.watabou.noosa.Game.runOnRenderThread(()->{if(revision==fxRevision)updateAura(on);});
    }
    private void updateAura(boolean on){
        if(aura!=null){aura.killAndErase();aura.destroy();aura=null;}
        if(!on||target==null||target.sprite==null||target.sprite.parent==null||kind!=DAWN&&kind!=BLADESTORM)return;
        aura=new com.watabou.noosa.Group(){
            float time;
            final com.watabou.noosa.Image[] orbit=new com.watabou.noosa.Image[kind==DAWN?2:2+rank/2];
            {
                for(int i=0;i<orbit.length;i++){
                    com.watabou.noosa.Image image=new com.watabou.noosa.Image("effects/skill_motes.png");
                    image.frame(new com.watabou.noosa.TextureFilm(image.texture,4,4).get(kind==DAWN?0:2));
                    image.origin.set(2,2);image.hardlight(kind==DAWN?0xF1D99C:0xB9DEEE);orbit[i]=image;add(image);
                }
            }
            @Override public void update(){
                super.update();time+=com.watabou.noosa.Game.elapsed;
                visible=sameFloor()&&target.isAlive()&&target.sprite.visible;
                for(int i=0;i<orbit.length;i++){
                    float angle=time*3+i*6.2831853f/orbit.length;
                    float xoff=(float)Math.cos(angle)*7;
                    float yoff=(float)Math.sin(angle)*3;
                    orbit[i].x=Math.round(target.sprite.center().x+xoff)-2;
                    orbit[i].y=Math.round(target.sprite.center().y+yoff)-3;
                    orbit[i].alpha(kind==DAWN?.45f+.1f*(float)Math.sin(time*3):.6f);
                    if(kind==BLADESTORM)orbit[i].angle=angle*57.2958f;
                }
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

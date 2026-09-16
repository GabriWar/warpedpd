package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import java.util.*;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;

/** Bounded, saved ground effects. Coordinates are always tied to their originating floor. */
public class SkillField extends Buff {
    public static final int CRACK=0, FRAGMENT=1, ICE=2, ARROWS=3, SANCTUARY=4, WIND=5, SOUL=6, SMOKE=7;
    // Skill-driven kinds: no built-in behaviour or floor sprite (the sheet holds frames 0-7 only);
    // the owning skill springs them from Skill.onCharMoved and draws them from Skill.onFieldTick.
    public static final int RIGGED=8, JAWS=9;
    public int kind, rank, remaining, age, origin;
    public int[] cells=new int[0];
    private int depth, branch;
    private SkillFieldFX visual;
    { type=buffType.NEUTRAL; }
    public static SkillField place(Hero hero,int kind,int rank,int duration,Collection<Integer> cells){
        // Keep the most recent eight fields of a kind to bound actor and particle costs.
        ArrayList<SkillField> old=new ArrayList<>();
        for(SkillField f:hero.buffs(SkillField.class)) if(f.kind==kind)old.add(f);
        if(old.size()>=8)old.get(0).detach();
        SkillField field=Buff.append(hero,SkillField.class);
        field.kind=kind; field.rank=rank; field.remaining=duration;
        field.cells=cells.stream().distinct().mapToInt(Integer::intValue).toArray();
        field.origin=hero.pos; field.depth=Dungeon.depth; field.branch=Dungeon.branch;
        field.spend(TICK); field.fx(true);
        return field;
    }
    public boolean sameFloor(){return depth==Dungeon.depth && branch==Dungeon.branch;}
    public boolean contains(int cell){if(!sameFloor())return false; for(int c:cells)if(c==cell)return true;return false;}
    public void removeCell(int cell){cells=Arrays.stream(cells).filter(c->c!=cell).toArray();fx(true);}
    @Override public String name(){return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(SkillField.class,"name_"+kind);}
    @Override public String desc(){return xyz.gabriwar.warpedpixeldungeon.messages.Messages.get(SkillField.class,"desc",name(),remaining);}
    @Override public boolean act(){
        if(!sameFloor() || !(target instanceof Hero) || !target.isAlive()){detach();return true;}
        Hero hero=(Hero)target; age++;
        if(kind==CRACK){
            for(int cell:cells) SkillInteractions.blast(cell,0,5+rank*3,0xE8B775);
            detach(); return true;
        }
        if(kind==SANCTUARY){
            for(int cell:cells){
                Char ch=Actor.findChar(cell);
                if(ch==null || !ch.isAlive())continue;
                if(ch.alignment==Char.Alignment.ALLY)ch.HP=Math.min(ch.HT,ch.HP+1+rank);
                else if(ch.alignment==Char.Alignment.ENEMY){
                    SkillInteractions.Mark mark=SkillInteractions.mark(ch,SkillInteractions.Mark.RADIANT,rank,remaining+1);
                    mark.power=Math.min(12,mark.power+1);mark.other=id();
                }
            }
        }
        if(kind==SMOKE)SkillInteractions.lure(origin,rank);
        if(kind>=RIGGED && hero.heroSkills!=null)hero.heroSkills.onFieldTick(this);
        if(target==null || !target.buffs().contains(this))return true;
        remaining--;
        if(remaining<=0 || cells.length==0){
            if(kind==SANCTUARY)SkillInteractions.detonateRadiance(this);
            detach();
        }else spend(TICK);
        return true;
    }
    @Override public void fx(boolean on){
        if(visual!=null){visual.killAndErase();visual=null;}
        if(on && kind<RIGGED && target!=null && target.sprite!=null && target.sprite.parent!=null && cells.length>0){
            visual=new SkillFieldFX(this);target.sprite.parent.add(visual);
        }
    }
    @Override public void storeInBundle(Bundle b){
        super.storeInBundle(b);b.put("kind",kind);b.put("rank",rank);b.put("remaining",remaining);b.put("age",age);
        b.put("cells",cells);b.put("depth",depth);b.put("branch",branch);b.put("origin",origin);
    }
    @Override public void restoreFromBundle(Bundle b){
        super.restoreFromBundle(b);kind=b.getInt("kind");rank=b.getInt("rank");remaining=b.getInt("remaining");age=b.getInt("age");
        cells=b.getIntArray("cells");depth=b.getInt("depth");branch=b.getInt("branch");origin=b.getInt("origin");
    }
}

/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
 *
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 */

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import java.util.*;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.*;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.*;

/** Keeps the old class/tag so purchased Phantom Strike levels remain valid. */
public class PhantomStrike extends Skill {
    {tag="A4";name="Shadow Company";castText="Out of the dark";image=118;tier=4;mana=12;}
    public static ArrayList<RogueShadow> shadows(){
        ArrayList<RogueShadow> result=new ArrayList<>();
        if(Dungeon.level!=null)for(Mob mob:Dungeon.level.mobs)
            if(mob instanceof RogueShadow&&mob.isAlive()&&mob.alignment==Char.Alignment.ALLY)result.add((RogueShadow)mob);
        return result;
    }
    public static boolean safe(Char ch,int cell){
        return SkillInteractions.valid(cell)&&Dungeon.level.passable[cell]&&!Dungeon.level.pit[cell]
                &&(!Char.hasProp(ch,Char.Property.LARGE)||Dungeon.level.openSpace[cell]);
    }
    public static boolean canSwap(Hero hero,RogueShadow shadow){
        return hero.isAlive()&&shadow.isAlive()&&shadow.alignment==Char.Alignment.ALLY&&Dungeon.level.mobs.contains(shadow)
                &&!hero.rooted&&!shadow.rooted&&safe(hero,shadow.pos)&&safe(shadow,hero.pos);
    }
    @Override public ArrayList<String> actions(Hero hero){
        ArrayList<String> out=new ArrayList<>();if(level>0&&(level>=MAX_LEVEL&&!shadows().isEmpty()||hero.MP>=getManaCost()))out.add(AC_CAST);return out;
    }
    @Override public void execute(Hero hero,String action){
        if(!AC_CAST.equals(action)||level<=0)return;
        GameScene.selectCell(new CellSelector.Listener(){
            @Override public String prompt(){return Messages.get(PhantomStrike.class,"prompt");}
            @Override public void onSelect(Integer cell){
                if(cell==null||!hero.isAlive()||!SkillInteractions.valid(cell))return;
                ArrayList<RogueShadow> allies=shadows();Char occupant=Actor.findChar(cell);
                if(occupant instanceof RogueShadow&&allies.contains(occupant)){
                    //at mastery you can step into one of your shadows and trade places
                    RogueShadow shadow=(RogueShadow)occupant;
                    if(level<MAX_LEVEL)return;
                    if(!canSwap(hero,shadow))return;
                    int old=hero.pos;hero.pos=cell;shadow.pos=old;
                    hero.sprite.place(hero.pos);shadow.sprite.place(shadow.pos);
                    SkillSpectacleFX.fly(SkillSpectacleFX.SHADOW,old,cell,0,.4f);
                    SkillSpectacleFX.fly(SkillSpectacleFX.SHADOW,cell,old,0,.4f);
                    Dungeon.level.occupyCell(shadow);Dungeon.level.occupyCell(hero);
                }else{
                    if(!Dungeon.level.heroFOV[cell]||Dungeon.level.distance(hero.pos,cell)>6||!SkillInteractions.clear(hero.pos,cell)
                            ||hero.MP<getManaCost()||allies.size()>=level+1)return;
                    int slots=Math.min(level+1-allies.size(),Math.max(3+hero.heroSkills.allSummonLimit(),level+1)-SummonedPet.activeCount());
                    int created=0;
                    for(int dest:SkillInteractions.area(cell,1)){
                        if(created>=slots)break;
                        if(!Dungeon.level.passable[dest]||Dungeon.level.pit[dest]||Actor.findChar(dest)!=null)continue;
                        RogueShadow shadow=new RogueShadow();shadow.setRank(level);shadow.pos=dest;
                        GameScene.add(shadow);SkillSpectacleFX.show(SkillSpectacleFX.SHADOW,dest);created++;
                    }
                    if(created==0)return;
                    hero.MP-=getManaCost();castTextYell();
                }
                Invisibility.dispel();Dungeon.observe();GameScene.updateFog();
                hero.heroSkills.lastUsed=PhantomStrike.this;hero.spendAndNext(TIME_TO_USE);
            }
        });
    }
    @Override public int getManaCost(){return (int)Math.ceil(mana*(1+.5*level));}
    @Override protected boolean upgrade(){return true;}
}

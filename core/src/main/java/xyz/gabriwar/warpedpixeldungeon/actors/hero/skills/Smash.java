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

import java.util.ArrayList;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.*;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class Smash extends ActiveSkill1 {
    {name="Smash";castText="Smash!";tier=1;image=17;mana=3;}
    @Override public boolean toggleable(){return false;}
    @Override public void restoreInBundle(Bundle b){super.restoreInBundle(b);active=false;}
    public float hitMultiplier(){return 1.25f+.25f*level;}
    @Override public ArrayList<String> actions(Hero hero){
        ArrayList<String> out=new ArrayList<>();if(level>0&&hero.MP>=getManaCost())out.add(AC_CAST);return out;
    }
    @Override public void execute(Hero hero,String action){
        if(!AC_CAST.equals(action)||level<=0||hero.MP<getManaCost())return;
        GameScene.selectCell(new CellSelector.Listener(){
            @Override public String prompt(){return Messages.get(Smash.class,"prompt");}
            @Override public void onSelect(Integer cell){
                if(cell==null)return;
                Char enemy=Actor.findChar(cell);
                if(enemy==null||!enemy.isAlive()||enemy.alignment!=Char.Alignment.ENEMY
                        ||!Dungeon.level.heroFOV[cell]||!hero.canAttack(enemy)||hero.isCharmedBy(enemy)){
                    GLog.w(Messages.get(Smash.class,"no_target"));return;
                }
                if(hero.MP<getManaCost())return;
                int depth=Dungeon.depth,branch=Dungeon.branch;
                hero.MP-=getManaCost();hero.heroSkills.lastUsed=Smash.this;hero.busy();Invisibility.dispel();
                hero.sprite.attack(cell,()->{
                    if(hero.isAlive()&&depth==Dungeon.depth&&branch==Dungeon.branch&&enemy.isAlive()&&hero.canAttack(enemy)){
                        int impact=enemy.pos,before=enemy.HP;
                        //at +3 whatever the target is slammed into makes it take half the blow again
                        if(hero.attack(enemy,hitMultiplier(),0,Float.POSITIVE_INFINITY)&&enemy.isAlive())
                            SkillInteractions.push(enemy,hero.pos,level,level>=MAX_LEVEL?Math.max(0,before-enemy.HP)/2:0);
                        WarriorImpactFX.show(impact,true);
                    }
                    hero.spendAndNext(TIME_TO_USE);
                });
            }
        });
    }
    @Override public int getManaCost(){return (int)Math.ceil(mana*(1+.55*level));}
    @Override protected boolean upgrade(){return true;}
}

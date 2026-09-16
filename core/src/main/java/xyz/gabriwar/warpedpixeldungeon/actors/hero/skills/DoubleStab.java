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
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.scenes.*;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/** Keeps the A1 tag and class for old saves; replaces the layered double-hit toggle. */
public class DoubleStab extends ActiveSkill1 {
    {name="Shadow Link";castText="Bound together!";image=61;tier=1;mana=5;}
    @Override public boolean toggleable(){return true;}
    @Override public boolean doubleStab(){return false;}
    public Char marked(){return null;}
    public static int sharePercent(int rank){return 20+20*Math.max(1,Math.min(3,rank));}
    public static int focusedPercent(int rank){return rank>=3?70:rank==2?50:35;}
    @Override public void execute(Hero hero,String action){
        super.execute(hero,action);
        if(!active)clearLinks();
    }
    public Char automaticPartner(Char enemy){
        Char nearest=null;
        for(Char other:Actor.chars())if(other!=enemy&&other.alignment==Char.Alignment.ENEMY&&other.isAlive()
                &&SkillInteractions.valid(other.pos)&&Dungeon.level.heroFOV[other.pos]
                &&Dungeon.level.distance(enemy.pos,other.pos)<=3&&SkillInteractions.clear(enemy.pos,other.pos)){
            if(nearest==null||Dungeon.level.distance(enemy.pos,other.pos)<Dungeon.level.distance(enemy.pos,nearest.pos)
                    ||Dungeon.level.distance(enemy.pos,other.pos)==Dungeon.level.distance(enemy.pos,nearest.pos)&&other.id()<nearest.id())nearest=other;
        }
        return nearest==null?enemy:nearest;
    }
    @Override public int onHitProc(Char enemy,int damage,boolean ranged){
        Hero hero=Dungeon.hero;
        if(!active||level<=0||hero.MP<getManaCost()||enemy==null||!enemy.isAlive()
                ||enemy.alignment!=Char.Alignment.ENEMY||damage<=0){clearLinks();return damage;}
        hero.MP-=getManaCost();
        link(enemy,automaticPartner(enemy),level);
        return damage;
    }
    public static void clearLinks(){
        for(Char ch:Actor.chars())for(SkillInteractions.Mark mark:ch.buffs(SkillInteractions.Mark.class))
            if(mark.kind==SkillInteractions.Mark.CUT)mark.detach();
    }
    public static void link(Char first,Char second,int rank){
        clearLinks();
        SkillInteractions.Mark a=SkillInteractions.mark(first,SkillInteractions.Mark.CUT,rank,8);
        SkillInteractions.Mark b=SkillInteractions.mark(second,SkillInteractions.Mark.CUT,rank,8);
        a.other=second.id();b.other=first.id();
    }
    @Override public int getManaCost(){return 6;}
    @Override protected boolean upgrade(){return true;}
}

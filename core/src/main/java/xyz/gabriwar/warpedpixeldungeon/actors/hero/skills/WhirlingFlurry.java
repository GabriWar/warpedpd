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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillSequence;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;

import java.util.*;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.*;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.*;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.*;
import xyz.gabriwar.warpedpixeldungeon.items.KindOfWeapon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfForce;

public class WhirlingFlurry extends ActiveSkill3 {

	//damage comes from the weapon or strength, which already grow with the hero
	@Override
	public boolean weaponScaled(){ return true; }

    {name="Whirling Flurry";castText="Flurry!";image=99;mana=5;tier=3;}
    @Override public boolean toggleable(){return false;}
    @Override public void restoreInBundle(com.watabou.utils.Bundle bundle){super.restoreInBundle(bundle);active=false;}
    @Override public ArrayList<String> actions(Hero hero){ArrayList<String> out=new ArrayList<>();if(level>0&&hero.MP>=getManaCost())out.add(AC_CAST);return out;}
    @Override public void execute(Hero hero,String action){
        if(!AC_CAST.equals(action)||level<=0||hero.MP<getManaCost())return;
        KindOfWeapon weapon=hero.belongings.weapon();
        int damage=Math.round((weapon==null?RingOfForce.damageRoll(hero):weapon.damageRoll(hero))*(.5f+.2f*level));
        //the damage lands at once; the cuts are drawn going round clockwise, one tile after another
        final int[] ring={-Dungeon.level.width()-1,-Dungeon.level.width(),-Dungeon.level.width()+1,1,Dungeon.level.width()+1,Dungeon.level.width(),Dungeon.level.width()-1,-1};
        FxTimeline cuts=FxTimeline.start();
        int struck=0;
        for(int i=0;i<ring.length;i++){
            final int c=hero.pos+ring[i];if(!SkillInteractions.valid(c)||!SkillInteractions.clear(hero.pos,c))continue;
            final Char enemy=Actor.findChar(c);
            final boolean hit=enemy!=null&&enemy.alignment==Char.Alignment.ENEMY;
            final float pitch=1.0f+0.08f*struck;
            cuts.at(0.04f*i,()->{
                WhirlHitFX.show(c);
                if(hit&&enemy.sprite!=null&&enemy.isAlive()){enemy.sprite.flash();Sample.INSTANCE.play(Assets.Sounds.HIT_SLASH,0.8f,pitch);}
            });
            if(!hit)continue;
            struck++;
            enemy.damage(damage,hero);
            if(level>=2&&enemy.isAlive())Buff.affect(enemy,Bleeding.class).set(level);
        }
        if(level>=MAX_LEVEL)SkillSequence.start(hero,SkillSequence.BLADESTORM,1,hero.pos,Math.max(1,damage/3),3,java.util.Collections.emptyList());
        //two blade arcs whipping round the duelist in opposite directions
        ArcSpinFX.around(hero.sprite,0xFFFFFF,12,0.3f,0,1000,0.4f);
        ArcSpinFX.around(hero.sprite,0xCCE4FF,9,0.22f,180,-820,0.4f);
        hero.sprite.operate(hero.pos);
        Sample.INSTANCE.play(Assets.Sounds.MISS,1f,0.8f);
        Sample.INSTANCE.play(Assets.Sounds.MISS,0.7f,1.1f);
        hero.MP-=getManaCost();castTextYell();Invisibility.dispel();hero.heroSkills.lastUsed=this;hero.spendAndNext(TIME_TO_USE);
    }
    @Override public int getManaCost(){return (int)Math.ceil(mana*(1+.4*level));}
    @Override protected boolean upgrade(){return true;}
}

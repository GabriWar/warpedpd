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


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import com.watabou.utils.PathFinder;

public class WhirlingFlurry extends ActiveSkill3 {

	{
		name = "Whirling Flurry";
		castText = "Flurry!";
		image = 99;
		mana = 5;
	}

	@Override
	public float damageModifier(){
		if (!active || Dungeon.hero.MP < getManaCost())
			return 1f;
		else {
			return 0.4f + 0.2f * level;
		}
	}

	@Override
	public boolean AoEDamage(){
		if (!active || Dungeon.hero.MP < getManaCost())
			return false;
		else {
			castTextYell();
			Dungeon.hero.MP -= getManaCost();
			bleedEveryoneAround();
			return true;
		}
	}

	//the whirl opens a cut on every enemy in reach, the one you swung at included.
	//the splash damage the hero deals afterwards never runs through attackProc, so
	//onHitProc would only ever reach the primary target - the bleed is applied here
	private void bleedEveryoneAround(){
		Hero hero = Dungeon.hero;
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( hero.pos + n );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				Buff.affect( ch, Bleeding.class ).set( level );
			}
		}
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			hero.heroSkills.active1.active = false;
			hero.heroSkills.active2.active = false;
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 1 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}

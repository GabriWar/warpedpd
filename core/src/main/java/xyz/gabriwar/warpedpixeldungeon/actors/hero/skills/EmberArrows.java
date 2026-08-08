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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;

public class EmberArrows extends ActiveSkill {

	{
		tag = "A5B";
		name = "Ember Arrows";
		castText = "Burn.";
		image = 128;
		tier = 4;
		mana = 2;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			//mutually exclusive with its fork partner
			for (Skill s : hero.heroSkills.activeSkills){
				if (s instanceof FrostArrows) s.active = false;
			}
		}
	}

	@Override
	public int getManaCost(){
		//one mana per arrow, two once the pitch burns hot enough
		return level >= 3 ? 2 : 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged || !active || level == 0 || enemy == null
				|| Dungeon.hero.MP < getManaCost())
			return damage;

		Dungeon.hero.MP -= getManaCost();

		Buff.affect( enemy, Burning.class ).reignite( enemy );

		if (Dungeon.level.heroFOV[enemy.pos]){
			CellEmitter.get( enemy.pos ).burst( FlameParticle.FACTORY, 4 );
		}

		return Math.round( damage * (1f + 0.06f * level) );
	}
}

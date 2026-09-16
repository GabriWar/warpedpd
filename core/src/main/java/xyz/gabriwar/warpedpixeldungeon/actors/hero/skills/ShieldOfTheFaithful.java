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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ShieldOfTheFaithfulWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;

public class ShieldOfTheFaithful extends Skill {

	{
		name = "Shield of the Faithful";
		tag = "CBB";
		image = 157;
		tier = 4;
		level = 0;
	}

	//the shield appears at the cleric's side the moment the skill is learned
	@Override
	protected boolean upgrade(){
		if (Dungeon.hero != null) Buff.affect( Dungeon.hero, ShieldOfTheFaithfulWard.class );
		return true;
	}

	//the hovering shield catches part of the blow, cracks, and in time shatters
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || damage <= 0 || hero == null || Skill.isTickDamage( source ))
			return 0;
		return Buff.affect( hero, ShieldOfTheFaithfulWard.class ).intercept( damage, source, level );
	}
}

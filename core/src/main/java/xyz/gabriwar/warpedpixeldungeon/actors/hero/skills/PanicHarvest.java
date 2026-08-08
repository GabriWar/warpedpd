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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Amok;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;

public class PanicHarvest extends Skill {

	{
		tag = "D4A";
		name = "Panic Harvest";
		image = 117;
		tier = 4;
	}

	/** the turn the last mana tick was taken; not bundled, it only gates within a turn */
	private float lastHarvest = -1f;

	@Override
	protected boolean upgrade(){ return true; }

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || enemy == null || !isPanicked( enemy )) return damage;

		Hero hero = Dungeon.hero;
		if (hero != null && lastHarvest != Actor.now()){
			lastHarvest = Actor.now();
			hero.MP = Math.min( hero.MT, hero.MP + 1 );
		}
		return damage + Math.round( damage * 0.05f * level );
	}

	private static boolean isPanicked( Char enemy ){
		if (enemy instanceof Mob && ((Mob) enemy).state == ((Mob) enemy).FLEEING) return true;
		return enemy.buff( Terror.class ) != null
				|| enemy.buff( Amok.class ) != null
				|| enemy.buff( Charm.class ) != null;
	}
}

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


import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vulnerable;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Random;

public class TrueEdge extends Skill {

	//the crit chance can never exceed this, however many openings the target is showing
	private static final int MAX_CRIT_CHANCE = 30;

	private static final float CRIT_MULTIPLIER = 1.75f;

	{
		tag = "PB4";
		name = "True Edge";
		image = 146;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || level <= 0 || enemy == null || !enemy.isAlive()){
			return damage;
		}
		int chance = 5 * level;
		if (enemy.buff( Vulnerable.class ) != null
				|| enemy.buff( Cripple.class ) != null
				|| enemy.buff( Bleeding.class ) != null){
			chance = Math.min( 2 * chance, MAX_CRIT_CHANCE );
		}
		if (Random.Int( 100 ) < chance){
			if (enemy.sprite != null){
				enemy.sprite.showStatus( CharSprite.WARNING, "critical!" );
			}
			return Math.round( damage * CRIT_MULTIPLIER );
		}
		return damage;
	}
}

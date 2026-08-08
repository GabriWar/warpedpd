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
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

public class Heartseeker extends Skill {

	{
		tag = "PB4";
		name = "Heartseeker";
		castText = "Through the heart.";
		image = 131;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (!ranged || level == 0 || enemy == null)
			return damage;

		int threshold = 3 * level + Dungeon.hero.lvl / 4;
		if (!enemy.properties().contains( Char.Property.BOSS )
				&& !enemy.properties().contains( Char.Property.MINIBOSS )
				&& enemy.HP <= threshold){
			castTextYell();
			if (enemy.sprite != null){
				enemy.sprite.emitter().burst( Speck.factory( Speck.STAR ), 5 );
			}
			return enemy.HP + enemy.drRoll();
		}

		return Math.round( damage * (1f + 0.05f * level) );
	}
}

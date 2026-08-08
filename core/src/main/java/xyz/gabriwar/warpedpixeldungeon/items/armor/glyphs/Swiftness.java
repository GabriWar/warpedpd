/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Swiftness extends Armor.Glyph {

	private static ItemSprite.Glowing YELLOW = new ItemSprite.Glowing( 0x66FFCC );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {
		//no proc effect, triggers in Char.speed()
		return damage;
	}

	public static float speedBoost( Char owner, int level ){
		if (level == -1){
			return 1;
		}

		boolean enemyNear = false;
		//an enemy counts as 'near' if they are within a 3-tile passable path of the hero
		//yes this does mean that things like visible trap tiles and chasms count as walls
		PathFinder.buildDistanceMap(owner.pos, Dungeon.level.passable, 3);
		for (Char ch : Actor.chars()){
			if (ch.alignment == Char.Alignment.ENEMY && PathFinder.distance[ch.pos] != Integer.MAX_VALUE){
				enemyNear = true;
			}
		}
		if (enemyNear){
			return 1;
		} else {
			if (owner.sprite != null){
				int particles = 1 + (int)Random.Float(1+level/5f);
				owner.sprite.emitter().startDelayed(Speck.factory(Speck.YELLOW_LIGHT), 0.02f, particles, 0.05f);
			}
			//only the bonus above 1x scales with glyph level
			return (1f + (0.2f + 0.04f * level) * owner.glyphPower(Swiftness.class)) * genericProcChanceMultiplier(owner);
		}
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return YELLOW;
	}

}

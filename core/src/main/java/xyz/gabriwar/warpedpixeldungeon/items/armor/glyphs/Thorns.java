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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Bleeding;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

public class Thorns extends Armor.Glyph {

	private static ItemSprite.Glowing RED = new ItemSprite.Glowing( 0x557711 );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {

		int level = Math.max(0, armor.buffedLvl());

		// lvl 0 - 16.7%
		// lvl 1 - 23.1%
		// lvl 2 - 28.5%
		float procChance = (level+2f)/(level+12f) * procChanceMultiplier(defender);
		if ( attacker.alignment != defender.alignment && Random.Float() < procChance ) {

			float powerMulti = Math.max(1f, procChance);

			Buff.affect( attacker, Bleeding.class).set( Math.round((4 + level)*powerMulti*power()) ); //scales with glyph level

		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return RED;
	}
}

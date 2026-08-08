/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import com.watabou.utils.Random;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;

public class Phasing extends Armor.Glyph {

	private static ItemSprite.Glowing BLUE = new ItemSprite.Glowing( 0x66AAFF );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {

		if (Dungeon.bossLevel()) {
			return damage;
		}

		// Only triggers when defender is below 33% HP
		if (defender.HP > defender.HT / 3f) {
			return damage;
		}

		//scale the chance by armor level like other glyphs (it was always-on before)
		int level = Math.max(0, armor.buffedLvl());
		float procChance = (0.2f + 0.08f * level) * procChanceMultiplier(defender);
		if (Random.Float() < procChance) {
			ScrollOfTeleportation.teleportChar(defender);
			Buff.affect(defender, Invisibility.class, Invisibility.DURATION * power()); //scales with glyph level
		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return BLUE;
	}

}

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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.EarthImbue;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FireImbue;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Recharging;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

//ported from SPS-PD's adapt glyph: when struck, the wearer draws power from
//the terrain they are standing on
public class Adaptation extends Armor.Glyph {

	private static ItemSprite.Glowing DEEP_GREEN = new ItemSprite.Glowing( 0x006633 );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {

		int level = Math.max(0, armor.buffedLvl());

		// lvl 0 - 25%
		// lvl 1 - 33%
		// lvl 2 - 40%
		float procChance = (level+2f)/(level+8f) * procChanceMultiplier(defender);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			float duration = Math.round( 5f * powerMulti * power() ); //scales with glyph level

			int answer = -1;
			switch (Dungeon.level.map[defender.pos]) {
				case Terrain.GRASS:
					Buff.prolong( defender, EarthImbue.class, duration );
					answer = ElementRows.GRASS;
					break;
				case Terrain.WATER:
					Buff.prolong( defender, Haste.class, duration );
					answer = ElementRows.WATER;
					break;
				case Terrain.HIGH_GRASS:
				case Terrain.FURROWED_GRASS:
					Buff.prolong( defender, Invisibility.class, duration );
					answer = ElementRows.TALL_GRASS;
					break;
				case Terrain.EMBERS:
					Buff.affect( defender, FireImbue.class ).set( duration );
					answer = ElementRows.EMBERS;
					break;
				case Terrain.INACTIVE_TRAP:
					Buff.prolong( defender, Recharging.class, duration );
					answer = ElementRows.TRAP;
					break;
			}
			//plain floor gives nothing, so nothing shows
			if (answer >= 0){
				EnchantFx.ward( EnchantLooks.of( getClass() ), defender, attacker, ElementRows.verdantStrength( level() ), level(), Quality.isVestige( armor, this ) ? EnchantFx.ECHO : 0,
						ElementRows.adaptation( answer ) );
			}

		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return DEEP_GREEN;
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

//ported from SPS-PD's crystal glyph: heavy blows cause the armor to
//crystallize, granting a protective barrier
public class Crystal extends Armor.Glyph {

	private static ItemSprite.Glowing ICE_WHITE = new ItemSprite.Glowing( 0xDDEEFF );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {

		int level = Math.max(0, armor.buffedLvl());

		//only triggers on heavy blows
		if (damage < defender.HT/10) {
			return damage;
		}

		// lvl 0 - 28.5%
		// lvl 1 - 37.5%
		// lvl 2 - 44%
		float procChance = (level+2f)/(level+7f) * procChanceMultiplier(defender);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			int shield = Math.max( 1, Math.round( damage/4f * powerMulti * power() )); //scales with glyph level
			Buff.affect( defender, Barrier.class ).setShield( shield );
			defender.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( shield ), FloatingText.SHIELDING );

		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return ICE_WHITE;
	}
}

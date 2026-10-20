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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfCleansing;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

//ported from SPS-PD's revival glyph: when struck, the armor has a chance to
//wash away all harmful effects afflicting the wearer
public class Purity extends Armor.Glyph {

	private static ItemSprite.Glowing PALE_GOLD = new ItemSprite.Glowing( 0xFFFFCC );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {

		int level = Math.max(0, armor.buffedLvl());

		// lvl 0 - 10%
		// lvl 1 - 18%
		// lvl 2 - 25%
		float procChance = (level+1f)/(level+10f) * procChanceMultiplier(defender);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			//cleanses all debuffs, plus a short period of debuff immunity
			int debuffs = debuffs( defender );
			PotionOfCleansing.cleanse( defender, Math.round( powerMulti * power() )); //scales with glyph level
			defender.sprite.emitter().start( Speck.factory( Speck.LIGHT ), 0.2f, 3 );
			int removed = Math.max( 0, debuffs - debuffs( defender ) );
			EnchantFx.ward( EnchantLooks.of( getClass() ), defender, attacker, Math.min( 1f, removed / 3f ), level(), Quality.isVestige( armor, this ) ? EnchantFx.ECHO : 0, ElementRows.purity( removed ) );

		}

		return damage;
	}

	//how many debuffs a character carries (a read)
	private static int debuffs( Char ch ){
		int n = 0;
		for (Buff b : ch.buffs()){
			if (b.type == Buff.buffType.NEGATIVE) n++;
		}
		return n;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return PALE_GOLD;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.Random;

//ported from SPS-PD's dark glyph: the armor drains life from attackers,
//feeding it to the wearer
public class Siphoning extends Armor.Glyph {

	private static ItemSprite.Glowing DARK_CRIMSON = new ItemSprite.Glowing( 0x550033 );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {

		int level = Math.max(0, armor.buffedLvl());

		// lvl 0 - 10%
		// lvl 1 - 18%
		// lvl 2 - 25%
		float procChance = (level+1f)/(level+10f) * procChanceMultiplier(defender);
		if ( attacker.alignment != defender.alignment && Random.Float() < procChance ) {

			float powerMulti = Math.max(1f, procChance);

			int drain = Random.NormalIntRange( 1, 2 + level );
			drain = Math.max( 1, Math.round( drain * powerMulti * power() )); //scales with glyph level

			attacker.damage( drain, this );
			attacker.sprite.emitter().burst( ShadowParticle.UP, 5 );

			int healAmt = Math.min( drain, defender.HT - defender.HP );
			if (healAmt > 0 && defender.isAlive()) {
				defender.HP += healAmt;
				defender.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( healAmt ), FloatingText.HEALING );
			}

		}

		return damage;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return DARK_CRIMSON;
	}
}

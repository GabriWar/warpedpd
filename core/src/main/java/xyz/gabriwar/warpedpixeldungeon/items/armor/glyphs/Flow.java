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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowRows;
import com.watabou.utils.Random;

public class Flow extends Armor.Glyph {

	private static ItemSprite.Glowing BLUE = new ItemSprite.Glowing( 0x3399CC );

	@Override
	public int proc(Armor armor, Char attacker, Char defender, int damage) {
		//no proc effect, triggers in Char.speed()
		return damage;
	}

	public static float speedBoost( Char owner, int level ){
		if (level == -1){
			return 1;
		}

		//the lingering time is what scales with glyph level (and glyph power)
		float duration = (level+2)/4f * genericProcChanceMultiplier(owner) * owner.glyphPower(Flow.class);
		if (Dungeon.level.water[owner.pos] && duration > 0.5f){
			//Each step is half a turn, so an extra step every 2nd upgrade lvl essentially, assuming no other speed boosts
			Buff.prolong(owner, FlowTracker.class, duration-0.5f);
		}

		if (Dungeon.level.water[owner.pos] || owner.buff(FlowTracker.class) != null){
			if (owner.sprite != null && owner.sprite.visible){
				int particles = 2 + (int) Random.Float(1+level/2f);
				owner.sprite.emitter().startDelayed(Speck.factory(Speck.BLUE_LIGHT), 0.02f, particles, 0.05f);
				ShadowRows.flowStep( owner );
			}
			return 2f;
		}

		return 1f;
	}

	@Override
	public ItemSprite.Glowing glowing() {
		return BLUE;
	}

	public static class FlowTracker extends FlavourBuff {}

}

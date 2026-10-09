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

package xyz.gabriwar.warpedpixeldungeon.items.potions.elixirs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Healing;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHealing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMending;
import xyz.gabriwar.warpedpixeldungeon.items.potions.exotic.PotionOfCleansing;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class ElixirOfRestoration extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_RESTO;
	}

	@Override
	public void apply( Hero hero ) {
		if (Dungeon.isChallenged( Challenges.NO_HEALING )) {
			PotionOfHealing.pharmacophobiaProc( hero );
		} else {
			Buff.affect( hero, Healing.class ).setHeal( (int)(0.5f * hero.HT + 10), 0.25f, 0 );
		}
		PotionOfCleansing.cleanse( hero );
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			SpatialSound.play( Assets.Sounds.SHATTER, cell );
		}

		for (int offset : PathFinder.NEIGHBOURS9) {
			Char ch = Actor.findChar( cell + offset );
			if (ch == null || (ch != Dungeon.hero && ch.alignment != Char.Alignment.ALLY)) continue;

			if (ch == Dungeon.hero && Dungeon.isChallenged( Challenges.NO_HEALING )) {
				PotionOfHealing.pharmacophobiaProc( Dungeon.hero );
			} else {
				ch.heal( Math.round( 0.25f * ch.HT ) );
			}
			PotionOfCleansing.cleanse( ch );
		}
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfHealing.class, PotionOfMending.class};
			inQuantity = new int[]{1, 1};

			cost = 3;

			output = ElixirOfRestoration.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0x80FF80 );
	}
}

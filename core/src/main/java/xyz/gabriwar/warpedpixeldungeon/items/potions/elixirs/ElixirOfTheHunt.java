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
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TalismanOfForesight;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HuntersFocus;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHaste;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfMindVision;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class ElixirOfTheHunt extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_HUNT;
	}

	@Override
	public void apply( Hero hero ) {
		Buff.prolong( hero, MindVision.class, 20f );
		Buff.prolong( hero, HuntersFocus.class, HuntersFocus.DURATION );
		SpellSprite.show( hero, SpellSprite.VISION, 1, 0.77f, 0.9f );
		Dungeon.observe();
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
		}

		boolean revealed = false;
		for (Char ch : Actor.chars()) {
			if (ch.alignment == Char.Alignment.ENEMY && Dungeon.level.distance( cell, ch.pos ) <= 3) {
				Buff.append( Dungeon.hero, TalismanOfForesight.CharAwareness.class, 40f ).charID = ch.id();
				revealed = true;
			}
		}
		if (revealed) {
			Dungeon.observe();
			Dungeon.hero.checkVisibleMobs();
			GameScene.updateFog();
		}
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfMindVision.class, PotionOfHaste.class};
			inQuantity = new int[]{1, 1};

			cost = 2;

			output = ElixirOfTheHunt.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xFFA040 );
	}
}

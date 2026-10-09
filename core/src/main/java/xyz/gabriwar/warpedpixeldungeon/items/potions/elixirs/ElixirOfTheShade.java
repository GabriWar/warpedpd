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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Smoke;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ShadeCloak;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfInvisibility;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfShadows;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;

public class ElixirOfTheShade extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_SHADE;
	}

	@Override
	public void apply( Hero hero ) {
		Buff.prolong( hero, Invisibility.class, Invisibility.DURATION );
		Buff.prolong( hero, ShadeCloak.class, ShadeCloak.DURATION ).reset();
		GLog.i( Messages.get( this, "shade" ) );
		SpatialSound.play( Assets.Sounds.MELD, hero );
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			SpatialSound.play( Assets.Sounds.SHATTER, cell );
			SpatialSound.play( Assets.Sounds.GAS, cell );
		}

		PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
		boolean heroInside = PathFinder.distance[Dungeon.hero.pos] < Integer.MAX_VALUE;
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				GameScene.add( Blob.seed( i, 4, Smoke.class ) );
			}
		}

		if (heroInside) {
			Buff.prolong( Dungeon.hero, Invisibility.class, 3f );
			//it is the thrower's own screen: he breathes easy inside it
			Buff.prolong( Dungeon.hero, SmokeVeil.class, SmokeVeil.DURATION );
			Sample.INSTANCE.play( Assets.Sounds.MELD );
		}
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfInvisibility.class, PotionOfShadows.class};
			inQuantity = new int[]{1, 1};

			cost = 3;

			output = ElixirOfTheShade.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0x5030A0 );
	}

	//while the smoke screen lasts the thrower does not cough in it; no icon, it is part of the screen
	public static class SmokeVeil extends FlavourBuff {

		{
			type = buffType.POSITIVE;
			immunities.add( Smoke.class );
		}

		public static final float DURATION = 8f;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.items.potions.brews;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import java.util.ArrayList;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MindVision;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SunEyes;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.GlowingGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Smoke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfGlowing;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfSmoke;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

public class BlindingBrew extends Brew {

	{
		image = ItemSpriteSheet.BREW_BLINDING;
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			Sample.INSTANCE.play( Assets.Sounds.SHATTER );
			Sample.INSTANCE.play( Assets.Sounds.BLAST );
		}

		for (int offset : PathFinder.NEIGHBOURS9) {
			if (!Dungeon.level.solid[cell + offset]) {
				GameScene.add( Blob.seed( cell + offset, 2, Smoke.class ) );
				GameScene.add( Blob.seed( cell + offset, 2, GlowingGas.class ) );
			}
		}

		PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				Char ch = Actor.findChar( i );
				if (ch != null && ch != Dungeon.hero && ch.alignment != Char.Alignment.ALLY) {
					Buff.prolong( ch, Blindness.class, 6f );
				}
			}
		}
	}

	@Override
	public ArrayList<String> actions( Hero hero ) {
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_DRINK );
		return actions;
	}

	@Override
	public void apply( Hero hero ) {
		Buff.prolong( hero, MindVision.class, 15f );
		Buff.prolong( hero, SunEyes.class, SunEyes.DURATION );
		SpellSprite.show( hero, SpellSprite.VISION, 1, 0.77f, 0.9f );
		Dungeon.observe();
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfSmoke.class, PotionOfGlowing.class};
			inQuantity = new int[]{1, 1};

			cost = 2;

			output = BlindingBrew.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0xFFFFC0 );
	}
}

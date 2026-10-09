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
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Regrowth;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BloomBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfFlora;
import xyz.gabriwar.warpedpixeldungeon.items.potions.PotionOfHarvest;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

public class ElixirOfBloom extends Elixir {

	{
		image = ItemSpriteSheet.ELIXIR_BLOOM;
	}

	@Override
	public void apply( Hero hero ) {
		Buff.prolong( hero, BloomBuff.class, BloomBuff.DURATION );
		hero.sprite.emitter().burst( LeafParticle.GENERAL, 10 );
		Buff.affect( hero, BloomBuff.class ).bloom( hero.pos );
	}

	@Override
	public void shatter( int cell ) {
		splash( cell );
		if (Dungeon.level.heroFOV[cell]) {
			SpatialSound.play( Assets.Sounds.SHATTER, cell );
			SpatialSound.play( Assets.Sounds.PLANT, cell );
		}

		PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
		int plantCell = -1;
		for (int i = 0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				GameScene.add( Blob.seed( i, 2, Regrowth.class ) );
				if (Dungeon.level.heroFOV[i]) {
					CellEmitter.get( i ).burst( LeafParticle.GENERAL, 3 );
				}

				if (Dungeon.level.passable[i]
						&& !Dungeon.level.pit[i]
						&& Dungeon.level.map[i] != Terrain.ALCHEMY
						&& Dungeon.level.plants.get( i ) == null
						&& Dungeon.level.traps.get( i ) == null
						&& Actor.findChar( i ) == null
						&& (plantCell == -1 || PathFinder.distance[i] < PathFinder.distance[plantCell])) {
					plantCell = i;
				}
			}
		}

		if (plantCell != -1 && !Dungeon.isChallenged( Challenges.NO_HERBALISM )) {
			Dungeon.level.plant( (Plant.Seed) Generator.randomUsingDefaults( Generator.Category.SEED ), plantCell );
		}
	}

	public static class Recipe extends xyz.gabriwar.warpedpixeldungeon.items.Recipe.SimpleRecipe {

		{
			inputs =  new Class[]{PotionOfFlora.class, PotionOfHarvest.class};
			inQuantity = new int[]{1, 1};

			cost = 2;

			output = ElixirOfBloom.class;
			outQuantity = 1;
		}

	}

	@Override
	public ItemSprite.Glowing potionGlowing() {
		return new ItemSprite.Glowing( 0x80E040 );
	}
}

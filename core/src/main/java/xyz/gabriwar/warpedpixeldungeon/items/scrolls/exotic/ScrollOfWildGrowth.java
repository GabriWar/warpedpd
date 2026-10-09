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

package xyz.gabriwar.warpedpixeldungeon.items.scrolls.exotic;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Water;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Invisibility;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.SpellSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * Exotic Scroll of Regrowth. The plain scroll floods the level with water and maps it;
 * this one also raises tall grass all around you and grows a handful of live plants
 * from the dungeon's own seed stock.
 */
public class ScrollOfWildGrowth extends ExoticScroll {

	{
		icon = ItemSpriteSheet.Icons.SCROLL_WILDGROWTH;
	}

	private static final int RADIUS = 5;
	private static final int PLANTS = 6;

	@Override
	public void doRead() {
		detach( curUser.belongings.backpack );

		Level level = Dungeon.level;
		int length = level.length();
		boolean noticed = false;
		for (int i = 0; i < length; i++){
			GameScene.add( Blob.seed( i, 40, Water.class ) );
			int terr = level.map[i];
			if (level.discoverable[i]){
				level.mapped[i] = true;
				if ((Terrain.flags[terr] & Terrain.SECRET) != 0){
					level.discover( i );
					if (level.heroFOV[i]){
						GameScene.discoverTile( i, terr );
						CellEmitter.get( i ).start( Speck.factory( Speck.DISCOVER ), 0.1f, 4 );
						noticed = true;
					}
				}
			}
		}

		//the wild takes the ground around you: bare floor and grass become tall grass
		ArrayList<Integer> grown = new ArrayList<>();
		for (int i = 0; i < length; i++){
			if (level.distance( curUser.pos, i ) > RADIUS || i == curUser.pos) continue;
			int t = level.map[i];
			if (t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.GRASS || t == Terrain.EMBERS
					|| t == Terrain.FURROWED_GRASS){
				Level.set( i, Terrain.HIGH_GRASS );
				GameScene.updateMap( i );
				grown.add( i );
				if (level.heroFOV[i]) CellEmitter.get( i ).burst( LeafParticle.GENERAL, 3 );
			}
		}
		//and a few of them sprout living plants
		Random.shuffle( grown );
		int planted = 0;
		for (int cell : grown){
			if (planted >= PLANTS) break;
			if (level.plants.get( cell ) != null || Actor.findChar( cell ) != null) continue;
			Plant.Seed seed = (Plant.Seed) Generator.randomUsingDefaults( Generator.Category.SEED );
			if (seed == null) continue;
			level.plant( seed, cell );
			planted++;
		}

		Dungeon.observe();
		GameScene.updateFog();
		GLog.p( Messages.get( this, "growth", planted ) );
		if (noticed) SpatialSound.play( Assets.Sounds.SECRET, curUser );
		SpellSprite.show( curUser, SpellSprite.MAP );
		SpatialSound.play( Assets.Sounds.READ, curUser );
		SpatialSound.play( Assets.Sounds.PLANT, curUser );
		Invisibility.dispel();
		identify();
		curUser.spendAndNext( TIME_TO_READ );
		readAnimation();
	}

	@Override
	public int value(){
		return isKnown() ? 60 * quantity : super.value();
	}
}

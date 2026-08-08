/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2016-2019 Anon
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

package xyz.gabriwar.warpedpixeldungeon.plants;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfAwareness;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfHealth;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfTransmutation;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WellWater;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.WaterweedPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Waterweed extends Plant {

	private static final Class<?>[] WATERS =
			{WaterOfAwareness.class, WaterOfHealth.class, WaterOfTransmutation.class};

	{
		image = 65;
		livingPlantImage = 32;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return -5f; }

	//converts one cell as OV does: EMPTY/GRASS to water, EMPTY_WELL to a random well
	private void convertCell( int cell ){
		if (Dungeon.level.map[cell] == Terrain.EMPTY || Dungeon.level.map[cell] == Terrain.GRASS){
			Level.set(cell, Terrain.WATER);
			GameScene.updateMap(cell);
		}
		if (Dungeon.level.map[cell] == Terrain.EMPTY_WELL){
			@SuppressWarnings("unchecked")
			Class<? extends WellWater> waterClass = (Class<? extends WellWater>) Random.element( WATERS );
			GameScene.add(Blob.seed(cell, 1, waterClass));
			Level.set(cell, Terrain.WELL);
			GameScene.updateMap(cell);
		}
	}

	@Override
	public void activate( Char ch ) {
		int center = ch == null ? pos : ch.pos;
		for (int p : PathFinder.NEIGHBOURS8){
			convertCell(center + p);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new WaterweedPoisonParticle().getColor(), 10);
		for (int p : PathFinder.NEIGHBOURS8){
			convertCell(ch.pos + p);
		}
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		convertCell(enemy.pos);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_WATERWEED;
			plantClass = Waterweed.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			new Waterweed().attackProc(defender, damage);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return WaterweedPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new WaterweedPoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

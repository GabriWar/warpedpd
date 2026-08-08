/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2018-2019 Anon
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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.GrassvinePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Grassvine extends Plant {

	{
		image = 25;
		livingPlantImage = 59;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Level.set( enemy.pos, Terrain.HIGH_GRASS );
		GameScene.updateMap( enemy.pos );
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			Level.set( pos,  Terrain.HIGH_GRASS );
			GameScene.updateMap( pos );
			return;
		}
		Level.set( ch.pos,  Terrain.HIGH_GRASS );
		GameScene.updateMap( ch.pos );
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new GrassvinePoisonParticle().getColor(), 10);
		Level.set( ch.pos, Terrain.HIGH_GRASS );
		GameScene.updateMap( ch.pos );
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_GRASSVINE;

			plantClass = Grassvine.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Level.set( defender.pos,  Terrain.HIGH_GRASS );
			GameScene.updateMap( defender.pos );
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return GrassvinePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new GrassvinePoisonParticle();
		}
	}
}

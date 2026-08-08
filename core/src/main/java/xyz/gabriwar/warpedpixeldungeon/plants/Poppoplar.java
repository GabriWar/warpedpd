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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.PopGas;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.PoppoplarPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Poppoplar extends Plant {

	{
		image = 60;
		livingPlantImage = 50;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		GameScene.add(Blob.seed(ch == null ? pos : ch.pos, 10, PopGas.class));
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		GameScene.add(Blob.seed(enemy.pos, 10, PopGas.class));
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new PoppoplarPoisonParticle().getColor(), 10);
		int newpos;
		int trys = 8;
		do {
			newpos = ch.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			trys--;
			if (trys <= 0) {
				return;
			}
		} while (!Dungeon.level.passable[newpos]);
		GameScene.add(Blob.seed(newpos, 20, PopGas.class));
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_POPPOPLAR;
			plantClass = Poppoplar.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			GameScene.add(Blob.seed(defender.pos, 10, PopGas.class));
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return PoppoplarPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new PoppoplarPoisonParticle();
		}
	}
}

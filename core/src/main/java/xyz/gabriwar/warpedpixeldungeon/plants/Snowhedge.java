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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Snowstorm;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SnowedIn;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.SnowhedgePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Snowhedge extends Plant {

	{
		image = 17;
		livingPlantImage = 19;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return -30f; }

	@Override
	public void activate( Char ch ) {
		GameScene.add(Blob.seed(ch == null ? pos : ch.pos, 50, Snowstorm.class));
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		GameScene.add(Blob.seed(enemy.pos, 50, Snowstorm.class));
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new SnowhedgePoisonParticle().getColor(), 10);
		Buff.prolong(ch, SnowedIn.class, 4f);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_SNOWHEDGE;
			plantClass = Snowhedge.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			Buff.prolong(defender, SnowedIn.class, SnowedIn.DURATION);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return SnowhedgePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new SnowhedgePoisonParticle();
		}
	}
}

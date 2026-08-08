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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Regrowth;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Feelers;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.FeelerfernPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Feelerfern extends Plant {

	{
		image = 55;
		livingPlantImage = 45;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong(enemy, Feelers.class, Feelers.DURATION);
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			GameScene.add(Blob.seed(pos, 10, Regrowth.class));
			return;
		}
		Buff.prolong(ch, Feelers.class, Feelers.DURATION);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new FeelerfernPoisonParticle().getColor(), 10);
		Buff.prolong(ch, Feelers.class, 4f);
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_FEELERFERN;

			plantClass = Feelerfern.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Buff.prolong(attacker, Feelers.class, Feelers.DURATION);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return FeelerfernPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new FeelerfernPoisonParticle();
		}
	}
}

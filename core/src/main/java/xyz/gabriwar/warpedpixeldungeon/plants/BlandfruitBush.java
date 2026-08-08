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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.BlandfruitPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class BlandfruitBush extends Plant {

	{
		image = 12;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): drop the fruit at the plant's position
			Dungeon.level.drop( new Blandfruit(), pos ).sprite.drop();
			return;
		}
		Dungeon.level.drop( new Blandfruit(), ch.pos ).sprite.drop();
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new BlandfruitPoisonParticle().getColor(), 10);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		//OPD defaultProc
		enemy.damage(damage, this);
	}

	//seed is never dropped
	public static class Seed extends Plant.Seed {
		{
			plantClass = BlandfruitBush.class;
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return BlandfruitPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new BlandfruitPoisonParticle();
		}
	}
}

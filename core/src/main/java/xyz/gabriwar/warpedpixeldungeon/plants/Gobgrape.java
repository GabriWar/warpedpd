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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.GobgrapePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.food.Grape;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class Gobgrape extends Plant {

	{
		image = 31;
		livingPlantImage = 56;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if(Random.Float() < 0.3f) Dungeon.level.drop(new Grape(), enemy.pos).sprite.drop(enemy.pos);
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			if(Random.Float() < 0.3f) Dungeon.level.drop(new Grape(), pos).sprite.drop(pos);
			return;
		}
		if(Random.Float() < 0.3f) Dungeon.level.drop(new Grape(), ch.pos).sprite.drop(ch.pos);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new GobgrapePoisonParticle().getColor(), 10);
		Dungeon.level.drop(new Grape(), ch.pos).sprite.drop(ch.pos);
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_GOBGRAPE;

			plantClass = Gobgrape.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			if(Random.Float() < 0.3f) Dungeon.level.drop(new Grape(), defender.pos).sprite.drop(defender.pos);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return GobgrapePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new GobgrapePoisonParticle();
		}
	}
}

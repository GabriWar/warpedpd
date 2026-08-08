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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.GrasslillyPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Reflection;

public class Grasslilly extends Plant {

	{
		image = 38;
		livingPlantImage = 33;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		try {
			Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
			Plant plant = Reflection.newInstance(seed.getPlantClass());
			plant.pos = enemy.pos;
			plant.attackProc(enemy, damage);
		} catch (Exception e){
			Game.reportException(e);
		}
	}

	@Override
	public void activate( Char ch ) {
		try {
			Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
			Plant plant = Reflection.newInstance(seed.getPlantClass());
			plant.pos = pos;
			plant.activate(ch);
		} catch (Exception e){
			Game.reportException(e);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new GrasslillyPoisonParticle().getColor(), 10);
		try {
			Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
			Plant plant = Reflection.newInstance(seed.getPlantClass());
			plant.pos = ch.pos;
			plant.activate(ch);
		} catch (Exception e){
			Game.reportException(e);
		}
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_GRASSLILLY;

			plantClass = Grasslilly.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			try {
				Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
				seed.procEffect(attacker, defender, damage);
			} catch (Exception e){
				Game.reportException(e);
			}
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return GrasslillyPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new GrasslillyPoisonParticle();
		}
	}
}

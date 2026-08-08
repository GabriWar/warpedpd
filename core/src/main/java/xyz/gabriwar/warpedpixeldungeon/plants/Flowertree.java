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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.FlowertreePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Generator;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.Game;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Reflection;

public class Flowertree extends Plant {

	{
		image = 37;
		livingPlantImage = 61;
		seedClass = Seed.class;
	}

	//OV couches a random seed at the target position (not player-planted)
	private static void plantRandomSeed( int pos ){
		try {
			Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
			Plant plant = Dungeon.level.plant(seed, pos);
			if (plant != null) plant.playerPlanted = false;
		} catch (Exception e){
			Game.reportException(e);
		}
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		plantRandomSeed(enemy.pos);
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			plantRandomSeed(pos);
			return;
		}
		plantRandomSeed(ch.pos);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new FlowertreePoisonParticle().getColor(), 10);
		try {
			Plant.Seed seed = (Plant.Seed) Generator.random(Generator.Category.SEED);
			Reflection.newInstance(seed.getPlantClass()).spiceEffect(ch);
		} catch (Exception e){
			Game.reportException(e);
		}
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_FLOWERTREE;

			plantClass = Flowertree.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			plantRandomSeed(defender.pos);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return FlowertreePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new FlowertreePoisonParticle();
		}
	}
}

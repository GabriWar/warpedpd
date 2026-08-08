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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.CornwheatPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class Cornwheat extends Plant {

	{
		image = 34;
		livingPlantImage = 42;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		float chances = 0.2f;
		if (enemy instanceof Hero){
			chances = (((Hero)enemy).subClass == HeroSubClass.WARDEN) ? 0.8f : 0.2f;
		}
		if (Random.Float() < chances) {
			Dungeon.level.drop(new Cornwheatshaft(), enemy.pos).sprite.drop(enemy.pos);
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): two 30% rolls to drop shafts at the plant's position
			for (int a = 2; a > 0; a--){
				if (Random.Float() < 0.3f) {
					Dungeon.level.drop(new Cornwheatshaft(), pos).sprite.drop(pos);
				}
			}
			return;
		}

		float chances = 0.3f;
		if (ch instanceof Hero){
			chances = (((Hero)ch).subClass == HeroSubClass.WARDEN) ? 0.7f : 0.3f;
		}
		for (int a = 2; a > 0; a--){
			if (Random.Float() < chances) {
				Dungeon.level.drop(new Cornwheatshaft(), ch.pos).sprite.drop(ch.pos);
			}
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new CornwheatPoisonParticle().getColor(), 10);
		Dungeon.level.drop(new Cornwheatshaft(), ch.pos).sprite.drop(ch.pos);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_CORNWHEAT;
			plantClass = Cornwheat.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			if (Random.Float() < 0.1f) {
				Dungeon.level.drop(new Cornwheatshaft(), defender.pos).sprite.drop(attacker.pos);
			}
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return CornwheatPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new CornwheatPoisonParticle();
		}
	}
}

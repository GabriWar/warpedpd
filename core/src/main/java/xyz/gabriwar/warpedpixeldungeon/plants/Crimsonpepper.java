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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Dehydrated;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HeatAura;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.CrimsonpepperPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Crimsonpepper extends Plant {

	{
		image = 27;
		livingPlantImage = 29;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return 20f; }

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong(enemy, Dehydrated.class, Dehydrated.DURATION);
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OV no-arg activate: spawnLasher(pos), which is a no-op
			return;
		}
		if(ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.prolong(ch, HeatAura.class, HeatAura.DURATION);
		} else {
			Buff.prolong(ch, Dehydrated.class, Dehydrated.DURATION);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new CrimsonpepperPoisonParticle().getColor(), 10);
		Buff.prolong(ch, HeatAura.class, 4f);
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_CRIMSONPEPPER;

			plantClass = Crimsonpepper.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Buff.prolong(defender, Dehydrated.class, Dehydrated.DURATION);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return CrimsonpepperPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new CrimsonpepperPoisonParticle();
		}
	}
}

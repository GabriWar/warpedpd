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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.ClockcypressPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class Clockcypress extends Plant {

	{
		image = 50;
		livingPlantImage = 54;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong( enemy, Slow.class, Slow.DURATION );
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): time stasis with 3-4 charges
			TimekeepersHourglass timekeepersHourglass = new TimekeepersHourglass();
			timekeepersHourglass.activateTimeStasis(Random.Int(3, 5));
			return;
		}

		TimekeepersHourglass timekeepersHourglass = new TimekeepersHourglass();
		int time = ch.attackSkill(ch);
		if (ch instanceof Hero){
			time = (((Hero) ch).subClass == HeroSubClass.WARDEN) ? 10 : ch.attackSkill(ch);
		}
		timekeepersHourglass.activateTimeFreeze(time);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new ClockcypressPoisonParticle().getColor(), 10);
		TimekeepersHourglass timekeepersHourglass = new TimekeepersHourglass();
		timekeepersHourglass.activateTimeStasis(4);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_CLOCKCYPRESS;
			plantClass = Clockcypress.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			TimekeepersHourglass timekeepersHourglass = new TimekeepersHourglass();
			timekeepersHourglass.activateTimeFreeze(damage);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return ClockcypressPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new ClockcypressPoisonParticle();
		}
	}
}

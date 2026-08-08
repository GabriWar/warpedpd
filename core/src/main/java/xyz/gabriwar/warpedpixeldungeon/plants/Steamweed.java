/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2016-2019 Anon
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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Steaming;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.SteamweedPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Steamweed extends Plant {

	{
		image = 29;
		livingPlantImage = 21;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return 15f; }

	@Override
	public void activate( Char ch ) {
		if (ch != null) {
			Buff.affect(ch, Steaming.class).set(ch.attackSkill(ch));
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new SteamweedPoisonParticle().getColor(), 10);
		Buff.affect(ch, Steaming.class).set(4f);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.affect(enemy, Steaming.class).set(damage);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_STEAMWEED;
			plantClass = Steamweed.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Buff.affect(defender, Steaming.class).set(damage);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return SteamweedPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new SteamweedPoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Levitation;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.StormvinePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Stormvine extends Plant {

	{
		image = 5;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {

		if (ch != null) {
			if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
				Buff.affect(ch, Levitation.class, 10f);
			} else {
				Buff.affect(ch, Vertigo.class, Vertigo.DURATION);
			}
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new StormvinePoisonParticle().getColor(), 10);
		Buff.affect(ch, Levitation.class, 4f);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy != null) {
			if (enemy instanceof Hero && ((Hero) enemy).subClass == HeroSubClass.WARDEN){
				Buff.affect(enemy, Levitation.class, 10f);
			} else {
				Buff.affect(enemy, Vertigo.class, Vertigo.DURATION);
			}
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_STORMVINE;

			plantClass = Stormvine.class;
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return StormvinePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new StormvinePoisonParticle();
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			new Stormvine().attackProc(defender, damage);
		}
	}
}

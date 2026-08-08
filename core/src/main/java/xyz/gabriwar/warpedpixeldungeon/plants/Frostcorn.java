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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.IceAura;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.FrostcornPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class Frostcorn extends Plant {

	{
		image = 18;
		livingPlantImage = 26;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return -20f; }

	@Override
	public void attackProc( Char enemy, int damage ) {
		if(enemy instanceof Hero && ((Hero) enemy).subClass == HeroSubClass.WARDEN){
			Buff.prolong(enemy, IceAura.class, IceAura.DURATION);
		} else {
			Buff.prolong( enemy, Frost.class, Frost.DURATION * Random.Float( 1.0f, 1.5f ) );
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OV no-arg activate: spawnLasher(pos), which is a no-op
			return;
		}
		if(ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.prolong(ch, IceAura.class, IceAura.DURATION*10);
		} else {
			Buff.prolong(ch, IceAura.class, IceAura.DURATION);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new FrostcornPoisonParticle().getColor(), 10);
		Buff.prolong(ch, IceAura.class, 4f);
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_FROSTCORN;

			plantClass = Frostcorn.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Buff.prolong( defender, Frost.class, Frost.DURATION * Random.Float( 1.0f, 1.5f ) );
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return FrostcornPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new FrostcornPoisonParticle();
		}
	}
}

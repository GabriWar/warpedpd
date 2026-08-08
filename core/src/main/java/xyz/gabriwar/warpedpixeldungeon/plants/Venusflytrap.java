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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.NectarWind;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Secreting;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Vertigo;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.VenusflytrapPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Venusflytrap extends Plant {

	{
		image = 64;
		livingPlantImage = 37;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			GameScene.add(Blob.seed(pos, 50, NectarWind.class));
			return;
		}

		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.affect( ch, Secreting.class ).setHeal(Math.round(ch.HT/2), 0.25f, 0);
		} else {
			Buff.affect( ch, Secreting.class ).setHeal(Math.round(ch.HT/4), 0.25f, 0);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new VenusflytrapPoisonParticle().getColor(), 10);
		Buff.affect( ch, Secreting.class ).setHeal(4, 2f, 2);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy != null) {
			if (enemy instanceof Hero && ((Hero) enemy).subClass == HeroSubClass.WARDEN){
				Buff.affect( enemy, Secreting.class ).setHeal(Math.round(enemy.HT/2), 0.25f, 0);
			} else {
				Buff.affect( enemy, Secreting.class ).setHeal(Math.round(enemy.HT/4), 0.25f, 0);
			}
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_VENUSFLYTRAP;
			plantClass = Venusflytrap.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Buff.prolong(defender, Vertigo.class, Vertigo.DURATION);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return VenusflytrapPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new VenusflytrapPoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

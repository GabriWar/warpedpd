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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.PoisonGas;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Poison;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ToxicImbue;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.SorrowmossPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Sorrowmoss extends Plant {

	{
		image = 6;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			GameScene.add(Blob.seed(pos, 50, PoisonGas.class));
			return;
		}

		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.affect(ch, ToxicImbue.class).set(15f);
		}

		Buff.affect( ch, Poison.class ).set( 5 + Math.round(2*Dungeon.depth / 3f) );

		if (Dungeon.level.heroFOV[ch.pos]) {
			CellEmitter.center( ch.pos ).burst( PoisonParticle.SPLASH, 3 );
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new SorrowmossPoisonParticle().getColor(), 10);
		Buff.affect(ch, ToxicImbue.class).set(4f);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy instanceof Hero && ((Hero) enemy).subClass == HeroSubClass.WARDEN){
			Buff.affect(enemy, ToxicImbue.class).set(15f);
		}

		if (enemy != null) {
			Buff.affect( enemy, Poison.class ).set( 5 + Math.round(2*Dungeon.depth / 3f) );
		}

		if (Dungeon.level.heroFOV[enemy.pos]) {
			CellEmitter.center( enemy.pos ).burst( PoisonParticle.SPLASH, 3 );
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_SORROWMOSS;

			plantClass = Sorrowmoss.class;
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return SorrowmossPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new SorrowmossPoisonParticle();
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			new Sorrowmoss().attackProc(defender, damage);
		}
	}
}

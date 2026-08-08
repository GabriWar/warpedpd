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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.HalomethaneFire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.HalomethaneBurning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.TankcabbagePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Tankcabbage extends Plant {

	{
		image = 63;
		livingPlantImage = 47;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			GameScene.add( Blob.seed( pos, 1+2, HalomethaneFire.class ) );
			return;
		}

		GameScene.add( Blob.seed( ch.pos, 1+2, HalomethaneFire.class ) );
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new TankcabbagePoisonParticle().getColor(), 10);
		Buff.affect(ch, HalomethaneBurning.class).reignite(ch);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		GameScene.add( Blob.seed( enemy.pos, 1+2, HalomethaneFire.class ) );
	}

	@Override
	public Class<?> immunity() {
		return HalomethaneFire.class;
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_TANKCABBAGE;
			plantClass = Tankcabbage.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Buff.affect(defender, HalomethaneBurning.class).reignite(defender);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return TankcabbagePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new TankcabbagePoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

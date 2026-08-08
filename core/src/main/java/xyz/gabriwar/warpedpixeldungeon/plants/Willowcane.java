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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Depressant;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.WillowcanePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Willowcane extends Plant {

	{
		image = 19;
		livingPlantImage = 27;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return -5f; }

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			GameScene.add(Blob.seed(pos, 50, Depressant.class));
			return;
		}

		if (ch instanceof Mob){
			if (!ch.properties().contains(Char.Property.INORGANIC)){
				Buff.prolong( ch, Slow.class, Slow.DURATION );
			}
		} else {
			Buff.prolong( ch, Slow.class, Slow.DURATION );
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new WillowcanePoisonParticle().getColor(), 10);
		Buff.prolong( ch, Slow.class, 4f);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy instanceof Mob){
			if (!enemy.properties().contains(Char.Property.INORGANIC)){
				Buff.prolong( enemy, Slow.class, Slow.DURATION );
			}
		} else {
			Buff.prolong( enemy, Slow.class, Slow.DURATION );
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_WILLOWCANE;
			plantClass = Willowcane.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Buff.prolong( defender, Slow.class, Slow.DURATION );
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return WillowcanePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new WillowcanePoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

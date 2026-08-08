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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.BeetleInfected;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Beetle;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.LarvaleavePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.noosa.tweeners.AlphaTweener;

public class Larvaleaf extends Plant {

	{
		image = 57;
		livingPlantImage = 46;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			Beetle beetle = new Beetle();
			beetle.pos = pos;
			beetle.spawn(Dungeon.depth);
			GameScene.add( beetle );
			Actor.addDelayed( new Pushing( beetle, pos, pos ), -1f );

			beetle.sprite.alpha( 0 );
			beetle.sprite.parent.add( new AlphaTweener( beetle.sprite, 1, 0.15f ) );
			return;
		}
		Buff.prolong(ch, BeetleInfected.class, BeetleInfected.DURATION);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong(enemy, BeetleInfected.class, BeetleInfected.DURATION);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new LarvaleavePoisonParticle().getColor(), 10);
		Buff.prolong(ch, BeetleInfected.class, 4f);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_LARVALEAF;
			plantClass = Larvaleaf.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			Buff.prolong(defender, BeetleInfected.class, BeetleInfected.DURATION);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return LarvaleavePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new LarvaleavePoisonParticle();
		}
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cocoshield;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Cripple;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Cococlam;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.CocostuftPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.noosa.tweeners.AlphaTweener;

public class Cocostuft extends Plant {

	{
		image = 51;
		livingPlantImage = 43;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy instanceof Hero && ((Hero) enemy).subClass == HeroSubClass.WARDEN){
			Buff.prolong( enemy, Cocoshield.class, Cocoshield.DURATION );
		} else {
			Buff.prolong( enemy, Cripple.class, Cripple.DURATION );
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): spawn a Cococlam
			Cococlam cococlam = new Cococlam();
			cococlam.pos = pos;
			cococlam.spawn(Dungeon.depth);
			GameScene.add( cococlam );
			Actor.addDelayed( new Pushing( cococlam, pos, pos ), -1f );

			cococlam.sprite.alpha( 0 );
			cococlam.sprite.parent.add( new AlphaTweener( cococlam.sprite, 1, 0.15f ) );
			return;
		}

		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.prolong( ch, Cocoshield.class, Cocoshield.DURATION );
		} else {
			Buff.prolong( ch, Cripple.class, Cripple.DURATION );
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new CocostuftPoisonParticle().getColor(), 10);
		Buff.prolong( ch, Cocoshield.class, 4f );
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_COCOSTUFT;
			plantClass = Cocostuft.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			Buff.prolong( defender, Cripple.class, Cripple.DURATION );
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return CocostuftPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new CocostuftPoisonParticle();
		}
	}
}

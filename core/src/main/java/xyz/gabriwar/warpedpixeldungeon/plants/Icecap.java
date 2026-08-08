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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FrostImbue;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.IceCapPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;

public class Icecap extends Plant {

	{
		image = 4;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return -15f; }

	@Override
	public void activate( Char ch ) {

		if (ch == null) {
			freeze( pos );
			return;
		}

		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.affect(ch, FrostImbue.class, 15f);
		}

		freeze( ch.pos );
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy instanceof Hero && ((Hero) enemy).subClass == HeroSubClass.WARDEN){
			Buff.affect(enemy, FrostImbue.class, 15f);
		}

		freeze( enemy.pos );
	}

	//OV builds a distance map of radius 1 around the target and freezes every
	//reachable cell; WPD's Freezing.affect(cell) extinguishes fire internally,
	//so the explicit Fire blob argument from OV is not needed
	private void freeze( int center ) {
		PathFinder.buildDistanceMap( center, BArray.not( Dungeon.level.losBlocking, null ), 1 );

		for (int i=0; i < PathFinder.distance.length; i++) {
			if (PathFinder.distance[i] < Integer.MAX_VALUE) {
				Freezing.affect( i );
			}
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new IceCapPoisonParticle().getColor(), 10);
		Buff.affect(ch, FrostImbue.class, 4f);
	}

	@Override
	public Class<?> immunity() {
		return Freezing.class;
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_ICECAP;

			plantClass = Icecap.class;
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return IceCapPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new IceCapPoisonParticle();
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			new Icecap().attackProc(defender, damage);
		}
	}
}

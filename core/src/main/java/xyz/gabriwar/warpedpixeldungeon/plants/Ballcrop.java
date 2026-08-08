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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Balling;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SuperBalling;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.BallcropPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Ballcrop extends Plant {

	{
		image = 45;
		livingPlantImage = 40;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy instanceof Hero && ((Hero) enemy).subClass == HeroSubClass.WARDEN){
			Buff.prolong(enemy, SuperBalling.class, SuperBalling.DURATION);
		} else {
			Buff.prolong(enemy, Balling.class, Balling.DURATION);
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): all mobs get Balling, hero gets (Super)Balling, at half duration
			for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
				Buff.prolong(mob, Balling.class, Balling.DURATION/2);
			}
			if (Dungeon.hero != null){
				if (Dungeon.hero.subClass == HeroSubClass.WARDEN){
					Buff.prolong(Dungeon.hero, SuperBalling.class, SuperBalling.DURATION/2);
				} else {
					Buff.prolong(Dungeon.hero, Balling.class, Balling.DURATION/2);
				}
			}
			return;
		}

		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.prolong(ch, SuperBalling.class, SuperBalling.DURATION);
		} else {
			Buff.prolong(ch, Balling.class, Balling.DURATION);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new BallcropPoisonParticle().getColor(), 10);
		Buff.prolong(ch, SuperBalling.class, 4f);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_BALLCROP;
			plantClass = Ballcrop.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			if (defender instanceof Hero && ((Hero) defender).subClass == HeroSubClass.WARDEN){
				Buff.prolong(defender, SuperBalling.class, SuperBalling.DURATION);
			} else {
				Buff.prolong(defender, Balling.class, Balling.DURATION);
			}
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return BallcropPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new BallcropPoisonParticle();
		}
	}
}

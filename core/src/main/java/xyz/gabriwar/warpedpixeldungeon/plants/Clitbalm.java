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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Charm;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.ClitbalmPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class Clitbalm extends Plant {

	{
		image = 21;
		livingPlantImage = 52;
		seedClass = Seed.class;
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy instanceof Mob){
			if (((Mob) enemy).getEnemy() != null && enemy.buff(Charm.class) != null) Buff.prolong(enemy, Charm.class, 10f).object = ((Mob) enemy).getEnemy().id();
		}
		if (enemy instanceof Hero){
			if (((Hero) enemy).enemy() != null) Buff.prolong(enemy, Charm.class, 10f).object = ((Hero) enemy).enemy().id();
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): 50/50 charm the hero towards mobs,
			//or charm already-charmed mobs towards their enemies
			if (Random.Int(2) == 0){
				for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
					Buff.prolong(Dungeon.hero, Charm.class, 10f).object = mob.id();
				}
			} else {
				for (Mob mob : Dungeon.level.mobs.toArray(new Mob[0])){
					if (mob.getEnemy() != null && mob.buff(Charm.class) != null) Buff.prolong(mob, Charm.class, 10f).object = mob.getEnemy().id();
				}
			}
			return;
		}

		if (ch instanceof Mob){
			if (((Mob) ch).getEnemy() != null && ch.buff(Charm.class) != null) Buff.prolong(ch, Charm.class, 10f).object = ((Mob) ch).getEnemy().id();
		}
		if (ch instanceof Hero){
			if (((Hero) ch).enemy() != null) Buff.prolong(ch, Charm.class, 10f).object = ((Hero) ch).enemy().id();
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new ClitbalmPoisonParticle().getColor(), 10);
		if (ch instanceof Hero){
			if (((Hero) ch).enemy() != null) Buff.prolong(ch, Charm.class, 20f).object = ((Hero) ch).enemy().id();
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_CLITBALM;
			plantClass = Clitbalm.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			Buff.prolong(defender, Charm.class, 10f).object = attacker.id();
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return ClitbalmPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new ClitbalmPoisonParticle();
		}
	}
}

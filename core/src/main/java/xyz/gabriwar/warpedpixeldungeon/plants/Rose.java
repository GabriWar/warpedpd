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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.RoseBarrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.RosePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.DriedRose;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Rose extends Plant {

	{
		image = 20;
		livingPlantImage = 36;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//drop DriedRose petals based on depth
			DriedRose rose = Dungeon.hero.belongings.getItem( DriedRose.class );
			if (rose != null && rose.isIdentified() && !rose.cursed){
				//aim to drop 1 petal every 2 floors
				int petalsNeeded = (int) Math.ceil((float)((Dungeon.depth / 2) - rose.droppedPetals) / 3);

				for (int i=1; i <= petalsNeeded; i++) {
					//the player may miss a single petal and still max their rose.
					if (rose.droppedPetals < 11) {
						Dungeon.level.drop(new DriedRose.Petal(), pos).sprite.drop(pos);
						rose.droppedPetals++;
					}
				}
			}
			return;
		}

		if (ch instanceof Hero){
			if (((Hero) ch).belongings.weapon() instanceof MeleeWeapon){
				int t = ((MeleeWeapon) ((Hero) ch).belongings.weapon()).tier;
				int d = Math.abs(t-5)+1;
				Buff.prolong(ch, RoseBarrier.class, d);
				return;
			}
		}
		Buff.prolong(ch, RoseBarrier.class, RoseBarrier.DURATION);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.affect(enemy, Thorns.class).set(damage);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new RosePoisonParticle().getColor(), 10);
		Buff.prolong(ch, RoseBarrier.class, 4f);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_ROSE;
			plantClass = Rose.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			Buff.affect(defender, Thorns.class).set(damage);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return RosePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new RosePoisonParticle();
		}
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.BlueEyedSusanPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Dewdrop;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfRemoveCurse;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Blueeyedsusan extends Plant {

	{
		image = 42;
		livingPlantImage = 41;
		seedClass = Seed.class;
	}

	//OPD: warden heroes get all cursed items uncursed, other heroes only their weapon;
	//mobs are healed to full
	private static void uncurseEffect( Char ch ) {
		if (ch instanceof Hero){
			if (((Hero) ch).subClass == HeroSubClass.WARDEN){
				boolean uncursed = false;
				for (Item item : ((Hero) ch).belongings){
					if (item.cursed){
						uncursed = ScrollOfRemoveCurse.uncurse((Hero) ch, item);
					}
				}
				if (uncursed){
					new Flare( 6, 32 ).show( ch.sprite, 1f );
				}
			} else
			if (((Hero) ch).belongings.weapon != null && ((Hero) ch).belongings.weapon.cursed){
				boolean bool = ScrollOfRemoveCurse.uncurse((Hero) ch, ((Hero) ch).belongings.weapon);
				if (bool) new Flare( 6, 32 ).show( ch.sprite, 1f );
			}
		}
		if (ch instanceof Mob){
			ch.HP = ch.HT;
		}
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		uncurseEffect(enemy);
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): drop a Dewdrop
			Dungeon.level.drop(new Dewdrop(), pos).sprite.drop(pos);
			return;
		}
		uncurseEffect(ch);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new BlueEyedSusanPoisonParticle().getColor(), 10);
		Dungeon.level.drop(new Dewdrop(), ch.pos).sprite.drop(ch.pos);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_BLUEEYEDSUSAN;
			plantClass = Blueeyedsusan.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			if (attacker instanceof Hero){
				if (((Hero) attacker).subClass == HeroSubClass.WARDEN){
					boolean uncursed = false;
					for (Item item : ((Hero) attacker).belongings){
						if (item.cursed){
							uncursed = ScrollOfRemoveCurse.uncurse((Hero) attacker, item);
						}
					}
					if (uncursed){
						new Flare( 6, 32 ).show( attacker.sprite, 1f );
					}
				} else
				if (((Hero) attacker).belongings.weapon != null && ((Hero) attacker).belongings.weapon.cursed){
					boolean bool = ScrollOfRemoveCurse.uncurse((Hero) attacker, ((Hero) attacker).belongings.weapon);
					if (bool) new Flare( 6, 32 ).show( attacker.sprite, 1f );
				}
			}
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return BlueEyedSusanPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new BlueEyedSusanPoisonParticle();
		}
	}
}

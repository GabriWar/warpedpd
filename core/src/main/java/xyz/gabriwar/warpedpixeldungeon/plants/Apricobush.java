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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Healing;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.ApricobushPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.food.Aprico;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Random;

public class Apricobush extends Plant {

	{
		image = 44;
		livingPlantImage = 15;
		seedClass = Seed.class;
	}

	protected void satisfy( Hero hero ){
		Hunger hunger = hero.buff( Hunger.class );
		if (hunger == null) return;
		if (hero.subClass == HeroSubClass.WARDEN){
			hunger.satisfy( 200f );
		} else {
			hunger.satisfy( 100f );
		}
	}

	protected void starve( Hero hero ){
		Hunger hunger = hero.buff( Hunger.class );
		if (hunger == null) return;
		if (hero.subClass == HeroSubClass.WARDEN){
			hunger.affectHunger( 25f );
		} else {
			hunger.affectHunger( 50f );
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): drop an Aprico fruit
			Dungeon.level.drop(new Aprico(), pos).sprite.drop(pos);
			return;
		}
		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.affect( ch, Healing.class ).setHeal(Math.round(ch.HT/2), 0.25f, 0);
			satisfy((Hero) ch);
		} else {
			Buff.affect( ch, Healing.class ).setHeal(Math.round(ch.HT/4), 0.25f, 0);
		}
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (Random.Float() < 0.3f){
			if (enemy instanceof Hero){
				starve((Hero) enemy);
			}
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new ApricobushPoisonParticle().getColor(), 10);
		Buff.affect(ch, Healing.class).setHeal(10, 0, 1);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_APRICOBUSH;
			plantClass = Apricobush.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			if (attacker instanceof Hero){
				statisfy((Hero) attacker);
			}
			Buff.affect( attacker, Healing.class ).setHeal(Math.round(damage/10), 0.25f, 0);
		}

		private void statisfy( Hero hero ) {
			Hunger hunger = hero.buff( Hunger.class );
			if (hunger != null) hunger.satisfy( 20f );
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return ApricobushPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new ApricobushPoisonParticle();
		}
	}
}

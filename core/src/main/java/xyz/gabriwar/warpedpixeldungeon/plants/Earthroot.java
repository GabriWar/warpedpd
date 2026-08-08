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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barkskin;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.EarthrootPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.Bundle;

public class Earthroot extends Plant {

	{
		image = 8;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {

		if (ch == null) {
			//OV no-arg activate: spawnLasher(pos), which is a no-op
			return;
		}

		if (ch == Dungeon.hero) {
			if (Dungeon.hero.subClass == HeroSubClass.WARDEN){
				Buff.affect(ch, Barkskin.class).set(Dungeon.hero.lvl + 5, 5);
			} else {
				Buff.affect(ch, Armor.class).level(ch.HT);
			}
		}

		if (Dungeon.level.heroFOV[ch.pos]) {
			CellEmitter.bottom( ch.pos ).start( EarthParticle.FACTORY, 0.05f, 8 );
			PixelScene.shake( 1, 0.4f );
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new EarthrootPoisonParticle().getColor(), 10);
		Buff.affect(ch, Barkskin.class).set(2, 4);
		if (Dungeon.level.heroFOV[ch.pos]) {
			CellEmitter.bottom( ch.pos ).start( EarthParticle.FACTORY, 0.05f, 8 );
			PixelScene.shake( 1, 0.4f );
		}
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy == Dungeon.hero) {
			if (Dungeon.hero.subClass == HeroSubClass.WARDEN){
				Buff.affect(enemy, Barkskin.class).set(Dungeon.hero.lvl + 5, 5);
			} else {
				Buff.affect(enemy, Armor.class).level(enemy.HT);
			}
		}

		if (Dungeon.level.heroFOV[enemy.pos]) {
			CellEmitter.bottom( enemy.pos ).start( EarthParticle.FACTORY, 0.05f, 8 );
			PixelScene.shake( 1, 0.4f );
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_EARTHROOT;

			plantClass = Earthroot.class;

			bones = true;
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return EarthrootPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new EarthrootPoisonParticle();
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			new Earthroot().attackProc(defender, damage);
		}
	}

	public static class Armor extends Buff {

		private static final float STEP = 1f;

		private int pos;
		private int level;

		{
			type = buffType.POSITIVE;
			announced = true;
		}

		@Override
		public boolean act() {
			if (target.pos != pos) {
				detach();
			}
			spend( STEP );
			return true;
		}

		private static int blocking(){
			return (Dungeon.scalingDepth() + 5)/2;
		}

		public int absorb( int damage ) {
			if (pos != target.pos){
				detach();
				return damage;
			}
			int block = Math.min( damage, blocking());
			if (level <= block) {
				detach();
				return damage - block;
			} else {
				level -= block;
				return damage - block;
			}
		}

		public void level( int value ) {
			if (target != null) {
				if (level < value) {
					level = value;
				}
				pos = target.pos;
			}
		}

		@Override
		public int icon() {
			return BuffIndicator.ARMOR;
		}

		@Override
		public float iconFadePercent() {
			return Math.max(0, (target.HT - level) / (float) target.HT);
		}

		@Override
		public String iconTextDisplay() {
			return Integer.toString(level);
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", blocking(), level);
		}

		private static final String POS		= "pos";
		private static final String LEVEL	= "level";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( POS, pos );
			bundle.put( LEVEL, level );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			pos = bundle.getInt( POS );
			level = bundle.getInt( LEVEL );
		}
	}
}

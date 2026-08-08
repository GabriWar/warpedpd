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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heavy;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.ButterlionPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Camera;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PathFinder;

public class Butterlion extends Plant {

	{
		image = 36;
		livingPlantImage = 17;
		seedClass = Seed.class;
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new ButterlionPoisonParticle().getColor(), 10);
		if (Dungeon.level.heroFOV[ch.pos]) {
			CellEmitter.get( ch.pos ).start( Speck.factory( Speck.ROCK ), 0.07f, 10 );
			Camera.main.shake( 1, 0.7f );
			Sample.INSTANCE.play( Assets.Sounds.ROCKS );
		}
		ch.damage(1, this);
		if (ch == Dungeon.hero && !ch.isAlive()){
			Dungeon.fail(getClass());
			GLog.n( Messages.get(this, "ondeath") );
		}
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (Dungeon.level.heroFOV[enemy.pos]) {
			CellEmitter.get( enemy.pos ).start( Speck.factory( Speck.ROCK ), 0.07f, 10 );
			Camera.main.shake( 3, 0.7f );
			Sample.INSTANCE.play( Assets.Sounds.ROCKS );
		}
		enemy.damage(damage, this);
		if (enemy == Dungeon.hero && !enemy.isAlive()){
			Dungeon.fail(getClass());
			GLog.n( Messages.get(this, "ondeath") );
		}
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			Char ch = Actor.findChar(enemy.pos + PathFinder.NEIGHBOURS8[i]);
			if (ch instanceof Mob && ch.alignment != Char.Alignment.ALLY){
				ch.damage(damage, this);
			}
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): rock burst at the plant's position, damaging adjacent mobs
			if (Dungeon.level.heroFOV[pos]) {
				CellEmitter.get( pos ).start( Speck.factory( Speck.ROCK ), 0.07f, 10 );
				Camera.main.shake( 3, 0.7f );
				Sample.INSTANCE.play( Assets.Sounds.ROCKS );
			}
			for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
				Char ch1 = Actor.findChar(pos + PathFinder.NEIGHBOURS8[i]);
				if (ch1 instanceof Mob && ch1.alignment != Char.Alignment.ALLY){
					ch1.damage(Math.round(ch1.HP/8), this);
				}
			}
			return;
		}

		if (ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.prolong( ch, Heavy.class, Heavy.DURATION );
		}
		if (Dungeon.level.heroFOV[ch.pos]) {
			CellEmitter.get( ch.pos ).start( Speck.factory( Speck.ROCK ), 0.07f, 10 );
			Camera.main.shake( 3, 0.7f );
			Sample.INSTANCE.play( Assets.Sounds.ROCKS );
		}
		ch.damage(Math.round(ch.HP/8), this);
		if (ch == Dungeon.hero && !ch.isAlive()){
			Dungeon.fail(getClass());
			GLog.n( Messages.get(this, "ondeath") );
		}
		for (int i = 0; i < PathFinder.NEIGHBOURS8.length; i++) {
			Char ch1 = Actor.findChar(ch.pos + PathFinder.NEIGHBOURS8[i]);
			if (ch1 instanceof Mob && ch1.alignment != Char.Alignment.ALLY){
				ch1.damage(Math.round(ch.HP/8), this);
			}
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_BUTTERLION;
			plantClass = Butterlion.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect( Char attacker, Char defender, int damage ) {
			new Butterlion().attackProc(defender, damage);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return ButterlionPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new ButterlionPoisonParticle();
		}
	}
}

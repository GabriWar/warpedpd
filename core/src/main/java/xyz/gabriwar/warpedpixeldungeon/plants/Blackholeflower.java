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
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpaceTimePowers;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.BlackholeflowerPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Blackholeflower extends Plant {

	{
		image = 47;
		livingPlantImage = 25;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return -50f; }

	//how hard the hole bites the air when it opens, and how far the cold carries
	private static final float COLD_CORE = 120f;
	private static final float COLD_EDGE = 55f;

	/**
	 * Space folding costs heat: the air where the hole opens is dragged down to
	 * nothing, and whoever it swallows comes out the other side frozen stiff.
	 */
	private static void chillAround( int cell ) {
		if (Dungeon.level == null) return;
		TileTemperature.depositHeat( cell, -COLD_CORE );
		if (Dungeon.level.heroFOV[cell]) CellEmitter.get( cell ).burst( SnowParticle.FACTORY, 10 );
		int w = Dungeon.level.width();
		int cx = cell % w;
		for (int offset : PathFinder.NEIGHBOURS8) {
			int n = cell + offset;
			if (n < 0 || n >= Dungeon.level.length()) continue;
			if (Math.abs( n % w - cx ) > 1) continue;
			if (Dungeon.level.solid[n]) continue;
			TileTemperature.depositHeat( n, -COLD_EDGE );
			if (Dungeon.level.heroFOV[n]) CellEmitter.get( n ).burst( SnowParticle.FACTORY, 3 );
		}
	}

	/**
	 * The hole throws you across the floor you are on, never off it. It used to
	 * send the hero up a level, which could drop them into a sealed boss arena
	 * they had no way to leave; a teleport stays inside the level and obeys the
	 * level's own rules about where a body may land.
	 */
	private static void swallowHero( Char ch ) {
		Buff buff = ch.buff(TimekeepersHourglass.timeFreeze.class);
		if (buff != null) buff.detach();

		chillAround( ch.pos );
		ScrollOfTeleportation.teleportChar( ch, Blackholeflower.class );
		//and the cold comes out with them
		chillAround( ch.pos );
		if (!ch.isImmune( Frost.class )) Buff.affect( ch, Chill.class, 8f );
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy != null) chillAround( enemy.pos );
		if (enemy instanceof Mob && !enemy.properties().contains(Char.Property.MINIBOSS) && !enemy.properties().contains(Char.Property.BOSS)){
			if (enemy.isAlive()) Chasm.mobFall((Mob) enemy);
		}
		if (enemy instanceof Hero) {
			swallowHero( enemy );
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): spawnLasher(pos), which is a no-op
			return;
		}

		chillAround( ch.pos );
		ch.damage(Math.round(ch.HP/2), this);
		if (!(ch instanceof Hero) && !ch.isImmune( Frost.class )) Buff.affect( ch, Chill.class, 8f );
		if (ch instanceof Hero) {
			if (((Hero) ch).subClass == HeroSubClass.WARDEN){
				Buff.prolong(ch, SpaceTimePowers.class, SpaceTimePowers.DURATION);
			} else {
				swallowHero( ch );
			}
		}
		if (ch instanceof Mob && !ch.properties().contains(Char.Property.MINIBOSS) && !ch.properties().contains(Char.Property.BOSS)){
			if (ch.isAlive()) Chasm.mobFall((Mob) ch);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new BlackholeflowerPoisonParticle().getColor(), 10);
		int newpos;
		int trys = 8;
		do{
			newpos = ch.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			trys--;
			if (trys <= 0){
				return;
			}
		} while (!Dungeon.level.passable[newpos]);
		if (ch instanceof Hero) ScrollOfTeleportation.teleportToLocation(ch, newpos);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_BLACKHOLEFLOWER;
			plantClass = Blackholeflower.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}


		@Override
		public Emitter.Factory getPixelParticle() {
			return BlackholeflowerPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new BlackholeflowerPoisonParticle();
		}
	}
}

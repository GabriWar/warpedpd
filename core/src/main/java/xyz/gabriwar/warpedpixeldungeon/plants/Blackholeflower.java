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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpaceTimePowers;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.BlackholeflowerPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.TimekeepersHourglass;
import xyz.gabriwar.warpedpixeldungeon.items.scrolls.ScrollOfTeleportation;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.features.Chasm;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.InterlevelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
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

	private static void ascendHero() {
		if (Dungeon.bossLevel() || Dungeon.depth <= 1) {

			GLog.w(Messages.get(ScrollOfTeleportation.class, "no_tele"));
			return;

		}

		Buff buff = Dungeon.hero.buff(TimekeepersHourglass.timeFreeze.class);
		if (buff != null) buff.detach();

		Level.beforeTransition();
		InterlevelScene.mode = InterlevelScene.Mode.RETURN;
		InterlevelScene.returnDepth = Dungeon.depth-1;
		InterlevelScene.returnBranch = 0;
		InterlevelScene.returnPos = -1;
		Game.switchScene( InterlevelScene.class );
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy instanceof Mob && !enemy.properties().contains(Char.Property.MINIBOSS) && !enemy.properties().contains(Char.Property.BOSS)){
			if (enemy.isAlive()) Chasm.mobFall((Mob) enemy);
		}
		if (enemy instanceof Hero) {
			ascendHero();
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			//OPD no-arg activate(): spawnLasher(pos), which is a no-op
			return;
		}

		ch.damage(Math.round(ch.HP/2), this);
		if (ch instanceof Hero) {
			if (((Hero) ch).subClass == HeroSubClass.WARDEN){
				Buff.prolong(ch, SpaceTimePowers.class, SpaceTimePowers.DURATION);
			} else {
				ascendHero();
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
		public void procEffect( Char attacker, Char defender, int damage ) {
			new Blackholeflower().attackProc(defender, damage);
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

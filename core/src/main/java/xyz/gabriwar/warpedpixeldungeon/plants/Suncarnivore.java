/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2016-2019 Anon
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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.UnfilteredSunlight;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.SuncarnivorePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Suncarnivore extends Plant {

	{
		image = 62;
		livingPlantImage = 38;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return 8f; }

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			GameScene.add(Blob.seed(pos, 50, UnfilteredSunlight.class));
			return;
		}

		GameScene.add(Blob.seed(ch.pos, 50, UnfilteredSunlight.class));
		if (ch instanceof Mob && ch.properties().contains(Char.Property.UNDEAD)){
			ch.die(this);
			if (Dungeon.level.heroFOV[ch.pos]){
				ch.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10 );
			}
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new SuncarnivorePoisonParticle().getColor(), 10);
		int newpos;
		int trys = 8;
		do {
			newpos = ch.pos + PathFinder.NEIGHBOURS8[Random.Int(8)];
			trys--;
			if (trys <= 0){
				return;
			}
		} while (!Dungeon.level.passable[newpos]);
		GameScene.add(Blob.seed(newpos, 100, UnfilteredSunlight.class));
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		GameScene.add(Blob.seed(enemy.pos, 50, UnfilteredSunlight.class));
		if (enemy instanceof Mob && enemy.properties().contains(Char.Property.UNDEAD)){
			enemy.die(this);
			if (Dungeon.level.heroFOV[enemy.pos]){
				enemy.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10 );
			}
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_SUNCARNIVORE;
			plantClass = Suncarnivore.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			if (defender instanceof Mob && defender.properties().contains(Char.Property.UNDEAD)){
				defender.die(this);
				if (Dungeon.level.heroFOV[defender.pos]){
					defender.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10 );
				}
			}
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return SuncarnivorePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new SuncarnivorePoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

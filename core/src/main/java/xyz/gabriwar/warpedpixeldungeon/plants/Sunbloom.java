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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Sunlight;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.SunbloomPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.SungrassPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;

public class Sunbloom extends Plant {

	{
		image = 23;
		livingPlantImage = 20;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return 10f; }

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			GameScene.add(Blob.seed(pos, 50, Sunlight.class));
			return;
		}

		if (ch instanceof Mob && ch.properties().contains(Char.Property.UNDEAD)){
			ch.die(this);
			if (Dungeon.level.heroFOV[ch.pos]){
				ch.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10 );
			}
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new SunbloomPoisonParticle().getColor(), 10);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		if (enemy instanceof Mob && enemy.properties().contains(Char.Property.UNDEAD)){
			enemy.die(this);
			if (Dungeon.level.heroFOV[enemy.pos]){
				enemy.sprite.emitter().start( ShadowParticle.UP, 0.05f, 10 );
			}
		}
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_SUNBLOOM;
			plantClass = Sunbloom.class;
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
			return SunbloomPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			//OV quirk kept as-is: Sunbloom's seed uses the Sungrass particle colour
			return new SungrassPoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

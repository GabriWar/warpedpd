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
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Wither;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.WitherfennelPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PathFinder;

public class Witherfennel extends Plant {

	{
		image = 26;
		livingPlantImage = 30;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			for (int p : PathFinder.NEIGHBOURS8){
				if (Dungeon.level.map[pos+p] == Terrain.HIGH_GRASS || Dungeon.level.map[pos+p] == Terrain.GRASS){
					Level.set(pos+p, Terrain.FURROWED_GRASS);
					GameScene.updateMap(pos+p);
				}
				if (Dungeon.level.plants.get(pos+p) != null){
					Dungeon.level.plants.get(pos+p).wither();
				}
			}
			return;
		}

		Buff.prolong(ch, Wither.class, Wither.DURATION);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new WitherfennelPoisonParticle().getColor(), 10);
		Buff.prolong(ch, Wither.class, 4f);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		Buff.prolong(enemy, Wither.class, Wither.DURATION);
	}

	public static class Seed extends Plant.Seed {
		{
			image = ItemSpriteSheet.SEED_WITHERFENNEL;
			plantClass = Witherfennel.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			Buff.prolong(defender, Wither.class, Wither.DURATION);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return WitherfennelPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new WitherfennelPoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

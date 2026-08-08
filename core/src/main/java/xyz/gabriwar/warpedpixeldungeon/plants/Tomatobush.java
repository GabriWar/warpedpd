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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.TomatoExplosionParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.TomatobushPoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

public class Tomatobush extends Plant {

	{
		image = 41;
		livingPlantImage = 22;
		seedClass = Seed.class;
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			explode(pos, null);
			return;
		}

		explode(ch.pos, null);
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new TomatobushPoisonParticle().getColor(), 10);
		explode(ch.pos, ch);
	}

	@Override
	public void attackProc( Char enemy, int damage ) {
		explode(enemy.pos, null);
	}

	public boolean explodesDestructively(){
		return true;
	}

	public void explode(int cell, Char notEffectedChar){

		if (explodesDestructively()) {
			if (Dungeon.level.heroFOV[cell]) {
				CellEmitter.center(cell).burst(TomatoExplosionParticle.FACTORY, 60);
			}

			boolean terrainAffected = false;
			for (int n : PathFinder.NEIGHBOURS9) {
				int c = cell + n;
				if (c >= 0 && c < Dungeon.level.length()) {

					if (Dungeon.level.flamable[c]) {
						Dungeon.level.destroy(c);
						GameScene.updateMap(c);
						terrainAffected = true;
					}

					Char ch = Actor.findChar(c);
					if (ch != null && ch != notEffectedChar) {
						//those not at the center of the blast take damage less consistently.
						int minDamage = c == cell ? Dungeon.depth + 5 : 1;
						int maxDamage = 10 + Dungeon.depth * 2;

						int dmg = Random.NormalIntRange(minDamage, maxDamage) - ch.drRoll();
						if (dmg > 0) {
							ch.damage(dmg, this);
						}

						if (ch == Dungeon.hero && !ch.isAlive()) {
							GLog.n(Messages.get(this, "died"));
							Dungeon.fail(Tomatobush.class);
						}
					}
				}
			}

			if (terrainAffected) {
				Dungeon.observe();
				if (Dungeon.level.heroFOV[cell]){
					GLog.w(Messages.get(this, "explode"));
				}
			}
		}
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_TOMATOBUSH;
			plantClass = Tomatobush.class;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			new Tomatobush().explode(defender.pos, attacker);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return TomatobushPoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new TomatobushPoisonParticle();
		}

		@Override
		public int value() {
			return 30 * quantity;
		}
	}
}

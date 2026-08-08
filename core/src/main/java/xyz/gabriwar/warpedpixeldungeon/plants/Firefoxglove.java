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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.TrailOfFire;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroSubClass;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.LivingPlant;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.poisonparticles.FirefoxglovePoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.BArray;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Firefoxglove extends Plant {

	{
		image = 56;
		livingPlantImage = 16;
		seedClass = Seed.class;
	}

	@Override
	public float temperatureBonus() { return 20f; }

	@Override
	public void attackProc( Char enemy, int damage ) {
		if(enemy instanceof Hero && ((Hero) enemy).subClass == HeroSubClass.WARDEN){
			Buff.prolong(enemy, TrailOfFire.class, TrailOfFire.DURATION);
		} else {
			new Firebomb().explode(enemy.pos);
		}
	}

	@Override
	public void activate( Char ch ) {
		if (ch == null) {
			new Firebomb().explode(pos);
			return;
		}
		if(ch instanceof Hero && ((Hero) ch).subClass == HeroSubClass.WARDEN){
			Buff.prolong(ch, TrailOfFire.class, TrailOfFire.DURATION);
		} else {
			new Firebomb().explode(ch.pos);
		}
	}

	@Override
	public void spiceEffect( Char ch ) {
		ch.sprite.burst(new FirefoxglovePoisonParticle().getColor(), 10);
		Buff.prolong(ch, TrailOfFire.class, 4f);
	}

	@Override
	public Class<?> immunity() {
		return Fire.class;
	}

	public static class Seed extends Plant.Seed {

		{
			image = ItemSpriteSheet.SEED_FIREFOXGLOVE;

			plantClass = Firefoxglove.class;
		}

		@Override
		public int value() {
			return 30 * quantity;
		}

		@Override
		public void procEffect(Char attacker, Char defender, int damage) {
			new Firebomb().explode(defender.pos);
		}

		@Override
		public Emitter.Factory getPixelParticle() {
			return FirefoxglovePoisonParticle.FACTORY;
		}

		@Override
		public PixelParticle poisonEmitterClass() {
			return new FirefoxglovePoisonParticle();
		}
	}

	//OV's plant-specific firebomb: weaker blast than the regular Firebomb item,
	//spares LivingPlants and floods a 2-tile radius with fire
	public static class Firebomb extends Bomb {

		@Override
		public void explode(int cell) {
			//We're blowing up, so no need for a fuse anymore.
			this.fuse = null;

			Sample.INSTANCE.play( Assets.Sounds.BLAST );

			if (explodesDestructively()) {

				ArrayList<Char> affected = new ArrayList<>();

				if (Dungeon.level.heroFOV[cell]) {
					CellEmitter.center(cell).burst(BlastParticle.FACTORY, 30);
				}

				boolean terrainAffected = false;
				for (int n : PathFinder.NEIGHBOURS9) {
					int c = cell + n;
					if (c >= 0 && c < Dungeon.level.length()) {
						if (Dungeon.level.heroFOV[c]) {
							CellEmitter.get(c).burst(SmokeParticle.FACTORY, 4);
						}

						if (Dungeon.level.flamable[c]) {
							Dungeon.level.destroy(c);
							GameScene.updateMap(c);
							terrainAffected = true;
						}

						//destroys items / triggers bombs caught in the blast.
						Heap heap = Dungeon.level.heaps.get(c);
						if (heap != null)
							heap.explode();

						Char ch = Actor.findChar(c);
						if (ch != null) {
							affected.add(ch);
						}
					}
				}

				for (Char ch : affected){
					//those not at the center of the blast take damage less consistently.
					int minDamage = ch.pos == cell ? Dungeon.scalingDepth() + 5 : 1;
					int maxDamage = 10 + Dungeon.scalingDepth() * 2;

					int dmg = Random.NormalIntRange(minDamage, maxDamage) - ch.drRoll();
					if (dmg > 0) {
						if(!(ch instanceof LivingPlant)){
							ch.damage(dmg, this);
						}
					}

					if (ch == Dungeon.hero && !ch.isAlive()) {
						Dungeon.fail(Bomb.class);
					}
				}

				if (terrainAffected) {
					Dungeon.observe();
				}
			}

			PathFinder.buildDistanceMap( cell, BArray.not( Dungeon.level.solid, null ), 2 );
			for (int i = 0; i < PathFinder.distance.length; i++) {
				if (PathFinder.distance[i] < Integer.MAX_VALUE) {
					if (Dungeon.level.pit[i])
						GameScene.add(Blob.seed(i, 2, Fire.class));
					else
						GameScene.add(Blob.seed(i, 10, Fire.class));
					CellEmitter.get(i).burst(FlameParticle.FACTORY, 5);
				}
			}
			Sample.INSTANCE.play(Assets.Sounds.BURNING);
		}
	}
}

/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.effects.WeatherBlobFX;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class StormCloud extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		int cell;

		Fire fire = (Fire) Dungeon.level.blobs.get(Fire.class);
		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*Dungeon.level.width();
				if (cur[cell] > 0) {
					Dungeon.level.setCellToWater(true, cell);
					if (fire != null){
						fire.clear(cell);
					}

					// Drench characters caught in the storm
					Char ch = Actor.findChar(cell);
					if (ch != null && !ch.isImmune(getClass())) {
						Buff.prolong(ch, Drenched.class, 5f);

						// Fiery enemies take damage
						if (Char.hasProp(ch, Char.Property.FIERY)) {
							ch.damage(1 + Dungeon.scalingDepth() / 5, this);
						}
					}

					// Lightning strike chance per cell per turn, harder in a real storm
					float odds = 0.012f;
					if (ClimateManager.isStorming()) odds *= 2.5f;
					odds *= 0.6f + ClimateManager.localPrecipRate();
					if (Random.Float() < odds) {
						strike(cell);
					}
				}
			}
		}

		//a storm cloud with no rain left in the sky rains itself out
		xyz.gabriwar.warpedpixeldungeon.actors.PrecipType pt = ClimateManager.localPrecipType();
		boolean wet = ClimateManager.localPrecipRate() > 0.05f
				&& (pt == xyz.gabriwar.warpedpixeldungeon.actors.PrecipType.RAIN
					|| pt == xyz.gabriwar.warpedpixeldungeon.actors.PrecipType.SLEET
					|| pt == xyz.gabriwar.warpedpixeldungeon.actors.PrecipType.HAIL);
		if (!wet) dissipate(0.75f);
	}

	private void strike(int cell) {
		// Visual: lightning arc from sky to tile
		PointF ground = DungeonTilemap.tileCenterToWorld(cell);
		//it comes out of the cloud deck the blob draws two cells up, not from nowhere
		PointF sky = new PointF(ground.x + Random.IntRange(-10, 10), ground.y - Random.IntRange(34, 52));

		//a channel that zigzags down, with a fork off one of the upper joints
		ArrayList<Lightning.Arc> arcs = new ArrayList<>();
		int steps = 4 + Random.Int(3);
		PointF prev = sky;
		PointF forkFrom = null;
		for (int i = 1; i <= steps; i++) {
			float t = i / (float)steps;
			PointF next = i == steps ? ground : new PointF(
					sky.x + (ground.x - sky.x) * t + Random.Float(-7f, 7f),
					sky.y + (ground.y - sky.y) * t + Random.Float(-3f, 3f));
			arcs.add(new Lightning.Arc(prev, next));
			if (i == 1 + Random.Int(2)) forkFrom = next;
			prev = next;
		}
		if (forkFrom != null) {
			PointF a = forkFrom;
			for (int i = 0; i < 2; i++) {
				PointF b = new PointF(a.x + Random.Float(-14f, 14f), a.y + Random.Float(6f, 14f));
				arcs.add(new Lightning.Arc(a, b));
				a = b;
			}
		}

		if (Dungeon.hero.fieldOfView[cell]) {
			Dungeon.hero.sprite.parent.addToFront(new Lightning(arcs, null));
			CellEmitter.center(cell).burst(SparkParticle.FACTORY, 8);
			Sample.INSTANCE.play(Assets.Sounds.LIGHTNING, 0.8f, Random.Float(0.9f, 1.1f));
		}

		// Damage character on the tile
		Char ch = Actor.findChar(cell);
		if (ch != null && !ch.isImmune(getClass())) {
			int dmg = Random.IntRange(2, 4) + Dungeon.scalingDepth() / 3;
			ch.damage(dmg, this);
			// Brief paralysis from shock
			Buff.prolong(ch, Paralysis.class, 1f);
		}

		// Ignite flammable tiles
		if (Dungeon.level.flamable[cell]) {
			GameScene.add(Blob.seed(cell, 4, Fire.class));
		}

		// Electricity spreads through adjacent water
		for (int n : com.watabou.utils.PathFinder.NEIGHBOURS4) {
			int adj = cell + n;
			if (adj >= 0 && adj < Dungeon.level.length() && Dungeon.level.water[adj]) {
				Char adjCh = Actor.findChar(adj);
				if (adjCh != null && !adjCh.isImmune(getClass())) {
					adjCh.damage(Random.IntRange(1, 3), this);
					CellEmitter.center(adj).burst(SparkParticle.FACTORY, 3);
				}
			}
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( WeatherBlobFX.STORM_CLOUD, 0.07f );
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

}

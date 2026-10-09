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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.StormStrikes;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetVisuals;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;
import java.util.Iterator;

public class StormCloud extends Blob {

	//every cloud strikes: at least once in its life, and twice TWICE of the time, its own chance
	//strikes counting toward it. Seeds within SAME_CLOUD of one made the same moment are one cloud
	//(the weather lays a patch at once); a cloud's cells are looked for within REACH of its seed,
	//and while its thickest is FADING or less it is about to go and strikes what it owes at once
	private static final float TWICE = 0.75f;
	private static final int SAME_CLOUD = 6, REACH = 6, FADING = 6;

	private static class Owed {
		int anchor, left, age, due;
		float born = -1;
	}

	private final ArrayList<Owed> owed = new ArrayList<>();

	@Override
	public void seed(Level level, int cell, int amount) {
		super.seed(level, cell, amount);
		for (Owed o : owed) {
			if (o.born == Actor.now() && level.distance(o.anchor, cell) <= SAME_CLOUD) return;
		}
		Owed o = new Owed();
		o.anchor = cell;
		o.left = Random.Float() < TWICE ? 2 : 1;
		o.due = Random.IntRange(1, 3);
		o.born = Actor.now();
		owed.add(o);
	}

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
							ch.damage(quench(ch), this);
						}
					}

					// Lightning strike chance per cell per turn, harder in a real storm
					float odds = 0.012f;
					if (ClimateManager.isStorming()) odds *= 2.5f;
					odds *= 0.6f + ClimateManager.localPrecipRate();
					if (Random.Float() < odds) {
						strike(cell);
						credit(cell);
					}
				}
			}
		}
		payOwed();

		//a storm cloud with no rain left in the sky rains itself out
		xyz.gabriwar.warpedpixeldungeon.actors.PrecipType pt = ClimateManager.localPrecipType();
		boolean wet = ClimateManager.localPrecipRate() > 0.05f
				&& (pt == xyz.gabriwar.warpedpixeldungeon.actors.PrecipType.RAIN
					|| pt == xyz.gabriwar.warpedpixeldungeon.actors.PrecipType.SLEET
					|| pt == xyz.gabriwar.warpedpixeldungeon.actors.PrecipType.HAIL);
		if (!wet) dissipate(0.75f);
	}

	//what the rain does to a fiery one a turn: 1 + depth/5 on a dungeon floor; on the world's slices
	//by share, a fourteenth of its health (as much as a fire elemental takes deep in the dungeon), at
	//least 1, since their slot depths (97 the surface) made it 18-23 a turn
	static int quench(Char ch) {
		return Dungeon.level instanceof OverworldLevel
				? Math.max(1, Math.round(ch.HT / 14f))
				: 1 + Dungeon.scalingDepth() / 5;
	}

	//a chance strike pays toward the cloud it fell in, the nearest one still owing
	private void credit(int cell) {
		Owed to = null;
		for (Owed o : owed) {
			if (o.left > 0 && Dungeon.level.distance(o.anchor, cell) <= REACH
					&& (to == null || Dungeon.level.distance(o.anchor, cell) < Dungeon.level.distance(to.anchor, cell))) {
				to = o;
			}
		}
		if (to != null) to.left--;
	}

	//what each cloud still owes: one strike once it is due, the next a few turns on, or all of it
	//at once while it fades; a cloud gone (cleared, blown off its seed) owes nothing
	private void payOwed() {
		int w = Dungeon.level.width(), h = Dungeon.level.height();
		ArrayList<Integer> cells = new ArrayList<>();
		for (Iterator<Owed> it = owed.iterator(); it.hasNext(); ) {
			Owed o = it.next();
			o.age++;
			if (o.left <= 0) {
				it.remove();
				continue;
			}
			cells.clear();
			int most = 0;
			int ax = o.anchor % w, ay = o.anchor / w;
			for (int y = Math.max(0, ay - REACH); y <= Math.min(h - 1, ay + REACH); y++) {
				for (int x = Math.max(0, ax - REACH); x <= Math.min(w - 1, ax + REACH); x++) {
					int c = x + y * w;
					if (cur[c] > 0) {
						cells.add(c);
						most = Math.max(most, cur[c]);
					}
				}
			}
			if (cells.isEmpty()) {
				it.remove();
				continue;
			}
			boolean fading = most <= FADING;
			if (o.age < o.due && !fading) continue;
			for (int n = fading ? o.left : 1; n > 0 && !cells.isEmpty(); n--) {
				strike(cells.remove(Random.Int(cells.size())));
				o.left--;
			}
			o.due = o.age + Random.IntRange(2, 4);
		}
	}

	private static final String OWED = "owed";

	@Override
	public void storeInBundle(Bundle bundle) {
		super.storeInBundle(bundle);
		int[] flat = new int[owed.size() * 4];
		for (int i = 0; i < owed.size(); i++) {
			Owed o = owed.get(i);
			flat[i * 4] = o.anchor;
			flat[i * 4 + 1] = o.left;
			flat[i * 4 + 2] = o.age;
			flat[i * 4 + 3] = o.due;
		}
		bundle.put(OWED, flat);
	}

	@Override
	public void restoreFromBundle(Bundle bundle) {
		super.restoreFromBundle(bundle);
		owed.clear();
		if (!bundle.contains(OWED)) return;
		int[] flat = bundle.getIntArray(OWED);
		for (int i = 0; i + 3 < flat.length; i += 4) {
			Owed o = new Owed();
			o.anchor = flat[i];
			o.left = flat[i + 1];
			o.age = flat[i + 2];
			o.due = flat[i + 3];
			owed.add(o);
		}
	}

	@Override
	public void fullyClear() {
		super.fullyClear();
		owed.clear();
	}

	private void strike(int cell) {
		//on the world's slices by share, as the sky's own bolts: by their slot depths (97 the
		//surface, 87-112 the peaks and caves) it would be a blow of 31-41
		StormStrikes.Discharge d = jolt(cell, this, Dungeon.level instanceof OverworldLevel
				? StormStrikes::share
				: ch -> Random.IntRange(2, 4) + Dungeon.scalingDepth() / 3);

		//drawn as the storm's own bolts are, on the render thread (WeatherOverlay): the bolt from the
		//top of the view, its glow, its arcs, the flash and the thunder, where the hero sees it; a
		//co-op guest's overlay draws it too
		StormStrikes.show(Dungeon.level, cell, true, d.arcs, d.water);
		NetVisuals.recordStrike(cell, true, d.arcs, d.water);

		// Ignite flammable tiles
		if (Dungeon.level.flamable[cell]) {
			GameScene.add(Blob.seed(cell, 4, Fire.class));
		}
	}

	/** A bolt's blow on one it hits, before it is shared out (jolt). */
	public interface Blow {
		int on(Char ch);
	}

	/**
	 * What a bolt does where it lands, a storm cloud's or the open sky's (StormStrikes): it runs
	 * as the wand of lightning's zap does (StormStrikes.discharge) and each one it hits takes his
	 * `blow` as the wand's are shared out, 0.4 + 0.6 / how many it hit, all of it where it struck
	 * standing water; the one it struck is stunned a turn. Whom its crawl over the water runs into
	 * takes 1-3, as whoever stood in the water beside a bolt always did. `src` is the blow's
	 * source, what they must be immune to and what a hero it kills died of. What the bolt sets
	 * burning is the caller's. Where it ran, for the overlay to draw.
	 */
	public static StormStrikes.Discharge jolt(int cell, Object src, Blow blow) {
		Char struck = Actor.findChar(cell);
		StormStrikes.Discharge d = StormStrikes.discharge(Dungeon.level, cell, src.getClass());
		float multiplier = d.full ? 1f : 0.4f + 0.6f / d.hit.size();
		for (Char ch : d.hit) {
			ch.damage(Math.round(blow.on(ch) * multiplier), src);
			// Brief paralysis from shock
			if (ch == struck) Buff.prolong(ch, Paralysis.class, 1f);
			killedHero(ch, src);
		}
		//the water's splash: it flows round the hero's side (discharge), so no hero dies of it
		for (Char ch : d.wet) {
			ch.damage(Random.IntRange(1, 3), src);
		}
		return d;
	}

	//a bolt is no Hero.Doom: the run is failed and the death told here, as Electricity does
	private static void killedHero(Char ch, Object src) {
		if (ch == Dungeon.hero && !ch.isAlive()) {
			Dungeon.fail(src);
			GLog.n(Messages.get(src, "ondeath"));
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

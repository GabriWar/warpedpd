/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Warped Pixel Dungeon
 * Copyright (C) 2026 Gabriel Duarte Guerra (gabriwar)
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

/*
 * Warped Pixel Dungeon
 *
 * Global portal registry. Tracks per-depth fast-travel state so the player
 * can pick destinations without having that depth loaded.
 *
 * Source of truth for portal state; PortalGate NPCs sync from this on link.
 */

package xyz.gabriwar.warpedpixeldungeon;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.PortalGate;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Portals {

	public static class Record {
		public PortalGate.State state = PortalGate.State.LOCKED;
		public int cell = -1;
		public boolean discovered = false;
	}

	//records are keyed by depth + branch*BRANCH_STRIDE, so a branch shop (the spider
	//nest's floor 6) gets its own gate instead of sharing the main-dungeon one.
	//Branch 0 keys equal the depth, so older saves and net snapshots read unchanged.
	private static final int BRANCH_STRIDE = 1000;
	private static final HashMap<Integer, Record> records = new HashMap<>();

	public static int key(int depth, int branch) {
		return depth + branch * BRANCH_STRIDE;
	}

	public static int keyHere() {
		return key(Dungeon.depth, Dungeon.branch);
	}

	public static int depthOf(int key) {
		return key % BRANCH_STRIDE;
	}

	public static int branchOf(int key) {
		return key / BRANCH_STRIDE;
	}

	// Activation cost by shop-depth. Indices map: 6→1000, 11→1500, 16→2300, 20→3500.
	public static int costForDepth(int depth) {
		switch (depth) {
			case 6:  return 1000;
			case 11: return 1500;
			case 16: return 2300;
			case 20: return 3500;
			default: return 1500; // fallback for unexpected depths
		}
	}

	public static void reset() {
		records.clear();
	}

	// Called when a PortalGate is spawned during level gen.
	// Creates the record if missing; preserves state if revisiting.
	public static Record register(int depth, int cell) {
		Record r = records.get(depth);
		if (r == null) {
			r = new Record();
			records.put(depth, r);
		}
		r.cell = cell;
		unlockFirstShop(depth, r);
		return r;
	}

	//The main dungeon's first shop starts unlocked, including existing saves.
	private static void unlockFirstShop(int key, Record record) {
		if (key == key(6, 0) && record.state == PortalGate.State.LOCKED) {
			record.state = PortalGate.State.ACTIVE;
		}
	}

	public static Record get(int depth) {
		return records.get(depth);
	}

	// Read-only view for network serialization (host side).
	public static java.util.Set<Map.Entry<Integer, Record>> entries() {
		return records.entrySet();
	}

	// Client side: replace a depth's record wholesale from a host snapshot.
	public static void registerNet(int depth, PortalGate.State state, int cell, boolean discovered) {
		Record r = new Record();
		r.state = state;
		r.cell = cell;
		r.discovered = discovered;
		records.put(depth, r);
	}

	public static PortalGate.State getState(int depth) {
		Record r = records.get(depth);
		return r == null ? PortalGate.State.LOCKED : r.state;
	}

	public static void setState(int depth, PortalGate.State s) {
		Record r = records.get(depth);
		if (r == null) {
			r = new Record();
			records.put(depth, r);
		}
		// DEAD is terminal — never overwrite
		if (r.state == PortalGate.State.DEAD) return;
		r.state = s;
	}

	public static void markDead(int depth) {
		Record r = records.get(depth);
		if (r == null) {
			r = new Record();
			records.put(depth, r);
		}
		r.state = PortalGate.State.DEAD;
	}

	public static void discover(int depth) {
		Record r = records.get(depth);
		if (r == null) {
			r = new Record();
			records.put(depth, r);
		}
		boolean wasNew = !r.discovered;
		r.discovered = true;
		//journal notes only know branch 0 (see Notes.Record.depth)
		if (wasNew && branchOf(depth) == 0) {
			xyz.gabriwar.warpedpixeldungeon.journal.Notes.add(
					xyz.gabriwar.warpedpixeldungeon.journal.Notes.Landmark.PORTAL,
					depth);
		}
	}

	// Depths the player can fast-travel to right now.
	public static List<Integer> travelable(int excludeDepth) {
		ArrayList<Integer> out = new ArrayList<>();
		for (Map.Entry<Integer, Record> e : records.entrySet()) {
			Record r = e.getValue();
			if (e.getKey() == excludeDepth) continue;
			if (!r.discovered) continue;
			if (r.state != PortalGate.State.ACTIVE) continue;
			out.add(e.getKey());
		}
		java.util.Collections.sort(out);
		return out;
	}

	private static final String DEPTHS     = "portal_depths";
	private static final String STATES     = "portal_states";
	private static final String CELLS      = "portal_cells";
	private static final String DISCOVERED = "portal_discovered";

	public static void storeInBundle(Bundle bundle) {
		int n = records.size();
		int[] depths = new int[n];
		int[] states = new int[n];
		int[] cells  = new int[n];
		boolean[] discovered = new boolean[n];
		int i = 0;
		for (Map.Entry<Integer, Record> e : records.entrySet()) {
			depths[i] = e.getKey();
			states[i] = e.getValue().state.ordinal();
			cells[i]  = e.getValue().cell;
			discovered[i] = e.getValue().discovered;
			i++;
		}
		bundle.put(DEPTHS, depths);
		bundle.put(STATES, states);
		bundle.put(CELLS, cells);
		bundle.put(DISCOVERED, discovered);
	}

	public static void restoreFromBundle(Bundle bundle) {
		records.clear();
		if (!bundle.contains(DEPTHS)) return;
		int[] depths = bundle.getIntArray(DEPTHS);
		int[] states = bundle.getIntArray(STATES);
		int[] cells  = bundle.getIntArray(CELLS);
		boolean[] discovered = bundle.getBooleanArray(DISCOVERED);
		PortalGate.State[] vals = PortalGate.State.values();
		for (int i = 0; i < depths.length; i++) {
			Record r = new Record();
			r.state = vals[states[i]];
			r.cell  = cells[i];
			r.discovered = discovered[i];
			unlockFirstShop(depths[i], r);
			records.put(depths[i], r);
		}
	}
}

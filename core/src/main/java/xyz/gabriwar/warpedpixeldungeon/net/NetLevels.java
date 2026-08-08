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

package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.TownChurchLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownCinemaLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownFortuneLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInnLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownInteriorLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownLibraryLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.TownShopLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.VillageHouseLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;

import org.json.JSONObject;

/**
 * The client's level factory. The host ships a level's identity (see
 * StateSerializer.serializeLevelIdentity) alongside its terrain; here that turns
 * back into the real class, so a spectator gets the surface's art layers, the
 * town's fog rules and the real terrain names instead of a bare SpectatorLevel.
 *
 * Nothing here may run level generation: every path below builds terrain and art
 * only, never createMobs()/createItems()/Level.create(). Mobs, heaps, plants and
 * traps all arrive on the wire and are applied by SpectatorReceiver.
 */
public final class NetLevels {

	private NetLevels() {}

	/** Build a fresh mirror of the host's level. Never returns null. */
	public static Level create(JSONObject identity, int branch,
			int[] map, int w, int h, String tilesTex, String waterTex) {

		Level level = null;
		String cls = identity != null ? identity.optString("cls", "") : "";

		try {
			if ("OverworldLevel".equals(cls)) {
				level = OverworldLevel.forNetwork(
						identity.optLong("wseed", 0L),
						identity.optInt("wx", 0),
						identity.optInt("wy", 0),
						(float) identity.optDouble("shift", 0),
						season(identity),
						map, w, h);

			} else if (branch == TownInteriorLevel.BRANCH) {
				TownInteriorLevel town = townLevel(cls);
				// a room the host sized differently is refused outright: a level that
				// took no map has no collections either, and every later step of the
				// packet would fault on it
				if (town != null && town.applyNetworkMap(map, w, h)) {
					level = town;
				}

			} else if (branch == VillageHouseLevel.BRANCH
					&& "VillageHouseLevel".equals(cls)) {
				VillageHouseLevel house = new VillageHouseLevel();
				if (house.applyNetworkMap(identity.optInt("dwx", 0), identity.optInt("dwy", 0),
						map, w, h)) {
					level = house;
				}
			}
		} catch (Throwable t) {
			// A mirror we can't build is not worth dropping the session over —
			// fall back to the plain spectator level, which renders terrain.
			NetManager.log("[NET-CLI] mirror build failed for " + cls + ": " + t);
			level = null;
		}

		if (level == null) {
			SpectatorLevel spectator = new SpectatorLevel();
			spectator.setupFromNetwork(map, w, h, tilesTex, waterTex,
					identity != null && identity.optBoolean("noFog", false));
			level = spectator;
		}

		if (identity != null) {
			level.viewDistance = identity.optInt("viewDist", level.viewDistance);
		}
		return level;
	}

	/**
	 * The surface window slid or was re-derived in place: re-adopt it on the level
	 * we already have, so mobs, heaps and sprites keep their identity across the
	 * move. Must run on the render thread — the surface rebuilds its art layers
	 * straight into the live scene. Returns false if this level can't take it.
	 */
	/**
	 * Generate the new window for a surface mirror WITHOUT touching it: the
	 * expensive half of applyWindow, meant for the network read thread so the
	 * render thread only pays for the adoption. Null when this level cannot
	 * take a staged window (a spectator fallback, or a packet with no origin).
	 */
	public static OverworldLevel.Window stageWindow(Level level, JSONObject identity) {
		if (identity == null || !(level instanceof OverworldLevel)) return null;
		if (!identity.has("wseed")) return null;
		try {
			return ((OverworldLevel) level).stageNetworkWindow(
					identity.optLong("wseed", 0L),
					identity.optInt("wx", 0),
					identity.optInt("wy", 0),
					(float) identity.optDouble("shift", 0));
		} catch (Throwable t) {
			//a mirror that cannot stage still adopts, it just pays on the
			//render thread like it used to
			NetManager.log("[NET-CLI] window staging failed: " + t);
			return null;
		}
	}

	/** applyWindow with the generator pass already done (see stageWindow). */
	public static boolean applyWindow(Level level, JSONObject identity, int[] map,
			OverworldLevel.Window staged) {
		if (staged != null && level instanceof OverworldLevel && identity != null && map != null) {
			try {
				if (((OverworldLevel) level).adoptNetworkWindow(
						identity.optLong("wseed", 0L),
						identity.optInt("wx", 0),
						identity.optInt("wy", 0),
						(float) identity.optDouble("shift", 0),
						season(identity),
						staged, map)) {
					level.viewDistance = identity.optInt("viewDist", level.viewDistance);
					return true;
				}
			} catch (Throwable t) {
				NetManager.log("[NET-CLI] staged window adopt failed: " + t);
			}
			return false;
		}
		return applyWindow(level, identity, map);
	}

	public static boolean applyWindow(Level level, JSONObject identity, int[] map) {
		if (identity == null || map == null) return false;
		try {
			if (level instanceof OverworldLevel) {
				if (!((OverworldLevel) level).applyNetworkWindow(
						identity.optLong("wseed", 0L),
						identity.optInt("wx", 0),
						identity.optInt("wy", 0),
						(float) identity.optDouble("shift", 0),
						season(identity),
						map)) {
					return false;
				}
			} else if (level instanceof SpectatorLevel) {
				if (map.length != level.length()) return false;
				// setupFromNetwork blanks the exploration memory along with the flag
				// maps. Keep it: a re-derive in place (the season turned) moves no cell
				// index, so the host's visited/mapped/fov don't change either and are
				// never resent — a blanked client would sit under permanent black fog.
				boolean[] visited = level.visited, mapped = level.mapped, fov = level.heroFOV;
				((SpectatorLevel) level).setupFromNetwork(map, level.width(), level.height(),
						level.tilesTex(), level.waterTex(),
						identity.optBoolean("noFog", false));
				if (visited != null && visited.length == level.length()) level.visited = visited;
				if (mapped != null && mapped.length == level.length()) level.mapped = mapped;
				if (fov != null && fov.length == level.length()) level.heroFOV = fov;
			} else {
				return false;
			}
		} catch (Throwable t) {
			NetManager.log("[NET-CLI] window update failed: " + t);
			return false;
		}
		level.viewDistance = identity.optInt("viewDist", level.viewDistance);
		return true;
	}

	//the host's slice of the year, not ours: the client's calendar lags the level
	//packet by however long the weather block takes to reach the render thread
	private static GameCalendar.Season season(JSONObject identity) {
		GameCalendar.Season[] all = GameCalendar.Season.values();
		int ord = identity.optInt("season", -1);
		return (ord >= 0 && ord < all.length) ? all[ord] : GameCalendar.season();
	}

	//the six town buildings, keyed the same way Dungeon.newLevel() keys them
	private static TownInteriorLevel townLevel(String cls) {
		switch (cls) {
			case "TownChurchLevel":  return new TownChurchLevel();
			case "TownCinemaLevel":  return new TownCinemaLevel();
			case "TownLibraryLevel": return new TownLibraryLevel();
			case "TownShopLevel":    return new TownShopLevel();
			case "TownFortuneLevel": return new TownFortuneLevel();
			case "TownInnLevel":     return new TownInnLevel();
			default:                 return null;
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.homestead.farming;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Bunny;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Deer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.HuntPack;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.homestead.animals.Roping;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * The pests of the fields, while the hero is there: at night every EVERY turns each wild bunny or
 * deer within REACH cells of a crop it can walk to (not fenced in, Enclosure) sets off for the
 * nearest one (Grazing), walks to it and eats it - the crop is gone. A hero within SHY cells
 * scares it back to its own ways (a deer bolts); dawn, a fence closed round the crop or the crop
 * gone first ends its errand; until then it is spared the night's cull of the daytime beasts
 * (OverworldFauna.cull). A roped, tied or penned animal grazes nothing. What the pests eat while
 * the hero is away is FarmSettler's.
 */
public final class Pests {

	private Pests(){}

	/** How far a grazer goes for a crop, how often the night sends them, and how near a hero
	 *  scares one off it (a deer's own shyness). */
	public static final int REACH = 12, EVERY = 10, SHY = 5;

	/** FarmSlice.worldTurn: at night, now and then, the grazers near an open crop set off for it. */
	static void watch( OverworldLevel ow ){
		if (!DayNightCycle.isNight() || ow != Dungeon.level || Dungeon.hero == null) return;
		FarmSlice f = Farming.fields( ow );
		if (f == null || f.size() == 0) return;
		float now = Actor.now();
		if (now >= f.pestsSent && now - f.pestsSent < EVERY) return;
		f.pestsSent = now;
		//whether each crop is fenced, worked out once for every grazer that asks
		HashMap<Crop, Boolean> fenced = new HashMap<>();
		for (Mob m : ow.mobs.toArray( new Mob[0] )){
			if (!grazer( m ) || m.buff( Grazing.class ) != null) continue;
			Crop c = nearest( ow, m, fenced );
			if (c == null) continue;
			Grazing g = Buff.affect( m, Grazing.class );
			g.wx = c.wx;
			g.wy = c.wy;
		}
	}

	/** Is it a wild grazer that eats crops: a bunny or a deer of the wild, alive, nobody's. */
	public static boolean grazer( Mob m ){
		return (m instanceof Bunny || m instanceof Deer) && m.isAlive() && m.alignment == Char.Alignment.NEUTRAL
				&& !Roping.held( m ) && m.buff( HuntPack.class ) == null;
	}

	//the nearest open crop within REACH a grazer could walk to, or null. `fenced` holds the crops'
	//Enclosure answers already worked out this watch (filled in as it goes); one map of the ways
	//from the grazer answers whether it could walk to each
	static Crop nearest( OverworldLevel ow, Mob m, HashMap<Crop, Boolean> fenced ){
		FarmSlice f = Farming.fields( ow );
		int w = ow.width(), mx = ow.worldX + m.pos % w, my = ow.worldY + m.pos / w;
		ArrayList<Crop> near = f.in( mx - REACH, my - REACH, mx + REACH + 1, my + REACH + 1 );
		if (near.isEmpty()) return null;
		Crop best = null;
		int bestD = Integer.MAX_VALUE;
		boolean mapped = false;
		for (Crop c : near){
			int cell = ow.localCell( c.wx, c.wy );
			if (cell == -1) continue;
			int d = ow.distance( m.pos, cell );
			if (d > REACH || d >= bestD) continue;
			Boolean shut = fenced.get( c );
			if (shut == null){
				shut = Farming.fenced( ow, cell );
				fenced.put( c, shut );
			}
			if (shut) continue;
			if (!mapped){
				PathFinder.buildDistanceMap( m.pos, ow.passable, REACH * 2 );
				mapped = true;
			}
			if (PathFinder.distance[cell] == Integer.MAX_VALUE) continue;
			best = c;
			bestD = d;
		}
		return best;
	}

	/**
	 * A grazer's turn (Animals.act): on its way to its crop, a step closer; beside it, it eats it.
	 * The time it spent, or 0 when it has no errand (or a hero is too near): its own ways then.
	 */
	public static float act( Mob m ){
		Grazing g = m.buff( Grazing.class );
		if (g == null) return 0f;
		Level level = Dungeon.level;
		if (!(level instanceof OverworldLevel) || !DayNightCycle.isNight() || !grazer( m )){
			g.detach();
			return 0f;
		}
		OverworldLevel ow = (OverworldLevel) level;
		int cell = ow.localCell( g.wx, g.wy );
		Crop c = cell == -1 ? null : Farming.cropAt( ow, cell );
		if (c == null || Farming.fenced( ow, cell )){
			g.detach();
			return 0f;
		}
		for (Hero hero : OverworldLevel.heroesOn( ow )){
			if (hero.invisible <= 0 && ow.distance( m.pos, hero.pos ) <= SHY) return 0f;
		}
		if (ow.adjacent( m.pos, cell )){
			if (m.sprite != null) m.sprite.turnTo( m.pos, cell );
			eat( ow, cell, c, m );
			g.detach();
			return 1f / m.speed();
		}
		int step = Roping.stepToward( m, cell );
		if (step == -1){
			g.detach();
			return 0f;
		}
		Roping.moveTo( m, step );
		return 1f / m.speed();
	}

	//the crop is eaten: gone, the hero told when he sees it
	static void eat( OverworldLevel ow, int cell, Crop c, Mob eater ){
		if (Farming.live( ow ) && ow.heroFOV[cell]){
			GLog.w( Messages.get( Pests.class, "eaten", eater.name(), Farming.name( c ) ) );
		}
		Farming.dies( ow, cell, c, Farming.Outcome.EATEN, true );
	}
}

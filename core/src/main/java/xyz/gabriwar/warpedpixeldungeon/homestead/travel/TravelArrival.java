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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A trip by the stones, on the hero until he has landed (Travel.depart puts it on him): the stone he
 * goes to, and the animals on a rope he took along. Once he stands on the stone's slice beside it
 * - before his first turn there: it acts ahead of him - the rune flashes over him and the stone, the
 * animals are put down round him, and it is gone. Saved with him, so a save on the way loses
 * nothing. Should the trip never land (the scene never changed), the animals are put down beside
 * him after WAIT turns wherever he is, and it ends there; one with no room round him yet stays
 * carried until there is. Shows nothing on the buff bar.
 */
public class TravelArrival extends Buff {

	{
		//ahead of the hero's own first turn on arrival
		actPriority = HERO_PRIO + 1;
	}

	/** How many turns it waits for the hero to stand by the stone before it gives up. */
	static final int WAIT = 5;

	private int depth, wx, wy;
	private int waited;
	//the rune has flashed for this landing (not saved: a load does not flash it again)
	private boolean flashed;
	private final ArrayList<Mob> carried = new ArrayList<>();

	void set( Travel.Place to, ArrayList<Mob> animals ){
		depth = to.depth;
		wx = to.wx;
		wy = to.wy;
		waited = 0;
		carried.addAll( animals );
	}

	/** The animals it carries (the tests look). */
	ArrayList<Mob> carried(){
		return carried;
	}

	@Override
	public boolean act(){
		if (!landed() && ++waited < WAIT){
			spend( TICK );
			return true;
		}
		Level level = Dungeon.level;
		if (!flashed && landed()){
			flashed = true;
			Travel.runeFlash( target.pos );
			int stone = ((OverworldLevel) level).localCell( wx, wy );
			if (stone != -1) Travel.runeFlash( stone );
		}
		for (Mob m : carried.toArray( new Mob[0] )){
			int cell = freeBeside( level, target.pos );
			if (cell == -1) continue;
			m.pos = cell;
			if (Game.scene() instanceof GameScene) GameScene.add( m );
			else level.mobs.add( m );
			carried.remove( m );
		}
		//no room round him for one yet: it stays carried, and is tried again next turn
		if (!carried.isEmpty()){
			spend( TICK );
			return true;
		}
		detach();
		return true;
	}

	//on the stone's slice, beside the stone
	private boolean landed(){
		if (!(Dungeon.level instanceof OverworldLevel) || Dungeon.depth != depth || target == null) return false;
		int stone = ((OverworldLevel) Dungeon.level).localCell( wx, wy );
		return stone != -1 && Dungeon.level.distance( stone, target.pos ) <= 4;
	}

	//an open cell round a cell, nearest first (ring by ring, out to REACH), inside the map, nobody on it
	private static int freeBeside( Level level, int cell ){
		int w = level.width(), cx = cell % w, cy = cell / w;
		for (int r = 1; r <= REACH; r++){
			for (int dy = -r; dy <= r; dy++){
				for (int dx = -r; dx <= r; dx++){
					if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != r) continue;
					int x = cx + dx, y = cy + dy;
					if (x <= 0 || y <= 0 || x >= w - 1 || y >= level.height() - 1) continue;
					int c = x + y * w;
					if (!level.passable[c] || level.pit[c] || Actor.findChar( c ) != null) continue;
					boolean taken = false;
					for (Mob m : level.mobs) if (m.pos == c){
						taken = true;
						break;
					}
					if (!taken) return c;
				}
			}
		}
		return -1;
	}

	//how far round the hero an animal brought along may be put down
	private static final int REACH = 3;

	private static final String DEPTH = "depth", WX = "wx", WY = "wy", WAITED = "waited", CARRIED = "carried";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( DEPTH, depth );
		bundle.put( WX, wx );
		bundle.put( WY, wy );
		bundle.put( WAITED, waited );
		bundle.put( CARRIED, carried );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		depth = bundle.getInt( DEPTH );
		wx = bundle.getInt( WX );
		wy = bundle.getInt( WY );
		waited = bundle.getInt( WAITED );
		carried.clear();
		if (bundle.contains( CARRIED )){
			for (Bundlable b : bundle.getCollection( CARRIED )) if (b instanceof Mob) carried.add( (Mob) b );
		}
	}
}

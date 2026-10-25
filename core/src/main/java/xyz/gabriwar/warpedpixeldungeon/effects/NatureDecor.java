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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Wildfire;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ChimneySmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

/**
 * A wildfire's smoke seen from afar (levels/overworld/WildfireEvent): thick black columns rising over
 * the fog from where its flames burn, so a fire shows on the window long before its flames are in
 * sight - one column for each SPAN x SPAN patch of the world alight, the MAX patches with the most
 * flames, each where its flames gather. Worked out every PERIOD seconds from the flames' blob
 * (actors/blobs/Wildfire), which a co-op guest's mirror holds as its host ships it, so every screen
 * shows the same columns; a column whose patch has burnt out stops pouring and its last puffs rise
 * away. The host's window slide carries the columns with the fog's group; a guest's mirror,
 * re-labelled without a slide, moves them by hand. Render thread, among the surface's visuals.
 */
public final class NatureDecor extends Gizmo {

	/** World cells a side of the patches the columns stand for, and the most columns at once. */
	public static final int SPAN = 6, MAX = 6;
	/** How often the columns are worked out again, in seconds. */
	public static final float PERIOD = 1f;
	/** A puff of a column every this many seconds. */
	public static final float PUFF = 0.16f;

	/** A wildfire's column: the chimneys', of a kind of its own (a scene hands a dead column back only
	 *  to its own class). */
	public static final class FireSmoke extends ChimneySmokeParticle.Column {}

	private final OverworldLevel level;
	private final HashMap<Long, FireSmoke> columns = new HashMap<>();
	private int sceneId, visX, visY;
	private float wait;

	public NatureDecor( OverworldLevel level ){
		this.level = level;
		visX = level.worldX;
		visY = level.worldY;
	}

	@Override
	public void update(){
		super.update();
		if (Dungeon.level != level) return;
		Object scene = Game.scene();
		if (!(scene instanceof GameScene)) return;
		int id = System.identityHashCode( scene );
		if (id != sceneId){
			//the old scene took its columns with it
			columns.clear();
			sceneId = id;
			wait = 0;
		} else if (level.isMirror() && (level.worldX != visX || level.worldY != visY)){
			float dx = (visX - level.worldX) * DungeonTilemap.SIZE, dy = (visY - level.worldY) * DungeonTilemap.SIZE;
			for (FireSmoke c : columns.values()) c.slide( dx, dy );
		}
		visX = level.worldX;
		visY = level.worldY;
		wait -= Game.elapsed;
		if (wait > 0) return;
		wait = PERIOD;
		lay();
	}

	//the patches alight and their columns: new ones lit, burnt-out ones let die
	private void lay(){
		HashMap<Long, int[]> patches = new HashMap<>();
		Blob b = level.blobs.get( Wildfire.class );
		int[] cur = b != null && b.volume > 0 ? b.cur : null;
		if (cur != null){
			int w = level.width(), ox = visX, oy = visY;
			for (int c = 0; c < cur.length; c++){
				if (cur[c] <= 0) continue;
				int x = c % w, y = c / w;
				long key = ((long) Math.floorDiv( oy + y, SPAN ) << 32) | (Math.floorDiv( ox + x, SPAN ) & 0xFFFFFFFFL);
				int[] p = patches.get( key );
				if (p == null) patches.put( key, p = new int[3] );
				p[0]++;
				p[1] += x;
				p[2] += y;
			}
		}
		ArrayList<Map.Entry<Long, int[]>> busiest = new ArrayList<>( patches.entrySet() );
		busiest.sort( ( a, c ) -> a.getValue()[0] != c.getValue()[0] ? c.getValue()[0] - a.getValue()[0]
				: Long.compare( a.getKey(), c.getKey() ) );
		HashSet<Long> want = new HashSet<>();
		for (int i = 0; i < Math.min( MAX, busiest.size() ); i++){
			long key = busiest.get( i ).getKey();
			want.add( key );
			if (columns.containsKey( key )) continue;
			int[] p = busiest.get( i ).getValue();
			float px = p[1] / (float) p[0] * DungeonTilemap.SIZE + 4, py = p[2] / (float) p[0] * DungeonTilemap.SIZE + 2;
			FireSmoke col = ChimneySmokeParticle.soot( FireSmoke.class, px, py, 8, 6, PUFF );
			if (col != null) columns.put( key, col );
		}
		for (Iterator<Map.Entry<Long, FireSmoke>> it = columns.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, FireSmoke> e = it.next();
			if (want.contains( e.getKey() )) continue;
			e.getValue().on = false;
			it.remove();
		}
	}
}

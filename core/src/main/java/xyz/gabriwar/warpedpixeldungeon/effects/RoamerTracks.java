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

import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * The tracks a roaming beast leaves (levels/overworld/RoamerEvent.trail): its prints pressed into
 * the open ground along the way it went over the last half day, the freshest darkest, the oldest
 * near gone. Pictures on the floor, under everyone and under the fog: what a hero has not seen of
 * the ground he does not see of them either. The level works out on the actor thread what should
 * be shown and posts it here; this brings the scene in line on the render thread, a print made
 * once and only faded after (the floor's group is slid with a rebase). A co-op guest's mirror
 * keeps one of its own and slides it by hand (slide). tools/assets/events_life_art.py draws them.
 */
public final class RoamerTracks {

	/** effects/roamer_tracks.png, 192x16: twelve 16x16 prints, four to a kind (great wolf, elder
	 *  yeti, giant scorpion), going east, south, west and north. */
	public static final String TEXTURE = "effects/roamer_tracks.png";

	//how plain a print is by its age in tenths: fresh to nearly gone
	private static final float FRESH = 0.85f, OLD = 0.3f;

	/** A print as posted: the world cell it is on (what it is known by, the same whatever the
	 *  window), that cell's top-left in scene pixels, its frame, and its age in tenths. */
	public static final class Print {
		final long key;
		final float x, y;
		final int frame, age;
		public Print( long key, float x, float y, int frame, int age ){
			this.key = key;
			this.x = x;
			this.y = y;
			this.frame = frame;
			this.age = age;
		}
	}

	/** Do two posts show the same prints at the same ages (where aside: a slide moves them)? */
	public static boolean same( List<Print> a, List<Print> b ){
		if (a.size() != b.size()) return false;
		for (int i = 0; i < a.size(); i++){
			Print x = a.get( i ), y = b.get( i );
			if (x.key != y.key || x.frame != y.frame || x.age != y.age) return false;
		}
		return true;
	}

	private int sceneId;
	private final HashMap<Long, Image> shown = new HashMap<>();

	/** Render thread: what is on the scene (of identity sceneId) is brought in line with a post. */
	public void show( int sceneId, List<Print> list ){
		if (!(Game.scene() instanceof GameScene) || System.identityHashCode( Game.scene() ) != sceneId) return;
		if (this.sceneId != sceneId){
			//the old scene took everything it showed with it
			shown.clear();
			this.sceneId = sceneId;
		}
		HashSet<Long> want = new HashSet<>();
		for (Print p : list){
			if (!want.add( p.key )) continue;
			Image img = shown.get( p.key );
			if (img == null){
				img = new Image( TEXTURE, 16 * p.frame, 0, 16, 16 );
				img.x = p.x;
				img.y = p.y;
				GameScene.floorEffect( img );
				shown.put( p.key, img );
			}
			img.alpha( FRESH - (FRESH - OLD) * p.age / 9f );
		}
		for (Iterator<Map.Entry<Long, Image>> it = shown.entrySet().iterator(); it.hasNext(); ){
			Map.Entry<Long, Image> e = it.next();
			if (want.contains( e.getKey() )) continue;
			Image img = e.getValue();
			if (img.parent != null){
				img.killAndErase();
				img.destroy();
			}
			it.remove();
		}
	}

	/** Render thread: every print moved by (dx, dy) scene pixels (a co-op guest's mirror). */
	public void slide( float dx, float dy ){
		for (Image img : shown.values()){
			img.x += dx;
			img.y += dy;
		}
	}
}

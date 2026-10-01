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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Game;
import com.watabou.noosa.Gizmo;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;

import java.util.ArrayList;

/**
 * A purely visual timeline: runs small callbacks (flashes, sounds, particles) at offsets from now.
 * The game has already resolved everything before the timeline starts; it only staggers what is
 * seen, exactly like Blink's arcs. It dies quietly on a floor change or when the hero's sprite
 * is gone, without running what is left.
 */
public class FxTimeline extends Gizmo {

	private static final class Step {
		final float at;
		final Runnable run;
		Step( float at, Runnable run ){ this.at = at; this.run = run; }
	}

	private final ArrayList<Step> steps = new ArrayList<>();
	private final int depth = Dungeon.depth, branch = Dungeon.branch;
	private float time = 0;
	private int next = 0;
	private final boolean live;

	private FxTimeline( boolean live ){
		this.live = live;
	}

	/** a timeline on the hero's layer; when there is nothing to draw on, the steps are simply dropped */
	public static FxTimeline start(){
		boolean ok = Dungeon.hero != null && Dungeon.hero.sprite != null && Dungeon.hero.sprite.parent != null;
		FxTimeline t = new FxTimeline( ok );
		if (ok) Dungeon.hero.sprite.parent.add( t );
		return t;
	}

	/** schedules run at seconds from the start; steps must be added in non-decreasing order */
	public FxTimeline at( float seconds, Runnable run ){
		if (live) steps.add( new Step( seconds, run ) );
		return this;
	}

	@Override
	public void update(){
		if (depth != Dungeon.depth || branch != Dungeon.branch || Dungeon.hero == null || Dungeon.hero.sprite == null
				|| Dungeon.hero.sprite.parent == null){
			killAndErase();
			return;
		}
		time += Game.elapsed;
		while (next < steps.size() && steps.get( next ).at <= time){
			steps.get( next++ ).run.run();
		}
		if (next >= steps.size()) killAndErase();
	}
}

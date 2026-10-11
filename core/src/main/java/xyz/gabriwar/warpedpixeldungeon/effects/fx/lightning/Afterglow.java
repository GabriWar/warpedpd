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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.lightning;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

/**
 * What a major lightning leaves on the eye once it is gone (its callback's frame past): its cores
 * alone, where they last lay, tinted COLOR, fading from PEAK to nothing over LIFE s, drawn as light
 * on the weather's layer, then let go of (Fx.dispose). At most MOST at once (a fourth major goes
 * without), each of at most LINES lines.
 */
public class Afterglow extends Group {

	public static final float LIFE = 0.12f, PEAK = 0.25f;
	public static final int COLOR = 0x6A80FF, MOST = 3, LINES = 48;

	private static int alive;

	private float age;
	private int lines;
	private boolean counted = true;

	private Afterglow(){
	}

	/** How many glow now. */
	public static synchronized int alive(){
		return alive;
	}

	/** A new scene: none of the last one's counted. */
	public static synchronized void reset(){
		alive = 0;
	}

	private static synchronized boolean claim(){
		if (alive >= MOST) return false;
		alive++;
		return true;
	}

	private static synchronized void release(){
		if (alive > 0) alive--;
	}

	/** A new afterglow in a group (the weather's layer); null with MOST already glowing or no
	 *  group. Its lines are added after (line). Render thread. */
	public static Afterglow begin( Group into ){
		if (into == null || !claim()) return null;
		Afterglow a = new Afterglow();
		into.add( a );
		return a;
	}

	/** A copy of a core line where it lies now (its frame, place, turn and stretch); false once it
	 *  holds LINES. */
	public boolean line( Image core ){
		if (lines >= LINES) return false;
		Image l = new Image( core );
		l.origin.set( core.origin.x, core.origin.y );
		l.scale.set( core.scale.x, core.scale.y );
		l.angle = core.angle;
		l.x = core.x;
		l.y = core.y;
		l.resetColor();
		l.hardlight( COLOR );
		Lines.shade( l, PEAK );
		add( l );
		lines++;
		return true;
	}

	public int lines(){
		return lines;
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		age += Game.elapsed;
		if (age >= LIFE){
			gone();
			return;
		}
		float a = PEAK * (1f - age / LIFE);
		for (int i = 0; i < length; i++){
			if (members.get( i ) instanceof Image) Lines.shade( (Image) members.get( i ), a );
		}
	}

	//off the layer, its lines' buffers freed
	private void gone(){
		if (counted){
			counted = false;
			release();
		}
		Fx.dispose( this );
	}

	@Override
	public void destroy(){
		if (counted){
			counted = false;
			release();
		}
		super.destroy();
	}

	@Override
	public void draw(){
		Blending.setLightMode();
		super.draw();
		Blending.setNormalMode();
	}
}

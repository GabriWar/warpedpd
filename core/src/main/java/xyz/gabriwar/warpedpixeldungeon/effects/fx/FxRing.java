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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * A ring spreading on the ground or the water: the painted 2:1 ring frames (FxFrames.RINGS)
 * stepped through from one width to another as it eases out, never a scaled image; its alpha
 * falling as (1 - p)^1.2. On the ground its near (lower) arc is the bright one; on water it is
 * mirrored (mirrorY) so the far crest catches the sky. Light (added in its colour) or matter (lit
 * in it). Placed about its middle on whole pixels; a caller may still nudge its y after making it
 * (the sewers' pipes do) and it keeps the nudge.
 *
 * At most FxBudget.ringCap() of them live at once, the water's and the ground's together: past it
 * the oldest goes to make room.
 */
public class FxRing extends Image {

	private static final FxRing[] live = new FxRing[96];
	private static int count;
	private static long serials;

	//the ring frames it steps through, its life, its peak alpha, and how long before it shows
	protected int first, frames;
	protected float life, age, peak, delay;
	protected boolean additive;
	//its middle, and where it was last put (to keep a caller's nudge)
	protected float cx, cy;
	private float placedX = Float.NaN, placedY = Float.NaN;
	private int shownIndex = -1;
	private FxRing leader;
	private long serial;
	private final RectF uv = new RectF();

	public FxRing(){
		super( Assets.Effects.FX_SHEET );
	}

	/** A new scene: none of the last one's left. */
	public static synchronized void reset(){
		for (int i = 0; i < count; i++) live[i] = null;
		count = 0;
	}

	/** How many are spreading. */
	public static synchronized int live(){
		return count;
	}

	/**
	 * A ring on the ground in `group` (the floor's layer, usually): spreading at (x, y) from the
	 * ring `fromW` px wide to the one `toW` wide over `life` s, in `color` at `alpha`, as light or
	 * as matter. Null when its group is gone.
	 */
	public static FxRing ground( Group group, float x, float y, int fromW, int toW, float life, int color, float alpha, boolean light ){
		if (group == null) return null;
		FxRing r = (FxRing) group.recycle( FxRing.class );
		r.setup( x, y, fromW, toW, life, color, alpha, light );
		return r;
	}

	/** Mirrored top to bottom: a ring on water, its far crest the bright one. */
	public FxRing mirrorY(){
		flipVertical = true;
		shownIndex = -1;
		show();
		return this;
	}

	/** Shows only after `seconds` (a second ring after the first). */
	public FxRing delay( float seconds ){
		delay = seconds;
		visible = false;
		return this;
	}

	/** Keeps to another ring's middle while it lives (a second ring after the first, which a
	 *  caller may have nudged). */
	public FxRing follow( FxRing leader ){
		this.leader = leader;
		return this;
	}

	public float centerX(){
		return cx;
	}

	public float centerY(){
		return cy;
	}

	/** How far through its life, 0..1 (0 before it shows). */
	public float progress(){
		return age <= delay ? 0f : Math.min( 1f, (age - delay) / life );
	}

	protected void setup( float x, float y, int fromW, int toW, float life, int color, float alpha, boolean light ){
		first = FxFrames.ringIndex( Math.min( fromW, toW ) );
		frames = FxFrames.ringIndex( Math.max( fromW, toW ) ) - first + 1;
		this.life = Math.max( 0.01f, life );
		age = 0;
		delay = 0;
		peak = alpha;
		additive = light;
		cx = x;
		cy = y;
		placedX = placedY = Float.NaN;
		leader = null;
		flipVertical = false;
		flipHorizontal = false;
		angle = 0;
		scale.set( 1 );
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		am = alpha;
		shownIndex = -1;
		show();
		visible = true;
		revive();
		enter( this );
	}

	private void show(){
		float p = progress();
		int i = first + Math.min( frames - 1, (int)(frames * FxCurves.easeOut( p )) );
		if (i != shownIndex){
			shownIndex = i;
			int[] f = FxFrames.RINGS[i];
			float tw = texture.width, th = texture.height;
			uv.set( f[0] / tw, f[1] / th, (f[0] + f[2]) / tw, (f[1] + f[3]) / th );
			frame( uv );
		}
		place();
	}

	//about its middle on whole pixels, after taking in any nudge a caller gave it since
	private void place(){
		if (!Float.isNaN( placedX )){
			cx += x - placedX;
			cy += y - placedY;
		}
		x = placedX = Math.round( cx - width / 2f );
		y = placedY = Math.round( cy - height / 2f );
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		age += Game.elapsed;
		if (leader != null){
			if (leader.alive){
				//its middle the leader's, the leader's nudges and all
				cx = leader.cx;
				cy = leader.cy;
				placedX = placedY = Float.NaN;
			} else {
				leader = null;
			}
		}
		if (age < delay){
			visible = false;
			return;
		}
		float p = progress();
		if (p >= 1f){
			kill();
			return;
		}
		show();
		float a = peak * (float)Math.pow( 1f - p, 1.2f );
		am = a;
		visible = a > 0.004f;
	}

	@Override
	public void draw(){
		if (additive){
			Blending.setLightMode();
			super.draw();
			Blending.setNormalMode();
		} else {
			super.draw();
		}
	}

	@Override
	public void kill(){
		super.kill();
		leave( this );
	}

	//counted in; past the cap the oldest goes
	private static synchronized void enter( FxRing r ){
		r.serial = ++serials;
		for (int i = 0; i < count; i++) if (live[i] == r) return;
		if (count >= FxBudget.ringCap() || count >= live.length){
			FxRing oldest = null;
			for (int i = 0; i < count; i++){
				if (oldest == null || live[i].serial < oldest.serial) oldest = live[i];
			}
			if (oldest != null) oldest.kill();
		}
		if (count < live.length){
			live[count++] = r;
			FxBudget.add( FxBudget.RINGS, 1 );
		}
	}

	private static synchronized void leave( FxRing r ){
		for (int i = 0; i < count; i++){
			if (live[i] == r){
				live[i] = live[--count];
				live[count] = null;
				FxBudget.add( FxBudget.RINGS, -1 );
				return;
			}
		}
	}

	/** The oldest ring spreading, null for none: what a ring that may not be dropped makes way by. */
	static synchronized FxRing oldest(){
		FxRing oldest = null;
		for (int i = 0; i < count; i++){
			if (oldest == null || live[i].serial < oldest.serial) oldest = live[i];
		}
		return oldest;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.gas;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

/**
 * One picture lying on a cell for as long as what makes it is there: a web strung across it, rime
 * on a frozen floor, a thick gas's tint, the etching a corrosive one leaves, a pit's dark mouth,
 * the cracks of water freezing. Whatever keeps it (a blob's emitter, once a tick) calls keep() for
 * its cell again and again; once that stops - the web burnt, the frost thawed, the cell out of
 * sight - it holds `hold` s more and fades over `fade`. One per kind and cell, at most CAPS[kind]
 * of a kind at once (a new one past it is not made).
 *
 * Matter lies on the floor under everyone, drawn normally; the cracks of freezing water (CRACK)
 * are light, added in the light layer. World-anchored, on whole pixels, slid with the window (its
 * cell with it, shiftAll). A
 * web shakes (WEB_B for JIGGLE s, and a couple of silk motes) when someone moves through it, and
 * catches a dew glint every 6-10 s.
 */
public class CellOverlay extends Image {

	public static final int WEB = 0, RIME = 1, TINT = 2, ETCH = 3, PIT = 4, CRACK = 5;
	/** Which way one grows into place (revealCrop), as FxDecal's. */
	public static final int FROM_LEFT = 0, FROM_RIGHT = 1, FROM_BOTTOM = 2, FROM_TOP = 3, FROM_MIDDLE = 4;
	/** The most of each kind at once. */
	public static final int[] CAPS = { 64, 48, 24, 16, 24, 24 };
	/** A web's shake when walked through, s; the dew glint's wait, s. */
	public static final float JIGGLE = 0.3f, DEW_MIN = 6f, DEW_MAX = 10f;
	/** Silk's colour. */
	public static final int SILK = 0xE6E2EE;

	private static final CellOverlay[] live = new CellOverlay[200];
	private static int count;
	private static final int[] kinds = new int[CAPS.length];
	private static volatile int webs;

	private int kind, cell;
	private int[] frame, alt, shownFrame;
	private float altLeft;
	private boolean lightSheet, additive;
	private float peak, in, hold, fade, age, since;
	private int reveal = -1;
	private float revealFor;
	private float glintIn;
	private float cx, cy, placedX, placedY;
	private final RectF uv = new RectF();

	public CellOverlay(){
		super( Assets.Effects.FX_SHEET );
	}

	/** A new scene: none of the last one's left. */
	public static synchronized void reset(){
		for (int i = 0; i < count; i++) live[i] = null;
		count = 0;
		for (int i = 0; i < kinds.length; i++) kinds[i] = 0;
		webs = 0;
	}

	/** How many of a kind are lying about. */
	public static synchronized int live( int kind ){
		return kinds[kind];
	}

	/** Whether any web is strung (any thread). */
	public static boolean anyWeb(){
		return webs > 0;
	}

	/** The one of a kind on a cell, or null. */
	public static synchronized CellOverlay on( int kind, int cell ){
		for (int i = 0; i < count; i++){
			CellOverlay o = live[i];
			if (o.kind == kind && o.cell == cell) return o;
		}
		return null;
	}

	/**
	 * Keeps a picture of a kind on a cell: the one there held on (its hold starting over), or a new
	 * one with its middle at (x, y) - a frame of the sprite sheet (or of the light sheet) lit in
	 * `color` up to `alpha` over `in` s, held `hold` s past its last keep, faded over `fade` s,
	 * mirrored or not. Null when there is no room for one of its kind or no scene. Render thread.
	 */
	public static CellOverlay keep( int kind, int cell, float x, float y, int[] frame, boolean lightSheet, int color,
	                                float alpha, float in, float hold, float fade, boolean mirror ){
		CellOverlay o = on( kind, cell );
		if (o != null){
			o.since = 0;
			//kept again more strongly (a pit's warning, then its fall): it deepens to that
			if (alpha > o.peak) o.peak = alpha;
			return o;
		}
		synchronized (CellOverlay.class){
			if (kinds[kind] >= CAPS[kind] || count >= live.length) return null;
		}
		o = GameScene.recycle( kind == CRACK ? GameScene.Layer.LIGHTS : GameScene.Layer.FLOOR, CellOverlay.class );
		if (o == null) return null;
		o.setup( kind, cell, x, y, frame, lightSheet, color, alpha, in, hold, fade, mirror );
		enter( o );
		return o;
	}

	/**
	 * The overworld's window slid by (dx, dy) cells (a cell's index goes from x + y w to
	 * (x - dx) + (y - dy) w): each picture follows its cell (its image is slid with its layer), one
	 * left off the window keeps no cell and fades. Render thread, in the slide's atomic block.
	 */
	public static synchronized void shiftAll( int dx, int dy, int width, int height ){
		for (int i = 0; i < count; i++){
			CellOverlay o = live[i];
			if (o.cell < 0) continue;
			int x = o.cell % width - dx, y = o.cell / width - dy;
			if (x < 0 || y < 0 || x >= width || y >= height){
				o.cell = -1;
				if (o.since < o.hold) o.since = o.hold;
			} else {
				o.cell = x + y * width;
			}
		}
	}

	/** Fades the one of a kind on a cell now (a web burnt away, a frost thawed). */
	public static void release( int kind, int cell ){
		CellOverlay o = on( kind, cell );
		if (o != null && o.since < o.hold) o.since = o.hold;
	}

	/** Someone moved through the web on a cell: it shakes and sheds silk. Render thread. */
	public static void jiggle( int cell ){
		CellOverlay o = on( WEB, cell );
		if (o == null || o.altLeft > 0) return;
		o.altLeft = JIGGLE;
		FxEmitter air = Fx.air();
		if (air == null) return;
		for (int i = 0; i < 2; i++){
			FxFactories.mote( SILK, false ).emit( air, i, o.cx + FxRandom.Float( -5f, 5f ), o.cy + FxRandom.Float( -5f, 3f ) );
		}
	}

	/** Its shaken frame (a web's WEB_B), shown while it shakes. */
	public CellOverlay alt( int[] frame ){
		alt = frame;
		return this;
	}

	/** It grows into place from one side over `seconds` (FxDecal's FROM_ sides). */
	public CellOverlay revealCrop( int from, float seconds ){
		reveal = from;
		revealFor = Math.max( 0.01f, seconds );
		crop( 0f, frame );
		return this;
	}

	/** Whether it was made this frame (not one held on). */
	public boolean isNew(){
		return age == 0;
	}

	/** Its cell. */
	public int cell(){
		return cell;
	}

	/** How much of it shows now, 0..its alpha. */
	public float shown(){
		return am;
	}

	private void setup( int kind, int cell, float x, float y, int[] frame, boolean lightSheet, int color, float alpha,
	                    float in, float hold, float fade, boolean mirror ){
		this.kind = kind;
		this.cell = cell;
		this.frame = frame;
		alt = null;
		altLeft = 0;
		if (lightSheet != this.lightSheet || texture == null){
			this.lightSheet = lightSheet;
			texture( lightSheet ? Assets.Effects.FX_LIGHT : Assets.Effects.FX_SHEET );
		}
		additive = kind == CRACK;
		peak = alpha;
		this.in = in;
		this.hold = hold;
		this.fade = Math.max( 0.01f, fade );
		age = since = 0;
		reveal = -1;
		glintIn = FxRandom.Float( DEW_MIN, DEW_MAX );
		cx = x;
		cy = y;
		flipHorizontal = mirror;
		flipVertical = false;
		angle = 0;
		scale.set( 1 );
		resetColor();
		hardlight( ((color >> 16) & 0xFF) / 255f, ((color >> 8) & 0xFF) / 255f, (color & 0xFF) / 255f );
		am = in > 0 ? 0 : alpha;
		crop( 1f, frame );
		visible = true;
		revive();
	}

	//shows the share p of a frame, grown from its reveal side, placed on whole pixels about its middle
	private void crop( float p, int[] f ){
		shownFrame = f;
		int fx = f[0], fy = f[1], fw = f[2], fh = f[3];
		int l = fx, t = fy, r = fx + fw, b = fy + fh;
		if (reveal >= 0 && p < 1f){
			int w = Math.max( 1, Math.round( fw * p ) ), h = Math.max( 1, Math.round( fh * p ) );
			switch (reveal){
				case FROM_LEFT:   r = fx + w; break;
				case FROM_RIGHT:  l = fx + fw - w; break;
				case FROM_BOTTOM: t = fy + fh - h; break;
				case FROM_TOP:    b = fy + h; break;
				default:
					l = fx + (fw - w) / 2; r = l + w;
					t = fy + (fh - h) / 2; b = t + h;
			}
		}
		float tw = texture.width, th = texture.height;
		uv.set( l / tw, t / th, r / tw, b / th );
		frame( uv );
		x = Math.round( cx - fw / 2f ) + (flipHorizontal ? (fx + fw - r) : (l - fx));
		y = Math.round( cy - fh / 2f ) + (t - fy);
		placedX = x;
		placedY = y;
	}

	private static synchronized void enter( CellOverlay o ){
		live[count++] = o;
		kinds[o.kind]++;
		if (o.kind == WEB) webs++;
		FxBudget.add( FxBudget.DECALS, 1 );
	}

	private static synchronized void leave( CellOverlay o ){
		for (int i = 0; i < count; i++){
			if (live[i] == o){
				live[i] = live[--count];
				live[count] = null;
				kinds[o.kind]--;
				if (o.kind == WEB) webs--;
				FxBudget.add( FxBudget.DECALS, -1 );
				return;
			}
		}
	}

	@Override
	public void kill(){
		super.kill();
		leave( this );
	}

	@Override
	public void update(){
		super.update();
		if (Emitter.freezeEmitters && Game.timeTotal > 1) return;
		cx += x - placedX;
		cy += y - placedY;
		float dt = Game.elapsed;
		age += dt;
		since += dt;
		if (since > hold + fade){
			kill();
			return;
		}
		int[] f = frame;
		if (altLeft > 0){
			altLeft -= dt;
			if (alt != null) f = alt;
		}
		if (reveal >= 0){
			float p = Math.min( 1f, age / revealFor );
			crop( p, f );
			if (p >= 1f) reveal = -1;
		} else if (f != shownFrame || x != placedX || y != placedY){
			crop( 1f, f );
		}
		float a = peak;
		if (in > 0 && age < in) a *= age / in;
		if (since > hold) a *= 1f - (since - hold) / fade;
		am = a;
		if (kind == WEB && (glintIn -= dt) <= 0){
			glintIn = FxRandom.Float( DEW_MIN, DEW_MAX );
			dew();
		}
	}

	//a drop of dew on the silk catching the light
	private void dew(){
		if (!Fx.seen( cx, cy )) return;
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		FxFactories.glint( 0xF0F8FF ).emit( glow, 0, cx + FxRandom.IntRange( -5, 5 ), cy + FxRandom.IntRange( -5, 5 ) );
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

	/** A web's two looks, by its cell's parity (and mirrored by it). */
	public static int[] webFrame( int cell, int width ){
		return FxFrames.WEB[((cell % width) + (cell / width)) & 1];
	}
}

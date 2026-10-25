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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.items.catching.FishingLine;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.Visual;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.RectF;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A fishing line's picture (items/catching/FishingLine), on the render thread: the rod in the hero's
 * hands, the line from its tip, sagging, to a red and white bobber riding the water (or sitting in a
 * hole cut in the ice), and what it does:
 *
 * - CAST: the bobber flies out from the rod's tip in an arc (CAST_TIME) and plops in - a small splash,
 *   or chips of ice off the hole;
 * - WAITING: it rides the water, a pixel up and down, now and then a faint ring off it;
 * - NIBBLE: it twitches, a small ring each time;
 * - BITE: it is pulled under and bobs up again, a ring and a few droplets each time, and a "!" hangs
 *   over it while the bite holds; the line goes taut;
 * - and how it came in (FishingLine.End): LANDED, what was caught flies out of the water to the hero
 *   in an arc, shedding drops, the bobber with it; EMPTY, ESCAPED and PULLED, the bobber is drawn back
 *   to the rod's tip (an escape with a swirl first).
 *
 * Everything is placed from the hero's sprite each frame (the bobber this many cells from him), so a
 * window's slide moves it all with him. Shown only while the line's slice is the one on screen. The
 * matter it throws draws on the effects' own dice (FxRandom), only while it shows.
 */
public class FishingFx extends Visual {

	//its sheet (tools/assets/catch_art_gen.py): the rod held up, the bobber afloat, dipping and under,
	//the "!", a pixel of line, and a hole in the ice
	private static final int[] ROD = { 0, 0, 12, 12 }, FLOAT = { 12, 0, 5, 7 }, DIP = { 17, 0, 5, 7 },
			UNDER = { 22, 0, 5, 7 }, BANG = { 27, 0, 3, 8 }, DOT = { 30, 0, 1, 1 }, HOLE = { 0, 12, 16, 16 };
	//where the rod is held (its handle's pixel) and its tip, in its frame facing right
	private static final int HANDLE_X = 1, HANDLE_Y = 10, TIP_X = 10, TIP_Y = 1;

	/** The cast's flight and the reel's, s. */
	public static final float CAST_TIME = 0.35f, REEL_TIME = 0.45f, BACK_TIME = 0.3f;
	/** The line's sag while it waits and when taut, px. */
	public static final float SAG = 6f, TAUT = 1.5f;
	/** The line's colour and alpha. */
	public static final int LINE = 0xE6E4DA;
	public static final float LINE_ALPHA = 0.8f;

	private static final HashMap<FishingLine, FishingFx> showing = new HashMap<>();

	/** The line came out (its buff attached, or the hero's sprite made with it on): its picture, once. Any thread. */
	public static void show( final FishingLine line ){
		Fx.post( () -> {
			if (showing.containsKey( line ) || !(Game.scene() instanceof GameScene)) return;
			FishingFx fx = new FishingFx( line );
			GameScene.effect( fx );
			if (fx.parent != null) showing.put( line, fx );
		} );
	}

	/** The line came in: its picture plays out how (FishingLine.end) and is gone. Any thread. */
	public static void hide( final FishingLine line ){
		Fx.post( () -> {
			FishingFx fx = showing.get( line );
			if (fx != null) fx.closing();
		} );
	}

	private final FishingLine line;
	private final Image rod, bobber, bang;
	//the bobber's poses on its sheet, worked out once
	private final RectF floatUV, dipUV, underUV;
	private Image hole;
	private ItemSprite caught;
	private final ArrayList<Image> dots = new ArrayList<>();
	private float t, castT = -1f, closeT = -1f, ringIn, twitchIn, dipT;
	private boolean closing, plopped, splashed;
	//the bobber's point as last drawn, and where the rod's tip was
	private float bx, by, tipX, tipY;

	private FishingFx( FishingLine line ){
		super( 0, 0, 0, 0 );
		this.line = line;
		visible = false;
		rod = image( ROD );
		bobber = image( FLOAT );
		bang = image( BANG );
		bang.visible = false;
		floatUV = uv( FLOAT );
		dipUV = uv( DIP );
		underUV = uv( UNDER );
		if (line.fresh){
			castT = 0f;
			line.fresh = false;
		} else {
			//one loaded from a save is already in the water
			plopped = true;
		}
	}

	private RectF uv( int[] f ){
		return bobber.texture.uvRect( f[0], f[1], f[0] + f[2], f[1] + f[3] );
	}

	private Image image( int[] f ){
		Image i = new Image( Assets.Effects.FISHING, f[0], f[1], f[2], f[3] );
		i.visible = false;
		GameScene.effect( i );
		return i;
	}

	private Image dot( int i ){
		while (dots.size() <= i){
			Image d = new Image( Assets.Effects.FISHING, DOT[0], DOT[1], DOT[2], DOT[3] );
			d.hardlight( LINE );
			d.alpha( LINE_ALPHA );
			d.visible = false;
			GameScene.effect( d );
			//behind the bobber and the rod
			if (d.parent != null) d.parent.sendToBack( d );
			dots.add( d );
		}
		return dots.get( i );
	}

	//the line came in: play out how, then go
	private void closing(){
		if (closing) return;
		closing = true;
		closeT = 0f;
		if (line.end == FishingLine.End.NONE) closeT = Float.MAX_VALUE;
	}

	@Override
	public void destroy(){
		super.destroy();
		if (showing.get( line ) == this) showing.remove( line );
		for (Image i : new Image[]{ rod, bobber, bang, hole, caught }){
			if (i != null){
				i.killAndErase();
				i.destroy();
			}
		}
		for (Image d : dots){
			d.killAndErase();
			d.destroy();
		}
		dots.clear();
	}

	private void gone(){
		killAndErase();
		destroy();
	}

	@Override
	public void update(){
		super.update();
		Char hero = line.target;
		CharSprite hs = hero == null ? null : hero.sprite;
		boolean shown = hs != null && hs.parent != null && line.onThisSlice();
		if (closing && (closeT == Float.MAX_VALUE || !shown)){
			gone();
			return;
		}
		if (!shown){
			hideAll();
			return;
		}
		if (Emitter.freezeEmitters) return;
		float dt = Game.elapsed;
		t += dt;

		//the hero's cell's middle, his hand and the rod's tip
		float hx = hs.x + hs.width() / 2f, hy = hs.y + hs.height() - 2f;
		boolean left = hs.flipHorizontal;
		float handX = left ? hs.x + hs.width() * 0.38f : hs.x + hs.width() * 0.62f;
		float handY = hs.y + hs.height() * 0.6f;
		rod.flipHorizontal = left;
		rod.x = Math.round( left ? handX - (ROD[2] - 1 - HANDLE_X) : handX - HANDLE_X );
		rod.y = Math.round( handY - HANDLE_Y );
		tipX = left ? rod.x + (ROD[2] - 1 - TIP_X) + 0.5f : rod.x + TIP_X + 0.5f;
		tipY = rod.y + TIP_Y + 0.5f;
		rod.visible = hs.visible;

		//where the bobber floats: the cast's cell's middle
		float wx = hx + line.dx() * DungeonTilemap.SIZE, wy = hy + line.dy() * DungeonTilemap.SIZE;
		if (line.ice()) holeAt( wx, wy );

		float sag = SAG;
		int[] pose = FLOAT;
		bang.visible = false;
		if (closing){
			float p = closeT;
			closeT += dt;
			if (!closeOut( wx, wy, p )) return;
			sag = TAUT;
		} else if (castT >= 0f && castT < CAST_TIME){
			//flying out from the tip in an arc
			float p = castT / CAST_TIME;
			castT += dt;
			bx = tipX + (wx - tipX) * p;
			by = tipY + (wy - tipY) * p - 12f * 4f * p * (1f - p);
			sag = TAUT;
		} else {
			if (!plopped){
				plopped = true;
				plop( wx, wy );
			}
			bx = wx;
			by = wy;
			FishingLine.State s = line.state;
			if (s == FishingLine.State.BITE){
				pose = bite( wx, wy, dt );
				sag = TAUT;
				bang.visible = hs.visible;
				bang.x = Math.round( wx - 1f );
				bang.y = Math.round( wy - 17f - Math.abs( (float)Math.sin( t * 4f ) ) );
			} else {
				pose = waiting( wx, wy, dt, s == FishingLine.State.NIBBLE );
			}
		}
		place( pose, hs.visible );
		//the line runs to the bobber's top, or down to the water as it is pulled under
		lay( tipX, tipY, bx, by - (pose == UNDER ? 0f : pose == DIP ? 1f : 3f), sag, hs.visible );
	}

	//the bobber riding the water: a pixel up and down, a faint ring now and then; a nibble twitches it
	private int[] waiting( float wx, float wy, float dt, boolean nibble ){
		by = wy + (Math.sin( t * 2.2f ) > 0.35 ? -1f : 0f);
		if (!line.ice() && (ringIn -= dt) <= 0f){
			ringIn = FxRandom.Float( 1.8f, 3.2f );
			ring( wx, wy, WaterFX.S, 0.45f );
		}
		if (!nibble) return FLOAT;
		if ((twitchIn -= dt) <= 0f){
			twitchIn = FxRandom.Float( 0.45f, 0.8f );
			dipT = 0.12f;
			if (!line.ice()) ring( wx, wy, WaterFX.S, 0.7f );
		}
		if (dipT > 0f){
			dipT -= dt;
			return DIP;
		}
		return FLOAT;
	}

	//a fish on: pulled under and up again, a ring and drops each time
	private int[] bite( float wx, float wy, float dt ){
		if ((twitchIn -= dt) <= 0f){
			twitchIn = FxRandom.Float( 0.55f, 0.85f );
			dipT = 0.28f;
			if (!line.ice()){
				ring( wx, wy, WaterFX.M, 0.85f );
				if (FxBudget.keep( FxBudget.P2, wx, wy )) WaterFX.droplets( wx, wy - 1f, FxRandom.IntRange( 2, 3 ), 25f, 45f, 90f, foam() );
			} else {
				chips( wx, wy, 2 );
			}
		}
		if (dipT > 0f){
			dipT -= dt;
			return dipT > 0.1f ? UNDER : DIP;
		}
		return FLOAT;
	}

	//how it came in, played out from (wx, wy) `p` s in: false once it is all over (and gone)
	private boolean closeOut( float wx, float wy, float p ){
		FishingLine.End end = line.end;
		if (end == FishingLine.End.LANDED){
			if (!splashed){
				splashed = true;
				if (!line.ice()) WaterFX.splash( wx, wy, WaterFX.M, Element.NONE );
				else chips( wx, wy, 5 );
				if (line.landed >= 0){
					caught = new ItemSprite( line.landed );
					GameScene.effect( caught );
					if (caught.parent != null) caught.parent.bringToFront( caught );
				}
			}
			float k = Math.min( 1f, p / REEL_TIME );
			float hx = tipX, hy = tipY + 12f;
			bx = wx + (hx - wx) * k;
			by = wy + (hy - wy) * k - 16f * 4f * k * (1f - k);
			if (caught != null){
				caught.x = Math.round( bx - caught.width() / 2f );
				caught.y = Math.round( by - caught.height() + 2f );
				caught.angle = (1f - k) * (bx < hx ? -35f : 35f);
				caught.visible = true;
				if (k > 0.2f && k < 0.7f && FxRandom.chance( 0.25f ) && FxBudget.keep( FxBudget.P3, bx, by )){
					WaterFX.droplets( bx, by - 2f, 1, 5f, 15f, 120f, foam() );
				}
			}
			if (k >= 1f){
				gone();
				return false;
			}
			return true;
		}
		if (end == FishingLine.End.ESCAPED && !splashed){
			splashed = true;
			if (!line.ice()) ring( wx, wy, WaterFX.M, 0.9f );
		}
		float k = Math.min( 1f, p / BACK_TIME );
		bx = wx + (tipX - wx) * k;
		by = wy + (tipY + 4f - wy) * k - 8f * 4f * k * (1f - k);
		if (k >= 1f){
			gone();
			return false;
		}
		return true;
	}

	private void place( int[] pose, boolean seen ){
		bobber.frame( pose == DIP ? dipUV : pose == UNDER ? underUV : floatUV );
		bobber.x = Math.round( bx - 2f );
		bobber.y = Math.round( by - 5f );
		bobber.visible = seen;
		if (caught != null) bobber.visible = false;
	}

	//the line from the rod's tip to the bobber's top, sagging, a pixel at a time
	private void lay( float x0, float y0, float x1, float y1, float sag, boolean seen ){
		float mx = (x0 + x1) / 2f, my = Math.max( y0, y1 ) + sag;
		int n = (int)Math.max( Math.abs( x1 - x0 ), Math.abs( y1 - y0 ) ) + 1;
		int used = 0, lastX = Integer.MIN_VALUE, lastY = Integer.MIN_VALUE;
		for (int i = 0; i <= n && n > 1; i++){
			float s = i / (float)n, r = 1f - s;
			int px = Math.round( r * r * x0 + 2 * r * s * mx + s * s * x1 );
			int py = Math.round( r * r * y0 + 2 * r * s * my + s * s * y1 );
			if (px == lastX && py == lastY) continue;
			lastX = px;
			lastY = py;
			Image d = dot( used++ );
			d.x = px;
			d.y = py;
			d.visible = seen;
		}
		for (int i = used; i < dots.size(); i++) dots.get( i ).visible = false;
	}

	//the hole cut in the ice, under the bobber
	private void holeAt( float wx, float wy ){
		if (hole == null){
			hole = new Image( Assets.Effects.FISHING, HOLE[0], HOLE[1], HOLE[2], HOLE[3] );
			GameScene.floorEffect( hole );
		}
		hole.x = Math.round( wx - 8f );
		hole.y = Math.round( wy - 8f );
		hole.visible = true;
	}

	//the bobber plopping in: a small splash, or chips of ice off the hole's rim
	private void plop( float wx, float wy ){
		if (line.ice()){
			chips( wx, wy, 4 );
		} else {
			WaterFX.splash( wx, wy, WaterFX.S, Element.NONE );
		}
	}

	private void ring( float x, float y, int size, float alpha ){
		WaterFX.quietRing( x, y, size, foam(), WaterFX.alphaOf( size ) * alpha, FxBudget.P2 );
	}

	private static int foam(){
		return WaterFX.liquid().foam;
	}

	//chips of ice thrown off the hole
	private static void chips( float x, float y, int n ){
		FxEmitter e = Fx.air();
		if (e == null) return;
		for (int i = 0; i < n; i++){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			((FxParticle) e.recycle( FxParticle.class )).reset( x + FxRandom.Float( -3f, 3f ), y - 1f )
					.look( FxFrames.SHARD_ICE[FxRandom.Int( FxFrames.SHARD_ICE.length )] ).paint( 0xE4F4FF )
					.vel( FxRandom.Float( -18f, 18f ), -FxRandom.Float( 20f, 40f ) ).gravity( 180f )
					.life( FxRandom.Float( 0.3f, 0.45f ) ).alpha( 0.95f, 0f, 0.12f ).priority( FxBudget.P2 );
		}
	}

	private void hideAll(){
		rod.visible = bobber.visible = bang.visible = false;
		if (hole != null) hole.visible = false;
		if (caught != null) caught.visible = false;
		for (Image d : dots) d.visible = false;
	}
}

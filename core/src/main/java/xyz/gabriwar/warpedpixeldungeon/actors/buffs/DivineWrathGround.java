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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Bundle;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Divine Wrath: holy ground left by the cleric's blows. Enemies standing on it burn each
 * turn, twice as hard if unholy; at mastery the ground holds them in place.
 *
 * Its look is hallowed ground, never a fire on the floor nor Sanctuary's circle: a halo of light
 * lying on each cell over a pool of deep gold, a crest of light rolling across them (Hallow), holy
 * motes rising off it, gold glints where it is laid and where it burns.
 */
public class DivineWrathGround extends Buff {

	/** turns a tile stays consecrated after its last blow */
	public static final int DURATION = 3;
	/** holy damage each turn to an enemy on the ground */
	public static final int BURN = 4;

	{
		type = buffType.NEUTRAL;
		announced = false;
	}

	private int[] cells = new int[0];
	private int[] left = new int[0];
	private int rank = 1;
	private int depth = -1, branch = -1;

	private final ArrayList<Emitter> shafts = new ArrayList<>();
	private Hallow hallow;
	//the cells laid by the last consecrate, whose halos open (none past it)
	private int[] fresh = new int[0];

	private boolean sameFloor(){
		return depth == Dungeon.depth && branch == Dungeon.branch;
	}

	public void consecrate( Collection<Integer> add, int rank ){
		if (!sameFloor()){
			cells = new int[0];
			left = new int[0];
			depth = Dungeon.depth;
			branch = Dungeon.branch;
		}
		this.rank = rank;
		int[] born = new int[add.size()];
		int nb = 0;
		for (int c : add){
			int i = indexOf( c );
			if (i >= 0){
				left[i] = DURATION;
				continue;
			}
			born[nb++] = c;
			cells = java.util.Arrays.copyOf( cells, cells.length + 1 );
			left = java.util.Arrays.copyOf( left, left.length + 1 );
			cells[cells.length - 1] = c;
			left[left.length - 1] = DURATION;
			if (Dungeon.level.heroFOV[c]) CellEmitter.get( c ).burst( Speck.factory( Speck.YELLOW_LIGHT ), 3 );
		}
		fresh = java.util.Arrays.copyOf( born, nb );
		fx( true );
		fresh = new int[0];
	}

	private int indexOf( int cell ){
		for (int i = 0; i < cells.length; i++) if (cells[i] == cell) return i;
		return -1;
	}

	@Override
	public boolean act(){
		if (!(target instanceof Hero) || !sameFloor() || cells.length == 0){
			detach();
			return true;
		}
		//the cell nearest the hero that burned, where the sound comes from; every cell that burned,
		//whose halo flares
		int burned = -1;
		int[] flared = new int[cells.length];
		int nf = 0;
		int kept = 0;
		int[] nc = new int[cells.length];
		int[] nl = new int[cells.length];
		for (int i = 0; i < cells.length; i++){
			int c = cells[i];
			Char ch = Actor.findChar( c );
			if (ch != null && ch.alignment == Char.Alignment.ENEMY && ch.isAlive()){
				boolean unholy = Char.hasProp( ch, Char.Property.UNDEAD ) || Char.hasProp( ch, Char.Property.DEMONIC );
				ch.damage( unholy ? BURN * 2 : BURN, this );
				SkillFX.flash( ch );
				if (Dungeon.level.heroFOV[c]) CellEmitter.center( c ).burst( Speck.factory( Speck.YELLOW_LIGHT ), unholy ? 6 : 3 );
				if (ch.isAlive() && rank >= Skill.MAX_LEVEL && !Char.hasProp( ch, Char.Property.BOSS ))
					Buff.prolong( ch, Roots.class, 2f );
				burned = SpatialSound.nearer( burned, c );
				flared[nf++] = c;
			}
			if (--left[i] > 0){
				nc[kept] = c;
				nl[kept++] = left[i];
			}
		}
		if (burned >= 0) SpatialSound.play( Assets.Sounds.RAY, burned, 0.6f, 1.4f );
		boolean changed = kept != cells.length;
		cells = java.util.Arrays.copyOf( nc, kept );
		left = java.util.Arrays.copyOf( nl, kept );
		if (cells.length == 0){
			detach();
			return true;
		}
		if (changed) fx( true );
		for (int i = 0; i < nf; i++){
			if (hallow != null) hallow.flare( flared[i] );
			if (SkillVfx.seen( flared[i] )) PixelFlare.show( SkillVfx.cx( flared[i] ), SkillVfx.cy( flared[i] ), 0xF1D99C, PixelFlare.S, 0.25f );
		}
		spend( TICK );
		return true;
	}

	@Override
	public void fx( boolean on ){
		for (Emitter e : shafts) e.on = false;
		shafts.clear();
		//the halos of cells that went stay a moment, fading (every halo when the ground goes)
		if (hallow != null){
			hallow.fadeExcept( on && sameFloor() ? cells : new int[0] );
			hallow = null;
		}
		if (!on || target.sprite == null || target.sprite.parent == null || !sameFloor()) return;
		for (int c : cells){
			Emitter e = CellEmitter.get( c );
			e.pour( SkillBursts.HOLY_MOTE, 0.3f );
			shafts.add( e );
		}
		hallow = Hallow.show( cells, fresh );
	}

	/** The hallowed ground's light, its own (never Sanctuary's glyph circle, never a fire on the
	 *  floor, never a trap's plate): on each cell a halo of light lying on the floor (FxFrames.RING_13,
	 *  RING_LIT, about RING_A) round a faint glow (Light.POOL_15, POOL_LIT, about POOL_A), breathing by
	 *  BREATH; a crest of light rolls out from the ground's middle across its halos every RIPPLE s
	 *  (CREST_LEN s on each, at RIPPLE_SPEED px/s), brightening each by CREST and swelling it a step
	 *  (RING_17) as it passes, so the cells read as one ground. A new cell's
	 *  halo opens over OPEN s, stepping RING_5, RING_9, RING_13; a burning cell's flares to full for
	 *  FLARE s; a cell that goes fades over FADE s. Two FxPics a cell (at most the ground's cells),
	 *  added in the floor's layer under the bodies; nothing allocated a frame; let go of with
	 *  Fx.dispose. Made anywhere (asset keys), shown on the render thread. */
	public static class Hallow extends Group {

		public static final int RING_LIT = 0xFFE6A1, POOL_LIT = 0xFFE6A1, FLARE_LIT = 0xFFF4D0;
		public static final float RING_A = 0.6f, POOL_A = 0.18f, BREATH = 0.08f, CREST = 0.4f;
		public static final float RIPPLE = 1.4f, CREST_LEN = 0.3f, RIPPLE_SPEED = 64f;
		public static final float OPEN = 0.15f, FLARE = 0.07f, FADE = 0.3f;
		/** How far under the cell's middle the halo lies, px (on the floor under a body's middle). */
		public static final int DROP = 2;

		final int[] cells;
		final FxPic[] rings, pools;
		//per cell: its distance from the ground's middle (px), seconds since it opened (-1: there
		//already), its flare left, its fade left (-1: none)
		final float[] dist, open, flare, fade;
		//given way to the next (any thread: it is never added past it); ending its fades (render thread)
		private volatile boolean fading;
		private boolean ending;

		Hallow( int[] cells, int[] fresh ){
			this.cells = cells.clone();
			int n = cells.length;
			rings = new FxPic[n];
			pools = new FxPic[n];
			dist = new float[n];
			open = new float[n];
			flare = new float[n];
			fade = new float[n];
			float mx = 0, my = 0;
			for (int c : cells){
				mx += SkillVfx.cx( c ) / n;
				my += SkillVfx.cy( c ) / n;
			}
			for (int i = 0; i < n; i++){
				float x = SkillVfx.cx( cells[i] ), y = SkillVfx.cy( cells[i] ) + DROP;
				pools[i] = pic( SkillVfx.LIGHT_SHEET, FxFrames.Light.POOL_15, POOL_LIT, x, y );
				rings[i] = pic( SkillVfx.SHEET, FxFrames.RING_13, RING_LIT, x, y );
				dist[i] = (float) Math.hypot( x - mx, y - DROP - my );
				open[i] = -1;
				for (int f : fresh) if (f == cells[i]) open[i] = 0;
				fade[i] = -1;
			}
		}

		private FxPic pic( String sheet, int[] frame, int color, float x, float y ){
			FxPic p = new FxPic( sheet );
			p.show( frame );
			p.additive = true;
			p.lit( color );
			p.place( x, y );
			p.visible = false;
			add( p );
			return p;
		}

		/** The ground's light for these cells onto the floor's layer (posted from any thread). */
		public static Hallow show( int[] cells, int[] fresh ){
			if (cells.length == 0) return null;
			final Hallow g = new Hallow( cells, fresh );
			Fx.post( () -> {
				Group floor = GameScene.layer( GameScene.Layer.FLOOR );
				if (floor == null || g.fading) return;
				floor.add( g );
			} );
			return g;
		}

		/** The cell burned: its halo flares (any thread). */
		public void flare( final int cell ){
			Fx.post( () -> {
				for (int i = 0; i < cells.length; i++) if (cells[i] == cell) flare[i] = FLARE;
			} );
		}

		/** Gives way to the next ground: the halos of cells it does not keep fade out, the rest go at
		 *  once (the next draws them), and then it is let go of (any thread). */
		public void fadeExcept( final int[] keep ){
			fading = true;
			Fx.post( () -> {
				ending = true;
				boolean any = false;
				for (int i = 0; i < cells.length; i++){
					boolean kept = false;
					for (int k : keep) if (k == cells[i]) kept = true;
					if (kept || parent == null) rings[i].visible = pools[i].visible = false;
					else { fade[i] = FADE; any = true; }
				}
				if (!any) Fx.dispose( this );
			} );
		}

		@Override
		public void update(){
			super.update();
			float dt = Game.elapsed;
			//the scene's clock, so a ground made again when it changes keeps its breath and its crest
			float time = Game.timeTotal;
			boolean any = false;
			for (int i = 0; i < cells.length; i++){
				FxPic r = rings[i], p = pools[i];
				if (ending && fade[i] < 0) continue;
				float breath = 1f + BREATH * (float) Math.sin( time * 2.4f + i * 0.6f );
				//the crest rolling out from the middle: up and down again over CREST_LEN s on each halo
				float w = (time - dist[i] / RIPPLE_SPEED) % RIPPLE;
				if (w < 0) w += RIPPLE;
				float crest = w < CREST_LEN ? (float) Math.sin( Math.PI * w / CREST_LEN ) : 0f;
				float ra = (RING_A + CREST * crest) * breath, pa = POOL_A * breath;
				//the crest swells the halo a step as it passes
				int[] frame = crest > 0.6f ? FxFrames.RING_17 : FxFrames.RING_13;
				if (open[i] >= 0){
					open[i] += dt;
					float k = Math.min( 1f, open[i] / OPEN );
					frame = k < 1 / 3f ? FxFrames.RING_5 : k < 2 / 3f ? FxFrames.RING_9 : FxFrames.RING_13;
					pa *= k;
					if (open[i] >= OPEN) open[i] = -1;
				}
				boolean flared = flare[i] > 0;
				if (flared){
					flare[i] -= dt;
					ra = 1f;
					pa = 0.8f;
				}
				r.lit( flared ? FLARE_LIT : RING_LIT );
				p.lit( flared ? FLARE_LIT : POOL_LIT );
				boolean faded = false;
				if (fade[i] >= 0){
					fade[i] -= dt;
					float k = Math.max( 0f, fade[i] / FADE );
					ra *= k;
					pa *= k;
					if (fade[i] > 0) any = true;
					else faded = true;
				}
				//its frame stepped about its middle, a slide of the window taken in
				r.takeSlide();
				if (r.width != frame[2]) r.show( frame );
				r.place( r.midX(), r.midY() );
				p.takeSlide();
				p.place( p.midX(), p.midY() );
				boolean seen = !faded && SkillVfx.seen( Fx.cellAt( r.midX(), r.midY() - DROP ) );
				r.visible = p.visible = seen;
				r.am = Math.min( 1f, ra );
				p.am = Math.min( 1f, pa );
			}
			if (ending && !any) Fx.dispose( this );
		}
	}

	private static final String CELLS = "cells", LEFT = "left", RANK = "rank", DEPTH = "depth", BRANCH = "branch";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CELLS, cells );
		bundle.put( LEFT, left );
		bundle.put( RANK, rank );
		bundle.put( DEPTH, depth );
		bundle.put( BRANCH, branch );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		cells = bundle.getIntArray( CELLS );
		left = bundle.getIntArray( LEFT );
		rank = bundle.getInt( RANK );
		depth = bundle.getInt( DEPTH );
		branch = bundle.getInt( BRANCH );
		if (cells == null || left == null || cells.length != left.length){
			cells = new int[0];
			left = new int[0];
		}
	}
}

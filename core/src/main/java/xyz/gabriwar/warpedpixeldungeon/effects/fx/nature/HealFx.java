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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.nature;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;

/**
 * Healing and shielding, answered where they land: every healing number (FloatingText's HEALING
 * icon: the host's, and a co-op guest's as it replays the text) gets a green-gold ring at the
 * healed one's feet (9 to 17 px, 0.35 s) and two to six motes rising by the amount; every shielding
 * number a pale bubble (BUBBLE_23, BBAACC) fading round him; a drink from the well of health its
 * water lifting to the drinker, a bloom and motes. Render thread (handed there otherwise).
 */
public final class HealFx {

	private HealFx(){}

	/** The healing ring: its widths, life, colour and alpha; the motes, at most. */
	public static final int RING_FROM = 9, RING_TO = 17, RING_COLOR = 0xB8F0A0;
	public static final float RING_LIFE = 0.35f, RING_ALPHA = 0.6f;
	public static final int MOTES_MIN = 2, MOTES_MAX = 6;
	/** Its pool of light under the ring: size, level (of the pool's cap). */
	public static final int POOL = 15;
	public static final float POOL_LEVEL = 1f;
	/** The shield's bubble: colour, alpha, fade. */
	public static final int BUBBLE_COLOR = 0xBBAACC;
	public static final float BUBBLE_ALPHA = 0.4f, BUBBLE_FADE = 0.3f;
	/** The well: droplets lifted, their speed and life; motes. */
	public static final int DROPLETS = 6, DRINK_MOTES = 6;
	public static final float LIFT = 20f, LIFT_FOR = 0.4f;

	//hooks played since the scene began (tests read them)
	private static int heals, shields;

	/** Healing hooks played (a test's count). */
	public static synchronized int heals(){
		return heals;
	}

	/** Shielding hooks played. */
	public static synchronized int shields(){
		return shields;
	}

	/** How many motes a heal of `amount` raises: 2 + min(4, amount / 5). */
	public static int motes( int amount ){
		return MOTES_MIN + Math.min( MOTES_MAX - MOTES_MIN, Math.max( 0, amount ) / 5 );
	}

	//the number in a floating text ("12", "+12"), 0 for none
	static int amount( String text ){
		if (text == null) return 0;
		int n = 0;
		boolean any = false;
		for (int i = 0; i < text.length(); i++){
			char c = text.charAt( i );
			if (c >= '0' && c <= '9'){
				n = Math.min( 100000, n * 10 + (c - '0') );
				any = true;
			} else if (any){
				break;
			}
		}
		return n;
	}

	/**
	 * A floating text was shown (FloatingText.show, render thread): (x, y) its place over the
	 * sprite's middle and top, `cell` the cell it is about (-1 for none), `icon` its icon.
	 */
	public static void floatingText( float x, float y, int cell, int icon, String text ){
		if (icon == FloatingText.HEALING) heal( x, y, cell, amount( text ) );
		else if (icon == FloatingText.SHIELDING) shield( x, y, cell );
	}

	//the healed one's feet: his cell's ground, or under the text
	private static float feet( float y, int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return y + 14;
		return (cell / level.width()) * DungeonTilemap.SIZE + 13;
	}

	private static boolean seen( int cell ){
		Level level = Dungeon.level;
		return level == null || cell < 0 || cell >= level.length() || level.heroFOV[cell];
	}

	/** A heal of `amount` shown at the text's (x, y) over `cell`. */
	public static synchronized void heal( float x, float y, int cell, int amount ){
		heals++;
		if (!seen( cell )) return;
		float fy = feet( y, cell );
		FxRing.ground( GameScene.layer( GameScene.Layer.FLOOR ), x, fy, RING_FROM, RING_TO, RING_LIFE, RING_COLOR, RING_ALPHA, true );
		//and a breath of its light on the ground, so it reads at night too
		FxLight.pool( x, fy, POOL, Nature.HEAL, POOL_LEVEL ).life( 0.05f, 0.1f, 0.45f );
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		int n = FxBudget.allow( motes( amount ), FxBudget.P2 );
		for (int i = 0; i < n; i++){
			Nature.make( glow, x + FxRandom.Float( -5f, 5f ), fy - FxRandom.Float( 1f, 8f ) )
					.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_1 : FxFrames.PLUS_3 ).paint( Nature.HEAL ).light( true )
					.vel( FxRandom.Float( -1.5f, 1.5f ), -FxRandom.Float( 10f, 16f ) ).drag( 0.5f )
					.life( FxRandom.Float( 0.6f, 1f ) ).alpha( 0.9f, 0.08f + i * 0.04f, 0.35f ).snap().priority( FxBudget.P2 );
		}
	}

	/** A shield shown at the text's (x, y) over `cell`: a bubble round the one it is on. */
	public static synchronized void shield( float x, float y, int cell ){
		shields++;
		if (!seen( cell )) return;
		Emitter glow = Nature.glow( null );
		if (glow == null || !FxBudget.keep( FxBudget.P2, x, y )) return;
		Nature.lit( glow, x, y + 7 ).look( FxFrames.Light.BUBBLE_23 ).paint( BUBBLE_COLOR )
				.life( BUBBLE_FADE + 0.05f ).alpha( BUBBLE_ALPHA, 0.05f, BUBBLE_FADE ).snap().priority( FxBudget.P2 );
	}

	/** A drink from the well of health at `cell`: six droplets lift from its water to the drinker
	 *  (20 px/s, 0.4 s), a bloom of healing light, six motes rising. Any thread. */
	public static void drink( final Char drinker, final int cell ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> drink( drinker, cell ) );
			return;
		}
		CharSprite s = drinker.sprite;
		Level level = Dungeon.level;
		if (s == null || level == null || cell < 0 || cell >= level.length() || !level.heroFOV[cell]) return;
		int w = level.width();
		float cx = (cell % w) * DungeonTilemap.SIZE + 8, water = (cell / w) * DungeonTilemap.SIZE + 13;
		float chest = s.y + s.height() / 2f;
		FxLight.glow( cx, chest, 23, Nature.HEAL, 1f ).life( 0.05f, 0.1f, 0.45f );
		Emitter air = Nature.air( null ), glow = Nature.glow( null );
		if (air != null){
			for (int i = 0; i < DROPLETS; i++){
				if (!FxBudget.keep( FxBudget.P2, cx, water )) continue;
				Nature.make( air, cx + FxRandom.Float( -5f, 5f ), water ).look( FxFrames.DROP_2 ).paint( Nature.WELL_DROP )
						.vel( 0, -LIFT * FxRandom.Float( 0.9f, 1.1f ) ).life( LIFT_FOR ).alpha( 1f, 0.05f, 0.1f ).priority( FxBudget.P2 );
			}
		}
		if (glow != null){
			for (int i = 0; i < DRINK_MOTES; i++){
				if (!FxBudget.keep( FxBudget.P2, cx, chest )) continue;
				Nature.make( glow, cx + FxRandom.Float( -6f, 6f ), chest + FxRandom.Float( -2f, 6f ) )
						.look( FxFrames.MOTE_1 ).paint( Nature.HEAL ).light( true )
						.vel( 0, -FxRandom.Float( 8f, 14f ) ).life( FxRandom.Float( 0.9f, 1.3f ) ).alpha( 0.9f, 0.2f + i * 0.05f, 0.4f )
						.snap().priority( FxBudget.P2 );
			}
		}
	}

	/** A new scene: the counts from zero. */
	public static synchronized void reset(){
		heals = shields = 0;
	}
}

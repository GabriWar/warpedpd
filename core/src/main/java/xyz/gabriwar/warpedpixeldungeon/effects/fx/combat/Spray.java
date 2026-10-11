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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxDecal;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.particles.Emitter;

/**
 * One splash (effects/Splash, every call its own: two splashes of a colour in one frame keep
 * their own directions): what a creature's blood, a potion, a crystal or a pile of bones throws
 * when struck, by its colour -
 *
 *   LIQUID droplets turned to their way, stepping 3 -> 2 -> 1 px long as they fly, the first one or
 *          two bigger drips; they land: a splat in the colour (0.6, held 3 s, faded over 3) for the
 *          drips and one droplet, on water a small ring and once a cloud of the colour spreading on
 *          the surface (two EDGE_WISPs at 0.35, 2 px/s apart, 1.5 s)
 *   DUST   (greys, bone) pebbles bouncing once, a puff, and a mark of dust
 *   SPARK  (white, pale bright light: crystals, purity, energy) sparks falling and bouncing, as light
 *
 * At most MOST a splash. Emitted on the render thread from FxRandom; the call's one draw of the
 * game's Random is Splash's.
 */
public final class Spray extends Emitter.Factory implements FxParticle.Fate {

	public static final int LIQUID = 0, DUST = 1, SPARK = 2;
	/** The most particles a splash throws. */
	public static final int MOST = 24;
	/** The splats a splash leaves at most: its drips' and one droplet's. */
	static final int SPLATS = 3;
	/** A splat's alpha, hold and fade. */
	static final float SPLAT_ALPHA = 0.6f, SPLAT_HOLD = 3f, SPLAT_FADE = 3f;
	/** A cloud on the water: alpha, life, spread px/s. */
	static final float CLOUD_ALPHA = 0.35f, CLOUD_LIFE = 1.5f, CLOUD_SPREAD = 2f;

	public final int color, kind;
	public final float dir, cone;
	private int splats;
	private boolean clouded;

	public Spray( int color, float dir, float cone ){
		this.color = color & 0xFFFFFF;
		this.kind = kind( color );
		this.dir = dir;
		this.cone = cone;
	}

	/** What a colour splashes as: low-chroma greys as dust (white as sparks), pale bright light as
	 *  sparks, everything else as a liquid. */
	public static int kind( int color ){
		int r = (color >> 16) & 0xFF, g = (color >> 8) & 0xFF, b = color & 0xFF;
		int max = Math.max( r, Math.max( g, b ) ), min = Math.min( r, Math.min( g, b ) );
		int chroma = max - min;
		if (chroma <= 0x28){
			if (max >= 0xF0) return SPARK;
			if (max >= 0x30) return DUST;
			return LIQUID;
		}
		if (min >= 0x99 && max >= 0xF0 && chroma >= 0x50) return SPARK;
		return LIQUID;
	}

	/** Whether a kind is light (sparks are). */
	@Override
	public boolean lightMode(){
		return kind == SPARK;
	}

	/** The emitter a splash of this kind belongs in: light for sparks, matter otherwise. Null off
	 *  the game's scene. */
	public FxEmitter emitter(){
		return kind == SPARK ? GameScene.lightEmitter() : GameScene.fxEmitter();
	}

	private float angle(){
		return dir + FxRandom.Float( -cone / 2f, cone / 2f );
	}

	@Override
	public void emit( Emitter e, int index, float x, float y ){
		switch (kind){
			case DUST:  dust( e, index, x, y ); break;
			case SPARK: spark( e, x, y ); break;
			default:    liquid( e, index, x, y ); break;
		}
	}

	private static final int[][] DROP_LONG = { FxFrames.DROP_3 }, DROP_MID = { FxFrames.DROP_2 }, DROP_DOT = { FxFrames.MOTE_1 };

	private void liquid( Emitter e, int index, float x, float y ){
		boolean drip = index < 2;
		if (!FxBudget.keep( drip ? FxBudget.P1 : FxBudget.P2, x, y )) return;
		FxParticle p = CombatFactories.make( e, x, y ).paint( color ).fate( this ).priority( drip ? FxBudget.P1 : FxBudget.P2 );
		if (drip){
			//a heavier drip: 2 px, slower, falling short
			p.look( FxFrames.MOTE_2 ).polar( angle(), FxRandom.Float( 15f, 30f ) ).gravity( 240f )
					.ground( y + FxRandom.Float( 3f, 6f ), FxParticle.DIE, 0 ).life( 1f ).alpha( 1f, 0, 0.1f );
		} else {
			//a droplet: long while fast, shorter as it slows and falls; it lives till it lands
			float life = FxRandom.Float( 0.8f, 1f );
			p.look( DROP_LONG, 0 ).then( 0.3f, DROP_MID ).then( 0.6f, DROP_DOT ).orient()
					.polar( angle(), FxRandom.Float( 30f, 60f ) ).gravity( 240f )
					.ground( y + FxRandom.Float( 2f, 8f ), FxParticle.DIE, 0 ).life( life ).alpha( 1f, 0, life * 0.25f );
		}
	}

	private void dust( Emitter e, int index, float x, float y ){
		if (index == 0 && FxBudget.keep( FxBudget.P2, x, y )){
			CombatFactories.make( e, x, y ).look( FxFrames.PUFF_S[FxRandom.Int( FxFrames.PUFF_S.length )] ).paint( color ).smooth()
					.life( 0.6f ).alpha( 0.45f, 0, 0.4f ).vel( FxRandom.Float( -3f, 3f ), -6f ).drag( 2f ).priority( FxBudget.P2 );
			if (splats++ < 1){
				FxDecal.at( x, y + 6f, FxFrames.SPLAT[0], color, 0.4f, 2f, 2f, FxRandom.chance( 0.5f ) );
			}
		}
		if (!FxBudget.keep( FxBudget.P2, x, y )) return;
		int dark = Ramp.lerp( color, 0x000000, 0.3f );
		CombatFactories.make( e, x, y ).look( FxRandom.chance( 0.5f ) ? FxFrames.PEBBLE_2 : FxFrames.PEBBLE_3 )
				.paint( FxRandom.chance( 0.5f ) ? color : dark ).polar( angle(), FxRandom.Float( 30f, 60f ) ).gravity( 240f )
				.ground( y + FxRandom.Float( 4f, 8f ), FxParticle.BOUNCE, 0.3f ).spin( 0.12f )
				.life( FxRandom.Float( 0.5f, 0.8f ) ).alpha( 1f, 0, 0.25f ).priority( FxBudget.P2 );
	}

	private void spark( Emitter e, float x, float y ){
		if (!FxBudget.keep( FxBudget.P2, x, y )) return;
		CombatFactories.make( e, x, y ).look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 15f )
				.ramp( sparkRamp( color ), true ).life( FxRandom.Float( 0.3f, 0.6f ) ).alpha( 1f, 0, 0.1f )
				.polar( angle(), FxRandom.Float( 30f, 70f ) ).gravity( 120f ).drag( 1.2f )
				.ground( y + FxRandom.Float( 4f, 8f ), FxParticle.BOUNCE, 0.35f ).fate( FxFactories.SPARK_END ).priority( FxBudget.P2 );
	}

	private static final com.badlogic.gdx.utils.IntMap<Ramp> sparkRamps = new com.badlogic.gdx.utils.IntMap<>();

	private static synchronized Ramp sparkRamp( int color ){
		Ramp r = sparkRamps.get( color );
		if (r == null){
			r = new Ramp( 0xFFFFFF, color, Ramp.lerp( color, 0x000000, 0.5f ) );
			sparkRamps.put( color, r );
		}
		return r;
	}

	// ------------------------------------------------------------------ landing (a liquid's)

	@Override
	public void end( FxParticle p, boolean landed ){
		if (!landed) return;
		if (WaterFX.onWater( p.px, p.py )){
			WaterFX.drop( p.px, p.py, 0.2f, Element.NONE );
			if (!clouded){
				clouded = true;
				cloud( p.px, p.py );
			}
		} else if (splats < SPLATS && (p.priority() <= FxBudget.P1 || splats < SPLATS - 1)){
			splats++;
			FxDecal.at( p.px, p.py, FxFrames.SPLAT[splats == 1 ? 1 : FxRandom.Int( FxFrames.SPLAT.length )], color,
					SPLAT_ALPHA, SPLAT_HOLD, SPLAT_FADE, FxRandom.chance( 0.5f ) );
		}
		p.kill();
	}

	//the colour clouding the water where it fell: two wisps spreading apart on the surface
	private void cloud( float x, float y ){
		FxEmitter e = GameScene.recycle( GameScene.Layer.SURFACE, FxEmitter.class );
		if (e == null) return;
		e.revive();
		e.pos( x, y );
		e.burst( new Emitter.Factory(){
			@Override
			public void emit( Emitter em, int index, float ex, float ey ){
				if (!FxBudget.keep( FxBudget.P2, ex, ey )) return;
				float side = index == 0 ? -1f : 1f;
				CombatFactories.make( em, ex + side * 2f, ey ).look( FxFrames.EDGE_WISP[index % FxFrames.EDGE_WISP.length] )
						.paint( color ).smooth().life( CLOUD_LIFE ).alpha( CLOUD_ALPHA, 0.1f, CLOUD_LIFE * 0.6f )
						.vel( side * CLOUD_SPREAD, 0 ).snap().priority( FxBudget.P2 );
			}
		}, 2 );
	}
}

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

import com.watabou.noosa.particles.Emitter;

import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.AmbientSnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DriftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FallingLeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.MistParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.RainParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;

import static xyz.gabriwar.warpedpixeldungeon.effects.WeatherSprites.*;

/**
 * What the weather's blobs look like on the ground: the storm cloud's dark wisps
 * with rain falling out of them, the blizzard's driven snow, the mist's creeping
 * fog, the heat haze's shimmer, the moonbeam's motes, and the seasonal winds with
 * what each carries: seeds in summer, leaves in autumn, petals in spring, embers
 * when it burns. Each is one factory that mixes its parts by turn. Below them,
 * the details any blob mixes into its own look (layered): bubbles, motes, drips,
 * flies, soot, embers, frost, leaf bits, sparkles. All of it draws the effects'
 * own random numbers (FxRandom), never the game's.
 */
public final class WeatherBlobFX {

	private WeatherBlobFX(){}

	private static DriftParticle drift( Emitter e ){
		return (DriftParticle)e.recycle( DriftParticle.class );
	}

	/** how high a cloud floats over the ground it belongs to: two cells */
	private static final float CLOUD_LIFT = 32f;

	/**
	 * The body of a cloud, drawn two cells above the cell that owns it and lit
	 * from its own ground: lumpy puffs that drift, with the deck of them thick
	 * enough to read as weather overhead rather than a smudge.
	 */
	private static void cloudBody( Emitter e, float x, float y, int top, int under, float alpha, float size ){
		float cy = y - CLOUD_LIFT + FxRandom.Float( -4f, 4f );
		float cx = x + FxRandom.Float( -7f, 7f );
		boolean big = FxRandom.Float() < 0.55f;
		drift( e ).look( big ? PUFF_L : PUFF_S, FxRandom.Float() < 0.62f ? top : under,
						FxRandom.Float( 3.5f, 6f ), alpha * FxRandom.Float( 0.85f, 1.15f ) )
				.at( cx, cy ).from( x, y )
				.motion( 0, -1f, 1.5f, 0.6f, 0, 0 ).sway( 1.6f, 0.5f, 0.5f )
				.fades( 0.25f, 0.3f );
		if (size > 1f && FxRandom.Float() < 0.35f){
			drift( e ).look( PUFF_L, under, FxRandom.Float( 3f, 5f ), alpha * 0.7f )
					.at( cx + FxRandom.Float( -10f, 10f ), cy + FxRandom.Float( 2f, 7f ) ).from( x, y )
					.motion( 0, -0.6f, 1.2f, 0.4f, 0, 0 ).sway( 1.2f, 0.4f, 0.5f )
					.fades( 0.3f, 0.35f );
		}
	}

	/** a storm: the dark deck of cloud overhead, and the rain falling out of it */
	public static final Emitter.Factory STORM_CLOUD = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			int k = index % 4;
			if (k == 0){
				cloudBody( e, x, y, 0x4A5264, 0x333A48, 0.72f, 1.2f );
			} else if (k == 1){
				//the ragged underside of the deck, hanging lower than the body
				drift( e ).look( FxRandom.Float() < 0.5f ? WISP_L : WISP_XL, FxRandom.Float() < 0.5f ? 0x5A6478 : 0x6C7488, FxRandom.Float( 2f, 3.5f ), 0.5f )
						.at( x, y - 14 ).from( x, y ).motion( 0, -1, 2, 1, 0, 0 ).sway( 2f, 0.7f, 0.35f );
			} else {
				((RainParticle)e.recycle( RainParticle.class )).reset( x, y - FxRandom.Float( 4, 12 ) );
			}
		}
	};

	/** snow driven through the cloud: streaks, flakes, and the white of it */
	public static final Emitter.Factory BLIZZARD = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			int k = index % 5;
			if (k == 4){
				cloudBody( e, x, y, 0xD8E0EC, 0xA8B4C8, 0.6f, 1f );
			} else if (k == 0){
				drift( e ).look( WISP_XL, 0xEEF4FF, FxRandom.Float( 1.5f, 2.5f ), 0.35f )
						.at( x, y ).motion( 0, -2, 3, 2, 0, 0 ).sway( 3f, 1f, 0.6f );
			} else if (k == 1){
				((AmbientSnowParticle)e.recycle( AmbientSnowParticle.class )).reset( x, y );
			} else {
				((AmbientSnowParticle)e.recycle( AmbientSnowParticle.class )).resetStorm( x, y );
			}
		}
	};

	/**
	 * Fog: a bank of it. Bodies of pale cloud lying on the ground with longer
	 * wisps creeping between them, and a thinner deck of the same hanging above,
	 * so a mist patch is something you stand inside rather than a faint smear.
	 */
	public static final Emitter.Factory MIST = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			int k = index % 5;
			if (k == 0){
				//the deck above, thin and slow
				drift( e ).look( FxRandom.Float() < 0.5f ? PUFF_L : PUFF_S, 0xC6CEDC, FxRandom.Float( 3.5f, 6f ), 0.3f )
						.at( x + FxRandom.Float( -6f, 6f ), y - CLOUD_LIFT + FxRandom.Float( -3f, 5f ) ).from( x, y )
						.motion( 0, -0.6f, 1.2f, 0.4f, 0, 0 ).sway( 1.4f, 0.4f, 0.5f ).fades( 0.3f, 0.3f );
			} else if (k == 1 || k == 2){
				//the body on the ground: lumps of pale cloud, slowly rolling
				drift( e ).look( FxRandom.Float() < 0.5f ? PUFF_L : PUFF_S,
								FxRandom.Float() < 0.5f ? 0xE2E8F2 : 0xCED6E2, FxRandom.Float( 2.5f, 4.5f ), 0.42f )
						.at( x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( -3f, 3f ) )
						.motion( 0, -1.5f, 2f, 1f, 0, 0 ).sway( 2.2f, 0.5f, 0.5f ).fades( 0.25f, 0.3f );
			} else {
				((MistParticle)e.recycle( MistParticle.class )).reset( x, y );
			}
		}
	};

	/**
	 * The air standing over ground hot enough to hurt: threads of it rising and
	 * snaking, thickest where it is hottest, with a glare at their feet.
	 */
	public static final Emitter.Factory HEAT_RAYS = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (index % 6 == 0){
				drift( e ).look( GLOW_3, 0xFFDCA0, FxRandom.Float( 0.5f, 0.9f ), 0.45f )
						.at( x, y + 3 ).motion( 0, -5, 2, 2, 0, -3 ).pulse( 7f );
			} else {
				drift( e ).look( HEAT_RAY, FxRandom.Float() < 0.4f ? 0xFFE6B4 : 0xFFF4DC,
								FxRandom.Float( 0.7f, 1.3f ), FxRandom.Float( 0.3f, 0.5f ) )
						.at( x + FxRandom.Float( -5f, 5f ), y + FxRandom.Float( -2f, 4f ) )
						.motion( 0, -20, 3, 6, 0, -10 ).sway( 3.5f, 9f, 0.1f ).fades( 0.25f, 0.4f ).tumbling( 0.3f, 0.5f );
			}
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	/** hot air rising off the ground: pale shimmer, and a spark of glare now and then */
	public static final Emitter.Factory HEAT_HAZE = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (index % 5 == 0){
				drift( e ).look( GLOW_3, 0xFFE8B0, FxRandom.Float( 0.6f, 1.2f ), 0.5f )
						.at( x, y ).motion( 0, -8, 2, 3, 0, -4 ).pulse( 6f ).sway( 1f, 8f, 0 );
			} else {
				drift( e ).look( FxRandom.Float() < 0.5f ? WISP_S : WISP_L, 0xFFF0D8, FxRandom.Float( 0.8f, 1.6f ), 0.14f )
						.at( x, y ).motion( 0, -14, 3, 4, 0, -6 ).sway( 4f, 7f, 0.1f ).fades( 0.3f, 0.4f );
			}
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	/** the shaft of moonlight, and the motes that hang in it */
	public static final Emitter.Factory MOONBEAM = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (index % 3 == 0){
				drift( e ).look( FxRandom.Float() < 0.6f ? GLOW_3 : GLOW_5, 0xC8D8FF, FxRandom.Float( 2f, 4f ), 0.45f )
						.at( x, y ).motion( 0, -3, 1, 1, 0, 0 ).sway( 2f, 1.2f, 0 ).pulse( 2.5f );
			} else {
				((ShaftParticle)e.recycle( ShaftParticle.class )).reset( x, y );
			}
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	};

	/** leaves the autumn wind piles up and lifts again */
	public static final Emitter.Factory FALLEN_LEAVES = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			int[] cols = FallingLeafParticle.seasonColors( GameCalendar.season() );
			drift( e ).look( LEAVES[FxRandom.Int( LEAVES.length )], cols[FxRandom.Int( cols.length )], FxRandom.Float( 1.5f, 3f ), 1f )
					.at( x, y ).motion( 0, -10, 6, 4, 0, 18 ).sway( 8f, 3f, 1.2f ).tumbling();
		}
	};

	/** the summer wind: grass seed and small green leaves on the move */
	public static final Emitter.Factory SOWING_WIND = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (index % 3 == 0){
				drift( e ).look( LEAF_C, FxRandom.Float() < 0.5f ? 0x78B24A : 0x5A9E3A, FxRandom.Float( 1.5f, 2.5f ), 1f )
						.at( x, y ).motion( 6, -6, 4, 3, 0, 6 ).sway( 6f, 3f, 1.5f ).tumbling();
			} else {
				drift( e ).look( FxRandom.Float() < 0.6f ? SPECK_1 : SPECK_2, FxRandom.Float() < 0.5f ? 0xE8E080 : 0xC8D860, FxRandom.Float( 1f, 2f ), 0.8f )
						.at( x, y ).motion( 10, -4, 6, 4, 0, 2 ).sway( 3f, 5f, 1.8f );
			}
		}
	};

	/** the autumn wind: red and gold leaves, and the dust it kicks up */
	public static final Emitter.Factory HARVEST_WIND = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (index % 3 != 0){
				int[] cols = FallingLeafParticle.seasonColors( GameCalendar.Season.AUTUMN );
				drift( e ).look( LEAVES[FxRandom.Int( LEAVES.length )], cols[FxRandom.Int( cols.length )], FxRandom.Float( 1.5f, 3f ), 1f )
						.at( x, y ).motion( 8, -8, 6, 4, 0, 10 ).sway( 8f, 3f, 1.5f ).tumbling();
			} else {
				drift( e ).look( FxRandom.Float() < 0.5f ? SPECK_1 : SPECK_2, 0xBB9966, FxRandom.Float( 1f, 2f ), 0.45f )
						.at( x, y ).motion( 12, -3, 6, 3, 0, 1 ).sway( 2f, 5f, 1.8f );
			}
		}
	};

	/** the spring wind: blossom and pollen */
	public static final Emitter.Factory NECTAR_WIND = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (index % 2 == 0){
				drift( e ).look( PETAL, FxRandom.Float() < 0.5f ? 0xFFAABB : 0xFFE0EA, FxRandom.Float( 2f, 3.5f ), 1f )
						.at( x, y ).motion( 4, -6, 4, 3, 0, 4 ).sway( 6f, 2.5f, 1.2f ).tumbling();
			} else {
				drift( e ).look( SPECK_1, FxRandom.Float() < 0.5f ? 0xF0E060 : 0xF8F0A0, FxRandom.Float( 1.5f, 3f ), 0.75f )
						.at( x, y ).motion( 3, -8, 3, 3, 0, 0 ).sway( 3f, 4f, 1f ).pulse( 3f );
			}
		}
	};

	/** a wind that burns: embers, ash, and the hot air over them */
	public static final Emitter.Factory FIREWIND = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			int k = index % 4;
			if (k == 0){
				drift( e ).look( WISP_S, 0xFF9050, FxRandom.Float( 0.8f, 1.4f ), 0.3f )
						.at( x, y ).motion( 4, -16, 3, 4, 0, -8 ).sway( 3f, 6f, 0.8f ).fades( 0.3f, 0.4f );
			} else if (k == 1){
				drift( e ).look( ASH_2, 0x5A5258, FxRandom.Float( 1.5f, 3f ), 0.6f )
						.at( x, y ).motion( 6, -6, 4, 3, 0, 3 ).sway( 5f, 3f, 1.4f ).tumbling();
			} else {
				drift( e ).look( EMBER, FxRandom.Float() < 0.5f ? 0xFF8040 : 0xFFC050, FxRandom.Float( 1f, 2.2f ), 1f )
						.at( x, y ).motion( 8, -12, 6, 5, 0, -2 ).sway( 4f, 5f, 1.6f ).pulse( 5f ).fades( 0.05f, 0.5f );
			}
		}
	};

	// ------------------------------------------------ details for any blob

	/**
	 * The blob's own look with a detail every nth particle: the bodies stay,
	 * and among them rise bubbles, sparks, drips, flies, whatever the cloud is made
	 * of. The detail draws in the base's blend mode.
	 */
	public static Emitter.Factory layered( final Emitter.Factory base, final Emitter.Factory detail, final int every ){
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter e, int index, float x, float y ){
				if (index % every == 0) detail.emit( e, index, x, y );
				else base.emit( e, index, x, y );
			}
			@Override
			public boolean lightMode(){
				return base.lightMode();
			}
		};
	}

	private static FxParticle fx( Emitter e, float x, float y ){
		return ((FxParticle)e.recycle( FxParticle.class )).reset( x, y );
	}

	/** A bubble's end: it pops into a little ring for a frame or two. */
	static final FxParticle.Fate BUBBLE_POP = ( p, landed ) -> {
		Emitter e = (Emitter)p.parent;
		if (e == null || !FxBudget.keep( FxBudget.P3 )) return;
		fx( e, p.px, p.py ).look( FxFrames.POP_5 ).paint( p.colorNow() ).life( 0.08f ).alpha( 0.8f, 0, 0.04f )
				.snap().priority( FxBudget.P3 );
	};

	/** bubbles welling up through the cloud, swaying, popping into a ring as they go */
	public static Emitter.Factory bubbles( final int color ){
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter e, int index, float x, float y ){
				if (!FxBudget.keep( FxBudget.P2, x, y )) return;
				fx( e, x, y + FxRandom.Float( 2, 8 ) ).look( FxRandom.chance( 0.6f ) ? FxFrames.BUBBLE_2 : FxFrames.BUBBLE_3 )
						.paint( color ).life( FxRandom.Float( 0.6f, 1.1f ) ).alpha( 0.9f, 0.1f, 0.12f )
						.vel( FxRandom.Float( -1f, 1f ), -FxRandom.Float( 6f, 12f ) ).sway( 1.5f, 5f )
						.fate( BUBBLE_POP ).snap().priority( FxBudget.P2 );
			}
		};
	}

	//a mote's twinkle: a dot that swells to two pixels a moment now and then
	private static final int[][] TWINKLE = { FxFrames.MOTE_1, FxFrames.MOTE_1, FxFrames.MOTE_2, FxFrames.MOTE_1 };

	/** motes that hang and twinkle, on whole pixels */
	public static Emitter.Factory motes( final int color ){
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter e, int index, float x, float y ){
				if (!FxBudget.keep( FxBudget.P2, x, y )) return;
				fx( e, x, y ).look( TWINKLE, FxRandom.Float( 3f, 5f ) ).paint( color ).life( FxRandom.Float( 1f, 2f ) )
						.alpha( 0.85f, 0.2f, 0.4f ).smooth().vel( FxRandom.Float( -2f, 2f ), FxRandom.Float( -4f, 0f ) ).drag( 1f )
						.sway( 2f, 2f ).snap().priority( FxBudget.P2 );
			}
		};
	}

	//the confusion's colours: one array, never one an emit
	private static final int[] SWIRL_COLORS = { 0xF080F0, 0x80C0FF, 0x80F0A0, 0xFFE080 };

	/** specks swinging in wide loops, in the confusion's colours */
	public static final Emitter.Factory SWIRL = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			fx( e, x, y ).look( FxRandom.chance( 0.7f ) ? FxFrames.MOTE_2 : FxFrames.PLUS_3 ).paint( FxRandom.element( SWIRL_COLORS ) )
					.life( FxRandom.Float( 1.2f, 2f ) ).alpha( 0.8f, 0.2f, 0.4f ).smooth()
					.vel( 0, -FxRandom.Float( 2f, 6f ) ).sway( 10f, 5f ).turbulence( 2f, 4f ).priority( FxBudget.P2 );
		}
	};

	/** small dark things that dart about */
	public static final Emitter.Factory FLIES = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			fx( e, x, y ).look( FxFrames.MOTE_1 ).paint( 0x2A2418 ).life( FxRandom.Float( 1.5f, 2.5f ) ).alpha( 0.9f, 0.1f, 0.1f )
					.vel( FxRandom.Float( -6f, 6f ), FxRandom.Float( -6f, 6f ) ).sway( 7f, 14f ).turbulence( 2f, 9f )
					.snap().priority( FxBudget.P2 );
		}
	};

	/** an acid's sizzle */
	public static final int SIZZLE = 0xFFB040;

	/** A drip's landing: on the water a ring, on the floor a splat a moment. */
	static final FxParticle.Fate DRIP_END = ( p, landed ) -> {
		if (!landed) return;
		if (WaterFX.onWater( p.px, p.py )){
			WaterFX.drop( p.px, p.py, 0.1f, p.colorNow() );
			return;
		}
		Emitter e = (Emitter)p.parent;
		if (e == null || !FxBudget.keep( FxBudget.P3 )) return;
		fx( e, p.px, p.py ).look( FxFrames.SPLAT[0] ).paint( p.colorNow() ).life( 0.15f ).alpha( 0.8f, 0, 0.1f )
				.snap().priority( FxBudget.P3 );
	};

	/** An acid drip's landing: a drip's, and two sparks of sizzle over the night's tint. */
	static final FxParticle.Fate ACID_END = ( p, landed ) -> {
		DRIP_END.end( p, landed );
		if (!landed || WaterFX.onWater( p.px, p.py )) return;
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		for (int i = 0; i < 2; i++){
			if (!FxBudget.keep( FxBudget.P3 )) return;
			fx( glow, p.px, p.py - 1 ).look( FxFrames.MOTE_1 ).paint( SIZZLE ).light( true ).life( 0.2f ).alpha( 1f, 0, 0.1f )
					.polar( FxRandom.Float( (float)-Math.PI, 0f ), FxRandom.Float( 10f, 20f ) ).gravity( 60f )
					.priority( FxBudget.P3 );
		}
	};

	private static Emitter.Factory drips( final int color, final FxParticle.Fate end ){
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter e, int index, float x, float y ){
				if (!FxBudget.keep( FxBudget.P2, x, y )) return;
				fx( e, x, y - FxRandom.Float( 4, 10 ) ).look( FxRandom.chance( 0.5f ) ? FxFrames.DROP_2 : FxFrames.DROP_3 ).orient()
						.paint( color ).life( 1f ).alpha( 0.9f, 0.05f, 0.05f )
						.vel( 0, FxRandom.Float( 16f, 28f ) ).gravity( 60f ).ground( y + FxRandom.Float( 6f, 10f ), FxParticle.DIE, 0 )
						.fate( end ).priority( FxBudget.P2 );
			}
		};
	}

	/** drops that fall out of the cloud: a splat on the floor, a ring on the water */
	public static Emitter.Factory drips( final int color ){
		return drips( color, DRIP_END );
	}

	/** acid dripping out of the cloud: a drip that sizzles where it lands */
	public static Emitter.Factory acidDrips( final int color ){
		return drips( color, ACID_END );
	}

	/** soot: dark flakes that lift and turn */
	public static final Emitter.Factory SOOT = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			drift( e ).look( FxRandom.Float() < 0.6f ? ASH_2 : SPECK_1, FxRandom.Float() < 0.5f ? 0x3A3638 : 0x4A4448, FxRandom.Float( 1.5f, 3f ), 0.7f )
					.at( x, y ).motion( 0, -5, 3, 2, 0, -1 ).sway( 4f, 2.5f, 0.3f ).tumbling();
		}
	};

	//the fires an ember's colour may belong to
	private static final Element[] EMBER_ELEMENTS = { Element.FIRE, Element.HELL, Element.FIEND, Element.MAGIC_FIRE,
			Element.HALOMETHANE, Element.LAVA, Element.HOLY, Element.ELMO };

	/** The fire whose glow is nearest a colour. */
	static Element emberElement( int color ){
		Element best = Element.FIRE;
		int bestD = Integer.MAX_VALUE;
		for (Element el : EMBER_ELEMENTS){
			int c = el.light;
			int dr = ((c >> 16) & 0xFF) - ((color >> 16) & 0xFF), dg = ((c >> 8) & 0xFF) - ((color >> 8) & 0xFF), db = (c & 0xFF) - (color & 0xFF);
			int d = dr * dr + dg * dg + db * db;
			if (d < bestD){
				bestD = d;
				best = el;
			}
		}
		return best;
	}

	/** embers rising out of a fire: the kit's ember of the fire nearest this colour */
	public static Emitter.Factory embers( final int color ){
		return FxFactories.ember( emberElement( color ) );
	}

	//frost's crystal: a plus that winks to a dot and back
	private static final int[][] CRYSTAL = { FxFrames.PLUS_3, FxFrames.MOTE_1 };

	/** frost: crystals that wink and drift down */
	public static final Emitter.Factory FROST = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if (!FxBudget.keep( FxBudget.P2, x, y )) return;
			fx( e, x, y ).look( CRYSTAL, FxRandom.Float( 2.5f, 4f ) ).paint( 0xE8F4FF ).life( FxRandom.Float( 1f, 2f ) )
					.alpha( 0.9f, 0.15f, 0.3f ).vel( FxRandom.Float( -1f, 1f ), FxRandom.Float( 2f, 4f ) ).sway( 2f, 2f )
					.snap().priority( FxBudget.P2 );
		}
	};

	/** bits of withered leaf, dry gold to brown, tumbling up out of the cloud */
	private static final int[] WITHERED = { 0x9A8A58, 0x6A5A38, 0xB0A068 };
	public static final Emitter.Factory LEAF_BITS = new Emitter.Factory() {
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			drift( e ).look( LEAVES[FxRandom.Int( LEAVES.length )], WITHERED[FxRandom.Int( WITHERED.length )], FxRandom.Float( 1.2f, 2f ), 1f )
					.at( x, y ).motion( 0, -14, 6, 4, 0, 20 ).sway( 6f, 3f, 0 ).tumbling();
		}
	};

	/** a glint now and then, over the night's tint */
	public static Emitter.Factory sparkles( final int color ){
		return new Emitter.Factory() {
			@Override
			public void emit( Emitter e, int index, float x, float y ){
				FxEmitter glow = Fx.glowAir();
				FxFactories.glint( color ).emit( glow != null ? glow : e, index, Math.round( x ), Math.round( y ) );
			}
		};
	}
}

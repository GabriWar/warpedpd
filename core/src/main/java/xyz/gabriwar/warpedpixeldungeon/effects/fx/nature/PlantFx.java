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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRing;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import xyz.gabriwar.warpedpixeldungeon.tiles.TerrainFeaturesTilemap;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;

/**
 * Plants: a seed coming up (its picture grown from its foot in four steps, a pixel's bounce, soil
 * and leaves thrown), a plant triggered or withering (squashed a pixel for 0.05 s, petals of its
 * own colour puffing off) and the plants' own moments: Sungrass's sunlight, Earthroot's bark, Blindweed's glare,
 * Fadeleaf's motes, Lavenderlantern's bloom. Render thread unless said; visual only, FxRandom.
 */
public final class PlantFx {

	private PlantFx(){}

	/** A seed coming up: its pebbles and leaves. */
	public static final int PEBBLES = 4, LEAVES_MIN = 2, LEAVES_MAX = 3;
	/** A plant triggered: its squash, s; its petals. */
	public static final float SQUASH = 0.05f;
	public static final int PETALS = 3;

	/** Sungrass's shaft: the tallest, holy, up to 0.5. */
	public static final Emitter.Factory SUNGRASS_SHAFT = ShaftParticle.factory( FxFrames.Light.SHAFT_40, Nature.HOLY, 0.5f );

	private static float x( int cell ){
		return (cell % Dungeon.level.width()) * DungeonTilemap.SIZE;
	}

	private static float y( int cell ){
		return (cell / Dungeon.level.width()) * DungeonTilemap.SIZE;
	}

	private static boolean seen( int cell ){
		Level l = Dungeon.level;
		return l != null && cell >= 0 && cell < l.length() && l.heroFOV[cell];
	}

	// ------------------------------------------------------------------ a seed coming up

	/** A seed's plant, grown from its foot over its cell's top-left (x, y) in `into`, then `then`. */
	public static void grow( Group into, Image plant, float x, float y, Runnable then ){
		into.add( new TileCrop( plant, x, y, TileCrop.GROW, 1f, false ).then( then ) );
	}

	/** What a seed coming up throws: four grains of soil and two or three leaves (at most 7). */
	public static void sprung( int cell ){
		if (!seen( cell )) return;
		Emitter air = Nature.air( null );
		if (air == null) return;
		float cx = x( cell ) + 8, cy = y( cell ) + 12;
		for (int i = 0; i < PEBBLES; i++){
			if (!FxBudget.keep( FxBudget.P3, cx, cy )) continue;
			Nature.make( air, cx + FxRandom.Float( -3f, 3f ), cy ).look( FxRandom.chance( 0.6f ) ? FxFrames.PEBBLE_2 : FxFrames.PEBBLE_3 )
					.paint( Element.EARTH.ramp.stop( 2 + FxRandom.Int( 2 ) ) )
					.vel( FxRandom.Float( -16f, 16f ), -FxRandom.Float( 20f, 36f ) ).gravity( 200f )
					.ground( cy + FxRandom.Float( 1f, 3f ), FxParticle.BOUNCE, 0.3f )
					.life( FxRandom.Float( 0.6f, 0.9f ) ).alpha( 1f, 0, 0.25f ).priority( FxBudget.P3 );
		}
		int leaves = FxRandom.IntRange( LEAVES_MIN, LEAVES_MAX );
		for (int i = 0; i < leaves; i++){
			if (!LeafParticle.allowed( cx, cy )) continue;
			((LeafParticle) air.recycle( LeafParticle.class )).leaf( cx + FxRandom.Float( -4f, 4f ), cy - 4, Nature.levelLeaf() );
		}
	}

	// ------------------------------------------------------------------ triggered, withered

	/** A plant triggered or withering at `cell`: its picture squashed a pixel for SQUASH s, then
	 *  PETALS petals of its colour puffing off (its leaves are its caller's burst). */
	public static void wither( final Plant plant, final int cell ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> wither( plant, cell ) );
			return;
		}
		if (!seen( cell )) return;
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		Image pic = floor != null ? TerrainFeaturesTilemap.getPlantVisual( plant ) : null;
		if (pic != null){
			pic.x = x( cell );
			pic.y = y( cell ) + 1;
			floor.add( new Brief( pic, SQUASH ) );
		}
		Emitter air = Nature.air( null );
		if (air == null) return;
		int color = Plant.colorOf( plant );
		float cx = x( cell ) + 8, cy = y( cell ) + 9;
		for (int i = 0; i < PETALS; i++){
			if (!FxBudget.keep( FxBudget.P2, cx, cy )) continue;
			float a = (float)(-Math.PI / 2) + FxRandom.Float( -1.1f, 1.1f );
			Nature.make( air, cx, cy ).look( FxFrames.PETAL_B ).paint( color )
					.polar( a, FxRandom.Float( 22f, 36f ) ).gravity( 90f ).drag( 1.5f )
					.sway( FxRandom.Float( 3f, 6f ), FxRandom.Float( 4f, 7f ) )
					.ground( cy + FxRandom.Float( 2f, 6f ), FxParticle.SETTLE, 0 )
					.life( 1.2f ).alpha( 1f, 0, 0.3f ).priority( FxBudget.P2 );
		}
	}

	/** A picture shown for a moment, then let go. */
	public static final class Brief extends Image {
		private float left;

		public Brief( Image src, float seconds ){
			super( src );
			x = src.x;
			y = src.y;
			left = seconds;
		}

		@Override
		public void update(){
			super.update();
			if ((left -= Game.elapsed) <= 0) Fx.dispose( this );
		}
	}

	// ------------------------------------------------------------------ the plants' own

	/** Sungrass trampled at `cell`: a pool of holy light under it and six motes rising. */
	public static void sungrass( final int cell ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> sungrass( cell ) );
			return;
		}
		if (!seen( cell )) return;
		float cx = x( cell ) + 8, cy = y( cell ) + 12;
		FxLight.pool( cx, cy, 23, Nature.HOLY, 1f ).life( 0.1f, 0.4f, 0.8f );
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		int n = FxBudget.allow( 6, FxBudget.P2 );
		for (int i = 0; i < n; i++){
			Nature.make( glow, cx + FxRandom.Float( -6f, 6f ), cy - FxRandom.Float( 0f, 6f ) )
					.look( FxRandom.chance( 0.7f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).paint( Nature.HOLY ).light( true )
					.vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 10f, 18f ) ).drag( 0.6f )
					.life( FxRandom.Float( 0.8f, 1.3f ) ).alpha( 0.9f, 0.1f, 0.4f ).snap().priority( FxBudget.P2 );
		}
	}

	/** Earthroot's armour taking hold at `cell`: six plates of bark rising round the feet over
	 *  0.2 s and a ring of dust spreading 9 to 23 px over 0.4 s. */
	public static void bark( final int cell ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> bark( cell ) );
			return;
		}
		if (!seen( cell )) return;
		float cx = x( cell ) + 8, cy = y( cell ) + 13;
		Group floor = GameScene.layer( GameScene.Layer.FLOOR );
		if (floor != null) FxRing.ground( floor, cx, cy, 9, 23, 0.4f, Element.EARTH.ramp.stop( 1 ), 0.6f, false );
		Emitter air = Nature.air( null );
		if (air == null) return;
		for (int i = 0; i < 6; i++){
			if (!FxBudget.keep( FxBudget.P2, cx, cy )) continue;
			//round the feet, on a 2:1 ellipse, rising 6-9 px in 0.2 s and settling back
			float a = (float)(2 * Math.PI * i / 6) + FxRandom.Float( -0.3f, 0.3f );
			float px = cx + (float)Math.cos( a ) * 6, py = cy + (float)Math.sin( a ) * 3;
			Nature.make( air, px, py ).look( FxFrames.BARK[i % 2] ).paint( Element.EARTH.ramp.stop( 2 ) )
					.vel( 0, -FxRandom.Float( 30f, 45f ) ).gravity( 160f ).ground( py, FxParticle.SETTLE, 0 )
					.life( 0.9f ).alpha( 1f, 0, 0.3f ).snap().priority( FxBudget.P2 );
		}
	}

	/** Blindweed's glare at `cell`: a white glow fading over 0.3 s and six twinkles (no screen
	 *  flash). */
	public static void blind( final int cell ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> blind( cell ) );
			return;
		}
		if (!seen( cell )) return;
		float cx = x( cell ) + 8, cy = y( cell ) + 8;
		FxLight.glow( cx, cy, 31, 0xFFFFFF, 1f ).life( 0, 0.02f, 0.3f );
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		Emitter.Factory twinkle = FxFactories.glint( 0xFFFFFF );
		for (int i = 0; i < 6; i++){
			twinkle.emit( glow, i, cx + FxRandom.Float( -9f, 9f ), cy + FxRandom.Float( -9f, 6f ) );
		}
	}

	/** Fadeleaf's passage: eight motes (FF51CC) drawn in where it left over 0.25 s, and thrown
	 *  out where it arrives over 0.25 s. */
	public static void fade( final int from, final int to ){
		if (!Fx.onRenderThread()){
			Fx.post( () -> fade( from, to ) );
			return;
		}
		Emitter glow = Nature.glow( null );
		if (glow == null) return;
		for (int k = 0; k < 2; k++){
			int cell = k == 0 ? from : to;
			if (!seen( cell )) continue;
			float cx = x( cell ) + 8, cy = y( cell ) + 8;
			for (int i = 0; i < 8; i++){
				float a = (float)(2 * Math.PI * i / 8) + FxRandom.Float( -0.2f, 0.2f ), d = FxRandom.Float( 9f, 12f );
				float ox = (float)Math.cos( a ) * d, oy = (float)Math.sin( a ) * d * 0.7f;
				//in to the middle where it leaves, out from it where it arrives
				float sx = k == 0 ? cx + ox : cx, sy = k == 0 ? cy + oy : cy;
				float vx = (k == 0 ? -ox : ox) / 0.25f, vy = (k == 0 ? -oy : oy) / 0.25f;
				Nature.make( glow, sx, sy ).look( FxFrames.MOTE_2 ).paint( 0xFF51CC ).light( true )
						.vel( vx, vy ).life( 0.25f ).alpha( 1f, 0.03f, 0.08f ).priority( FxBudget.P1 );
			}
		}
	}

	/** Lavenderlantern's bloom at `cell`: a violet glow 0.6 fading over 0.5 s, its pool, motes. */
	public static void lantern( final int cell ){
		if (!seen( cell )) return;
		int w = Dungeon.level.width();
		FxRecipes.bloom( (cell % w) * DungeonTilemap.SIZE + 8, (cell / w) * DungeonTilemap.SIZE + 8, 0xA470DD, 47, 0.6f, 0.5f );
	}
}

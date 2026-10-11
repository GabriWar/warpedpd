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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.modules;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.CritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.DungeonCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.SliceCritterSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.FishShadows;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterAmbience;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterDrops;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterLava;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterLive;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterScenes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterTerrain;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterWalk;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.DripParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PointF;

import java.util.List;

/**
 * Water and its life (the water-life area: docs/fx/water-life.md): how the water answers what
 * walks in it, jumps in it, drops into it and rains on it, and what lives in it - wired into the
 * game's scene from here (its live surface and fish shadows, the sprites' steps, landings and
 * waterlines, the terrain's coming and going water) with its gallery page and its debug scenes.
 */
public class WaterLifeFx implements FxModule {

	@Override
	public String key(){
		return "water-life";
	}

	@Override
	public String title(){
		return "Water and its life";
	}

	//the live surface: the palette cycle on the water's plane, the sheens in the sheen layer over
	//it, and what it lays on its surface (glints, foam, whitecaps, bubbles, the lava's life); the
	//fish shadows of the open water under it; a water scene on its way here finished
	@Override
	public void sceneCreated( GameScene scene ){
		WaterLive.sceneCreated();
		GameScene.sheen( new WaterLive() );
		GameScene.subsurface( new FishShadows() );
		WaterScenes.arrived( scene );
	}

	//a wader's crown, droplets, wake and rings, a footstep's flicked grass or snow (not a flier's)
	@Override
	public void stepped( CharSprite s, int from, int to ){
		WaterWalk.stepped( s, from, to );
	}

	//a jumper's landing in the water
	@Override
	public void jumped( CharSprite s ){
		WaterWalk.jumped( s );
	}

	//the waterline over a wader's feet, drawn right after it
	@Override
	public void afterDraw( CharSprite s ){
		WaterWalk.afterDraw( s );
	}

	@Override
	public void gone( CharSprite s ){
		WaterWalk.gone( s );
	}

	//water coming and going: a ring and foam where it comes, droplets and mist where it goes
	@Override
	public void terrainChanged( int cell, int oldTerrain, int newTerrain ){
		WaterTerrain.changed( cell, oldTerrain, newTerrain );
	}

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		WaterScenes.all( out );
	}

	//the liquids the carousel shows, six seconds each
	private static final Element.Liquid[] CAROUSEL = { Element.Liquid.SEWER, Element.Liquid.PRISON, Element.Liquid.CAVE,
			Element.Liquid.CITY, Element.Liquid.LAVA, Element.Liquid.FROZEN, Element.Liquid.TEMPLE };
	private static final float CAROUSEL_EACH = 6f;

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "the seven liquids, six seconds each: cycle, sheens, glints, foam", CAROUSEL.length * CAROUSEL_EACH, s -> {
			for (int i = 0; i < CAROUSEL.length; i++){
				final Element.Liquid l = CAROUSEL[i];
				s.after( i * CAROUSEL_EACH, () -> WaterLive.debugLiquid = l );
			}
			s.after( CAROUSEL.length * CAROUSEL_EACH - 0.05f, () -> WaterLive.debugLiquid = null );
		} );
		page.add( "a wader walks into the pool and through it", 3f, s -> {
			WaterLive.debugLiquid = null;
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 1, 1 ) );
			p.move( s.cell( 2, 1 ) );
			s.after( 0.6f, () -> p.move( s.cell( 3, 1 ) ) );
			s.after( 1.2f, () -> p.move( s.cell( 4, 1 ) ) );
			s.after( 1.8f, () -> p.move( s.cell( 5, 1 ) ) );
		} );
		page.add( "a wader walks out of the pool", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 6, 0 ) );
			s.after( 0.3f, () -> p.move( s.cell( 7, 0 ) ) );
		} );
		page.add( "a wader stands in the pool", 3f, s -> s.puppet( Rat.class, s.cell( 3, -1 ) ) );
		page.add( "a jump into the pool", 2.5f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 1, -2 ) );
			s.after( 0.3f, () -> p.jump( s.cell( 3, 0 ) ) );
		} );
		page.add( "a fish under the water startled by a step", 3f, s -> {
			swimmer( s.cell( 4, 1 ), (float)Math.PI );
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( 1, 1 ) );
			s.after( 1.2f, () -> p.move( s.cell( 2, 1 ) ) );
		} );
		page.add( "a fish comes to look at something dropped in", 4f, s -> {
			swimmer( s.cell( 2, -1 ), 0f );
			s.after( 1f, () -> {
				PointF c = FxStage.center( s.cell( 4, -1 ) );
				WaterDrops.item( c.x, c.y + 2 );
			} );
		} );
		page.add( "the caves' blind fish under their dark water: pale ghosts, and a leap", 4f, s -> {
			WaterLive.debugLiquid = Element.Liquid.CAVE;
			swimmer( s.cell( 1, -1 ), 0f );
			swimmer( s.cell( 4, 1 ), (float)Math.PI );
			s.after( 1.5f, () -> {
				PointF c = FxStage.center( s.poolMiddle() );
				GameScene.effect( CritterSprite.Fish.blind( c.x - 8, c.y + 2, 1 ) );
			} );
			s.after( 3.95f, () -> WaterLive.debugLiquid = null );
		} );
		page.add( "a fish leaps, the water giving it away first", 2.5f, s -> {
			PointF c = FxStage.center( s.poolMiddle() );
			GameScene.effect( new CritterSprite.Fish( false, c.x - 8, c.y + 2, 1 ) );
		} );
		page.add( "drops from the roof: a bead, the fall, a crown and rings", 3f, s -> {
			for (int i = 0; i < 3; i++){
				final int c = s.cell( 2 + 2 * i, i - 1 );
				s.after( 0.6f * i, () -> {
					FxEmitter e = GameScene.fxEmitter();
					if (e == null || c == -1) return;
					PointF p = FxStage.center( c );
					e.pos( p.x, p.y );
					e.burst( DripParticle.FALL, 1 );
				} );
			}
		} );
		page.add( "a strider glides; a frog on the bank plunges", 4f, s -> {
			PointF w = FxStage.center( s.cell( 3, 1 ) ), bank = FxStage.center( s.cell( 1, 0 ) ), in = FxStage.center( s.cell( 2, 0 ) );
			GameScene.floorEffect( new DungeonCritterSprite.Strider( w.x, w.y + 2 ) );
			final DungeonCritterSprite.Frog f = new DungeonCritterSprite.Frog( bank.x, bank.y + 4, in.x, in.y + 4, true );
			GameScene.floorEffect( f );
			s.after( 1.5f, () -> f.scare( bank.x - 16, bank.y ) );
		} );
		page.add( "gnats dip to the water; a fish rises to one now and then", 4f, s -> {
			for (int i = 0; i < 10; i++){
				s.after( 0.35f * i, () -> {
					PointF c = FxStage.center( s.pool()[FxRandom.Int( 15 )] );
					WaterAmbience.dip( c.x + FxRandom.Float( -5f, 5f ), c.y + FxRandom.Float( -3f, 5f ) );
				} );
			}
		} );
		page.add( "light rain on the pool (0.2): rings", 5f, s -> s.sky( PrecipType.RAIN, 0.2f, 2f, false ) );
		page.add( "heavy rain on the pool (0.6): rings and pocks", 5f, s -> s.sky( PrecipType.RAIN, 0.6f, 2f, false ) );
		page.add( "a downpour on the pool (1.0)", 5f, s -> s.sky( PrecipType.RAIN, 1f, 4f, false ) );
		page.add( "water floods a cell and drains away", 3f, s -> {
			final int c = s.cell( 1, -1 );
			flood( c, Terrain.WATER );
			s.after( 1.6f, () -> flood( c, Terrain.EMPTY ) );
		} );
		page.add( "the pool as lava: glow, crust, bubbles, heat, the banks lit, spits", 7f, s -> {
			WaterLive.debugLiquid = Element.Liquid.LAVA;
			for (int i = 0; i < 10; i++){
				s.after( 0.2f + 0.6f * i, () -> {
					PointF c = FxStage.center( s.pool()[FxRandom.Int( 15 )] );
					WaterLava.spit( c.x, c.y + 2 );
				} );
			}
			s.after( 6.9f, () -> WaterLive.debugLiquid = null );
		} );
	}

	//a fish gliding under the pool's surface at a cell, heading a way
	private static void swimmer( int cell, float heading ){
		if (cell == -1) return;
		PointF c = FxStage.center( cell );
		GameScene.subsurface( new SliceCritterSprite.Swimmer( c.x, c.y + 1, heading ) );
	}

	//a cell of the stage's floor become water, or dry again, as the game would make it
	private static void flood( int cell, int terrain ){
		Level level = Dungeon.level;
		if (cell == -1 || level == null) return;
		Level.set( cell, terrain );
		GameScene.updateMap( cell );
	}
}

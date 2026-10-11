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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BallGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blizzard;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CloudOfCorruption;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ConfusionGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CorrosiveGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.CorruptGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Depressant;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.DigestingAcidCloud;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Dirtcloud;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.GooWarn;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.GrassSmokeGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.IceStorm;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Miasma;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ParalyticGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.PoisonGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.PopGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Slownesscloud;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.SmokeScreen;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Snowstorm;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Snowwhirlwind;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.StenchGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ToxicGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Web;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Withercloud;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.IceBlock;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.gas.CellOverlay;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.gas.GasGlow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.gas.GasPuff;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.CorrosionParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PitfallParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoppolarExplosionParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.SkeletonKey;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

import java.util.List;

/**
 * Gases, cold and earth (the gases-cold-earth area, docs/fx/gases-cold-earth.md): every gas cloud,
 * frost and ice, shadow and curses, poison and corrosion, earth, pits and webs.
 *
 * Its hooks: a new scene forgets the last one's cell pictures (CellOverlay); a step through a web
 * shakes it; water freezing cracks white from its middle with a glint, and ice thawing rings the
 * water, throws two drops and breathes a wisp of mist. Its gallery page shows each gas as the real
 * cloud (a 3x3 seeded on the stone patch, a rat walking through), smoke, frost on the floor and on
 * the pool, ice and snow storms, shadow, poison, acid, earth, pits, webs, goo and the spectral wall;
 * its scenes play a part of that page, by day and at night, over and over.
 */
public class GasesColdFx implements FxModule {

	/** Water freezing: its cracks' alpha, growth, hold and fade. */
	public static final float CRACK_ALPHA = 0.5f, CRACK_GROW = 0.3f, CRACK_HOLD = 0.5f, CRACK_FADE = 0.6f;
	/** Ice thawing: its drops and the mist's colour. */
	public static final int THAW_DROPS = 2, THAW_MIST = 0xE4F0FA;

	//the part of the page a scene asked for (null: all of it); read once as the page is built
	private static volatile String focus;

	@Override
	public String key(){
		return "gases-cold-earth";
	}

	@Override
	public String title(){
		return "Gases, cold and earth";
	}

	// ------------------------------------------------------------------ hooks

	@Override
	public void sceneCreated( GameScene scene ){
		CellOverlay.reset();
		GasPuff.reset();
		GasGlow.reset();
	}

	@Override
	public void stepped( CharSprite s, final int from, final int to ){
		if (!CellOverlay.anyWeb()) return;
		Fx.post( () -> {
			CellOverlay.jiggle( from );
			CellOverlay.jiggle( to );
		} );
	}

	@Override
	public void terrainChanged( int cell, int oldTerrain, int newTerrain ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length() || level.heroFOV == null || !level.heroFOV[cell]) return;
		if (oldTerrain == Terrain.WATER && newTerrain == Terrain.FROZEN_WATER) freeze( cell );
		else if (oldTerrain == Terrain.FROZEN_WATER && newTerrain == Terrain.WATER) thaw( cell );
	}

	/** Water freezing on a cell: white cracks spreading from its middle over 0.3 s, and a glint. */
	public static void freeze( int cell ){
		int w = Dungeon.level.width();
		float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		int k = ((cell % w) + (cell / w)) & 1;
		CellOverlay o = CellOverlay.keep( CellOverlay.CRACK, cell, cx, cy + 2, FxFrames.CRACK_17[k], false, 0xFFFFFF,
				CRACK_ALPHA, 0f, CRACK_HOLD, CRACK_FADE, ((cell / w) & 1) == 1 );
		if (o != null && o.isNew()) o.revealCrop( CellOverlay.FROM_MIDDLE, CRACK_GROW );
		FxEmitter glow = Fx.glowAir();
		if (glow != null) FxFactories.glint( 0xFFFFFF ).emit( glow, cell, Math.round( cx ) + 3 - 6 * k, Math.round( cy ) );
	}

	/** Ice thawing on a cell: a middling ring, two drops thrown up and a wisp of mist. */
	public static void thaw( int cell ){
		int w = Dungeon.level.width();
		float cx = (cell % w + 0.5f) * DungeonTilemap.SIZE, cy = (cell / w + 0.5f) * DungeonTilemap.SIZE;
		WaterFX.ring( cx, cy, WaterFX.M );
		WaterFX.droplets( cx, cy, THAW_DROPS, 20f, 35f, 60f, WaterFX.liquid().foam );
		FxEmitter air = Fx.air();
		if (air != null) FxFactories.wisp( THAW_MIST ).emit( air, cell, cx, cy - 2 );
	}

	// ------------------------------------------------------------------ the gallery

	//every gas, with the blob it is
	private static final Object[][] GASES = {
			{ "toxic gas", ToxicGas.class }, { "paralytic gas", ParalyticGas.class }, { "confusion gas", ConfusionGas.class },
			{ "corrosive gas", CorrosiveGas.class }, { "stench", StenchGas.class }, { "smoke screen", SmokeScreen.class },
			{ "poison gas", PoisonGas.class }, { "digesting acid", DigestingAcidCloud.class }, { "grass smoke", GrassSmokeGas.class },
			{ "withering cloud", Withercloud.class }, { "corrupting gas", CorruptGas.class },
			{ "cloud of corruption", CloudOfCorruption.class }, { "depressant", Depressant.class }, { "dirt cloud", Dirtcloud.class },
			{ "miasma", Miasma.class }, { "slowness cloud", Slownesscloud.class }, { "pop gas", PopGas.class },
			{ "ball gas", BallGas.class }, { "ice storm", IceStorm.class } };

	//the stone patch and the row north of it: a 3x3, its middle first
	private static int[] patch( FxStage s ){
		return new int[]{ s.cell( -5, -1 ), s.cell( -6, -2 ), s.cell( -5, -2 ), s.cell( -4, -2 ), s.cell( -6, -1 ), s.cell( -4, -1 ),
				s.cell( -6, 0 ), s.cell( -5, 0 ), s.cell( -4, 0 ) };
	}

	//a cloud of a gas on the patch: thick in its middle, thinner round it
	@SuppressWarnings("unchecked")
	private static void cloud( FxStage s, Class<?> kind, int middle, int rest ){
		s.clearBlobs();
		int[] p = patch( s );
		s.seed( (Class<? extends Blob>) kind, middle, p[0] );
		for (int i = 1; i < p.length; i++) s.seed( (Class<? extends Blob>) kind, rest, p[i] );
	}

	//a rat walking west to east through the patch
	private static void walker( FxStage s, float from ){
		FxStage.Puppet rat = s.puppet( Rat.class, s.cell( -7, -1 ) );
		if (rat == null) return;
		for (int i = 0; i < 4; i++){
			final int to = s.cell( -6 + i, -1 );
			s.after( from + 0.6f * i, () -> rat.move( to ) );
		}
	}

	private static void gases( FxGallery.Page page ){
		for (Object[] g : GASES){
			final Class<?> kind = (Class<?>) g[1];
			page.add( (String) g[0], 4f, s -> {
				cloud( s, kind, 400, 120 );
				walker( s, 1f );
			} );
		}
		page.add( "a smoke bomb going off", 4.5f, s -> cloud( s, SmokeScreen.class, 1000, 400 ) );
		page.add( "goo welling out of the floor", 3f, s -> cloud( s, GooWarn.class, 50, 20 ) );
	}

	private static void cold( FxGallery.Page page ){
		page.add( "frost on the floor and on the pool", 4f, s -> {
			s.clearBlobs();
			for (int c : s.stone()) s.seed( Freezing.class, 10, c );
			int[] pool = s.pool();
			for (int i = 5; i < 10; i++) s.seed( Freezing.class, 10, pool[i] );
		} );
		page.add( "the pool freezing, then thawing", 4.5f, s -> {
			int[] pool = s.pool();
			for (int i = 0; i < pool.length; i += 2) setTerrain( pool[i], Terrain.FROZEN_WATER );
			s.after( 2.5f, () -> {
				for (int i = 0; i < pool.length; i += 2) setTerrain( pool[i], Terrain.WATER );
			} );
		} );
		page.add( "snow falling into the pool", 3f, s -> {
			for (int c : s.pool()) s.pour( c, SnowParticle.FACTORY, 0.3f, 2.5f );
		} );
		page.add( "an ice storm", 4f, s -> cloud( s, IceStorm.class, 200, 80 ) );
		page.add( "a blizzard", 4f, s -> cloud( s, Blizzard.class, 200, 80 ) );
		page.add( "a snowstorm", 4f, s -> cloud( s, Snowstorm.class, 200, 80 ) );
		page.add( "a whirlwind of snow", 4f, s -> cloud( s, Snowwhirlwind.class, 200, 80 ) );
		page.add( "a rat frozen and thawing", 3f, s -> {
			if (s.dryRat.sprite == null) return;
			IceBlock ice = IceBlock.freeze( s.dryRat.sprite );
			s.after( 2f, ice::melt );
		} );
	}

	private static void dark( FxGallery.Page page ){
		page.add( "a curse closing on the skeleton", 2.5f, s -> s.burstCenter( s.skeleton.pos, ShadowParticle.CURSE, 12 ) );
		page.add( "shadow rising off the rat", 2.5f, s -> {
			if (s.dryRat.sprite != null) s.dryRat.sprite.emitter().burst( ShadowParticle.UP, 10 );
		} );
		page.add( "a shadow bolt", 2.5f, s -> {
			if (s.dryRat.sprite == null || s.skeleton.sprite == null) return;
			MagicMissile.boltFromChar( s.dryRat.sprite.parent, MagicMissile.SHADOW, s.dryRat.sprite, s.skeleton.pos, null );
		} );
		page.add( "poison and acid splashes", 2.5f, s -> {
			s.burstCenter( s.stone()[1], PoisonParticle.SPLASH, 12 );
			s.burstCenter( s.stone()[2], CorrosionParticle.SPLASH, 12 );
			s.burstCenter( s.poolMiddle(), PoisonParticle.SPLASH, 8 );
		} );
	}

	private static void earth( FxGallery.Page page ){
		page.add( "earth thrown up, and a door's dust", 3f, s -> {
			s.burst( s.stone()[3], EarthParticle.FACTORY, 10 );
			for (int i = 0; i < 3; i++) s.after( 0.5f * i, () -> s.burstCenter( s.stone()[5], EarthParticle.SMALL, 1 ) );
		} );
		page.add( "rocks trickling down", 3f, s -> s.burst( s.stone()[0], EarthParticle.FALLING, 4 ) );
		page.add( "a pit opening: the warning, then the fall", 3.5f, s -> {
			int[] p = patch( s );
			for (int c : p) s.burst( c, PitfallParticle.FACTORY4, 8 );
			s.after( 1.2f, () -> {
				for (int c : p) s.burst( c, PitfallParticle.FACTORY8, 12 );
			} );
		} );
		page.add( "a web shaking as a rat walks through it", 4f, s -> {
			s.clearBlobs();
			for (int i = 0; i < 3; i++) s.seed( Web.class, 20, s.cell( -6 + i, -1 ) );
			walker( s, 1f );
		} );
		page.add( "a pod popping", 2f, s -> s.burstCenter( s.stone()[0], PoppolarExplosionParticle.FACTORY, 60 ) );
		page.add( "the skeleton key's wall", 3.5f, s -> {
			s.clearBlobs();
			for (int c : s.stone()) s.seed( SkeletonKey.KeyWall.class, 20, c );
		} );
	}

	//a cell's terrain set as the game sets it, the map redrawn there (the terrain watch hears it)
	private static void setTerrain( int cell, int terrain ){
		if (cell == -1) return;
		Level.set( cell, terrain );
		GameScene.updateMap( cell );
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		String part = focus;
		focus = null;
		if (part == null || part.equals( "gases" )) gases( page );
		if (part == null || part.equals( "cold" )) cold( page );
		if (part == null || part.equals( "dark" )) dark( page );
		if (part == null || part.equals( "earth" )) earth( page );
	}

	// ------------------------------------------------------------------ scenes

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		out.add( new Part( "fx-gases", "Effects: every gas, a rat walking through each, day and night", "gases" ) );
		out.add( new Part( "fx-cold", "Effects: frost on the floor and the pool, freezing and thawing, storms", "cold" ) );
		out.add( new Part( "fx-dark", "Effects: curses, shadow and its bolts, poison and acid", "dark" ) );
	}

	//a part of this area's gallery page, played by day and at night over and over: the area's own
	//gallery scene, its page built from that part alone
	private final class Part implements DebugScenes.Scene {
		final String id, title, part;

		Part( String id, String title, String part ){
			this.id = id;
			this.title = title;
			this.part = part;
		}

		@Override
		public String id(){
			return id;
		}

		@Override
		public String title(){
			return title;
		}

		@Override
		public void apply( Hero hero ){
			String gallery = "fx-gallery-" + key();
			for (DebugScenes.Scene s : FxGallery.SCENES){
				if (s.id().equals( gallery )){
					focus = part;
					s.apply( hero );
					return;
				}
			}
		}
	}
}

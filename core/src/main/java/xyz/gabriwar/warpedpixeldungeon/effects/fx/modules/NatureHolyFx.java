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
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Foliage;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.GlowingGas;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Portal;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Regrowth;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Sunlight;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.WaterOfHealth;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.HallowedGround;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.spells.WallOfLight;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.BlobFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.GrowthPulse;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HealFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HolyFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.NatureScenes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.PlantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.SeasonSweep;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Ticker;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.Trample;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FireflyParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.GatheringFx;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfRegrowth;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.plants.Sungrass;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.OverworldDress;
import com.watabou.utils.PointF;

import java.util.List;

/**
 * Nature, life and the holy (the nature-life-holy area: docs/fx/nature-life-holy.md): grass and
 * leaves, growth, plants, sunlight and foliage, healing, holy light, fireflies, the aurora and the
 * rainbow. Its pictures are in effects/fx/nature; here it wires them to the scene (its clock, the
 * terrain's changes: grass trampled and grass growing) and fills its gallery page and scenes.
 */
public class NatureHolyFx implements FxModule {

	@Override
	public String key(){
		return "nature-life-holy";
	}

	@Override
	public String title(){
		return "Nature, life and the holy";
	}

	//the area's clock: the growths waiting for their wave, the plants at rest
	@Override
	public void sceneCreated( GameScene scene ){
		GrowthPulse.reset();
		HealFx.reset();
		scene.add( new Ticker() );
	}

	//high grass trampled, grass growing: as the render side sees the terrain change (a guest too)
	@Override
	public void terrainChanged( int cell, int oldTerrain, int newTerrain ){
		if (Trample.trampled( oldTerrain, newTerrain )) Trample.at( cell );
		else GrowthPulse.changed( cell, oldTerrain, newTerrain );
	}

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		NatureScenes.add( out );
	}

	//a cell's terrain set on the stage (it is carved again at the next page)
	private static void grow( int cell, int terrain ){
		if (cell == -1 || Dungeon.level.map[cell] == Terrain.WALL) return;
		Level.set( cell, terrain );
		GameScene.updateMap( cell );
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		page.add( "a rat tramples the high grass", 3.5f, s -> {
			int[] g = s.grass();
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( -2, -3 ) );
			for (int i = 0; i < 3; i++){
				final int to = g[i];
				s.after( 0.2f + 0.5f * i, () -> p.move( to ) );
				s.after( 0.3f + 0.5f * i, () -> {
					grow( to, Terrain.GRASS );
					s.burst( to, LeafParticle.LEVEL_SPECIFIC, 2 );
				} );
			}
		} );
		page.add( "a wand of regrowth +3: the grass races down the cone", 3.5f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( -1, 1 ) );
			final int from = s.cell( -1, 1 );
			s.after( 0.3f, () -> MagicMissile.boltFromChar( p.sprite.parent, MagicMissile.FOLIAGE_CONE, p.sprite, s.cell( -1, 5 ), () -> {
				GrowthPulse.waveFrom( from );
				for (int dy = 2; dy <= 5; dy++){
					for (int dx = -1 - (dy - 1) / 2; dx <= -1 + (dy - 1) / 2; dx++){
						grow( s.cell( dx, dy ), dy >= 4 ? Terrain.HIGH_GRASS : Terrain.GRASS );
					}
				}
			} ) );
		} );
		page.add( "a potion of regrowth's pool", 3.5f, s -> {
			final int middle = s.cell( -5, 3 );
			final int[] cells = { s.cell( -6, 2 ), s.cell( -5, 2 ), s.cell( -6, 3 ), middle, s.cell( -6, 4 ), s.cell( -5, 4 ) };
			s.seed( Regrowth.class, 40, cells );
			GrowthPulse.waveFrom( middle );
			s.after( 0.2f, () -> {
				for (int c : cells) grow( c, c == middle ? Terrain.HIGH_GRASS : Terrain.GRASS );
			} );
		} );
		page.add( "foliage: shafts, flecks and fluff", 4f, s -> s.seed( Foliage.class, 20, s.stone() ) );
		page.add( "a drink from the well of health", 3f, s -> {
			final int well = s.cell( -2, -1 );
			s.seed( WaterOfHealth.class, 1, well );
			FxStage.Puppet p = s.puppet( Rat.class, well );
			s.after( 1f, () -> {
				HealFx.drink( p.ch, well );
				p.sprite.showStatusWithIcon( CharSprite.POSITIVE, "30", FloatingText.HEALING );
			} );
		} );
		page.add( "healing and shielding ticks", 3f, s -> {
			for (int i = 0; i < 3; i++){
				s.after( 0.2f + 0.8f * i, () -> {
					if (s.dryRat.sprite != null) s.dryRat.sprite.showStatusWithIcon( CharSprite.POSITIVE, "8", FloatingText.HEALING );
				} );
			}
			s.after( 1.4f, () -> {
				if (s.skeleton.sprite != null) s.skeleton.sprite.showStatusWithIcon( CharSprite.POSITIVE, "10", FloatingText.SHIELDING );
			} );
		} );
		page.add( "a sunray, then a pillar of light", 3.5f, s -> {
			FxStage.Puppet p = s.puppet( Rat.class, s.cell( -2, 3 ) );
			s.after( 0.3f, () -> {
				if (s.skeleton.sprite == null) return;
				p.sprite.parent.add( new Beam.SunRay( mid( p.sprite ), mid( s.skeleton.sprite ) ) );
				s.skeleton.sprite.burst( 0xFFFFFF44, 5 );
				HolyFx.sunMotes( s.skeleton.sprite );
			} );
			s.after( 1.6f, () -> PillarRiseFX.show( s.cell( 0, 3 ), 0xFFEE88, null ) );
		} );
		page.add( "bless: a star where the flare was", 2.5f, s -> {
			if (s.dryRat.sprite == null) return;
			HolyFx.star( s.dryRat.sprite, HolyFx.BLESS, false, 8 );
			s.dryRat.sprite.showStatusWithIcon( CharSprite.POSITIVE, "15", FloatingText.SHIELDING );
		} );
		page.add( "a wall of light", 3.5f, s -> s.seed( WallOfLight.LightWall.class, 10, s.cell( -3, -2 ), s.cell( -3, -1 ), s.cell( -3, 0 ) ) );
		page.add( "hallowed ground and divine wrath's", 3.5f, s -> {
			int[] st = s.stone();
			s.seed( HallowedGround.HallowedTerrain.class, 10, st[0], st[1], st[2] );
			for (int i = 3; i < 6; i++) if (st[i] != -1) CellEmitter.get( st[i] ).start( BlobFx.WRATH, 0.3f, 10 );
		} );
		page.add( "glowing gas, sunlight and a portal", 4f, s -> {
			s.seed( GlowingGas.class, 30, s.cell( -6, -1 ), s.cell( -5, -1 ), s.cell( -6, 0 ) );
			s.seed( Sunlight.class, 10, s.cell( -1, 2 ), s.cell( 0, 2 ) );
			s.seed( Portal.class, 1, s.cell( -2, -3 ) );
		} );
		page.add( "fireflies over the pool", 4f, s -> {
			for (int i = 0; i < 3; i++) s.pour( s.cell( 2 + 2 * i, 0 ), FireflyParticle.FACTORY, 0.6f, 3.5f );
		} );
		page.add( "leaves landing on the pool", 3f, s -> {
			for (int i = 0; i < 4; i++) s.burst( s.cell( 2 + i, -1 + i % 2 ), LeafParticle.GENERAL, 3 );
		} );
		page.add( "a seed coming up, trampled and withering", 4f, s -> {
			final int c = s.cell( -1, 2 );
			if (c == -1) return;
			Dungeon.level.plant( new Sungrass.Seed(), c );
			s.after( 1.4f, () -> {
				CellEmitter.get( c ).start( PlantFx.SUNGRASS_SHAFT, 0.2f, 3 );
				PlantFx.sungrass( c );
			} );
			s.after( 2.6f, () -> {
				Plant plant = Dungeon.level.plants.get( c );
				if (plant == null) return;
				PlantFx.wither( plant, c );
				s.burst( c, LeafParticle.GENERAL, 6 );
				Dungeon.level.uproot( c );
			} );
		} );
		page.add( "earthroot's bark, blindweed's glare, fadeleaf, lavenderlantern", 4.5f, s -> {
			s.after( 0.2f, () -> PlantFx.bark( s.dryRat.pos ) );
			s.after( 1.2f, () -> PlantFx.blind( s.skeleton.pos ) );
			s.after( 2.2f, () -> PlantFx.fade( s.cell( -1, 1 ), s.cell( -4, 1 ) ) );
			s.after( 3.2f, () -> PlantFx.lantern( s.cell( 1, 1 ) ) );
		} );
		page.add( "the avatar's halo and the guiding light's mark", 3.5f, s -> {
			if (s.dryRat.sprite == null || s.skeleton.sprite == null) return;
			final HolyFx.Halo h = HolyFx.halo( s.dryRat.sprite, 1 );
			HolyFx.mark( s.skeleton.sprite, true );
			int r = s.dryRat.pos, w = Dungeon.level.width();
			int[] edge = { r - w - 1, r - w, r - w + 1, r - 1, r + 1, r + w - 1, r + w, r + w + 1 };
			HolyFx.sparkles( edge, edge.length );
			s.after( 3.2f, () -> {
				h.out();
				HolyFx.mark( s.skeleton.sprite, false );
			} );
		} );
		page.add( "a lotus opening", 3f, s -> s.puppet( WandOfRegrowth.Lotus.class, s.cell( -2, 3 ) ) );
		page.add( "spirits rising, holy water on the undead", 3f, s -> {
			if (s.skeleton.sprite == null) return;
			s.skeleton.sprite.emitter().start( HolyFx.SPIRIT_WISP, 0.3f, 3 );
			final int k = s.skeleton.pos;
			s.after( 1.2f, () -> HolyFx.holyWater( k, new int[]{ k, k - 1, k + 1, s.cell( 1, 2 ) }, 4, new int[]{ k }, 1 ) );
		} );
		page.add( "glints over a form, a shield of light, cleanse", 3f, s -> {
			if (s.dryRat.sprite == null) return;
			HolyFx.glintSweep( s.dryRat.sprite );
			s.after( 0.8f, () -> HolyFx.bubble( s.dryRat.sprite, 0xFFE9A8, 0.3f, 0.6f, 4 ) );
			s.after( 1.6f, () -> {
				HolyFx.star( s.dryRat.sprite, HolyFx.CLEANSE, false, 0 );
				HolyFx.wisps( s.dryRat.sprite, 6 );
			} );
		} );
		page.add( "a change of season over the grass", 3f, s ->
				SeasonSweep.sweep( s.origin, s.grass(), SeasonSweep.color( GameCalendar.season() ) ) );
		//the homesteads' felling (homestead/gathering/GatheringFx): the strokes, then the tree comes down
		page.add( "an axe on an oak, and it falls east; a pine falls south", 4f, s -> {
			final int oak = s.cell( -4, 1 ), pine = s.cell( -2, -1 );
			for (int i = 0; i < 3; i++){
				s.after( 0.1f + 0.45f * i, () -> GatheringFx.play( GatheringFx.CHOP, oak, 0, 0 ) );
			}
			s.after( 1.5f, () -> GatheringFx.play( GatheringFx.TREE_FALLS, oak, GatheringFx.direction( 1, 0, true ),
					GatheringFx.picture( OverworldDress.OAK_SUMMER_TRUNK, OverworldDress.OAK_SUMMER_TOP ) ) );
			s.after( 2.2f, () -> GatheringFx.play( GatheringFx.TREE_FALLS, pine, GatheringFx.direction( 0, 1, true ),
					GatheringFx.picture( OverworldDress.SUMMER_PINES[0], -1 ) ) );
		} );
		//the climate's own events, switched on and off again (the scene shows what the climate says)
		page.add( "the aurora", 5f, s -> {
			if (!ClimateManager.isAuroraActive()) ClimateManager.debugToggleAurora();
			s.after( 4.5f, () -> {
				if (ClimateManager.isAuroraActive()) ClimateManager.debugToggleAurora();
			} );
		} );
		page.add( "the rainbow", 5f, s -> {
			if (!ClimateManager.isRainbowActive()) ClimateManager.debugToggleRainbow();
			s.after( 4.5f, () -> {
				if (ClimateManager.isRainbowActive()) ClimateManager.debugToggleRainbow();
			} );
		} );
	}

	private static PointF mid( CharSprite s ){
		return new PointF( s.x + s.width() / 2f, s.y + s.height() / 2f );
	}
}

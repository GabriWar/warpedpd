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

package xyz.gabriwar.warpedpixeldungeon.homestead.pieces;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.CookingPot;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Forge;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Hearth;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Kiln;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.MasonsBench;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Quern;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.TanningRack;
import xyz.gabriwar.warpedpixeldungeon.homestead.stations.Workbench;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTileSheet;
import com.watabou.utils.Reflection;

/**
 * The stations (BuildCatalog.PROVIDERS' StationPieces, the build menu's Stations group): the
 * workbench, the mason's bench, the kiln, the hearth (of brick or of stone), the cooking pot, the
 * tanning rack, the forge and the quern, each an actor (homestead/stations), ten turns to build;
 * the wooden ones burn. And the two the hero once bought with the builder's tool: the alchemy pot
 * (the ALCHEMY terrain, which Hero.actAlchemy works at as anywhere) and the enchanting pedestal
 * (an EnchantingStation on the PEDESTAL terrain), each paid in gold at the town's rate
 * (BuildPiece.Cost.amount, TownLedger.scaled) as well as materials, and never hurt by anything.
 */
public class StationPieces implements BuildCatalog.Provider {

	/** The base gold prices of the alchemy pot and the enchanting pedestal (before the town's rate). */
	public static final int ALCHEMY_GOLD = 800, PEDESTAL_GOLD = 1500;
	/** Turns to build a station. */
	public static final int TURNS = 10;

	@Override
	public void register(){
		BuildCatalog.register( BuildPiece.actor( "workbench", BuildCategory.STATIONS, Workbench::new, HomesteadSprites.WORKBENCH )
				.cost( Plank.class, 6 ).turns( TURNS ).hp( 40 ).flammable().done() );
		BuildCatalog.register( BuildPiece.actor( "masons_bench", BuildCategory.STATIONS, MasonsBench::new, HomesteadSprites.MASONS_BENCH )
				.cost( RoughStone.class, 4 ).cost( Plank.class, 2 ).turns( TURNS ).hp( 80 ).done() );
		BuildCatalog.register( BuildPiece.actor( "kiln", BuildCategory.STATIONS, Kiln::new, HomesteadSprites.KILN )
				.cost( RoughStone.class, 8 ).cost( Clay.class, 4 ).turns( TURNS ).hp( 120 ).done() );
		BuildCatalog.register( BuildPiece.actor( "hearth_brick", BuildCategory.STATIONS, Hearth::new, HomesteadSprites.HEARTH )
				.cost( FiredBrick.class, 6 ).turns( TURNS ).hp( 120 ).done() );
		BuildCatalog.register( BuildPiece.actor( "hearth_stone", BuildCategory.STATIONS, Hearth::new, HomesteadSprites.HEARTH )
				.cost( RoughStone.class, 8 ).turns( TURNS ).hp( 120 ).done() );
		BuildCatalog.register( BuildPiece.actor( "cooking_pot", BuildCategory.STATIONS, CookingPot::new, HomesteadSprites.COOKING_POT )
				.cost( IronFittings.class, 2 ).cost( FiredBrick.class, 2 ).turns( TURNS ).hp( 60 ).done() );
		BuildCatalog.register( BuildPiece.actor( "tanning_rack", BuildCategory.STATIONS, TanningRack::new, HomesteadSprites.TANNING_RACK )
				.cost( Plank.class, 4 ).cost( Rope.class, 2 ).turns( TURNS ).hp( 30 ).flammable().done() );
		BuildCatalog.register( BuildPiece.actor( "forge", BuildCategory.STATIONS, Forge::new, HomesteadSprites.FORGE )
				.cost( FiredBrick.class, 10 ).cost( IronFittings.class, 4 ).turns( TURNS ).hp( 160 ).done() );
		BuildCatalog.register( BuildPiece.actor( "quern", BuildCategory.STATIONS, Quern::new, HomesteadSprites.QUERN )
				.cost( CutStone.class, 4 ).turns( TURNS ).hp( 100 ).done() );

		//bought with gold as well: nothing hurts them (no health), and no gold comes back
		BuildCatalog.register( BuildPiece.terrain( "alchemy_pot", BuildCategory.STATIONS, Terrain.ALCHEMY )
				.cost( Gold.class, ALCHEMY_GOLD ).cost( FiredBrick.class, 4 ).turns( TURNS ).done() );
		BuildCatalog.register( BuildPiece.actor( "enchanting_pedestal", BuildCategory.STATIONS, EnchantingStation::new, 0 )
				.preview( Assets.Environment.TILES_CITY, DungeonTileSheet.PEDESTAL )
				.cost( Gold.class, PEDESTAL_GOLD ).cost( CutStone.class, 4 ).turns( TURNS ).listener( PEDESTAL ).done() );
	}

	/**
	 * The enchanting pedestal's stone (the PEDESTAL terrain) under its station, as the builder's
	 * tool laid it: laid with the station and taken away with it, the land's own ground back. A
	 * built floor or path it is laid over is taken up first, half of it given back, as a terrain
	 * piece would take it up (Homestead.place).
	 */
	static final BuildPiece.Listener PEDESTAL = new BuildPiece.Listener(){
		@Override
		public void placed( OverworldLevel level, int cell, Hero builder ){
			int t = level.map[cell];
			if (builder != null && (BuiltTerrain.floor( t ) || t == Terrain.PATH_STONE)){
				BuildPiece floor = BuildCatalog.forTerrain( t );
				if (floor != null){
					for (BuildPiece.Cost c : Homestead.refundOf( floor )){
						Item back = Reflection.newInstance( c.material );
						if (back == null) continue;
						back.quantity( c.quantity );
						if (!back.collect( builder.belongings.backpack )){
							Heap h = level.drop( back, builder.pos );
							if (h.sprite != null) h.sprite.drop();
						}
					}
				}
			}
			Level.set( cell, Terrain.PEDESTAL, level );
			Homestead.refresh( level, cell );
		}

		@Override
		public void removed( OverworldLevel level, int cell ){
			if (level.map[cell] != Terrain.PEDESTAL) return;
			Level.set( cell, Homestead.groundUnder( level, cell ), level );
			Homestead.refresh( level, cell );
		}
	};
}

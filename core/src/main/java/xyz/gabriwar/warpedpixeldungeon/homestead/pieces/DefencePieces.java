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
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCatalog;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildCategory;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuildPiece;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.AlarmBell;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.SpikeTrap;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.ThreatSlice;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.Watchtower;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;

/**
 * The threats' pieces, in the build menu's DEFENCE group after the core palisade: the watchtower
 * (Watchtower), the alarm bell (AlarmBell) and the spike trap (SpikeTrap, laid on walkable ground:
 * its terrain is the spent trap's open floor, the trap itself the slice's, ThreatSlice).
 */
public class DefencePieces implements BuildCatalog.Provider {

	@Override
	public void register(){
		BuildCatalog.register( BuildPiece.actor( "watchtower", BuildCategory.DEFENCE, Watchtower::new, HomesteadSprites.WATCHTOWER )
				.cost( Plank.class, 8 ).cost( Log.class, 4 ).cost( Rope.class, 2 ).turns( 12 ).hp( 80 ).flammable().done() );

		BuildCatalog.register( BuildPiece.actor( "alarm_bell", BuildCategory.DEFENCE, AlarmBell::new, HomesteadSprites.ALARM_BELL )
				.cost( IronFittings.class, 2 ).cost( Plank.class, 2 ).cost( Rope.class, 1 ).turns( 5 ).hp( 40 ).done() );

		BuildCatalog.register( BuildPiece.terrain( ThreatSlice.SPIKE_PIECE, BuildCategory.DEFENCE, Terrain.INACTIVE_TRAP )
				.cost( IronFittings.class, 2 ).cost( Plank.class, 1 ).turns( 3 ).line()
				.preview( Assets.Environment.TERRAIN_FEATURES, SpikeTrap.ARMED )
				.listener( new BuildPiece.Listener(){
					@Override
					public void placed( OverworldLevel level, int cell, Hero builder ){
						ThreatSlice slice = ThreatSlice.of( level );
						if (slice != null) slice.lay( level, cell );
					}

					@Override
					public void removed( OverworldLevel level, int cell ){
						ThreatSlice slice = ThreatSlice.of( level );
						if (slice != null) slice.lift( level, cell );
					}
				} ).done() );
	}
}

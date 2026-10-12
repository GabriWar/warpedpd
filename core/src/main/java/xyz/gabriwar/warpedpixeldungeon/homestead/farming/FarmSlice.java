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

package xyz.gabriwar.warpedpixeldungeon.homestead.farming;

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.FarmCrop;
import xyz.gabriwar.warpedpixeldungeon.homestead.BuiltTerrain;
import xyz.gabriwar.warpedpixeldungeon.homestead.StoreSection;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.SectorMap;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A world slice's fields (HomesteadStore's farming section): every crop in the ground, by world
 * cell. The record is the crop; the FarmCrop on its cell is laid back on every new window
 * (materialize) or taken over from the one the window brought back (a save's, a parked one), and
 * one standing for no record leaves (Farming.owns). Pieces going up and coming down water or dry
 * the farmland round them, and a crop whose farmland is taken from under it is uprooted.
 * At night its world turn sends the wild grazers after the open fields (Pests).
 */
public class FarmSlice extends StoreSection {

	private final SectorMap<Crop> crops = new SectorMap<>();
	//when the night last sent the grazers out (Pests.watch, the game's clock): not saved
	float pestsSent = Float.NEGATIVE_INFINITY;

	/** The crop on a world cell, or null. */
	public synchronized Crop at( int wx, int wy ){
		return crops.get( OverworldLevel.worldKey( wx, wy ) );
	}

	synchronized void put( Crop c ){
		crops.put( OverworldLevel.worldKey( c.wx, c.wy ), c );
	}

	synchronized void remove( int wx, int wy ){
		crops.remove( OverworldLevel.worldKey( wx, wy ) );
	}

	public synchronized int size(){
		return crops.size();
	}

	/** Every crop in the world rect [x0, x1) x [y0, y1), a copy of the list (the caller may take
	 *  crops out as it goes). */
	public synchronized ArrayList<Crop> in( int x0, int y0, int x1, int y1 ){
		ArrayList<Crop> out = new ArrayList<>();
		crops.forEachIn( x0, y0, x1, y1, (x, y, c) -> out.add( c ) );
		return out;
	}

	/** Every crop in the window. */
	public ArrayList<Crop> inWindow( OverworldLevel ow ){
		return in( ow.worldX, ow.worldY, ow.worldX + ow.width(), ow.worldY + ow.height() );
	}

	@Override
	public void materialize( OverworldLevel ow ){
		for (Crop c : inWindow( ow )){
			int cell = ow.localCell( c.wx, c.wy );
			if (cell == -1) continue;
			//its ground was taken while it lay out of the window (nothing but the facade should, but
			//a crop never floats on stone)
			if (!BuiltTerrain.farmland( ow.map[cell] )){
				remove( c.wx, c.wy );
				continue;
			}
			Mob here = null;
			for (Mob m : ow.mobs){
				if (m.pos == cell){
					here = m;
					break;
				}
			}
			if (here instanceof FarmCrop && Farming.same( ((FarmCrop) here).crop, c )){
				((FarmCrop) here).crop = c;
			} else if (here == null){
				Farming.show( ow, cell, c );
			}
			//anyone else on its cell: the crop is laid when the cell is free, the next window
		}
	}

	@Override
	public void cellChanged( OverworldLevel ow, int cell ){
		Farming.water( ow, cell );
		Crop c = Farming.cropAt( ow, cell );
		if (c != null && !BuiltTerrain.farmland( ow.map[cell] )){
			Farming.dies( ow, cell, c, Farming.Outcome.UPROOTED, true );
		}
	}

	@Override
	public void worldTurn( OverworldLevel ow ){
		Pests.watch( ow );
	}

	private static final String CROPS = "crops";

	@Override
	public synchronized void storeInBundle( Bundle bundle ){
		bundle.put( CROPS, new ArrayList<>( crops.values() ) );
	}

	@Override
	public synchronized void restoreFromBundle( Bundle bundle ){
		crops.clear();
		if (!bundle.contains( CROPS )) return;
		for (Bundlable b : bundle.getCollection( CROPS )){
			if (b instanceof Crop && ((Crop) b).seed != null) put( (Crop) b );
		}
	}
}

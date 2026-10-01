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

package xyz.gabriwar.warpedpixeldungeon.levels.features;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.SparseArray;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A monster that survived a fall into a chasm, waiting for the level below to be
 * shown. On the world's slices it lands on the same world cell it fell from; in the
 * dungeon anywhere open on the floor below, like a falling hero.
 */
public class FallenMob implements Bundlable {

	public Mob mob;
	//the world cell it fell from, when it fell off one of the world's slices
	public boolean world;
	public int wx, wy;

	public FallenMob(){}

	public FallenMob( Mob mob ){
		this.mob = mob;
	}

	/** Every fallen monster headed for this level is put down on it (Dungeon.switchLevel). */
	public static void land( Level level ){
		ArrayList<FallenMob> fallen = Dungeon.fallenMobs.get( Dungeon.depth );
		if (fallen == null || Dungeon.branch != 0) return;
		Iterator<FallenMob> it = fallen.iterator();
		while (it.hasNext()){
			FallenMob f = it.next();
			if (f.world && level instanceof OverworldLevel){
				((OverworldLevel) level).receiveFallen( f.mob, f.wx, f.wy );
				it.remove();
				continue;
			}
			int cell = level.randomRespawnCell( f.mob );
			//nowhere open to land yet: it waits for the next visit
			if (cell == -1) continue;
			f.mob.pos = cell;
			level.mobs.add( f.mob );
			it.remove();
		}
		if (fallen.isEmpty()) Dungeon.fallenMobs.remove( Dungeon.depth );
	}

	/** Saves every waiting fallen monster, per destination depth. */
	public static void store( Bundle bundle, SparseArray<ArrayList<FallenMob>> all ){
		int[] depths = all.keyArray();
		bundle.put( DEPTHS, depths );
		for (int d : depths){
			bundle.put( FALLEN + d, all.get( d ) );
		}
	}

	public static SparseArray<ArrayList<FallenMob>> restore( Bundle bundle ){
		SparseArray<ArrayList<FallenMob>> all = new SparseArray<>();
		if (!bundle.contains( DEPTHS )) return all;
		for (int d : bundle.getIntArray( DEPTHS )){
			ArrayList<FallenMob> list = new ArrayList<>();
			for (Bundlable b : bundle.getCollection( FALLEN + d )){
				list.add( (FallenMob) b );
			}
			if (!list.isEmpty()) all.put( d, list );
		}
		return all;
	}

	private static final String DEPTHS = "fallen_depths";
	private static final String FALLEN = "fallen_";
	private static final String MOB = "mob";
	private static final String WORLD = "world";
	private static final String WX = "wx";
	private static final String WY = "wy";

	@Override
	public void storeInBundle( Bundle bundle ){
		bundle.put( MOB, mob );
		bundle.put( WORLD, world );
		bundle.put( WX, wx );
		bundle.put( WY, wy );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		mob = (Mob) bundle.get( MOB );
		world = bundle.getBoolean( WORLD );
		wx = bundle.getInt( WX );
		wy = bundle.getInt( WY );
	}
}

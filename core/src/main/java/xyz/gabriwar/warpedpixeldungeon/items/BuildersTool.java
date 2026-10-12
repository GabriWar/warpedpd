/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.homestead.Homestead;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildPlacer;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildRecent;
import xyz.gabriwar.warpedpixeldungeon.homestead.building.BuildSelection;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBuild;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * The Builder's Tool: everything the hero builds out in the open world goes up through it (the
 * homesteads, homestead/Homestead). BUILD opens the build menu (WndBuild), whose pieces are placed
 * from the build bar (homestead/building/BuildPlacer); DEMOLISH, REPAIR and INSPECT go straight to the
 * bar in that mode. It works on the world's slices only (the surface, the mountains, the caves),
 * and only for the host in a shared game.
 *
 * It keeps the run's last pieces built (the menu's "Recent" group, BuildRecent) and saves them
 * with itself.
 */
public class BuildersTool extends Item {

	public static final String AC_BUILD    = "BUILD";
	public static final String AC_DEMOLISH = "DEMOLISH";
	public static final String AC_REPAIR   = "REPAIR";
	public static final String AC_INSPECT  = "INSPECT";

	{
		image = ItemSpriteSheet.DWARF_HAMMER;
		defaultAction = AC_BUILD;
		stackable = false;
		unique = true;
	}

	/** The ids of the last pieces built, newest first (BuildRecent). */
	public final ArrayList<String> recent = new ArrayList<>();

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_BUILD );
		actions.add( AC_DEMOLISH );
		actions.add( AC_REPAIR );
		actions.add( AC_INSPECT );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );

		BuildSelection.Mode mode;
		switch (action){
			case AC_BUILD:    mode = null; break;
			case AC_DEMOLISH: mode = BuildSelection.Mode.DEMOLISH; break;
			case AC_REPAIR:   mode = BuildSelection.Mode.REPAIR; break;
			case AC_INSPECT:  mode = BuildSelection.Mode.INSPECT; break;
			default: return;
		}

		//a co-op guest's hero: the host builds, the guest is told so on his own screen
		if (NetDialogs.handleNetHero( hero, Messages.get( this, "host_only" ) )) return;
		if (hero != Dungeon.hero) return;
		if (!Homestead.allowedHere( Dungeon.level )){
			GLog.w( Messages.get( this, Dungeon.level instanceof OverworldLevel ? "host_only" : "not_here" ) );
			return;
		}
		if (mode == null) GameScene.show( new WndBuild( this ) );
		else BuildPlacer.start( this, null, mode );
	}

	/** A piece was built with this tool: it heads the Recent list. */
	public void used( String pieceId ){
		BuildRecent.use( recent, pieceId );
	}

	/**
	 * Sets a cell to a terrain at once, with no cost and no checks, and redraws it and its
	 * neighbours (Homestead.refresh): what a bomb's blast uses to break the walls it reaches.
	 */
	public static void applyTerrain( int cell, int terrain ){
		applyTerrain( cell, terrain, true );
	}

	/** As applyTerrain, the builder's sound for the new tile played only `withSound`: a blast
	 *  breaking walls (Bomb) plays them breaking as one instead. */
	public static void applyTerrain( int cell, int terrain, boolean withSound ){
		Level.set( cell, terrain );
		Dungeon.level.visited[cell] = true;
		Dungeon.level.mapped[cell] = true;
		//the cell and its neighbours only, never the whole level (Homestead.refresh)
		Homestead.refresh( Dungeon.level, cell );

		if (!withSound) return;
		switch (terrain){
			case Terrain.WALL:
				SpatialSound.play( Assets.Sounds.ROCKS_LIGHT, cell );
				break;
			case Terrain.EMPTY:
				SpatialSound.play( Assets.Sounds.HIT_CRUSH, cell );
				break;
			case Terrain.WATER:
				SpatialSound.play( Assets.Sounds.WATER, cell );
				break;
			case Terrain.GRASS:
				SpatialSound.play( Assets.Sounds.GRASS, cell );
				break;
			case Terrain.DOOR:
				SpatialSound.play( Assets.Sounds.OPEN, cell );
				break;
		}
	}

	private static final String RECENT = "recent";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( RECENT, recent.toArray( new String[0] ) );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		recent.clear();
		if (bundle.contains( RECENT )) recent.addAll( Arrays.asList( bundle.getStringArray( RECENT ) ) );
	}

	private static final ItemSprite.Glowing GOLDEN = new ItemSprite.Glowing( 0xFFAA00 );

	@Override
	public ItemSprite.Glowing glowing(){
		return GOLDEN;
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	//worth something, so the town store's tap on it is a purchase and not a free pick-up
	//(Hero.handle: a ware worth nothing is taken); what a trader gives for it back
	@Override
	public int value(){
		return 30;
	}

	@Override
	public String desc(){
		return Messages.get( this, "desc", Homestead.RANGE );
	}
}

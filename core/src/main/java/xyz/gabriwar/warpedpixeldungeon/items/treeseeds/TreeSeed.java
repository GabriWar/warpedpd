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

package xyz.gabriwar.warpedpixeldungeon.items.treeseeds;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.Gathering;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.GatheringFx;
import xyz.gabriwar.warpedpixeldungeon.homestead.gathering.GatheringSlice;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * A seed the hero plants the land's own trees and bushes back with: an acorn, a pine cone, a bush
 * seed. Planted on natural ground beside him out in the world, it is a sapling (or a young bush) at
 * once, and grows like a felled tree's stump grows back (GatheringSlice): a tree after its growing
 * days of spring and summer, a bush after fewer. Felling an oak or a pine drops its seed one time in
 * two, and so does clearing a bush.
 */
public abstract class TreeSeed extends Item {

	public static final String AC_PLANT = "PLANT";

	{
		stackable = true;
		defaultAction = AC_PLANT;
		bones = false;
	}

	/** What the seed is planted as: Terrain.SAPLING_OAK, SAPLING_PINE or SAPLING_SHRUB. */
	public abstract int sapling();

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		if (Dungeon.level instanceof OverworldLevel) actions.add( AC_PLANT );
		return actions;
	}

	@Override
	public void execute( final Hero hero, String action ){
		super.execute( hero, action );
		if (!AC_PLANT.equals( action )) return;
		if (!Gathering.mayWork( hero )) return;
		GameScene.selectCell( new CellSelector.Listener(){
			@Override
			public void onSelect( Integer cell ){
				if (cell == null) return;
				String no = refusal( hero, cell );
				if (no != null){
					NetManager.heroLog( hero, GLog.WARNING + Messages.get( TreeSeed.class, no ) );
					return;
				}
				plant( hero, cell );
			}

			@Override
			public String prompt(){
				return Messages.get( TreeSeed.class, "prompt" );
			}
		} );
	}

	/** Why the seed can't go in on a cell (a message key of TreeSeed), null when it can: out in
	 *  the world, beside the hero, on natural ground nobody stands on, nothing lying there, not
	 *  frozen, and not where the world's places or villages are. */
	public static String refusal( Hero hero, int cell ){
		if (!(Dungeon.level instanceof OverworldLevel)) return "not_here";
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (cell < 0 || cell >= ow.length() || ow.isMirror()) return "not_here";
		if (!ow.adjacent( hero.pos, cell )) return "too_far";
		if (ow.protectedForBuilding( cell )) return "protected";
		if (!Gathering.earth( ow.map[cell] )) return "wrong_ground";
		if (ow.frozenAt( cell )) return "frozen";
		if (Actor.findChar( cell ) != null || ow.heaps.get( cell ) != null) return "occupied";
		return null;
	}

	/** The seed goes into the ground: its sapling there now, growing from today. */
	public void plant( Hero hero, int cell ){
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		Level.set( cell, sapling(), ow );
		GatheringSlice slice = ow.homestead().section( GatheringSlice.class );
		if (slice != null) slice.planted( ow, cell, WorldClock.day() );
		Gathering.changed( ow, cell );
		//the spadeful's chips and sound, on every screen
		GatheringFx.show( ow, GatheringFx.DIG, cell, 0, 0 );
		detach( hero.belongings.backpack );
		hero.spend( 1f );
		hero.busy();
		if (hero.sprite != null) hero.sprite.operate( cell );
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	@Override
	public int value(){
		return 5 * quantity;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.items.catching;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CatchFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * A bug net on a long handle: SWING it at a cell within REACH that the hero sees, where a small
 * critter is (CritterCatch: a butterfly or a firefly on the surface, the dungeon's moths, beetles,
 * snails, frogs and the rest), and the critter is caught into a jar (CritterJar) at its own chance,
 * or it gets away. A swing takes a turn; there is no swing at a cell with nothing on it. The first
 * few critters netted in a day in one stretch of land come easy, the next ever harder, and after
 * CritterCatch.CAP the rest keep clear of the net until the next day (CritterCatch.thinned).
 *
 * Made at a homestead's workbench from a plank and a coil of rope (CatchRecipes), and sold by the
 * villages' seed and food traders. The host's own hero's (Catching.mayUse).
 */
public class BugNet extends Item {

	public static final String AC_SWING = "SWING";

	/** How far the net reaches, cells (any way). */
	public static final int REACH = 3;

	{
		image = ItemSpriteSheet.BUG_NET;
		defaultAction = AC_SWING;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_SWING );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals( AC_SWING ) && Catching.mayUse( hero )){
			GameScene.selectCell( aim );
		}
	}

	private final CellSelector.Listener aim = new CellSelector.Listener(){
		@Override
		public void onSelect( Integer cell ){
			if (cell != null) swing( curUser, cell );
		}

		@Override
		public String prompt(){
			return Messages.get( BugNet.class, "prompt" );
		}
	};

	/** A swing at a cell (the net's aim, render thread): told at once why not when there is
	 *  nothing to swing at, else the swing, a turn, and the critter caught or gone. */
	public static void swing( final Hero hero, final int cell ){
		Level level = Dungeon.level;
		if (level == null || cell < 0 || cell >= level.length()) return;
		if (cell == hero.pos || level.distance( hero.pos, cell ) > REACH || !level.heroFOV[cell]){
			Catching.tell( hero, Messages.get( BugNet.class, "reach", REACH ) );
			return;
		}
		final CritterCatch.Target target = CritterCatch.at( cell );
		if (target == null){
			Catching.tell( hero, Messages.get( BugNet.class, "nothing" ) );
			return;
		}
		hero.busy();
		CatchFx.swing( hero, cell );
		SpatialSound.play( Assets.Sounds.MISS, hero, 0.8f, FxRandom.Float( 1.25f, 1.4f ) );
		hero.sprite.attack( cell, () -> {
			land( hero, cell, target );
			hero.spendAndNext( 1f );
		} );
	}

	//the swing comes down: caught at its chance (thinned by what this stretch of land gave today),
	//else it is off - or it already flitted off the cell
	private static void land( Hero hero, int cell, CritterCatch.Target target ){
		CatchWorld world = CatchWorld.get();
		long area = CritterCatch.areaKey( Dungeon.level, cell );
		int day = WorldClock.day();
		int netted = world == null ? 0 : world.nettedOn( area, day );
		boolean there = target.on( cell );
		CritterJar jar = new CritterJar( target.critter );
		if (there && Random.Float() < CritterCatch.thinned( target.critter.chance, netted )){
			target.take();
			if (world != null) world.netted( area, day );
			SpatialSound.play( Assets.Sounds.GRASS, cell, 0.6f, 1.3f );
			GLog.p( Messages.get( BugNet.class, "caught", jar.name() ) );
			if (!jar.collect( hero.belongings.backpack )) Dungeon.level.drop( jar, hero.pos ).sprite.drop();
			return;
		}
		target.spook( hero.sprite.center().x, hero.sprite.center().y );
		String why = !there ? "flitted" : netted >= CritterCatch.CAP ? "wary" : "missed";
		GLog.i( Messages.get( BugNet.class, why, jar.name() ) );
	}

	@Override
	public boolean collect( Bag container ){
		if (!super.collect( container )) return false;
		Catching.carried( this, container );
		return true;
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
		return 4;
	}
}

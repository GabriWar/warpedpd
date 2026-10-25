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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndBag;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A fishing rod. CAST it at open water within FishingRules.CAST_RANGE that the hero sees - a river, a
 * lake, the sea, a swamp's pool, a cave's pool, a mountain tarn; from a bank, a bridge or a boat,
 * whatever he stands on - or at the ice beside him, where he cuts a hole: a bait goes on the hook (the
 * one he chose with BAIT, else the first he carries: worms, then his jars), the line goes out
 * (FishingLine) and he rests, watching the bobber. When a fish bites he is roused; REEL within a few
 * turns lands it. Only on the world's slices: nothing in the dungeon's waters takes a hook.
 *
 * Made at a homestead's workbench from a plank, a coil of rope and iron fittings (CatchRecipes), and
 * sold by the villages' fishers (FisherTrade). The host's own hero's (Catching.mayUse).
 */
public class FishingRod extends Item {

	public static final String AC_CAST = "CAST";
	public static final String AC_REEL = "REEL";
	public static final String AC_BAIT = "BAIT";

	//the bait the angler would rather use (baitKey), null for whatever comes to hand
	private String preferred;

	{
		image = ItemSpriteSheet.FISHING_ROD;
		defaultAction = AC_CAST;
		bones = false;
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( hero.buff( FishingLine.class ) != null ? AC_REEL : AC_CAST );
		actions.add( AC_BAIT );
		return actions;
	}

	@Override
	public String defaultAction(){
		Hero hero = Dungeon.hero;
		return hero != null && hero.buff( FishingLine.class ) != null ? AC_REEL : AC_CAST;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		curUser = hero;
		if (action.equals( AC_CAST ) || action.equals( AC_REEL )){
			if (!Catching.mayUse( hero )) return;
			FishingLine line = hero.buff( FishingLine.class );
			if (line != null){
				line.reel( hero );
			} else if (action.equals( AC_CAST )){
				GameScene.selectCell( aim );
			}
		} else if (action.equals( AC_BAIT ) && Catching.mayUse( hero )){
			//the pack's window opens on the host's own screen: a guest's choice would open it there
			GameScene.selectItem( baitPicker );
		}
	}

	private final CellSelector.Listener aim = new CellSelector.Listener(){
		@Override
		public void onSelect( Integer cell ){
			if (cell != null) castLine( curUser, cell );
		}

		@Override
		public String prompt(){
			return Messages.get( FishingRod.class, "prompt" );
		}
	};

	/** Why a line cannot go into a cell now, or null when it can (the bait aside). */
	public static String refusal( Hero hero, int cell ){
		if (!(Dungeon.level instanceof OverworldLevel)) return Messages.get( FishingRod.class, "no_fish_here" );
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		if (cell < 0 || cell >= ow.length() || cell == hero.pos) return Messages.get( FishingRod.class, "not_water" );
		FishingWaters.Kind kind = FishingWaters.kindAt( ow, cell );
		if (kind == null) return Messages.get( FishingRod.class, "not_water" );
		if (kind == FishingWaters.Kind.ICE){
			if (ow.distance( hero.pos, cell ) > FishingRules.ICE_RANGE) return Messages.get( FishingRod.class, "ice_reach" );
		} else if (ow.distance( hero.pos, cell ) > FishingRules.CAST_RANGE || !ow.heroFOV[cell]){
			return Messages.get( FishingRod.class, "reach", FishingRules.CAST_RANGE );
		}
		if (Actor.findChar( cell ) != null) return Messages.get( FishingRod.class, "occupied" );
		return null;
	}

	//a cast at a cell (the rod's aim, render thread): told at once why not, else the bait on the hook,
	//the line out with a turn's swing, and the hero settling down to watch it
	private void castLine( final Hero hero, final int cell ){
		String no = refusal( hero, cell );
		if (no != null){
			Catching.tell( hero, no );
			return;
		}
		final Item bait = bait( hero );
		if (bait == null){
			Catching.tell( hero, Messages.get( FishingRod.class, "no_bait" ) );
			return;
		}
		final OverworldLevel ow = (OverworldLevel) Dungeon.level;
		final FishingWaters.Kind kind = FishingWaters.kindAt( ow, cell );
		hero.busy();
		SpatialSound.play( Assets.Sounds.MISS, hero, 0.6f, FxRandom.Float( 1.45f, 1.6f ) );
		hero.sprite.zap( cell, () -> {
			Item one = bait.detach( hero.belongings.backpack );
			Bait b = Catching.baitOf( one );
			FishingLine.cast( hero, ow, cell, kind, b != null ? b : Bait.WORM );
			GLog.i( castLog( kind, one.name() ) );
			updateQuickslot();
			//he settles down to watch the bobber: resting until it bites, or anything else rouses him
			hero.resting = true;
			hero.spendAndNext( 1f );
		} );
	}

	/** The log's line for a cast into this water with this bait on the hook (the ice's names no water). */
	static String castLog( FishingWaters.Kind kind, String bait ){
		if (kind == FishingWaters.Kind.ICE) return Messages.get( FishingRod.class, "cast_ice", bait );
		return Messages.get( FishingRod.class, "cast",
				Messages.get( FishingRod.class, "water_" + kind.name().toLowerCase( Locale.ENGLISH ) ), bait );
	}

	/** The bait a cast would put on the hook now: the one chosen, if he still has it, else the first he carries. */
	public Item bait( Hero hero ){
		Item first = null;
		for (Item i : hero.belongings.getAllItems( Item.class )){
			if (Catching.baitOf( i ) == null) continue;
			if (preferred != null && preferred.equals( baitKey( i ) )) return i;
			if (first == null || (i instanceof Earthworm && !(first instanceof Earthworm))) first = i;
		}
		return first;
	}

	//one kind of bait as the rod remembers it
	private static String baitKey( Item item ){
		return item instanceof CritterJar ? "jar_" + ((CritterJar) item).critter.name() : "worm";
	}

	private final WndBag.ItemSelector baitPicker = new WndBag.ItemSelector(){
		@Override
		public String textPrompt(){
			return Messages.get( FishingRod.class, "bait_prompt" );
		}

		@Override
		public boolean itemSelectable( Item item ){
			return Catching.baitOf( item ) != null;
		}

		@Override
		public void onSelect( Item item ){
			if (item == null) return;
			preferred = baitKey( item );
			GLog.i( Messages.get( FishingRod.class, "bait_chosen", item.name() ) );
		}
	};

	@Override
	public String desc(){
		String d = super.desc();
		Hero hero = Dungeon.hero;
		if (hero != null){
			Item b = bait( hero );
			d += "\n\n" + (b == null ? Messages.get( FishingRod.class, "bait_none" )
					: Messages.get( FishingRod.class, "bait_next", b.name() ));
		}
		return d;
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
		return 10;
	}

	private static final String PREFERRED = "preferred_bait";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		if (preferred != null) bundle.put( PREFERRED, preferred );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		preferred = bundle.contains( PREFERRED ) ? bundle.getString( PREFERRED ) : null;
	}
}

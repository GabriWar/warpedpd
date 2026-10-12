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

package xyz.gabriwar.warpedpixeldungeon.homestead.stations;

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.Torch;
import xyz.gabriwar.warpedpixeldungeon.items.food.Blandfruit;
import xyz.gabriwar.warpedpixeldungeon.items.food.Bread;
import xyz.gabriwar.warpedpixeldungeon.items.food.ChargrilledMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Cornwheatshaft;
import xyz.gabriwar.warpedpixeldungeon.items.food.Flour;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.items.food.FrozenCarpaccio;
import xyz.gabriwar.warpedpixeldungeon.items.food.MeatPie;
import xyz.gabriwar.warpedpixeldungeon.items.food.MonsterMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.Pasty;
import xyz.gabriwar.warpedpixeldungeon.items.food.PhantomMeat;
import xyz.gabriwar.warpedpixeldungeon.items.food.StewedMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Clay;
import xyz.gabriwar.warpedpixeldungeon.items.materials.CutStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.FiredBrick;
import xyz.gabriwar.warpedpixeldungeon.items.materials.GlassPane;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Hide;
import xyz.gabriwar.warpedpixeldungeon.items.materials.IronFittings;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Leather;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.PineLog;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Resin;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Rope;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Sand;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Thatch;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.potions.Potion;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.MasterworkCore;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MasonsHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WoodcuttersAxe;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * Every recipe the stations know, in each station's window order. The built-in ones are the
 * homesteads' own (below); another part adds its own with register. A recipe is checked as it is
 * registered (validate): an id of its own, a station, at least one input, every quantity at least
 * one, an output, some work, heat asked only of a station with a fire - and no recipe at any
 * station makes a masterwork core (the smith's one crucible a day, Blacksmith2, stays the only way
 * to one).
 *
 *   workbench      1 oak or pine log -> 3 planks (10 turns); 3 thatch -> 1 rope (5);
 *                  1 resin + 1 oak log -> 2 torches (5)
 *   mason's bench  2 rough stone -> 1 cut stone (10)
 *   kiln (fire)    2 clay -> 2 fired bricks (60); 2 sand -> 1 glass pane (60);
 *                  1 oak or pine log -> 2 charcoal (40)
 *   tanning rack   1 hide -> 1 leather (300)
 *   forge (fire)   1 iron ore -> 4 iron fittings (40); 2 iron fittings + 2 planks -> a woodcutter's
 *                  axe, or a mason's hammer (60 each)
 *   quern          2 cornwheat shafts -> 1 flour (20)
 *   hearth (fire)  1 flour -> 1 bread (30); 1 raw meat (mystery or monster meat, frozen
 *                  carpaccio) -> 1 grilled meat (5)
 *   cooking pot    1 mystery or monster meat -> 1 stewed meat (10); a blandfruit + a seed of the
 *                  hero's choice -> the blandfruit cooked with it (15); a pasty or phantom meat + a
 *                  ration + a piece of meat -> a meat pie (20)
 */
public final class StationRecipes {

	private StationRecipes(){}

	private static LinkedHashMap<String, StationRecipe> recipes;

	private static synchronized LinkedHashMap<String, StationRecipe> recipes(){
		if (recipes == null){
			recipes = new LinkedHashMap<>();
			builtIn();
		}
		return recipes;
	}

	/** Adds a recipe at the end of its station's list, once checked (validate). */
	public static synchronized void register( StationRecipe r ){
		validate( r );
		LinkedHashMap<String, StationRecipe> all = recipes();
		if (all.containsKey( r.id )) throw new IllegalArgumentException( "a station recipe is already called " + r.id );
		all.put( r.id, r );
	}

	/** Takes a recipe away (a test's). */
	public static synchronized void unregister( String id ){
		recipes().remove( id );
	}

	/** Throws when a recipe is malformed, or makes what no station may. */
	public static void validate( StationRecipe r ){
		if (r.id == null || r.id.isEmpty()) throw new IllegalArgumentException( "a station recipe needs an id" );
		if (r.station == null) throw new IllegalArgumentException( r.id + " has no station" );
		if (r.inputs.isEmpty()) throw new IllegalArgumentException( r.id + " takes nothing" );
		for (StationRecipe.Input in : r.inputs){
			if (in.quantity < 1) throw new IllegalArgumentException( r.id + " takes less than one of something" );
		}
		if (r.output == null || r.output.sample() == null) throw new IllegalArgumentException( r.id + " makes nothing" );
		if (r.turns < 1) throw new IllegalArgumentException( r.id + " takes no work" );
		if (r.needsHeat && !HeatStation.class.isAssignableFrom( r.station )){
			throw new IllegalArgumentException( r.id + " needs a fire its station does not have" );
		}
		if (r.output.sample() instanceof MasterworkCore){
			throw new IllegalArgumentException( r.id + ": no station makes a masterwork core" );
		}
	}

	/** Every recipe, in order. */
	public static List<StationRecipe> all(){
		return Collections.unmodifiableList( new ArrayList<>( recipes().values() ) );
	}

	/** The recipes a station makes, in order. */
	public static List<StationRecipe> forStation( HomesteadStation station ){
		ArrayList<StationRecipe> out = new ArrayList<>();
		for (StationRecipe r : recipes().values()) if (r.station.isInstance( station )) out.add( r );
		return out;
	}

	/** The recipe with that id, or null. */
	public static StationRecipe byId( String id ){
		return id == null ? null : recipes().get( id );
	}

	// ----------------------------------------------------------------- the homesteads' own

	private static void add( StationRecipe r ){
		validate( r );
		recipes.put( r.id, r );
	}

	private static void builtIn(){
		//the workbench: sawing, rope, torches
		add( StationRecipe.at( "planks_oak", Workbench.class ).in( Log.class, 1 ).out( Plank.class, 3 ).turns( 10 ).done() );
		add( StationRecipe.at( "planks_pine", Workbench.class ).in( PineLog.class, 1 ).out( Plank.class, 3 ).turns( 10 ).done() );
		add( StationRecipe.at( "rope", Workbench.class ).in( Thatch.class, 3 ).out( Rope.class, 1 ).turns( 5 ).done() );
		add( StationRecipe.at( "torches", Workbench.class ).in( Resin.class, 1 ).in( Log.class, 1 ).out( Torch.class, 2 ).turns( 5 ).done() );

		//the mason's bench
		add( StationRecipe.at( "cut_stone", MasonsBench.class ).in( RoughStone.class, 2 ).out( CutStone.class, 1 ).turns( 10 ).done() );

		//the kiln, fired
		add( StationRecipe.at( "bricks", Kiln.class ).in( Clay.class, 2 ).out( FiredBrick.class, 2 ).turns( 60 ).heat().done() );
		add( StationRecipe.at( "glass", Kiln.class ).in( Sand.class, 2 ).out( GlassPane.class, 1 ).turns( 60 ).heat().done() );
		add( StationRecipe.at( "charcoal_oak", Kiln.class ).in( Log.class, 1 ).out( Charcoal.class, 2 ).turns( 40 ).heat().done() );
		add( StationRecipe.at( "charcoal_pine", Kiln.class ).in( PineLog.class, 1 ).out( Charcoal.class, 2 ).turns( 40 ).heat().done() );

		//the tanning rack: slow
		add( StationRecipe.at( "leather", TanningRack.class ).in( Hide.class, 1 ).out( Leather.class, 1 ).turns( 300 ).done() );

		//the forge, fired: fittings, and the gathering's tools
		add( StationRecipe.at( "fittings", Forge.class ).in( IronOre.class, 1 ).out( IronFittings.class, 4 ).turns( 40 ).heat().done() );
		add( StationRecipe.at( "woodcutters_axe", Forge.class ).in( IronFittings.class, 2 ).in( Plank.class, 2 )
				.out( WoodcuttersAxe.class, 1 ).turns( 60 ).heat().done() );
		add( StationRecipe.at( "masons_hammer", Forge.class ).in( IronFittings.class, 2 ).in( Plank.class, 2 )
				.out( MasonsHammer.class, 1 ).turns( 60 ).heat().done() );

		//the quern
		add( StationRecipe.at( "flour", Quern.class ).in( Cornwheatshaft.class, 2 ).out( Flour.class, 1 ).turns( 20 ).done() );

		//the hearth, lit: baking, and grilling what Heap.burn grills
		add( StationRecipe.at( "bread", Hearth.class ).in( Flour.class, 1 ).out( Bread.class, 1 ).turns( 30 ).heat().done() );
		add( StationRecipe.at( "grilled_meat", Hearth.class )
				.inAny( 1, "raw_meat", MysteryMeat.class, MonsterMeat.class, FrozenCarpaccio.class )
				.out( ChargrilledMeat.class, 1 ).turns( 5 ).heat().done() );

		//the cooking pot: the alchemy pot's food, without its energy
		add( StationRecipe.at( "stew", CookingPot.class )
				.inAny( 1, "stew_meat", MysteryMeat.class, MonsterMeat.class )
				.out( StewedMeat.class, 1 ).turns( 10 ).done() );
		add( StationRecipe.at( "blandfruit", CookingPot.class )
				.inWhere( Blandfruit.class, 1, i -> ((Blandfruit) i).potionAttrib == null )
				.inChosen( Plant.Seed.class, 1, "any_seed", i -> Potion.SeedToPotion.types.containsKey( i.getClass() ),
						Plant.Seed.PlaceHolder.class )
				.out( COOKED_FRUIT ).named( "cooked_blandfruit" ).turns( 15 ).done() );
		add( StationRecipe.at( "meat_pie", CookingPot.class )
				.inAny( 1, "pie_crust", Pasty.class, PhantomMeat.class )
				.in( Food.class, 1 )
				.inAny( 1, "pie_meat", MysteryMeat.class, StewedMeat.class, ChargrilledMeat.class, FrozenCarpaccio.class )
				.out( MeatPie.class, 1 ).turns( 20 ).done() );
	}

	//a blandfruit cooked with the seed it used (as the alchemy pot's Blandfruit.CookFruit)
	private static final StationRecipe.Output COOKED_FRUIT = new StationRecipe.Output(){
		@Override
		public List<Item> make( List<Item> used ){
			Plant.Seed seed = null;
			for (Item i : used) if (i instanceof Plant.Seed) seed = (Plant.Seed) i;
			if (seed == null) return Collections.emptyList();
			return Collections.singletonList( new Blandfruit().cook( seed ) );
		}

		@Override
		public Item sample(){
			return new Blandfruit();
		}
	};
}

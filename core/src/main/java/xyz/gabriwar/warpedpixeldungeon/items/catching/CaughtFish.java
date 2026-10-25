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

import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.utils.Bundle;

import java.util.Locale;

/**
 * A fish fresh off the hook (FishingRod), of its species (FishSpecies): eaten raw it feeds a little
 * (its size's raw share); cooked it feeds far better and heals a little (CookedFish) - grilled on a
 * lit hearth or in any fire (Heap.burn), poached in a cooking pot (CatchRecipes). The villages'
 * fishers buy it fresh (FisherTrade). One stack per species.
 */
public class CaughtFish extends Food {

	public FishSpecies species = FishSpecies.PERCH;

	{
		energy = species.size.raw;
		image = species.image;
		bones = false;
	}

	public CaughtFish(){
	}

	public CaughtFish( FishSpecies species ){
		species( species );
	}

	public CaughtFish species( FishSpecies species ){
		this.species = species;
		energy = species.size.raw;
		image = species.image;
		return this;
	}

	@Override
	public int image(){
		return species.image;
	}

	/** Its species' name, as the catch's messages and its cooked self call it. */
	public static String speciesName( FishSpecies species ){
		return Messages.get( CaughtFish.class, species.name().toLowerCase( Locale.ENGLISH ) );
	}

	@Override
	public String name(){
		if (customName != null && !customName.isEmpty()) return customName;
		String n = speciesName( species );
		if (seed != null) return n + " " + Messages.get( Food.class, "spiced_with" ).toLowerCase( Locale.ENGLISH ) + " " + seed.name();
		return n;
	}

	@Override
	public String desc(){
		return Messages.get( CaughtFish.class, species.name().toLowerCase( Locale.ENGLISH ) + "_desc" )
				+ "\n\n" + Messages.get( CaughtFish.class, "raw" ) + spiceDescription();
	}

	/** The same fish cooked: grilled on a fire, or poached in a pot. Its spice comes with it. */
	public CookedFish cooked( boolean poached ){
		CookedFish c = new CookedFish( species, poached );
		c.quantity( quantity );
		c.seed = seed;
		return c;
	}

	@Override
	public boolean isSimilar( Item item ){
		return super.isSimilar( item ) && ((CaughtFish) item).species == species;
	}

	@Override
	public int value(){
		return species.value * quantity;
	}

	private static final String SPECIES = "species";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SPECIES, species.name() );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		species( FishSpecies.named( bundle.contains( SPECIES ) ? bundle.getString( SPECIES ) : null ) );
	}
}

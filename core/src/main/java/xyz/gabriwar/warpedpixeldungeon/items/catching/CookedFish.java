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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.food.Food;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

import java.util.Locale;

/**
 * A caught fish cooked (CaughtFish.cooked): grilled on a lit hearth or in any fire, or poached in a
 * cooking pot (a meal cooked in a kitchen keeps the hero fed longer: homestead/rooms/RoomTypes). It
 * feeds as its size does cooked (FishSpecies.Size) and, a fresh meal, heals a twentieth of the
 * hero's health, at least two, as the bridge fisherman's fish does a little more. One stack per
 * species and way of cooking.
 */
public class CookedFish extends Food {

	public FishSpecies species = FishSpecies.PERCH;
	public boolean poached = false;

	{
		image = ItemSpriteSheet.COOKED_FISH;
		energy = species.size.cooked;
		bones = false;
	}

	public CookedFish(){
	}

	public CookedFish( FishSpecies species, boolean poached ){
		this.species = species;
		this.poached = poached;
		energy = species.size.cooked;
	}

	@Override
	public String name(){
		if (customName != null && !customName.isEmpty()) return customName;
		String n = Messages.get( CookedFish.class, poached ? "poached" : "grilled", CaughtFish.speciesName( species ) );
		if (seed != null) return n + " " + Messages.get( Food.class, "spiced_with" ).toLowerCase( Locale.ENGLISH ) + " " + seed.name();
		return n;
	}

	@Override
	public String desc(){
		return Messages.get( CookedFish.class, poached ? "poached_desc" : "grilled_desc", CaughtFish.speciesName( species ) )
				+ spiceDescription() + (kitchenMade ? "\n\n" + Messages.get( Food.class, "kitchen_made" ) : "");
	}

	@Override
	protected void satisfy( Hero hero ){
		super.satisfy( hero );
		int heal = Math.max( 2, hero.HT / 20 );
		heal = Math.min( heal, hero.HT - hero.HP );
		if (heal > 0){
			hero.HP += heal;
			if (hero.sprite != null) hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );
		}
	}

	@Override
	public boolean isSimilar( Item item ){
		return super.isSimilar( item ) && ((CookedFish) item).species == species && ((CookedFish) item).poached == poached;
	}

	@Override
	public int value(){
		return (species.value + 4) * quantity;
	}

	private static final String SPECIES = "species", POACHED = "poached";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SPECIES, species.name() );
		bundle.put( POACHED, poached );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		species = FishSpecies.named( bundle.contains( SPECIES ) ? bundle.getString( SPECIES ) : null );
		poached = bundle.contains( POACHED ) && bundle.getBoolean( POACHED );
		energy = species.size.cooked;
	}
}

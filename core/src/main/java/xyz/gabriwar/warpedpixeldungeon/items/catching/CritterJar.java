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
import xyz.gabriwar.warpedpixeldungeon.effects.CatchFx;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.bags.Bag;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import com.watabou.utils.Bundle;

import java.util.ArrayList;
import java.util.Locale;

/**
 * A small critter the bug net caught (BugNet), in a jar with holes in its lid: a fishing rod's bait,
 * of its critter's kind (Critter.bait: a winged insect, a grub, a live frog, a glowing one). Jars of
 * one critter stack; a glowing one's jar glows in its colour. It can be let go again (RELEASE).
 */
public class CritterJar extends Item {

	public static final String AC_RELEASE = "RELEASE";

	public Critter critter = Critter.BEETLE;

	{
		stackable = true;
		bones = false;
		image = critter.image();
	}

	public CritterJar(){
	}

	public CritterJar( Critter critter ){
		this.critter = critter;
		image = critter.image();
	}

	@Override
	public int image(){
		return critter.image();
	}

	@Override
	public ItemSprite.Glowing glowing(){
		return critter.glow != 0 ? new ItemSprite.Glowing( critter.glow, 1.6f ) : null;
	}

	@Override
	public String name(){
		if (customName != null && !customName.isEmpty()) return customName;
		return Messages.get( CritterJar.class, key() );
	}

	@Override
	public String desc(){
		return Messages.get( CritterJar.class, key() + "_desc" ) + "\n\n"
				+ Messages.get( CritterJar.class, "bait_" + critter.bait.name().toLowerCase( Locale.ENGLISH ) );
	}

	private String key(){
		return critter.name().toLowerCase( Locale.ENGLISH );
	}

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_RELEASE );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals( AC_RELEASE )){
			Item one = detach( hero.belongings.backpack );
			if (one == null) return;
			NetManager.heroLog( hero, Messages.get( CritterJar.class, "released", name() ) );
			CatchFx.released( hero, critter );
			hero.sprite.operate( hero.pos );
			hero.spendAndNext( 1f );
		}
	}

	@Override
	public boolean collect( Bag container ){
		if (!super.collect( container )) return false;
		Catching.carried( this, container );
		return true;
	}

	@Override
	public boolean isSimilar( Item item ){
		return super.isSimilar( item ) && ((CritterJar) item).critter == critter;
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
		return critter.value * quantity;
	}

	private static final String CRITTER = "critter";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CRITTER, critter.name() );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		critter = Critter.named( bundle.contains( CRITTER ) ? bundle.getString( CRITTER ) : null );
		image = critter.image();
	}
}

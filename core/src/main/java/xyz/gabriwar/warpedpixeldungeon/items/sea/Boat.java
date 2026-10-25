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

package xyz.gabriwar.warpedpixeldungeon.items.sea;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Plank;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sea.Seafaring;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;

import java.util.ArrayList;

/**
 * A boat carried ashore (sea/Seafaring): launched from a shore beside open water, it floats where it
 * is left and the hero sails it over the water, the shallows and the deep alike. Its hull is kept in
 * the item while it is carried and on the boat while it floats; planks mend it, a few hull points
 * each, one turn a plank.
 */
public abstract class Boat extends Item {

	public static final String AC_LAUNCH = "LAUNCH";
	public static final String AC_REPAIR = "REPAIR";

	/** The two boats: what each weathers and how it sails. */
	public enum Kind {
		//a raft rides low: the rain's waves wash over it and wear it, a storm can sink it
		RAFT( 40, 8, 1f, CharSprite.State.BOAT_RAFT ),
		//a sailboat rides out the rain; only a storm wears it, and slowly. Half again as quick, twice
		//as quick before the wind
		SAILBOAT( 120, 10, 1.5f, CharSprite.State.BOAT_SAIL );

		/** Its hull's points, what one plank mends, its speed over the water. */
		public final int hull, perPlank;
		public final float speed;
		/** What its sailor's sprite wears while aboard. */
		public final CharSprite.State state;

		Kind( int hull, int perPlank, float speed, CharSprite.State state ){
			this.hull = hull;
			this.perPlank = perPlank;
			this.speed = speed;
			this.state = state;
		}

		/** A fresh boat of this kind, as an item. */
		public Boat item(){
			return this == RAFT ? new Raft() : new Sailboat();
		}
	}

	/** Its hull's points while carried. */
	public int hull;

	{
		stackable = false;
		defaultAction = AC_LAUNCH;
		unique = false;
	}

	protected Boat(){
		hull = kind().hull;
	}

	public abstract Kind kind();

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = super.actions( hero );
		actions.add( AC_LAUNCH );
		if (hull < kind().hull) actions.add( AC_REPAIR );
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute( hero, action );
		if (action.equals( AC_LAUNCH )){
			String no = Seafaring.launchRefusal( hero );
			if (no != null){
				NetManager.heroLog( hero, GLog.WARNING + no );
				return;
			}
			Seafaring.launch( hero, this );
		} else if (action.equals( AC_REPAIR )){
			repair( hero );
		}
	}

	//planks from the pack into the hull until it is whole or the planks run out, a turn each
	private void repair( Hero hero ){
		int max = kind().hull;
		if (hull >= max) return;
		int used = 0;
		while (hull < max){
			Plank p = hero.belongings.getItem( Plank.class );
			if (p == null) break;
			p.detach( hero.belongings.backpack );
			hull = Math.min( max, hull + kind().perPlank );
			used++;
		}
		if (used == 0){
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( Boat.class, "no_planks" ) );
			return;
		}
		hero.spend( used );
		hero.busy();
		if (hero.sprite != null) hero.sprite.operate( hero.pos );
		SpatialSound.play( Assets.Sounds.STURDY, hero );
		NetManager.heroLog( hero, Messages.get( Boat.class, "repaired", used, hull, max ) );
		updateQuickslot();
	}

	@Override
	public String desc(){
		return super.desc() + "\n\n" + Messages.get( Boat.class, "hull", hull, kind().hull );
	}

	@Override
	public boolean isUpgradable(){
		return false;
	}

	@Override
	public boolean isIdentified(){
		return true;
	}

	//worth less the more it needs mending
	@Override
	public int value(){
		return Math.max( 1, Math.round( worth() * (0.4f + 0.6f * hull / (float) kind().hull) ) );
	}

	/** Its worth whole. */
	protected abstract int worth();

	private static final String HULL = "hull";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( HULL, hull );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		hull = bundle.contains( HULL ) ? Math.max( 1, Math.min( kind().hull, bundle.getInt( HULL ) ) ) : kind().hull;
	}
}

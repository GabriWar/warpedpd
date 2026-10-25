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

package xyz.gabriwar.warpedpixeldungeon.sea;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.sea.Boat;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;

/**
 * A hero's boat while it floats (sea/Seafaring): its kind and hull, the world cell of the surface it
 * floats on, and whether he is aboard. One a hero: kept on him, so the boat waits where he moored it
 * through every journey below and every save. Its icon shows while he sails, its hull on it; aboard,
 * his sprite wears the boat (CharSprite.State.BOAT_RAFT, BOAT_SAIL), which reaches co-op guests by name.
 */
public class Afloat extends Buff {

	{
		type = buffType.NEUTRAL;
		revivePersists = true;
	}

	public Boat.Kind kind = Boat.Kind.RAFT;
	public int hull = Boat.Kind.RAFT.hull;
	//the boat's world cell on the surface: under the hero while he is aboard, where it lies moored otherwise
	public int wx, wy;
	public boolean aboard = false;
	//the way it last went (screen axes, -1..1 each): the sailboat's wind, the way its bow points
	public int headX = 1, headY = 0;
	//how far its hull's wear has been told: 0 none, 1 half gone, 2 three quarters gone
	public int told = 0;
	//a storm first met at sea is warned of once
	public boolean stormTold = false;

	//the look the sprite wears now, and on which sprite (a scene's new sprite wears none yet)
	private transient CharSprite.State shown;
	private transient CharSprite shownOn;

	/** Its hull's points, whole. */
	public int max(){
		return kind.hull;
	}

	/** He climbs aboard, or steps off: the look comes or goes with it. */
	public void setAboard( boolean on ){
		if (aboard == on) return;
		aboard = on;
		if (target != null && target.sprite != null) fx( true );
	}

	@Override
	public boolean act(){
		spend( TICK );
		if (target instanceof Hero) Seafaring.boatTurn( (Hero) target, this );
		return true;
	}

	@Override
	public void fx( boolean on ){
		CharSprite s = target.sprite;
		if (s == null) return;
		if (s != shownOn){
			shownOn = s;
			shown = null;
		}
		CharSprite.State want = on && aboard ? kind.state : null;
		if (want == shown) return;
		if (shown != null) s.remove( shown );
		if (want != null) s.add( want );
		shown = want;
	}

	@Override
	public int icon(){
		return aboard ? BuffIndicator.BOAT : BuffIndicator.NONE;
	}

	@Override
	public float iconFadePercent(){
		return 1f - hull / (float) max();
	}

	@Override
	public String iconTextDisplay(){
		return Integer.toString( hull );
	}

	@Override
	public String name(){
		return Messages.get( this, kind == Boat.Kind.RAFT ? "name_raft" : "name_sail" );
	}

	@Override
	public String desc(){
		return Messages.get( this, kind == Boat.Kind.RAFT ? "desc_raft" : "desc_sail", hull, max() );
	}

	private static final String KIND = "kind";
	private static final String HULL = "hull";
	private static final String WX = "wx";
	private static final String WY = "wy";
	private static final String ABOARD = "aboard";
	private static final String HEAD_X = "head_x";
	private static final String HEAD_Y = "head_y";
	private static final String TOLD = "told";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( KIND, kind.name() );
		bundle.put( HULL, hull );
		bundle.put( WX, wx );
		bundle.put( WY, wy );
		bundle.put( ABOARD, aboard );
		bundle.put( HEAD_X, headX );
		bundle.put( HEAD_Y, headY );
		bundle.put( TOLD, told );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		kind = Boat.Kind.RAFT;
		try {
			kind = Boat.Kind.valueOf( bundle.getString( KIND ) );
		} catch (IllegalArgumentException e){
			//a kind this build does not know floats as a raft
		}
		hull = Math.max( 1, Math.min( kind.hull, bundle.getInt( HULL ) ) );
		wx = bundle.getInt( WX );
		wy = bundle.getInt( WY );
		aboard = bundle.getBoolean( ABOARD );
		headX = bundle.contains( HEAD_X ) ? bundle.getInt( HEAD_X ) : 1;
		headY = bundle.contains( HEAD_Y ) ? bundle.getInt( HEAD_Y ) : 0;
		told = bundle.getInt( TOLD );
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Charcoal;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.PineLog;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

import java.util.ArrayList;

/**
 * A station with a fire in it: the kiln, the forge, the hearth. It has a FUEL SLOT the hero fills
 * with logs (oak or pine, LOG_TURNS each) and charcoal (CHARCOAL_TURNS, twice as long), up to
 * FUEL_CAP pieces, and a fire he lights and puts out by hand. Lit, it burns a piece at a time,
 * taking the next from the slot as each burns away, until the slot is empty and it goes out; and
 * only while it is lit do its recipes (all of which need heat) get anywhere. The burning goes on by
 * the global clock like the work, so a fire left lit burns its fuel while the hero is away and
 * the work it fired is done when he comes back.
 *
 * While the hero is on its slice a lit station warms its cell and the cells round it
 * (TileTemperature.depositHeat), gives the room it stands in its roomHeat (StationHeat), shows its
 * fire and light, and roars or crackles (StationSprite).
 */
public abstract class HeatStation extends HomesteadStation {

	/** Pieces of fuel the slot holds. */
	public static final int FUEL_CAP = 20;
	/** How long a log burns, and a lump of charcoal, in turns. */
	public static final float LOG_TURNS = 200f, CHARCOAL_TURNS = 400f;

	private final ArrayList<Item> fuel = new ArrayList<>();
	//what is left of the piece burning now, turns
	private float burnLeft;
	private volatile boolean lit;

	/** The heat it lays on its own cell each turn it burns, and on each open cell round it. */
	protected abstract float cellHeat();
	protected abstract float radiate();
	/** The heat it gives its room while lit, C. */
	protected abstract float litRoomHeat();

	/** How long a piece of fuel burns, 0 for anything that is not fuel. */
	public static float burnTurns( Item item ){
		if (item instanceof Charcoal) return CHARCOAL_TURNS;
		if (item instanceof Log || item instanceof PineLog) return LOG_TURNS;
		return 0f;
	}

	public static boolean isFuel( Item item ){
		return burnTurns( item ) > 0f;
	}

	public boolean lit(){
		return lit;
	}

	/** Pieces in the slot. */
	public synchronized int fuelCount(){
		int n = 0;
		for (Item i : fuel) n += i.quantity();
		return n;
	}

	/** Room left in the slot, pieces. */
	public int fuelRoom(){
		return Math.max( 0, FUEL_CAP - fuelCount() );
	}

	/** Turns of burning left: the piece alight and the slot's. */
	public synchronized float fuelTurns(){
		float t = Math.max( 0f, burnLeft );
		for (Item i : fuel) t += burnTurns( i ) * i.quantity();
		return t;
	}

	/** What is in the slot. */
	public synchronized ArrayList<Item> fuel(){
		return new ArrayList<>( fuel );
	}

	/** The hero puts fuel in from a stack he carries (as much of it as the slot has room for):
	 *  how many pieces went in; none when it no longer stands (taken down while a window was open
	 *  on it). */
	public int addFuel( Hero hero, Item stack ){
		int n;
		boolean burning;
		synchronized (this){
			n = fill( hero, stack );
			burning = lit;
		}
		//a fire that will burn on longer warms and anchors for longer (StationSlice)
		if (n > 0 && burning) syncHere();
		return n;
	}

	private int fill( Hero hero, Item stack ){
		if (!standing() || !isFuel( stack )) return 0;
		int n = Math.min( stack.quantity(), FUEL_CAP - fuelCount() );
		if (n <= 0) return 0;
		Item in;
		if (n == stack.quantity()){
			in = stack.detachAll( hero.belongings.backpack );
		} else {
			in = stack.split( n );
			stack.updateQuickslot();
		}
		if (in == null) return 0;
		for (Item f : fuel){
			if (f.isSimilar( in )){
				f.quantity( f.quantity() + in.quantity() );
				return n;
			}
		}
		fuel.add( in );
		return n;
	}

	/** The hero lights its fire: false when it burns already, has nothing to burn or no longer
	 *  stands (taken down while a window was open on it). */
	public boolean light(){
		synchronized (this){
			if (lit || !standing() || (burnLeft <= 0f && !feed())) return false;
			lit = true;
			busy = looksBusy();
		}
		syncHere();
		return true;
	}

	/** The hero puts its fire out: what is left of the piece alight keeps for next time. */
	public void putOut(){
		synchronized (this){
			if (!lit) return;
			lit = false;
			busy = looksBusy();
		}
		syncHere();
	}

	//the next piece from the slot onto the fire
	private boolean feed(){
		if (fuel.isEmpty()) return false;
		Item f = fuel.get( 0 );
		burnLeft += burnTurns( f );
		if (f.quantity() > 1) f.quantity( f.quantity() - 1 );
		else fuel.remove( 0 );
		return true;
	}

	@Override
	protected boolean canWork( StationRecipe r ){
		return r != null && (!r.needsHeat || lit);
	}

	@Override
	protected boolean looksBusy(){
		return lit;
	}

	@Override
	public float roomHeat(){
		return lit ? litRoomHeat() : 0f;
	}

	//lit, it burns on what it holds from where it last caught up, with no one to put it out: the
	//register (StationSlice) reads its fire's end off the clock, whether it acts or lies parked
	@Override
	public synchronized float givesUntil(){
		return lit ? caughtUpTo() + fuelTurns() : Float.POSITIVE_INFINITY;
	}

	@Override
	protected void advance( OverworldLevel level, float dt, float speed ){
		float t = dt;
		while (lit && t > 0f){
			if (burnLeft <= 0f && !feed()){
				goOut( level );
				break;
			}
			float burn = Math.min( t, burnLeft );
			progress( level, burn, true, speed );
			burnLeft -= burn;
			t -= burn;
		}
		//its last piece burnt away with the time, and nothing left to put on
		if (lit && burnLeft <= 0f && fuel.isEmpty()) goOut( level );
		if (t > 0f) progress( level, t, false, speed );
	}

	private void goOut( OverworldLevel level ){
		lit = false;
		burnLeft = 0f;
		if (seen( level )) GLog.w( Messages.get( HeatStation.class, "went_out", name() ) );
	}

	@Override
	protected void liveTurn( OverworldLevel level ){
		super.liveTurn( level );
		if (!lit || !level.mobs.contains( this )) return;
		TileTemperature.depositHeat( pos, cellHeat() );
		for (int n : PathFinder.NEIGHBOURS8){
			int c = pos + n;
			if (c >= 0 && c < level.length() && !level.solid[c]) TileTemperature.depositHeat( c, radiate() );
		}
	}

	@Override
	protected synchronized ArrayList<Item> contents(){
		ArrayList<Item> all = super.contents();
		all.addAll( fuel );
		return all;
	}

	@Override
	protected synchronized void emptyOut(){
		super.emptyOut();
		fuel.clear();
		burnLeft = 0f;
		lit = false;
	}

	@Override
	public synchronized String status(){
		String s = super.status();
		int turns = Math.round( fuelTurns() );
		return s + '\n' + (lit ? Messages.get( HeatStation.class, "status_lit", turns )
				: turns > 0 ? Messages.get( HeatStation.class, "status_unlit", turns )
				: Messages.get( HeatStation.class, "status_no_fuel" ));
	}

	private static final String FUEL = "fuel";
	private static final String BURN = "burn_left";
	private static final String LIT  = "lit";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		synchronized (this){
			bundle.put( FUEL, fuel );
			bundle.put( BURN, burnLeft );
		}
		bundle.put( LIT, lit );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		synchronized (this){
			fuel.clear();
			for (Bundlable b : bundle.getCollection( FUEL )){
				if (b instanceof Item) fuel.add( (Item) b );
			}
			burnLeft = bundle.getFloat( BURN );
			lit = bundle.getBoolean( LIT );
			busy = looksBusy();
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.bounty;

import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;

/**
 * One notice of a village's board (Bounties): what it asks, for how much, and - once the hero has
 * taken it - how far along it is. An offer is a pure function of (seed, village, period, slot), so a
 * board shows the same three notices on every screen and after every reload; only a taken notice is
 * kept (BountyWorld), with the reward it promised when it was taken and its progress.
 *
 * HUNT: kill `need` of the prey `target` (Bounties.PREY) within Bounties.HUNT_REACH of the village's
 * well, then collect at its board. GATHER: bring `need` of the good `target` (Bounties.GOODS) to its
 * board. DELIVER: carry a Parcel to an elder of the village `dest` before `deadline` (a world turn).
 * A HUNT of a great beast (Bounties.beastOffer) asks for one roaming beast, the event `beast`, killed
 * anywhere before its days end (`deadline`); its `target` names its kind ("beast_wolf"...).
 */
public class Bounty implements Bundlable {

	//ORDINALS ARE NOT SAVED: kinds are bundled by name
	public enum Kind { HUNT, GATHER, DELIVER }

	//what comes with the gold, one notice in BONUS_ODDS (Bounties)
	public enum Bonus { NONE, POTION, SCROLL, STONE }

	/** The offer's identity (Bounties.offerId): the same notice on every screen. */
	public long id;
	/** The village whose board posted it (a sector key, WorldStructures.sectorOf). */
	public long village;
	/** The board's period it was posted in (Bounties.period) and its place on the board. */
	public int period, slot;
	public Kind kind = Kind.HUNT;
	/** HUNT: a Prey's id; GATHER: a Good's id; DELIVER: unused. */
	public String target = "";
	/** How many to kill or bring (1 for a delivery). */
	public int need = 1;
	/** The reward before the world's prices are applied (Bounties.reward scales it). */
	public int coin;
	public Bonus bonus = Bonus.NONE;
	/** DELIVER: the village whose elder waits for the parcel, and how far its well is, in cells. */
	public long dest = Long.MIN_VALUE;
	public int dist;
	/** A great beast's HUNT: the roaming beast's event id (RoamerEvent), Long.MIN_VALUE for any other. */
	public long beast = Long.MIN_VALUE;

	// ---- once taken

	/** The gold it pays, fixed when it was taken. */
	public int reward;
	/** HUNT: the kills so far. */
	public int progress;
	/** DELIVER: the world turn it was taken on and the one it is due by (WorldClock.turn); a great
	 *  beast's HUNT: the turn its days abroad end. */
	public int takenTurn, deadline;
	/** DELIVER: past its deadline (the log has said so: BountyActions.tick). */
	public boolean late;
	/** The notices the log has given about it already (BountyActions.tick), bit by bit. */
	public int told;

	public static final int TOLD_READY = 1, TOLD_DUE = 2, TOLD_ARRIVED = 4;

	public Bounty(){
	}

	public boolean told( int bit ){
		return (told & bit) != 0;
	}

	private static final String ID = "id", VILLAGE = "village", PERIOD = "period", SLOT = "slot", KIND = "kind",
			TARGET = "target", NEED = "need", COIN = "coin", BONUS = "bonus", DEST = "dest", DIST = "dist",
			REWARD = "reward", PROGRESS = "progress", TAKEN = "taken", DEADLINE = "deadline", LATE = "late",
			TOLD = "told", BEAST = "beast";

	@Override
	public void storeInBundle( Bundle bundle ){
		bundle.put( ID, id );
		bundle.put( VILLAGE, village );
		bundle.put( PERIOD, period );
		bundle.put( SLOT, slot );
		bundle.put( KIND, kind.name() );
		bundle.put( TARGET, target == null ? "" : target );
		bundle.put( NEED, need );
		bundle.put( COIN, coin );
		bundle.put( BONUS, bonus.name() );
		bundle.put( DEST, dest );
		bundle.put( DIST, dist );
		bundle.put( REWARD, reward );
		bundle.put( PROGRESS, progress );
		bundle.put( TAKEN, takenTurn );
		bundle.put( DEADLINE, deadline );
		bundle.put( LATE, late );
		bundle.put( TOLD, told );
		if (beast != Long.MIN_VALUE) bundle.put( BEAST, beast );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		id = bundle.getLong( ID );
		village = bundle.getLong( VILLAGE );
		period = bundle.getInt( PERIOD );
		slot = bundle.getInt( SLOT );
		kind = kindOf( bundle.getString( KIND ) );
		target = bundle.getString( TARGET );
		need = Math.max( 1, bundle.getInt( NEED ) );
		coin = bundle.getInt( COIN );
		bonus = bonusOf( bundle.getString( BONUS ) );
		dest = bundle.contains( DEST ) ? bundle.getLong( DEST ) : Long.MIN_VALUE;
		dist = bundle.getInt( DIST );
		reward = bundle.getInt( REWARD );
		progress = bundle.getInt( PROGRESS );
		takenTurn = bundle.getInt( TAKEN );
		deadline = bundle.getInt( DEADLINE );
		late = bundle.getBoolean( LATE );
		told = bundle.getInt( TOLD );
		//absent from older saves and from every other notice
		beast = bundle.contains( BEAST ) ? bundle.getLong( BEAST ) : Long.MIN_VALUE;
	}

	//a kind this build does not know reads as null: BountyWorld drops the notice
	static Kind kindOf( String name ){
		for (Kind k : Kind.values()) if (k.name().equals( name )) return k;
		return null;
	}

	private static Bonus bonusOf( String name ){
		for (Bonus b : Bonus.values()) if (b.name().equals( name )) return b;
		return Bonus.NONE;
	}
}

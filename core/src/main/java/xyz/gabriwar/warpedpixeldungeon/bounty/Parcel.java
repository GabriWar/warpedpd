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

import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import com.watabou.utils.Bundle;

/**
 * A village's parcel (a DELIVER notice, Bounties): wrapped, tied and sealed, handed over with the
 * notice and taken off the hero by an elder of the village it is for (BountyActions.settlerTalk).
 * It says where it goes, where it came from and how long it has left; a parcel whose notice is gone
 * (given up, or the run's record of it lost) is nobody's business any more. A quest item: never sold,
 * stolen or burnt, worth nothing to anyone but its elder.
 */
public class Parcel extends Item {

	{
		image = ItemSpriteSheet.PARCEL;
		unique = true;
		bones = false;
	}

	/** The delivery's notice (Bounty.id). */
	public long bounty;
	/** The villages it goes between, by name as the notice gave them. */
	public String from = "", to = "";
	/** The world turn it is due by. */
	public int deadline;

	public Parcel(){
	}

	public static Parcel of( long seed, Bounty b ){
		Parcel p = new Parcel();
		p.bounty = b.id;
		p.from = Bounties.villageName( seed, b.village );
		p.to = Bounties.villageName( seed, b.dest );
		p.deadline = b.deadline;
		return p;
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
		return 0;
	}

	@Override
	public String name(){
		return to == null || to.isEmpty() ? super.name() : Messages.get( this, "name_to", to );
	}

	@Override
	public String desc(){
		if (to == null || to.isEmpty()) return super.desc();
		BountyWorld w = BountyShare.world();
		Bounty b = w == null ? null : w.takenById( bounty );
		if (b == null) return Messages.get( this, "desc_forgotten", from, to );
		int left = deadline - WorldClock.turn();
		if (b.late || left <= 0) return Messages.get( this, "desc_late", from, to );
		return Messages.get( this, "desc_due", from, to, Bounties.hours( left ) );
	}

	private static final String BOUNTY = "bounty", FROM = "from", TO = "to", DEADLINE = "deadline";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( BOUNTY, bounty );
		bundle.put( FROM, from == null ? "" : from );
		bundle.put( TO, to == null ? "" : to );
		bundle.put( DEADLINE, deadline );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		bounty = bundle.getLong( BOUNTY );
		from = bundle.getString( FROM );
		to = bundle.getString( TO );
		deadline = bundle.getInt( DEADLINE );
	}
}

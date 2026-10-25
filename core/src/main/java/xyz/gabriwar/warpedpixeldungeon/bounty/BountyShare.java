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

import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import com.watabou.utils.Bundle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;

/**
 * The bounties a co-op guest's screen shows, which are the host's: the run's record (BountyWorld)
 * lives on the host alone, so he ships it (text, as the "wbounty" of StateSerializer's overworld
 * state, again whenever sig() changes) with the villages where his hero could collect a reward now,
 * and the guest takes it over (adopt). world() and readyAt() are what the journal, a parcel's words
 * and the boards' looks read on any screen: the run's own on the host's (or a lone hero's), the
 * host's shipped copy on a guest's. The guest only reads: the notices are the host's to take.
 */
public final class BountyShare {

	private BountyShare(){}

	//a guest's copy of the host's record (adopt) and his ready villages; swapped whole, never changed
	private static volatile BountyWorld guestWorld = new BountyWorld();
	private static volatile long[] guestReady = new long[0];

	private static boolean guest(){
		return NetManager.isNetClient();
	}

	/** The bounties' record this screen shows: the run's, or on a guest's screen the host's. */
	public static BountyWorld world(){
		return guest() ? guestWorld : BountyWorld.get();
	}

	/** Could the hero (the host's, on a guest's screen) collect a reward at this village's board now? */
	public static boolean readyAt( long village ){
		if (!guest()) return BountyActions.readyVillages().contains( village );
		for (long k : guestReady) if (k == village) return true;
		return false;
	}

	/** Co-op host: a fingerprint of what his guests see, shipped again (text) only when it changed. */
	public static long sig(){
		BountyWorld w = BountyWorld.get();
		long h = w == null ? 0 : w.version();
		for (long k : BountyActions.readyVillages()) h = h * 31 + k;
		return h;
	}

	private static final String READY = "ready_villages";

	/** Co-op host: the record and the ready villages, as a bundle's text for his guests (adopt). */
	public static String text(){
		Bundle b = new Bundle();
		BountyWorld w = BountyWorld.get();
		if (w != null) w.storeInBundle( b );
		ArrayList<Long> ready = BountyActions.readyVillages();
		long[] keys = new long[ready.size()];
		for (int i = 0; i < keys.length; i++) keys[i] = ready.get( i );
		b.put( READY, keys );
		return b.toString();
	}

	/** A co-op guest takes over the host's record (text). Any thread. */
	public static void adopt( String text ){
		if (text == null || text.isEmpty()) return;
		Bundle b;
		try {
			b = Bundle.read( new ByteArrayInputStream( text.getBytes() ) );
		} catch (IOException e){
			//Bundle.read has reported it. the copy stays as it was; the next change ships it again
			return;
		}
		BountyWorld w = new BountyWorld();
		w.restoreFromBundle( b );
		guestReady = b.contains( READY ) ? b.getLongArray( READY ) : new long[0];
		guestWorld = w;
	}
}

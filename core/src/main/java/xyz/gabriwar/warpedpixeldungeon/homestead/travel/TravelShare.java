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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.homestead.HomesteadWorld;
import xyz.gabriwar.warpedpixeldungeon.homestead.threats.ThreatMarks;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import com.watabou.utils.Bundle;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The homes and the waystones a co-op guest's world map pins, which are the host's: the run's
 * record of them (HomesteadWorld, TravelWorld) lives on the host alone, so he ships it (text, as the
 * "wtravel" of StateSerializer's overworld state, again whenever sig() changes) and the guest takes
 * it over (adopt). homes() and stones() are what the map and the waystones' glow read on any screen:
 * the run's own on the host's (or a lone hero's), the host's shipped copy on a guest's; with each
 * home goes the siege or raid under way there (threat), for the map's pin on it (threats/ThreatMarks).
 */
public final class TravelShare {

	private TravelShare(){}

	//a guest's copy of the host's homes and known waystones (adopt); swapped whole, never changed
	private static volatile List<HomesteadWorld.Home> guestHomes = Collections.emptyList();
	private static volatile List<TravelWorld.Stone> guestStones = Collections.emptyList();
	//what is under way at each of guestHomes, in its order (ThreatMarks.threatOf)
	private static volatile int[] guestThreats = new int[0];

	//is this screen a co-op guest's?
	private static boolean guest(){
		return NetManager.isNetClient();
	}

	/** The homes the map pins: the run's, or on a guest's screen the host's. */
	public static List<HomesteadWorld.Home> homes(){
		return guest() ? guestHomes : HomesteadWorld.homes();
	}

	/** The siege or raid under way at a home of homes() (ThreatMarks.threatOf): the run's, or on a
	 *  guest's screen what the host shipped. */
	public static int threat( HomesteadWorld.Home home ){
		if (!guest()) return ThreatMarks.threatOf( home );
		List<HomesteadWorld.Home> homes = guestHomes;
		int[] threats = guestThreats;
		for (int i = 0; i < homes.size() && i < threats.length; i++){
			if (homes.get( i ) == home) return threats[i];
		}
		return ThreatMarks.NONE;
	}

	/** The waystones known (seen or attuned), in the order they were found: the run's, or on a
	 *  guest's screen the host's. */
	public static List<TravelWorld.Stone> stones(){
		if (guest()) return guestStones;
		TravelWorld tw = TravelWorld.get();
		return tw == null ? Collections.emptyList() : tw.known();
	}

	/** Is the waystone standing on this world cell attuned (by stones())? */
	public static boolean attunedAt( int wx, int wy ){
		for (TravelWorld.Stone s : stones()) if (s.wx == wx && s.wy == wy) return s.attuned;
		return false;
	}

	/** Co-op host: a fingerprint of what his guests' maps pin - every home's slice, cell and name,
	 *  every known waystone's cell and attunement - shipped again (text) only when it changed. */
	public static long sig(){
		long h = 17;
		for (HomesteadWorld.Home home : HomesteadWorld.homes()){
			h = h * 31 + home.depth;
			h = h * 31 + home.wx;
			h = h * 31 + home.wy;
			h = h * 31 + home.title().hashCode();
			h = h * 31 + ThreatMarks.threatOf( home );
		}
		TravelWorld tw = TravelWorld.get();
		if (tw != null){
			for (TravelWorld.Stone s : tw.known()){
				h = h * 31 + s.key;
				h = h * 31 + s.wx;
				h = h * 31 + s.wy;
				h = h * 31 + (s.attuned ? 1 : 0);
			}
		}
		return h;
	}

	private static final String H_DEPTH = "hdepth", H_X = "hx", H_Y = "hy", H_NAME = "hname", H_THREAT = "hthreat";
	private static final String S_KEY = "skey", S_X = "sx", S_Y = "sy", S_NAME = "sname", S_ATTUNED = "satt";

	/** Co-op host: the homes and known waystones, as a bundle's text for his guests (adopt). */
	public static String text(){
		List<HomesteadWorld.Home> homes = HomesteadWorld.homes();
		int n = homes.size();
		int[] depth = new int[n], hx = new int[n], hy = new int[n], threat = new int[n];
		String[] hname = new String[n];
		for (int i = 0; i < n; i++){
			HomesteadWorld.Home home = homes.get( i );
			depth[i] = home.depth;
			hx[i] = home.wx;
			hy[i] = home.wy;
			hname[i] = home.title();
			threat[i] = ThreatMarks.threatOf( home );
		}
		TravelWorld tw = TravelWorld.get();
		List<TravelWorld.Stone> stones = tw == null ? Collections.emptyList() : tw.known();
		int m = stones.size();
		long[] key = new long[m];
		int[] sx = new int[m], sy = new int[m];
		String[] sname = new String[m];
		boolean[] att = new boolean[m];
		for (int i = 0; i < m; i++){
			TravelWorld.Stone s = stones.get( i );
			key[i] = s.key;
			sx[i] = s.wx;
			sy[i] = s.wy;
			sname[i] = s.name == null ? "" : s.name;
			att[i] = s.attuned;
		}
		Bundle b = new Bundle();
		b.put( H_DEPTH, depth );
		b.put( H_X, hx );
		b.put( H_Y, hy );
		b.put( H_NAME, hname );
		b.put( H_THREAT, threat );
		b.put( S_KEY, key );
		b.put( S_X, sx );
		b.put( S_Y, sy );
		b.put( S_NAME, sname );
		b.put( S_ATTUNED, att );
		return b.toString();
	}

	/** A co-op guest takes over the host's homes and known waystones (text). Any thread. */
	public static void adopt( String text ){
		if (text == null || text.isEmpty()) return;
		Bundle b;
		try {
			b = Bundle.read( new ByteArrayInputStream( text.getBytes() ) );
		} catch (IOException e){
			//Bundle.read has reported it. the pins stay as they were; the next change ships them again
			return;
		}
		if (!b.contains( H_DEPTH ) || !b.contains( S_KEY )) return;
		int[] depth = b.getIntArray( H_DEPTH ), hx = b.getIntArray( H_X ), hy = b.getIntArray( H_Y );
		String[] hname = b.getStringArray( H_NAME );
		int[] hthreat = b.contains( H_THREAT ) ? b.getIntArray( H_THREAT ) : new int[0];
		ArrayList<HomesteadWorld.Home> homes = new ArrayList<>();
		int n = Math.min( Math.min( depth.length, hx.length ), Math.min( hy.length, hname.length ) );
		int[] threats = new int[n];
		for (int i = 0; i < n; i++){
			homes.add( new HomesteadWorld.Home( depth[i], hx[i], hy[i], hname[i], 0 ) );
			threats[i] = i < hthreat.length ? hthreat[i] : ThreatMarks.NONE;
		}
		long[] key = b.getLongArray( S_KEY );
		int[] sx = b.getIntArray( S_X ), sy = b.getIntArray( S_Y );
		String[] sname = b.getStringArray( S_NAME );
		boolean[] att = b.getBooleanArray( S_ATTUNED );
		ArrayList<TravelWorld.Stone> stones = new ArrayList<>();
		int m = Math.min( Math.min( key.length, sx.length ), Math.min( Math.min( sy.length, sname.length ), att.length ) );
		for (int i = 0; i < m; i++) stones.add( new TravelWorld.Stone( key[i], sx[i], sy[i], sname[i], att[i] ) );
		guestThreats = threats;
		guestHomes = Collections.unmodifiableList( homes );
		guestStones = Collections.unmodifiableList( stones );
	}
}

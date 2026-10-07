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


package xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.GameCalendar;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoadTraffic;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.FarmerSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MessengerSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PedlarSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.PilgrimSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/** Someone walking the road between two villages - a pilgrim, a pedlar, a messenger, a
 *  farmer - by the clock RoadTraffic keeps. Tells you where they are bound and, every other
 *  one, something real they heard of on the way. */
public class Traveller extends RoadWalker {

	{
		spriteClass = PilgrimSprite.class;
	}

	public int kind = RoadTraffic.PILGRIM;
	//the villages it is from and bound for (sectors)
	public int fromSx, fromSy, toSx, toSy;
	//the turns of its day it set out and is due in
	public int depart, arrive;
	public float pace = RoadTraffic.PACE[RoadTraffic.PILGRIM];
	//which walk of the road it is (RoadTraffic.Trip.key)
	public long key;

	//turns it stands still after being spoken to (not bundled)
	private int hush = 0;

	/** The walker of a trip (RoadTraffic.tripAt), ready to be set down on the road. */
	public static Traveller of( RoadTraffic.Trip trip ){
		Traveller t = new Traveller();
		t.key = trip.key;
		t.kind = trip.kind;
		t.fromSx = trip.fromSx;
		t.fromSy = trip.fromSy;
		t.toSx = trip.toSx;
		t.toSy = trip.toSy;
		t.day = trip.day;
		t.depart = trip.depart;
		t.arrive = trip.arrive;
		t.pace = trip.pace;
		t.route = trip.route.clone();
		t.applyKind();
		return t;
	}

	//one sprite class per kind: a multiplayer spectator rebuilds a sprite from its class alone
	private void applyKind(){
		kind = Math.floorMod( kind, RoadTraffic.KINDS );
		switch (kind){
			case RoadTraffic.PEDLAR:    spriteClass = PedlarSprite.class; break;
			case RoadTraffic.MESSENGER: spriteClass = MessengerSprite.class; break;
			case RoadTraffic.FARMER:    spriteClass = FarmerSprite.class; break;
			default:                    spriteClass = PilgrimSprite.class; break;
		}
	}

	@Override
	public String name() {
		return Messages.get( this, "name_" + kind );
	}

	@Override
	public String description() {
		return Messages.get( this, "desc_" + kind );
	}

	@Override
	protected boolean walk( OverworldLevel ow, long now ){
		int tod = RoadTraffic.turnOfDay( now );
		//the walk is over, belongs to another day, or the hour is pinned to night: in at the far door
		if (RoadTraffic.day( now ) != day || tod >= arrive || RoadTraffic.nightForced()){
			return goIn( ow, RoadTraffic.length( route ), 1 );
		}
		if (hush > 0){
			hush--;   //stopped to talk
			spend( TICK );
			return true;
		}
		return follow( ow, Math.max( 0f, (tod - depart) * pace ), pace, 1 );
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );
		if (!(Dungeon.level instanceof OverworldLevel)) return true;
		final String text = talk( (OverworldLevel) Dungeon.level );
		hush = 3;
		if (NetDialogs.handleNetHero( c, text )) return true;
		if (c != Dungeon.hero) return true;
		Game.runOnRenderThread( () -> GameScene.show( new WndQuest( Traveller.this, text ) ) );
		return true;
	}

	private static final String[] RUMOURS = { "rumour_dragon", "rumour_ruin", "rumour_outlaws", "rumour_caravan", "rumour_watched" };

	//where they are bound, and - for every other traveller - something real they heard of
	private String talk( OverworldLevel ow ){
		long seed = ow.worldSeed();
		String line = Messages.get( this, "line_" + kind,
				WorldStructures.villageName( seed, toSx, toSy ), WorldStructures.villageName( seed, fromSx, fromSy ) );
		if (((key >>> 3) & 1L) != 0) return line;
		int wx = ow.worldX() + pos % ow.width(), wy = ow.worldY() + pos / ow.width();
		int[] r = RoadTraffic.rumour( seed, wx, wy, key, WorldClock.day(),
				GameCalendar.weekday().ordinal(), GameCalendar.dayOfSeason(), ow.clearedSites() );
		if (r == null) return line;
		//the outlaws by that stall have had their day already: only the stall is worth telling of
		if (r[0] == RoadTraffic.RUMOUR_WATCHED && ow.caravanAtPeace( r[1], r[2] )) r[0] = RoadTraffic.RUMOUR_CARAVAN;
		int dx = r[1] - wx, dy = r[2] - wy;
		double dist = Math.sqrt( (double) dx * dx + (double) dy * dy );
		String far = Messages.get( this, dist < 70 ? "dist_near" : dist < 160 ? "dist_half" : "dist_far" );
		return line + "\n\n" + Messages.get( this, RUMOURS[r[0]], far, Messages.get( this, "dir_" + RoadTraffic.compass( dx, dy ) ) );
	}

	private static final String KIND     = "kind";
	private static final String FROM_X   = "from_x";
	private static final String FROM_Y   = "from_y";
	private static final String TO_X     = "to_x";
	private static final String TO_Y     = "to_y";
	private static final String DEPART   = "depart";
	private static final String ARRIVE   = "arrive";
	private static final String PACE     = "pace";
	private static final String TRIP_KEY = "trip_key";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( KIND, kind );
		bundle.put( FROM_X, fromSx );
		bundle.put( FROM_Y, fromSy );
		bundle.put( TO_X, toSx );
		bundle.put( TO_Y, toSy );
		bundle.put( DEPART, depart );
		bundle.put( ARRIVE, arrive );
		bundle.put( PACE, pace );
		bundle.put( TRIP_KEY, key );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		kind   = bundle.contains( KIND )   ? bundle.getInt( KIND )   : RoadTraffic.PILGRIM;
		applyKind();
		fromSx = bundle.contains( FROM_X ) ? bundle.getInt( FROM_X ) : 0;
		fromSy = bundle.contains( FROM_Y ) ? bundle.getInt( FROM_Y ) : 0;
		toSx   = bundle.contains( TO_X )   ? bundle.getInt( TO_X )   : 0;
		toSy   = bundle.contains( TO_Y )   ? bundle.getInt( TO_Y )   : 0;
		depart = bundle.contains( DEPART ) ? bundle.getInt( DEPART ) : 0;
		arrive = bundle.contains( ARRIVE ) ? bundle.getInt( ARRIVE ) : 0;
		pace   = bundle.contains( PACE )   ? bundle.getFloat( PACE ) : RoadTraffic.PACE[kind];
		key    = bundle.contains( TRIP_KEY ) ? bundle.getLong( TRIP_KEY ) : 0L;
	}
}

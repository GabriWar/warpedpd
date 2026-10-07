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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.debug.LagMonitor;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RaidEvent;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.VillageRoutine;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ListIterator;

/**
 * The folk of the overworld's settlements (Villager, GnollVillager): untouchable, chatty,
 * and living a day - out to work at dawn, to the well at dusk, home to bed at night
 * (VillageRoutine). Near a hero they walk there, one step a turn along a path worked out
 * once; further off, or coming into the window, they simply are where the day has got to.
 * Off the surface they only amble.
 */
public abstract class Settler extends NPC {

	//which house of the settlement is theirs (its layout index), which of the family they are,
	//their trade (VillageRoutine.Role) and the part of the day they last settled into
	//(VillageRoutine.Slot). -1 until known: an older save works them out on its first turn
	//back on the surface (adopt)
	public int house = -1;
	public int member = -1;
	public int role = -1;
	public int slot = -1;
	//the house's centre, in world cells
	public int homeX = Integer.MIN_VALUE, homeY = Integer.MIN_VALUE;

	/** The packed sector of the settlement this one belongs to. */
	public abstract long settlementKey();

	protected abstract boolean gnoll();

	//how many line_N idle lines this kind has (what it says off the surface)
	protected abstract int idleLines();

	private static final long[] NO_SHORES = new long[0];

	//none of this is bundled: worked out again after a load
	private VillageRoutine.Settlement place;
	private boolean noRoutine = false;   //its sector is no settlement (spawned by hand, a house interior): only ever ambles
	private int personal;
	private long spot;   //VillageRoutine.spotHash: which of its trade's spots it takes, and when its legs turn
	//the spot of this part of the day, the free cell it resolved to (window cell, and world
	//cell to carry it across a rebase) and the window it was found in
	private VillageRoutine.Target plan;
	private int destCell = -1, destWX, destWY, destVersion = -1;
	//a spot that could not be had is looked for again only after this many turns
	private int resolveRetry = 0;
	//the walk there: worked out once (window cells), and the window origin it was worked out in
	private PathFinder.Path route;
	private int routeVersion = -1, routeOX, routeOY;
	private int avoidCell = -1, blocked = 0, retry = 0;
	//turns until the next gesture of its trade, and turns it stays put after being spoken to
	private int idle = 0, hush = 0;

	@Override
	protected boolean act() {
		throwItems();
		OverworldLevel ow = surface();
		if (sprite == null || ow == null || !ready( ow, ow.worldX() + pos % ow.width(), ow.worldY() + pos / ow.width() )){
			amble();
			spend( TICK );
			return true;
		}
		long t = LagMonitor.begin();
		float time = routine( ow );
		LagMonitor.end( "Settler routine", t );
		//every way through the routine takes a turn or a step: never no time at all
		spend( time );
		return true;
	}

	private static OverworldLevel surface(){
		return Dungeon.level instanceof OverworldLevel && ((OverworldLevel) Dungeon.level).altitude() == 0
				? (OverworldLevel) Dungeon.level : null;
	}

	private void amble(){
		//cheap ambling: one free adjacent step now and then, zero pathfinding.
		//a metropolis can hold dozens of these - the default wandering AI runs
		//a pathfind per mob per turn and was a real source of turn lag
		//settlers keep to their beds at night
		if (sprite != null && !Shopkeeper.closedForNight() && Random.Int( 3 ) == 0){
			int step = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
			if (step >= 0 && step < Dungeon.level.length()
					&& Dungeon.level.passable[step]
					&& !Dungeon.level.avoid[step]
					//tall grass tramples roll loot for ANY walker - a town of
					//amblers would slowly carpet itself in seeds
					&& Dungeon.level.map[step] != Terrain.HIGH_GRASS
					&& Actor.findChar( step ) == null){
				moveSprite( pos, step );
				move( step );
			}
		}
	}

	//finds its settlement once, and fills in whatever an older save did not carry from where
	//it stands (world wx, wy)
	private boolean ready( OverworldLevel ow, int wx, int wy ){
		if (place == null){
			if (noRoutine) return false;
			VillageRoutine.Settlement s = VillageRoutine.settlement( ow.worldSeed(), settlementKey() );
			if (s == null || s.gnoll != gnoll()){
				noRoutine = true;
				return false;
			}
			adopt( s, wx, wy, ow );
		}
		return true;
	}

	/** Takes its settlement and fills in what a save lacks, from where it stands (world wx,
	 *  wy): the nearest family house (passing over shops when the level is given), that house's
	 *  centre, a place in the family and the family's trade for it; then its own timing. */
	public void adopt( VillageRoutine.Settlement s, int wx, int wy, OverworldLevel ow ){
		place = s;
		if (house < 0 || house >= s.houses()){
			house = VillageRoutine.nearestHouse( s, wx, wy, ow );
			homeX = s.houseX( house );
			homeY = s.houseY( house );
		}
		if (homeX == Integer.MIN_VALUE || homeY == Integer.MIN_VALUE){
			homeX = s.houseX( house );
			homeY = s.houseY( house );
		}
		if (member < 0) member = Math.floorMod( wx * 7 + wy * 13, 3 );
		long family = VillageRoutine.familyHash( s.sx, s.sy, house );
		VillageRoutine.Role[] roles = VillageRoutine.Role.values();
		if (role < 0 || role >= roles.length || !VillageRoutine.belongs( roles[role], gnoll() )){
			role = VillageRoutine.roleOf( family, member, gnoll() ).ordinal();
		}
		personal = VillageRoutine.personal( family, member );
		spot = VillageRoutine.spotHash( family, member );
	}

	/** Comes into the window - spawned, unparked, loaded - having stood at world (wx, wy): set
	 *  straight on its spot for this part of the day, abed if that is where the day has got
	 *  to, before any sprite or turn. The window cell, or -1 when the spot cannot be had (the
	 *  caller sets it down where it always did). See VillageRoutine.arrivalCell. */
	public int arrive( OverworldLevel ow, int wx, int wy ){
		if (ow.altitude() != 0 || !ready( ow, wx, wy )) return -1;
		VillageRoutine.Clock c = VillageRoutine.clock();
		VillageRoutine.Role r = VillageRoutine.Role.values()[role];
		VillageRoutine.Slot s = slotNow( ow, c, r );
		slot = s.ordinal();
		aim( ow, c, r, s );
		//off the board while it looks: a cell from an older window is nobody's spot
		int was = pos;
		pos = -1;
		int cell = VillageRoutine.resolve( ow, place, plan, this, true, homeArea( ow ) );
		if (cell == -1){
			pos = was;
			return -1;
		}
		settleOn( ow, cell );
		state = s == VillageRoutine.Slot.BED ? SLEEPING : PASSIVE;
		idle = Random.IntRange( 2, 8 );
		return cell;
	}

	/** Has the day moved on past the part of it this one last settled into? A window coming
	 *  back out of the save puts those straight where it has got to (OverworldLevel). */
	public boolean dayMovedOn( OverworldLevel ow, int wx, int wy ){
		if (ow.altitude() != 0 || !ready( ow, wx, wy )) return false;
		return slotNow( ow, VillageRoutine.clock(), VillageRoutine.Role.values()[role] ).ordinal() != slot;
	}

	//where in its day it is
	private VillageRoutine.Slot slotNow( OverworldLevel ow, VillageRoutine.Clock c, VillageRoutine.Role r ){
		VillageRoutine.Slot s = VillageRoutine.slotFor( r, c, personal );
		//a travelling market in the settlement draws people to the well
		if (ow.marketActiveAt( settlementKey() )) s = VillageRoutine.marketSlot( r, s );
		//a raid, or the mending after one: everyone indoors (bed stays bed) - except the family
		//whose own house the band set alight: they are out at the well with the buckets
		if (!gnoll() && s != VillageRoutine.Slot.BED && RaidEvent.shelters( settlementKey(), house )){
			s = homeBurning( ow ) ? VillageRoutine.Slot.GATHER : VillageRoutine.Slot.INDOORS;
		}
		return s;
	}

	//is its own house one of the raid's fires (RaidEvent.burningHouses: the houses' middles)?
	private boolean homeBurning( OverworldLevel ow ){
		for (int[] fire : RaidEvent.burningHouses( ow )){
			if (fire[0] == homeX && fire[1] == homeY) return true;
		}
		return false;
	}

	//the area of the window's ground its house opens on: its spots are only ever there, so it
	//can walk to each and home again (VillageRoutine.homeArea)
	private int homeArea( OverworldLevel ow ){
		return VillageRoutine.homeArea( ow, place, homeX, homeY );
	}

	//a spot for a new part of the day, or for the next leg of a round
	private void aim( OverworldLevel ow, VillageRoutine.Clock c, VillageRoutine.Role r, VillageRoutine.Slot s ){
		long[] shores = r == VillageRoutine.Role.FISHER && s == VillageRoutine.Slot.WORK
				? VillageRoutine.shores( ow, place, homeArea( ow ) ) : NO_SHORES;
		plan = VillageRoutine.target( place, house, homeX, homeY, member, r, s, c, shores );
		destCell = -1;
		destVersion = ow.windowVersion();
		resolveRetry = 0;
		route = null;
		avoidCell = -1;
		blocked = 0;
		retry = 0;
	}

	//the free cell its spot resolved to
	private void settleOn( OverworldLevel ow, int cell ){
		destCell = cell;
		destWX = ow.worldX() + cell % ow.width();
		destWY = ow.worldY() + cell / ow.width();
		destVersion = ow.windowVersion();
	}

	//the day's round, once a turn: the time it took
	private float routine( OverworldLevel ow ){
		VillageRoutine.Clock c = VillageRoutine.clock();
		VillageRoutine.Role r = VillageRoutine.Role.values()[role];
		VillageRoutine.Slot s = slotNow( ow, c, r );
		if (s.ordinal() != slot){
			slot = s.ordinal();
			plan = null;
		} else if (plan != null && plan.beat != VillageRoutine.beatOf( r, s, c, spot )){
			plan = null;   //the round moved on
		}
		if (state != SLEEPING && state != PASSIVE) state = PASSIVE;
		if (state == SLEEPING && s != VillageRoutine.Slot.BED) state = PASSIVE;   //up and about
		if (hush > 0){
			hush--;   //stopped to talk
			return TICK;
		}
		if (plan == null) aim( ow, c, r, s );

		//the window moved under its spot (a rebase, a reseason): the same world cell, if it still holds
		if (destCell != -1 && destVersion != ow.windowVersion()){
			int cell = VillageRoutine.spotCell( ow, destWX, destWY );
			destCell = -1;
			if (cell != -1 && VillageRoutine.standable( ow, cell )){
				Char o = Actor.findChar( cell );
				if (o == null || o == this) settleOn( ow, cell );
			}
			destVersion = ow.windowVersion();
			resolveRetry = 0;
			avoidCell = -1;   //a cell of the old window
		}
		if (destCell == -1){
			if (resolveRetry > 0 && destVersion == ow.windowVersion()){
				resolveRetry--;
			} else {
				int cell = VillageRoutine.resolve( ow, place, plan, this, false, homeArea( ow ) );
				route = null;
				if (cell == -1){
					destVersion = ow.windowVersion();
					resolveRetry = 12 + (personal & 7);
				} else {
					settleOn( ow, cell );
				}
			}
		}
		if (destCell == -1){
			//its spot is off this window, or full: it potters about where it is, if anyone is there to see
			if (Random.Int( 3 ) == 0 && OverworldLevel.heroDistance( ow, pos ) <= VillageRoutine.NEAR){
				ambleNear( ow, pos, 1, false );
			}
			return TICK;
		}

		//near its spot is not idling in a doorway or on the planks (where one passing by left
		//it): on to the spot itself
		if (ow.distance( pos, destCell ) > plan.amble || (pos != destCell && !VillageRoutine.standable( ow, pos ))){
			if (OverworldLevel.heroDistance( ow, pos ) > VillageRoutine.NEAR
					&& OverworldLevel.heroDistance( ow, destCell ) > VillageRoutine.NEAR){
				//nobody near enough to see it go: it simply is there
				Char o = Actor.findChar( destCell );
				if (o != null && o != this){
					destCell = -1;   //taken this instant: look again next turn
					return TICK;
				}
				snap( destCell );
				return TICK;
			}
			return walk( ow );
		}
		return atSpot( ow, c, r, s );
	}

	//put straight on its spot
	private void snap( int cell ){
		move( cell, false );
		sprite.place( cell );
		route = null;
		blocked = 0;
		idle = Random.IntRange( 2, 8 );
		if (slot == VillageRoutine.Slot.BED.ordinal()) state = SLEEPING;
	}

	//one step along a path worked out once for the spot: a step's time, or a turn's while it waits
	private float walk( OverworldLevel ow ){
		//on its way, so up: it sleeps once it is in its bed (atSpot, snap)
		if (state == SLEEPING) state = PASSIVE;
		if (route != null && routeVersion != ow.windowVersion()) slideRoute( ow );
		if (route == null){
			if (retry > 0){
				retry--;
				return TICK;
			}
			//no way there over this window's ground at all (a pocket of brush it was set down in
			//unseen, its spot over the water): nothing to search for. it is simply there the
			//moment nobody can see either end, and until then stays where it is
			if (!VillageRoutine.reachable( ow, pos, destCell )){
				if (!VillageRoutine.inSight( ow, pos ) && !VillageRoutine.inSight( ow, destCell )){
					Char o = Actor.findChar( destCell );
					if (o != null && o != this) destCell = -1;   //taken this instant: look again next turn
					else snap( destCell );
				}
				return TICK;
			}
			//this turn's paths are spent: the next turn's then
			if (!VillageRoutine.takePath()) return TICK;
			long t = LagMonitor.begin();
			boolean[] mask = VillageRoutine.walkMask( ow );
			boolean held = avoidCell >= 0 && avoidCell < mask.length && mask[avoidCell];
			if (held) mask[avoidCell] = false;
			route = PathFinder.find( pos, destCell, mask );
			if (held) mask[avoidCell] = true;
			LagMonitor.end( "Settler path", t );
			routeVersion = ow.windowVersion();
			routeOX = ow.worldX();
			routeOY = ow.worldY();
			//there is a way (reachable), so the one there is runs through the cell it gave up on
			if (route == null) return makeWay( ow );
		}
		int next = route.getFirst();
		if (!ow.adjacent( pos, next )){
			route = null;
			return TICK;
		}
		Char o = Actor.findChar( next );
		if (o != null || !VillageRoutine.walkable( ow, next )){
			//somebody in the way, or the ground changed: a little patience, then a way round
			if (++blocked >= 3){
				blocked = 0;
				route = null;
				avoidCell = next;
				if (next == destCell){
					destCell = -1;
					resolveRetry = 0;
				}
			}
			return TICK;
		}
		blocked = 0;
		route.removeFirst();
		if (route.isEmpty()) route = null;
		moveSprite( pos, next );
		move( next );
		return 1f / speed();
	}

	//the only way on runs through the cell it gave up on (a bridge, a doorway, a gap in the
	//palings). whoever stood there has gone: the plain way, next turn. another of the folk,
	//awake: the two step past each other, as people do, rather than both standing there until
	//the hour changes. anyone else (a hero, a keeper at his stall): a while's wait
	private float makeWay( OverworldLevel ow ){
		Char o = Actor.findChar( avoidCell );
		avoidCell = -1;
		if (o == null) return TICK;
		if (o instanceof Settler && passBy( ow, (Settler) o )) return 1f / speed();
		retry = 20;
		return TICK;
	}

	//trades places with another of the folk beside it: a step each, never past a sleeper
	private boolean passBy( OverworldLevel ow, Settler o ){
		int here = pos, there = o.pos;
		if (o.state == SLEEPING || o.sprite == null || !ow.adjacent( here, there )
				|| !VillageRoutine.walkable( ow, here ) || !VillageRoutine.walkable( ow, there )) return false;
		o.moveSprite( there, here );
		o.move( here );
		moveSprite( here, there );
		move( there );
		//the step was its turn too; its walk, if it had one, set out from the cell it left
		o.spend( 1f / o.speed() );
		o.route = null;
		o.blocked = 0;
		o.avoidCell = -1;
		o.retry = 0;
		return true;
	}

	//the window slid under a walk in progress: the same path in the new window's cells,
	//dropped if any of it has left the window
	private void slideRoute( OverworldLevel ow ){
		int w = ow.width(), h = ow.height();
		int dx = routeOX - ow.worldX(), dy = routeOY - ow.worldY();
		for (ListIterator<Integer> it = route.listIterator(); it.hasNext(); ){
			int c = it.next();
			int x = c % w + dx, y = c / w + dy;
			if (x < 1 || y < 1 || x > w - 2 || y > h - 2){
				route = null;
				return;
			}
			it.set( x + y * w );
		}
		avoidCell = -1;
		routeVersion = ow.windowVersion();
		routeOX = ow.worldX();
		routeOY = ow.worldY();
	}

	//at its spot: asleep, or at its work - which only shows where a hero is near
	private float atSpot( OverworldLevel ow, VillageRoutine.Clock c, VillageRoutine.Role r, VillageRoutine.Slot s ){
		if (s == VillageRoutine.Slot.BED){
			state = SLEEPING;
			return TICK;
		}
		if (OverworldLevel.heroDistance( ow, pos ) > VillageRoutine.NEAR) return TICK;
		if (idle > 0){
			idle--;
			if (plan.amble > 0 && Random.Int( 4 ) == 0) ambleNear( ow, destCell, plan.amble, true );
			return TICK;
		}
		idle = Random.IntRange( 6, 14 );
		flourish( ow, c, r, s );
		return TICK;
	}

	//a step to a free open cell within `radius` of `around` - and, keeping to the spot, one
	//that is still the spot's kind of place
	private void ambleNear( OverworldLevel ow, int around, int radius, boolean keep ){
		int step = pos + PathFinder.NEIGHBOURS8[Random.Int( 8 )];
		if (step < 0 || step >= ow.length() || !VillageRoutine.restsOn( ow, step )
				|| ow.distance( step, around ) > radius || Actor.findChar( step ) != null
				|| (keep && !VillageRoutine.keeps( ow, place, plan, step ))) return;
		moveSprite( pos, step );
		move( step );
	}

	//a gesture of the trade now and then, for whoever is watching: a fisher eyes the water, a
	//farmer stoops over the crop, a gatherer over the brush, a crafter sweeps the step, an elder
	//hums on the bench, a hunter looks out over the fence, the well's company turn to each
	//other. a facing, and a puff of something where the hero sees it
	private void flourish( OverworldLevel ow, VillageRoutine.Clock c, VillageRoutine.Role r, VillageRoutine.Slot s ){
		int w = ow.width();
		if (s == VillageRoutine.Slot.INDOORS){
			int door = ow.localCell( VillageRoutine.doorX( place, homeX, homeY ), VillageRoutine.doorY( place, homeX, homeY ) );
			if (door != -1) sprite.turnTo( pos, door );
			return;
		}
		if (s != VillageRoutine.Slot.WORK || plan.kind == VillageRoutine.Kind.WELL){
			int face = company( ow );
			if (face != -1) sprite.turnTo( pos, face );
			return;
		}
		switch (plan.kind){
			case SHORE: {
				int water = VillageRoutine.flourishCell( ow, pos, plan.kind );
				if (water == -1) return;
				sprite.turnTo( pos, water );
				if (ow.heroFOV[water]){
					GameScene.ripple( water );
					//a tug on the line
					if (Random.Int( 5 ) == 0) Splash.at( water, 0xCCE4F0, 4 );
				}
				return;
			}
			case FIELD: case BRUSH: {
				int crop = VillageRoutine.flourishCell( ow, pos, plan.kind );
				if (crop == -1) return;
				sprite.turnTo( pos, crop );
				if (ow.heroFOV[crop]) CellEmitter.get( crop ).burst( LeafParticle.GENERAL, 3 );
				return;
			}
			case STREET: {
				//out at the street, away from the house
				int front = pos - VillageRoutine.inX( place, homeX, homeY ) - VillageRoutine.inY( place, homeX, homeY ) * w;
				sprite.turnTo( pos, front );
				if (plan.amble > 0){
					//sweeping the step: dust, or the snow
					if (ow.heroFOV[front]){
						boolean snow = ow.frozenAt( front );
						CellEmitter.get( front ).burst( snow ? SnowParticle.RISING_FACTORY : EarthParticle.FALLING, snow ? 3 : 2 );
					}
				} else if (Random.Int( 3 ) == 0 && ow.heroFOV[pos]){
					//the elder on the bench, humming
					CellEmitter.get( pos ).burst( Speck.factory( Speck.NOTE ), 1 );
				}
				return;
			}
			case RING: {
				//out over the fence
				int out = pos + Integer.signum( ow.worldX() + pos % w - place.cx )
						+ Integer.signum( ow.worldY() + pos / w - place.cy ) * w;
				sprite.turnTo( pos, out );
				return;
			}
			default:
				return;
		}
	}

	//who it turns to: its partner at the well if they stand beside it, else anyone of the
	//settlement beside it, else the well itself (-1 when the well is off the window)
	private int company( OverworldLevel ow ){
		int other = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			Char ch = Actor.findChar( pos + n );
			if (!(ch instanceof Settler) || ((Settler) ch).settlementKey() != settlementKey()) continue;
			Settler o = (Settler) ch;
			if (member < 2 && o.house == house && o.member == 1 - member) return o.pos;
			if (other == -1) other = o.pos;
		}
		return other != -1 ? other : ow.localCell( place.cx, place.cy );
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );
		if (c != Dungeon.hero) return true;
		//bandits in the streets (RaidEvent): get inside! and in the days after a raid nobody beat
		//off, those keeping in are mending (a sleeper sleeps on: the 'z' says so)
		if (!gnoll() && state != SLEEPING && RaidEvent.ongoing( settlementKey() )){
			yell( Messages.get( this, "raid" ) );
			return true;
		}
		if (!gnoll() && state != SLEEPING && slot == VillageRoutine.Slot.INDOORS.ordinal() && RaidEvent.recovering( settlementKey() )){
			yell( Messages.get( this, "recovering" ) );
			return true;
		}
		OverworldLevel ow = surface();
		if (ow != null && place != null && slot >= 0 && slot < VillageRoutine.Slot.values().length){
			if (state == SLEEPING){
				yell( Messages.get( this, "asleep" ) );
				return true;
			}
			hush = 3;   //stops to talk
			VillageRoutine.Role spoken = VillageRoutine.Role.values()[role];
			//a fisher with no water to fish works the fields, and talks of them
			if (spoken == VillageRoutine.Role.FISHER && VillageRoutine.shores( ow, place, homeArea( ow ) ).length == 0){
				spoken = VillageRoutine.Role.FARMER;
			}
			String stem = VillageRoutine.chatStem( spoken, VillageRoutine.Slot.values()[slot], VillageRoutine.clock(), place.farms() );
			yell( Messages.get( this, stem + "_" + Random.Int( VillageRoutine.chatLines( stem ) ) ) );
			return true;
		}
		if (Shopkeeper.closedForNight()){
			yell( Messages.get( this, "asleep" ) );
			return true;
		}
		//one of several idle chatter lines
		yell( Messages.get( this, "line_" + Random.Int( idleLines() ) ) );
		return true;
	}

	private static final String HOUSE  = "routine_house";
	private static final String MEMBER = "routine_member";
	private static final String ROLE   = "routine_role";
	private static final String SLOT   = "routine_slot";
	private static final String HOME_X = "routine_home_x";
	private static final String HOME_Y = "routine_home_y";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( HOUSE, house );
		bundle.put( MEMBER, member );
		bundle.put( ROLE, role );
		bundle.put( SLOT, slot );
		bundle.put( HOME_X, homeX );
		bundle.put( HOME_Y, homeY );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		house  = bundle.contains( HOUSE )  ? bundle.getInt( HOUSE )  : -1;
		member = bundle.contains( MEMBER ) ? bundle.getInt( MEMBER ) : -1;
		role   = bundle.contains( ROLE )   ? bundle.getInt( ROLE )   : -1;
		slot   = bundle.contains( SLOT )   ? bundle.getInt( SLOT )   : -1;
		homeX  = bundle.contains( HOME_X ) ? bundle.getInt( HOME_X ) : Integer.MIN_VALUE;
		homeY  = bundle.contains( HOME_Y ) ? bundle.getInt( HOME_Y ) : Integer.MIN_VALUE;
	}
}

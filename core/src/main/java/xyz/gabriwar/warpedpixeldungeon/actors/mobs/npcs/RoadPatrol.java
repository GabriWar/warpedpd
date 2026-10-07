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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.OverworldBandit;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoadTraffic;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuardVariantSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/** One of the pair a big human town sends walking its road by day (RoadTraffic.beat): the
 *  whole road to the neighbouring village's well and back home, once or twice before dusk.
 *  They fall on any outlaw that shows itself near their beat. Nobody can hurt them, and an
 *  outlaw they cut down is nobody's kill (OverworldBandit.forfeit). */
public class RoadPatrol extends RoadWalker {

	{
		spriteClass = GuardVariantSprite.class;
	}

	//cells off their beat they will go after an outlaw
	private static final int LEASH = 12;

	//the town that sends them (packed sector), which of the pair (0 leads, 1 walks a cell and
	//a half behind), the livery, and the turns of the day each beat sets out
	public long homeSector = Long.MIN_VALUE;
	public int rank = 0;
	public int tint = 0;
	public int[] starts = new int[0];

	//rebuilt from route, starts and day on first use (not bundled)
	private RoadTraffic.Beat beat;
	private boolean chasing = false, spotted = false;

	/** A guard of the watch on this beat, ready to be set down on the road. */
	public static RoadPatrol of( RoadTraffic.Beat beat, long homeSector, int rank, int tint ){
		RoadPatrol p = new RoadPatrol();
		p.route = beat.route.clone();
		p.starts = beat.starts.clone();
		p.day = beat.day;
		p.homeSector = homeSector;
		p.rank = rank;
		p.tint = tint;
		return p;
	}

	private RoadTraffic.Beat beat(){
		if (beat == null) beat = new RoadTraffic.Beat( route, starts, day );
		return beat;
	}

	@Override
	public CharSprite sprite() {
		return new GuardVariantSprite( tint );
	}

	@Override
	public int attackSkill( Char target ) {
		return 30;
	}

	@Override
	public int damageRoll() {
		return Random.NormalIntRange( 4, 12 );
	}

	@Override
	protected boolean walk( OverworldLevel ow, long now ){
		int tod = RoadTraffic.turnOfDay( now );
		boolean out = RoadTraffic.day( now ) == day && !RoadTraffic.nightForced();
		float s = out ? beat().distanceAt( tod, rank ) : -1f;
		//where the beat has got to, a world point: past the window's edge it still is one
		float[] post = RoadTraffic.point( route, Math.max( 0f, s ) );

		if (fieldOfView == null || fieldOfView.length != ow.length()) fieldOfView = new boolean[ow.length()];
		ow.updateFieldOfView( this, fieldOfView );
		Char foe = foe( ow, post );
		if (foe != null){
			chasing = true;
			if (!spotted && ow.heroFOV[pos]){
				spotted = true;
				yell( Messages.get( this, "spotted" ) );
			}
			enemy = foe;
			if (ow.adjacent( pos, foe.pos )) return doAttack( foe );
			int old = pos;
			if (getCloser( foe.pos )){
				spend( 1 / speed() );
				return moveSprite( old, pos );
			}
			spend( TICK );
			return true;
		}
		if (chasing){
			chasing = false;
			path = null;
		}
		spotted = false;
		enemy = null;
		//indoors: back in at the door they came out of
		if (s < 0) return goIn( ow, 0f, -1 );
		return follow( ow, s, RoadTraffic.Beat.PACE, beat().heading( tod ) );
	}

	//the nearest outlaw in sight that strays within LEASH of where the beat has got to (world
	//point; one past the window's edge leaves nobody in reach): enemies only - a bandit the hero
	//has turned is left alone
	private Char foe( OverworldLevel ow, float[] post ){
		int px = Math.round( post[0] ), py = Math.round( post[1] );
		Char best = null;
		int bestDist = Integer.MAX_VALUE;
		for (Mob m : ow.mobs){
			if (!(m instanceof OverworldBandit) || m.alignment != Alignment.ENEMY || !m.isAlive()
					|| m.invisible > 0 || m.pos < 0 || m.pos >= fieldOfView.length || !fieldOfView[m.pos]) continue;
			if (Math.max( Math.abs( ow.worldX() + m.pos % ow.width() - px ),
					Math.abs( ow.worldY() + m.pos / ow.width() - py ) ) > LEASH) continue;
			int d = ow.distance( pos, m.pos );
			if (d < bestDist){
				best = m;
				bestDist = d;
			}
		}
		return best;
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );
		if (c != Dungeon.hero) return true;
		yell( line() );
		return true;
	}

	//a word on the road: the two villages the beat joins, by name, or a question about outlaws
	private String line(){
		int pick = Random.Int( 3 );
		if (pick < 2 && homeSector != Long.MIN_VALUE && Dungeon.level instanceof OverworldLevel){
			long seed = ((OverworldLevel) Dungeon.level).worldSeed();
			int sx = (int)(homeSector >> 32), sy = (int) homeSector;
			long nv = WorldStructures.roadNeighbour( seed, sx, sy );
			if (nv != Long.MIN_VALUE){
				return Messages.get( this, "line_" + pick,
						WorldStructures.villageName( seed, (int)(nv >> 32), (int) nv ), WorldStructures.villageName( seed, sx, sy ) );
			}
		}
		return Messages.get( this, "line_2" );
	}

	private static final String HOME_SECTOR = "home_sector";
	private static final String RANK        = "rank";
	private static final String TINT        = "tint";
	private static final String STARTS      = "starts";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( HOME_SECTOR, homeSector );
		bundle.put( RANK, rank );
		bundle.put( TINT, tint );
		bundle.put( STARTS, starts );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		homeSector = bundle.contains( HOME_SECTOR ) ? bundle.getLong( HOME_SECTOR ) : Long.MIN_VALUE;
		rank = bundle.contains( RANK ) ? bundle.getInt( RANK ) : 0;
		tint = bundle.contains( TINT ) ? bundle.getInt( TINT ) : 0;
		starts = bundle.contains( STARTS ) ? bundle.getIntArray( STARTS ) : new int[0];
		beat = null;
	}
}

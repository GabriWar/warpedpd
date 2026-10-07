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
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.RoadTraffic;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldStructures;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetDialogs;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CaravanTrainSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndQuest;
import com.watabou.noosa.Game;
import com.watabou.utils.Bundle;

/** A road's caravan on its morning leg (RoadTraffic.caravanLeg): the caravaneer on the bench of
 *  his covered cart, his mule in the shafts, driving from where yesterday's stall stood to where
 *  today's goes up. Keeps to the traffic clock like any walker; once the clock says he is there
 *  he pulls up and the stall is pitched where the cart stopped (OverworldLevel.caravanArrives),
 *  outlaws and all. Nobody can hurt him, and nobody falls on him on the road: the outlaws wait
 *  for the stall. */
public class CaravanTrain extends RoadWalker {

	{
		spriteClass = CaravanTrainSprite.class;
	}

	//turns a cart a hero is watching may take, once the leg is over, to pull up on its spot
	private static final int LINGER = 40;

	//the road it is on (RoadTraffic.roadHash), the villages it is walked from and toward
	//(sectors), and the turns of its day it set out and is due in
	public long road = Long.MIN_VALUE;
	public int fromSx, fromSy, toSx, toSy;
	public int depart, arrive;
	public float pace = RoadTraffic.CARAVAN_PACE;

	//not bundled: turns stood still after being hailed, and spent pulling up past the clock
	private int hush = 0, overdue = 0;

	/** The cart of a caravan's leg (RoadTraffic.caravanLeg), ready to be set down on the road. */
	public static CaravanTrain of( RoadTraffic.Trip leg, long road ){
		CaravanTrain t = new CaravanTrain();
		t.road = road;
		t.fromSx = leg.fromSx;
		t.fromSy = leg.fromSy;
		t.toSx = leg.toSx;
		t.toSy = leg.toSy;
		t.day = leg.day;
		t.depart = leg.depart;
		t.arrive = leg.arrive;
		t.pace = leg.pace;
		t.route = leg.route.clone();
		return t;
	}

	@Override
	public String name() {
		return Messages.get( this, "name" );
	}

	@Override
	public String description() {
		return Messages.get( this, "desc" );
	}

	@Override
	protected boolean walk( OverworldLevel ow, long now ){
		//a leg of another day: long over, the stall pitched or struck without him
		if (RoadTraffic.day( now ) != day){
			spend( TICK );
			vanish();
			return true;
		}
		int tod = RoadTraffic.turnOfDay( now );
		float len = RoadTraffic.length( route );
		if (tod >= arrive){
			//there: he pulls up and the stall goes up - at once where nobody sees, else once the
			//cart has drawn up on its spot (or kept the onlookers waiting long enough)
			int spot = ow.inWindow( route[2], route[3] ) ? ow.localCell( Math.round( route[2] ), Math.round( route[3] ) ) : -1;
			if (spot == -1 || !NetManager.anyHeroSees( pos ) || ow.distance( pos, spot ) <= 2 || ++overdue > LINGER){
				spend( TICK );
				int at = pos;
				vanish();
				ow.caravanArrives( this, at );
				return true;
			}
			return follow( ow, len, pace, 1 );
		}
		if (hush > 0){
			hush--;   //pulled up to talk
			spend( TICK );
			return true;
		}
		return follow( ow, Math.max( 0f, (tod - depart) * pace ), pace, 1 );
	}

	@Override
	public boolean interact( Char c ) {
		sprite.turnTo( pos, c.pos );
		if (sprite instanceof CaravanTrainSprite) ((CaravanTrainSprite) sprite).wave();
		hush = 3;
		if (!(Dungeon.level instanceof OverworldLevel)) return true;
		long seed = ((OverworldLevel) Dungeon.level).worldSeed();
		//where the stall goes up today: a little further on, toward the village he is bound for
		final String text = Messages.get( this, "line",
				WorldStructures.villageName( seed, toSx, toSy ), WorldStructures.villageName( seed, fromSx, fromSy ) );
		if (NetDialogs.handleNetHero( c, text )) return true;
		if (c != Dungeon.hero) return true;
		Game.runOnRenderThread( () -> GameScene.show( new WndQuest( CaravanTrain.this, text ) ) );
		return true;
	}

	private static final String ROAD    = "road";
	private static final String FROM_X  = "from_x";
	private static final String FROM_Y  = "from_y";
	private static final String TO_X    = "to_x";
	private static final String TO_Y    = "to_y";
	private static final String DEPART  = "depart";
	private static final String ARRIVE  = "arrive";
	private static final String PACE    = "pace";

	@Override
	public void storeInBundle( Bundle bundle ) {
		super.storeInBundle( bundle );
		bundle.put( ROAD, road );
		bundle.put( FROM_X, fromSx );
		bundle.put( FROM_Y, fromSy );
		bundle.put( TO_X, toSx );
		bundle.put( TO_Y, toSy );
		bundle.put( DEPART, depart );
		bundle.put( ARRIVE, arrive );
		bundle.put( PACE, pace );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ) {
		super.restoreFromBundle( bundle );
		road   = bundle.contains( ROAD )   ? bundle.getLong( ROAD )   : Long.MIN_VALUE;
		fromSx = bundle.contains( FROM_X ) ? bundle.getInt( FROM_X )  : 0;
		fromSy = bundle.contains( FROM_Y ) ? bundle.getInt( FROM_Y )  : 0;
		toSx   = bundle.contains( TO_X )   ? bundle.getInt( TO_X )    : 0;
		toSy   = bundle.contains( TO_Y )   ? bundle.getInt( TO_Y )    : 0;
		depart = bundle.contains( DEPART ) ? bundle.getInt( DEPART )  : 0;
		arrive = bundle.contains( ARRIVE ) ? bundle.getInt( ARRIVE )  : 0;
		pace   = bundle.contains( PACE )   ? bundle.getFloat( PACE )  : RoadTraffic.CARAVAN_PACE;
	}
}

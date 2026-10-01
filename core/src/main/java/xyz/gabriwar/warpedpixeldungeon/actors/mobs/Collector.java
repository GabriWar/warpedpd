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

package xyz.gabriwar.warpedpixeldungeon.actors.mobs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CollectorSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * The black market's answer to a customer who has made too much noise. Once the hero's
 * heat reaches WarpedRooms.HEAT_LIMIT one of these is waiting on every floor they walk
 * onto, and it does not look for them: it already knows where they are. It hits hard,
 * and every blow that lands lifts some of their gold. Killing it settles the debt - the
 * heat is gone, and what it was carrying, plus a share of everything the hero ever paid
 * the market, is on the floor.
 */
public class Collector extends Mob {

	{
		spriteClass = CollectorSprite.class;

		int threat = WarpedRooms.threat();
		HP = HT = 40 + threat * 7;
		defenseSkill = 10 + threat;
		EXP = 10 + threat;
		maxLvl = 30;

		baseSpeed = 1.2f;
		state = HUNTING;

		properties.add( Property.MINIBOSS );
	}

	//gold lifted off the hero, blow by blow
	private int carried = 0;
	private boolean announced = false;

	@Override
	public int damageRoll(){
		int threat = WarpedRooms.threat();
		return Random.NormalIntRange( 3 + threat, 8 + threat * 2 );
	}

	@Override
	public int attackSkill( Char target ){
		return 14 + WarpedRooms.threat();
	}

	@Override
	public int drRoll(){
		return super.drRoll() + Random.NormalIntRange( 0, 3 + WarpedRooms.threat() / 2 );
	}

	@Override
	protected boolean act(){
		Hero hero = Dungeon.hero;
		if (hero != null && hero.isAlive() && alignment == Alignment.ENEMY){
			if (!announced){
				announced = true;
				GLog.n( Messages.get( this, "arrive" ) );
				Sample.INSTANCE.play( Assets.Sounds.ALERT, 0.7f, 0.8f );
			}
			//no searching, no losing the trail: it walks straight at the debt
			if (state != HUNTING && state != FLEEING) state = HUNTING;
			if (enemy == null || !enemySeen) target = hero.pos;
		}
		return super.act();
	}

	@Override
	public int attackProc( Char enemy, int damage ){
		damage = super.attackProc( enemy, damage );
		if (enemy instanceof Hero && alignment == Alignment.ENEMY && Dungeon.gold > 0){
			int lifted = Math.min( Dungeon.gold, Math.max( 10 * WarpedRooms.threat(), Dungeon.gold / 12 ) );
			Dungeon.gold -= lifted;
			carried += lifted;
			GLog.w( Messages.get( this, "lifted", lifted ) );
		}
		return damage;
	}

	@Override
	public void die( Object cause ){
		//two fifths of all the hero ever paid the market, and every coin it took back
		int purse = carried + Math.round( WarpedRooms.goldSpent * 0.4f );
		carried = 0;
		WarpedRooms.settle();
		super.die( cause );
		if (purse > 0){
			Dungeon.level.drop( new Gold( purse ), pos ).sprite.drop();
		}
		GLog.p( Messages.get( this, "settled" ) );
	}

	@Override
	public float spawningWeight(){
		return 0f;
	}

	private static final String CARRIED   = "carried";
	private static final String ANNOUNCED = "announced";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CARRIED, carried );
		bundle.put( ANNOUNCED, announced );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		carried = bundle.getInt( CARRIED );
		announced = bundle.getBoolean( ANNOUNCED );
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.BlackMarket;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.BlackMarketDealer;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.Shopkeeper;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.MarketBodyguardSprite;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * Hired muscle at the black market. While the peace holds he is furniture with a pulse:
 * neutral, rooted to his post, and good for one gruff line. The moment anything in the
 * market is hurt - him, his partner or the dealer - the market turns on the hero and he
 * is an ordinary, and unusually heavy, enemy.
 */
public class MarketBodyguard extends Mob {

	{
		spriteClass = MarketBodyguardSprite.class;

		int threat = WarpedRooms.threat();
		HP = HT = 50 + threat * 9;
		defenseSkill = 8 + threat;
		EXP = 8 + threat;
		maxLvl = 30;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;
	}

	private boolean hostile = false;

	@Override
	public int damageRoll(){
		int threat = WarpedRooms.threat();
		return Random.NormalIntRange( 4 + threat, 10 + threat * 2 );
	}

	@Override
	public int attackSkill( Char target ){
		return 12 + WarpedRooms.threat();
	}

	@Override
	public int drRoll(){
		return super.drRoll() + Random.NormalIntRange( 0, 4 + WarpedRooms.threat() / 2 );
	}

	public void turnHostile(){
		if (hostile) return;
		hostile = true;
		alignment = Alignment.ENEMY;
		state = HUNTING;
		enemy = Dungeon.hero;
		target = Dungeon.hero.pos;
	}

	private void provoke(){
		BlackMarket market = BlackMarket.of( Dungeon.level );
		if (market != null) market.provoke();
		//a guard with no market left to answer to still answers for himself
		turnHostile();
	}

	@Override
	public void damage( int dmg, Object src ){
		if (!hostile && !(src instanceof Blob)) provoke();
		super.damage( dmg, src );
	}

	//as with the dealer: a cloud drifting over his post is weather, and the market does
	//not turn on the hero for it. He still takes what it does to him
	@Override
	public boolean add( Buff buff ){
		if (!hostile && buff.type == Buff.buffType.NEGATIVE
				&& !Shopkeeper.environmental( buff )
				&& !BlackMarketDealer.inHarmfulBlob( pos )
				&& Dungeon.level.heroFOV[pos]) provoke();
		return super.add( buff );
	}

	//the post is the job: he does not wander off it, and nothing calls him away
	@Override
	public void beckon( int cell ){
		if (hostile) super.beckon( cell );
	}

	@Override
	public boolean interact( Char c ){
		if (hostile) return super.interact( c );
		if (sprite != null) sprite.turnTo( pos, c.pos );
		if (c == Dungeon.hero){
			yell( Messages.get( this, Random.Int( 2 ) == 0 ? "line_a" : "line_b" ) );
		}
		return true;
	}

	@Override
	public float spawningWeight(){
		return 0f;
	}

	@Override
	public boolean reset(){
		return !hostile;
	}

	@Override
	public String description(){
		return Messages.get( this, hostile ? "desc_hostile" : "desc" );
	}

	private static final String HOSTILE = "hostile";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( HOSTILE, hostile );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		hostile = bundle.getBoolean( HOSTILE );
		if (hostile) alignment = Alignment.ENEMY;
	}
}

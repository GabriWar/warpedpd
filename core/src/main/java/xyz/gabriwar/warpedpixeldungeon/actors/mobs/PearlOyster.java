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
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.wetlands.Pearl;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.net.NetManager;
import xyz.gabriwar.warpedpixeldungeon.sprites.PearlOysterSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

/**
 * A pearl oyster settled in a beach's tide pool (levels/overworld/WetlandSites), now and then at
 * low water. It never moves and never fights: a hero beside it prises it open for its pearl, and
 * one time in PINCH it snaps shut on his fingers first (a nip, and he tries again). Whatever kills
 * it some other way leaves its pearl in the pool too.
 */
public class PearlOyster extends Mob {

	{
		spriteClass = PearlOysterSprite.class;

		HP = HT = 8;
		defenseSkill = 0;
		EXP = 1;
		maxLvl = 12;

		alignment = Alignment.NEUTRAL;
		state = PASSIVE;

		properties.add( Property.IMMOVABLE );
	}

	/** One try in this many it snaps shut instead. */
	public static final int PINCH = 3;

	public long poolsKey = 0;

	public static PearlOyster of( long poolsKey ){
		PearlOyster o = new PearlOyster();
		o.poolsKey = poolsKey;
		return o;
	}

	@Override
	public int drRoll(){
		//its shell
		return super.drRoll() + Random.NormalIntRange( 2, 6 );
	}

	@Override
	protected boolean act(){
		spend( TICK );
		return true;
	}

	@Override
	public boolean interact( Char c ){
		if (!(c instanceof Hero) || !isAlive()) return true;
		Hero hero = (Hero) c;
		if (hero.sprite != null) hero.sprite.operate( pos );
		if (Random.Int( PINCH ) == 0){
			hero.damage( Random.IntRange( 1, 3 ), this );
			if (sprite != null && sprite.parent != null) SpatialSound.play( Assets.Sounds.HIT_CRUSH, pos, 0.5f, 1.6f );
			NetManager.heroLog( hero, GLog.WARNING + Messages.get( this, "snap" ) );
		} else {
			NetManager.heroLog( hero, GLog.POSITIVE + Messages.get( this, "opened" ) );
			open();
		}
		hero.spendAndNext( 1f );
		return true;
	}

	//prised open: its pearl in the pool, the shell left empty
	private void open(){
		layPearl();
		if (sprite != null && sprite.parent != null && Dungeon.level.heroFOV[pos]){
			Splash.at( pos, 0xE8F2F6, 4 );
			SpatialSound.play( Assets.Sounds.WATER, pos, 0.6f, 1.3f );
		}
		destroy();
		if (sprite != null) sprite.killAndErase();
	}

	private void layPearl(){
		Heap h = Dungeon.level.drop( new Pearl(), pos );
		if (h.sprite != null) h.sprite.drop();
	}

	@Override
	public void die( Object cause ){
		layPearl();
		super.die( cause );
	}

	@Override
	public boolean reset(){
		return true;
	}

	private static final String POOLS = "oyster_pools";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( POOLS, poolsKey );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		poolsKey = bundle.contains( POOLS ) ? bundle.getLong( POOLS ) : 0;
	}
}

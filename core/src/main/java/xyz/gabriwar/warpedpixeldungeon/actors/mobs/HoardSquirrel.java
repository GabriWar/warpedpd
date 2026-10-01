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
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Blob;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Burning;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Chill;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drenched;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Frost;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Heatstroke;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hypothermia;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SnowedIn;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.warped.WarpedRooms;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.plants.Plant;
import xyz.gabriwar.warpedpixeldungeon.sprites.HoardSquirrelSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundlable;
import com.watabou.utils.Bundle;
import com.watabou.utils.Random;

import java.util.ArrayList;

/**
 * The owner of the Squirrel's Hoard, asleep on it through the winter. Left asleep it is
 * no trouble at all and the hoard is free for the taking - which is a stealth problem,
 * not a fight. Woken, it does what a squirrel does: grabs what it can carry of its seeds
 * (GRAB of them, from the heaps within reach), and runs, fast. Catch it and it drops them;
 * lose sight of it and both are gone for good.
 */
public class HoardSquirrel extends Mob {

	{
		spriteClass = HoardSquirrelSprite.class;

		int threat = WarpedRooms.threat();
		HP = HT = 6 + threat * 2;
		defenseSkill = 6 + threat;
		EXP = 1;
		maxLvl = 30;

		baseSpeed = 2f;

		FLEEING = new Fleeing();

		//it sleeps the winter through on its hoard: the cold it was made for must not
		//kill it before the hero ever gets there
		immunities.add( Hypothermia.class );
		immunities.add( Heatstroke.class );
		immunities.add( SnowedIn.class );
		immunities.add( Chill.class );
		immunities.add( Frost.class );
		immunities.add( Drenched.class );
		immunities.add( Burning.class );
	}

	private static final int GRAB = 2;
	private static final int REACH = 3;

	private ArrayList<Item> carried = new ArrayList<>();
	private boolean bolted = false;

	@Override
	public int damageRoll(){
		return Random.NormalIntRange( 1, 3 + WarpedRooms.threat() / 3 );
	}

	@Override
	public int attackSkill( Char target ){
		return 8 + WarpedRooms.threat();
	}

	@Override
	protected boolean act(){
		//anything but sleep means it is awake, and an awake squirrel has one plan
		if (!bolted && state != SLEEPING){
			bolted = true;
			grab();
			state = FLEEING;
		}
		return super.act();
	}

	private void grab(){
		for (Heap heap : Dungeon.level.heaps.valueList()){
			if (carried.size() >= GRAB) break;
			if (heap.type != Heap.Type.HEAP || Dungeon.level.distance( pos, heap.pos ) > REACH) continue;
			if (!(heap.peek() instanceof Plant.Seed)) continue;
			int at = heap.pos;
			carried.add( heap.pickUp() );
			if (Dungeon.level.heroFOV[at]) CellEmitter.get( at ).burst( Speck.factory( Speck.WOOL ), 2 );
		}
		if (!carried.isEmpty() && Dungeon.level.heroFOV[pos]){
			GLog.w( Messages.get( this, "snatch" ) );
		}
	}

	@Override
	public void rollToDropLoot(){
		for (Item item : carried){
			Dungeon.level.drop( item, pos ).sprite.drop();
		}
		carried.clear();
		super.rollToDropLoot();
	}

	//nor the weather or a gas drifting over the hollow
	@Override
	public boolean isImmune( Class effect ){
		return Blob.class.isAssignableFrom( effect ) || super.isImmune( effect );
	}

	@Override
	public float spawningWeight(){
		return 0f;
	}

	private class Fleeing extends Mob.Fleeing {
		@Override
		protected void escaped(){
			if (!Dungeon.level.heroFOV[pos] && Dungeon.level.distance( Dungeon.hero.pos, pos ) >= 6){
				if (!carried.isEmpty()) GLog.n( Messages.get( HoardSquirrel.class, "gone" ) );
				carried.clear();
				destroy();
				if (sprite != null) sprite.killAndErase();
			}
		}
	}

	private static final String CARRIED = "carried";
	private static final String BOLTED  = "bolted";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( CARRIED, carried );
		bundle.put( BOLTED, bolted );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		carried = new ArrayList<>();
		for (Bundlable b : bundle.getCollection( CARRIED )){
			carried.add( (Item) b );
		}
		bolted = bundle.getBoolean( BOLTED );
	}
}

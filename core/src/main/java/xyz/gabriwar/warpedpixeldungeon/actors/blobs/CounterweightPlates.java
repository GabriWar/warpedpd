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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.MeleeWeapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

/**
 * The mechanism of the Counterweight Vault. It marks the pressure plates, and every turn
 * it asks one thing of each: is there weight on it. A body is weight - the hero, another
 * player, a pet, a summoned thing, a shadow, a sheep, even an enemy that wandered onto
 * it. So is gear: a heap with a weapon or a suit of armor in it, a chest, a real pile of
 * gold (HEAVY_GOLD coins). Paper, glass and seeds are not.
 *
 * While every plate is held the vault's bars are up. When one is let go they come down
 * again - but never onto anything: not while somebody is inside the vault, and not while
 * a body or a heap lies in the gateway. Nobody is ever shut in.
 */
public class CounterweightPlates extends Blob {

	public static final int HEAVY_GOLD = 300;

	{
		//after everyone has moved this turn
		actPriority = MOB_PRIO - 1;
	}

	/** the gateway cell and the cells inside the vault, set by the room */
	public int gate = -1;
	public int[] vault = new int[0];

	@Override
	protected void evolve(){
		int plates = 0, held = 0;
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * Dungeon.level.width();
				if (!Dungeon.level.insideMap( cell )) continue;
				off[cell] = cur[cell];
				volume += off[cell];
				if (cur[cell] <= 0) continue;
				plates++;
				boolean down = weighted( cell );
				if (down) held++;
				WarpedRoomTiles.flip( Dungeon.level, cell, down );
			}
		}
		if (gate < 0 || plates == 0) return;

		boolean open = Dungeon.level.map[gate] != Terrain.CUSTOM_DECO;
		if (held == plates && !open){
			raise();
		} else if (held < plates && open && nothingInTheWay()){
			drop();
		}
	}

	private static boolean weighted( int cell ){
		Char ch = Actor.findChar( cell );
		if (ch != null && !ch.flying) return true;
		Heap heap = Dungeon.level.heaps.get( cell );
		if (heap == null) return false;
		if (heap.type != Heap.Type.HEAP && heap.type != Heap.Type.FOR_SALE) return true;
		for (Item item : heap.items){
			if (heavy( item )) return true;
		}
		return false;
	}

	public static boolean heavy( Item item ){
		return item instanceof MeleeWeapon || item instanceof Armor
				|| (item instanceof Gold && item.quantity() >= HEAVY_GOLD);
	}

	//nothing in the vault and nothing in the gateway: the bars may come down
	private boolean nothingInTheWay(){
		if (Actor.findChar( gate ) != null || Dungeon.level.heaps.get( gate ) != null) return false;
		for (int cell : vault){
			if (Actor.findChar( cell ) != null) return false;
		}
		return true;
	}

	private void raise(){
		Level.set( gate, Terrain.EMPTY_SP );
		WarpedRoomTiles.flip( Dungeon.level, gate, true );
		GameScene.updateMap( gate );
		Dungeon.observe();
		if (Dungeon.level.heroFOV[gate] || Dungeon.level.distance( gate, Dungeon.hero.pos ) <= 8){
			Sample.INSTANCE.play( Assets.Sounds.UNLOCK, 1f, 0.6f );
			Sample.INSTANCE.play( Assets.Sounds.ROCKS, 0.5f, 1.4f );
			GLog.p( Messages.get( this, "raised" ) );
		}
	}

	private void drop(){
		Level.set( gate, Terrain.CUSTOM_DECO );
		WarpedRoomTiles.flip( Dungeon.level, gate, false );
		GameScene.updateMap( gate );
		Dungeon.observe();
		if (Dungeon.level.heroFOV[gate] || Dungeon.level.distance( gate, Dungeon.hero.pos ) <= 8){
			Sample.INSTANCE.play( Assets.Sounds.ROCKS, 0.7f, 0.8f );
			GLog.w( Messages.get( this, "dropped" ) );
		}
	}

	private static final String GATE  = "gate";
	private static final String VAULT = "vault";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( GATE, gate );
		bundle.put( VAULT, vault );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		gate = bundle.getInt( GATE );
		vault = bundle.getIntArray( VAULT );
	}
}

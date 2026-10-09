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
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.WarpedRoomTiles;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;

/**
 * The lock on the Elemental Lock Vault. It marks the rune plots in front of the vault
 * and reads, every turn, the world on each of them - never what the hero did, only what
 * is physically so:
 *
 *   ember  the tile is at EMBER_TEMP or hotter, or open fire stands on it
 *   rime   the tile is at freezing or colder, or frost lies on it
 *   bloom  something is growing on it: a plant, or grass of any height
 *   tide   water lies on the plot's doorstep: any of the eight tiles around it
 *
 * A seed is the cheap key to each and a wand or a potion the dear one; the weather is
 * the free one, when it happens to be on the hero's side (a hard winter lights the rime
 * rune before they arrive). A rune that has woken stays awake. When every marked rune is
 * awake the gate is unmade and the lock with it.
 *
 * A marked cell holds its rune's kind (1-4); ten more once it is awake.
 */
public class ElementalLock extends Blob {

	public static final int EMBER = 1, RIME = 2, BLOOM = 3, TIDE = 4;
	private static final int AWAKE = 10;

	public static final float EMBER_TEMP = 60f;

	{
		//after fire, frost and water have had their turn
		actPriority = BLOB_PRIO - 1;
	}

	/** the cell of the vault's gate, set by the room */
	public int gate = -1;

	@Override
	protected void evolve(){
		int asleep = 0, runes = 0;
		int cell;
		for (int i = area.top - 1; i <= area.bottom; i++){
			for (int j = area.left - 1; j <= area.right; j++){
				cell = j + i * Dungeon.level.width();
				if (!Dungeon.level.insideMap( cell )) continue;
				int v = cur[cell];
				if (v > 0 && v < AWAKE && satisfied( v, cell )){
					v += AWAKE;
					wake( v - AWAKE, cell );
				}
				off[cell] = v;
				volume += v;
				if (v > 0){
					runes++;
					if (v < AWAKE) asleep++;
				}
			}
		}
		if (runes > 0 && asleep == 0) open();
	}

	private boolean satisfied( int kind, int cell ){
		Level level = Dungeon.level;
		switch (kind){
			case EMBER: {
				Blob fire = level.blobs.get( Fire.class );
				return (fire != null && fire.volume > 0 && fire.cur[cell] > 0)
						|| TileTemperature.tileTemp( cell ) >= EMBER_TEMP;
			}
			case RIME: {
				Blob frost = level.blobs.get( Freezing.class );
				return (frost != null && frost.volume > 0 && frost.cur[cell] > 0)
						|| TileTemperature.tileTemp( cell ) <= 0f;
			}
			case BLOOM: {
				int t = level.map[cell];
				return level.plants.get( cell ) != null || t == Terrain.GRASS
						|| t == Terrain.HIGH_GRASS || t == Terrain.FURROWED_GRASS;
			}
			case TIDE: {
				for (int ofs : PathFinder.NEIGHBOURS8){
					int n = cell + ofs;
					if (level.insideMap( n ) && level.map[n] == Terrain.WATER) return true;
				}
				return false;
			}
			default:
				return false;
		}
	}

	private void wake( int kind, int cell ){
		WarpedRoomTiles.flip( Dungeon.level, cell, true );
		if (!Dungeon.level.heroFOV[cell]) return;
		switch (kind){
			case EMBER: CellEmitter.get( cell ).burst( FlameParticle.FACTORY, 10 ); break;
			case RIME:  CellEmitter.get( cell ).burst( SnowParticle.FACTORY, 10 ); break;
			case BLOOM: CellEmitter.get( cell ).burst( LeafParticle.GENERAL, 10 ); break;
			case TIDE:  CellEmitter.get( cell ).burst( Speck.factory( Speck.BUBBLE ), 10 ); break;
		}
		SpatialSound.play( Assets.Sounds.TELEPORT, cell, 0.6f, 1.4f );
		GLog.p( Messages.get( this, "wake_" + kind ) );
	}

	private void open(){
		//the lock is done either way: clear the marks so this never runs again
		for (int i = 0; i < off.length; i++) off[i] = 0;
		volume = 0;
		if (gate < 0 || Dungeon.level.map[gate] != Terrain.CUSTOM_DECO) return;

		Level.set( gate, Terrain.EMPTY_SP );
		WarpedRoomTiles.remove( Dungeon.level, gate );
		GameScene.updateMap( gate );
		Dungeon.observe();

		if (Dungeon.level.heroFOV[gate]){
			CellEmitter.get( gate ).burst( Speck.factory( Speck.LIGHT ), 12 );
		}
		SpatialSound.play( Assets.Sounds.UNLOCK, gate );
		SpatialSound.play( Assets.Sounds.SECRET, gate );
		GLog.p( Messages.get( this, "opened" ) );
	}

	private static final String GATE = "gate";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( GATE, gate );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		gate = bundle.getInt( GATE );
	}
}

/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Regeneration;
import xyz.gabriwar.warpedpixeldungeon.effects.CellEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementRows;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

public class Blooming extends Weapon.Enchantment {
	
	private static ItemSprite.Glowing DARK_GREEN = new ItemSprite.Glowing( 0x008800 );
	
	@Override
	public int proc(Weapon weapon, Char attacker, Char defender, int damage) {
		int level = Math.max( 0, weapon.buffedLvl() );

		// lvl 0 - 33%
		// lvl 1 - 50%
		// lvl 2 - 60%
		float procChance = (level+1f)/(level+3f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			float plants = (1.5f + 0.1f*level) * powerMulti * power(); //scales with enchantment level
			if (Random.Float() < plants%1){
				plants = (float)Math.ceil(plants);
			} else {
				plants = (float)Math.floor(plants);
			}
			
			ArrayList<Integer> grown = new ArrayList<>();
			grow( attacker, defender, plants, grown );
			//its look on every cell it grew
			if (!grown.isEmpty()){
				int[] cells = new int[grown.size()];
				for (int i = 0; i < cells.length; i++) cells[i] = grown.get( i );
				EnchantFx.proc( EnchantLooks.of( getClass() ), attacker, defender, ElementRows.bloomStrength( cells.length, level(), powerMulti ),
						level(), Quality.isVestige( weapon, this ) ? EnchantFx.ECHO : 0, ElementRows.blooming( cells, defender.pos ) );
			}
			
		}
		
		return damage;
	}

	//grass grown on up to `plants` cells round the defender, its own first, the attacker's last;
	//each one grown noted in `grown`
	private void grow( Char attacker, Char defender, float plants, ArrayList<Integer> grown ){
		if (plantGrass(defender.pos)){
			grown.add(defender.pos);
			plants--;
			if (plants <= 0){
				return;
			}
		}
		
		ArrayList<Integer> positions = new ArrayList<>();
		for (int i : PathFinder.NEIGHBOURS8){
			if (defender.pos + i != attacker.pos) {
				positions.add(defender.pos + i);
			}
		}
		Random.shuffle( positions );

		//The attacker's position is always lowest priority
		if (Dungeon.level.adjacent(attacker.pos, defender.pos)){
			positions.add(attacker.pos);
		}

		for (int i : positions){
			if (plantGrass(i)){
				grown.add(i);
				plants--;
				if (plants <= 0) {
					return;
				}
			}
		}
	}
	
	private boolean plantGrass(int cell){
		int t = Dungeon.level.map[cell];
		if ((t == Terrain.EMPTY || t == Terrain.EMPTY_DECO || t == Terrain.EMBERS
				|| t == Terrain.GRASS || t == Terrain.FURROWED_GRASS)
				&& Dungeon.level.plants.get(cell) == null){
			if (!Regeneration.regenOn()){
				Level.set(cell, Terrain.FURROWED_GRASS);
			} else {
				Level.set(cell, Terrain.HIGH_GRASS);
			}
			GameScene.updateMap(cell);
			CellEmitter.get( cell ).burst( LeafParticle.LEVEL_SPECIFIC, 4 );
			return true;
		}
		return false;
	}
	
	@Override
	public ItemSprite.Glowing glowing() {
		return DARK_GREEN;
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.LeafParticle;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite.Glowing;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.utils.Bundle;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

//ported from SPS-PD's earth enchantment: plants a parasitic seed on the
//target which saps its health, healing adjacent enemies of the target
public class Parasitic extends Weapon.Enchantment {

	private static ItemSprite.Glowing OLIVE = new ItemSprite.Glowing( 0x88CC44 );

	@Override
	public int proc( Weapon weapon, Char attacker, Char defender, int damage ) {

		int level = Math.max( 0, weapon.buffedLvl() );

		// lvl 0 - 11%
		// lvl 1 - 20%
		// lvl 2 - 27%
		float procChance = (level+1f)/(level+9f) * procChanceMultiplier(attacker);
		if (Random.Float() < procChance) {

			float powerMulti = Math.max(1f, procChance);

			int seedDmg = Random.NormalIntRange( 1, 2 + Dungeon.scalingDepth()/6 );
			seedDmg = Math.max( 1, Math.round( seedDmg * powerMulti * power() )); //scales with enchantment level

			Buff.affect( defender, ParasiticSeed.class ).set( ParasiticSeed.DURATION, seedDmg );
			defender.sprite.emitter().burst( LeafParticle.GENERAL, 5 );

		}

		return damage;
	}

	@Override
	public Glowing glowing() {
		return OLIVE;
	}

	public static class ParasiticSeed extends Buff {

		public static final float DURATION = 5f;

		{
			type = buffType.NEGATIVE;
			announced = true;
		}

		private float left;
		private int dmg;

		public void set( float duration, int dmg ) {
			left = Math.max( left, duration );
			this.dmg = Math.max( this.dmg, dmg );
		}

		@Override
		public boolean act() {
			if (target.isAlive()) {

				target.damage( dmg, this );

				//the seed feeds the target's enemies around it
				for (int i : PathFinder.NEIGHBOURS8) {
					Char ch = Actor.findChar( target.pos + i );
					if (ch != null && ch.isAlive()
							&& ch.alignment != target.alignment
							&& ch.HP < ch.HT) {
						int heal = Math.min( dmg, ch.HT - ch.HP );
						ch.HP += heal;
						ch.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), FloatingText.HEALING );
					}
				}

				spend( TICK );
				left -= TICK;
				if (left <= 0) {
					detach();
				}

			} else {
				detach();
			}

			return true;
		}

		@Override
		public int icon() {
			return BuffIndicator.HERB_HEALING;
		}

		@Override
		public String desc() {
			return Messages.get(this, "desc", dmg, dispTurns(left));
		}

		private static final String LEFT = "left";
		private static final String DMG  = "dmg";

		@Override
		public void storeInBundle( Bundle bundle ) {
			super.storeInBundle( bundle );
			bundle.put( LEFT, left );
			bundle.put( DMG, dmg );
		}

		@Override
		public void restoreFromBundle( Bundle bundle ) {
			super.restoreFromBundle( bundle );
			left = bundle.getFloat( LEFT );
			dmg = bundle.getInt( DMG );
		}
	}
}

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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.actors.hero.skills;


import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FlavourBuff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.Bundle;

public class Demoralize extends Skill {

	{
		tag = "D2";
		name = "Demoralize";
		castText = "Break their nerve";
		image = 6;
		tier = 2;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (ranged || level <= 0 || enemy == null || !enemy.isAlive()) return damage;
		Demoralized debuff = enemy.buff(Demoralized.class);
		if (debuff == null) debuff = Buff.affect(enemy, Demoralized.class);
		debuff.factor = 1f - (level == 1 ? .05f : level == 2 ? .10f : .20f);
		debuff.hits++;
		Buff.prolong(enemy, Demoralized.class, 20f);
		if (enemy.sprite != null){
			enemy.sprite.emitter().burst(Speck.factory(Speck.SCREAM), 1);
			new Flare(4 + level, 10 + 3 * level).color(0x664488, true).show(enemy.sprite, .45f);
		}
		if (level >= MAX_LEVEL && debuff.hits >= 5){
			// onHitProc runs just before Char.attack applies damage; Terror recovers
			// five time units whenever that damage lands, so prime it with 10 to
			// leave the intended 5-turn duration after the triggering hit.
			Terror terror = Buff.prolong(enemy, Terror.class, 10f);
			terror.object = Dungeon.hero.id();
			debuff.hits = 0;
			if (enemy.buff(Terror.class) != null){
				if (enemy.sprite != null){
					enemy.sprite.showStatus(CharSprite.NEGATIVE, "TERRIFIED");
					enemy.sprite.emitter().burst(Speck.factory(Speck.SCREAM), 1);
					new Flare(10, 24).color(0xCC3355, true).show(enemy.sprite, .8f);
				}
			}
		}
		return damage;
	}

	@Override public void onHeroAttackMiss(Char enemy, boolean ranged){
		if (enemy != null){
			Demoralized debuff = enemy.buff(Demoralized.class);
			if (debuff != null) debuff.hits = 0;
		}
	}

	public static class Demoralized extends FlavourBuff {
		public float factor = .95f;
		public int hits;
		@Override public void storeInBundle(Bundle b){ super.storeInBundle(b); b.put("factor", factor); b.put("hits", hits); }
		@Override public void restoreFromBundle(Bundle b){ super.restoreFromBundle(b); factor=b.getFloat("factor"); hits=b.getInt("hits"); }
	}
}

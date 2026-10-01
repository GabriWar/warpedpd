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

package xyz.gabriwar.warpedpixeldungeon.items.wands;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.WandEmpower;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import com.watabou.noosa.audio.Sample;

//for wands that directly damage a target
//wands with AOE or circumstantial direct damage count here (e.g. fireblast, transfusion), but wands with indirect damage do not (e.g. corrosion)
public abstract class DamageWand extends Wand{

	public int min(){
		return minAt(buffedLvl());
	}

	public abstract int min(int lvl);

	public int max(){
		return maxAt(buffedLvl());
	}

	public abstract int max(int lvl);

	/** the levelled number with the wand's rarity worked in; the subclasses override
	 *  min(int)/max(int), so the multiplier is taken here where nothing does */
	public int minAt( int lvl ){
		return xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality.scale( this, xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine.ZAP_DAMAGE, min(lvl) );
	}

	public int maxAt( int lvl ){
		return xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality.scale( this, xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine.ZAP_DAMAGE, max(lvl) );
	}

	public int damageRoll(){
		return damageRoll(buffedLvl());
	}

	public int damageRoll(int lvl){
		int dmg = Hero.heroDamageIntRange(minAt(lvl), maxAt(lvl));
		//skill tree: Mage's Sorcerer - wands hit harder
		dmg = Math.round(dmg * Dungeon.hero.heroSkills.allWandDamage());
		WandEmpower emp = Dungeon.hero.buff(WandEmpower.class);
		if (emp != null){
			dmg += emp.dmgBoost;
			emp.left--;
			if (emp.left <= 0) {
				emp.detach();
			}
			Sample.INSTANCE.play(Assets.Sounds.HIT_STRONG, 0.75f, 1.2f);
		}
		return dmg;
	}

	@Override
	public String statsDesc() {
		if (levelKnown)
			return Messages.get(this, "stats_desc", min(), max());
		else
			return Messages.get(this, "stats_desc", min(0), max(0));
	}

	@Override
	public String upgradeStat1(int level) {
		return min(level) + "-" + max(level);
	}
}

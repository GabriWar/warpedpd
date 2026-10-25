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

package xyz.gabriwar.warpedpixeldungeon.items.trinkets;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.DamageLedger;
import xyz.gabriwar.warpedpixeldungeon.actors.ledger.Step;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.GearPerk;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;

/**
 * The relic of the buried temples (levels/Delves: a temple's guardian keeps it): a golden scarab
 * rolling a red sun before it, the sun the temples' dead were laid to rest under. Carried, the
 * hero's blows land harder on the undead, the more the higher it is. Never found anywhere else,
 * so every temple cleared gives one.
 */
public class SunScarab extends Trinket {

	{
		image = ItemSpriteSheet.SUN_SCARAB;
	}

	@Override
	protected int upgradeEnergyCost(){
		//6 -> 6(12) -> 8(20) -> 10(30), as most trinkets
		return 6 + 2 * level();
	}

	@Override
	public String statsDesc(){
		int lvl = isIdentified() ? buffedLvl() : 0;
		return Messages.get( this, isIdentified() ? "stats_desc" : "typical_stats_desc",
				Math.round( 100 * (undeadFactor( lvl ) - 1f) ) );
	}

	/** How much harder its carrier hits the undead at a level (-1 for none carried): 1 for none. */
	public static float undeadFactor( int level ){
		return level < 0 ? 1f : 1.15f + 0.1f * level;
	}

	/** Hero.attackProc: a blow of its carrier's on one of the undead, made harder. */
	public static int heroHit( Hero hero, Char enemy, int damage ){
		if (enemy == null || hero.belongings == null || !enemy.properties().contains( Char.Property.UNDEAD )) return damage;
		SunScarab scarab = hero.belongings.getItem( SunScarab.class );
		if (scarab == null) return damage;
		float f = undeadFactor( GearPerk.trinketLevel( scarab, scarab.buffedLvl() ) );
		return DamageLedger.mulRound( enemy, Step.ATTACKER, scarab, damage, f, Math.round( damage * f ) );
	}

	@Override
	public int value(){
		return 150;
	}
}

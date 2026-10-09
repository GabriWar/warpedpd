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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Badges;
import xyz.gabriwar.warpedpixeldungeon.Challenges;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.artifacts.MasterThievesArmband;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;


public class Bandit extends PassiveSkillA1 {

	{
		name = "Bandit";
		image = 49;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//a surprise blow picks the mark's pocket, once per enemy, and the take flies to you
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || (ranged && level < Skill.MAX_LEVEL)
				|| !(enemy instanceof Mob) || !enemy.isAlive() || enemy.alignment != Char.Alignment.ENEMY
				|| enemy.buff( Pickpocketed.class ) != null || !((Mob) enemy).surprisedBy( hero )){
			return damage;
		}
		Mob mark = (Mob) enemy;
		Buff.affect( mark, Pickpocketed.class );

		if (mark.sprite != null){
			mark.sprite.emitter().burst( Speck.factory( Speck.COIN ), 5 );
			mark.sprite.showStatus( CharSprite.NEUTRAL, Messages.get( Bandit.class, "pickpocket" ) );
		}
		SpatialSound.play( Assets.Sounds.MISS, mark, 1f, 1.5f );

		final Item loot = level >= 2 ? snatchLoot( mark ) : null;
		MasterThief.pocket( MasterThief.COINS_PER_PILE );
		if (loot == null || loot instanceof Gold){
			int purse = loot != null ? loot.quantity() : Random.IntRange( 3, 6 + 2 * Dungeon.depth );
			Dungeon.gold += purse;
			Statistics.goldCollected += purse;
			Badges.validateGoldCollected();
			if (hero.sprite != null){
				hero.sprite.showStatusWithIcon( CharSprite.NEUTRAL, Integer.toString( purse ), FloatingText.GOLD );
			}
			SkillFX.streak( mark.pos, hero.pos, new Gold(), () -> {
				if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.COIN ), 4 );
				SpatialSound.play( Assets.Sounds.GOLD, hero, 1f, 1.2f );
			} );
		} else {
			//the item sails over and lands at your feet, to be picked up like any other
			GLog.i( Messages.get( Bandit.class, "snatch", loot.name() ) );
			final int cell = hero.pos;
			SkillFX.streak( mark.pos, cell, loot, () -> {
				Dungeon.level.drop( loot, cell ).sprite.drop();
				if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.STAR ), 4 );
				SpatialSound.play( Assets.Sounds.ITEM, cell, 1f, 1.1f );
			} );
		}
		return damage;
	}

	//the mark's own drop, rolled at its usual chance; taking it means it will not drop again
	private static Item snatchLoot( Mob mark ){
		if (mark.properties().contains( Char.Property.BOSS )
				|| Dungeon.hero.lvl > mark.maxLvl + 2
				|| mark.buff( MasterThievesArmband.StolenTracker.class ) != null
				|| Random.Float() >= mark.lootChance()){
			return null;
		}
		Item loot = mark.createLoot();
		if (loot == null || Challenges.isItemBlocked( loot )) return null;
		Buff.affect( mark, MasterThievesArmband.StolenTracker.class ).setItemStolen( true );
		return loot;
	}

	/** this enemy's pocket is already empty */
	public static class Pickpocketed extends Buff {
		@Override
		public int icon(){ return BuffIndicator.NONE; }
	}
}

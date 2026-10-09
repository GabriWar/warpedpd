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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Blindness;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.MasterThiefCoins;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;

public class MasterThief extends Skill {

	/** coins each pile of gold sets circling */
	public static final int COINS_PER_PILE = 2;
	public static final float BLIND_TURNS = 2f;

	{
		tag = "PA4";
		name = "Master Thief";
		image = 70;
		tier = 4;
	}

	@Override
	protected boolean upgrade(){ return true; }

	public int maxCoins(){ return 2 * level; }

	/** a pocket picked (Bandit) palms coins into the orbit too */
	public static void pocket( int coins ){
		Hero hero = Dungeon.hero;
		if (hero == null || hero.heroSkills == null) return;
		MasterThief skill = hero.heroSkills.get( MasterThief.class );
		if (skill == null || skill.level <= 0) return;
		Buff.affect( hero, MasterThiefCoins.class ).load( coins, skill.maxCoins() );
		if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.COIN ), 3 );
	}

	//picking up gold never adds gold here: it loads the orbit
	@Override
	public int lootBonus( int gold ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || gold <= 0) return 0;
		Buff.affect( hero, MasterThiefCoins.class ).load( COINS_PER_PILE, maxCoins() );
		if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.COIN ), 4 );
		SpatialSound.play( Assets.Sounds.GOLD, hero, 0.8f, 1.5f );
		return 0;
	}

	//whoever lands a blow on you catches a coin in the eyes
	@Override
	public int onDefendProc( Char enemy, int damage ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || enemy == null || !enemy.isAlive()
				|| enemy.alignment != Char.Alignment.ENEMY) return damage;
		MasterThiefCoins coins = hero.buff( MasterThiefCoins.class );
		if (coins == null || coins.coins() <= 0) return damage;
		coins.spendCoin();

		Buff.prolong( enemy, Blindness.class, BLIND_TURNS );
		SpatialSound.play( Assets.Sounds.GOLD, hero, 1f, 1.7f );
		if (hero.sprite != null){
			SkillFX.streak( hero.sprite, enemy.pos, new Gold(), () -> struck( enemy ) );
		} else {
			struck( enemy );
		}

		//at mastery the coin skips off into a second face
		if (level >= Skill.MAX_LEVEL){
			for (Char other : Actor.chars()){
				if (other != enemy && other != hero && other.isAlive() && other.alignment == Char.Alignment.ENEMY
						&& Dungeon.level.distance( enemy.pos, other.pos ) <= 2
						&& SkillInteractions.clear( enemy.pos, other.pos )){
					Buff.prolong( other, Blindness.class, BLIND_TURNS );
					final Char second = other;
					SkillFX.streak( enemy.pos, other.pos, new Gold(), () -> struck( second ) );
					break;
				}
			}
		}
		return damage;
	}

	private static void struck( Char ch ){
		if (ch.sprite != null) ch.sprite.emitter().burst( Speck.factory( Speck.COIN ), 4 );
		SpatialSound.play( Assets.Sounds.HIT, ch, 0.7f, 1.6f );
	}
}

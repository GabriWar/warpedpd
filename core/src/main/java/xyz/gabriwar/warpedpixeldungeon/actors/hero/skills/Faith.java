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


import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

public class Faith extends PassiveSkillA1 {

	//answered prayer: once per floor, a blow that leaves you under half your health refills mana
	//at max rank the golden light also turns aside part of that blow
	private static final int PRAYER_BLOCK = 6;

	private static final String ANSWERED_DEPTH  = "faith_answered_depth";
	private static final String ANSWERED_BRANCH = "faith_answered_branch";

	private int answeredDepth = -1;
	private int answeredBranch = -1;

	{
		name = "Faith";
		image = 103;
		tier = 1;
	}

	//ranks no longer grant max mana; old saves simply keep what earlier ranks already added
	@Override
	protected boolean upgrade(){
		return true;
	}

	/** 8 / 12 / 16 mana */
	private int prayerMana(){
		return 4 + 4 * level;
	}

	//Faith sits before Sanctuary and Last Rites in the branch, so the mana it answers with
	//is already there for them to spend on the same hit. poison, burning and hunger are not blows
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || damage <= 0 || source instanceof Buff.DOTbuff || source instanceof Hunger)
			return 0;
		if ((hero.HP - damage) * 2 >= hero.HT)
			return 0;
		if (answeredDepth == Dungeon.depth && answeredBranch == Dungeon.branch)
			return 0;
		int maxMana = hero.MT + RingOfMagic.manaBonus( hero );
		int gain = Math.max( 0, Math.min( prayerMana(), maxMana - hero.MP ) );
		boolean blocks = level >= MAX_LEVEL;
		if (gain <= 0 && !blocks)
			return 0;

		answeredDepth = Dungeon.depth;
		answeredBranch = Dungeon.branch;
		hero.MP += gain;

		if (hero.sprite != null){
			new Flare( 6, 32 ).color( 0xFFEE88, true ).show( hero.sprite, 1f );
			hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 8 );
			if (gain > 0) hero.sprite.showStatus( 0x8ac0ff, "+" + gain );
			if (blocks) hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
		}
		Sample.INSTANCE.play( Assets.Sounds.CHARMS, 1f, 1.3f );
		GLog.p( Messages.get( this, "answered" ) );
		return blocks ? Math.min( PRAYER_BLOCK, damage ) : 0;
	}

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( ANSWERED_DEPTH, answeredDepth );
		bundle.put( ANSWERED_BRANCH, answeredBranch );
	}

	@Override
	public void restoreInBundle( Bundle bundle ){
		super.restoreInBundle( bundle );
		answeredDepth = bundle.contains( ANSWERED_DEPTH ) ? bundle.getInt( ANSWERED_DEPTH ) : -1;
		answeredBranch = bundle.contains( ANSWERED_BRANCH ) ? bundle.getInt( ANSWERED_BRANCH ) : -1;
	}
}

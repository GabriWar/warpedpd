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
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.Bundle;

public class Conditioning extends PassiveSkillA1 {

	private static final String SAVED_DEPTH  = "conditioning_depth";
	private static final String SAVED_BRANCH = "conditioning_branch";

	private static final String RALLIES = "conditioning_rallies";

	//the floor the rallies were last spent on; -1 while none has been spent
	private int savedDepth = -1;
	private int savedBranch = -1;
	private int rallies = 0;

	{
		name = "Conditioning";
		image = 79;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	//once per floor (twice at +3), a blow that leaves you under a third of your health makes the body
	//rally, healing 10% / 20% / 30% of max health. It never answers a killing blow: that is Last Rites'.
	//starvation and damage over time never trigger it, so it cannot be spent or abused on them
	@Override
	public int incomingDamageReduction( int damage, Object source ){
		Hero hero = Dungeon.hero;
		if (level <= 0 || hero == null || damage <= 0
				|| source instanceof Buff.DOTbuff || source instanceof Hunger) return 0;
		if (savedDepth != Dungeon.depth || savedBranch != Dungeon.branch) rallies = 0;
		if (rallies >= (level >= MAX_LEVEL ? 2 : 1)) return 0;
		int remaining = hero.HP + hero.shielding() - damage;
		if (remaining >= hero.HT / 3 || remaining < 1) return 0;
		savedDepth = Dungeon.depth;
		savedBranch = Dungeon.branch;
		rallies++;

		int heal = Math.max( 1, Math.round( hero.HT * 0.1f * level ) );
		hero.HP = Math.min( hero.HT, hero.HP + heal );

		if (hero.sprite != null){
			hero.sprite.showStatus( CharSprite.POSITIVE, Messages.get( this, "rally", heal ) );
			hero.sprite.emitter().burst( Speck.factory( Speck.HEALING ), 4 + 2 * level );
		}
		Sample.INSTANCE.play( Assets.Sounds.DRINK, 1f, 1.2f );
		return 0;
	}

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( SAVED_DEPTH, savedDepth );
		bundle.put( SAVED_BRANCH, savedBranch );
		bundle.put( RALLIES, rallies );
	}

	@Override
	public void restoreInBundle( Bundle bundle ){
		super.restoreInBundle( bundle );
		//saves from before the rally existed have not spent it anywhere
		savedDepth = bundle.contains( SAVED_DEPTH ) ? bundle.getInt( SAVED_DEPTH ) : -1;
		savedBranch = bundle.contains( SAVED_BRANCH ) ? bundle.getInt( SAVED_BRANCH ) : -1;
		//saves from before the second rally spent their one rally if a floor is recorded
		rallies = bundle.contains( RALLIES ) ? bundle.getInt( RALLIES ) : (savedDepth >= 0 ? 1 : 0);
	}
}

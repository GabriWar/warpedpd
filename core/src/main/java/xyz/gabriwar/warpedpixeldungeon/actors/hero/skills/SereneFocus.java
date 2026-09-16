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

import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Slow;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;


public class SereneFocus extends Skill {

	{
		tag = "PA5A";
		name = "Serene Focus";
		tier = 4;
		image = 38;
		level = 0;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** Wand's zapper calls this once the bolt has landed, with whoever stood where it was aimed.
	 *  A steady mind (mana at least three quarters full) makes the zap slow what it hit */
	public static void onZap( Char target ){
		Hero hero = Dungeon.hero;
		if (hero == null || hero.heroSkills == null) return;
		SereneFocus skill = hero.heroSkills.get( SereneFocus.class );
		if (skill == null || skill.level <= 0) return;
		if (target == null || target == hero || !target.isAlive() || target.alignment != Char.Alignment.ENEMY) return;
		int effectiveMT = hero.MT + RingOfMagic.manaBonus( hero );
		if (hero.MP * 4 < effectiveMT * 3) return;

		float duration = 1 + skill.level;
		calm( target, duration );
		if (skill.level >= MAX_LEVEL){
			for (int n : PathFinder.NEIGHBOURS8){
				Char ch = Actor.findChar( target.pos + n );
				if (ch != null && ch != hero && ch.isAlive() && ch.alignment == Char.Alignment.ENEMY) calm( ch, duration );
			}
		}
		if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
		Sample.INSTANCE.play( Assets.Sounds.MELD, 0.6f, 1.4f );
	}

	private static void calm( Char ch, float duration ){
		Buff.prolong( ch, Slow.class, duration );
		if (ch.sprite != null && Dungeon.level.heroFOV[ch.pos]){
			ch.sprite.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 5 );
		}
	}
}

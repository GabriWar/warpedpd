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
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SlipstreamWind;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.WindParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Haste;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;

/**
 * A burst of running that leaves swirls of wind on the tiles behind you; enemies who
 * step into one are tripped. At mastery a second cast rides the wind back to the far
 * end of the trail, tripping every enemy along it.
 */
public class Slipstream extends SubSkill1 {

	public static final float HASTE = 6f;

	{
		name = "Slipstream";
		castText = "Catch me!";
		image = 192;
		mana = 8;
		tier = 1;
	}

	private SlipstreamWind wind( Hero hero ){
		SlipstreamWind wind = hero.buff( SlipstreamWind.class );
		return wind != null && wind.sameFloor() ? wind : null;
	}

	private boolean canSnap( Hero hero ){
		SlipstreamWind wind = wind( hero );
		return level >= MAX_LEVEL && wind != null && !wind.isEmpty();
	}

	@Override
	public boolean toggleable(){ return false; }

	@Override
	public ArrayList<String> actions( Hero hero ){
		ArrayList<String> actions = new ArrayList<>();
		if (level > 0 && (canSnap( hero ) || hero.MP >= getManaCost()))
			actions.add(AC_CAST);
		return actions;
	}

	@Override
	public void execute( Hero hero, String action ){
		if (!action.equals(Skill.AC_CAST) || level <= 0) return;
		if (canSnap( hero )){
			snap( hero );
			return;
		}
		if (hero.MP < getManaCost()) return;
		Buff.affect( hero, SlipstreamWind.class ).set( level );
		Buff.prolong( hero, Haste.class, HASTE );
		hero.MP -= getManaCost();
		castTextYell();
		Sample.INSTANCE.play( Assets.Sounds.MISS, 1f, 1.5f );
		hero.sprite.emitter().burst( WindParticle.FACTORY, 12 );
		hero.sprite.emitter().burst( Speck.factory( Speck.DUST ), 6 );
		hero.heroSkills.lastUsed = this;
		hero.spend( TIME_TO_USE );
		hero.busy();
		hero.sprite.operate( hero.pos );
	}

	//the wind rushes back along the trail and carries you to its far end
	private void snap( Hero hero ){
		SlipstreamWind wind = wind( hero );
		int dest = hero.rooted ? -1 : wind.snapCell( hero );
		if (dest < 0){
			GLog.w( Messages.get( Slipstream.class, "no_snap" ) );
			return;
		}
		hero.heroSkills.lastUsed = this;
		boolean line = SkillInteractions.clear( hero.pos, dest );
		wind.gust( hero );
		Sample.INSTANCE.play( Assets.Sounds.PUFF, 1f, 0.7f );
		hero.busy();
		final int from = hero.pos;
		if (line && hero.sprite.parent != null){
			hero.sprite.jump( from, dest, 0f, 0.05f * Math.max( 1, Dungeon.level.distance( from, dest ) ), () -> arrive( hero, dest ) );
		} else {
			arrive( hero, dest );
		}
	}

	private void arrive( Hero hero, int dest ){
		if (hero.isAlive() && Actor.findChar( dest ) == null){
			hero.move( dest, false );
			Dungeon.observe();
			GameScene.updateFog();
		}
		hero.sprite.place( hero.pos );
		hero.sprite.emitter().burst( WindParticle.FACTORY, 10 );
		hero.spendAndNext( TIME_TO_USE );
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.5 * level));
	}

	@Override
	protected boolean upgrade(){ return true; }
}

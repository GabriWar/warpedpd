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

import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.DreadFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;


public class SoulDrain extends SubSkill1 {

	{
		name = "Soul Drain";
		image = 185;
		tier = 1;
	}

	//a passive: nothing to switch on, so it stays out of the quick panel
	@Override
	public boolean toggleable(){ return false; }

	@Override
	public java.util.ArrayList<String> actions( xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero ){
		return new java.util.ArrayList<>();
	}

	@Override
	protected boolean upgrade(){ return true; }

	/** Motes of soul peeled out of the dead body into the flying soul. */
	public static final int PEEL = 6;

	//a soul-marked enemy's soul does not rest: it flies to the nearest enemy in reach and marks it too.
	//at mastery it passes through the Warlock on the way and mends him
	@Override
	public void onEnemyDeath( xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob mob, Object cause ){
		xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero hero = xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;
		if (level <= 0 || hero == null || mob == null
				|| mob.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark.class ) == null) return;
		final int from = mob.pos;
		xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob next = null;
		for (xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob m : xyz.gabriwar.warpedpixeldungeon.Dungeon.level.mobs.toArray( new xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob[0] )){
			if (m == mob || !m.isAlive() || m.alignment != xyz.gabriwar.warpedpixeldungeon.actors.Char.Alignment.ENEMY
					|| !xyz.gabriwar.warpedpixeldungeon.Dungeon.level.heroFOV[m.pos]
					|| m.buff( xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark.class ) != null
					|| xyz.gabriwar.warpedpixeldungeon.Dungeon.level.distance( from, m.pos ) > 2 + level) continue;
			if (next == null || xyz.gabriwar.warpedpixeldungeon.Dungeon.level.distance( from, m.pos )
					< xyz.gabriwar.warpedpixeldungeon.Dungeon.level.distance( from, next.pos )) next = m;
		}
		final xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob target = next;
		final boolean mend = level >= MAX_LEVEL;
		if (target == null && !mend) return;
		SkillInteractions.defer( () -> {
			//the Warlock is mended below (he is hurt and alive): its picture waits for the soul to reach him
			final boolean mends = mend && hero.isAlive() && hero.HP < hero.HT;
			//the soul flies on, swaying, a mote of soul light (never a shadow bolt), peeled out of the
			//body it leaves: to the next it marks, closing a ring on its feet as it lands, or home
			//through the Warlock
			if (hero.sprite != null && hero.sprite.parent != null){
				SkillBolt.fly( hero.sprite.parent, from, mend ? hero.pos : target.pos, SkillBolt.SOUL, () -> {
					if (!mend && target.sprite != null && target.isAlive()){
						ClosingRing.on( target.sprite, 23, 9, 0.3f, DreadFX.SOUL[2] );
					} else if (mends && hero.sprite != null && hero.sprite.parent != null && hero.sprite.visible){
						//the soul home: the heal's red crosses and green light as it reaches him
						MoteFX.crosses( hero.sprite, 2 );
						MoteFX.greens( hero.sprite, 3 );
					}
				} ).peel( mob.sprite, PEEL );
			}
			if (mend && hero.isAlive()){
				int heal = Math.min( SkillInteractions.ofHealth( hero.HT, 0.03f ), hero.HT - hero.HP );
				if (heal > 0){
					hero.HP += heal;
					if (hero.sprite != null){
						//HP gained with no heal of the game's own: the soul's wisps now, the heal's red
						//crosses and green light when the soul arrives (the bolt's callback above)
						hero.sprite.emitter().burst( SkillBursts.SOUL_WISP, 4 );
						hero.sprite.showStatusWithIcon( CharSprite.POSITIVE, Integer.toString( heal ), xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.HEALING );
					}
				}
			}
			if (target != null && target.isAlive()){
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.prolong( target,
						xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark.class,
						xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark.DURATION );
				if (target.sprite != null){
					target.sprite.emitter().burst( SkillBursts.SOUL_WISP, 6 );
					//the soul went home through the Warlock: the mark's ring closes at once
					if (mend) ClosingRing.on( target.sprite, 23, 9, 0.3f, DreadFX.SOUL[2] );
				}
			}
			SpatialSound.play( Assets.Sounds.GHOST, mob, 0.6f, 1.3f );
		} );
	}
}

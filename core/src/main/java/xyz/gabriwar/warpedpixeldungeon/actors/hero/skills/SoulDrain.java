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
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;


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
			if (hero.sprite != null && hero.sprite.parent != null){
				xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile bolt = (xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile)
						hero.sprite.parent.recycle( xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile.class );
				bolt.reset( xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile.SHADOW, from,
						mend ? hero.pos : target.pos, null );
			}
			if (mend && hero.isAlive()){
				int heal = Math.min( SkillInteractions.ofHealth( hero.HT, 0.03f ), hero.HT - hero.HP );
				if (heal > 0){
					hero.HP += heal;
					if (hero.sprite != null){
						hero.sprite.emitter().burst( ShadowParticle.UP, 4 );
						hero.sprite.showStatus( CharSprite.POSITIVE, "+" + heal );
					}
				}
			}
			if (target != null && target.isAlive()){
				xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff.prolong( target,
						xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark.class,
						xyz.gabriwar.warpedpixeldungeon.actors.buffs.SoulMark.DURATION );
				if (target.sprite != null) target.sprite.emitter().burst( ShadowParticle.CURSE, 6 );
			}
			Sample.INSTANCE.play( Assets.Sounds.GHOST, 0.6f, 1.3f );
		} );
	}
}

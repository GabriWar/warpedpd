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
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.FletchingFeathers;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

import java.util.ArrayList;

/**
 * Huntress: every ranged hit plucks a feather into a ring that circles her. When the
 * ring is full, the next ranged hit bursts it: the feathers become splinters that
 * fly off the target at the enemies around it. One more splinter per level; fully
 * trained, a splinter that kills plucks its feather back into the ring.
 */
public class Fletching extends PassiveSkillA1 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	private static final float SPLINTER_DAMAGE = 0.3f;
	private static final int SPLINTER_REACH = 3;

	{
		name = "Fletching";
		image = 73;
		tier = 1;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (!ranged || level <= 0 || enemy == null || hero == null) return damage;

		FletchingFeathers ring = Buff.affect( hero, FletchingFeathers.class );
		if (!ring.full()){
			ring.pluck();
			return damage;
		}

		ArrayList<Char> targets = splinterTargets( enemy, level + 1 );
		//no one near the target: the feathers wait for a crowd
		if (targets.isEmpty()) return damage;

		ring.empty();
		castTextYell();
		if (enemy.sprite != null) burst( enemy.sprite );
		SpatialSound.play( Assets.Sounds.HIT_ARROW, enemy, 1f, 1.4f );

		int splinter = Math.max( 1, Math.round( damage * SPLINTER_DAMAGE ) );
		int order = 0;
		for (Char victim : targets){
			victim.damage( splinter, this );
			//each splinter lands a note higher than the last
			final float pitch = 1.2f + 0.1f * order++;
			//a feather dart of light (never a dart, an item), landing when the dart would have
			SkillFX.streak( enemy.pos, victim.pos, SkillFX.Streak.FEATHER_DART, new xyz.gabriwar.warpedpixeldungeon.items.weapon.missiles.darts.Dart(), () -> {
				SkillFX.flash( victim );
				if (victim.sprite != null) splintered( victim.sprite );
				SpatialSound.play( Assets.Sounds.HIT_ARROW, victim, 0.6f, pitch );
			} );
			if (!victim.isAlive() && level >= MAX_LEVEL) ring.pluck();
		}
		return damage;
	}

	/** The ring bursting on the struck: its feathers thrown off it (never a flock's wool, a thief's
	 *  poof) and a nature flare. Any thread (one burst of the sprite's emitter, as the wool's). */
	public static void burst( CharSprite s ){
		s.emitter().burst( SkillBursts.FEATHER, 6 );
		PixelFlare.show( SkillVfx.midX( s ), SkillVfx.midY( s ), xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette.NATURE_LIGHT, PixelFlare.S, 0.35f );
	}

	/** A splinter landing: a small nature flare where it strikes. Any thread. */
	public static void splintered( CharSprite s ){
		PixelFlare.show( SkillVfx.midX( s ), SkillVfx.midY( s ), xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette.NATURE_LIGHT, PixelFlare.S, 0.2f );
	}

	/** the nearest enemies around the struck one, each with a clear line from it */
	private static ArrayList<Char> splinterTargets( Char struck, int count ){
		ArrayList<Char> found = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs){
			if (m == struck || m.alignment != Char.Alignment.ENEMY || !m.isAlive()
					|| Dungeon.level.distance( struck.pos, m.pos ) > SPLINTER_REACH
					|| !Dungeon.level.heroFOV[m.pos] || !SkillInteractions.clear( struck.pos, m.pos )) continue;
			found.add( m );
		}
		found.sort( (a, b) -> Float.compare( Dungeon.level.trueDistance( struck.pos, a.pos ),
				Dungeon.level.trueDistance( struck.pos, b.pos ) ) );
		while (found.size() > count) found.remove( found.size() - 1 );
		return found;
	}

	@Override
	public boolean rangedSource(){ return true; }

	@Override
	protected boolean upgrade(){
		return true;
	}
}

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
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;


import com.watabou.utils.PathFinder;
import xyz.gabriwar.warpedpixeldungeon.effects.Pushing;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;

import java.util.ArrayList;

public class HolySmite extends ActiveSkill1 {

	//its damage is already a share of a blow, a hit or a health pool, so it grows with the hero on its own
	@Override
	public boolean weaponScaled(){ return true; }


	{
		name = "Holy Smite";
		castText = "Smite!";
		image = 111;
		mana = 3;
	}

	@Override
	public void execute( Hero hero, String action ){
		super.execute(hero, action);
		if (action.equals(Skill.AC_ACTIVATE)){
			Skill wrath = hero.heroSkills.get( DivineWrath.class );
			if (wrath != null) wrath.active = false;
			if (hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), 5 );
			Sample.INSTANCE.play( Assets.Sounds.CHARMS, 0.8f, 1.3f );
		}
	}

	@Override
	public int getManaCost(){
		return (int)Math.ceil(mana * (1 + 0.55 * level));
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	/** how far from the hammer enemies are dragged in: 2 / 3 / 3 tiles */
	private static int reach( int rank ){
		return rank >= 2 ? 3 : 2;
	}

	//each paid blow brings a hammer of light down on the target, and the impact drags the
	//enemies around it one tile in toward it
	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		Hero hero = Dungeon.hero;
		if (!active || level <= 0 || ranged || enemy == null || hero == null || hero.MP < getManaCost())
			return damage;

		hero.MP -= getManaCost();
		castTextYell();
		SkillSpectacleFX.show( SkillSpectacleFX.HAMMER, enemy.pos );
		SkillFX.land( enemy.pos );
		Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 0.8f, 0.9f );

		int r = reach( level );
		ArrayList<Mob> caught = new ArrayList<>();
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (m == enemy || m.alignment != Char.Alignment.ENEMY || !m.isAlive() || !Dungeon.level.heroFOV[m.pos]) continue;
			if (Dungeon.level.distance( enemy.pos, m.pos ) > r || !SkillInteractions.clear( enemy.pos, m.pos )) continue;
			caught.add( m );
		}
		//the nearest are drawn in first, so the ones behind them close up
		caught.sort( (a, b) -> Float.compare( Dungeon.level.trueDistance( enemy.pos, a.pos ), Dungeon.level.trueDistance( enemy.pos, b.pos ) ) );
		for (Mob m : caught){
			if (Dungeon.level.distance( enemy.pos, m.pos ) >= 2) drag( m, enemy.pos );
		}

		//at mastery every enemy caught around the hammer takes half the blow
		if (level >= MAX_LEVEL){
			for (Mob m : caught){
				if (!m.isAlive()) continue;
				m.damage( Math.max( 1, damage / 2 ), this );
				SkillFX.flash( m );
			}
		}
		return damage;
	}

	private static void drag( Char ch, int toward ){
		if (ch.rooted || Char.hasProp( ch, Char.Property.IMMOVABLE )) return;
		int from = ch.pos;
		int best = -1;
		for (int n : PathFinder.NEIGHBOURS8){
			int c = from + n;
			if (!SkillInteractions.valid( c ) || Dungeon.level.solid[c] || !Dungeon.level.passable[c]
					|| Dungeon.level.pit[c] || Actor.findChar( c ) != null) continue;
			if (Char.hasProp( ch, Char.Property.LARGE ) && !Dungeon.level.openSpace[c]) continue;
			if (Dungeon.level.distance( c, toward ) >= Dungeon.level.distance( from, toward )) continue;
			if (best == -1 || Dungeon.level.trueDistance( c, toward ) < Dungeon.level.trueDistance( best, toward )) best = c;
		}
		if (best == -1) return;
		//the enemy slides in without a step of its own; move() occupies the new cell
		if (ch.sprite != null) Actor.add( new Pushing( ch, from, best ) );
		ch.move( best, false );
		SkillInteractions.flare( from, 0xFFF1A1 );
	}
}

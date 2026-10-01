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
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;


import com.watabou.noosa.audio.Sample;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;

import java.util.HashSet;

public class RighteousStrikes extends PassiveSkillB1 {

	//light gathers over this many landed melee blows and is released on the last of them
	private static final int BLOWS_PER_RELEASE = 3;
	//holy damage of each link of the chain, doubled against the unholy
	private static final int LIGHT = 5;
	//how far the light can leap from one enemy to the next
	private static final int LEAP = 2;

	private int blows = 0;

	{
		name = "Righteous Strikes";
		image = 108;
		tier = 1;
	}

	@Override
	protected boolean upgrade(){
		return true;
	}

	private static int holy( Char ch ){
		boolean unholy = Char.hasProp( ch, Char.Property.UNDEAD ) || Char.hasProp( ch, Char.Property.DEMONIC );
		return unholy ? LIGHT * 2 : LIGHT;
	}

	@Override
	public int onHitProc( Char enemy, int damage, boolean ranged ){
		if (level <= 0 || ranged || enemy == null)
			return damage;
		Hero hero = Dungeon.hero;
		if (++blows < BLOWS_PER_RELEASE){
			//the light is seen gathering on the cleric, brighter each blow
			if (hero != null && hero.sprite != null) hero.sprite.emitter().burst( Speck.factory( Speck.LIGHT ), blows * 2 );
			return damage;
		}
		blows = 0;

		if (enemy.sprite != null){
			new Flare( 6, 20 ).color( 0xFFEE88, true ).show( enemy.sprite, 0.6f );
			enemy.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 6 );
		}
		Sample.INSTANCE.play( Assets.Sounds.HIT_MAGIC, 0.8f, 1.2f );

		//the released light leaps from enemy to enemy, one more leap per level
		//the light leaps one enemy after another, each leap a shade higher
		FxTimeline chain = FxTimeline.start();
		HashSet<Char> struck = new HashSet<>();
		struck.add( enemy );
		Char from = enemy;
		for (int i = 0; i < level; i++){
			Char next = null;
			for (Mob m : Dungeon.level.mobs){
				if (struck.contains( m ) || m.alignment != Char.Alignment.ENEMY || !m.isAlive()
						|| !Dungeon.level.heroFOV[m.pos] || Dungeon.level.distance( from.pos, m.pos ) > LEAP
						|| !SkillInteractions.clear( from.pos, m.pos )) continue;
				if (next == null || Dungeon.level.trueDistance( from.pos, m.pos ) < Dungeon.level.trueDistance( from.pos, next.pos )) next = m;
			}
			if (next == null) break;
			final Char a = from, b = next;
			final float pitch = 1.2f + 0.1f * i;
			chain.at( 0.1f * (i + 1), () -> { ray( a, b ); Sample.INSTANCE.play( Assets.Sounds.RAY, 0.5f, pitch ); } );
			struck.add( next );
			next.damage( holy( next ), this );
			from = next;
		}

		//at mastery the light comes home and mends the cleric for every enemy it touched
		if (level >= MAX_LEVEL && hero != null && hero.sprite != null){
			final Char last = from;
			chain.at( 0.1f * (struck.size() + 1), () -> ray( last, hero ) );
			hero.heal( SkillInteractions.ofHealth( hero.HT, 0.01f ) * struck.size() );
		}

		return damage + holy( enemy );
	}

	private static void ray( Char a, Char b ){
		if (a.sprite == null || b.sprite == null || a.sprite.parent == null) return;
		a.sprite.parent.add( new Beam.LightRay( a.sprite.center(), b.sprite.center() ) );
		b.sprite.emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 4 );
	}
}

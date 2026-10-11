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

package xyz.gabriwar.warpedpixeldungeon.actors.buffs;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.nature.HolyFx;
import com.watabou.noosa.Halo;
import com.watabou.utils.Bundle;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;

import java.util.HashSet;

/**
 * Avatar of Light: a blazing halo around the cleric. The first time each enemy comes
 * inside it, a ray sears and blinds it; at mastery allies inside are blessed and mended.
 */
public class AvatarOfLightHalo extends Buff {

	/** holy damage of the sear, doubled against the unholy */
	public static final int SEAR = 6;
	public static final float BLIND = 3f;

	{
		type = buffType.POSITIVE;
		announced = false;
	}

	private int left = 0;
	private int rank = 1;
	private final HashSet<Integer> seared = new HashSet<>();

	private HolyFx.Halo glow;

	/** how far the halo reaches: 1 / 2 / 2 tiles */
	public static int radius( int rank ){
		return rank >= 2 ? 2 : 1;
	}

	public void set( int rank, int turns ){
		this.rank = rank;
		left = turns;
		seared.clear();
		fx( true );
	}

	@Override
	public boolean act(){
		if (!target.isAlive() || left <= 0){
			detach();
			return true;
		}
		int r = radius( rank );
		for (Mob m : Dungeon.level.mobs.toArray( new Mob[0] )){
			if (!m.isAlive() || Dungeon.level.distance( target.pos, m.pos ) > r || !SkillInteractions.clear( target.pos, m.pos )) continue;
			if (m.alignment == Char.Alignment.ENEMY){
				if (!seared.add( m.id() )) continue;
				boolean unholy = Char.hasProp( m, Char.Property.UNDEAD ) || Char.hasProp( m, Char.Property.DEMONIC );
				if (target.sprite != null && target.sprite.parent != null && m.sprite != null)
					target.sprite.parent.add( new Beam.LightRay( target.sprite.center(), m.sprite.center() ) );
				SpatialSound.play( Assets.Sounds.RAY, m, 0.8f, 1.3f );
				m.damage( unholy ? SEAR * 2 : SEAR, this );
				SkillFX.flash( m );
				if (m.isAlive()) Buff.prolong( m, Blindness.class, BLIND );
			} else if (rank >= Skill.MAX_LEVEL && m.alignment == Char.Alignment.ALLY){
				Buff.prolong( m, Bless.class, 2f );
				if (m.HP < m.HT) m.HP = Math.min( m.HT, m.HP + xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions.ofHealth( m.HT, 0.01f ) );
			}
		}
		//the edge of the halo sparkles on the floor each turn
		int[] edge = new int[16];
		int n = 0;
		for (int c : SkillInteractions.area( target.pos, r ))
			if (Dungeon.level.distance( target.pos, c ) == r && n < edge.length) edge[n++] = c;
		HolyFx.sparkles( edge, n );

		if (--left <= 0) detach();
		else spend( TICK );
		return true;
	}

	//its light: a pool and a glow round the bearer, breathing (effects/fx/nature/HolyFx)
	@Override
	public void fx( boolean on ){
		if (glow != null){
			glow.out();
			glow = null;
		}
		if (on && target.sprite != null && target.sprite.parent != null){
			glow = HolyFx.halo( target.sprite, radius( rank ) );
		}
	}

	private static final String LEFT = "left", RANK = "rank", SEARED = "seared";

	@Override
	public void storeInBundle( Bundle bundle ){
		super.storeInBundle( bundle );
		bundle.put( LEFT, left );
		bundle.put( RANK, rank );
		int[] ids = new int[seared.size()];
		int i = 0;
		for (int id : seared) ids[i++] = id;
		bundle.put( SEARED, ids );
	}

	@Override
	public void restoreFromBundle( Bundle bundle ){
		super.restoreFromBundle( bundle );
		left = bundle.getInt( LEFT );
		rank = bundle.getInt( RANK );
		seared.clear();
		int[] ids = bundle.getIntArray( SEARED );
		if (ids != null) for (int id : ids) seared.add( id );
	}
}

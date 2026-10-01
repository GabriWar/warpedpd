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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.ChargedSteam;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Electricity;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import com.watabou.noosa.Image;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PathFinder;
import com.watabou.utils.Random;

import java.util.ArrayList;

//thunderhead brew, drunk: immune to lightning, and melee hits can arc to a second enemy
public class StormCharge extends FlavourBuff {

	{
		type = buffType.POSITIVE;
		announced = true;

		immunities.add( Electricity.class );
		immunities.add( ChargedSteam.class );
	}

	public static final float DURATION = 40f;
	public static final float ARC_CHANCE = 0.3f;

	public void proc( Char enemy ) {
		//melee only, like the warmth elixir
		if (target instanceof Hero && ((Hero) target).belongings.thrownWeapon != null) return;
		if (Random.Float() >= ARC_CHANCE) return;

		ArrayList<Char> candidates = new ArrayList<>();
		for (int offset : PathFinder.NEIGHBOURS8) {
			Char ch = Actor.findChar( enemy.pos + offset );
			if (ch != null && ch != target && ch.isAlive() && ch.alignment != target.alignment) {
				candidates.add( ch );
			}
		}
		if (candidates.isEmpty()) return;

		Char victim = Random.element( candidates );
		if (enemy.sprite != null && victim.sprite != null && enemy.sprite.parent != null) {
			enemy.sprite.parent.addToFront( new Lightning( enemy.sprite.center(), victim.sprite.center(), null ) );
			victim.sprite.centerEmitter().burst( SparkParticle.FACTORY, 3 );
		}
		if (Dungeon.level.heroFOV[victim.pos]) {
			Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
		}
		victim.damage( Random.NormalIntRange( 1 + Dungeon.scalingDepth()/4, 4 + Dungeon.scalingDepth()/2 ), this );
	}

	@Override
	public int icon() {
		return BuffIndicator.IMBUE;
	}

	@Override
	public void tintIcon( Image icon ) {
		icon.hardlight( 0.6f, 0.9f, 1f );
	}

	@Override
	public float iconFadePercent() {
		return Math.max( 0, (DURATION - visualcooldown()) / DURATION );
	}
}

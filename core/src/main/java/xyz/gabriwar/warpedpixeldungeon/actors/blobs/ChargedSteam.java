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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.audio.Sample;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

//the charge a thunderhead brew leaves in its steam: it does not spread, it just counts
//down in place and zaps whoever stands in it every turn
public class ChargedSteam extends Blob {

	public static final int DURATION = 8;

	{
		//acts after mobs, like electricity
		actPriority = MOB_PRIO - 1;
	}

	@Override
	protected void evolve() {

		boolean zapped = false;
		int cell;
		for (int i = area.left; i < area.right; i++) {
			for (int j = area.top; j < area.bottom; j++) {
				cell = i + j*Dungeon.level.width();
				if (cur[cell] > 0) {
					Char ch = Actor.findChar( cell );
					if (ch != null && !ch.isImmune( getClass() ) && !ch.isImmune( Electricity.class )) {
						if (Dungeon.level.heroFOV[cell] && ch.sprite != null && ch.sprite.parent != null) {
							PointF to = ch.sprite.center();
							ch.sprite.parent.addToFront( new Lightning( new PointF( to.x, to.y - 24 ), to, null ) );
							zapped = true;
						}
						ch.damage( Math.round( Random.Float( 2 + Dungeon.scalingDepth() / 5f ) ), this );
						if (!ch.isAlive() && ch == Dungeon.hero){
							Dungeon.fail( this );
							GLog.n( Messages.get( this, "ondeath" ) );
						}
					}

					off[cell] = cur[cell] - 1;
					volume += off[cell];
				} else {
					off[cell] = 0;
				}
			}
		}

		if (zapped) {
			Sample.INSTANCE.play( Assets.Sounds.LIGHTNING );
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.start( SparkParticle.FACTORY, 0.1f, 0 );
	}

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}

}

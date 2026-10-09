/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2023 Evan Debenham
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

package xyz.gabriwar.warpedpixeldungeon.effects;

import static xyz.gabriwar.warpedpixeldungeon.Dungeon.hero;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.utils.PointF;

public class ThunderBolt {

	//strikes a single bolt down onto the given sprite, with flash and sound: the in-game
	//lightning's (Lightning), out of 35 px above it
	public static void thunderEffect(CharSprite sprite) {
		if (sprite != null) {
			sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
			sprite.flash();
			hero.sprite.parent.addToFront( new Lightning( new PointF( sprite.center().x, sprite.center().y-35 ), sprite.center(), null ) );
			SpatialSound.play(Assets.Sounds.ROCKS, sprite.ch, 0.7f, 0.5f);
			SpatialSound.play( Assets.Sounds.LIGHTNING, sprite.ch );
			SpatialSound.play( Assets.Sounds.BLAST, sprite.ch );
		}
	}
}

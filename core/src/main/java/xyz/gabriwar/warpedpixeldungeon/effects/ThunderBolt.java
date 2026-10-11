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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Camera;
import com.watabou.utils.PointF;

import java.util.ArrayList;

public class ThunderBolt {

	//its foot: the last FOOT px down onto the sprite, an arc as it always was (its numbers drawn
	//from the game's Random as they always were); above it the bolt from TOP px over the view's
	//top, leaning with the wind, in UPPER_MIN to UPPER_MAX more legs with a fork (FORK times in
	//ten), drawn from the effects' own random numbers. Its leader runs down the upper part over
	//UPPER_LEAD of its time, then down the foot
	static final float FOOT = 35f, TOP = 12f, FORK = 0.7f, UPPER_LEAD = 0.6f;
	static final int UPPER_MIN = 3, UPPER_MAX = 4;

	//strikes a single bolt down onto the given sprite out of the sky above the view, a major
	//lightning (its flash and crack, its leader and the ground's answer), with sound
	public static void thunderEffect(CharSprite sprite) {
		if (sprite != null) {
			sprite.centerEmitter().burst(SparkParticle.FACTORY, 3);
			sprite.flash();
			if (hero != null && hero.sprite != null && hero.sprite.parent != null) {
				PointF to = sprite.center();
				PointF foot = new PointF( to.x, to.y - FOOT );
				ArrayList<Lightning.Arc> arcs = new ArrayList<>();
				Lightning.Arc upper = upper( foot );
				if (upper != null) arcs.add( upper );
				arcs.add( new Lightning.Arc( foot, to ).joined( upper != null ? UPPER_LEAD : 0f, 1f ) );
				hero.sprite.parent.addToFront( new Lightning( arcs, null ).major() );
			}
			SpatialSound.play(Assets.Sounds.ROCKS, sprite.ch, 0.7f, 0.5f);
			SpatialSound.play( Assets.Sounds.LIGHTNING, sprite.ch );
			SpatialSound.play( Assets.Sounds.BLAST, sprite.ch );
		}
	}

	//the bolt above its foot, from over the view's top leaning with the wind; none where the foot
	//is already above the view
	private static Lightning.Arc upper( PointF foot ){
		Camera cam = Camera.main;
		if (cam == null) return null;
		float top = cam.scroll.y - TOP;
		if (foot.y - top < 16f) return null;
		PointF from = new PointF( foot.x - WeatherOverlay.skyLean(), top );
		return Lightning.Arc.visual( from, foot, FxRandom.IntRange( UPPER_MIN, UPPER_MAX ), 1f, FORK, false )
				.lead( 0f, UPPER_LEAD );
	}
}

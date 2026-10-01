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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.glwrap.Blending;
import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.Callback;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;

/**
 * A pillar of light that rises from a cell, stands for a breath, then rains sparks as it fades.
 * onPeak fires the moment it reaches full height, for the flash and the sound.
 */
public class PillarRiseFX extends Group {

	private static final float RISE = 0.2f, HOLD = 0.12f, FADE = 0.45f;
	private static final float HEIGHT = DungeonTilemap.SIZE * 6.5f;

	private final Image ray;
	private final PointF foot;
	private final int color;
	private final Callback onPeak;
	private final float length;
	private float time = 0;
	private boolean peaked = false;

	private PillarRiseFX( int cell, int color, Callback onPeak ){
		this.color = color;
		this.onPeak = onPeak;
		foot = DungeonTilemap.raisedTileCenterToWorld( cell );
		ray = new Image( Effects.get( Effects.Type.LIGHT_RAY ) ){
			@Override
			public void draw(){
				Blending.setLightMode();
				super.draw();
				Blending.setNormalMode();
			}
		};
		ray.hardlight( color );
		ray.origin.set( 0, ray.height / 2 );
		ray.x = foot.x - ray.origin.x;
		ray.y = foot.y - ray.origin.y;
		ray.angle = -90;
		length = HEIGHT / ray.width;
		ray.scale.set( 0, 1.6f );
		add( ray );
	}

	public static void show( int cell, int color, Callback onPeak ){
		if (Dungeon.hero == null || Dungeon.hero.sprite == null || Dungeon.hero.sprite.parent == null
				|| Dungeon.level == null || !Dungeon.level.heroFOV[cell]){
			if (onPeak != null) onPeak.call();
			return;
		}
		Dungeon.hero.sprite.parent.add( new PillarRiseFX( cell, color, onPeak ) );
	}

	@Override
	public void update(){
		super.update();
		time += Game.elapsed;
		if (time < RISE){
			float p = time / RISE;
			ray.scale.set( length * (1 - (1 - p) * (1 - p)), 1.6f );
		} else if (!peaked){
			peaked = true;
			ray.scale.set( length, 1.6f );
			if (parent != null){
				new Flare( 6, 14 ).color( color, true ).show( parent, foot, 0.35f );
				Emitter rain = new Emitter();
				rain.pos( foot.x - 5, foot.y - HEIGHT * 0.55f, 10, 4 );
				parent.add( rain );
				rain.burst( SparkParticle.FACTORY, 12 );
			}
			if (onPeak != null) onPeak.call();
		} else if (time > RISE + HOLD){
			float p = (time - RISE - HOLD) / FADE;
			if (p >= 1){ killAndErase(); return; }
			ray.scale.set( length, 1.6f * (1 - p) );
			ray.alpha( 1 - p );
		}
	}
}

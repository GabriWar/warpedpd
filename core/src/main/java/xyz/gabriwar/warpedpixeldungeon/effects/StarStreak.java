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

package xyz.gabriwar.warpedpixeldungeon.effects;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.audio.SpatialSound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.Camera;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.particles.PixelParticle;
import com.watabou.utils.PointF;
import com.watabou.utils.Random;

/**
 * A falling star across the sky (levels/overworld/WorldEvents): a white-hot head trailing a
 * pale core and sparks that drift down as they cool. It lives above the fog and the night's
 * tint, among the effects a window slide moves, and everything it sheds is placed off its own
 * position, so a slide in mid-flight never shows. One that strikes the ground close by ends in
 * a blast, flames, a flash, a shudder of the camera and the crash of rock.
 */
public class StarStreak extends PixelParticle {

	private boolean impact, landed;

	/** A star across the sky from `from` to `to` in `duration` seconds, striking the ground at
	 *  `to` when `impact`. Render thread only; nothing without a game scene. */
	public static void fall( PointF from, PointF to, float duration, boolean impact ){
		StarStreak s = new StarStreak();
		s.reset( from.x, from.y, 0xFFFFF4, 3, duration );
		s.speed.set( (to.x - from.x) / duration, (to.y - from.y) / duration );
		s.impact = impact;
		GameScene.effectOverFog( s );
	}

	@Override
	public void update(){
		if (parent != null && !landed){
			((Trail) parent.recycle( Trail.class )).core( x + Random.Float( -1, 1 ), y + Random.Float( -1, 1 ) );
			if (Random.Int( 2 ) == 0) ((Trail) parent.recycle( Trail.class )).ember( x, y );
		}
		super.update();
		if (!alive && !landed){
			landed = true;
			//the last frame carried it past the end by the time it overran: back to where it struck
			//(a pixel particle is drawn about its own pixel's middle)
			if (impact) land( x + 0.5f + speed.x * left, y + 0.5f + speed.y * left );
			killAndErase();
		}
	}

	private void land( float lx, float ly ){
		Emitter blast = GameScene.emitter();
		if (blast != null){
			blast.pos( lx - 8, ly - 8, 16, 16 );
			blast.burst( BlastParticle.FACTORY, 24 );
		}
		Emitter fire = GameScene.emitter();
		if (fire != null){
			fire.pos( lx - 8, ly - 6, 16, 12 );
			fire.burst( FlameParticle.FACTORY, 10 );
		}
		if (parent != null) new Flare( 8, 28 ).color( 0xFFF0B0, true ).show( parent, new PointF( lx, ly ), 0.6f );
		Camera.main.shake( 3, 0.4f );
		//from where it struck
		int cell = Dungeon.level == null ? -1 : DungeonTilemap.worldToTile( lx, ly, Dungeon.level.width() );
		SpatialSound.play( Assets.Sounds.BLAST, cell, 0.8f );
		SpatialSound.play( Assets.Sounds.ROCKS, cell, 0.7f, 0.8f );
	}

	/** What the star sheds: a pale core that shrinks where it was left, and sparks that drift
	 *  down and cool. Recycled by its group, so it keeps a public no-argument constructor. */
	public static class Trail extends PixelParticle.Shrinking {

		public void core( float x, float y ){
			reset( x, y, 0xFFF4C8, 2, Random.Float( 0.3f, 0.45f ) );
			speed.set( 0 );
			acc.set( 0 );
		}

		public void ember( float x, float y ){
			reset( x, y, 0xFF9A30, 1, Random.Float( 0.5f, 0.8f ) );
			speed.set( Random.Float( -10, 10 ), Random.Float( -10, 10 ) );
			acc.set( 0, 20 );
		}
	}
}

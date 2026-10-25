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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Vulture;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;

/**
 * A vulture (tools/ow_fauna_gen.py, 16x16 frames): aloft, gliding with a wingbeat now and then, the
 * wingbeat as it flies, landed and tearing at carrion, the stoop and the strike, the fall and the
 * carcass. Aloft it is drawn LIFT pixels over its cell, bobbing a little, its shadow left on the
 * ground under it; standing on carrion (raw meat on top of the heap on its cell: what every machine
 * sees, Vulture.carrionImage) it settles down onto the ground and feeds. The height eases in and
 * out, so a landing and a take-off glide rather than jump.
 *
 * The height is the drawing's alone (updateMatrix): its place (x, y) stays what its cell, a move's
 * tween, a push or the window's shift writes, whichever of them runs last in a frame.
 */
public class VultureSprite extends WildSprite {

	//how high it flies over its cell, how far it bobs, and how quickly it climbs or comes down
	private static final float LIFT = 3f, BOB = 0.5f, EASE = 6f;

	private final Animation feed;

	//the height it is drawn at now over its place
	private float lift = 0f;

	public VultureSprite(){
		super();
		texture( Assets.Sprites.VULTURE );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		//gliding, a wingbeat now and then
		idle = new Animation( 6, true );
		idle.frames( frames, 0, 0, 0, 0, 1, 2 );

		run = new Animation( 10, true );
		run.frames( frames, 1, 0, 2, 0 );

		feed = new Animation( 3, true );
		feed.frames( frames, 3, 3, 4, 3, 4, 4 );

		attack = new Animation( 12, false );
		attack.frames( frames, 5, 6, 6, 0 );

		die = new Animation( 10, false );
		die.frames( frames, 7, 8 );

		play( idle );
	}

	@Override
	public void idle(){
		play( landed() ? feed : idle );
	}

	//on carrion: raw meat on top of the heap on its cell
	private boolean landed(){
		Level level = Dungeon.level;
		if (ch == null || level == null || level.heaps == null) return false;
		Heap h = level.heaps.get( ch.pos );
		return h != null && h.peek() != null && Vulture.carrionImage( h.peek().image() );
	}

	@Override
	public void update(){
		super.update();
		//a picture of one with no creature behind it (the guide's) is placed by whoever shows it
		if (ch == null) return;
		//down on carrion, dying, or knocked out of the air (frozen, paralysed: Char.loseFlight)
		boolean down = curAnim == feed || curAnim == die || !ch.isAlive() || ch.groundedFlier();
		float aim = down ? 0f : LIFT + BOB * (float) Math.sin( Game.timeTotal * 2f );
		lift += (aim - lift) * Math.min( 1f, Game.elapsed * EASE );
	}

	//its body drawn `lift` over its place; the shadow, laid out from the place first, stays on the ground
	@Override
	protected void updateMatrix(){
		super.updateMatrix();
		matrix[13] -= lift;
	}
}

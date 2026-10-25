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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HorseSprite;

/**
 * Riding (RIDING: homestead/mounts/Riding): the rider's horse, drawn right before him, so it is
 * always under him and always where he is, in step: its feet on his feet's line, SEAT px ahead of
 * his middle, turned the way he faces, at his alpha (an invisible rider's horse is as faint), its own
 * shadow under it (the rider's is not drawn while he rides: CharSprite). It gallops while he moves
 * and a moment after, so a run of steps is one gallop, never a stand between two of them; else it
 * stands, grazing now and then. The rider himself is drawn RIDE_LIFT up (CharSprite).
 *
 * Its sprite is made the first time it is drawn and let go of on the render thread; its coat is
 * the rider's sprite's rideCoat (a co-op guest's is the first coat). One quad and its shadow.
 */
final class RiddenHorse extends StateLooks.Look {

	/** How long it keeps galloping after its rider's last step ended, s; how far ahead of his
	 *  middle it is drawn, px (he sits its back behind the withers, its neck and head out in front). */
	static final float GALLOP_ON = 0.2f;
	static final int SEAT = 2;

	private HorseSprite horse;
	private int coat = -1;
	private float galloping;

	RiddenHorse( CharSprite s ){
		super( s );
	}

	@Override
	boolean step( float dt ){
		if (stopping) return false;
		if (s.isMoving) galloping = GALLOP_ON;
		else galloping = Math.max( 0f, galloping - dt );
		if (horse != null){
			horse.gallop( galloping > 0f );
			horse.step();
		}
		return true;
	}

	@Override
	void drawBehind(){
		if (stopping) return;
		int want = s.rideCoat;
		if (horse == null){
			horse = new HorseSprite( want, true ).shadowed();
			coat = want;
			horse.gallop( galloping > 0f );
		} else if (coat != want){
			horse.look( want, true );
			coat = want;
		}
		horse.camera = s.camera();
		horse.flipHorizontal = s.flipHorizontal;
		//the rider's own offsets, so the two move as one even between whole pixels
		horse.x = s.x + (s.width() - horse.width()) / 2f + (s.flipHorizontal ? -SEAT : SEAT);
		horse.y = feet() - horse.height();
		horse.alpha( s.alpha() );
		horse.draw();
	}

	@Override
	void release(){
		HorseSprite gone = horse;
		horse = null;
		if (gone != null) Fx.dispose( gone );
	}
}

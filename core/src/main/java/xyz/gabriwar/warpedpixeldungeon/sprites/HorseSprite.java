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
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.Callback;

/**
 * A horse of the surface (tools/assets/horse_gen.py): six rows of the sheet, a coat each (bay,
 * chestnut, grey) bare and saddled. Frames of a row: stand, graze, four of the gallop, a rear, the
 * fall, the carcass. Each look is a class of its own (classFor), so a co-op guest, who rebuilds a
 * sprite from its class alone, sees the horse the host does.
 *
 * Under a rider it is drawn by the rider's own sprite (effects/fx/combat/RiddenHorse): shadowed,
 * its animation stepped by hand (step).
 */
public class HorseSprite extends MobSprite {

	/** A frame's size (wider than a cell: a hero sits it) and how many a row holds. */
	public static final int WIDTH = 24, HEIGHT = 18, FRAMES = 9;

	private int coat = -1;
	private boolean saddled;
	private Animation munch;

	public HorseSprite(){
		this( 0, false );
	}

	public HorseSprite( int coat, boolean saddled ){
		super();
		texture( Assets.Sprites.HORSE );
		look( coat, saddled );
	}

	/** Its coat and saddle: the row its frames come from. The animation playing goes on in the new
	 *  look. */
	public synchronized void look( int coat, boolean saddled ){
		if (coat == this.coat && saddled == this.saddled) return;
		Animation was = curAnim;
		boolean wasRun = was != null && was == run, wasDie = was != null && was == die;
		this.coat = coat;
		this.saddled = saddled;
		TextureFilm frames = new TextureFilm( texture, WIDTH, HEIGHT );
		int o = (Math.floorMod( coat, 3 ) * 2 + (saddled ? 1 : 0)) * FRAMES;

		//head down to graze now and then
		idle = new Animation( 2, true );
		idle.frames( frames, o, o, o, o, o + 1, o + 1, o + 1, o );

		//the gallop: gathered, reaching, the push, the flight
		run = new Animation( 14, true );
		run.frames( frames, o + 2, o + 3, o + 4, o + 5 );

		//its own object: a startled rear, never the looped idle (an attack that never ends)
		attack = new Animation( 10, false );
		attack.frames( frames, o + 6, o + 6, o );

		die = new Animation( 8, false );
		die.frames( frames, o + 7, o + 8 );

		//eating: the head down a while
		munch = new Animation( 3, false );
		munch.frames( frames, o + 1, o + 1, o + 1 );

		play( wasDie ? die : wasRun ? run : idle, true );
	}

	/** Its head down over its food a moment, then it stands again. */
	public synchronized void munch(){
		animCallback = new Callback(){
			@Override
			public void call(){
				idle();
			}
		};
		play( munch, true );
	}

	/** Its shadow under it, drawn under a rider (a sprite linked to a horse gets its own). */
	public HorseSprite shadowed(){
		renderShadow = true;
		return this;
	}

	/** Its animation stepped on by a frame, for one drawn by hand under a rider. */
	public void step(){
		updateAnimation();
	}

	/** Plays the gallop (true) or stands (false), for one drawn by hand under a rider. */
	public void gallop( boolean on ){
		play( on ? run : idle );
	}

	/** The sprite class of a coat and saddle. */
	public static Class<? extends HorseSprite> classFor( int coat, boolean saddled ){
		switch (Math.floorMod( coat, 3 )){
			case 0: default: return saddled ? Saddled.class : HorseSprite.class;
			case 1:          return saddled ? ChestnutSaddled.class : Chestnut.class;
			case 2:          return saddled ? GreySaddled.class : Grey.class;
		}
	}

	public static class Saddled extends HorseSprite {
		public Saddled(){ super( 0, true ); }
	}

	public static class Chestnut extends HorseSprite {
		public Chestnut(){ super( 1, false ); }
	}

	public static class ChestnutSaddled extends HorseSprite {
		public ChestnutSaddled(){ super( 1, true ); }
	}

	public static class Grey extends HorseSprite {
		public Grey(){ super( 2, false ); }
	}

	public static class GreySaddled extends HorseSprite {
		public GreySaddled(){ super( 2, true ); }
	}
}

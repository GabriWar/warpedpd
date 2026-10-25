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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.water.WaterDrops;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import com.watabou.noosa.TextureFilm;

/**
 * An alligator of the swamp (tools/ow_fauna_gen.py, 24x16 frames: longer than its cell). On land it
 * basks, its jaws gaping now and then, and walks on its short legs; in the water it lurks with only
 * its eyes, its nostrils and a few ridges above the surface, blinking, and swims with its back
 * clear, the rest of it a shape under the water and no shadow. Which it shows follows the cell it
 * stands on (or steps onto), so a co-op guest's copy shows the same. Its bite: the jaws thrown open
 * and snapped shut, out of the water in a spray when it strikes from there; the death roll, onto
 * its back.
 */
public class AlligatorSprite extends WildSprite {

	private final Animation bask, walk, lurk, swim;

	//the cell it last struck at: the spray flies that way
	private int bitten = -1;

	public AlligatorSprite(){
		super();
		texture( Assets.Sprites.ALLIGATOR );
		TextureFilm frames = new TextureFilm( texture, 24, 16 );

		//basking, the jaws gaping now and then
		bask = new Animation( 2, true );
		bask.frames( frames, 0, 0, 0, 0, 1, 1, 0, 0 );

		walk = new Animation( 8, true );
		walk.frames( frames, 2, 0, 3, 0 );

		//lurking, a slow blink
		lurk = new Animation( 2, true );
		lurk.frames( frames, 4, 4, 4, 4, 4, 5 );

		swim = new Animation( 6, true );
		swim.frames( frames, 6, 7 );

		attack = new Animation( 12, false );
		attack.frames( frames, 8, 9, 9 );

		die = new Animation( 8, false );
		die.frames( frames, 10, 11 );

		idle = bask;
		run = walk;
		play( idle );
	}

	@Override
	public void idle(){
		boolean wet = inWater();
		renderShadow = !wet;
		idle = wet ? lurk : bask;
		super.idle();
	}

	@Override
	public void move( int from, int to ){
		Level level = Dungeon.level;
		boolean wet = ch != null && level != null && to >= 0 && to < level.length() && level.water[to];
		run = wet ? swim : walk;
		renderShadow = !wet;
		super.move( from, to );
	}

	@Override
	public void attack( int cell ){
		bitten = cell;
		super.attack( cell );
	}

	@Override
	public void onComplete( Animation anim ){
		//where it struck from: the blow (super) may drag its prey and back it into other water
		int from = ch != null ? ch.pos : -1;
		boolean wet = inWater();
		super.onComplete( anim );
		//up out of the water and back: a crown, droplets flung at what it bit, a ring
		if (anim == attack && wet) WaterDrops.lunge( from, bitten );
	}
}

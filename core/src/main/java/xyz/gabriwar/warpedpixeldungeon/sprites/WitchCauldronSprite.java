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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;

/**
 * The swamp witch's cauldron (actors/mobs/npcs/WitchCauldron): a black iron pot on three short
 * legs, a green brew rolling in it (tools/assets/wetland_art.py), and pale green steam curling up
 * off it while it is seen - a wisp every STEAM_MIN to STEAM_MAX seconds, from the effects' own
 * random numbers (FxRandom) into their loose holder (Fx.air), which the overworld's window slide
 * moves with everything else. Furniture: it never turns.
 */
public class WitchCauldronSprite extends MobSprite {

	/** The steam's colour: the brew's green, washed out. */
	public static final int STEAM = 0xA8D898;
	private static final float STEAM_MIN = 0.25f, STEAM_MAX = 0.55f;

	private float steamIn = 0.3f;

	public WitchCauldronSprite(){
		super();

		texture( Assets.Sprites.WITCH_CAULDRON );
		TextureFilm frames = new TextureFilm( texture, 16, 16 );

		idle = new Animation( 6, true );
		idle.frames( frames, 0, 1, 2, 3 );

		run = new Animation( 1, false );
		run.frames( frames, 0 );
		attack = new Animation( 1, false );
		attack.frames( frames, 0 );
		operate = new Animation( 1, false );
		operate.frames( frames, 0 );
		die = new Animation( 1, false );
		die.frames( frames, 0 );

		play( idle );
	}

	@Override
	public void turnTo( int from, int to ){
		//a pot faces nowhere
	}

	@Override
	public void update(){
		super.update();
		//time stopped (an hourglass, a swiftthistle): the steam it already made hangs still, and no
		//more piles up under it
		if (!visible || ch == null || parent == null || Emitter.freezeEmitters) return;
		if ((steamIn -= Game.elapsed) > 0) return;
		steamIn = FxRandom.Float( STEAM_MIN, STEAM_MAX );
		FxEmitter air = Fx.air();
		if (air == null) return;
		//off the brew's surface, the pot's mouth 5..11 px across and 4 px down its frame
		FxFactories.wisp( STEAM ).emit( air, 0, x + FxRandom.Float( 5f, 11f ), y + 4f );
	}

	@Override
	public int blood(){
		return 0xFF2A2A30;
	}
}

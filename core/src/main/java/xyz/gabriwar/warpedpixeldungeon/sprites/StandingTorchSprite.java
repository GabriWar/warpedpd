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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.fire.PlaceLight;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;

/** A standing torch (homestead/torches/StandingTorch): its flame flickering through three frames,
 *  and a torch's light on the ground round it, made for the cell it stands on. */
public class StandingTorchSprite extends MobSprite {

	private PlaceLight light;
	private int lightCell = -1;

	public StandingTorchSprite(){
		super();
		texture( HomesteadSprites.TEXTURE );
		TextureFilm film = HomesteadSprites.film();
		int f = HomesteadSprites.STANDING_TORCH;
		idle = new Animation( 6, true );
		idle.frames( film, f, f + 1, f + 2, f + 1 );
		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();
		play( idle );
	}

	@Override
	public void linkVisuals( Char ch ){
		super.linkVisuals( ch );
		play( idle, true );
	}

	@Override
	public void turnTo( int from, int to ){
		//a pole faces nowhere
	}

	@Override
	public void update(){
		super.update();
		if (ch == null || ch.pos < 0){
			putOut();
			return;
		}
		if (light != null && lightCell != ch.pos) putOut();
		if (light == null){
			PointF p = DungeonTilemap.tileCenterToWorld( ch.pos );
			light = new PlaceLight( ch.pos, p.x, p.y - 4, 23, p.x, p.y + DungeonTilemap.SIZE / 2f + 2, 15, 0xFFA050, 0.7f, FxLight.TORCH );
			lightCell = ch.pos;
		}
		if (visible) light.update();
	}

	private void putOut(){
		if (light != null) light.out();
		light = null;
		lightCell = -1;
	}

	//the overworld's window moved under it: the light is made again for its new cell
	@Override
	public void shiftWorld( float sx, float sy ){
		super.shiftWorld( sx, sy );
		putOut();
	}

	@Override
	public void kill(){
		putOut();
		super.kill();
	}

	@Override
	public void killAndErase(){
		putOut();
		super.killAndErase();
	}
}

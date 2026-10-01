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

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;

/**
 * The quiet presence of a held stance: two motes drifting slowly around the hero, and now and
 * then one accent particle. A handful of sprites, no allocation per frame; killed when the stance
 * is lowered or the sprite goes.
 */
public class StanceAuraFX extends Group {

	private final Char owner;
	private final Image[] motes = new Image[2];
	private final Emitter accent;
	private float time;

	public StanceAuraFX( Char owner, int color, int frame, Emitter.Factory accentFactory, float accentInterval ){
		this.owner = owner;
		for (int i = 0; i < motes.length; i++){
			Image mote = new Image( "effects/skill_motes.png" );
			mote.frame( new TextureFilm( mote.texture, 4, 4 ).get( frame ) );
			mote.hardlight( color );
			add( motes[i] = mote );
		}
		if (accentFactory != null && owner.sprite != null){
			accent = new Emitter();
			accent.pos( owner.sprite );
			accent.autoKill = false;
			add( accent );
			accent.pour( accentFactory, accentInterval );
		} else {
			accent = null;
		}
	}

	@Override
	public void update(){
		super.update();
		if (owner.sprite == null || !owner.sprite.exists){ killAndErase(); return; }
		visible = owner.sprite.visible;
		if (accent != null) accent.on = visible;
		time += Game.elapsed;
		PointF c = owner.sprite.center();
		for (int i = 0; i < motes.length; i++){
			double a = time * 1.3 + i * Math.PI;
			motes[i].x = c.x + (float) Math.cos( a ) * 8 - 2;
			motes[i].y = c.y - 3 + (float) Math.sin( a ) * 3.5f - 2;
			motes[i].alpha( 0.45f + 0.35f * (float) Math.sin( time * 2.2 + i * 1.7 ) );
		}
	}

	public void stop(){
		if (accent != null) accent.on = false;
		killAndErase();
	}
}

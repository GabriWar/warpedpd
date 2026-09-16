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

import com.watabou.noosa.Game;
import com.watabou.noosa.Group;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.utils.PointF;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpiritArmorMotes;

/** Spirit Armor's mana motes, circling the hero; purely cosmetic, reads the buff's count every frame. */
public class SpiritArmorFX extends Group {

	private static final int MAX = SpiritArmorMotes.capacity( 3 );

	private final Char owner;
	private final SpiritArmorMotes state;
	private final Image[] motes = new Image[MAX];
	private float time;

	public SpiritArmorFX( Char owner, SpiritArmorMotes state ){
		this.owner = owner;
		this.state = state;
		for (int i = 0; i < MAX; i++){
			Image mote = new Image( "effects/skill_motes.png" );
			mote.frame( new TextureFilm( mote.texture, 4, 4 ).get( 3 ) );
			mote.hardlight( 0x66CCFF );
			mote.scale.set( 1.5f );
			add( motes[i] = mote );
		}
	}

	@Override
	public void update(){
		super.update();
		if (owner.sprite == null || !owner.sprite.exists){ killAndErase(); return; }
		visible = owner.sprite.visible;
		time += Game.elapsed;
		PointF center = owner.sprite.center();
		int count = Math.min( MAX, state.motes );
		for (int i = 0; i < MAX; i++){
			Image mote = motes[i];
			mote.visible = i < count;
			if (!mote.visible) continue;
			double a = time * 2.4 + i * Math.PI * 2 / count;
			mote.x = center.x + (float) Math.cos( a ) * 10 - mote.width() / 2;
			mote.y = center.y - 2 + (float) Math.sin( a ) * 5 - mote.height() / 2;
			mote.alpha( 0.75f + 0.25f * (float) Math.sin( time * 6 + i ) );
		}
	}
}

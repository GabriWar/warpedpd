/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Overgrown Pixel Dungeon
 * Copyright (C) 2022-2025 Overgrown Team
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

package xyz.gabriwar.warpedpixeldungeon.actors.blobs;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SteamParticle;
import com.watabou.noosa.particles.Emitter;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Steaming;
import xyz.gabriwar.warpedpixeldungeon.effects.BlobEmitter;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;

public class Steam extends Blob {

	@Override
	protected void evolve() {
		super.evolve();

		Char ch;
		int cell;

		for (int i = area.left; i < area.right; i++){
			for (int j = area.top; j < area.bottom; j++){
				cell = i + j*Dungeon.level.width();
				if (cur[cell] > 0) {
					TileTemperature.depositHeat(cell, cur[cell] * 0.12f);
					if ((ch = Actor.findChar( cell )) != null && !ch.isImmune(this.getClass())) {
						Buff.affect(ch, Steaming.class).set(2);
					}
				}
			}
		}
	}

	@Override
	public void use( BlobEmitter emitter ) {
		super.use( emitter );
		emitter.pour( CLOUD, 0.3f );
	}

	/** A steam cloud's look: wisps of steam (SteamParticle) rising off every cell, and one visit in
	 *  four a soft body of it hanging there (a middling puff at 0.3, 2-3 s). */
	public static final Emitter.Factory CLOUD = new Emitter.Factory(){
		@Override
		public void emit( Emitter e, int index, float x, float y ){
			if ((index & 3) != 0){
				SteamParticle.FACTORY.emit( e, index, x, y );
				return;
			}
			if (!FxBudget.keep( FxBudget.P1, x, y )) return;
			float life = FxRandom.Float( 2f, 3f );
			((SteamParticle) e.recycle( SteamParticle.class )).reset( x, y ).look( FxFrames.PUFF_M[FxRandom.Int( 2 )] )
					.paint( SteamParticle.COLOR ).smooth().life( life ).alpha( 0.3f, life * 0.3f, life * 0.4f )
					.vel( FxRandom.Float( -2f, 2f ), -FxRandom.Float( 1f, 3f ) ).turbulence( 1.5f, 1.5f ).wind( 0.15f )
					.priority( FxBudget.P1 );
		}
	};

	@Override
	public String tileDesc() {
		return Messages.get(this, "desc");
	}
}

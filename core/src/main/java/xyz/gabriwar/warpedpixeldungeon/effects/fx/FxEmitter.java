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

package xyz.gabriwar.warpedpixeldungeon.effects.fx;

import com.watabou.noosa.particles.Emitter;

/**
 * The kit's emitter: an Emitter whose start and spawn points draw from FxRandom, never from the
 * game's seeded Random, so starting one costs gameplay nothing whichever thread it is on; and
 * never sent on to a co-op guest (the trigger it answers to already reaches him). Pooled by the
 * game's scene in its three layers: GameScene.fxEmitter() among the effects, lightEmitter() in
 * the light layer, floorFxEmitter() on the floor. Frozen with every emitter (freezeEmitters).
 */
public class FxEmitter extends Emitter {

	@Override
	public void start( Factory factory, float interval, int quantity ){
		netCell = -1;
		netEmType = null;
		startDelayed( factory, interval, quantity, interval > 0 ? FxRandom.Float( interval ) : 0f );
	}

	@Override
	public void burst( Factory factory, int quantity ){
		start( factory, 0, quantity );
	}

	@Override
	protected void emit( int index ){
		if (target == null){
			factory.emit( this, index, x + FxRandom.Float( width ), y + FxRandom.Float( height ) );
		} else if (fillTarget){
			factory.emit( this, index,
					target.x + FxRandom.Float( target.width() ),
					target.y + FxRandom.Float( target.height() ) );
		} else {
			factory.emit( this, index,
					target.x + x + FxRandom.Float( width ),
					target.y + y + FxRandom.Float( height ) );
		}
	}

	@Override
	public void revive(){
		super.revive();
		netCell = -1;
		netEmType = null;
		lightMode = false;
	}

	/** Drawn as light (added) though it is never started: a holder of loose light. */
	public void holdLight(){
		lightMode = true;
	}
}

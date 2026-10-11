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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.fire;

import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;

/**
 * What a house on fire adds to its flames and its black smoke (levels/overworld/
 * SettlementAmbience): embers rising off its roof into the smoke, one every EMBER_EVERY s, and the
 * fire's light on the roof - a persistent pool flickering as a torch. Its owner lights it, slides
 * it with a mirror's window and puts it out. Render thread.
 */
public final class BurningRoof {

	/** An ember every this many s; the roof's light's level. */
	public static final float EMBER_EVERY = 0.3f, LEVEL = 0.8f;

	private final FxEmitter embers;
	private final LightRef light = new LightRef();

	private BurningRoof( FxEmitter embers ){
		this.embers = embers;
	}

	/** A roof burning over the rect (x, y, w, h) of scene pixels; null off the game's scene. */
	public static BurningRoof light( float x, float y, float w, float h ){
		FxEmitter e = GameScene.lightEmitter();
		if (e == null) return null;
		e.pos( x, y, w, h );
		e.start( FxFactories.ember( Element.FIRE ), EMBER_EVERY, 0 );
		BurningRoof r = new BurningRoof( e );
		r.light.setLasting( FxLight.pool( x + w / 2f, y + h / 2f + 4, 31, Element.FIRE.light, LEVEL )
				.flicker( FxLight.TORCH ).persistent() );
		return r;
	}

	/** Moved with a mirror's window by (dx, dy). */
	public void slide( float dx, float dy ){
		embers.pos( embers.x + dx, embers.y + dy, embers.width, embers.height );
		FxLight l = light.get();
		if (l != null){
			l.x += dx;
			l.y += dy;
		}
	}

	/** The fire out: no more embers, its light fading. */
	public void out(){
		if (embers.parent != null) embers.on = false;
		light.release( 0.6f );
	}
}

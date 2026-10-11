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
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import com.watabou.noosa.Visual;

/**
 * What an elemental's body gives off besides its particles (sprites/ElementalSprite, FistSprite,
 * VaultBossElementalSprite): a burning one a glow on its body (GLOW_15, 0.4) that moves with it
 * and a shimmer of heat rising off it every RAY_EVERY s; a frozen one a glint now and then; a
 * charged one a crackle of sparks; a chaotic one motes of every colour. Its owner updates it
 * every frame its sprite is shown and puts it out when the body falls. Render thread; nothing
 * draws the game's random numbers.
 */
public final class BodyGlow {

	public static final int FIRE = 0, ELMO = 1, FROST = 2, SHOCK = 3, CHAOS = 4;
	/** The body's glow as shown, and how often each kind's detail comes, s. */
	public static final float GLOW = 0.4f, RAY_EVERY = 0.5f, GLINT_EVERY = 0.8f, CRACKLE_EVERY = 0.4f, MOTE_EVERY = 0.3f;

	private final int kind;
	private final LightRef light = new LightRef();
	private float detailIn;
	//a glow the kit took for a newer light is asked for again only after RETRY s, and only when
	//the kit has room for it (no churn of lights taking each other's places)
	private float retryIn;
	/** How long it waits to light its body again after losing its glow, s. */
	public static final float RETRY = 2f;

	public BodyGlow( int kind ){
		this.kind = kind;
		detailIn = FxRandom.Float( 0.5f );
	}

	/** Every frame: its glow kept on the body and its detail when due; let go when not shown. */
	public void update( Visual body, boolean shown, float dt ){
		if (!shown){
			light.release( 0.2f );
			return;
		}
		float cx = body.x + body.width() / 2f, cy = body.y + body.height() / 2f;
		if (kind == FIRE || kind == ELMO){
			int color = kind == FIRE ? Element.FIRE.light : Element.ELMO.light;
			if (light.held()) light.place( cx, cy, GLOW / FxLight.GLOW_CAP );
			else if ((retryIn -= dt) <= 0 && LightRef.room( 1, 0 )){
				retryIn = RETRY;
				light.set( FxLight.glow( cx, cy, 15, color, GLOW / FxLight.GLOW_CAP ).flicker( FxLight.HEARTH ).day( Fx.DAY_PERSISTENT ) );
			}
		}
		if ((detailIn -= dt) > 0) return;
		FxEmitter glow = Fx.glowAir();
		if (glow == null) return;
		float x = cx + FxRandom.Float( -5f, 5f ), y = cy + FxRandom.Float( -6f, 5f );
		switch (kind){
			case FIRE:
			case ELMO:
				detailIn = RAY_EVERY;
				HeatRay.rise( glow, x, cy + 4 );
				break;
			case FROST:
				detailIn = GLINT_EVERY;
				FxFactories.glint( Element.FROST.glint ).emit( glow, 0, x, y );
				break;
			case SHOCK:
				detailIn = CRACKLE_EVERY;
				for (int i = 0; i < 2; i++) FxFactories.spark( Element.ELECTRIC ).emit( glow, i, x, y );
				break;
			default:
				detailIn = MOTE_EVERY;
				FxFactories.mote( Element.PRISM.ramp.stop( FxRandom.Int( Element.PRISM.ramp.size() ) ), true ).emit( glow, 0, x, y );
		}
	}

	/** Its body fell or is gone: its glow out. */
	public void out(){
		light.release( 0.3f );
	}
}

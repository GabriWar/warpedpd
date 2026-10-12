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
import com.watabou.noosa.Game;
import com.watabou.noosa.TextureFilm;

/**
 * A carved stone of the homesteads' travel (sprites/homestead.png): a dormant frame, and a lit
 * animation with a small glow of its rune's light over it (FxLight: a lasting light that asks for
 * room first, breathing, in the light layer). Whether it is lit is asked a few times a second
 * (lit()), so it wakes and sleeps with the hour or its attunement on any screen. Furniture: it
 * never turns, and its run, attack and death are clones of its idle, never the idle itself.
 */
public abstract class GlowStoneSprite extends MobSprite {

	//how often lit() is asked, seconds
	private static final float CHECK_EVERY = 0.5f;

	private final Animation dormant, lit;
	private boolean showingLit;
	private float checkIn;
	private FxLight glow;

	protected GlowStoneSprite( int dormantFrame, int litFrame, int litFrames, int litFps ){
		super();

		texture( HomesteadSprites.TEXTURE );
		TextureFilm film = HomesteadSprites.film();

		dormant = new Animation( 1, true );
		dormant.frames( film, dormantFrame );
		lit = new Animation( litFps, true );
		lit.frames( film, HomesteadSprites.frames( litFrame, litFrames ) );

		idle = dormant;
		//never the idle instance itself: a looped idle that IS attack fires onAttackComplete
		//every cycle (see TemperingAnvilSprite)
		run = dormant.clone();
		attack = dormant.clone();
		die = dormant.clone();

		play( idle );
	}

	/** Is the stone lit now? */
	protected abstract boolean lit();

	/** Its glow's colour, size (FxLight.glow) and level. */
	protected abstract int glowColor();

	protected abstract int glowSize();

	protected abstract float glowLevel();

	@Override
	public void link( Char ch ){
		super.link( ch );
		renderShadow = false;
		checkIn = 0;
	}

	@Override
	public void update(){
		super.update();
		checkIn -= Game.elapsed;
		if (checkIn <= 0 && ch != null){
			checkIn = CHECK_EVERY;
			show( lit() );
		}
	}

	private void show( boolean on ){
		if (on != showingLit){
			showingLit = on;
			idle = on ? lit : dormant;
			play( idle, true );
		}
		if (on && glow == null && visible && parent != null && FxLight.room( 1, 1 )){
			glow = FxLight.glow( x + width / 2f, y + height / 2f, glowSize(), glowColor(), glowLevel() )
					.flicker( FxLight.BREATH ).follow( this, 0, -2 ).persistent();
		} else if (!on && glow != null){
			putOut();
		}
	}

	private void putOut(){
		if (glow != null){
			glow.putOut( 0.5f );
			glow = null;
		}
	}

	@Override
	public void turnTo( int from, int to ){
		//a stone faces nowhere
	}

	@Override
	public void die(){
		super.die();
		putOut();
	}

	@Override
	public void kill(){
		super.kill();
		putOut();
	}

	@Override
	public int blood(){
		return 0xFF6E6E78;
	}
}

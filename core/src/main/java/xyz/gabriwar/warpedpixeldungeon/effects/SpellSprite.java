/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcImage;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

import java.util.HashMap;

//a spell's sign over a character's head: it pops up (3 px, settling back 1, a position and never
//a scale), with a sparkle at its top right, on a soft plate of its own colour; it floats 2-3 px
//while it shows and leaves in four painted steps of 0.1 s
//FIXME this is seriously underused atm, should add more of these!
public class SpellSprite extends Image {

	public static final int FOOD		= 0;
	public static final int MAP			= 1;
	public static final int CHARGE		= 2;
	public static final int BERSERK     = 3;
	public static final int ANKH        = 4;
	public static final int HASTE       = 5;
	public static final int VISION      = 6;
	public static final int PURITY      = 7;

	private static final int SIZE	= 16;

	private enum Phase {
		FADE_IN, STATIC, FADE_OUT
	}

	private static final float FADE_IN_TIME		= 0.2f;
	private static final float STATIC_TIME		= 0.8f;
	private static final float FADE_OUT_TIME	= 0.4f;

	/** Its pop: up RISE px, settling back SETTLE; its float (px either side of its middle, Hz). */
	public static final int RISE = 3, SETTLE = 1;
	public static final float FLOAT = 1f, FLOAT_HZ = 1.2f;
	/** Its plate's alpha; its exit's painted steps. */
	public static final float PLATE = 0.3f;
	public static final float[] EXIT = { 1f, 0.6f, 0.3f, 0f };
	/** Each sign's own colour, where its caller gives none. */
	static final int[] COLORS = { 0xF0E0C0, 0xCCFFFF, 0xFFFF66, 0xFF4040, 0xFFE070, 0xFFFFFF, 0xE0E8FF, 0xA8E040 };

	private static TextureFilm film;

	private Char target;

	private Phase phase;
	private float duration;
	private float passed;
	private int index;
	private float age;
	private ArcImage plate;

	private static HashMap<Char,SpellSprite> all = new HashMap<>();

	public SpellSprite() {
		super( Assets.Effects.SPELL_ICONS );

		if (film == null) {
			film = new TextureFilm( texture, SIZE );
		}
	}

	public void reset( int index ) {
		frame( film.get( index ) );
		origin.set( width / 2, height / 2 );
		this.index = index;

		phase = Phase.FADE_IN;

		duration = FADE_IN_TIME;
		passed = 0;
		age = 0;
	}

	/** Its rise over its owner's head at a share p of its pop: up 3, then back down 1. */
	/** Its float while it shows, `age` s in: whole pixels, -1, 0 or +1 (3 px of travel). */
	public static int floatAt( float age ){
		return Math.round( FLOAT * (float)Math.sin( 2 * Math.PI * FLOAT_HZ * age ) );
	}

	public static float pop( float p ){
		if (p < 0.6f) return RISE * p / 0.6f;
		return RISE - SETTLE * (p - 0.6f) / 0.4f;
	}

	//its colour: its tint's, else its sign's own
	private int color(){
		if (ra + ga + ba > 0.01f && !(ra > 0.99f && ga > 0.99f && ba > 0.99f)){
			return (Math.round( Math.min( 1f, ra ) * 255 ) << 16) | (Math.round( Math.min( 1f, ga ) * 255 ) << 8)
					| Math.round( Math.min( 1f, ba ) * 255 );
		}
		return COLORS[Math.max( 0, Math.min( COLORS.length - 1, index ) )];
	}

	@Override
	public void update() {
		super.update();

		float lift = 0;
		if (phase == Phase.FADE_IN) lift = pop( passed / duration );
		else if (phase != null) lift = RISE - SETTLE + floatAt( age );

		if (target != null && target.sprite != null) {
			x = Math.round( target.sprite.x + target.sprite.width() / 2f - SIZE / 2 );
			y = Math.round( target.sprite.y - SIZE - lift );
		}

		if (phase == null || target == null){
			return;
		}
		age += Game.elapsed;

		switch (phase) {
			case FADE_IN:
				alpha( passed / duration );
				break;
			case STATIC:
				alpha( 1 );
				break;
			case FADE_OUT:
				alpha( EXIT[Math.min( EXIT.length - 1, (int)(passed / duration * EXIT.length) )] );
				break;
		}
		plate();

		if ((passed += Game.elapsed) > duration) {
			switch (phase) {
			case FADE_IN:
				phase = Phase.STATIC;
				duration = STATIC_TIME;
				break;
			case STATIC:
				phase = Phase.FADE_OUT;
				duration = FADE_OUT_TIME;
				break;
			case FADE_OUT:
				kill();
				break;
			}

			passed = 0;
		}
	}

	//its plate of light behind it, and the sparkle as it appears
	private void plate(){
		if (plate == null || !plate.exists){
			if (parent == null) return;
			plate = ArcImage.in( parent, true, true );
			parent.sendToBack( plate );
			plate.show( FxFrames.Light.GLOW_23 );
			if (Arc.onScene()){
				FxParticle s = Arc.light( x + SIZE - 3, y + 3, FxBudget.P2 );
				if (s != null) s.look( SPARKLE, 0 ).paint( 0xFFFFFF ).life( 0.35f ).alpha( 1f, 0, 0.1f ).snap();
			}
		}
		plate.rgb( color() );
		plate.centre( x + SIZE / 2f, y + SIZE / 2f );
		plate.am = PLATE * am;
		plate.visible = visible && am > 0.004f;
	}

	static final int[][] SPARKLE = { FxFrames.PLUS_3, FxFrames.PLUS_5, FxFrames.PLUS_3 };

	@Override
	public void kill() {
		super.kill();
		all.remove( target );
		if (plate != null) plate.kill();
		plate = null;
	}

	public static void show( Char ch, int index ) {
		show(ch, index, 1, 1, 1);
	}

	public static void show( Char ch, int index, float r, float g, float b ) {

		if (!ch.sprite.visible) {
			return;
		}

		SpellSprite old = all.get( ch );
		if (old != null) {
			old.kill();
		}

		SpellSprite sprite = GameScene.spellSprite();
		sprite.target = ch;
		sprite.reset( index );
		sprite.hardlight(r, g, b);
		sprite.revive();
		all.put( ch, sprite );
	}
}

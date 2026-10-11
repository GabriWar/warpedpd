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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxEmitter;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxLight;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.Arc;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.arcane.ArcImage;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.InfusionParticle;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import com.watabou.noosa.Game;

//an item being enchanted over its owner's head: its ghost fading in (never scaled up from
//nothing), runes circling it on a 10x4 ellipse (dimmer behind it), sparks spiralling into it as
//the enchantment takes, then it is drawn into its owner - four motes and a glow on him - stepping
//to x2 as it fades. At most DRAWS pictures; let go of with it.
public class Enchanting extends ItemSprite {

	private enum Phase {
		FADE_IN, STATIC, FADE_OUT
	}

	private static final float FADE_IN_TIME		= 0.2f;
	private static final float STATIC_TIME		= 1.0f;
	private static final float FADE_OUT_TIME	= 0.4f;

	private static final float ALPHA	= 0.6f;

	/** Its runes: how many, the ellipse they circle (px), how fast (rad/s), the far half's alpha. */
	public static final int RUNES = 6;
	public static final float RUNE_RX = 5f, RUNE_RY = 2f, RUNE_TURN = 2.4f, BACK = 0.5f;
	/** The sparks spiralling in, and the motes drawn into the owner. */
	public static final int SPARKS = 12, MOTES = 4;
	/** Its pictures at its peak: itself, the runes, the sparks (the motes and glow come after them). */
	public static final int DRAWS = 1 + RUNES + SPARKS;

	private int color;

	private Char target;

	private Phase phase;
	private float duration;
	private float passed;
	private float age;

	private ArcImage[] runes;

	public Enchanting( Item item ) {
		super( item.image(), null );
		//originToCenter();

		if (item.glowing() != null) {
			color = item.glowing().color;
		} else {
			color = -1;
		}

		phase = Phase.FADE_IN;
		duration = FADE_IN_TIME;
		passed = 0;
	}

	//the colour its magic shows in: its glow's, or the arcane's light
	private int magic(){
		return color != -1 ? color & 0xFFFFFF : Element.ARCANE.light;
	}

	@Override
	public void update() {
		super.update();

		if (target == null || target.sprite == null) {
			finish();
			return;
		}

		float cx = target.sprite.x + target.sprite.width() / 2f;
		x = cx - width() / 2;
		y = target.sprite.y - 8 - height()/2;
		float mx = x + width() / 2f, my = y + height() / 2f;
		age += Game.elapsed;

		switch (phase) {
			case FADE_IN:
				alpha( passed / duration * ALPHA );
				break;
			case STATIC:
				if (color != -1) {
					tint(color, passed / duration * 0.8f);
				}
				break;
			case FADE_OUT:
				alpha( (1 - passed / duration) * ALPHA );
				scale.set( 2 );
				x = cx - width() / 2;
				y = target.sprite.y - 8 - height() / 2;
				break;
		}
		runes( mx, my );

		if ((passed += Game.elapsed) > duration) {
			switch (phase) {
				case FADE_IN:
					phase = Phase.STATIC;
					duration = STATIC_TIME;
					sparks( mx, my );
					break;
				case STATIC:
					phase = Phase.FADE_OUT;
					duration = FADE_OUT_TIME;
					absorb( mx, my );
					break;
				case FADE_OUT:
					finish();
					return;
			}

			passed = 0;
		}
	}

	//six runes circling it, the far half dimmer
	private void runes( float mx, float my ){
		if (runes == null){
			if (!Arc.onScene()) return;
			runes = new ArcImage[RUNES];
			for (int i = 0; i < RUNES; i++){
				runes[i] = ArcImage.get( GameScene.Layer.LIGHTS, false, true );
				runes[i].show( FxFrames.RUNE_3[i % FxFrames.RUNE_3.length] ).rgb( magic() );
			}
		}
		float fade = phase == Phase.FADE_IN ? passed / duration : (phase == Phase.FADE_OUT ? 1 - passed / duration : 1f);
		for (int i = 0; i < RUNES; i++){
			float a = (float)(2 * Math.PI * i / RUNES) + age * RUNE_TURN;
			float sin = (float)Math.sin( a );
			runes[i].centre( mx + (float)Math.cos( a ) * RUNE_RX, my + 6 + sin * RUNE_RY );
			runes[i].am = (sin < 0 ? BACK : 1f) * fade;
		}
	}

	//the enchantment taking hold: sparks spiralling into it
	private void sparks( float mx, float my ){
		FxEmitter e = GameScene.lightEmitter();
		if (e == null) return;
		e.pos( mx, my );
		e.burst( InfusionParticle.spiralIn( magic() ), SPARKS );
	}

	//drawn into its owner: four motes falling into him, a glow on him
	private void absorb( float mx, float my ){
		if (!Arc.onScene()) return;
		float tx = target.sprite.x + target.sprite.width() / 2f, ty = target.sprite.y + target.sprite.height() / 2f;
		for (int i = 0; i < MOTES; i++){
			FxParticle p = Arc.light( mx + (i - 1.5f) * 3f, my, FxBudget.P1 );
			if (p == null) break;
			p.look( FxFrames.MOTE_2 ).paint( magic() ).vel( (tx - mx) / 0.3f, (ty - my) / 0.3f ).life( 0.3f ).alpha( 1f, 0.05f, 0.1f );
		}
		FxLight l = Arc.glow( tx, ty, 15, magic(), 0.5f );
		if (l != null) l.life( 0.25f, 0, 0.15f );
	}

	//gone, its runes and its own buffer let go of
	private void finish(){
		if (runes != null) for (ArcImage r : runes) r.kill();
		runes = null;
		Fx.dispose( this );
	}

	public static void show( Char ch, Item item ) {

		if (!ch.sprite.visible) {
			return;
		}

		Enchanting sprite = new Enchanting( item );
		sprite.target = ch;
		ch.sprite.parent.add( sprite );
	}
}

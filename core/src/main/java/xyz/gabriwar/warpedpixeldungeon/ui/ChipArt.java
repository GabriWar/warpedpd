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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.HitCard;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;
import com.watabou.noosa.ui.Component;

/**
 * One chip of a hit card drawn outside it, exactly as the card draws it (plan's chip codes):
 * - code >= SKILL_BASE: the hero skill icon (code - SKILL_BASE) of hero_skills.png at half size in a
 *   10 px square (nothing behind it), 11 px to the next chip;
 * - 0 <= code < SKILL_BASE: a text_icons.png cell (7x8) drawn bare, 8 px to the next (a hollow
 *   family cell, an echo's, a little see-through);
 * - code < 0: the small buff icon -(code + 1) (7x7) in a 9 px square (nothing behind it), 10 px to the next.
 * Its tint (0 = plain) is laid on with hardlight. Every chip of a row sits on one bottom line:
 * place it with bottom(x, y). Render thread.
 */
public class ChipArt extends Component {

	/** Skill icons start here in the chip codes (the hit card's own). */
	public static final int SKILL_BASE = HitCard.SKILL_BASE;

	private static TextureFilm buffFilm, skillFilm;

	public final int code;
	//a buff's or a skill's chip stands in a square, its icon 1 px in (nothing drawn behind it)
	private final boolean boxed;
	private final Image icon;
	private float alpha = 1f;

	public ChipArt( int code, int tint ){
		super();
		this.code = code;
		if (code >= SKILL_BASE){
			boxed = true;
			icon = new Image( Assets.Sprites.HERO_SKILLS );
			if (skillFilm == null) skillFilm = new TextureFilm( icon.texture, 16, 16 );
			icon.frame( skillFilm.get( code - SKILL_BASE ) );
			icon.scale.set( 0.5f );
		} else if (code < 0){
			boxed = true;
			icon = new Image( Assets.Interfaces.BUFFS_SMALL );
			if (buffFilm == null) buffFilm = new TextureFilm( icon.texture, 7, 7 );
			icon.frame( buffFilm.get( -code - 1 ) );
		} else {
			boxed = false;
			icon = new Image( Assets.Effects.TEXT_ICONS );
			icon.frame( FloatingText.iconFilm.get( code ) );
		}
		add( icon );
		icon.resetColor();
		if (tint != 0) icon.hardlight( tint );
		width = pitch( code ) - 1;
		height = tall( code );
		alpha( 1f );
	}

	/** How far its left edge is from the next chip's on a card. */
	public static int pitch( int code ){
		return code >= SKILL_BASE ? 11 : code < 0 ? 10 : FloatingText.ICON_WIDTH + 1;
	}

	/** How tall it stands over the row's bottom line. */
	public static int tall( int code ){
		return code >= SKILL_BASE ? 10 : code < 0 ? 9 : FloatingText.ICON_HEIGHT;
	}

	//an echo's family mark (the hollow twin) is drawn a little see-through on the card
	private static boolean echo( int code ){
		return code >= FloatingText.hollow( FloatingText.F_EMBER ) && code <= FloatingText.hollow( FloatingText.F_HEX );
	}

	/** Puts its left edge at x and its bottom on the row's line y. */
	public ChipArt bottom( float x, float y ){
		setPos( x, y - height );
		return this;
	}

	@Override
	protected void layout(){
		float top = y;
		if (boxed){
			icon.x = x + 1f;
			icon.y = top + 1f;
		} else {
			icon.x = x;
			icon.y = top;
		}
	}

	/** Fades it as a whole. */
	public void alpha( float a ){
		alpha = a;
		icon.alpha( a * (!boxed && echo( code ) ? HitCard.ECHO_ALPHA : 1f) );
	}

	public float alpha(){
		return alpha;
	}
}

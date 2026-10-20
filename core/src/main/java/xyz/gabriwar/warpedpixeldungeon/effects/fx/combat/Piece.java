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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;

/**
 * What a picture hands the open blow (Blow.offer) instead of drawing at once: its rank (what
 * leads), its lane (where its accent goes), the character it plays on, its card chip, its accent
 * priority and the buffs it stands for (their announcements fold into its chip). The presenter
 * (BlowPresenter) calls lead() for the one that leads and accent() for the others on their beats,
 * on the render thread; each draws its whole look (the owner's call: a blow arranges its pictures,
 * it never thins them), from the effects' own random numbers only.
 *
 * Made on whichever thread the gameplay runs (fields set before it is offered, read after the blow
 * closes); drawn on the render thread. docs/fx/hit-card.md.
 */
public abstract class Piece {

	public static final Class<?>[] NONE = new Class<?>[0];

	/** Its PieceFx / EnchantFx wire kind (for guests); -1 for a host-only picture. */
	public final int kind;
	/** Blow.FINISHER .. Blow.MINOR. */
	public final int rank;
	/** Blow.TARGET .. Blow.MOTION. */
	public final int lane;
	/** The character it plays on. */
	public final Char on;
	/** Its card chip, or -1 (code as in Blow.chip), and the chip's tint (0: drawn plain). */
	public int chip = -1, chipTint = 0;
	/** Its accent priority (Blow.A_TWIST .. Blow.A_LATE): the order of the beats. */
	public int priority = Blow.A_PERK;
	/** Buff classes this piece stands for: their announcements fold into its chip. */
	public Class<?>[] folds = NONE;

	protected Piece( int kind, int rank, int lane, Char on ){
		this.kind = kind;
		this.rank = rank;
		this.lane = lane;
		this.on = on;
	}

	/** Its full form, on the render thread, at t = 0. mayShake: the hero is in the blow (it shakes
	 *  by its own rule then). */
	public abstract void lead( boolean mayShake );

	/** Its form on its beat at (x, y) (the presenter's point for TARGET and WIELDER lanes, a few
	 *  pixels higher each beat; PLACED pieces keep their own geometry and ignore it). No shake: the
	 *  lead's, on the blow's frame, is the blow's. */
	public abstract void accent( float x, float y );

	/** Its sound, played only when it leads. */
	public void sting(){}

	/** When what it carries to the blow's wielder (a drain's beads) reaches him, s after its own
	 *  beat; -1 when it carries nothing home. The presenter holds the wielder's small card (a heal,
	 *  mana, a shield) for the latest of the blow's, as Blow.flowBack does. Read once its place on
	 *  the blow is set (a lead's after it drew). */
	public float home(){
		return -1f;
	}

	/** The host's word of it to co-op guests (EnchantNet), tagged with its blow's serial: sent by the
	 *  presenter before the blow's CARD. Nothing by default (a host-only picture). */
	public void send( int serial ){}
}

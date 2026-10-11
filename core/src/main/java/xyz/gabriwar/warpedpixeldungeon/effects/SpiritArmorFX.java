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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SpiritArmorMotes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRecipes;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillOrbit;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * Spirit Armor's mana motes circling the hero in the kit's SPIRIT_ARMOR orbit, one a mote held:
 * additive, at x1, trailed as far as the orbit's six quads allow (two motes with two fainter
 * copies, three with one, four bare), a cross instead of a dot once the ring is full; the ring
 * flares as a mote forms, and a mote spent flies to what it strikes in FLY s and bursts there in
 * sparks. Purely cosmetic: it reads the buff's count every frame.
 */
public class SpiritArmorFX extends SkillOrbit {

	public static final int COLOR = 0x66CCFF;
	public static final float FLY = 0.15f;
	public static final int SPARKS = 4;

	private static final int MAX = SpiritArmorMotes.capacity( 3 );

	private final SpiritArmorMotes state;
	private int last = -1;

	public SpiritArmorFX( Char owner, SpiritArmorMotes state ){
		super( owner, FxOrbit.Slot.SPIRIT_ARMOR );
		this.state = state;
	}

	/** How many trailing copies n motes may have within the orbit's quads. */
	public static int trailFor( int n ){
		return n <= 2 ? 2 : (n == 3 ? 1 : 0);
	}

	@Override
	protected int count(){
		return Math.min( MAX, state.motes );
	}

	@Override
	protected void look( FxOrbit.Ring ring, int n ){
		ring.motes( n, state.full() ? FxFrames.PLUS_3 : FxFrames.MOTE_2, COLOR, true ).trail( trailFor( n ) )
				.breath( 0.75f, 1f, 0.9f ).speed( 2.4f );
	}

	@Override
	protected void tick( float dt ){
		int n = count();
		//a mote formed: the ring brightens
		if (last >= 0 && n > last && ring != null) ring.flare( 0.5f, 0.3f, 0 );
		last = n;
	}

	/** A mote spent on `aim`: it leaves the ring and flies there in FLY s, bursting in sparks.
	 *  Any thread. */
	public static void spend( final Char owner, final Char aim ){
		if (owner == null || aim == null) return;
		Fx.post( () -> {
			CharSprite from = owner.sprite, to = aim.sprite;
			if (from == null || to == null || !from.visible) return;
			float sx = SkillVfx.midX( from ), sy = SkillVfx.midY( from ) + FxOrbit.Slot.SPIRIT_ARMOR.h;
			float tx = SkillVfx.midX( to ), ty = SkillVfx.midY( to );
			//from the side of the ring facing it
			float dx = tx - sx, dy = ty - sy, d = Math.max( 1f, (float)Math.sqrt( dx * dx + dy * dy ) );
			sx += dx / d * FxOrbit.Slot.SPIRIT_ARMOR.rx;
			sy += dy / d * FxOrbit.Slot.SPIRIT_ARMOR.ry;
			FxParticle p = SkillVfx.glow( sx, sy, FxBudget.P1 );
			if (p == null) return;
			p.look( FxFrames.MOTE_2 ).paint( COLOR ).vel( (tx - sx) / FLY, (ty - sy) / FLY ).life( FLY ).alpha( 1f, 0, 0.03f )
					.fate( ( q, landed ) -> FxRecipes.sparks( q.px, q.py, Element.SACRIFICIAL, SPARKS ) );
		} );
	}
}

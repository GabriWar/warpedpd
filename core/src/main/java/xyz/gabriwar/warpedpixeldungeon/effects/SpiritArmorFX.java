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
 * copies, three with one, four bare). Once the ring is full its motes are runes (RUNE_3, the glyph
 * changing GLYPHS_A_SECOND times a second: the ring armed with the runes the volley throws; never a
 * plus, which is the heal's picture), the same quads. The ring flares as a mote forms, and a mote
 * spent leaves it as a rune (RUNE_3 lit COLOR, its glyph changing every SPEND_GLYPH s) flying to
 * what it strikes in FLY s and bursting there in sparks. Purely cosmetic: it reads the buff's count
 * every frame.
 */
public class SpiritArmorFX extends SkillOrbit {

	public static final int COLOR = 0x66CCFF;
	public static final float FLY = 0.15f;
	public static final int SPARKS = 4;
	/** A full ring's runes change glyph this often a second; a spent rune every SPEND_GLYPH s. */
	public static final float GLYPHS_A_SECOND = 4f, SPEND_GLYPH = 0.05f;

	private static final int MAX = SpiritArmorMotes.capacity( 3 );

	private final SpiritArmorMotes state;
	private int last = -1, glyph = -1;

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

	//the rune a full ring shows now
	private int glyphNow(){
		return (int) (time * GLYPHS_A_SECOND) % FxFrames.RUNE_3.length;
	}

	@Override
	protected void look( FxOrbit.Ring ring, int n ){
		glyph = glyphNow();
		ring.motes( n, state.full() ? FxFrames.RUNE_3[glyph] : FxFrames.MOTE_2, COLOR, true ).trail( trailFor( n ) )
				.breath( 0.75f, 1f, 0.9f ).speed( 2.4f );
	}

	@Override
	protected void tick( float dt ){
		int n = count();
		//a mote formed: the ring brightens
		if (last >= 0 && n > last && ring != null) ring.flare( 0.5f, 0.3f, 0 );
		last = n;
		//a full ring's runes change glyph (the orbit takes one frame: all of them at once)
		if (ring != null && n > 0 && state.full() && glyphNow() != glyph && leads()) look( ring, n );
	}

	/** A mote spent on `aim`: it leaves the ring as a rune and flies there in FLY s, bursting in
	 *  sparks. Any thread. */
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
			p.look( FxFrames.RUNE_3, 1f / SPEND_GLYPH ).paint( COLOR ).vel( (tx - sx) / FLY, (ty - sy) / FLY ).life( FLY ).alpha( 1f, 0, 0.03f )
					.fate( ( q, landed ) -> FxRecipes.sparks( q.px, q.py, Element.SACRIFICIAL, SPARKS ) );
		} );
	}
}

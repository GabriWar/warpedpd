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
package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview;

import com.watabou.utils.Bundle;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.SkillField;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFieldFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StanceAuraFX;

/**
 * Round 2's Sanctuary moments as master f6f2758ae shows them, and their stagings, which
 * SkillPreviewsRfx2 shares (master's calls only, so this compiles there alone): dumped once in a
 * detached f6f2758ae worktree (FXPREVIEW_SIDE=before FXPREVIEW_REGISTRIES=Rfx2). The let-go is
 * master's: the picture goes at once.
 */
public final class SkillPreviewsRfx2Before {

	private SkillPreviewsRfx2Before(){}

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "sanctuary", s -> raised( s, 1 ) ).heroClass( HeroClass.CLERIC ).seconds( 2.5f );
		r.entry( "sanctuary-wide", s -> raised( s, 2 ) ).heroClass( HeroClass.CLERIC ).seconds( 2.5f );
		r.entry( "sanctuary-mend", SkillPreviewsRfx2Before::mend ).heroClass( HeroClass.CLERIC ).rat( 4, 4 ).seconds( 2.5f );
		r.entry( "sanctuary-end", s -> {
			final SkillFieldFX fx = held( s, 1 );
			s.later( 1.2f, () -> Fx.dispose( fx ) );
		} ).heroClass( HeroClass.CLERIC ).seconds( 2f );
	}

	static final int GOLD = 0xF1D99C;

	//Sanctuary.execute (activate), then the circle consecrated 0.6 s on (Sanctuary.consecrate)
	static void raised( SkillPreviewDump.Scene s, int reach ){
		SkillPreviewsRfxRunes.cast( new Sanctuary(), s.hero );
		PulseRingFX.around( s.h, GOLD, 24, 0.5f );
		s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 6 );
		Sanctuary.raised( s.h );
		s.h.parent.add( new StanceAuraFX( s.hero, 0xFFF1A1, SkillBursts.HOLY_MOTE, 1.6f ) );
		s.later( 0.6f, () -> {
			int[] cells = square( s.hero.pos, reach );
			field( cells, s.hero.pos, 0 );
			for (int c : cells) SkillInteractions.flare( c, 0xFFF1A1 );
			PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), GOLD, PixelFlare.L, 0.8f );
		} );
	}

	//a circle consecrated a turn ago (shown again: no opening), the hurt ally and the cleric mended
	//this turn and the next (SkillField.act)
	static void mend( SkillPreviewDump.Scene s ){
		held( s, 1 );
		s.later( 0.3f, () -> {
			SkillField.mended( s.d[0].ch );
			SkillField.mended( s.hero );
		} );
		s.later( 1.5f, () -> SkillField.mended( s.d[0].ch ) );
	}

	//a circle consecrated a turn ago round the hero
	static SkillFieldFX held( SkillPreviewDump.Scene s, int reach ){
		return field( square( s.hero.pos, reach ), s.hero.pos, 1 );
	}

	static int[] square( int middle, int reach ){
		int w = Dungeon.level.width(), side = 2 * reach + 1;
		int[] out = new int[side * side];
		int k = 0;
		for (int dy = -reach; dy <= reach; dy++) for (int dx = -reach; dx <= reach; dx++) out[k++] = middle + dx + dy * w;
		return out;
	}

	//a sanctuary on cells round its origin, as SkillField.place makes one, `age` turns on
	static SkillFieldFX field( int[] cells, int origin, int age ){
		Bundle b = new Bundle();
		b.put( "kind", SkillField.SANCTUARY );
		b.put( "rank", 1 );
		b.put( "remaining", 5 );
		b.put( "age", age );
		b.put( "cells", cells );
		b.put( "depth", Dungeon.depth );
		b.put( "branch", Dungeon.branch );
		b.put( "origin", origin );
		SkillField f = new SkillField();
		f.restoreFromBundle( b );
		SkillFieldFX fx = new SkillFieldFX( f );
		SkillFieldFX.show( fx );
		return fx;
	}
}

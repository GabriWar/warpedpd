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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShaftParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PillarRiseFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RogueHuntressAuras;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SeraphGuardianSprite;
import com.watabou.noosa.Group;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The meaning package's new preview names as f0783eacd draws them (its calls only, so this file
 * dumps in a detached f0783eacd checkout: rfx/previews/meaning/json-before-new): the four mana gains
 * without a gather and with Faith's lilac ring, the guardian's shafts, the dawn's wings with no heal
 * picture, Frost Nova without shards or rime, and the stance auras built for real (their motes in
 * f0783's sage, salmon and orange). The shared helpers are the after side's too.
 */
public final class SkillPreviewsRfxMeaningBefore {

	private SkillPreviewsRfxMeaningBefore(){}

	public static void register( SkillPreviewDump.Registry r ){
		r.entry( "mana-gains", s -> {
			new Flare( 6, 32 ).color( 0xFFEE88, true ).show( s.h, 1f );
			PulseRingFX.around( s.h, 0x8AC0FF, 14, 0.5f );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 8 );
			s.later( 0.65f, () -> {
				new Flare( 5, 18 ).color( 0x66CCFF, true ).show( s.h, 1f );
				s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 7 );
			} );
			s.later( 0.75f, () -> new Flare( 8, 30 ).color( 0x99DDFF, true ).show( s.h, 0.8f ) );
			s.later( 1.3f, () -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 6 ) );
			s.later( 1.95f, () -> {
				s.d[0].emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 5 );
				s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 );
			} );
		} ).heroClass( HeroClass.MAGE ).rat( 6, 4 ).seconds( 2.5f );
		r.entry( "guardian-spirit", s -> guardian( s, ShaftParticle.FACTORY ) ).heroClass( HeroClass.CLERIC ).view( 1, 0, 9, 8 ).seconds( 2f );
		r.entry( "dawn-mend", s -> SkillSpectacleFX.show( SkillSpectacleFX.WINGS, s.hero.pos ) )
				.heroClass( HeroClass.CLERIC ).rat( 4, 4 ).rat( 2, 3 ).seconds( 1.5f );
		r.entry( "frost-nova", SkillPreviewsRfxMeaningBefore::nova ).heroClass( HeroClass.MAGE )
				.rat( 5, 4 ).rat( 4, 2 ).rat( 2, 6 ).view( 0, 0, 8, 8 ).seconds( 2f );
		r.entry( "stance-accents", s -> auras( s, RogueHuntressAuras.class ) )
				.heroClass( HeroClass.HUNTRESS ).hero( 5, 7 ).view( 0, 0, 11, 9 ).seconds( 2.5f );
	}

	/** Guardian Spirit's summoning at (6, 4): the seraph fading in over 0.8 s, `rising` started on it
	 *  every 0.12 s five times, the pillar of pale light and the two glints on its cell. */
	static void guardian( SkillPreviewDump.Scene s, Emitter.Factory rising ){
		int cell = at( 6, 4 );
		CharSprite g = new SeraphGuardianSprite();
		s.rig.stage.add( g );
		g.place( cell );
		g.visible = true;
		g.alpha( 0 );
		s.rig.stage.add( new AlphaTweener( g, 1f, 0.8f ) );
		g.emitter().start( rising, 0.12f, 5 );
		PillarRiseFX.show( cell, 0xDAE9FF, null );
		SkillInteractions.flare( cell, 0xDAE9FF );
		SkillInteractions.flare( cell, 0xE9BC59 );
	}

	/** Frost Nova's cast as f0783 draws it (radius 3 round the hero): the snow and the star on him,
	 *  then ring by ring 0.09 s apart snow and a glint on every cell, the caught flashing; returns the
	 *  caught's cells. */
	static int[] nova( SkillPreviewDump.Scene s ){
		s.center( s.hero.pos ).burst( SnowParticle.FACTORY, 12 );
		new Flare( 6, 32 ).color( 0x88DDFF, true ).show( s.h, 0.8f );
		final int[] caught = new int[s.d.length];
		for (int i = 0; i < s.d.length; i++) caught[i] = s.d[i].ch.pos;
		for (int r = 1; r <= 3; r++){
			final int radius = r;
			s.later( 0.09f * r, () -> {
				for (int y = 0; y < SkillPreviewDump.ROOM_H; y++) for (int x = 0; x < SkillPreviewDump.ROOM_W; x++){
					int c = at( x, y );
					if (Dungeon.level.distance( s.hero.pos, c ) != radius) continue;
					boolean hit = false;
					for (CharSprite d : s.d) if (d.ch.pos == c){
						hit = true;
						d.flash();
					}
					s.get( c ).burst( SnowParticle.FACTORY, hit ? 6 : 2 );
					SkillInteractions.flare( c, 0xA4E9FF );
				}
			} );
		}
		return caught;
	}

	/** Every stance aura of `holder` (RogueHuntressAuras) built for real, each on a rat standing in
	 *  for its bearer, in the order of their names. */
	static void auras( SkillPreviewDump.Scene s, Class<?> holder ){
		Class<?>[] kinds = holder.getDeclaredClasses();
		Arrays.sort( kinds, Comparator.comparing( Class::getSimpleName ) );
		ArrayList<int[]> stands = new ArrayList<>( Arrays.asList( new int[][]{ { 2, 2 }, { 5, 2 }, { 8, 2 }, { 2, 5 }, { 5, 5 }, { 8, 5 } } ) );
		for (int i = 0; i < kinds.length && i < stands.size(); i++){
			try {
				CharSprite fig = s.rig.rat( at( stands.get( i )[0], stands.get( i )[1] ) );
				Object buff = kinds[i].getDeclaredConstructor().newInstance();
				Field target = Buff.class.getDeclaredField( "target" );
				target.setAccessible( true );
				target.set( buff, fig.ch );
				Method build = kinds[i].getDeclaredMethod( "build" );
				build.setAccessible( true );
				s.rig.stage.add( (Group) build.invoke( buff ) );
			} catch (ReflectiveOperationException e){
				throw new AssertionError( kinds[i].getName(), e );
			}
		}
	}
}

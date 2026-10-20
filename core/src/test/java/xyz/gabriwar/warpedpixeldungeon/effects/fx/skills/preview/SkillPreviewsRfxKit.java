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

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.RighteousStrikes;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Lightning;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.FxPic;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.LinkFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.MoteFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.RuneFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBolt;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillRay;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ChargedShot;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;

import java.lang.reflect.Method;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The second pass's kit (rfx, plan.md section 7) as the new code shows it: the three restored skills
 * (Righteous Strikes, Retribution, Regeneration's growth and its kill heal), every SkillBolt look and
 * a four-bolt ward volley, the bursts with DAZE, every RuneFX verb, LinkFX's bind, held and drain,
 * SkillRay's looks and stream, MoteFX, the pulses' first colour, the storm and arcane lightning
 * tints, and the palette sheet. Its before side: SkillPreviewsRfxKitBefore (f0783eacd's calls) and,
 * for the entries f0783 already had, the base dump (rfx/previews/_base/json-f0783).
 */
public final class SkillPreviewsRfxKit {

	private SkillPreviewsRfxKit(){}

	static final HeroClass CLERIC = HeroClass.CLERIC;

	public static void register( SkillPreviewDump.Registry r ){
		// ---------------------------------------------------------------- the three restored
		r.entry( "righteous-strikes", SkillPreviewsRfxKit::righteous ).heroClass( CLERIC ).rat( 4, 4 ).rat( 5, 2 ).rat( 7, 3 ).view( 1, 0, 9, 8 );
		r.entry( "retribution", s -> {
			s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 6 );
			s.d[0].flash();
			s.h.parent.add( new Beam.LightRay( s.d[0].center(), s.d[1].center() ) );
			SkillRay.stream( s.d[0], s.d[1], SkillRay.STREAM_HOLY, 0.5f );
		} ).heroClass( CLERIC ).rat( 4, 4 ).rat( 5, 2 );
		r.entry( "regeneration", s -> s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 ) ).heroClass( CLERIC );
		r.entry( "regeneration-kill", s -> {
			new Flare( 5, 16 ).color( 0x66FF66, true ).show( s.h, 0.6f );
			s.h.emitter().burst( Speck.factory( Speck.HEALING ), 3 + 3 );
			MoteFX.greens( s.h, 3 );
		} ).heroClass( CLERIC );

		// ---------------------------------------------------------------- the bolts
		r.entry( "bolt-soul", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.SOUL, null ) ).rat( 7, 4 );
		r.entry( "bolt-mana", s -> SkillBolt.fly( s.h.parent, s.d[0], s.h, SkillBolt.MANA, null ) ).rat( 7, 4 );
		r.entry( "bolt-hex", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.HEX, null ) ).rat( 7, 4 );
		r.entry( "bolt-ember", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.EMBER, null ) ).rat( 7, 4 );
		r.entry( "bolt-holy", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.HOLY, null ) ).rat( 7, 4 );
		r.entry( "bolt-heal", s -> SkillBolt.mote( s.h, s.d[0], SkillBolt.HEAL, null ) ).rat( 7, 4 );
		r.entry( "bolt-spark", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.SPARK, null ) ).rat( 7, 4 );
		r.entry( "bolt-arcane", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.ARCANE, null ) ).rat( 7, 4 );
		r.entry( "bolt-rune", s -> SkillBolt.fly( s.h.parent, s.h, s.d[0], SkillBolt.RUNE, null ) ).rat( 7, 4 );
		r.entry( "bolt-volley-4", s -> {
			for (CharSprite a : s.d) SkillBolt.fly( s.h.parent, s.h.center(), a.destinationCenter(), SkillBolt.RUNE, null );
		} ).heroClass( HeroClass.MAGE ).rat( 7, 2 ).rat( 8, 4 ).rat( 7, 6 ).rat( 5, 7 ).view( 1, 0, 9, 9 );
		r.entry( "bursts-sheet", s -> {
			for (int i = 0; i < SkillBursts.ALL.length; i++){
				if (SkillBursts.ALL[i][1] == SkillBursts.DAZE) s.d[0].emitter().burst( SkillBursts.DAZE, 4 );
				else s.center( SHEET[i] ).burst( (Emitter.Factory) SkillBursts.ALL[i][1], 4 );
			}
		} ).view( 0, 0, 11, 9 ).hero( 5, 4 ).rat( 10, 7 ).seconds( 1.5f );

		// ---------------------------------------------------------------- runes, links, rays, motes
		r.entry( "runes-sheet", SkillPreviewsRfxKit::runes ).heroClass( HeroClass.MAGE ).view( 0, 0, 11, 9 ).hero( 5, 7 )
				.rat( 2, 2 ).rat( 5, 2 ).rat( 8, 2 ).rat( 2, 5 ).rat( 5, 5 ).rat( 8, 5 ).seconds( 2.5f );
		r.entry( "link-shadow", s -> {
			LinkFX.bind( s.h.parent, s.d[0].center(), s.d[1].center(), LinkFX.SHADOW, null );
			s.later( 1.1f, () -> LinkFX.bind( s.h.parent, s.d[2].center(), s.d[3].center(), LinkFX.SHADOW, null ) );
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 3 ).rat( 7, 4 ).rat( 4, 6 ).rat( 5, 6 ).seconds( 2f );
		r.entry( "link-steel", s -> {
			LinkFX.bind( s.h.parent, s.d[0].center(), s.d[1].center(), LinkFX.STEEL, null );
			LinkFX.bind( s.h.parent, s.d[2].center(), s.d[3].center(), LinkFX.STEEL, null );
		} ).rat( 5, 2 ).rat( 6, 2 ).rat( 2, 6 ).rat( 6, 6 ).view( 0, 1, 9, 7 );
		r.entry( "link-held", s -> {
			final Gizmo h = LinkFX.held( s.d[0], s.d[1], LinkFX.SHADOW );
			s.later( 2.3f, () -> xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx.dispose( h ) );
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 3 ).rat( 7, 5 ).seconds( 2.5f );
		r.entry( "link-drain", s -> LinkFX.drain( s.d[0], s.h, LinkFX.SOUL_HEAL, 0.3f ) ).heroClass( HeroClass.MAGE ).rat( 6, 3 );
		r.entry( "ray-holy", s -> {
			SkillRay.show( s.h, s.d[0], SkillRay.HOLY, 0.5f );
			s.later( 0.7f, () -> SkillRay.show( s.h, s.d[1], SkillRay.HOLY, 0.4f ) );
		} ).heroClass( CLERIC ).rat( 7, 3 ).rat( 6, 6 );
		//round 2: the nature and steel rays are gone; Charged Shot's release is its thin stroke again,
		//True Edge's stroke never was a ray (its metal sparks are the links pass's)
		r.entry( "ray-nature", s -> ChargedShot.release( s.h, s.d[0].ch.pos ) ).heroClass( HeroClass.HUNTRESS ).rat( 8, 3 );
		r.entry( "ray-steel", s -> StreakFX.show( s.hero.pos, s.d[0].ch.pos, 0xB9DEEE, 0.45f, 0.25f, StreakFX.SPARKS ) ).heroClass( HeroClass.DUELIST ).rat( 7, 4 );
		r.entry( "ray-heal", s -> SkillRay.show( s.h, s.d[0], SkillRay.HEAL, 0.6f ) ).heroClass( CLERIC ).rat( 6, 4 );
		r.entry( "ray-stream", s -> {
			s.h.parent.add( new Beam.LightRay( s.d[0].center(), s.d[1].center() ) );
			SkillRay.stream( s.d[0], s.d[1], SkillRay.STREAM_HOLY, 0.5f );
		} ).heroClass( CLERIC ).rat( 4, 3 ).rat( 7, 5 );
		r.entry( "motes-sheet", SkillPreviewsRfxKit::motes ).view( 0, 0, 11, 9 ).hero( 5, 7 ).rat( 8, 2 ).seconds( 2.4f );
		r.entry( "pulse-tone", s -> {
			for (int i = 0; i < PULSES.length; i++) PulseRingFX.at( PULSE_AT[i], PULSES[i], 20, 0.6f );
		} ).view( 0, 0, 11, 9 ).hero( 5, 8 ).seconds( 1.25f );
		r.entry( "lightning-tints", s -> {
			s.h.parent.add( new Lightning( at( 1, 2 ), at( 8, 2 ), null ).tint( Lightning.STORM ) );
			s.h.parent.add( new Lightning( at( 1, 6 ), at( 8, 6 ), null ).tint( Lightning.ARCANE ) );
		} ).view( 0, 0, 10, 9 ).hero( 9, 4 );
		r.entry( "palette-sheet", SkillPreviewsRfxKit::palette ).view( 0, 0, 11, 9 ).hero( 10, 8 ).seconds( 1.25f );
	}

	/** The bursts' places on the sheet, a cell apart. */
	static final int[] SHEET = new int[24];
	static {
		for (int i = 0; i < SHEET.length; i++) SHEET[i] = at( 2 * (i % 6), 1 + 2 * (i / 6) );
	}

	/** The pulses' colours (the heal, holy, fire, mana, arcane, steel) and their cells. */
	static final int[] PULSES = { SkillPalette.HEAL_LIGHT, 0xF1D99C, 0xFF8418, SkillPalette.MANA_LIGHT, 0x9070F0, 0xB9DEEE };
	static final int[] PULSE_AT = { at( 2, 2 ), at( 5, 2 ), at( 8, 2 ), at( 2, 5 ), at( 5, 5 ), at( 8, 5 ) };

	// ------------------------------------------------------------------ the moments

	//Righteous Strikes' release at +3 on a third blow, through the skill's own leap
	static void righteous( SkillPreviewDump.Scene s ){
		new Flare( 6, 20 ).color( 0xFFEE88, true ).show( s.d[0], 0.6f );
		s.d[0].emitter().burst( Speck.factory( Speck.YELLOW_LIGHT ), 6 );
		FxTimeline chain = FxTimeline.start();
		for (int i = 0; i + 1 < s.d.length; i++){
			final Char a = s.d[i].ch, b = s.d[i + 1].ch;
			chain.at( 0.1f * (i + 1), () -> leap( a, b, false ) );
		}
		final Char last = s.d[s.d.length - 1].ch;
		chain.at( 0.1f * (s.d.length + 1), () -> leap( last, s.hero, true ) );
	}

	//RighteousStrikes.ray, the skill's own
	static void leap( Char a, Char b, boolean home ){
		try {
			Method m = RighteousStrikes.class.getDeclaredMethod( "ray", Char.class, Char.class, boolean.class );
			m.setAccessible( true );
			m.invoke( null, a, b, home );
		} catch (ReflectiveOperationException e){
			throw new AssertionError( e );
		}
	}

	//every RuneFX verb: rings (one spun up and released), a ward locking on whole and on a side, a
	//ring snapping shut, a ward breaking, a ring rising, stamps, a stream's sheds, two runes of a
	//pact meeting, a lock lifting
	static void runes( SkillPreviewDump.Scene s ){
		RuneFX.ring( s.d[0], 6, SkillPalette.RUNE_ARCANE, 5, 2, 2.4f, 2.2f );
		final RuneFX.Runes spun = RuneFX.ring( s.d[1], 6, SkillPalette.RUNE_ARCANE, 6, 2.5f, 2.4f, 2f ).spin( 8f, 0.3f );
		s.later( 0.9f, () -> spun.release( 40f, 0.25f ) );
		RuneFX.lock( s.d[2], 6, SkillPalette.RUNE_MANA, 8, 4, 0.08f, 0.3f );
		RuneFX.lock( s.d[3], 3, SkillPalette.RUNE_MANA, 8, 4, 0.08f, 0.3f, 0f, 1.2f );
		s.later( 0.1f, () -> RuneFX.shut( s.d[4], 5, SkillPalette.RUNE_VOID, 9, 4.5f, 0.2f ) );
		s.later( 0.2f, () -> RuneFX.scatter( s.d[5], 6, SkillPalette.RUNE_MANA, 10, 0.35f ) );
		s.later( 0.3f, () -> RuneFX.rise( s.h, 6, SkillPalette.RUNE_HOLY, 9, 3.5f, 18, 0.6f ) );
		s.later( 0.4f, () -> RuneFX.stamp( SkillVfx.cx( at( 2, 7 ) ), SkillVfx.cy( at( 2, 7 ) ), FxFrames.RUNE_5[2], 0xF1D99C, 0.4f ) );
		s.later( 0.4f, () -> RuneFX.stamp( SkillVfx.cx( at( 8, 7 ) ), SkillVfx.cy( at( 8, 7 ) ), FxFrames.RUNE_3[0], 0xD01818, 0.4f ) );
		for (int k = 0; k < 12; k++){
			final int i = k;
			s.later( 0.1f * i, () -> RuneFX.shed( SkillVfx.cx( at( 9, 7 ) ) - 6 + i, SkillVfx.cy( at( 9, 7 ) ), SkillPalette.RUNE_ARCANE ) );
		}
		//a pact sealed: one rune of blood and one of mana turning opposite ways, meeting at his front
		s.later( 1.0f, () -> {
			RuneFX.ring( s.h, 1, SkillPalette.RUNE_BLOOD, 6, 2.5f, (float) Math.PI, 0.55f, 0f );
			RuneFX.ring( s.h, 1, SkillPalette.RUNE_MANA, 6, 2.5f, (float) -Math.PI, 0.55f, (float) Math.PI );
		} );
		//an echo: a lock on the struck lifting off it
		s.later( 1.2f, () -> {
			final RuneFX.Runes echo = RuneFX.lock( s.d[2], 5, SkillPalette.RUNE_ARCANE, 8, 4, 0.04f, 0.6f );
			s.later( 0.2f, () -> echo.lift( -12f, 0.35f ) );
		} );
	}

	//the motes: the heal's crosses and greens, a gather of mana, a peel off a body, sparks, shards and
	//drops flicked (the drops dying at the feet), a glint running
	static void motes( SkillPreviewDump.Scene s ){
		for (int k = 0; k < 2; k++){
			s.later( 1.2f * k, () -> {
				float cx = SkillVfx.cx( at( 2, 2 ) ), cy = SkillVfx.cy( at( 2, 2 ) );
				MoteFX.crosses( cx, cy, 3 );
				MoteFX.greens( SkillVfx.cx( at( 4, 2 ) ), SkillVfx.cy( at( 4, 2 ) ), 5 );
				MoteFX.gather( SkillVfx.cx( at( 6, 2 ) ), SkillVfx.cy( at( 6, 2 ) ), 8, SkillPalette.MANA, 18, 0.4f );
				MoteFX.peel( s.d[0], SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 6, SkillPalette.SOUL, 0.5f );
				MoteFX.flick( SkillVfx.cx( at( 2, 5 ) ), SkillVfx.cy( at( 2, 5 ) ), 6, FxFrames.SPARK_2, SkillPalette.STEEL, 30f, 50f, 4f, 0f,
						0.2f, 10f, FxBudget.P2 );
				MoteFX.flick( SkillVfx.cx( at( 4, 5 ) ), SkillVfx.cy( at( 4, 5 ) ), 3, FxFrames.SHARD_GLASS, 0x66CCFF, 20f, 34f, 3f, 0f,
						0.35f, 10f, FxBudget.P2 );
				MoteFX.flick( SkillVfx.cx( at( 6, 5 ) ), SkillVfx.cy( at( 6, 5 ) ) - 4, 3, FxFrames.DROP_2, SkillPalette.POISON.stop( 2 ),
						(float) -Math.PI / 2, 1.4f, 10f, 20f, 0f, 160f, 0.8f, 10f, FxBudget.P2 );
				MoteFX.glint( SkillVfx.cx( at( 8, 5 ) ) - 6, SkillVfx.cy( at( 8, 5 ) ), FxFrames.STAR4_5, 0xE8F0FF, 0.3f, 40f, 0f );
			} );
		}
	}

	//every SkillPalette ramp as stepped squares (each stop), a cross and a rune of its body
	static void palette( SkillPreviewDump.Scene s ){
		for (int i = 0; i < SkillPalette.RAMPS.length; i++){
			Ramp r = (Ramp) SkillPalette.RAMPS[i][1];
			float x = 16 * (1 + i % 9), y = 16 * (1 + 2 * (i / 9));
			for (int k = 0; k < r.size(); k++) pic( FxFrames.SQ_4, r.stop( k ), x + 4 * k, y );
			//a heal's cross is red (Speck.HEALING's), never green
			pic( FxFrames.PLUS_5, r == SkillPalette.HEAL ? SkillPalette.HEAL_CROSS : SkillPalette.mid( r ), x, y + 7 );
			pic( FxFrames.RUNE_5[i % 4], SkillPalette.mid( r ), x + 7, y + 7 );
		}
		//DAZE: one colour
		pic( FxFrames.STAR4_5, SkillPalette.DAZE, 16 * 1, 16 * 7 );
		pic( FxFrames.PLUS_3, SkillPalette.DAZE, 16 * 1 + 7, 16 * 7 + 1 );
	}

	//a still light quad of the sheet at (x, y) (its top-left), in the light layer
	static void pic( int[] frame, int color, float x, float y ){
		FxPic p = new FxPic( SkillVfx.SHEET );
		p.additive = true;
		p.show( frame ).lit( color );
		p.x = x;
		p.y = y;
		GameScene.light( p );
	}
}

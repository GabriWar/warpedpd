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
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.MagicMissile;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BlastParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.FlameParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.ShadowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SmokeParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SnowParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.items.Gold;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.Bomb;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.MirrorSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.SkeletonSprite;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PointF;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The dread-shadow package's moments as the game drew them before it (master 72b40617b): each
 * skill's visual calls of that code, restaged on the dump's stage (SkillPreviewDump), CellEmitter's
 * bursts through the scene's stand-in emitters, at rank 3 where the rank shows. Compiles against
 * that code and this one, so its "before" side can be dumped from a checkout of the old code
 * (tools/fxpreview/README.md). What a skill does not draw itself (a SmokeScreen's blob, a field, a
 * floating number, a damage card) is left out on both sides.
 */
public final class SkillPreviewsDreadBefore {

	private SkillPreviewsDreadBefore(){}

	public static void register( SkillPreviewDump.Registry r ){
		//Demoralize: the alarm's brackets and a purple fan on every blow; the red fan of the Terror
		r.entry( "demoralize-hit", s -> demoralizeHit( s.d[0] ) ).rat( 4, 4 ).heroClass( HeroClass.WARRIOR );
		r.entry( "demoralize-terror", s -> {
			demoralizeHit( s.d[0] );
			s.d[0].emitter().burst( Speck.factory( Speck.SCREAM ), 1 );
			new Flare( 10, 24 ).color( 0xCC3355, true ).show( s.d[0], .8f );
		} ).rat( 4, 4 );
		//Lone Wolf: the alarm's brackets on her; a level-up's stars when she is alone again
		r.entry( "lone-wolf-howl", s -> s.h.emitter().burst( Speck.factory( Speck.SCREAM ), 3 ) )
				.heroClass( HeroClass.HUNTRESS ).rat( 4, 4 ).rat( 5, 3 ).rat( 2, 5 );
		r.entry( "lone-wolf-alone", s -> s.h.emitter().burst( Speck.factory( Speck.STAR ), 4 ) ).heroClass( HeroClass.HUNTRESS );
		//Dread Howl: a curse's shadow, shadow bolts' sparks on the rings, the brackets and a wraith's rise
		r.entry( "dread-howl", SkillPreviewsDreadBefore::dreadHowl ).heroClass( HeroClass.ROGUE ).hero( 5, 4 ).view( 0, 0, 11, 9 )
				.rat( 7, 4 ).rat( 5, 2 ).rat( 3, 6 ).rat( 8, 6 );
		//Panic Harvest: a shadow bolt into the rogue; brackets, a curse and a purple fan as fear spreads
		r.entry( "panic-harvest-reap", s -> {
			s.get( s.d[0].ch.pos ).burst( ShadowParticle.UP, 5 );
			((MagicMissile) s.h.parent.recycle( MagicMissile.class )).reset( MagicMissile.SHADOW, s.d[0].center(), s.h.center(),
					() -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 4 ) );
		} ).heroClass( HeroClass.ROGUE ).rat( 6, 4 );
		r.entry( "panic-harvest-spread", s -> {
			for (int i = 1; i < s.d.length; i++) s.get( s.d[i].ch.pos ).burst( ShadowParticle.CURSE, 4 );
			s.get( s.d[0].ch.pos ).burst( Speck.factory( Speck.SCREAM ), 3 );
			new Flare( 5, 24 ).color( 0x663399, true ).show( s.d[0], 0.6f );
		} ).heroClass( HeroClass.ROGUE ).rat( 5, 4 ).rat( 6, 4 ).rat( 5, 3 ).rat( 6, 5 );
		//Hex: a wand's shadow bolt, a curse's shadow
		r.entry( "hex", s -> {
			final CharSprite ch = s.d[0];
			MagicMissile.boltFromChar( s.h.parent, MagicMissile.SHADOW, s.h, ch.ch.pos, () -> {
				ch.emitter().burst( ShadowParticle.CURSE, 6 );
				ch.flash();
			} );
		} ).heroClass( HeroClass.MAGE ).rat( 7, 4 );
		//Silent Death: a grim trap's column off the sleeper and the rogue; grey smoke on the hushed
		r.entry( "silent-death", s -> {
			Wound.hit( s.d[0].ch );
			s.get( s.d[0].ch.pos ).burst( ShadowParticle.UP, 8 );
			s.h.emitter().burst( ShadowParticle.UP, 6 );
			s.get( s.d[1].ch.pos ).burst( Speck.factory( Speck.SMOKE ), 3 );
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 4 ).rat( 6, 3 );
		//Vanish: a curse rising off him, a question mark burst over each hunter
		r.entry( "vanish", s -> {
			s.h.emitter().burst( ShadowParticle.UP, 10 );
			for (CharSprite m : s.d) m.emitter().burst( Speck.factory( Speck.QUESTION ), 1 );
			s.h.alpha( 0.4f );
		} ).heroClass( HeroClass.ROGUE ).rat( 6, 4 ).rat( 5, 2 );
		//Stealth: a Mirror Image beside him in grey smoke, the shade's spectacle, a curse rising off him
		r.entry( "stealth-decoy", s -> {
			int cell = at( 4, 3 );
			CharSprite decoy = sprite( s, new MirrorSprite(), cell );
			decoy.alpha( 0.55f );
			s.get( cell ).burst( Speck.factory( Speck.SMOKE ), 8 );
			SkillSpectacleFX.show( SkillSpectacleFX.SHADOW, cell );
			s.h.emitter().burst( ShadowParticle.UP, 6 );
		} ).heroClass( HeroClass.ROGUE ).rat( 7, 4 ).seconds( 2.5f );
		//Dark Pact: a curse on the adjacent, grey smoke, red and blue lights, the flash
		r.entry( "dark-pact", s -> {
			for (CharSprite m : s.d) m.emitter().burst( ShadowParticle.CURSE, 6 );
			s.h.emitter().burst( Speck.factory( Speck.SMOKE ), 6 );
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 4 );
			s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 7 );
			s.h.flash();
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 4 ).rat( 3, 3 );
		//Ambush: a grim trap's column where the stake should be, dust
		r.entry( "ambush-stake", s -> {
			Wound.hit( s.d[0].ch );
			s.get( s.d[0].ch.pos ).burst( ShadowParticle.UP, 8 );
			s.bottom( s.d[0].ch.pos ).burst( Speck.factory( Speck.DUST ), 4 );
		} ).heroClass( HeroClass.ROGUE ).rat( 5, 4 );
		//Shadow Link: the ethereal chain (the artifact's) and a curse at each end
		r.entry( "shadow-link", s -> {
			final CharSprite a = s.d[0], b = s.d[1];
			s.center( a.ch.pos ).burst( ShadowParticle.CURSE, 4 );
			a.parent.add( new Chains( a.center(), b.destinationCenter(), Effects.Type.ETHEREAL_CHAIN, () -> {
				b.flash();
				b.emitter().burst( ShadowParticle.CURSE, 4 );
			} ) );
		} ).heroClass( HeroClass.ROGUE ).rat( 5, 4 ).rat( 7, 2 );
		//Soul Drain: a shadow bolt to the next and a curse on it
		r.entry( "soul-drain", s -> {
			((MagicMissile) s.h.parent.recycle( MagicMissile.class )).reset( MagicMissile.SHADOW, s.d[0].ch.pos, s.d[1].ch.pos, null );
			s.d[1].emitter().burst( ShadowParticle.CURSE, 6 );
		} ).heroClass( HeroClass.MAGE ).rat( 4, 5 ).rat( 7, 3 );
		//Soul Detonation: a healing beam from the pet, a wraith's rise, a violet fan, the mana's lights
		r.entry( "soul-detonation", s -> {
			CharSprite pet = s.d[0];
			int origin = pet.ch.pos;
			s.h.parent.add( new Beam.HealthRay( pet.center(), s.h.center() ) );
			s.bottom( origin ).burst( ShadowParticle.UP, 12 );
			pet.die();
			new Flare( 6, 16 ).color( 0xBBA7EE, true ).show( s.h.parent, DungeonTilemap.tileCenterToWorld( origin ), 0.5f );
			s.later( 0.3f, () -> s.h.emitter().burst( Speck.factory( Speck.BLUE_LIGHT ), 3 ) );
		} ).heroClass( HeroClass.MAGE ).rat( 6, 4 );
		//Summon Skeleton: a wraith's rise out of the floor, then bones bursting (a skeleton breaking)
		r.entry( "summon-skeleton", s -> {
			int grave = at( 4, 4 );
			CharSprite body = sprite( s, new SkeletonSprite(), grave );
			body.alpha( 0 );
			s.h.parent.add( new AlphaTweener( body, 1, 0.6f ) );
			s.bottom( grave ).start( ShadowParticle.UP, 0.06f, 10 );
			StaggerFX.after( 0.35f, () -> s.get( grave ).burst( Speck.factory( Speck.BONE ), 6 ) );
		} ).heroClass( HeroClass.MAGE );
		//Blackout: grey smoke, a dark fan, shadow bolts' sparks on the rings, smoke on the blinded
		r.entry( "blackout", s -> {
			int cell = at( 6, 4 );
			s.get( cell ).burst( Speck.factory( Speck.SMOKE ), 6 );
			new Flare( 8, 36 ).color( 0x1A0A2A, false ).show( s.h.parent, DungeonTilemap.tileCenterToWorld( cell ), 0.6f );
			FxTimeline t = FxTimeline.start();
			for (int r0 = 0; r0 <= 2; r0++){
				final int ring = r0;
				t.at( 0.09f * r0, () -> {
					for (int c : ring( cell, ring )) s.get( c ).burst( ShadowParticle.MISSILE, 3 );
				} );
			}
			for (CharSprite m : s.d) s.get( m.ch.pos ).burst( Speck.factory( Speck.SMOKE ), 4 );
		} ).heroClass( HeroClass.ROGUE ).hero( 2, 4 ).rat( 6, 3 ).rat( 7, 5 );
		//Shadow Company: the shade's spectacle to each place, a wraith's rise as each figure fades in
		r.entry( "phantom-strike", s -> company( s, true ) ).heroClass( HeroClass.ROGUE ).seconds( 1.6f );
		//Ninja Bomb: a bomb thrown (no bomb goes off), the real smoke where it lands
		r.entry( "ninja-bomb", s -> {
			final int cell = at( 7, 4 );
			SkillFX.streak( s.h, cell, new Bomb(), () -> ninjaSmoke( s, cell, Speck.factory( Speck.SMOKE ) ) );
		} ).heroClass( HeroClass.ROGUE ).seconds( 1.6f );
		//Master Thief: a pile of gold thrown, coins bursting off the struck (loot)
		r.entry( "master-thief-coin", s -> {
			final CharSprite ch = s.d[0];
			SkillFX.streak( s.h, ch.ch.pos, new Gold(), () -> ch.emitter().burst( Speck.factory( Speck.COIN ), 4 ) );
		} ).heroClass( HeroClass.ROGUE ).rat( 6, 4 );
		//Necrotoxin: real poison's splash, a toxic gas puff, a green fan
		r.entry( "necrotoxin", s -> {
			CharSprite e = s.d[0];
			s.center( e.ch.pos ).burst( PoisonParticle.SPLASH, 10 );
			s.get( e.ch.pos ).burst( Speck.factory( Speck.TOXIC ), 6 );
			new Flare( 6, 20 ).color( 0x66DD44, true ).show( e, 0.6f );
			necroSpray( s );
		} ).heroClass( HeroClass.ROGUE ).rat( 4, 4 ).rat( 5, 3 ).rat( 5, 5 );
		//Venom: a poison missile's particles on the rogue (as if he were poisoned)
		r.entry( "venom", s -> s.h.emitter().burst( PoisonParticle.MISSILE, 4 ) ).heroClass( HeroClass.ROGUE );
		//the stance accents: a curse rising, light, stars, smoke, snow, flame on the hero
		r.entry( "stance-accents", s -> accents( s, new Emitter.Factory[]{ ShadowParticle.UP, Speck.factory( Speck.LIGHT ),
				Speck.factory( Speck.STAR ), SmokeParticle.FACTORY, SnowParticle.FACTORY, FlameParticle.FACTORY } ) )
				.heroClass( HeroClass.HUNTRESS ).hero( 5, 7 ).view( 0, 0, 11, 9 ).seconds( 2.5f );
		//Aimed Shot: a rockfall's rocks where the arrow pins it
		r.entry( "aimed-pin", s -> {
			CharSprite e = s.d[0];
			s.get( e.ch.pos ).burst( Speck.factory( Speck.ROCK ), 4 );
			s.bottom( e.ch.pos ).startDelayed( Speck.factory( Speck.DUST ), 0, 4, 0.15f );
			e.emitter().burst( Speck.factory( Speck.LIGHT ), 2 );
		} ).heroClass( HeroClass.HUNTRESS ).rat( 7, 4 );
		//Double Shot: a level-up's stars on her, a red fan and stars on the mark
		r.entry( "double-shot", s -> {
			s.h.emitter().burst( Speck.factory( Speck.STAR ), 6 );
			new Flare( 6, 14 ).color( 0xFF5555, true ).show( s.d[0], 0.8f );
			s.d[0].emitter().burst( Speck.factory( Speck.STAR ), 3 );
		} ).heroClass( HeroClass.HUNTRESS ).rat( 6, 4 );
		//Bombvoyage: grey smoke on her as the stance goes up and as each charge leaves, the blast's fan
		r.entry( "bombvoyage-fuse", s -> {
			s.h.emitter().burst( Speck.factory( Speck.SMOKE ), 5 );
			s.later( 0.3f, () -> s.h.emitter().burst( SmokeParticle.FACTORY, 3 ) );
			s.later( 0.6f, () -> {
				int c0 = s.d[0].ch.pos;
				s.center( c0 ).burst( BlastParticle.FACTORY, 30 );
				new Flare( 6, 20 ).color( 0xFFAA44, true ).show( s.h.parent, DungeonTilemap.tileCenterToWorld( c0 ), 0.4f );
				blastSmoke( s, c0 );
			} );
		} ).heroClass( HeroClass.HUNTRESS ).rat( 7, 4 ).seconds( 1.6f );
	}

	// ------------------------------------------------------------------ the moments then

	//Demoralize.onHitProc's every blow at rank 3
	static void demoralizeHit( CharSprite e ){
		e.emitter().burst( Speck.factory( Speck.SCREAM ), 1 );
		new Flare( 4 + 3, 10 + 3 * 3 ).color( 0x664488, true ).show( e, .45f );
	}

	//DreadHowl.execute's pictures: the rats 0 and 1 terrified already (maddened), 2 and 3 not (mastery)
	static void dreadHowl( SkillPreviewDump.Scene s ){
		int hero = s.hero.pos;
		for (int i = 2; i < 4; i++) s.get( s.d[i].ch.pos ).burst( ShadowParticle.CURSE, 4 );
		FxTimeline t = FxTimeline.start();
		int w = SkillPreviewDump.W;
		for (int r = 1; r <= 5; r++){
			final int ring = r;
			t.at( 0.06f * r, () -> {
				for (int c : ring( hero, ring )){
					if (((c % w) + (c / w) + ring) % 2 == 0) s.get( c ).burst( ShadowParticle.MISSILE, 1 );
				}
			} );
		}
		for (int i = 0; i < 2; i++){
			final CharSprite mad = s.d[i];
			t.at( 0.06f * Dungeon.level.distance( hero, mad.ch.pos ), () -> {
				s.get( mad.ch.pos ).burst( Speck.factory( Speck.SCREAM ), 2 );
				s.get( mad.ch.pos ).burst( ShadowParticle.UP, 3 );
				mad.flash();
			} );
		}
		s.h.emitter().burst( ShadowParticle.UP, 8 );
	}

	//Shadow Company: three of them out of the dark one at a time round (6, 4)
	static void company( SkillPreviewDump.Scene s, boolean before ){
		int origin = s.hero.pos;
		int[] dests = { at( 6, 3 ), at( 7, 4 ), at( 6, 5 ) };
		FxTimeline rise = FxTimeline.start();
		for (int k = 0; k < dests.length; k++){
			final int order = k, dest = dests[k];
			//a figure of the hero stands in for RogueShadowSprite (the painter has no picture of its sheet)
			final CharSprite shade = sprite( s, new MirrorSprite(), dest );
			final float shown = 0.5f;
			shade.alpha( 0 );
			rise.at( 0.15f * order, () -> SkillSpectacleFX.fly( SkillSpectacleFX.SHADOW, origin, dest, 0, .3f ) );
			rise.at( 0.15f * order + 0.3f, () -> {
				s.h.parent.add( new AlphaTweener( shade, shown, 0.35f ) );
				if (before) s.get( dest ).burst( ShadowParticle.UP, 6 );
			} );
		}
		if (before) s.h.emitter().burst( ShadowParticle.UP, 6 );
	}

	//Ninja Bomb's landing: the real smoke, and its rings rolling out at rank 3 (the ring's burst as given)
	static void ninjaSmoke( SkillPreviewDump.Scene s, int cell, Emitter.Factory ringSmoke ){
		s.get( cell ).burst( SmokeParticle.FACTORY, 8 );
		s.get( cell ).start( SmokeParticle.FACTORY, 0.06f, 20 );
		FxTimeline t = FxTimeline.start();
		for (int r = 1; r <= 3; r++){
			final int ring = r;
			t.at( 0.09f * r, () -> {
				for (int c : ring( cell, ring )) s.get( c ).burst( ringSmoke, 3 );
			} );
		}
	}

	//Necrotoxin at mastery: the rupture sprays onto its neighbours (rats 1 and 2)
	static void necroSpray( SkillPreviewDump.Scene s ){
		FxTimeline t = FxTimeline.start();
		for (int i = 1; i < s.d.length; i++){
			final int at = s.d[i].ch.pos;
			t.at( 0.08f + 0.05f * (i - 1), () -> s.center( at ).burst( PoisonParticle.SPLASH, 4 ) );
		}
	}

	//Bombvoyage's blast: the smoke rolling out over the nine cells
	static void blastSmoke( SkillPreviewDump.Scene s, int c0 ){
		int ring = 0;
		for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++){
			int c = c0 + dx + dy * SkillPreviewDump.W;
			boolean mid = dx == 0 && dy == 0;
			s.get( c ).startDelayed( SmokeParticle.FACTORY, 0, 3, mid ? 0f : 0.06f + 0.03f * ring++ );
		}
	}

	//six stances, each on a figure of the hero of its own, poured at its own rate
	static final float[] ACCENT_EVERY = { 0.7f, 0.8f, 0.9f, 0.4f, 0.5f, 0.7f };
	static final int[][] STANDS = { { 2, 2 }, { 5, 2 }, { 8, 2 }, { 2, 5 }, { 5, 5 }, { 8, 5 } };

	static void accents( SkillPreviewDump.Scene s, Emitter.Factory[] f ){
		for (int i = 0; i < f.length; i++){
			CharSprite fig = sprite( s, new MirrorSprite(), at( STANDS[i][0], STANDS[i][1] ) );
			Emitter e = new Emitter();
			e.pos( fig );
			e.autoKill = false;
			s.rig.stage.add( e );
			e.pour( f[i], ACCENT_EVERY[i] );
		}
	}

	// ------------------------------------------------------------------ the stage

	/** A sprite standing on the stage at a cell (no character). */
	static CharSprite sprite( SkillPreviewDump.Scene s, CharSprite sp, int cell ){
		s.rig.stage.add( sp );
		sp.place( cell );
		sp.visible = true;
		return sp;
	}

	/** The cells exactly `r` from `c` on the stage's room (r 0: c itself). */
	static int[] ring( int c, int r ){
		int w = SkillPreviewDump.W;
		int cx = c % w, cy = c / w, n = 0;
		int[] out = new int[Math.max( 1, 8 * r )];
		for (int y = cy - r; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++){
			if (Math.max( Math.abs( x - cx ), Math.abs( y - cy ) ) != r) continue;
			if (x < 0 || y < 0 || x >= SkillPreviewDump.ROOM_W || y >= SkillPreviewDump.ROOM_H) continue;
			out[n++] = x + y * w;
		}
		return java.util.Arrays.copyOf( out, n );
	}

	/** A cell's middle (tileCenterToWorld). */
	static PointF mid( int cell ){
		return DungeonTilemap.tileCenterToWorld( cell );
	}
}

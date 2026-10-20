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

import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SkillInteractions;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.SummonedPet;
import xyz.gabriwar.warpedpixeldungeon.effects.Beam;
import xyz.gabriwar.warpedpixeldungeon.effects.Chains;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillFX;
import xyz.gabriwar.warpedpixeldungeon.effects.SkillSpectacleFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Splash;
import xyz.gabriwar.warpedpixeldungeon.effects.WarriorImpactFX;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.BloodParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.EarthParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.FxTimeline;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.items.stones.StoneOfBlast;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuardSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSpriteSheet;
import xyz.gabriwar.warpedpixeldungeon.tiles.DungeonTilemap;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.tweeners.AlphaTweener;
import com.watabou.utils.PointF;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The martial-steel package's moments as the game drew them before it (master 72b40617b): each
 * skill's visual calls of that code, restaged on the dump's stage (CellEmitter's bursts through the
 * scene's stand-in emitters, SkillFX.land as its burst and its landing). Compiles against that code
 * and this, so its side can be dumped from a checkout of master (with MartialPreviewStage beside it).
 */
public final class SkillPreviewsMartialBefore {

	private SkillPreviewsMartialBefore(){}

	/** The stages both sides share. */
	static final int[][] ROAR_RATS = { { 4, 4 }, { 5, 2 }, { 6, 6 } };

	public static void register( SkillPreviewDump.Registry r ){
		//Undying Will: the cast's red fan, the save's crimson fan, the ward's roar as an alarm's brackets
		r.entry( "undying-cast", s -> {
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 5 );
			new Flare( 6, 22 ).color( 0xFF5544, true ).show( s.h, 0.8f );
		} );
		r.entry( "undying-saved", s -> {
			s.h.flash();
			new Flare( 8, 26 ).color( 0xFF2222, true ).show( s.h, 1f );
		} ).rat( 4, 4 );
		rats( r.entry( "undying-roar", s -> {
			for (CharSprite m : s.d) m.emitter().burst( Speck.factory( Speck.SCREAM ), 2 );
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 10 );
			s.h.emitter().burst( Speck.factory( Speck.SCREAM ), 4 );
		} ), ROAR_RATS );
		//Stone Skin: rocks
		r.entry( "stone-skin-break", s -> {
			s.h.emitter().burst( Speck.factory( Speck.ROCK ), 6 );
			s.center( s.d[0].ch.pos ).burst( Speck.factory( Speck.ROCK ), 5 );
		} ).rat( 4, 4 );
		r.entry( "stone-skin-regrow", s -> s.h.emitter().burst( Speck.factory( Speck.ROCK ), 3 ) );
		//Endurance: the stomp's ring of rocks, forge sparks on him
		r.entry( "endurance-stomp", s -> {
			land( s, s.hero.pos );
			for (int c : ring( s.hero.pos )){
				s.later( 0.1f, () -> {
					s.get( c ).burst( Speck.factory( Speck.ROCK ), 2 );
					s.bottom( c ).burst( EarthParticle.FACTORY, 1 );
				} );
			}
			s.later( 0.1f, () -> { for (CharSprite m : s.d) m.flash(); } );
			s.h.emitter().burst( Speck.factory( Speck.FORGE ), 6 );
		} ).rat( 4, 4 ).rat( 2, 3 );
		//Groundwork: rocks and thorns coming up under the neighbours
		r.entry( "groundwork", s -> {
			s.get( s.d[0].ch.pos ).burst( Speck.factory( Speck.ROCK ), 4 );
			for (int k = 1; k < s.d.length; k++){
				final int at = s.d[k].ch.pos;
				s.later( 0.05f * (k - 1), () -> {
					s.get( at ).burst( Speck.factory( Speck.ROCK ), 3 );
					SkillSpectacleFX.show( SkillSpectacleFX.THORN, at );
				} );
			}
		} ).rat( 4, 4 ).rat( 5, 3 ).rat( 5, 5 );
		//Knock Back: the +3 crush; the stance's forge sparks
		r.entry( "knockback-crush", s -> s.get( s.d[0].ch.pos ).burst( Speck.factory( Speck.ROCK ), 4 ) ).rat( 4, 4 );
		r.entry( "knockback-stance", s -> stance( s, Speck.factory( Speck.FORGE ), Speck.factory( Speck.FORGE ) ) ).seconds( 2.5f );
		//Firm Hand's stagger: stars circling its head (beside it, a real stun's stars)
		r.entry( "stagger-mark", s -> {
			MartialPreviewStage.mark( s, s.d[0].ch, SkillInteractions.Mark.STAGGER, 2 );
			s.d[0].emitter().burst( Speck.factory( Speck.FORGE ), 5 );
			MartialPreviewStage.stunStars( s.d[1] );
		} ).rat( 5, 4 ).rat( 7, 4 ).seconds( 2.5f );
		//Shieldbearer: he lands (a fan of gold), rocks on the enemies about him, the bash ringing out
		r.entry( "shieldbearer-bash", s -> {
			SummonedPet g = MartialPreviewStage.pet( s, GuardSprite.class, at( 5, 4 ) );
			new Flare( 6, 18 ).color( 0xFFDD88, true ).show( g.sprite, 0.7f );
			for (CharSprite m : s.d) s.get( m.ch.pos ).burst( Speck.factory( Speck.ROCK ), 3 );
			bash( s, at( 5, 4 ) );
		} ).rat( 6, 3 ).rat( 6, 5 ).seconds( 1.5f );
		//summoned allies, beside a hostile rat: nothing tells them apart
		r.entry( "summons-marked", SkillPreviewsMartialBefore::summons ).rat( 7, 4 ).seconds( 2f );
		//Summon Crab: the crab simply fades in
		r.entry( "summon-crab-arrival", s -> crab( s ) );
		//Toughness: forge sparks, then the held pain landing as blood
		r.entry( "toughness-held", s -> {
			s.h.emitter().burst( Speck.factory( Speck.FORGE ), 6 );
			s.later( 0.5f, () -> s.h.emitter().burst( BloodParticle.FACTORY, 3 ) );
			s.later( 1.0f, () -> s.h.emitter().burst( BloodParticle.FACTORY, 3 ) );
		} ).seconds( 1.5f );
		//Warbreaker: stun stars on a guard broken
		r.entry( "warbreaker", s -> {
			Wound.hit( s.d[0].ch );
			s.d[0].emitter().burst( Speck.factory( Speck.STAR ), 5 );
			s.d[1].emitter().burst( Speck.factory( Speck.STAR ), 3 );
		} ).rat( 4, 4 ).rat( 5, 3 );
		//Challenge: a level-up's stars on him, a fan, red lights on the challenged
		r.entry( "challenge", s -> {
			for (CharSprite m : s.d) m.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 3 );
			new Flare( 6, 22 ).color( 0xFF6644, true ).show( s.h, 0.5f );
			s.h.emitter().burst( Speck.factory( Speck.STAR ), 6 );
		} ).rat( 5, 2 ).rat( 6, 5 );
		//Opening Blow: an orange fan on the dazed enemy, its landing
		r.entry( "combo-opener", s -> {
			new Flare( 8, 20 ).color( 0xFFB060, true ).show( s.d[0], 0.5f );
			land( s, s.d[0].ch.pos );
		} ).rat( 4, 4 );
		//Aplomb: forge sparks and a grey fan on him, the attacker dazed
		r.entry( "aplomb", s -> {
			s.h.emitter().burst( Speck.factory( Speck.FORGE ), 4 );
			new Flare( 4, 12 ).color( 0xD8D8D8, true ).show( s.h, 0.3f );
			s.d[0].emitter().burst( Speck.factory( Speck.STAR ), 4 );
		} ).rat( 4, 4 );
		//Blade Bind: the Ethereal Chains, electric sparks, stun stars
		r.entry( "blade-bind", s -> {
			CharSprite ch = s.d[0];
			PointF a = s.h.center(), b = ch.center();
			s.h.parent.add( new Chains( new PointF( a.x - 4, a.y - 3 ), new PointF( b.x + 3, b.y - 2 ), Effects.Type.ETHEREAL_CHAIN, null ) );
			s.h.parent.add( new Chains( new PointF( a.x + 4, a.y + 2 ), new PointF( b.x - 3, b.y + 3 ), Effects.Type.ETHEREAL_CHAIN, () -> {
				ch.flash();
				ch.emitter().burst( SparkParticle.FACTORY, 5 );
			} ) );
			ch.emitter().burst( Speck.factory( Speck.STAR ), 6 );
			ch.flash();
			s.center( ch.ch.pos ).burst( SparkParticle.FACTORY, 8 );
		} ).rat( 4, 4 ).heroClass( HeroClass.DUELIST );
		//Iron Tip: electric sparks
		r.entry( "iron-tip", s -> s.center( s.d[0].ch.pos ).burst( SparkParticle.FACTORY, 4 ) ).rat( 6, 4 );
		//Lock Smith: a trap's sparks
		r.entry( "locksmith-disarm", s -> s.center( s.hero.pos ).burst( SparkParticle.FACTORY, 6 ) );
		r.entry( "locksmith-reveal", s -> {
			for (int c : TRAPS) s.get( c ).burst( SparkParticle.FACTORY, 5 );
		} );
		//Lunge: the dash, the sword spectacle behind her, stars on the stabbed
		r.entry( "lunge", SkillPreviewsMartialBefore::lunge ).hero( 2, 4 ).rat( 6, 4 ).heroClass( HeroClass.DUELIST );
		//True Edge: a beam of light through the lane
		r.entry( "true-edge", s -> {
			trueEdge( s );
			s.h.parent.add( new Beam.LightRay( DungeonTilemap.tileCenterToWorld( s.hero.pos ), DungeonTilemap.tileCenterToWorld( at( 6, 4 ) ) ) );
		} ).rat( 4, 4 ).rat( 5, 4 ).rat( 6, 4 ).heroClass( HeroClass.DUELIST );
		//Hamstring: a skeleton's bones
		r.entry( "hamstring", s -> {
			s.get( s.d[0].ch.pos ).burst( Speck.factory( Speck.BONE ), 3 );
			s.bottom( s.d[0].ch.pos ).burst( Speck.factory( Speck.DUST ), 3 );
			s.d[0].flash();
		} ).rat( 4, 4 );
		//Banner: a golden spear lying on the floor, a beam of light down onto it
		r.entry( "banner-plant", s -> {
			oldPillar( s, BANNER, 0xFFD060 );
			banner( s, BANNER );
		} ).view( 1, 0, 9, 8 );
		r.entry( "banner-standing", s -> {
			banner( s, BANNER );
			s.later( 0.2f, () -> s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 2 ) );
			s.later( 1.2f, () -> s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 2 ) );
		} ).seconds( 2.5f );
		//Deadfall: runestones raining on every cell, rocks
		r.entry( "deadfall-trigger", s -> deadfall( s ) ).rat( 6, 4 ).rat( 5, 5 ).view( 1, 0, 9, 9 ).seconds( 1.5f );
		//Fleche / Hurl: a level-up's stars on her, a small fan in her hand
		r.entry( "fleche-hurl-flourish", s -> {
			s.h.emitter().burst( Speck.factory( Speck.STAR ), 4 );
			s.later( 0.5f, () -> {
				new Flare( 4, 8 ).color( 0xFFFFFF, true ).show( s.h, 0.25f );
				s.h.emitter().burst( Speck.factory( Speck.STAR ), 3 );
			} );
		} ).heroClass( HeroClass.DUELIST );
		//Dragon Kick: slammed into a wall, rocks
		r.entry( "dragon-kick-wall", s -> {
			new Flare( 6, 26 ).color( 0xFFAA44, true ).show( s.h, 0.5f );
			Wound.hit( s.d[0].ch );
			s.d[0].emitter().burst( Speck.factory( Speck.STAR ), 4 );
			s.center( s.d[0].ch.pos ).burst( Speck.factory( Speck.ROCK ), 5 );
		} ).rat( 4, 4 ).heroClass( HeroClass.DUELIST );
		//Overwatch: a pinned target's slam, rocks
		r.entry( "overwatch-pin", s -> {
			s.d[0].emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
			s.d[0].flash();
			s.get( s.d[0].ch.pos ).burst( Speck.factory( Speck.ROCK ), 4 );
		} ).rat( 6, 4 ).heroClass( HeroClass.HUNTRESS );
		//Accuracy, Weapon Bond: the old fans
		r.entry( "accuracy", s -> new Flare( 4, 16 ).color( 0xFF4444, true ).show( s.d[0], 0.5f ) ).rat( 6, 4 ).heroClass( HeroClass.HUNTRESS );
		r.entry( "weapon-bond", s -> {
			new Flare( 6, 20 ).color( 0xCCEEFF, true ).show( s.h, 0.5f );
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 4 );
		} ).heroClass( HeroClass.DUELIST );
		//Blood Dance: a spinning red fan and a ring of red splashes
		r.entry( "blood-dance", s -> {
			s.h.emitter().burst( Speck.factory( Speck.RED_LIGHT ), 6 );
			new Flare( 6, 20 ).color( 0xCC2222, true ).show( s.h, 0.6f ).angularSpeed = 120;
			int[] around = around( s );
			FxTimeline t = FxTimeline.start();
			for (int i = 0; i < around.length; i++){
				final int c = around[i];
				t.at( 0.05f * i, () -> Splash.at( c, 0xCC1111, 3 ) );
			}
		} ).heroClass( HeroClass.ROGUE );
	}

	// ------------------------------------------------------------------ shared stages

	static final int BANNER = at( 6, 4 );
	static final int[] TRAPS = { at( 5, 2 ), at( 6, 5 ), at( 2, 6 ) };

	static SkillPreviewDump.Entry rats( SkillPreviewDump.Entry e, int[][] cells ){
		for (int[] c : cells) e.rat( c[0], c[1] );
		return e;
	}

	//the eight cells round a cell, north-west first, clockwise
	static int[] around( SkillPreviewDump.Scene s ){
		int w = SkillPreviewDump.W, p = s.hero.pos;
		return new int[]{ p - w - 1, p - w, p - w + 1, p + 1, p + w + 1, p + w, p + w - 1, p - 1 };
	}

	static int[] ring( int c ){
		int w = SkillPreviewDump.W;
		return new int[]{ c - w - 1, c - w, c - w + 1, c + 1, c + w + 1, c + w, c + w - 1, c - 1 };
	}

	//SkillFX.land: its dust and its landing
	static void land( SkillPreviewDump.Scene s, int cell ){
		s.bottom( cell ).burst( Speck.factory( Speck.DUST ), 8 );
		SkillFX.landing( cell );
	}

	//a stance raised: the arming burst, then the stance's slow trickle off him
	static void stance( SkillPreviewDump.Scene s, Emitter.Factory arm, Emitter.Factory trickle ){
		s.h.emitter().burst( arm, 6 );
		Emitter e = new Emitter();
		e.pos( s.h );
		s.h.parent.add( e );
		e.pour( trickle, 0.5f );
	}

	//Shieldbearer's +3 bash rings out round where he stands (StaggerFX.ring at 0.38 s)
	static void bash( SkillPreviewDump.Scene s, int stand ){
		s.later( 0.38f, () -> {
			for (int c : ring( stand )){
				WarriorImpactFX.show( c );
				s.bottom( c ).burst( Speck.factory( Speck.DUST ), 2 );
			}
		} );
	}

	//a summoned rat, crab, skeleton and shieldbearer by a hostile rat
	static void summons( SkillPreviewDump.Scene s ){
		SummonedPet a = MartialPreviewStage.pet( s, SummonedPet.PET_TYPES.RAT, at( 5, 2 ) );
		SummonedPet b = MartialPreviewStage.pet( s, SummonedPet.PET_TYPES.CRAB, at( 5, 6 ) );
		SummonedPet c = MartialPreviewStage.pet( s, SummonedPet.PET_TYPES.SKELETON, at( 2, 2 ) );
		SummonedPet d = MartialPreviewStage.pet( s, GuardSprite.class, at( 2, 6 ) );
		MartialPreviewStage.orbits( a.sprite, b.sprite, c.sprite, d.sprite, s.d[0] );
	}

	//Summon Crab: the crab at its cell, fading in over 0.15 s
	static SummonedPet crab( SkillPreviewDump.Scene s ){
		SummonedPet crab = MartialPreviewStage.pet( s, SummonedPet.PET_TYPES.CRAB, at( 4, 4 ) );
		crab.sprite.alpha( 0 );
		crab.sprite.parent.add( new AlphaTweener( crab.sprite, 1, 0.15f ) );
		MartialPreviewStage.orbits( crab.sprite );
		return crab;
	}

	//Lunge: the dash from (2, 4) to (5, 4), the stab on (6, 4) when she lands (master's: the sword
	//spectacle behind her, stars on it)
	static void lunge( SkillPreviewDump.Scene s ){
		final int from = s.hero.pos, land = at( 5, 4 );
		StreakFX.dash( from, land, 0xFFFFFF, 0.5f, 0.35f, true );
		final CharSprite enemy = s.d[0];
		s.h.jump( from, land, () -> {
			s.h.place( land );
			enemy.flash();
			SkillSpectacleFX.fly( SkillSpectacleFX.SABER, from, land, 0, .4f );
			s.bottom( land ).burst( Speck.factory( Speck.DUST ), 4 );
			enemy.emitter().burst( Speck.factory( Speck.STAR ), 4 );
		} );
	}

	//True Edge's crit on the first and its cut on through the rest (TrueEdge.Slash.draw, Wound.hit)
	static void trueEdge( SkillPreviewDump.Scene s ){
		Wound.hit( s.d[0].ch );
		ArcSpinFX.at( s.d[0].ch.pos, 0xFFFFFF, 10, 0.3f, 220, 1100, 0.22f );
		s.d[0].flash();
		for (int k = 1; k < s.d.length; k++) Wound.hit( s.d[k].ch );
	}

	//SkillFX.pillar as master drew it: a beam down onto the cell, a flare at its foot, the light burst
	static void oldPillar( SkillPreviewDump.Scene s, int cell, int color ){
		PointF foot = DungeonTilemap.raisedTileCenterToWorld( cell );
		PointF top = new PointF( foot.x, foot.y - DungeonTilemap.SIZE * 7 );
		s.h.parent.add( new Beam.LightRay( top, foot ) );
		PixelFlare.show( foot.x, foot.y, color, PixelFlare.L, 0.8f );
		s.center( cell ).burst( Speck.factory( Speck.LIGHT ), 6 );
	}

	//BannerStandard.fx as master drew it: a golden spear on the cell, yellow light poured from its foot
	static void banner( SkillPreviewDump.Scene s, int cell ){
		ItemSprite pole = new ItemSprite( ItemSpriteSheet.SPEAR );
		pole.hardlight( 0xFFD060 );
		pole.place( cell );
		s.h.parent.add( pole );
		s.center( cell ).pour( Speck.factory( Speck.YELLOW_LIGHT ), 0.4f );
	}

	//Deadfall's drop over a 3x3 at (6, 4) as master drew it: dust in a ring two out, then
	//runestones and rocks on each cell 0.04 s after the last
	static void deadfall( SkillPreviewDump.Scene s ){
		int centre = at( 6, 4 );
		dustRing( s, centre );
		StoneOfBlast look = new StoneOfBlast();
		int order = 0;
		for (int c : nine( centre )){
			final int at = c, n = 1 + (order % 2);
			s.later( 0.1f + 0.04f * order++, () -> {
				SkillFX.rain( at, look, n, null );
				s.get( at ).burst( Speck.factory( Speck.ROCK ), 4 );
				s.bottom( at ).burst( Speck.factory( Speck.DUST ), 3 );
			} );
		}
	}

	static void dustRing( SkillPreviewDump.Scene s, int centre ){
		int w = SkillPreviewDump.W;
		for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++){
			if (Math.max( Math.abs( dx ), Math.abs( dy ) ) != 2) continue;
			final int c = centre + dx + dy * w;
			s.later( 0.04f, () -> s.bottom( c ).burst( Speck.factory( Speck.DUST ), 2 ) );
		}
	}

	static int[] nine( int c ){
		int w = SkillPreviewDump.W;
		return new int[]{ c - w - 1, c - w, c - w + 1, c - 1, c, c + 1, c + w - 1, c + w, c + w + 1 };
	}
}

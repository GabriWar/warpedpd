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
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.effects.Flare;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.effects.Surprise;
import xyz.gabriwar.warpedpixeldungeon.effects.Wound;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.ClosingRing;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.PixelFlare;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.PoisonParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.particles.SparkParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.ArcSpinFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.PulseRingFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.SkillBursts;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StaggerFX;
import xyz.gabriwar.warpedpixeldungeon.effects.skillfx.StreakFX;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GooSprite;
import com.watabou.noosa.particles.Emitter;
import com.watabou.utils.PathFinder;

import static xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.preview.SkillPreviewDump.at;

/**
 * The rfx-stars package's moments as f0783eacd draws them (its calls only, so it compiles and is
 * dumped in a checkout of it, FXPREVIEW_REGISTRIES=RfxStars): every skill's star burst where it
 * stood - a body really dazed, dizzy or held (the daze sheet, Last Stand with a rat and a boss,
 * Lunge on a dazed foe, Sixth Sense's striker), a blow that dazes no one (steel-sparks), a parry,
 * read or mark (steel-glints), the hero's own success (flourish-hero), the duelist weapons'
 * abilities - and Second Wind on its own duelist. Each moment is a helper taking the factories
 * that changed, so SkillPreviewsRfxStars plays the very same stage with the new ones.
 */
public final class SkillPreviewsRfxStarsBefore {

	private SkillPreviewsRfxStarsBefore(){}

	public static void register( SkillPreviewDump.Registry r ){
		Emitter.Factory star = Speck.factory( Speck.STAR );
		r.entry( "daze-sheet", s -> dazeSheet( s, star, SparkParticle.FACTORY ) ).view( 0, 0, 9, 8 ).hero( 8, 7 )
				.rat( 1, 2 ).rat( 4, 2 ).rat( 7, 2 ).rat( 1, 5 ).rat( 4, 5 ).rat( 7, 5 ).seconds( 1.5f );
		r.entry( "last-stand", s -> lastStand( s, star, star ) ).hero( 4, 4 ).rat( 5, 4 ).seconds( 2f );
		r.entry( "steel-sparks", s -> steelSparks( s, star ) ).rat( 5, 2 ).rat( 6, 4 ).rat( 5, 6 ).rat( 7, 2 )
				.view( 1, 0, 9, 8 ).seconds( 2f );
		r.entry( "steel-glints", s -> steelGlints( s, star ) ).heroClass( HeroClass.DUELIST ).rat( 5, 4 ).rat( 6, 2 ).seconds( 2f );
		r.entry( "flourish-hero", s -> flourishHero( s, star, star, star, star ) ).hero( 4, 4 ).heroClass( HeroClass.HUNTRESS ).seconds( 2f );
		r.entry( "weapon-abilities", s -> weaponAbilities( s, star, star, star, Speck.factory( Speck.SCREAM ), Speck.factory( Speck.SCREAM ) ) ).hero( 4, 4 ).heroClass( HeroClass.DUELIST )
				.rat( 2, 3 ).rat( 7, 4 ).rat( 5, 5 ).rat( 5, 3 ).view( 0, 1, 9, 7 ).seconds( 2f );
		//three moments the base dump has, staged again to show what changed: Lunge on a foe really
		//dazed, Sixth Sense's +3 dazing its striker, Second Wind on the duelist it belongs to
		r.entry( "lunge", s -> lunge( s, star ) ).hero( 2, 4 ).rat( 6, 4 ).heroClass( HeroClass.DUELIST ).seconds( 1.5f );
		r.entry( "sixth-sense", s -> sixthSense( s, star ) ).heroClass( HeroClass.HUNTRESS ).rat( 4, 4 ).seconds( 1.5f );
		r.entry( "second-wind", s -> secondWind( s, 0xB3D699 ) ).heroClass( HeroClass.DUELIST ).seconds( 1.5f );
	}

	// ------------------------------------------------------------------ the moments

	/** A body really dazed, dizzy or held, one skill a rat: Sidestep (2, dazed), Pirouette's +3 (3,
	 *  dizzy), Head Shot's ring-out (3, dizzy), Iron Stance's +3 (4, dizzy), Scorpion's mastery (4 on
	 *  its cell, held: the real stun's stars on it too, and the poison's splash), Piercing Focus (2,
	 *  dazed, and its pin on the cell: the shock's sparks then, a steel glint now). */
	static void dazeSheet( SkillPreviewDump.Scene s, Emitter.Factory daze, Emitter.Factory pin ){
		s.d[0].emitter().burst( daze, 2 );
		s.d[1].emitter().burst( daze, 3 );
		s.d[2].emitter().burst( daze, 3 );
		s.d[3].emitter().burst( daze, 4 );
		s.center( s.d[4].ch.pos ).burst( daze, 4 );
		s.center( s.d[4].ch.pos ).burst( PoisonParticle.SPLASH, 5 );
		MartialPreviewStage.stunStars( s.d[4] );
		s.d[5].emitter().burst( daze, 2 );
		s.center( s.d[5].ch.pos ).burst( pin, 3 );
	}

	/** Last Stand at +3: his golden flares, light streaming off him, the landing; a rat beside him
	 *  held (the real stun's stars on it) and a boss beside him that it cannot hold. */
	static void lastStand( SkillPreviewDump.Scene s, Emitter.Factory onHeld, Emitter.Factory onBoss ){
		CharSprite boss = boss( s, at( 3, 4 ) );
		new Flare( 6, 20 ).color( 0xFFCC66, true ).show( s.h, 0.6f );
		s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 5 );
		s.h.emitter().start( Speck.factory( Speck.YELLOW_LIGHT ), 0.05f, 6 );
		StaggerFX.after( 0.2f, () -> new Flare( 8, 28 ).color( 0xFFE6A1, true ).show( s.h, 0.5f ) );
		//SkillFX.land, restaged (its CellEmitter wants a scene)
		SkillPreviewsMartialBefore.land( s, s.hero.pos );
		s.d[0].emitter().burst( onHeld, 3 );
		MartialPreviewStage.stunStars( s.d[0] );
		boss.emitter().burst( onBoss, 3 );
	}

	/** A boss (Goo's sprite) at a cell, facing the hero. */
	static CharSprite boss( SkillPreviewDump.Scene s, int cell ){
		Rat ch = new Rat();
		ch.state = ch.PASSIVE;
		ch.HP = ch.HT = 50;
		ch.pos = cell;
		CharSprite b = new GooSprite();
		b.ch = ch;
		ch.sprite = b;
		s.rig.stage.add( b );
		b.place( cell );
		b.visible = true;
		b.flipHorizontal = cell % SkillPreviewDump.W > s.hero.pos % SkillPreviewDump.W;
		return b;
	}

	/** Blows that daze no one, 0.3 s apart: Aggression's cleave (a flash), Mastery's strike on one
	 *  stepping in (light off him), a Head Shot and Hunting's ambush (each knocked down after the
	 *  blow: the real stun's stars). */
	static void steelSparks( SkillPreviewDump.Scene s, Emitter.Factory spark ){
		Wound.hit( s.d[0].ch );
		s.d[0].flash();
		s.d[0].emitter().burst( spark, 4 );
		s.later( 0.3f, () -> {
			s.h.emitter().burst( Speck.factory( Speck.LIGHT ), 3 );
			Wound.hit( s.d[1].ch );
			s.d[1].emitter().burst( spark, 4 );
		} );
		s.later( 0.6f, () -> {
			Wound.hit( s.d[2].ch );
			s.d[2].emitter().burst( spark, 6 );
			MartialPreviewStage.stunStars( s.d[2] );
		} );
		s.later( 0.9f, () -> {
			Surprise.hit( s.d[3].ch );
			s.d[3].emitter().burst( spark, 4 );
			MartialPreviewStage.stunStars( s.d[3] );
		} );
	}

	/** The duelist's parries, reads and marks: a parry (0), Blade Mastery's read on the near rat
	 *  (0.2), Riposte's stance up (0.5), Precision's Gap mark on the far rat (0.7), Riposte's answer
	 *  (0.9), Counter-Time's cut (1.3), a dodge read off the blade (1.6). */
	static void steelGlints( SkillPreviewDump.Scene s, Emitter.Factory glint ){
		final CharSprite near = s.d[0], far = s.d[1];
		s.h.emitter().burst( glint, 6 );
		new Flare( 5, 16 ).color( 0xFFFFFF, true ).show( s.h, 0.4f );
		ArcSpinFX.slash( s.h, 0xFFFFFF, true );
		s.later( 0.2f, () -> near.emitter().burst( glint, 3 ) );
		s.later( 0.5f, () -> {
			new Flare( 4, 12 ).color( 0xFFFFFF, true ).show( s.h, 0.35f );
			s.h.emitter().burst( glint, 3 );
		} );
		s.later( 0.7f, () -> {
			new Flare( 4, 14 ).color( 0xE6EDF5, true ).show( far, 0.5f );
			far.emitter().burst( glint, 3 );
			SkillInteractions.flare( near.ch.pos, 0xE6EDF5 );
		} );
		s.later( 0.9f, () -> {
			ArcSpinFX.slash( s.h, 0xFFFFFF, true );
			StreakFX.show( s.hero.pos, near.ch.pos, 0xFFFFFF, 0.4f, 0.25f );
			near.flash();
			s.h.emitter().burst( glint, 4 );
		} );
		s.later( 1.3f, () -> {
			StreakFX.show( s.hero.pos, near.ch.pos, 0xFFFFFF, 0.4f, 0.28f );
			near.flash();
			s.h.emitter().burst( glint, 4 );
		} );
		s.later( 1.6f, () -> s.h.emitter().burst( glint, 3 ) );
	}

	/** The hero's own success, 0.45 s apart, each in its skill's class: Awareness (a dodge sensed,
	 *  the huntress), Rampage's stance (the warrior), Bandit's snatch (the rogue), Piercing Focus's
	 *  cast (the huntress). */
	static void flourishHero( SkillPreviewDump.Scene s, Emitter.Factory awareness, Emitter.Factory rampage,
			Emitter.Factory bandit, Emitter.Factory focus ){
		s.h.emitter().burst( awareness, 4 );
		s.later( 0.45f, () -> {
			s.hero.heroClass = HeroClass.WARRIOR;
			s.h.emitter().burst( rampage, 5 );
		} );
		s.later( 0.9f, () -> {
			s.hero.heroClass = HeroClass.ROGUE;
			s.h.emitter().burst( bandit, 4 );
		} );
		s.later( 1.35f, () -> {
			s.hero.heroClass = HeroClass.HUNTRESS;
			s.h.emitter().burst( focus, 3 );
		} );
	}

	/** The duelist weapons: the Trumpet's fanfare (an enemy held with a shriek, the ally and the hero
	 *  blessed, a gold fan), the Wardrum's beat (0.8: the ally hasted, the one beside him terrified,
	 *  dust all round) and Fight Gloves' three jabs (1.3, 1.4, 1.5: the third holds it). */
	static void weaponAbilities( SkillPreviewDump.Scene s, Emitter.Factory blessed, Emitter.Factory hasted, Emitter.Factory jab,
			Emitter.Factory onHeld, Emitter.Factory onScared ){
		final CharSprite ally = s.d[0], held = s.d[1], scared = s.d[2], punched = s.d[3];
		held.emitter().burst( onHeld, 3 );
		MartialPreviewStage.stunStars( held );
		ally.emitter().burst( blessed, 3 );
		new Flare( 8, 12 + 4 * 3 ).color( 0xFFD700, true ).show( s.h, 0.9f ).angularSpeed = 45;
		s.h.emitter().burst( blessed, 6 );
		s.later( 0.8f, () -> {
			ally.emitter().burst( hasted, 3 );
			scared.emitter().burst( onScared, 2 );
			for (int n : PathFinder.NEIGHBOURS8) s.bottom( s.hero.pos + n ).burst( Speck.factory( Speck.DUST ), 2 );
		} );
		for (int i = 0; i < 3; i++){
			final int k = i;
			s.later( 1.3f + 0.1f * i, () -> {
				punched.emitter().burst( jab, 2 );
				if (k == 2){
					Wound.hit( punched.ch );
					MartialPreviewStage.stunStars( punched );
				}
			} );
		}
	}

	/** Lunge onto a foe really dazed (SkillPreviewsMartial's lunge, f0783eacd's: the dash, the leap,
	 *  the landing's dust, the stab's burst). */
	static void lunge( SkillPreviewDump.Scene s, Emitter.Factory dazed ){
		final int from = s.hero.pos, land = at( 5, 4 );
		StreakFX.dash( from, land, 0xFFFFFF, 0.5f, 0.35f, true );
		final CharSprite enemy = s.d[0];
		s.h.jump( from, land, () -> {
			s.h.place( land );
			enemy.flash();
			s.bottom( land ).burst( Speck.factory( Speck.DUST ), 4 );
			enemy.emitter().burst( dazed, 4 );
		} );
	}

	/** Sixth Sense's warning on the huntress, and at +3 its striker dazed. */
	static void sixthSense( SkillPreviewDump.Scene s, Emitter.Factory dazed ){
		PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), 0x88CC66, PixelFlare.M, 0.6f );
		ClosingRing.on( s.h, 31, 9, 0.45f, 0x88CC66 );
		s.h.emitter().burst( SkillBursts.FLOURISH, 6 );
		s.d[0].emitter().burst( dazed, 4 );
	}

	/** Second Wind: the weapon's charges back, its flare and ring in a colour, breath rising. */
	static void secondWind( SkillPreviewDump.Scene s, int color ){
		PixelFlare.show( SkillVfx.midX( s.h ), SkillVfx.midY( s.h ), color, PixelFlare.M, 0.6f );
		PulseRingFX.around( s.h, color, 16, 0.4f );
		s.h.emitter().burst( SkillBursts.BREATH, 5 );
	}
}

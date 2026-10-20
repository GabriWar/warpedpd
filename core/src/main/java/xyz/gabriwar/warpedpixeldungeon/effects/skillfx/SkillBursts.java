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

package xyz.gabriwar.warpedpixeldungeon.effects.skillfx;

import com.watabou.noosa.Gizmo;
import com.watabou.noosa.particles.Emitter;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Element;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxBudget;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFactories;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxFrames;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxParticle;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxRandom;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Ramp;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.WaterFX;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillPalette;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.skills.SkillVfx;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;

/**
 * The skills' own bursts: factories a skill's existing burst swaps in for one that borrowed the
 * game's look (an alarm's brackets, a curse's shadow, a level-up's stars, a rockfall's rocks, a
 * charm's hearts, a gas). The call stays where it was, on the same emitter and the same thread,
 * so the game's Random is drawn exactly as before; only what each emit makes changes. Each emit
 * makes at most two kit particles, drawing only FxRandom; the light ones are made in a carrier of
 * the light layer of their own (Fx.lightCarrier, as HolyFlameParticle), so they read over the
 * night's tint whatever emitter bursts them; the matter ones in the emitter that burst them.
 * Every one is registered with NetVisuals as "skill_" + its name, so a co-op guest sees the same
 * burst (docs/fx/skills.md, SkillBursts).
 *
 * Priorities: P2 the burst's body, P3 its dressing (as FxBudget asks; desktop x1.5).
 */
public final class SkillBursts {

	private SkillBursts(){}

	// ------------------------------------------------------------------ the schools' ramps

	//the schools' colours by meaning (SkillPalette): no ramp here starts at white (its palest stop
	//is a coloured flash), so a burst never reads as a white spark before its colour arrives

	/** The skills' fire: embers and motes, never a flame's tongue (SkillPalette.FIRE: orange, never peach). */
	public static final Ramp FIRE = SkillPalette.FIRE;
	/** The storm's sparks in the game's white-blue (SkillPalette.LIGHTNING). */
	public static final Ramp STORM = SkillPalette.LIGHTNING;
	public static final Ramp HOLY = SkillPalette.HOLY;
	/** The mage's soul: a ghost's cold lavender-grey, apart from the rogue's lilac. */
	public static final Ramp SOUL = SkillPalette.SOUL;
	/** HP gained: green light (its crosses are Speck.HEALING's or MoteFX.crosses). */
	public static final Ramp HEAL = SkillPalette.HEAL;
	/** Blood: crimson, never pink. */
	public static final Ramp BLOOD = SkillPalette.BLOOD;
	public static final Ramp STONE = new Ramp( 0x8F8A82, 0x6E6A64, 0x4E4B47 );
	/** A fuse's spark: a pale hot yellow to amber. */
	public static final Ramp FUSE_RAMP = new Ramp( 0xFFF0A0, 0xFFC070 );
	public static final Ramp FROST = new Ramp( 0xA4E9FF, 0xFFFFFF );
	public static final Ramp STEEL = new Ramp( 0xB9DEEE, 0xFFFFFF );
	public static final int TREMOR = 0xF0E8FF, SCENT_COLOR = 0xE8C27A;
	public static final int[] FEATHERS = { 0xC8B89A, 0xF3EBD7 };
	private static final int[][] CHIPS = { FxFrames.PEBBLE_3, FxFrames.PEBBLE_2, FxFrames.PEBBLE_3V };

	// ------------------------------------------------------------------ the bursts

	/** Fear sinking in: ink wisps curling down (never rising like a curse's or a grim trap's
	 *  shadow), now and then a drop of ink. Matter. Replaces ShadowParticle.CURSE / MISSILE on enemies. */
	public static final Emitter.Factory DREAD = new Matter(){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null){
				p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( FxRandom.Float( 0.2f, 0.5f ) ) )
						.vel( FxRandom.Float( -6f, 6f ), FxRandom.Float( 1f, 4f ) ).turbulence( 1.5f, 3f ).drag( 1.5f )
						.life( FxRandom.Float( 0.6f, 0.8f ) ).alpha( 0.75f, 0.08f, 0.35f ).smooth().priority( FxBudget.P2 );
			}
			if (FxRandom.chance( 0.25f )){
				p = particle( e, x, y, FxBudget.P2 );
				if (p != null) p.look( FxFrames.INK_CORE ).paint( Element.SHADOW.ramp.step( 0.6f ) ).vel( 0, 2f ).life( 0.5f )
						.alpha( 0.6f, 0, 0.25f ).smooth().priority( FxBudget.P2 );
			}
		}
	};

	/** A tremor: thin ticks of light either side of the body (a shudder, never an alarm's
	 *  brackets). Light. Replaces Speck.SCREAM on enemies. */
	public static final Emitter.Factory TREMBLE = new Light( TrembleCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxFrames.SPARK_2 ).orient().paint( TREMOR ).light( true )
					.vel( FxRandom.sign() * FxRandom.Float( 28f, 40f ), FxRandom.Float( -2f, 2f ) ).drag( 6f )
					.life( 0.18f ).alpha( 0.9f, 0, 0.12f ).priority( FxBudget.P2 );
		}
	};

	/** A shout's motes thrown out from the body in the hero's class colour. Light. Replaces
	 *  Speck.SCREAM on the hero. */
	public static final Emitter.Factory ROAR = new Light( RoarCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p == null) return;
			float a = outward( x, y );
			float v = FxRandom.Float( 50f, 80f );
			p.look( FxFrames.MOTE_2 ).paint( Ramp.lerp( 0xFFFFFF, SkillVfx.classColor(), FxRandom.Float( 0.3f, 1f ) ) ).light( true )
					.vel( (float) Math.cos( a ) * v, (float) Math.sin( a ) * v * 0.5f ).drag( 4f ).life( 0.35f )
					.alpha( 1f, 0, 0.2f ).priority( FxBudget.P2 );
		}
	};

	/** Grit: a puff of dust pushed up and now and then a pebble hopping out and bouncing once (on
	 *  water a drop flicked up, ringing). Matter. Lane dust, legs giving way, small knocks. */
	public static final Emitter.Factory GRIT = new Matter(){
		@Override
		void make( Emitter e, float x, float y ){
			if (WaterFX.onWater( x, y + 3 )){
				//on water: a few drops flicked up and ringing where they fall, no dust
				FxParticle d = particle( e, x, y, FxBudget.P3 );
				if (d != null) d.look( FxFrames.DROP_2 ).orient().paint( 0xC8E0E8 )
						.vel( FxRandom.Float( -20f, 20f ), FxRandom.Float( -35f, -20f ) ).gravity( 200f )
						.ground( y + 3, FxParticle.RING, 0 ).strength( 0.1f ).life( 0.5f ).alpha( 0.9f, 0, 0.15f ).priority( FxBudget.P3 );
				return;
			}
			FxParticle p = particle( e, x, y, FxBudget.P3 );
			if (p != null) p.look( FxFrames.PUFF_S[FxRandom.Int( 2 )] ).paint( Element.EARTH.smoke.step( 0.4f ) )
					.vel( FxRandom.Float( -10f, 10f ), FxRandom.Float( -3f, -1f ) ).drag( 3f ).life( 0.35f )
					.alpha( 0.5f, 0, 0.2f ).priority( FxBudget.P3 );
			if (FxRandom.chance( 0.5f )){
				p = particle( e, x, y, FxBudget.P3 );
				if (p != null) p.look( FxFrames.PEBBLE_2 ).paint( Element.EARTH.ember.step( FxRandom.Float() ) )
						.vel( FxRandom.Float( -25f, 25f ), FxRandom.Float( -30f, -15f ) ).gravity( 200f )
						.ground( y + 3, FxParticle.BOUNCE, 0.3f ).life( 0.5f ).alpha( 1f, 0, 0.2f ).priority( FxBudget.P3 );
			}
		}
	};

	/** Chips of stone thrown outward and up from where it bursts, falling and bouncing: never
	 *  rocks dropping from above. Matter. Replaces Speck.ROCK. */
	public static final Emitter.Factory STONE_CHIP = new Matter(){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null){
				p.look( CHIPS[FxRandom.Int( 3 )] ).paint( STONE.step( FxRandom.Float() ) )
						.vel( FxRandom.sign() * FxRandom.Float( 30f, 60f ), FxRandom.Float( -60f, -35f ) ).gravity( 220f )
						.ground( y + 5, FxParticle.RING, 0.3f ).landDry( FxParticle.BOUNCE ).strength( 0.2f ).spin( 0.08f )
						.life( 0.7f ).alpha( 1f, 0, 0.2f )
						.priority( FxBudget.P2 );
			}
			if (FxRandom.chance( 0.3f )){
				p = particle( e, x, y, FxBudget.P3 );
				if (p != null) p.look( FxFrames.PUFF_S[FxRandom.Int( 2 )] ).paint( Element.EARTH.smoke.step( 0.4f ) )
						.vel( FxRandom.Float( -10f, 10f ), FxRandom.Float( -4f, -1f ) ).drag( 3f ).life( 0.35f )
						.alpha( 0.5f, 0, 0.2f ).priority( FxBudget.P3 );
			}
		}
	};

	/** Sparks of struck metal: the kit's METAL spark (a 1x2 streak of the metal's embers turned to its
	 *  way, a dot once slow, its last twinkle), kept short where it strikes: off into the upper half
	 *  at 25 to 50 px/s with a drag of 3, falling, dying where it lands 4 to 8 px below (never bouncing
	 *  away): at most 16 px from where it was struck, never off the screen. Light. Replaces
	 *  SparkParticle (non-electric), Speck.FORGE, Speck.STAR on a blow that dazes no one. */
	public static final Emitter.Factory METAL_SPARK = new Light( MetalCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 15f ).ramp( Element.METAL.ember, true )
					.life( FxRandom.Float( 0.3f, 0.5f ) ).alpha( 1f, 0, 0.1f )
					.polar( FxRandom.Float( (float) -Math.PI, 0f ), FxRandom.Float( 25f, 50f ) ).gravity( 120f ).drag( 3f )
					.ground( y + FxRandom.Float( 4f, 8f ), FxParticle.DIE, 0 ).fate( FxFactories.SPARK_END ).priority( FxBudget.P2 );
		}
	};

	/** A lock clicking: a steel glint in place. Light. "A lock clicks", a disarm. */
	public static final Emitter.Factory STEEL_GLINT = new Light( SteelCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxRandom.chance( 0.5f ) ? FxFrames.PLUS_3 : FxFrames.STAR4_5 ).ramp( STEEL, true )
					.snap().life( 0.3f ).alpha( 1f, 0, 0.15f ).priority( FxBudget.P2 );
		}
	};

	/** Arcane sparks (the kit's ARCANE spark). Light. Replaces SparkParticle on arcane skills. */
	public static final Emitter.Factory ARCANE_SPARK = carried( ArcaneCarried.class, FxFactories.spark( Element.ARCANE ) );

	/** A storm's sparks in the storm's tint, never the Shocking proc's or a wand's blue-white.
	 *  Light. Replaces SparkParticle on storm skills. */
	public static final Emitter.Factory STORM_SPARK = new Light( StormCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxFrames.SPARK_2 ).orient().slow( FxFrames.MOTE_1, 15f ).ramp( STORM, true )
					.life( FxRandom.Float( 0.25f, 0.45f ) ).alpha( 1f, 0, 0.1f )
					.polar( FxRandom.Float( (float) -Math.PI, 0f ), FxRandom.Float( 30f, 70f ) )
					.gravity( 120f ).drag( 1.2f ).ground( y + FxRandom.Float( 4f, 8f ), FxParticle.BOUNCE, 0.35f )
					.fate( FxFactories.SPARK_END ).priority( FxBudget.P2 );
		}
	};

	/** Ink drifting sideways off a shade (never rising like a wraith). Matter. Replaces
	 *  ShadowParticle.UP on the hero, SMOKE on shadow skills. */
	public static final Emitter.Factory INK_DRIFT = new Matter(){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P3 );
			if (p != null) p.look( FxFrames.INK_WISP, 5 ).paint( Element.SHADOW.ramp.step( FxRandom.Float( 0.2f, 0.45f ) ) )
					.vel( FxRandom.sign() * FxRandom.Float( 6f, 12f ), FxRandom.Float( -1f, 3f ) ).turbulence( 1.5f, 3f )
					.life( 0.8f ).alpha( 0.55f, 0.1f, 0.4f ).smooth().priority( FxBudget.P3 );
		}
	};

	/** Embers of the skill's fire drifting up: motes, never a flame's tongue (the hero is not
	 *  burning). Light. Replaces FlameParticle on the hero or empty floor. */
	public static final Emitter.Factory EMBER_MOTE = new Light( EmberCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_2 : FxFrames.EMBER_2 ).paint( FIRE.at( FxRandom.Float() ) )
					.light( true ).vel( FxRandom.Float( -4f, 4f ), FxRandom.Float( -10f, -6f ) ).sway( 2f, 4f ).life( 0.7f )
					.alpha( 0.9f, 0.05f, 0.35f ).priority( FxBudget.P2 );
		}
	};

	/** Frost glinting in place, never snow falling (the hero is not chilled). Light. Replaces
	 *  SnowParticle on the hero. */
	public static final Emitter.Factory FROST_GLINT = new Light( FrostCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P3 );
			if (p != null) p.look( FxRandom.chance( 0.5f ) ? FxFrames.SNOWFLAKE_5[FxRandom.Int( 2 )] : FxFrames.PLUS_3 )
					.ramp( FROST, true ).snap().life( 0.35f ).alpha( 1f, 0.05f, 0.2f ).priority( FxBudget.P3 );
		}
	};

	/** A feather drifting down and settling. Matter. Replaces Speck.WOOL. */
	public static final Emitter.Factory FEATHER = new Matter(){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P3 );
			if (p != null) p.look( FxFrames.FEATHER ).paint( FEATHERS[FxRandom.Int( 2 )] )
					.vel( FxRandom.Float( -10f, 10f ), FxRandom.Float( -8f, -2f ) ).gravity( 28f ).drag( 2.5f ).sway( 4f, 3f )
					.spin( 0.3f ).ground( y + 8, FxParticle.SETTLE, 0 ).life( 1.2f ).alpha( 1f, 0, 0.4f ).priority( FxBudget.P3 );
		}
	};

	/** The hero's own sparkle as a skill takes: motes and pluses of his class's colour rising
	 *  off him (never the level-up's stars). Light. Replaces Speck.STAR on the hero. */
	public static final Emitter.Factory FLOURISH = new Light( FlourishCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxRandom.chance( 0.6f ) ? FxFrames.MOTE_2 : FxFrames.PLUS_3 )
					.paint( Ramp.lerp( 0xFFFFFF, SkillVfx.classColor(), FxRandom.Float( 0.2f, 0.9f ) ) ).light( true )
					.vel( FxRandom.Float( -8f, 8f ), FxRandom.Float( -26f, -16f ) ).drag( 2f ).life( 0.45f )
					.alpha( 1f, 0.04f, 0.25f ).priority( FxBudget.P2 );
		}
	};

	/** A glint of gold rising a little. Light. Replaces Speck.COIN on enemies, YELLOW_LIGHT pours. */
	public static final Emitter.Factory GOLD_GLINT = new Light( GoldCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxRandom.chance( 0.6f ) ? FxFrames.PLUS_3 : FxFrames.STAR4_5 ).paint( Element.GOLD.light )
					.light( true ).vel( 0, -6f ).snap().life( 0.4f ).alpha( 1f, 0, 0.2f ).priority( FxBudget.P2 );
		}
	};

	/** A verse: runes of holy light rising (never a lullaby's notes). Light. Replaces Speck.NOTE. */
	public static final Emitter.Factory VERSE = new Light( VerseCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxFrames.RUNE_3[FxRandom.Int( 4 )] ).ramp( HOLY, true )
					.vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -14f, -9f ) ).drag( 0.6f ).life( 0.7f )
					.alpha( 1f, 0.05f, 0.3f ).priority( FxBudget.P2 );
		}
	};

	/** A soul's wisp swaying up (never a wraith's shadow). Light. Replaces ShadowParticle.UP on
	 *  soul skills. */
	public static final Emitter.Factory SOUL_WISP = new Light( SoulCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).ramp( SOUL, true )
					.sway( 3f, 6f ).vel( 0, FxRandom.Float( -12f, -7f ) ).life( 0.8f ).alpha( 0.9f, 0.1f, 0.35f )
					.priority( FxBudget.P2 );
		}
	};

	/** Holy motes rising, the cleric's own (never orange fire). Light. Replaces FlameParticle on
	 *  holy skills, ShaftParticle, STAR streams on clerics. */
	public static final Emitter.Factory HOLY_MOTE = new Light( HolyCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxRandom.chance( 0.5f ) ? FxFrames.EMBER_1 : FxFrames.MOTE_2 ).ramp( Element.HOLY.ember, true )
					.vel( FxRandom.Float( -5f, 5f ), FxRandom.Float( -16f, -10f ) ).drag( 0.8f ).life( 0.7f )
					.alpha( 1f, 0.05f, 0.3f ).priority( FxBudget.P2 );
		}
	};

	/** A breath: air, a breath caught, calm (SkillPalette.BREATH, steam-white motes swaying up; never
	 *  a heal: real HP shows crosses). Light. Replaces Speck.UP, STEAM. */
	public static final Emitter.Factory BREATH = new Light( BreathCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P3 );
			if (p != null) p.look( FxRandom.chance( 0.5f ) ? FxFrames.MOTE_1 : FxFrames.MOTE_2 ).ramp( SkillPalette.BREATH, true )
					.vel( FxRandom.Float( -3f, 3f ), FxRandom.Float( -12f, -8f ) ).sway( 2f, 3f ).life( 0.9f )
					.alpha( 0.8f, 0.1f, 0.4f ).priority( FxBudget.P3 );
		}
	};

	/** A heartbeat: two crimson motes thrown apart from the point, a throb (never a charm's
	 *  hearts, never Bleeding). Light. Replaces Speck.HEART, BloodParticle ticks of held pain. */
	public static final Emitter.Factory HEARTBEAT = new Light( HeartCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			float a = FxRandom.angle();
			for (int i = 0; i < 2; i++){
				FxParticle p = particle( e, x, y, FxBudget.P2 );
				if (p == null) continue;
				float t = a + i * (float) Math.PI;
				p.look( FxFrames.MOTE_2 ).ramp( BLOOD, true ).vel( (float) Math.cos( t ) * 18f, (float) Math.sin( t ) * 9f )
						.life( 0.25f ).alpha( 1f, 0, 0.15f ).priority( FxBudget.P2 );
			}
		}
	};

	/** A fuse's sparks spitting (never smoke). Light. Replaces SmokeParticle on the Fuse aura. */
	public static final Emitter.Factory FUSE = new Light( FuseCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxFrames.SPARK_2 ).orient().ramp( FUSE_RAMP, true ).polar( FxRandom.angle(), FxRandom.Float( 30f, 50f ) )
					.life( 0.15f ).alpha( 1f, 0, 0.08f ).priority( FxBudget.P2 );
		}
	};

	/** Ichor: droplets of venom flicked out and splatting (no lingering cloud: it is not a gas).
	 *  Matter. Replaces Speck.TOXIC. */
	public static final Emitter.Factory ICHOR = new Matter(){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p != null) p.look( FxFrames.DROP_2 ).orient().paint( Element.POISON.ramp.step( 0.3f ) )
					.vel( FxRandom.Float( -30f, 30f ), FxRandom.Float( -40f, -20f ) ).gravity( 200f )
					.ground( y + 6, FxParticle.SPLAT, 0 ).life( 0.6f ).alpha( 1f, 0, 0.2f ).priority( FxBudget.P2 );
		}
	};

	/** A scent: a warm glint swaying up (never a stench's gas). Light. Replaces Speck.STENCH. */
	public static final Emitter.Factory SCENT = new Light( ScentCarried.class ){
		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P3 );
			if (p != null) p.look( FxFrames.PLUS_3 ).paint( SCENT_COLOR ).light( true ).sway( 3f, 4f ).vel( 0, -6f ).life( 0.6f )
					.alpha( 0.9f, 0.1f, 0.3f ).priority( FxBudget.P3 );
		}
	};

	/** A real daze, vertigo or paralysis, in the game's own stun picture (Looks.Stunned's colour,
	 *  SkillPalette.DAZE) and in place: a star on the MARK ellipse over the head (a sprite's emitter:
	 *  its middle + DAZE_RX cos a, its top - 2 + DAZE_RY sin a; any other emitter: DAZE_UP px over the
	 *  point), stepping to a plus at half its life, sliding along the ellipse 8 to 12 px/s with a drag
	 *  of 3 (at most 5 px), never falling, never leaving the screen. Light. Replaces Speck.STAR on a
	 *  body really dazed. */
	public static final Emitter.Factory DAZE = new Light( DazeCarried.class ){
		//the star's angle on the ellipse, from emit to make (render thread)
		private float angle;

		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			angle = FxRandom.angle();
			Object t = targetOf( emitter );
			if (t instanceof CharSprite){
				CharSprite s = (CharSprite) t;
				super.emit( emitter, index, SkillVfx.midX( s ) + DAZE_RX * (float) Math.cos( angle ),
						s.y - 2 + DAZE_RY * (float) Math.sin( angle ) );
			} else {
				super.emit( emitter, index, x, y - DAZE_UP );
			}
		}

		@Override
		void make( Emitter e, float x, float y ){
			FxParticle p = particle( e, x, y, FxBudget.P2 );
			if (p == null) return;
			//along the ellipse, one way or the other
			float tx = -DAZE_RX * (float) Math.sin( angle ), ty = DAZE_RY * (float) Math.cos( angle );
			float k = FxRandom.sign() * FxRandom.Float( 8f, 12f ) / Math.max( 0.001f, (float) Math.hypot( tx, ty ) );
			p.look( FxFrames.STAR4_5 ).then( 0.5f, DAZE_PLUS ).paint( SkillPalette.DAZE ).light( true ).vel( tx * k, ty * k ).drag( 3f )
					.life( FxRandom.Float( 0.35f, 0.45f ) ).alpha( 1f, 0.03f, 0.2f ).snap().priority( FxBudget.P2 );
		}
	};
	/** The daze's ellipse over the head (the MARK slot's), px; how far over a point with no sprite. */
	public static final float DAZE_RX = 6f, DAZE_RY = 2.5f, DAZE_UP = 10f;
	private static final int[][] DAZE_PLUS = { FxFrames.PLUS_3 };

	//an emitter's target (a sprite's emitter follows its sprite), or null
	private static final java.lang.reflect.Field TARGET;
	static {
		java.lang.reflect.Field f = null;
		try {
			f = Emitter.class.getDeclaredField( "target" );
			f.setAccessible( true );
		} catch (Exception ignored){
			f = null;
		}
		TARGET = f;
	}

	private static Object targetOf( Emitter e ){
		try {
			return TARGET == null ? null : TARGET.get( e );
		} catch (IllegalAccessException ex){
			return null;
		}
	}

	/** Every burst with its NetVisuals id, in this order (the registry and the tests read it). */
	public static final Object[][] ALL = {
			{ "skill_dread", DREAD }, { "skill_tremble", TREMBLE }, { "skill_roar", ROAR }, { "skill_grit", GRIT },
			{ "skill_stone_chip", STONE_CHIP }, { "skill_metal_spark", METAL_SPARK }, { "skill_steel_glint", STEEL_GLINT },
			{ "skill_arcane_spark", ARCANE_SPARK }, { "skill_storm_spark", STORM_SPARK }, { "skill_ink", INK_DRIFT },
			{ "skill_ember", EMBER_MOTE }, { "skill_frost", FROST_GLINT }, { "skill_feather", FEATHER },
			{ "skill_flourish", FLOURISH }, { "skill_gold", GOLD_GLINT }, { "skill_verse", VERSE }, { "skill_soul", SOUL_WISP },
			{ "skill_holy", HOLY_MOTE }, { "skill_breath", BREATH }, { "skill_heartbeat", HEARTBEAT }, { "skill_fuse", FUSE },
			{ "skill_ichor", ICHOR }, { "skill_scent", SCENT }, { "skill_daze", DAZE } };

	// ------------------------------------------------------------------ the two kinds

	//matter: made in the emitter that burst it
	private abstract static class Matter extends Emitter.Factory {
		abstract void make( Emitter e, float x, float y );

		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			make( emitter, x, y );
		}

		FxParticle particle( Emitter e, float x, float y, int priority ){
			if (!FxBudget.keep( priority, x, y )) return null;
			return ((FxParticle) e.recycle( FxParticle.class )).reset( x, y ).priority( priority );
		}
	}

	//light: made in a carrier of the light layer of its own (the emitter's own when there is no
	//scene), drawn added over the night's tint
	private abstract static class Light extends Matter {
		private final Class<? extends Gizmo> carried;

		Light( Class<? extends Gizmo> carried ){
			this.carried = carried;
		}

		@Override
		public void emit( Emitter emitter, int index, float x, float y ){
			Emitter host = Fx.lightCarrier( carried );
			make( host != null ? host : emitter, x, y );
		}

		@Override
		FxParticle particle( Emitter e, float x, float y, int priority ){
			FxParticle p = super.particle( e, x, y, priority );
			return p == null ? null : p.light( true );
		}

		@Override
		public boolean lightMode(){
			return true;
		}
	}

	//one of the kit's factories, carried to the light layer under a key of its own
	private static Emitter.Factory carried( final Class<? extends Gizmo> key, final Emitter.Factory f ){
		return new Emitter.Factory(){
			@Override
			public void emit( Emitter emitter, int index, float x, float y ){
				Emitter host = Fx.lightCarrier( key );
				f.emit( host != null ? host : emitter, index, x, y );
			}

			@Override
			public boolean lightMode(){
				return true;
			}
		};
	}

	//the way out from the body it bursts on: from the hero's middle when it is his (a shout's
	//burst is), else a way of its own
	private static float outward( float x, float y ){
		CharSprite s = Dungeon.hero != null ? Dungeon.hero.sprite : null;
		if (s != null){
			float dx = x - SkillVfx.midX( s ), dy = y - SkillVfx.midY( s );
			if (Math.abs( dx ) < 12 && Math.abs( dy ) < 14 && dx * dx + dy * dy > 0.25f) return (float) Math.atan2( dy, dx );
		}
		return FxRandom.angle();
	}

	//the light layer's carriers: a key each
	private static final class TrembleCarried extends Gizmo {}
	private static final class RoarCarried extends Gizmo {}
	private static final class MetalCarried extends Gizmo {}
	private static final class SteelCarried extends Gizmo {}
	private static final class ArcaneCarried extends Gizmo {}
	private static final class StormCarried extends Gizmo {}
	private static final class EmberCarried extends Gizmo {}
	private static final class FrostCarried extends Gizmo {}
	private static final class FlourishCarried extends Gizmo {}
	private static final class GoldCarried extends Gizmo {}
	private static final class VerseCarried extends Gizmo {}
	private static final class SoulCarried extends Gizmo {}
	private static final class HolyCarried extends Gizmo {}
	private static final class BreathCarried extends Gizmo {}
	private static final class HeartCarried extends Gizmo {}
	private static final class FuseCarried extends Gizmo {}
	private static final class ScentCarried extends Gizmo {}
	private static final class DazeCarried extends Gizmo {}
}

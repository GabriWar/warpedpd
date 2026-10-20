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

package xyz.gabriwar.warpedpixeldungeon.journal.guidedemos;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Barrier;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Drowsy;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Terror;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.AlbinoPiranha;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DemonLord;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKing;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfKingTomb;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.DwarfLich;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Eye;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Ghoul;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.GraveProtector;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceGuardian;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.IceGuardianCore;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Monk;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.MossySkeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Oni;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogDzewa;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.YogFist;
import xyz.gabriwar.warpedpixeldungeon.effects.Effects;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.DumplingBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.FishingBomb;
import xyz.gabriwar.warpedpixeldungeon.items.bombs.HolyHandGrenade;
import xyz.gabriwar.warpedpixeldungeon.journal.guidecites.GuideEffect;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.GuidePuppet;
import com.watabou.noosa.MovieClip;

//Demos for the last regions: the dwarven city, the demon halls and their frozen halls, and the postgame.
//Words: messages/guide/demos_late.properties.
public final class LateDemos {

	private LateDemos(){}

	public static void register( DemoBook book ){
		ghoulPair( book );
		monkFocus( book );
		kingShield( book );
		tombLiches( book );
		eyeBeam( book );
		yogBeams( book );
		yogFist( book );
		iceCore( book );
		demonLordBreath( book );
		holyGrenade( book );
		fishingBomb( book );
		dumplingBomb( book );
	}

	//a downed ghoul lies crumpled (GhoulSprite.crumple, held on its last frame) until it gets up
	private static final Demo.Look CRUMPLED = ( s, on ) -> {
		MovieClip.Animation a = GuidePuppet.named( s, "crumple" );
		if (on && a != null){
			s.play( a, true );
			s.frame( a.frames[a.frames.length - 1] );
			s.paused = true;
		} else {
			s.paused = false;
			GuidePuppet.play( s, GuidePuppet.Slot.IDLE );
		}
	};

	//A ghoul killed while another can see it or stands within 4 tiles only collapses (Ghoul.die:
	//GhoulLifeLink on the other), and gets up timesDowned*5 turns later, 5 the first time, with a
	//tenth of its 45 health: 5 (GhoulLifeLink.act). Ghouls hit for 16 to 22.
	private static void ghoulPair( DemoBook book ){
		book.demo( "city_f1" ).under( "come in pairs" )
				.stage( Demo.Region.CITY,
						"#######",
						"......,",
						",......",
						"......." )
				.mob( "a", Ghoul.class, 3, 2 )
				.mob( "b", Ghoul.class, 4, 1 )
				.hero( "you", 2, 2 )
				.face( "a", 2, 2 )
				.caption( "pair" )
				.turn()
				.attack( "you", 3, 2 ).beat().hurt( "a", 24 )
				.look( "a", CRUMPLED, true )
				.caption( "down" )
				.turn()
				.move( "b", 3, 1 )
				.turn()
				.attack( "b", 2, 2 ).beat().hurt( "you", 19 )
				.turn()
				.attack( "you", 3, 1 ).beat().hurt( "b", 22 )
				.turn()
				.attack( "b", 2, 2 ).beat().hurt( "you", 17 )
				.turn()
				//5 turns after it fell
				.look( "a", CRUMPLED, false ).heal( "a", 5 )
				.caption( "rise" )
				.turn()
				.caption( "apart" )
				.turns( 2 );
	}

	//A hunting monk gains Focus (Monk.act), which makes it dodge anything (INFINITE_EVASION) until it
	//is spent on a parry (Monk.defenseVerb: "parried", Focus gone, 6 to 7 turns to focus again). It
	//strikes twice a turn (attackDelay x0.5) for 22 to 36.
	private static void monkFocus( DemoBook book ){
		book.demo( "city_f2" ).under( "builds focus and parries" )
				.stage( Demo.Region.CITY,
						"######",
						"......",
						"......",
						"..,..." )
				.mob( "monk", Monk.class, 3, 2 )
				.hero( "you", 2, 2 )
				.face( "monk", 2, 2 )
				.buff( "monk", Monk.Focus.class )
				.caption( "focus" )
				.turn()
				//the cheap blow spends the parry
				.attack( "you", 3, 2 ).beat()
				.chip( "monk", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.PARRY ).clear( "monk" )
				.caption( "spend" )
				.beat()
				.attack( "monk", 2, 2 ).beat().hurt( "you", 26 )
				.attack( "monk", 2, 2 ).beat().hurt( "you", 31 )
				.turn()
				.attack( "you", 3, 2 ).beat().hurt( "monk", 38 )
				.caption( "hard" )
				.turns( 2 );
	}

	//At 50 health the king takes the throne for phase 2 (DwarfKing.damage): invulnerable to all but
	//his servants' deaths, which show IMMUNE on his card (Char.attack). Each servant summoned in
	//phase 2 carries KingDamager, which takes HT/12 = 25 off his 300 shield when it dies for good
	//(a ghoul revived keeps it: revivePersists), so 12 deaths end the phase.
	private static void kingShield( DemoBook book ){
		book.demo( "boss_dwarfking" ).under( "a 300-point shield nothing breaks" )
				.stage( Demo.Region.CITY,
						"#######",
						".P...P.",
						"..._...",
						".P...P.",
						"......." )
				.mob( "king", DwarfKing.class, 3, 2 )
				.mob( "ghoul", Ghoul.class, 1, 3 )
				.hero( "you", 3, 3 )
				.face( "you", 3, 2 )
				.buff( "king", Barrier.class )
				.hide( "ghoul" )
				.caption( "shield" )
				.turn()
				.attack( "you", 3, 2 ).beat()
				.chip( "king", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.IMMUNE )
				.turn()
				//a servant comes up on a pedestal
				.show( "ghoul" )
				.turn()
				.move( "ghoul", 2, 3 )
				.turn()
				.attack( "ghoul", 3, 3 ).beat().hurt( "you", 19 )
				.turn()
				.attack( "you", 2, 3 ).beat().hurt( "ghoul", 45 ).die( "ghoul" )
				.beat()
				.status( "king", "minus", CharSprite.WARNING )
				.caption( "kill" )
				.turns( 2 )
				.caption( "twelve" )
				.turns( 2 );
	}

	//The tomb stands two tiles north of the throne (CityBossLevel.tombPos). When the king dies a lich
	//comes out on each open side of it (CityBossLevel.unseal, DwarfLich.spawnAround: NEIGHBOURS4),
	//here three, the north being wall, and acts 2 turns later (SPAWN_DELAY). A lich shoots, for 20 to
	//32, only along a clear line (Ballistica PROJECTILE, stopped by creatures) and never at a
	//neighbour (DwarfLich.canAttack). From (2, 1) and (4, 1) the lines to the throne at (3, 3) run
	//through (3, 2), so with the hero there the lich standing on (3, 2) blocks both.
	private static void tombLiches( DemoBook book ){
		book.demo( "boss_dwarftomb" ).under( "a dwarf lich steps out" )
				.stage( Demo.Region.CITY,
						"#######",
						"......,",
						".......",
						"..._...",
						",......" )
				.mob( "tomb", DwarfKingTomb.class, 3, 1 )
				.mob( "king", DwarfKing.class, 3, 3 )
				.mob( "w", DwarfLich.class, 2, 1 )
				.mob( "e", DwarfLich.class, 4, 1 )
				.mob( "s", DwarfLich.class, 3, 2 )
				.hero( "you", 3, 4 )
				.face( "you", 3, 3 )
				.hide( "w" ).hide( "e" ).hide( "s" )
				.turn()
				.attack( "you", 3, 3 ).beat().hurt( "king", 24 ).die( "king" )
				.beat()
				.show( "w" ).show( "e" ).show( "s" )
				.caption( "rise" )
				.turns( 2 )
				//no bolt flies: bones and rocks rain on where it aims (DwarfLichSprite.attack)
				.attack( "s", 3, 4 ).burst( GuideEffect.BONES, 3, 4 ).burst( GuideEffect.ROCKS, 3, 4 )
				.beat().hurt( "you", 26 )
				.attack( "e", 3, 4 ).burst( GuideEffect.BONES, 3, 4 ).burst( GuideEffect.ROCKS, 3, 4 )
				.beat().hurt( "you", 21 )
				.attack( "w", 3, 4 ).burst( GuideEffect.BONES, 3, 4 ).burst( GuideEffect.ROCKS, 3, 4 )
				.beat().hurt( "you", 30 )
				.caption( "bolts" )
				.turn()
				//next to the one on (3, 2), which blocks the others' lines: none of them can shoot
				.move( "you", 3, 3 )
				.caption( "close" )
				.turn()
				.attack( "you", 3, 2 ).beat().hurt( "s", 31 )
				.turns( 2 );
	}

	//An evil eye that sees you on its line charges (EyeSprite.charging) for 2 turns (Eye.doAttack:
	//attackDelay*2), then fires at where you stood: the beam runs on to the first wall (Ballistica
	//STOP_SOLID) and hits everything on it for 30 to 50. From (1, 2) at (4, 2), it ends on the wall
	//at (6, 2). While charging it takes a quarter damage (Eye.damage), so hit it once it has fired.
	private static final int[] EYE_LINE = { 2,2, 3,2, 4,2, 5,2 };

	private static void eyeBeam( DemoBook book ){
		book.demo( "halls_f1" ).under( "charge for 2 turns" )
				.stage( Demo.Region.HALLS,
						"#######",
						"......,",
						"......#",
						".,....." )
				.mob( "eye", Eye.class, 1, 2 )
				.hero( "you", 4, 2 )
				.face( "you", 1, 2 )
				.turn()
				.anim( "eye", "charging" )
				.arrow( Demo.Mark.DANGER, 1, 2, 6, 2 )
				.caption( "charge" )
				.turn()
				.move( "you", 4, 1 )
				.turn()
				.move( "you", 3, 1 )
				//the beam (EyeSprite: a death ray to where it ends), the game's purple bursts along it, as on
				//what it strikes (Eye.deathGaze)
				.zap( "eye", 6, 2 ).ray( "eye", Effects.Type.DEATH_RAY, 6, 2 )
				.unarrow().burst( GuideEffect.ARCANE, EYE_LINE )
				.caption( "fire" )
				.turn()
				.move( "you", 2, 1 )
				.turn()
				.attack( "you", 1, 2 ).beat().hurt( "eye", 30 )
				.caption( "hit" )
				.turns( 2 );
	}

	//Yog's beams (YogDzewa.act): 1 + (1000 - health)/400 of them, the first at you, the rest at
	//cells next to you no nearer to Yog. Every cell of each beam's line (Ballistica WONT_STOP) is
	//marked, and a turn later they fire through everything, hitting for 20 to 30 and never missing.
	//From (3, 1): at (3, 4) the line is column 3; at (2, 4) it bends through (2, 3); the extra beam
	//at (3, 5), as far as you are, is column 3 again, so (1, 4) beside you stays clear.
	private static final int[] YOG_AT_3_4 = { 3,1, 3,2, 3,3, 3,4, 3,5 };
	private static final int[] YOG_AT_2_4 = { 3,1, 3,2, 2,3, 2,4, 2,5 };

	private static void yogBeams( DemoBook book ){
		book.demo( "boss_yog" ).under( "the target cells light up" )
				.stage( Demo.Region.HALLS,
						"#######",
						"......,",
						".......",
						".,.....",
						".......",
						"......." )
				.mob( "yog", YogDzewa.class, 3, 1 )
				.hero( "you", 3, 4 )
				.turn()
				.mark( Demo.Mark.TARGET, YOG_AT_3_4 )
				.caption( "light" )
				.beat()
				.move( "you", 2, 4 )
				.turn()
				//the beams (YogDzewa.act: a death ray to each line's end), the game's purple bursts along them
				.unmark().ray( "yog", Effects.Type.DEATH_RAY, 3, 5 ).burst( GuideEffect.ARCANE, YOG_AT_3_4 )
				.caption( "fire" )
				.turns( 2 )
				//weaker now: two beams, one at you, one beside you
				.mark( Demo.Mark.TARGET, YOG_AT_2_4 ).mark( Demo.Mark.TARGET, YOG_AT_3_4 )
				.mark( Demo.Mark.SAFE, 1, 4 )
				.caption( "more" )
				.beat()
				.move( "you", 1, 4 )
				.turn()
				.unmark().ray( "yog", Effects.Type.DEATH_RAY, 2, 5 ).ray( "yog", Effects.Type.DEATH_RAY, 3, 5 )
				.burst( GuideEffect.ARCANE, YOG_AT_2_4 ).burst( GuideEffect.ARCANE, YOG_AT_3_4 )
				.turns( 2 );
	}

	//A fist within 4 tiles of Yog is invulnerable (YogFist.isNearYog, isInvulnerable): blows on it
	//show IMMUNE on their card. Five tiles off it can be hurt. Fists hit for 18 to 36.
	private static final int[] NEAR_YOG = { 1,1, 2,1, 3,1, 4,1, 0,2, 1,2, 2,2, 3,2, 4,2 };

	private static void yogFist( DemoBook book ){
		book.demo( "boss_yog" ).under( "can't be hurt within 4 tiles of Yog" )
				.stage( Demo.Region.HALLS,
						"#######",
						".......",
						"....,.." )
				.mob( "yog", YogDzewa.class, 0, 1 )
				.mob( "fist", YogFist.BrightFist.class, 2, 1 )
				.hero( "you", 3, 1 )
				.face( "you", 2, 1 )
				.mark( Demo.Mark.HINT, NEAR_YOG )
				.caption( "near" )
				.turn()
				.attack( "you", 2, 1 ).beat()
				.chip( "fist", xyz.gabriwar.warpedpixeldungeon.effects.FloatingText.IMMUNE )
				.turn()
				.move( "you", 4, 1 ).move( "fist", 3, 1 )
				.turn()
				.move( "you", 5, 1 ).move( "fist", 4, 1 )
				.turn()
				.move( "you", 6, 1 ).move( "fist", 5, 1 )
				.turn()
				.attack( "fist", 6, 1 ).beat().hurt( "you", 27 )
				.turn()
				.face( "you", 5, 1 )
				.attack( "you", 5, 1 ).beat().hurt( "fist", 24 )
				.caption( "away" )
				.turns( 2 );
	}

	//The ice guardian core (1,000 health) keeps up to four guardians. A guardian that breaks takes
	//150 off the core (IceGuardian.CORE_DAMAGE_ON_DEATH) and another forms, with a flash of light, on
	//the first free cell round where it fell (resurrect: NEIGHBOURS8 from the top left). Guardians hit
	//for 10 to 15 and have 70 health.
	private static void iceCore( DemoBook book ){
		book.demo( "frozen_overview" ).under( "four guardians that re-form when broken" )
				.stage( Demo.Region.FROZEN,
						"#######",
						"......,",
						".......",
						".,.....",
						"......." )
				.mob( "core", IceGuardianCore.class, 3, 1 )
				.mob( "g1", IceGuardian.class, 3, 3 )
				.mob( "g2", IceGuardian.class, 1, 2 )
				.mob( "g3", IceGuardian.class, 5, 2 )
				.mob( "g4", IceGuardian.class, 4, 1 )
				.mob( "g5", IceGuardian.class, 2, 2 )
				.hero( "you", 3, 4 )
				.face( "you", 3, 3 )
				.hide( "g5" )
				.caption( "core" )
				.turn()
				.attack( "you", 3, 3 ).beat().hurt( "g1", 36 )
				.turn()
				.attack( "g1", 3, 4 ).beat().hurt( "you", 12 )
				.turn()
				.attack( "you", 3, 3 ).beat().hurt( "g1", 38 ).die( "g1" )
				.beat()
				.hurt( "core", 150 )
				.show( "g5" ).burst( GuideEffect.LIGHT, 2, 2 )
				.caption( "break" )
				.turns( 3 );
	}

	//Within 4 tiles and in his sight the demon lord may inhale (DemonLord.act): "!!!", the cone he
	//aims at you (ConeAOE, 4 tiles, 70 degrees, stopped by solid ice) is marked and frosts over, the
	//count shows 2, then 1, and on the third turn he exhales: 20 to 40 and chill on everything in it
	//(DemonLord.breathe; DemonLordSprite: snow and light on every cell). The cone stays where it
	//was aimed. From (3, 1) at (3, 4), with an ice pillar at (4, 3), the cone is these cells; the
	//pillar's shadow (4, 4) (4, 5) (5, 4) (5, 5) is out of it:
	private static final int[] DL_CONE = { 2,2, 3,2, 4,2, 1,3, 2,3, 3,3, 5,3, 1,4, 2,4, 3,4, 1,5, 2,5, 3,5 };

	private static void demonLordBreath( DemoBook book ){
		book.demo( "boss_demonlord" ).under( "he may inhale" )
				.stage( Demo.Region.FROZEN,
						"#######",
						"......,",
						".......",
						"....%..",
						".,.....",
						"......." )
				.mob( "dl", DemonLord.class, 3, 1 )
				.hero( "you", 3, 4 )
				.face( "dl", 3, 4 )
				.caption( "near" )
				.turn()
				.anim( "dl", "inhale" ).status( "dl", "actors.mobs.demonlord.!!!", CharSprite.WARNING )
				.mark( Demo.Mark.TARGET, DL_CONE ).pour( GuideEffect.BLIZZARD, DL_CONE )
				.caption( "cone" )
				.turn()
				.status( "dl", "two", CharSprite.WARNING )
				.beat()
				.move( "you", 4, 4 )
				.caption( "hide" )
				.turn()
				.status( "dl", "one", CharSprite.WARNING )
				.turn()
				.anim( "dl", "breath" )
				.stop().unmark()
				.burst( GuideEffect.BLIZZARD, DL_CONE ).burst( GuideEffect.LIGHT, DL_CONE )
				.caption( "breath" )
				.turns( 2 );
	}

	//A thrown bomb goes off 2 turns after it lands (Bomb.onThrow). A holy hand grenade hurts every
	//undead within 2 tiles (5x5) for 75 to 155 and terrifies it, and nothing else, not even you
	//(HolyHandGrenade.explode). Moss covered skeletons have 105 to 135 health, hit for 20 to 45 and
	//burst when they die, hurting each neighbour for 3 to 8 (MossySkeleton.die). The grave
	//protector is not undead.
	private static final int[] GRENADE_AREA = {
			1,2, 2,2, 3,2, 4,2, 5,2,
			1,3, 2,3, 3,3, 4,3, 5,3,
			1,4, 2,4, 3,4, 4,4, 5,4,
			1,5, 2,5, 3,5, 4,5, 5,5 };

	private static void holyGrenade( DemoBook book ){
		book.demo( "pg_battle" ).under( "throw one at your own feet" )
				.stage( Demo.Region.PRISON,
						"#######",
						"......,",
						".......",
						".......",
						".,.....",
						"......." )
				.mob( "s1", MossySkeleton.class, 2, 3 )
				.mob( "s2", MossySkeleton.class, 4, 3 )
				.mob( "s3", MossySkeleton.class, 3, 2 )
				.mob( "gp", GraveProtector.class, 5, 2 )
				.hero( "you", 3, 3 )
				.item( "nade", HolyHandGrenade.class, 3, 4 )
				.hide( "nade" )
				.caption( "feet" )
				.turn()
				.zap( "you", 3, 4 ).toss( "you", HolyHandGrenade.class, 3, 4 )
				.beat()
				.show( "nade" )
				.attack( "s1", 3, 3 ).beat().hurt( "you", 31 )
				.turn()
				.attack( "s2", 3, 3 ).beat().hurt( "you", 28 )
				.turn()
				.hide( "nade" ).burst( GuideEffect.SMOKE, GRENADE_AREA )
				.hurt( "s1", 118 ).hurt( "s2", 142 ).hurt( "s3", 97 )
				.buff( "s1", Terror.class ).buff( "s3", Terror.class )
				.die( "s2" ).burst( GuideEffect.BONES, 4, 3 )
				.caption( "blast" )
				.beat()
				//the fallen one bursts on its neighbours
				.hurt( "you", 4 ).hurt( "s3", 5 )
				.turn()
				.move( "s1", 1, 3 ).move( "s3", 3, 1 )
				.caption( "living" )
				.turns( 2 );
	}

	//A fishing bomb goes off 2 turns after it lands (Bomb.onThrow) and does no damage: every albino
	//piranha within 2 tiles (5x5) is put down on a dry cell of the floor (FishingBomb.explode), where
	//it dies on its next turn (AlbinoPiranha.act: out of water).
	private static final int[] FISHING_AREA = {
			1,0, 2,0, 3,0, 4,0, 5,0,
			1,1, 2,1, 3,1, 4,1, 5,1,
			1,2, 2,2, 3,2, 4,2, 5,2,
			1,3, 2,3, 3,3, 4,3, 5,3,
			1,4, 2,4, 3,4, 4,4, 5,4 };

	private static void fishingBomb( DemoBook book ){
		book.demo( "pg_fishing" ).under( "A fishing bomb deals no damage" )
				.stage( Demo.Region.SEWERS,
						"~~~~~~~",
						"~~~~~~~",
						"~~~~~~~",
						"~~~~~~~",
						"......,",
						".,....." )
				.mob( "p1", AlbinoPiranha.class, 2, 2 )
				.mob( "p2", AlbinoPiranha.class, 4, 1 )
				.mob( "p3", AlbinoPiranha.class, 4, 3 )
				.mob( "p4", AlbinoPiranha.class, 6, 2 )
				.mob( "b1", AlbinoPiranha.class, 0, 5 )
				.mob( "b2", AlbinoPiranha.class, 6, 5 )
				.mob( "b3", AlbinoPiranha.class, 5, 4 )
				.hero( "you", 3, 5 )
				.item( "bomb", FishingBomb.class, 3, 2 )
				.hide( "b1" ).hide( "b2" ).hide( "b3" ).hide( "bomb" )
				.caption( "throw" )
				.turn()
				.zap( "you", 3, 2 ).toss( "you", FishingBomb.class, 3, 2 )
				.move( "p3", 3, 3 )
				.beat()
				.show( "bomb" )
				.turn()
				.move( "p1", 2, 3 ).move( "p4", 6, 1 )
				.turn()
				.hide( "bomb" ).burst( GuideEffect.SMOKE, FISHING_AREA )
				.hide( "p1" ).hide( "p2" ).hide( "p3" )
				.show( "b1" ).show( "b2" ).show( "b3" )
				.caption( "beach" )
				.turn()
				.die( "b1" ).die( "b2" ).die( "b3" )
				.caption( "die" )
				.turns( 2 );
	}

	//A dumpling bomb goes off 2 turns after it lands (Bomb.onThrow): everything in the 3x3 round it
	//that is no boss, NPC or undead grows drowsy (with the game's notes), and those still at full
	//health are sent to another cell of the floor (DumplingBomb.explode). Oni have 520 health.
	private static final int[] DUMPLING_AREA = { 2,1, 3,1, 4,1, 2,2, 3,2, 4,2, 2,3, 3,3, 4,3 };

	private static void dumplingBomb( DemoBook book ){
		book.demo( "pg_fortress" ).under( "dumpling bombs" )
				.stage( Demo.Region.CITY,
						"#######",
						"......,",
						".......",
						".,.....",
						"......." )
				.mob( "a", Oni.class, 2, 1 )
				.mob( "b", Oni.class, 4, 1 )
				.hero( "you", 3, 4 )
				.item( "bomb", DumplingBomb.class, 3, 2 )
				.hide( "bomb" )
				.caption( "throw" )
				.turn()
				.zap( "you", 3, 2 ).toss( "you", DumplingBomb.class, 3, 2 )
				.move( "a", 2, 2 ).move( "b", 4, 2 )
				.beat()
				.show( "bomb" )
				.turn()
				.move( "b", 4, 3 )
				.beat()
				.attack( "you", 4, 3 ).beat().hurt( "b", 64 )
				.turn()
				.hide( "bomb" ).burst( GuideEffect.SMOKE, DUMPLING_AREA )
				.buff( "a", Drowsy.class ).buff( "b", Drowsy.class )
				.burst( GuideEffect.NOTES, 2, 2, 4, 3 )
				.beat()
				.hide( "a" )
				.caption( "away" )
				.turns( 2 );
	}
}

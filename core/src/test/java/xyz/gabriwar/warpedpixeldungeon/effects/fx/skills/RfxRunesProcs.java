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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.skills;

import xyz.gabriwar.warpedpixeldungeon.actors.buffs.ManaShieldWard;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Aegis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ArcaneEcho;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Condemn;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.CurrentSkills;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Hex;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.ManaShield;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sanctuary;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Skill;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.Sorcerer;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpellBlade;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.SpiritArmor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.skills.WeaponBond;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.items.wands.WandOfMagicMissile;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Shortsword;
import xyz.gabriwar.warpedpixeldungeon.levels.rooms.WarpedRoomsTest;
import xyz.gabriwar.warpedpixeldungeon.scenes.CellSelector;
import com.watabou.utils.Random;
import org.junit.Test;

import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The runes package's skills (rfx plan 5.2) played for real on DreadShadowProcs' headless stage, each
 * from a seed through its call and two seconds of frames after it: Spirit Armor raised and its ring
 * filled until the volley fires, Mana Shield raised, a blow held and a blow that breaks it (and the
 * ward reforming, +3), Spell Blade's blows (drinking and overflowing), Arcane Echo's blows with a
 * neighbour to chain to, Sorcerer's zaps and the overload discharging, Hex cast, Condemn's blows,
 * Weapon Bond's kill, Sanctuary raised, Aegis catching a blow. Each one's draws of the game's Random
 * and the next value after them. Written against the API f0783eacd already had, so the same file
 * measures that baseline: with RFX_RUNES_BASELINE set, `baseline` writes the table to
 * build/rfx-runes-baseline.txt (SfxRfxRunesTest holds it and checks the new code against it).
 */
public class RfxRunesProcs {

	static final long SEED = 2028;

	interface Proc {
		Runnable stage( DreadShadowProcs t ) throws Exception;
	}

	static final LinkedHashMap<String, Proc> PROCS = new LinkedHashMap<>();
	static {
		//the ward raised, two blows paid in mana: the ring full, the volley fired at three
		PROCS.put( "spirit-volley", t -> {
			SpiritArmor sa = own( t, HeroClass.MAGE, new SpiritArmor() );
			t.hero.MP = t.hero.MT = 200;
			Rat a = t.rat( t.at( 5, 3 ) );
			t.rat( t.at( 6, 5 ) );
			t.rat( t.at( 4, 6 ) );
			return () -> {
				sa.execute( t.hero, Skill.AC_ACTIVATE );
				sa.incomingDamageReduction( 40, a );
				sa.incomingDamageReduction( 40, a );
			};
		} );
		//raised, a blow held, a blow that breaks it (and it reforms at +3)
		PROCS.put( "mana-shield", t -> {
			ManaShield ms = own( t, HeroClass.MAGE, new ManaShield() );
			t.hero.MP = t.hero.MT = 200;
			Rat a = t.rat( t.at( 4, 4 ) );
			return () -> {
				ms.execute( t.hero, Skill.AC_CAST );
				ManaShieldWard w = t.hero.buff( ManaShieldWard.class );
				ms.incomingDamageReduction( 2, a );
				w.absorbDamage( 2 );
				ms.incomingDamageReduction( 500, a );
				w.absorbDamage( 500 );
			};
		} );
		//six blows drinking mana
		PROCS.put( "spell-blade", t -> {
			SpellBlade sb = own( t, HeroClass.MAGE, new SpellBlade() );
			t.hero.MP = 0;
			t.hero.MT = 100;
			Rat a = t.rat( t.at( 4, 4 ) );
			return () -> { for (int i = 0; i < 6; i++) sb.onHitProc( a, 5, false ); };
		} );
		//six blows with the pool full: the overflow
		PROCS.put( "spell-blade-overflow", t -> {
			SpellBlade sb = own( t, HeroClass.MAGE, new SpellBlade() );
			t.hero.MP = t.hero.MT = 100;
			Rat a = t.rat( t.at( 4, 4 ) );
			return () -> { for (int i = 0; i < 6; i++) sb.onHitProc( a, 5, false ); };
		} );
		//six blows echoing, a neighbour to chain to
		PROCS.put( "arcane-echo", t -> {
			ArcaneEcho ae = own( t, HeroClass.MAGE, new ArcaneEcho() );
			Rat a = t.rat( t.at( 4, 4 ) );
			t.rat( t.at( 5, 4 ) );
			return () -> { for (int i = 0; i < 6; i++) ae.onHitProc( a, 5, false ); };
		} );
		//zaps until an overload is caught (at most twelve), then the overload arming and discharging
		//into its neighbours
		PROCS.put( "sorcerer", t -> {
			own( t, HeroClass.MAGE, new Sorcerer() );
			Rat a = t.rat( t.at( 6, 4 ) );
			t.rat( t.at( 7, 4 ) );
			t.rat( t.at( 6, 5 ) );
			WandOfMagicMissile wand = new WandOfMagicMissile();
			return () -> {
				for (int i = 0; i < 12 && a.buff( Sorcerer.Overload.class ) == null; i++) Sorcerer.onZap( wand, a, a.pos );
				Sorcerer.Overload o = a.buff( Sorcerer.Overload.class );
				if (o != null){
					o.act();
					o.act();
				}
			};
		} );
		//cast on a rat in sight: the bolt flies, the curse lands
		PROCS.put( "hex", t -> {
			Hex h = own( t, HeroClass.MAGE, new Hex() );
			Rat r = t.rat( t.at( 7, 4 ) );
			t.hero.MP = t.hero.MT = 200;
			Class<?> c = Class.forName( Hex.class.getName() + "$Curser" );
			Constructor<?> k = c.getDeclaredConstructor( Hex.class );
			k.setAccessible( true );
			CellSelector.Listener l = (CellSelector.Listener) k.newInstance( h );
			return () -> l.onSelect( r.pos );
		} );
		//six blows judged
		PROCS.put( "condemn", t -> {
			Condemn c = own( t, HeroClass.CLERIC, new Condemn() );
			Rat a = t.rat( t.at( 4, 4 ) );
			return () -> { for (int i = 0; i < 6; i++) c.onHitProc( a, 5, false ); };
		} );
		//a melee kill feeding the blade
		PROCS.put( "weapon-bond", t -> {
			WeaponBond wb = own( t, HeroClass.DUELIST, new WeaponBond() );
			t.hero.belongings.weapon = new Shortsword();
			Rat dead = t.rat( t.at( 4, 4 ) );
			dead.HP = 0;
			return () -> wb.onKill( dead, false );
		} );
		//the ward raised
		PROCS.put( "sanctuary", t -> {
			Sanctuary s = own( t, HeroClass.CLERIC, new Sanctuary() );
			return () -> s.execute( t.hero, Skill.AC_ACTIVATE );
		} );
		//a heavy blow caught and the attacker bashed away
		PROCS.put( "aegis", t -> {
			Aegis a = own( t, HeroClass.CLERIC, new Aegis() );
			Rat r = t.rat( t.at( 4, 4 ) );
			return () -> a.onDefendProc( r, 30 );
		} );
	}

	//the hero of a class holding `s` at its last level, first among his skills (so CurrentSkills
	//and heroSkills.get find this one)
	@SuppressWarnings("unchecked")
	static <S extends Skill> S own( DreadShadowProcs t, HeroClass c, S s ) throws Exception {
		t.hero.heroClass = c;
		CurrentSkills cs = new CurrentSkills( c );
		Field all = CurrentSkills.class.getDeclaredField( "allSkills" );
		all.setAccessible( true );
		s.level = Skill.MAX_LEVEL;
		((List<Skill>) all.get( cs )).add( 0, s );
		t.hero.heroSkills = cs;
		return s;
	}

	/** Each one's draws of the game's Random and the next value after them: { draws, next * 1e6 }. */
	public static LinkedHashMap<String, long[]> measure() throws Exception {
		WarpedRoomsTest.boot();
		Class.forName( "xyz.gabriwar.warpedpixeldungeon.net.NetVisuals", true, RfxRunesProcs.class.getClassLoader() );
		LinkedHashMap<String, long[]> out = new LinkedHashMap<>();
		for (Map.Entry<String, Proc> e : PROCS.entrySet()){
			DreadShadowProcs t = new DreadShadowProcs();
			t.up();
			try {
				Runnable run = e.getValue().stage( t );
				Random.pushGenerator( SEED );
				run.run();
				t.rig.frames( 120, true );
				float next = Random.Float();
				Random.popGenerator();
				out.put( e.getKey(), new long[]{ count( next ), Math.round( next * 1e6 ) } );
			} finally {
				t.down();
			}
		}
		return out;
	}

	//how many draws from SEED come before `next`
	private static int count( float next ){
		Random.pushGenerator( SEED );
		try {
			for (int k = 0; k < 8192; k++){
				if (Random.Float() == next) return k;
			}
			return -1;
		} finally {
			Random.popGenerator();
		}
	}

	@Test
	public void baseline() throws Exception {
		if (System.getenv( "RFX_RUNES_BASELINE" ) == null) return;
		new File( "build" ).mkdirs();
		try (Writer w = new FileWriter( new File( "build/rfx-runes-baseline.txt" ) )){
			for (Map.Entry<String, long[]> e : measure().entrySet()){
				w.write( "{ \"" + e.getKey() + "\", " + e.getValue()[0] + ", " + e.getValue()[1] + " },\n" );
			}
		}
	}
}

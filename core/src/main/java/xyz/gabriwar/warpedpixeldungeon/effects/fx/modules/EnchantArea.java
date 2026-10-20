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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.modules;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.ClimateManager;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.DayNightCycle;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Buff;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Skeleton;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Sentinel;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Statue;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModule;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.FxModules;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.ItemGhost;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantState;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.Family;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ElementExhibits;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.HeapLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.ItemExhibits;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.WieldLook;
import xyz.gabriwar.warpedpixeldungeon.items.Heap;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.Armor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.PlateArmor;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.SlayerFamily;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flail;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Flute;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.HandAxe;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Harp;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Triangolo;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Trumpet;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.WarHammer;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Wardrum;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Whip;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.bow.Bow;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.noosa.Game;
import com.watabou.utils.PointF;
import com.watabou.utils.Reflection;

import java.util.List;

/**
 * Enchantments and glyphs (docs/fx/enchant.md): how an enchanted weapon's proc, a glyph's answer,
 * a curse's hex and a vestige's echo look, the hit taking the enchantment's colour, the swing's
 * trail, the stunned, charmed, seeded and surging states. The area's hooks: a new scene clears
 * what the enchantments count (EnchantFx.reset, ItemGhost.reset); every drawn sprite of a hero or a
 * statue has its weapon looked at again now and then (EnchantState, its poll); a killed sprite's
 * state is let go of.
 *
 * Each later enchantment area adds one line to each hook it needs, calling its own class: its
 * exhibits (exhibits), its debug scenes (scenes), what it steps after a sprite draws (afterDraw),
 * on a sprite's step (stepped), and what it lets go of with a killed sprite (gone).
 *
 * Its gallery page restages every look by hand through EnchantFx's stage entries on puppets, never
 * a real proc; its scenes, fx-enchant-arena (by day) and fx-enchant-arena-night, are a room to swing
 * at hardy dummies with a weapon for every enchantment and an armour for every glyph.
 */
public class EnchantArea implements FxModule {

	@Override
	public String key(){
		return "enchant";
	}

	@Override
	public String title(){
		return "Enchantments & glyphs";
	}

	@Override
	public void sceneCreated( GameScene scene ){
		EnchantFx.reset();
		ItemGhost.reset();
		HeapLook.reset();
	}

	@Override
	public void afterDraw( CharSprite s ){
		if (s.ch instanceof Hero || s.ch instanceof Statue || s.ch instanceof Sentinel) EnchantState.of( s ).poll( s.ch );
		WieldLook.step( s );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.SenseFx.step( s );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.LineLooks.step( s );
	}

	@Override
	public void stepped( CharSprite s, int from, int to ){
		WieldLook.stepped( s );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.LineLooks.stepped( s, from, to );
	}

	@Override
	public void gone( CharSprite s ){
		WieldLook.gone( s );
		s.fxSlots[FxModules.ENCHANT] = null;
	}

	@Override
	public void exhibits( FxGallery.Page page ){
		coreExhibits( page );
		ElementExhibits.exhibits( page );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows.ShadowExhibits.add( page );
		ItemExhibits.exhibits( page );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.GainExhibits.exhibits( page );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.PieceExhibits.exhibits( page );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.BlowExhibits.exhibits( page );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.CalmExhibits.exhibits( page );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.TypeExhibits.exhibits( page );
		xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.LineExhibits.exhibits( page );
	}

	@Override
	public void scenes( List<DebugScenes.Scene> out ){
		out.add( new Arena( false ) );
		out.add( new Arena( true ) );
		ItemExhibits.scenes( out );
		out.add( new xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.GainExhibits.Pedestal() );
	}

	// ------------------------------------------------------------------ the core's exhibits

	static final String[] TIERS = { "weak", "middling", "strong", "the killing blow" };

	//the first look of each family, by code: the gallery's face of it
	static EnchantLook face( Family f ){
		for (EnchantLook l : EnchantLooks.all()) if (l.fam == f && !l.curse) return l;
		return null;
	}

	static EnchantLook look( Class<?> c ){
		return EnchantLooks.of( c );
	}

	//a striker beside the rat, facing it
	static FxStage.Puppet striker( FxStage s ){
		return s.puppet( Gnoll.class, s.cell( -3, 2 ) );
	}

	//a proc restaged on the rat from a striker, its killing blow ending as one ends
	static void hit( FxStage s, FxStage.Puppet p, EnchantLook l, int tier, int flags ){
		if (p == null || s.dryRat == null || s.dryRat.sprite == null || l == null) return;
		CharSprite rat = s.dryRat.sprite;
		p.sprite.attack( s.dryRat.pos );
		EnchantFx.stage( l, p.sprite, rat, tier, 0, flags );
		if (tier == EnchantFx.FINISHER) EnchantFx.stageEnded( p.sprite, rat );
		else HitFx.hit( rat, p.sprite.x + p.sprite.width() / 2f, p.sprite.y + p.sprite.height() / 2f, 3, HitFx.SLASH, false );
	}

	static void coreExhibits( FxGallery.Page page ){
		final Family[] fams = Family.values();
		page.add( "weak, strong and the killing blow, family by family", fams.length * 4 * 0.75f + 0.5f, s -> {
			final FxStage.Puppet p = striker( s );
			for (int f = 0; f < fams.length; f++){
				for (int t = 0; t < 4; t++){
					final EnchantLook l = face( fams[f] );
					final int tier = t;
					s.after( 0.3f + (f * 4 + t) * 0.75f, () -> {
						GLog.i( "  " + l.of.getSimpleName() + " (" + l.fam + "): " + TIERS[tier] );
						hit( s, p, l, tier, 0 );
					} );
				}
			}
		} );

		page.add( "the hit takes the enchantment's colour", 6.5f, s -> {
			final FxStage.Puppet p = striker( s );
			final FxStage.Puppet far = s.puppet( Gnoll.class, s.cell( -4, 2 ) );
			final EnchantLook blazing = look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class );
			final EnchantLook chilling = look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling.class );
			final EnchantLook projecting = look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting.class );
			s.after( 0.3f, () -> {
				GLog.i( "  a plain blow: the star white to the blood" );
				strike( s, p, null );
			} );
			s.after( 1.2f, () -> {
				GLog.i( "  a blow its enchantment stamped: the star, bloom and crescent in the element" );
				if (s.dryRat.sprite == null) return;
				s.dryRat.sprite.blowLook = blazing;
				s.dryRat.sprite.blowUntil = Game.timeTotal + EnchantFx.STAMP_FOR;
				strike( s, p, null );
			} );
			s.after( 2.1f, () -> {
				GLog.i( "  an enchanted weapon's blow that procs nothing: drawn a third toward its look, and its trail" );
				strike( s, p, blazing );
			} );
			s.after( 3.0f, () -> {
				GLog.i( "  a miss: the trail fading past the foe" );
				if (s.dryRat.sprite == null) return;
				p.sprite.attack( s.dryRat.pos );
				EnchantFx.stageSwing( chilling, p.sprite, s.dryRat.sprite, false, 0, 1, false );
				HitFx.dodged( s.dryRat.sprite, p.sprite.x + p.sprite.width() / 2f, p.sprite.y + p.sprite.height() / 2f );
			} );
			s.after( 3.9f, () -> {
				GLog.i( "  a reach blow: a thrust across the gap" );
				if (s.dryRat.sprite == null || far == null) return;
				far.sprite.attack( s.dryRat.pos );
				EnchantFx.stageSwing( projecting, far.sprite, s.dryRat.sprite, true, 0, 2, false );
			} );
			s.after( 4.8f, () -> {
				GLog.i( "  under Wayward's curse the trail curves away" );
				if (s.dryRat.sprite == null) return;
				p.sprite.attack( s.dryRat.pos );
				EnchantFx.stageSwing( blazing, p.sprite, s.dryRat.sprite, true, 2, 1, true );
			} );
		} );

		final Class<?>[] echoes = {
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky.class,
				xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch.class };
		page.add( "the echo beside the real thing", echoes.length * 1.6f + 0.5f, s -> {
			final FxStage.Puppet p = striker( s );
			for (int i = 0; i < echoes.length; i++){
				final EnchantLook l = look( echoes[i] );
				s.after( 0.3f + 1.6f * i, () -> {
					GLog.i( "  " + l.of.getSimpleName() + ", then its echo" );
					hit( s, p, l, EnchantFx.MID, 0 );
				} );
				s.after( 1.1f + 1.6f * i, () -> hit( s, p, l, EnchantFx.MID, EnchantFx.ECHO ) );
			}
		} );

		page.add( "two enchantments and an echo on one blow", 4.5f, s -> {
			final FxStage.Puppet p = striker( s );
			s.after( 0.3f, () -> {
				GLog.i( "  Blazing and Shocking on one blow, and an echo of Chilling" );
				hit( s, p, look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class ), EnchantFx.MID, 0 );
				hit( s, p, look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking.class ), EnchantFx.MID, 0 );
				hit( s, p, look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling.class ), EnchantFx.MID, EnchantFx.ECHO );
			} );
			s.after( 2.2f, () -> {
				GLog.i( "  Vampiric, then Grim killing" );
				hit( s, p, look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric.class ), EnchantFx.STRONG, 0 );
				hit( s, p, look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim.class ), EnchantFx.FINISHER, 0 );
			} );
		} );

		page.add( "gathering and finale, family by family", fams.length * 1.9f + 0.5f, s -> {
			for (int f = 0; f < fams.length; f++){
				final EnchantLook l = face( fams[f] );
				s.after( 0.3f + 1.9f * f, () -> {
					GLog.i( "  " + l.fam + ": gathering, then its finale on the skeleton" );
					PointF at = FxStage.center( s.cell( -1, -2 ) );
					EnchantFx.gather( l, at.x, at.y - 4f, 1f );
				} );
				s.after( 1.4f + 1.9f * f, () -> {
					if (s.skeleton.sprite == null) return;
					CharSprite k = s.skeleton.sprite;
					EnchantFx.finale( l, k.x + k.width() / 2f, k.y + k.height() / 2f, 0.5f );
				} );
			}
		} );

		page.add( "stunned, charmed, seeded and surging", 5f, s -> {
			//a real stun (a Paralysis buff): a bare PARALYSED is a time freeze's pause, starless
			s.after( 0.2f, () -> {
				if (s.dryRat != null && s.dryRat.sprite != null) Buff.affect( s.dryRat, Paralysis.class, Paralysis.DURATION );
				state( s.skeleton, CharSprite.State.HEARTS, true );
				state( s.wetRat, CharSprite.State.SEEDED, true );
				state( s.bat, CharSprite.State.SURGING, true );
			} );
			s.after( 4.2f, () -> {
				if (s.dryRat != null) Buff.detach( s.dryRat, Paralysis.class );
				state( s.skeleton, CharSprite.State.HEARTS, false );
				state( s.wetRat, CharSprite.State.SEEDED, false );
				state( s.bat, CharSprite.State.SURGING, false );
			} );
		} );

		page.add( "a curse's hex", 5f, s -> {
			final FxStage.Puppet p = striker( s );
			final Class<?>[] curses = {
					xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing.class,
					xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial.class };
			for (int i = 0; i < curses.length; i++){
				final EnchantLook l = look( curses[i] );
				s.after( 0.3f + 1.2f * i, () -> {
					GLog.i( "  " + l.of.getSimpleName() + " (" + l.fam + ", hexed)" );
					hit( s, p, l, EnchantFx.MID, EnchantFx.CURSE );
				} );
			}
			s.after( 3.9f, () -> {
				GLog.i( "  Corrosion on the skeleton's armour" );
				if (s.skeleton.sprite == null) return;
				EnchantFx.stageWard( look( xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Corrosion.class ), s.skeleton.sprite,
						p.sprite, EnchantFx.MID, 0, EnchantFx.CURSE, -1 );
			} );
		} );

		page.add( "a crowded fight", 7f, s -> {
			final FxStage.Puppet[] dummies = new FxStage.Puppet[6];
			for (int i = 0; i < dummies.length; i++) dummies[i] = s.puppet( Rat.class, s.cell( -4 + (i % 3) * 2, -2 + (i / 3) * 2 ) );
			final FxStage.Puppet[] fighters = {
					s.puppet( Gnoll.class, s.cell( -5, -1 ) ), s.puppet( Gnoll.class, s.cell( -1, -1 ) ), s.puppet( Gnoll.class, s.cell( -3, 1 ) ) };
			final EnchantLook[] looks = {
					look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing.class ),
					look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking.class ),
					look( xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric.class ) };
			for (int k = 0; k < 15; k++){
				final int n = k;
				s.after( 0.2f + 0.4f * k, () -> {
					for (int f = 0; f < fighters.length; f++){
						FxStage.Puppet a = fighters[f], d = dummies[(n + 2 * f) % dummies.length];
						if (a == null || d == null) continue;
						a.sprite.attack( d.ch.pos );
						EnchantFx.stageSwing( looks[f], a.sprite, d.sprite, true, 1, 1, false );
						EnchantFx.stage( looks[f], a.sprite, d.sprite, (n + f) % 3, 0, 0 );
						HitFx.hit( d.sprite, a.sprite.x + a.sprite.width() / 2f, a.sprite.y + a.sprite.height() / 2f, 3, HitFx.SLASH, false );
					}
				} );
			}
		} );
	}

	//a blow from the striker on the rat; an enchanted weapon's swing first, if one is given
	static void strike( FxStage s, FxStage.Puppet p, EnchantLook swing ){
		if (s.dryRat == null || s.dryRat.sprite == null || p == null) return;
		CharSprite rat = s.dryRat.sprite;
		p.sprite.attack( s.dryRat.pos );
		if (swing != null) EnchantFx.stageSwing( swing, p.sprite, rat, true, 0, 1, false );
		HitFx.hit( rat, p.sprite.x + p.sprite.width() / 2f, p.sprite.y + p.sprite.height() / 2f, 3, HitFx.SLASH, false );
	}

	static void state( Mob m, CharSprite.State st, boolean on ){
		if (m == null || m.sprite == null) return;
		if (on) m.sprite.add( st );
		else m.sprite.remove( st );
	}

	// ------------------------------------------------------------------ the arena

	/**
	 * fx-enchant-arena (and fx-enchant-arena-night): a room to swing an enchanted weapon in. The
	 * blank room round the hero (blank-room), a pool east of him and grass north, three hardy dummies
	 * (9999 health, standing still: a rat, a skeleton, a gnoll; the rat in the pool's edge), two
	 * allied rats beside him, and for the hero a +0 sword of every weapon enchantment, a cloth armour
	 * of every glyph, an alpha weapon of every type of the rt2 pass, a rare weapon of every weapon
	 * trait and a rare armour of every armour trait (what the pack cannot hold lies in rows south and north of
	 * him). Real procs: whatever the enchantments' areas draw shows here. By day, or held at night.
	 */
	static final class Arena implements DebugScenes.Scene {

		static final int HARDY = 9999;
		final boolean night;

		Arena( boolean night ){
			this.night = night;
		}

		@Override
		public String id(){
			return night ? "fx-enchant-arena-night" : "fx-enchant-arena";
		}

		@Override
		public String title(){
			return night ? "Effects: an arena for every enchantment, at night" : "Effects: an arena for every enchantment";
		}

		@Override
		public void apply( Hero hero ){
			DebugScenes.Scene room = null;
			for (DebugScenes.Scene s : DebugScenes.SCENES) if (s.id().equals( "blank-room" )) room = s;
			if (room == null) return;
			room.apply( hero );
			if (Dungeon.level instanceof OverworldLevel){
				GLog.w( "Scene: run " + id() + " again once below the surface (the blank room needs a dungeon floor)" );
				return;
			}
			Level level = Dungeon.level;
			for (int dy = -1; dy <= 1; dy++){
				for (int dx = 3; dx <= 5; dx++) set( level, at( hero, dx, dy ), Terrain.WATER );
			}
			for (int dx = -2; dx <= 2; dx++) set( level, at( hero, dx, -3 ), dx % 2 == 0 ? Terrain.HIGH_GRASS : Terrain.GRASS );
			GameScene.updateMap();
			level.cleanWalls();
			spawn( Rat.class, at( hero, 3, 0 ), null );
			spawn( Skeleton.class, at( hero, -2, 0 ), null );
			spawn( Gnoll.class, at( hero, 0, 2 ), null );
			//two allies beside the hero, for an instrument's chord to reach
			spawn( Rat.class, at( hero, -1, -1 ), Char.Alignment.ALLY );
			spawn( Rat.class, at( hero, 1, -1 ), Char.Alignment.ALLY );

			int rack = 0;
			for (Class<?>[] list : new Class<?>[][]{ Weapon.Enchantment.common, Weapon.Enchantment.uncommon, Weapon.Enchantment.rare,
					{ xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech.class,
							xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw.class,
							xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.CromLuck.class,
							xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror.class,
							xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.LokisPoison.class,
							xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.NeptuneShock.class,
							xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom.class,
							xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Osmose.class } }){
				for (Class<?> c : list){
					Weapon.Enchantment e = (Weapon.Enchantment) Reflection.newInstance( c );
					if (e == null) continue;
					Sword w = new Sword();
					w.enchant( e );
					rack = give( hero, w, rack );
				}
			}
			for (Class<?>[] list : new Class<?>[][]{ Armor.Glyph.common, Armor.Glyph.uncommon, Armor.Glyph.rare }){
				for (Class<?> c : list){
					Armor.Glyph g = (Armor.Glyph) Reflection.newInstance( c );
					if (g == null) continue;
					ClothArmor a = new ClothArmor();
					a.inscribe( g );
					rack = give( hero, a, rack );
				}
			}
			//an alpha weapon of each type of the rt2 pass: Woodsman, Quarry, the five Bard
			//instruments, Lash
			for (Class<?> c : new Class<?>[]{ HandAxe.class, WarHammer.class, Flute.class, Harp.class, Trumpet.class,
					Wardrum.class, Triangolo.class, Whip.class }){
				Item w = (Item) Reflection.newInstance( c );
				Quality.roll( w, Rarity.RARE, ItemType.ALPHA, 0 );
				rack = give( hero, w, rack );
			}
			//a weapon of every weapon trait, on a kind that offers it (a slayer of each family)
			rack = give( hero, lined( new HandAxe(), RarityLine.WARM_GRIP, null ), rack );
			for (SlayerFamily f : SlayerFamily.values()) rack = give( hero, lined( new WarHammer(), RarityLine.SLAYER, f ), rack );
			rack = give( hero, lined( new Whip(), RarityLine.EXECUTIONER, null ), rack );
			rack = give( hero, lined( new Flute(), RarityLine.MOONLIT, null ), rack );
			rack = give( hero, lined( new Harp(), RarityLine.STORMBORN, null ), rack );
			rack = give( hero, lined( new Flail(), RarityLine.DEFIANT, null ), rack );
			rack = give( hero, lined( new Whip(), RarityLine.HUNTER, null ), rack );
			rack = give( hero, lined( new Bow(), RarityLine.STEADY, null ), rack );
			//an armor of every armor trait: the light ones on leather, the heavy ones on plate
			for (RarityLine l : new RarityLine[]{ RarityLine.FLEET, RarityLine.HUSHED, RarityLine.RAINPROOF, RarityLine.COOLING }){
				rack = give( hero, lined( new LeatherArmor(), l, null ), rack );
			}
			for (RarityLine l : new RarityLine[]{ RarityLine.WINDBREAK, RarityLine.SUREFOOT, RarityLine.RESOLVE, RarityLine.WARMTH }){
				rack = give( hero, lined( new PlateArmor(), l, null ), rack );
			}

			DayNightCycle.debugPhaseOverride = night ? DayNightCycle.Phase.NIGHT : DayNightCycle.Phase.DAY;
			ClimateManager.onHeroTurn();
			GameScene.updateDayNightTint();
			GLog.i( "Scene: a rat in the pool's edge east, a skeleton west, a gnoll south, standing still, and two allied rats"
					+ " beside you. In the pack (and in rows south and north): a +0 sword of every weapon enchantment and a cloth armour of"
					+ " every glyph, then an alpha weapon of every new type, a rare weapon of every weapon trait and a rare armour"
					+ " of every armour trait. " + (night ? "Held at night." : "Held by day.") );
		}

		//the blank room's free rows for what the pack cannot hold: two south of the gnoll, two north
		//of the grass, clear of the pool and the dummies (the room's floor runs to 5 each way)
		private static final int[] RACK_ROWS = { 4, 5, -5, -4 };

		//into the pack, or on the floor in the rows south and north of the hero, the later ones
		//heaped on the earlier when every place is taken; the rack's next place
		private static int give( Hero hero, Item item, int rack ){
			item.identify();
			if (item.collect( hero.belongings.backpack )) return rack;
			int cell = at( hero, -6 + rack % 13, RACK_ROWS[(rack / 13) % RACK_ROWS.length] );
			if (cell < 0 || cell >= Dungeon.level.length() || Dungeon.level.solid[cell]) cell = hero.pos;
			Heap h = Dungeon.level.drop( item, cell );
			if (h.sprite != null) h.sprite.drop();
			return rack + 1;
		}

		//a rare alpha piece whose lines are the trait (at its full roll, sharpened against a family
		//for a slayer) and then the plain bonus it rolled
		private static Item lined( Item item, RarityLine trait, SlayerFamily family ){
			Quality.roll( item, Rarity.RARE, ItemType.ALPHA, 0 );
			Quality q = Quality.of( item );
			q.lines.set( 0, trait );
			q.rolls.set( 0, 1f );
			q.slayer = family;
			q.applyDerived( item );
			return item;
		}

		private static int at( Hero hero, int dx, int dy ){
			return hero.pos + dx + dy * Dungeon.level.width();
		}

		private static void set( Level level, int cell, int terrain ){
			if (cell < 0 || cell >= level.length() || Actor.findChar( cell ) != null) return;
			Level.set( cell, terrain );
			level.visited[cell] = true;
			level.mapped[cell] = true;
		}

		//a hardy dummy standing still: an enemy, or on the side given
		private static void spawn( Class<? extends Mob> kind, int cell, Char.Alignment side ){
			if (cell < 0 || cell >= Dungeon.level.length() || Actor.findChar( cell ) != null || Dungeon.level.solid[cell]) return;
			Mob mob = Reflection.newInstance( kind );
			if (mob == null) return;
			mob.pos = cell;
			mob.HP = mob.HT = HARDY;
			mob.state = mob.PASSIVE;
			if (side != null) mob.alignment = side;
			GameScene.add( mob );
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.rows;

import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.MirrorImage;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Displacement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Metabolism;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Multiplicity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Affection;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Phasing;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Siphoning;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Displacing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Friendly;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wayward;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Wondrous;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.AresLeech;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.BuzzSaw;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Corrupting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.CromLuck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Eldritch;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Horror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Hunting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.JupitersHorror;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Luck;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Lucky;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Midas;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Nomnom;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Osmose;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Projecting;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Slashing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Unstable;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vicious;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vorpal;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The gallery's pages for the blood, shadow, fortune and mirage rows (EnchantArea's exhibits):
 * every weapon enchantment of theirs on the rat, middling, then strong, then killing; their glyphs
 * answering a gnoll's blow, Flow and Swiftness walking; their curses; drains and mirages side by
 * side. Each look restaged by hand on puppets through EnchantFx's and ShadowRows' entries with
 * the rows' own twists, never a real proc.
 */
public final class ShadowExhibits {

	private ShadowExhibits(){}

	public static void add( FxGallery.Page page ){
		weapons( page );
		glyphs( page );
		curses( page );
		drains( page );
	}

	static EnchantLook look( Class<?> c ){
		return EnchantLooks.of( c );
	}

	//the gnoll that strikes, west of the rat
	static FxStage.Puppet striker( FxStage s ){
		return s.puppet( Gnoll.class, s.cell( -3, 2 ) );
	}

	//a blow from p onto d with a look and a twist at a tier; the killing one ends as one ends
	static void blow( FxStage.Puppet p, CharSprite d, EnchantLook l, int tier, int flags, EnchantFx.Twist twist ){
		if (p == null || d == null || d.parent == null || l == null) return;
		p.sprite.attack( d.ch.pos );
		EnchantFx.stage( l, p.sprite, d, tier, 0, flags, twist );
		if (tier == EnchantFx.FINISHER) EnchantFx.stageEnded( p.sprite, d );
		else HitFx.hit( d, p.sprite.x + p.sprite.width() / 2f, p.sprite.y + p.sprite.height() / 2f, 3, HitFx.SLASH, false );
	}

	//Buzz saw's further cuts on the same blow (the game makes them all at once): the saw turning
	static void cuts( FxStage.Puppet p, CharSprite d, int tier ){
		if (p == null || d == null || d.parent == null) return;
		for (int k = 2; k <= 3; k++) EnchantFx.stage( look( BuzzSaw.class ), p.sprite, d, tier, 0, 0, ShadowRows.buzz( k ) );
	}

	/** A weapon row on the gallery: its look's class and its twist (none: the family's alone). */
	static final class Shown {
		final Class<?> c;
		final EnchantFx.Twist twist;

		Shown( Class<?> c, EnchantFx.Twist twist ){
			this.c = c;
			this.twist = twist;
		}
	}

	static final String[] TIER = { "weak", "middling", "strong", "killing" };

	// ------------------------------------------------------------------ the weapon enchantments

	static void weapons( FxGallery.Page page ){
		final float each = 2.6f;
		final int n = 20;
		page.add( "blood, shadow and fortune: every weapon enchantment, one by one, on the rat", n * each + 0.5f, s -> {
			final FxStage.Puppet p = striker( s );
			final List<Char> witnesses = new ArrayList<>( Arrays.asList( s.skeleton, s.bat ) );
			final Shown[] rows = {
					new Shown( Eldritch.class, ShadowRows.eldritch( witnesses ) ),
					new Shown( Lucky.class, ShadowRows.LUCKY ),
					new Shown( Projecting.class, null ),
					new Shown( Unstable.class, ShadowRows.DICE ),
					new Shown( Vorpal.class, ShadowRows.VORPAL ),
					new Shown( Corrupting.class, null ),
					new Shown( Grim.class, ShadowRows.GRIM ),
					new Shown( Vampiric.class, ShadowRows.VAMPIRIC ),
					new Shown( Horror.class, ShadowRows.HORROR ),
					new Shown( Hunting.class, ShadowRows.HUNTING ),
					new Shown( Midas.class, ShadowRows.MIDAS_KILL ),
					new Shown( Vicious.class, ShadowRows.VICIOUS ),
					new Shown( Luck.class, null ),
					new Shown( Slashing.class, ShadowRows.SLASHING ),
					new Shown( CromLuck.class, ShadowRows.CROM ),
					new Shown( AresLeech.class, ShadowRows.ARES_RING ),
					new Shown( BuzzSaw.class, ShadowRows.buzz( 1 ) ),
					new Shown( JupitersHorror.class, ShadowRows.JUPITERS ),
					new Shown( Nomnom.class, ShadowRows.NOMNOM ),
					new Shown( Osmose.class, null ) };
			for (int i = 0; i < rows.length; i++){
				final Shown r = rows[i];
				final float t0 = 0.3f + each * i;
				s.after( t0, () -> {
					GLog.i( "  " + r.c.getSimpleName() + ": middling, strong, killing" );
					if (s.dryRat.sprite == null) return;
					if (r.c == Osmose.class){
						//the mana drawn into the striker (no wielder of the family's own)
						EnchantFx.stage( look( r.c ), null, s.dryRat.sprite, EnchantFx.MID, 0, 0, ShadowRows.osmose( p.ch ) );
					} else {
						blow( p, s.dryRat.sprite, look( r.c ), EnchantFx.MID, 0, r.twist );
					}
					if (r.c == BuzzSaw.class) cuts( p, s.dryRat.sprite, EnchantFx.MID );
					if (r.c == Lucky.class) ShadowRows.luckMark( s.dryRat, true );
					if (r.c == Corrupting.class) ShadowRows.corruptMark( s.dryRat, false );
				} );
				s.after( t0 + 0.8f, () -> {
					if (s.dryRat.sprite == null) return;
					if (r.c == Lucky.class) ShadowRows.luckMark( s.dryRat, false );
					blow( p, s.dryRat.sprite, look( r.c ), EnchantFx.STRONG, 1, r.twist );
					if (r.c == BuzzSaw.class) cuts( p, s.dryRat.sprite, EnchantFx.STRONG );
				} );
				s.after( t0 + 1.6f, () -> {
					if (s.dryRat.sprite == null) return;
					if (r.c == Corrupting.class){
						ShadowRows.corruptCollapse( s.dryRat.sprite, false );
						ShadowRows.corruptRise( s.dryRat, false );
						return;
					}
					blow( p, s.dryRat.sprite, look( r.c ), EnchantFx.FINISHER, 0, r.twist );
					if (r.c == Lucky.class) ShadowRows.luckyDrop( s.dryRat.sprite );
					if (r.c == Hunting.class) ShadowRows.huntMeat( s.dryRat.pos );
				} );
			}
		} );
	}

	// ------------------------------------------------------------------ the glyphs

	static void glyphs( FxGallery.Page page ){
		page.add( "blood, shadow and fortune: glyphs answering a gnoll's blow", 21f, s -> {
			final FxStage.Puppet wearer = s.puppet( Rat.class, s.cell( -2, 0 ) );
			final FxStage.Puppet foe = s.puppet( Gnoll.class, s.cell( -3, 0 ) );
			if (wearer == null || foe == null) return;
			s.after( 0.3f, () -> {
				GLog.i( "  Affection: the pink arc, hearts" );
				foe.sprite.attack( wearer.ch.pos );
				EnchantFx.ward( look( Affection.class ), wearer.ch, foe.ch, 0.45f, 0, 0 );
			} );
			s.after( 1.8f, () -> {
				GLog.i( "  Afterimage: the blow goes through a ghost" );
				foe.sprite.attack( wearer.ch.pos );
				ShadowRows.afterimageDodge( wearer.ch, foe.ch );
			} );
			s.after( 3.3f, () -> {
				GLog.i( "  Mirror image: images step out of the wearer" );
				foe.sprite.attack( wearer.ch.pos );
				ShadowRows.mirrorimage( wearer.ch, foe.ch, 0, 0, images( s, s.cell( -1, -1 ), s.cell( -2, -1 ) ) );
			} );
			s.after( 4.8f, () -> {
				GLog.i( "  Mirroring: a mirror pane, flipped ghosts" );
				foe.sprite.attack( wearer.ch.pos );
				ShadowRows.mirroring( wearer.ch, foe.ch, 0, 0, images( s, s.cell( -1, 1 ) ) );
			} );
			s.after( 6.3f, () -> GLog.i( "  Obfuscation: no moment to show (its smoke is the armour's)" ) );
			s.after( 7.3f, () -> {
				GLog.i( "  Phasing: a blue ghost left where the wearer stood" );
				foe.sprite.attack( wearer.ch.pos );
				int from = wearer.ch.pos;
				wearer.place( s.cell( -1, 1 ) );
				EnchantFx.ward( look( Phasing.class ), wearer.ch, foe.ch, 0.7f, 0, 0, ShadowRows.phase( from ) );
			} );
			s.after( 8.8f, () -> {
				wearer.place( s.cell( -2, 0 ) );
				GLog.i( "  Siphoning: the attacker's blood drawn into the wearer" );
				foe.sprite.attack( wearer.ch.pos );
				EnchantFx.ward( look( Siphoning.class ), wearer.ch, foe.ch, 0.5f, 0, 0 );
			} );
			s.after( 10.3f, () -> {
				GLog.i( "  Thorns: thorns shot into the attacker, its blood" );
				foe.sprite.attack( wearer.ch.pos );
				EnchantFx.stage( look( Thorns.class ), wearer.sprite, foe.sprite, EnchantFx.MID, 0, 0, ShadowRows.THORNS );
			} );
			s.after( 11.8f, () -> {
				GLog.i( "  a strong Siphoning and Thorns at once" );
				foe.sprite.attack( wearer.ch.pos );
				EnchantFx.ward( look( Siphoning.class ), wearer.ch, foe.ch, 0.8f, 0, 0 );
				EnchantFx.stage( look( Thorns.class ), wearer.sprite, foe.sprite, EnchantFx.STRONG, 0, 0, ShadowRows.THORNS );
			} );
			//Flow through the pool, Swiftness on dry floor: a step every third of a second
			final int[] pool = s.pool();
			final FxStage.Puppet wader = s.puppet( Rat.class, pool[0] );
			if (wader == null) return;
			s.after( 13.3f, () -> GLog.i( "  Flow: a wake, a streak, a ghost every second step" ) );
			for (int i = 1; i < 5; i++){
				final int to = pool[i];
				s.after( 13.3f + 0.35f * i, () -> {
					wader.move( to );
					ShadowRows.flowStep( wader.ch );
				} );
			}
			s.after( 15.6f, () -> GLog.i( "  Swiftness: a mint streak, a faint ghost every second step" ) );
			for (int i = 1; i < 6; i++){
				final int to = s.cell( -6 + i, -2 );
				s.after( 15.6f + 0.35f * i, () -> {
					wader.move( to );
					ShadowRows.swiftStep( wader.ch );
				} );
			}
			s.after( 15.5f, () -> wader.place( s.cell( -6, -2 ) ) );
		} );
	}

	//mirror images standing on cells (puppets), their sprites
	static List<CharSprite> images( FxStage s, int... cells ){
		List<CharSprite> out = new ArrayList<>();
		for (int c : cells){
			FxStage.Puppet p = s.puppet( MirrorImage.class, c );
			if (p != null) out.add( p.sprite );
		}
		return out;
	}

	// ------------------------------------------------------------------ the curses

	static void curses( FxGallery.Page page ){
		page.add( "blood, shadow and fortune: the curses", 14f, s -> {
			final FxStage.Puppet p = striker( s );
			final Shown[] weapon = {
					new Shown( Annoying.class, ShadowRows.ANNOYING ),
					new Shown( Displacing.class, null ),
					new Shown( Friendly.class, ShadowRows.FRIENDLY ),
					new Shown( Sacrificial.class, ShadowRows.SACRIFICIAL ),
					new Shown( Wayward.class, ShadowRows.WAYWARD ),
					new Shown( Wondrous.class, ShadowRows.WONDROUS ) };
			for (int i = 0; i < weapon.length; i++){
				final Shown r = weapon[i];
				s.after( 0.3f + 1.4f * i, () -> {
					GLog.i( "  " + r.c.getSimpleName() + " (hexed)" );
					if (s.dryRat.sprite == null || p == null) return;
					if (r.c == Sacrificial.class){
						//the wielder bleeding into his own blade
						p.sprite.attack( s.dryRat.pos );
						EnchantFx.stage( look( r.c ), null, p.sprite, EnchantFx.MID, 0, EnchantFx.CURSE, r.twist );
					} else if (r.c == Displacing.class){
						int from = s.dryRat.pos;
						blow( p, s.dryRat.sprite, look( r.c ), EnchantFx.MID, EnchantFx.CURSE, ShadowRows.displaced( from ) );
					} else {
						blow( p, s.dryRat.sprite, look( r.c ), EnchantFx.MID, EnchantFx.CURSE, r.twist );
					}
				} );
			}
			final FxStage.Puppet wearer = s.puppet( Rat.class, s.cell( -2, 0 ) );
			final FxStage.Puppet foe = s.puppet( Gnoll.class, s.cell( -3, 0 ) );
			if (wearer == null || foe == null) return;
			s.after( 8.8f, () -> {
				GLog.i( "  Displacement: the blow through a grey arc, ink at both ends" );
				foe.sprite.attack( wearer.ch.pos );
				int from = wearer.ch.pos;
				wearer.place( s.cell( -1, 1 ) );
				EnchantFx.ward( look( Displacement.class ), wearer.ch, foe.ch, 0.45f, 0, EnchantFx.CURSE, ShadowRows.displacement( from ) );
			} );
			s.after( 10.3f, () -> {
				wearer.place( s.cell( -2, 0 ) );
				GLog.i( "  Metabolism: healing drawn in, hunger leaving" );
				EnchantFx.ward( look( Metabolism.class ), wearer.ch, null, 0.45f, 0, EnchantFx.CURSE, ShadowRows.METABOLISM );
			} );
			s.after( 11.8f, () -> {
				GLog.i( "  Multiplicity: a dark mirror where the copy comes out" );
				foe.sprite.attack( wearer.ch.pos );
				EnchantFx.ward( look( Multiplicity.class ), wearer.ch, foe.ch, 0.45f, 0, EnchantFx.CURSE,
						ShadowRows.multiplied( s.cell( -1, -1 ) ) );
			} );
		} );
	}

	// ------------------------------------------------------------------ drains and mirages

	static void drains( FxGallery.Page page ){
		page.add( "drains and mirages", 10f, s -> {
			final FxStage.Puppet p = striker( s );
			final FxStage.Puppet g = s.puppet( Gnoll.class, s.cell( 0, 3 ) );
			s.after( 0.3f, () -> {
				GLog.i( "  Vampiric on the rat: its red blood drawn home" );
				if (s.dryRat.sprite != null) blow( p, s.dryRat.sprite, look( Vampiric.class ), EnchantFx.STRONG, 0, ShadowRows.VAMPIRIC );
			} );
			s.after( 1.6f, () -> {
				GLog.i( "  Vampiric on the skeleton: a bone-white drain" );
				if (s.skeleton.sprite != null) blow( g, s.skeleton.sprite, look( Vampiric.class ), EnchantFx.STRONG, 0, ShadowRows.VAMPIRIC );
			} );
			s.after( 3.0f, () -> {
				GLog.i( "  Ares' leech: spokes from every foe in reach, a ring of blood closing" );
				if (p == null) return;
				p.sprite.attack( s.dryRat.pos );
				ShadowRows.aresLeech( look( AresLeech.class ), p.ch, Arrays.asList( s.dryRat, s.skeleton, s.wetRat, s.bat ), 0.5f, 0 );
			} );
			final FxStage.Puppet wearer = s.puppet( Rat.class, s.cell( -2, 0 ) );
			final FxStage.Puppet foe = s.puppet( Gnoll.class, s.cell( -3, 0 ) );
			if (wearer == null || foe == null) return;
			s.after( 4.6f, () -> {
				GLog.i( "  Afterimage" );
				foe.sprite.attack( wearer.ch.pos );
				ShadowRows.afterimageDodge( wearer.ch, foe.ch );
			} );
			s.after( 6.0f, () -> {
				GLog.i( "  Mirror image" );
				foe.sprite.attack( wearer.ch.pos );
				ShadowRows.mirrorimage( wearer.ch, foe.ch, 0, 0, images( s, s.cell( -1, -1 ) ) );
			} );
			s.after( 7.5f, () -> {
				GLog.i( "  Phasing" );
				foe.sprite.attack( wearer.ch.pos );
				int from = wearer.ch.pos;
				wearer.place( s.cell( -1, 1 ) );
				EnchantFx.ward( look( Phasing.class ), wearer.ch, foe.ch, 0.7f, 0, 0, ShadowRows.phase( from ) );
			} );
		} );
	}
}

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

import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantFx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLook;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant.EnchantLooks;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.AntiEntropy;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Corrosion;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Overgrowth;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Stench;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Adaptation;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.AntiMagic;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Bounce;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Camouflage;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Entanglement;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Potential;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Purity;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Repulsion;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Satisfying;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Stone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Viscosity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Dazzling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Explosive;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Polarized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Pressurized;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Ancient;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blooming;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Crystal;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Elastic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Holy;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Kinetic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.LokisPoison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.NeptuneShock;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Paralysis;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Parasitic;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Poison;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Surging;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Venomous;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * The gallery's pages of the elements and nature (EnchantArea's page, after the core's): every
 * weapon enchantment of the eight families on the rat, one by one at MID, then strong and as the
 * killing blow; the glyphs answering a gnoll's blow; the curses; and the looks that answer water and
 * grass. Restaged by hand on the stage's dummies and puppets through EnchantFx's entries with each
 * row's own twist (ElementRows), never a real proc; the moments of no proc (Kinetic's store,
 * Crystal's hit, the ticks) through their own entries, which only draw.
 */
public final class ElementExhibits {

	private ElementExhibits(){}

	/** How long each row is given on the rat, s. */
	static final float EACH = 1.4f;

	/** A row of the gallery: its caption, its look and its twist (made afresh for each showing). */
	interface Twister {
		EnchantFx.Twist make();
	}

	static final class Shown {
		final String caption;
		final EnchantLook look;
		final Twister twist;

		Shown( String caption, Class<?> of, Twister twist ){
			this.caption = caption;
			this.look = EnchantLooks.of( of );
			this.twist = twist;
		}
	}

	//the seventeen weapon rows, as the gallery restages them
	static Shown[] weapons(){
		return new Shown[]{
				new Shown( "Blazing: a fire catching, tongues clinging", Blazing.class, () -> ElementRows.blazing( true, false ) ),
				new Shown( "Chilling: frost clinging, rime crawling on", Chilling.class, () -> ElementRows.chilling( 4f ) ),
				new Shown( "Shocking: the impact, an etch, a pool of its light", Shocking.class, () -> ElementRows.shock( 0, -1 ) ),
				new Shown( "Kinetic: the stored force let go", Kinetic.class, () -> ElementRows.kinetic( 0.6f ) ),
				new Shown( "Venomous: the venom's mark over the head", Venomous.class, ElementRows::venomous ),
				new Shown( "Blocking: a ward on the wielder", Blocking.class, ElementRows::blocking ),
				new Shown( "Blooming: grass shooting up round the foe", Blooming.class, null ),
				new Shown( "Elastic: the launch ring", Elastic.class, ElementRows::elastic ),
				new Shown( "Crystal: glass at every hit", Crystal.class, null ),
				new Shown( "Paralysis: a ring stopping dead, shattering", Paralysis.class, () -> ElementRows.paralysis( 3f ) ),
				new Shown( "Surging: the surge's streak", Surging.class, () -> ElementRows.surging( 4, 3 ) ),
				new Shown( "Parasitic: the seed planted", Parasitic.class, ElementRows::parasitic ),
				new Shown( "Holy: radiance on the undead", Holy.class, () -> null ),
				new Shown( "Ancient: a rune stamped, one drawn into the blade", Ancient.class, () -> ElementRows.ancient( true ) ),
				new Shown( "Loki's poison: the sickly ring", LokisPoison.class, () -> ElementRows.plague( 3 ) ),
				new Shown( "Neptune's shock: the reach and the foam", NeptuneShock.class, () -> ElementRows.shock( 0, 3 ) ),
				new Shown( "Poison: spores", Poison.class, ElementRows::poison ) };
	}

	//a striker beside the rat, facing it
	static FxStage.Puppet striker( FxStage s, int dx, int dy ){
		return s.puppet( Gnoll.class, s.cell( dx, dy ) );
	}

	//a row restaged on a defender from a striker at a tier, its blow's star after it
	static void hit( FxStage s, FxStage.Puppet p, Mob foe, Shown r, int tier, int flags ){
		if (p == null || foe == null || foe.sprite == null || r.look == null) return;
		CharSprite d = foe.sprite;
		p.sprite.attack( foe.pos );
		if (r.look.of == Blooming.class){
			int[] cells = { foe.pos, s.cell( -1, 2 ), s.cell( -2, 3 ), s.cell( -1, 3 ) };
			EnchantFx.stage( r.look, p.sprite, d, tier, 0, flags, ElementRows.blooming( cells, foe.pos ) );
		} else if (r.look.of == Crystal.class){
			ElementRows.crystalHit( p.ch, foe );
		} else {
			EnchantFx.stage( r.look, p.sprite, d, tier, 0, flags, r.twist.make() );
		}
		if (r.look.of == Kinetic.class) ElementRows.kineticStore( foe, p.ch, 12 );
		if (tier == EnchantFx.FINISHER) EnchantFx.stageEnded( p.sprite, d );
		else HitFx.hit( d, p.sprite.x + p.sprite.width() / 2f, p.sprite.y + p.sprite.height() / 2f, 3, HitFx.SLASH, false );
	}

	/** The pages: one line in EnchantArea.exhibits. */
	public static void exhibits( FxGallery.Page page ){
		final Shown[] w = weapons();
		page.add( "elements and nature: every weapon enchantment, one by one, on the rat", w.length * EACH * 2f + 0.5f, s -> {
			final FxStage.Puppet p = striker( s, -3, 2 );
			for (int i = 0; i < w.length; i++){
				final Shown r = w[i];
				s.after( 0.3f + EACH * i, () -> {
					GLog.i( "  " + r.caption + " (middling)" );
					hit( s, p, s.dryRat, r, EnchantFx.MID, 0 );
				} );
				//then strong, and as the killing blow
				s.after( 0.3f + EACH * (w.length + i), () -> {
					GLog.i( "  " + r.caption + ": strong, then the killing blow" );
					hit( s, p, s.dryRat, r, EnchantFx.STRONG, 0 );
				} );
				s.after( 0.3f + EACH * (w.length + i) + EACH * 0.5f, () -> hit( s, p, s.dryRat, r, EnchantFx.FINISHER, 0 ) );
			}
			s.after( 0.3f + EACH * 2 * w.length - 0.4f, () -> {
				GLog.i( "  Ancient attuning itself" );
				if (p != null) ElementRows.attuned( p.ch );
			} );
		} );

		final Object[][] glyphs = {
				{ "Adaptation on grass: the arc in the grass's green, earth hopping", Adaptation.class, (Twister) () -> ElementRows.adaptation( ElementRows.GRASS ) },
				{ "Adaptation on embers", Adaptation.class, (Twister) () -> ElementRows.adaptation( ElementRows.EMBERS ) },
				{ "AntiMagic: the magic bending into the armour", AntiMagic.class, (Twister) () -> ElementRows.nullified( 8 ) },
				{ "Bounce: a sproing where the blow met", Bounce.class, (Twister) ElementRows::bounce },
				{ "Brimstone: fire refused", Brimstone.class, null },
				{ "Camouflage: blades and leaves closing", Camouflage.class, (Twister) ElementRows::camouflage },
				{ "Crystal: glass converging, a bubble", xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Crystal.class, (Twister) ElementRows::crystalWard },
				{ "Entanglement: vines up the legs", Entanglement.class, (Twister) ElementRows::entanglement },
				{ "Potential: static to the wand hand", Potential.class, (Twister) () -> ElementRows.potential( 2 ) },
				{ "Purity: light washing up, debuffs leaving", Purity.class, (Twister) () -> ElementRows.purity( 2 ) },
				{ "Repulsion: a shock ring from the wearer", Repulsion.class, (Twister) ElementRows::repulsion },
				{ "Satisfying: warm motes into the belly", Satisfying.class, (Twister) ElementRows::satisfying },
				{ "Stone: the blow turned to stone", Stone.class, (Twister) ElementRows::stone },
				{ "Viscosity: the blow sinking into tar, then a tick of it", Viscosity.class, (Twister) ElementRows::viscosity } };
		page.add( "elements and nature: glyphs answering a gnoll's blow", glyphs.length * EACH + 0.5f, s -> {
			final FxStage.Puppet p = striker( s, -3, 2 );
			for (int i = 0; i < glyphs.length; i++){
				final Object[] g = glyphs[i];
				s.after( 0.3f + EACH * i, () -> {
					Mob wearer = s.dryRat;
					if (p == null || wearer == null || wearer.sprite == null) return;
					GLog.i( "  " + g[0] );
					EnchantLook l = EnchantLooks.of( (Class<?>) g[1] );
					p.sprite.attack( wearer.pos );
					if (g[1] == Brimstone.class){
						ElementRows.brimstone( wearer );
					} else if (g[1] == Viscosity.class){
						EnchantFx.passive( l, wearer, EnchantFx.ABSORB, 0.5f, ((Twister) g[2]).make() );
						s.after( 0.7f, () -> ElementRows.tarTick( wearer ) );
					} else if (g[1] == Camouflage.class){
						EnchantFx.ward( l, wearer, null, ElementRows.verdantStrength( 0 ), 0, 0, ((Twister) g[2]).make() );
					} else {
						//each at the tier its glyph answers with (Adaptation's middling, Satisfying's weak)
						float st = g[1] == Adaptation.class ? ElementRows.verdantStrength( 0 )
								: g[1] == Satisfying.class ? ElementRows.SATISFYING : 0.7f;
						EnchantFx.ward( l, wearer, p.ch, st, 0, 0, ((Twister) g[2]).make() );
					}
				} );
			}
		} );

		final Object[][] curses = {
				{ "Dazzling: a holy bloom at the wielder", Dazzling.class, (Twister) ElementRows::dazzling },
				{ "Explosive, hot: smoke and hell sparks off the blade", Explosive.class, (Twister) () -> ElementRows.explosive( true ) },
				{ "Polarized, half again: red and blue meeting", Polarized.class, (Twister) ElementRows::polarized },
				{ "Polarized, nothing: a grey fizzle", Polarized.class, null },
				{ "Pressurized: steam spurting", Pressurized.class, (Twister) ElementRows::pressurized },
				{ "AntiEntropy: rime round, a lick of fire", AntiEntropy.class, null },
				{ "Bulk: wedged in a doorway", xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Bulk.class, null },
				{ "Corrosion: acid round", Corrosion.class, null },
				{ "Overgrowth: the hex under a plant", Overgrowth.class, (Twister) () -> null },
				{ "Stench: a heave of poison smoke", Stench.class, (Twister) ElementRows::stench } };
		page.add( "elements and nature: the curses", curses.length * EACH + 0.5f, s -> {
			final FxStage.Puppet p = striker( s, -3, 2 );
			for (int i = 0; i < curses.length; i++){
				final Object[] c = curses[i];
				s.after( 0.3f + EACH * i, () -> {
					Mob foe = s.dryRat;
					if (p == null || foe == null || foe.sprite == null) return;
					GLog.i( "  " + c[0] );
					EnchantLook l = EnchantLooks.of( (Class<?>) c[1] );
					p.sprite.attack( foe.pos );
					if (!l.glyph){
						if (c[2] == null) ElementRows.polarFizzle( p.ch, foe );
						else EnchantFx.stage( l, p.sprite, foe.sprite, EnchantFx.STRONG, 0, EnchantFx.CURSE, ((Twister) c[2]).make() );
						HitFx.hit( foe.sprite, p.sprite.x + p.sprite.width() / 2f, p.sprite.y + p.sprite.height() / 2f, 3, HitFx.SLASH, false );
					} else if (c[1] == AntiEntropy.class){
						EnchantFx.ward( l, foe, p.ch, 0.45f, 0, EnchantFx.CURSE, ElementRows.antiEntropy( foe.pos ) );
					} else if (c[1] == Corrosion.class){
						EnchantFx.ward( l, foe, p.ch, 0.45f, 0, EnchantFx.CURSE, ElementRows.corrosion( foe.pos ) );
					} else if (c[2] == null){
						ElementRows.bulk( foe );
					} else {
						EnchantFx.ward( l, foe, p.ch, 0.45f, 0, EnchantFx.CURSE, ((Twister) c[2]).make() );
					}
				} );
			}
		} );

		page.add( "elements and nature: on water and on grass", 9.5f, s -> {
			final FxStage.Puppet wet = striker( s, 3, 0 );
			final int[] g = s.grass();
			final FxStage.Puppet onGrass = s.puppet( Rat.class, g[3] );
			final FxStage.Puppet grower = s.puppet( Gnoll.class, g[4] );
			final int[] wetRows = { 0, 1, 2, 4, 6 };
			for (int i = 0; i < wetRows.length; i++){
				final Shown r = w[wetRows[i]];
				s.after( 0.3f + EACH * i, () -> {
					GLog.i( "  in water: " + r.caption );
					hit( s, wet, s.wetRat, r, EnchantFx.STRONG, 0 );
				} );
			}
			s.after( 0.3f + EACH * wetRows.length, () -> {
				GLog.i( "  on grass: Blooming's blades and petals" );
				if (grower == null || onGrass == null) return;
				grower.sprite.attack( onGrass.ch.pos );
				int[] cells = { g[3], g[2], g[1], g[5] };
				EnchantFx.stage( EnchantLooks.of( Blooming.class ), grower.sprite, onGrass.sprite, EnchantFx.STRONG, 0, 0,
						ElementRows.blooming( cells, g[3] ) );
			} );
		} );
	}
}

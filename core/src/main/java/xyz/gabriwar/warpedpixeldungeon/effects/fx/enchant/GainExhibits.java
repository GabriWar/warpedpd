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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.enchant;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.npcs.EnchantingStation;
import xyz.gabriwar.warpedpixeldungeon.debug.DebugScenes;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.Enchanting;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Brimstone;
import xyz.gabriwar.warpedpixeldungeon.items.armor.glyphs.Thorns;
import xyz.gabriwar.warpedpixeldungeon.items.armor.curses.Corrosion;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Annoying;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.curses.Sacrificial;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Blazing;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Chilling;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Grim;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Vampiric;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import xyz.gabriwar.warpedpixeldungeon.scenes.EnchantingScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;

/**
 * Gaining an enchantment, in the enchantments' gallery (EnchantArea's page) and its debug scene:
 * Enchanting's ceremony family by family on the skeleton (a curse run in reverse, glyphs on armour,
 * Magical Infusion's first enchantment), identifying revealing a curse and a vestige, an enchanted
 * item's first pickup - each restaged by hand, nothing sent, nothing of the game changed - and
 * fx-enchant-pedestal: the enchanting pedestal opened on an exotic sword that carries a vestige,
 * with energy for every move.
 */
public final class GainExhibits {

	private GainExhibits(){}

	/** A ceremony, and the gap after it. */
	static final float CEREMONY = 1.9f;

	/** The gallery's exhibits of gaining (EnchantArea.exhibits). */
	public static void exhibits( FxGallery.Page page ){
		final Family[] fams = Family.values();
		page.add( "gaining an enchantment", (fams.length + 4) * CEREMONY + 0.5f, s -> {
			for (int f = 0; f < fams.length; f++){
				final EnchantLook l = face( fams[f] );
				gain( s, 0.3f + CEREMONY * f, "  " + l.of.getSimpleName() + " (" + l.fam + "): its family gathers, binds, ends on him",
						new Sword(), l );
			}
			float t = 0.3f + CEREMONY * fams.length;
			gain( s, t, "  a curse taking hold (Sacrificial): its runes leave, the ghost darkens to ink", cursed(),
					EnchantLooks.of( Sacrificial.class ) );
			gain( s, t + CEREMONY, "  a glyph (Thorns), drawn from the armour", new ClothArmor(), EnchantLooks.of( Thorns.class ) );
			gain( s, t + 2 * CEREMONY, "  a glyph (Brimstone)", new ClothArmor(), EnchantLooks.of( Brimstone.class ) );
			gain( s, t + 3 * CEREMONY, "  Magical Infusion's first enchantment (Shocking)", new Sword(), EnchantLooks.of( Shocking.class ) );
		} );

		page.add( "revealed by identifying", 6.5f, s -> {
			s.after( 0.3f, () -> {
				GLog.i( "  a curse found out: the hex in small, drawn into him" );
				GainFx.stageCurseReveal( sprite( s ), EnchantLooks.of( Annoying.class ) );
			} );
			s.after( 1.6f, () -> {
				GLog.i( "  an armour's curse (Corrosion)" );
				GainFx.stageCurseReveal( sprite( s ), EnchantLooks.of( Corrosion.class ) );
			} );
			s.after( 2.9f, () -> {
				GLog.i( "  a vestige (Blazing): its finale in small, then its echo" );
				GainFx.stageVestigeReveal( sprite( s ), EnchantLooks.of( Blazing.class ) );
			} );
			s.after( 4.4f, () -> {
				GLog.i( "  a vestige (Vampiric)" );
				GainFx.stageVestigeReveal( sprite( s ), EnchantLooks.of( Vampiric.class ) );
			} );
		} );

		final Class<?>[] picked = { Blazing.class, Chilling.class, Grim.class, Thorns.class, Annoying.class };
		page.add( "the first pickup", picked.length * 1.1f + 0.5f, s -> {
			for (int i = 0; i < picked.length; i++){
				final EnchantLook l = EnchantLooks.of( picked[i] );
				s.after( 0.3f + 1.1f * i, () -> {
					GLog.i( "  " + l.of.getSimpleName() + " picked up the first time: a bloom in its palette" );
					GainFx.stagePickup( sprite( s ), l );
				} );
			}
		} );
	}

	private static void gain( FxStage s, float at, final String caption, final Item item, final EnchantLook l ){
		s.after( at, () -> {
			if (s.skeleton == null || s.skeleton.sprite == null) return;
			GLog.i( caption );
			Enchanting.play( s.skeleton.sprite, item.image(), l, -1, 0, null );
		} );
	}

	//the first look of a family that is no curse (the gallery's face of it)
	static EnchantLook face( Family f ){
		for (EnchantLook l : EnchantLooks.all()) if (l.fam == f && !l.curse) return l;
		return EnchantLooks.byCode( 0 );
	}

	private static CharSprite sprite( FxStage s ){
		return s.skeleton != null ? s.skeleton.sprite : null;
	}

	private static Sword cursed(){
		Sword w = new Sword();
		w.enchant( new Sacrificial() );
		w.cursed = w.cursedKnown = true;
		return w;
	}

	// ------------------------------------------------------------------ the pedestal

	/**
	 * fx-enchant-pedestal: the blank room round the hero (blank-room) with the enchanting pedestal
	 * beside him, an identified exotic sword (with its vestige) and a cloth armour in the pack, energy
	 * for every move, and the pedestal opened on the sword: reveal, take, empower, reroll and unweave
	 * all play there, and the vestige's line shows under its name.
	 */
	public static final class Pedestal implements DebugScenes.Scene {

		/** The energy it holds the hero to. */
		static final int ENERGY = 2000;

		@Override
		public String id(){
			return "fx-enchant-pedestal";
		}

		@Override
		public String title(){
			return "Effects: the enchanting pedestal, an exotic sword with a vestige on it";
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
			int cell = hero.pos + 2;
			if (cell >= 0 && cell < level.length() && Actor.findChar( cell ) == null && !level.solid[cell]){
				Level.set( cell, Terrain.PEDESTAL );
				GameScene.updateMap( cell );
				EnchantingStation station = new EnchantingStation();
				station.pos = cell;
				GameScene.add( station );
			}

			Sword sword = new Sword();
			Quality.roll( sword, Rarity.EXOTIC, null, 0 );
			sword.identify();
			sword.collect( hero.belongings.backpack );
			ClothArmor cloth = new ClothArmor();
			cloth.identify();
			cloth.collect( hero.belongings.backpack );
			Dungeon.energy = Math.max( Dungeon.energy, ENERGY );

			Weapon.Enchantment v = Quality.vestigeOf( sword );
			GLog.i( "Scene: the enchanting pedestal east of the hero, opened on an exotic sword"
					+ (v != null ? " whose vestige is " + v.name() : "") + "; a cloth armour in the pack and "
					+ ENERGY + " energy for every move." );
			EnchantingScene.open( sword );
		}
	}
}

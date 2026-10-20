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

import xyz.gabriwar.warpedpixeldungeon.actors.PrecipType;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Gnoll;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Rat;
import xyz.gabriwar.warpedpixeldungeon.debug.FxGallery;
import xyz.gabriwar.warpedpixeldungeon.debug.FxStage;
import xyz.gabriwar.warpedpixeldungeon.effects.FloatingText;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.Fx;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.Blow;
import xyz.gabriwar.warpedpixeldungeon.effects.fx.combat.HitFx;
import xyz.gabriwar.warpedpixeldungeon.items.Item;
import xyz.gabriwar.warpedpixeldungeon.items.armor.ClothArmor;
import xyz.gabriwar.warpedpixeldungeon.items.armor.LeatherArmor;
import xyz.gabriwar.warpedpixeldungeon.items.food.MysteryMeat;
import xyz.gabriwar.warpedpixeldungeon.items.materials.Log;
import xyz.gabriwar.warpedpixeldungeon.items.materials.RoughStone;
import xyz.gabriwar.warpedpixeldungeon.items.ore.IronOre;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.ItemType;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Quality;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.Rarity;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.RarityLine;
import xyz.gabriwar.warpedpixeldungeon.items.rarity.SlayerFamily;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.Weapon;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.enchantments.Shocking;
import xyz.gabriwar.warpedpixeldungeon.items.weapon.melee.Sword;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.WndInfoItem;
import com.watabou.noosa.tweeners.Tweener;
import com.watabou.utils.Random;

/**
 * The rarity lines' page of the enchantments' gallery (EnchantArea.exhibits; docs/fx/line-looks.md):
 * the gold chips the blow-changing lines put on the hit card, the looks of the lines that act on
 * their wearer, the extra yields' gold ghosts and the item hints. Restaged on puppets through the
 * looks' own entries (LineLooks.stage, PieceFx.extra, the blow's ledger): no real roll, nothing of
 * the game changed. Captions in the log.
 */
public final class LineExhibits {

	private LineExhibits(){}

	public static void exhibits( FxGallery.Page page ){

		page.add( "lines on the blow: their gold chips", 11f, s -> {
			final FxStage.Puppet striker = s.puppet( Gnoll.class, s.cell( -3, 1 ) );
			final FxStage.Puppet foe = s.puppet( Rat.class, s.cell( -2, 1 ) );
			if (striker == null || foe == null) return;
			final RarityLine[] lines = { RarityLine.EXECUTIONER, RarityLine.MOONLIT, RarityLine.STORMBORN, RarityLine.DEFIANT };
			s.after( 0.3f, () -> GLog.i( "  each line that lifted the blow: its gold chip on the card, no particles" ) );
			for (int i = 0; i < lines.length; i++){
				final RarityLine l = lines[i];
				s.after( 0.3f + 1.1f * i, () -> {
					GLog.i( "  " + l.shortName() );
					blow( striker, foe, 14, LineLooks.chipOf( l, null ) );
				} );
			}
			final SlayerFamily[] fams = SlayerFamily.values();
			for (int i = 0; i < fams.length; i++){
				final SlayerFamily f = fams[i];
				s.after( 4.8f + 0.8f * i, () -> {
					GLog.i( "  slayer of " + f.title() + ": the family's emblem" );
					blow( striker, foe, 17, LineLooks.chipOf( RarityLine.SLAYER, f ) );
				} );
			}
			s.after( 9.8f, () -> {
				GLog.i( "  five lines on one blow: three chips and +2" );
				blow( striker, foe, 23, LineLooks.chipOf( RarityLine.SLAYER, SlayerFamily.BEASTS ),
						LineLooks.chipOf( RarityLine.EXECUTIONER, null ), LineLooks.chipOf( RarityLine.MOONLIT, null ),
						LineLooks.chipOf( RarityLine.STORMBORN, null ), LineLooks.chipOf( RarityLine.DEFIANT, null ) );
			} );
		} );

		page.add( "lines on the body, only while they act", 10f, s -> {
			s.sky( PrecipType.RAIN, 0.6f, 12f, false );
			final FxStage.Puppet warm = s.puppet( Gnoll.class, s.cell( -5, -2 ) );
			final FxStage.Puppet cool = s.puppet( Gnoll.class, s.cell( -3, -2 ) );
			final FxStage.Puppet rain = s.puppet( Gnoll.class, s.cell( -1, -1 ) );
			final FxStage.Puppet wind = s.puppet( Gnoll.class, s.cell( 1, -1 ) );
			final FxStage.Puppet moon = s.puppet( Gnoll.class, s.cell( -6, 2 ) );
			final FxStage.Puppet fleet = s.puppet( Gnoll.class, s.cell( -1, 4 ) );
			final FxStage.Puppet hushed = s.puppet( Gnoll.class, s.cell( 3, 3 ) );
			final FxStage.Puppet sure = s.puppet( Gnoll.class, s.cell( 6, 3 ) );
			if (warm == null || cool == null || rain == null || wind == null || moon == null || fleet == null
					|| hushed == null || sure == null) return;
			s.after( 0.2f, () -> GLog.i( "  rain and wind held; each line's look on its own gnoll" ) );
			s.after( 0.3f, () -> {
				GLog.i( "  warm grip starting to warm him: embers off the hand and a warm afterglow, then a faint ember every 2 s" );
				LineLooks.stage( warm.sprite, LineLooks.WARM_GRIP, true );
			} );
			s.after( 0.9f, () -> {
				GLog.i( "  cooling starting to cool him: cool motes off the shoulders, then a breath of cool air sinking every 2.5 s" );
				LineLooks.stage( cool.sprite, LineLooks.COOLING, true );
			} );
			repeat( s, warm, LineLooks.WARM_GRIP, 2.3f );
			repeat( s, cool, LineLooks.COOLING, 3.4f );
			s.after( 1.5f, () -> GLog.i( "  rainproof holding the rain off: a bead rolls down his outline every 1.2 s" ) );
			repeat( s, rain, LineLooks.RAINPROOF, 1.5f );
			s.after( 2f, () -> GLog.i( "  windbreak: a wisp of the wind parting round his windward side every 2.5 s" ) );
			repeat( s, wind, LineLooks.WINDBREAK, 2f );
			s.after( 2.5f, () -> GLog.i( "  moonlit, by night only: moonlight drifting off the blade every 2 s (staged on the day pass too)" ) );
			repeat( s, moon, LineLooks.MOONLIT, 2.5f );
			s.after( 3f, () -> GLog.i( "  fleet: a little dust behind the heel every third step of a run, on dry floor" ) );
			for (int k = 1; k <= 6; k++){
				final int step = k, to = s.cell( -1 + (k <= 3 ? k : 6 - k), 4 );
				s.after( 3f + 0.25f * k, () -> {
					fleet.move( to );
					if (step % LineLooks.FLEET_EVERY == 0) LineLooks.stage( fleet.sprite, LineLooks.FLEET, false );
				} );
			}
			s.after( 5f, () -> GLog.i( "  hushed: a faint ring at his feet stepping near a foe that has not noticed him, at most every 1.5 s" ) );
			for (int k = 0; k < 4; k++){
				final int to = s.cell( 3 - (k % 2), 3 );
				//a step every 0.8 s rings on every other one, the rest inside the 1.5 s gap
				final boolean ring = k % 2 == 0;
				s.after( 5f + 0.8f * k, () -> {
					hushed.move( to );
					if (ring) LineLooks.stage( hushed.sprite, LineLooks.HUSHED, false );
				} );
			}
			s.after( 7.5f, () -> {
				GLog.i( "  surefoot: landing well from a fall, a ring and a glint at his feet" );
				sure.jump( s.cell( 6, 4 ) );
			} );
			s.after( 8f, () -> LineLooks.stage( sure.sprite, LineLooks.SUREFOOT, false ) );
		} );

		page.add( "extra yields: a gold ghost of what came extra", 5f, s -> {
			final int[] at = { s.cell( -4, 1 ), s.cell( -2, 1 ), s.cell( 0, 1 ), s.cell( -6, 1 ) };
			final Item[] items = { new Log(), new RoughStone(), new IronOre(), new MysteryMeat() };
			final float[] pitches = { 1.3f, 1.2f, 1.2f, 1f };
			for (int i = 0; i < 3; i++){
				final int k = i;
				s.after( 0.3f + 1f * i, () -> {
					GLog.i( "  an extra " + items[k].name() + ": its gold ghost rising out of the cell" );
					PieceFx.extra( at[k], items[k], pitches[k] );
				} );
			}
			s.after( 3.3f, () -> {
				GLog.i( "  a hunter's meat: the glint alone (the meat drops as it always did)" );
				PieceFx.extraGlint( at[3] );
			} );
		} );

		page.add( "item hints, and the info window teaching the chips", 7f, s -> {
			final Item[] hinted = { lined( new Sword(), RarityLine.WARM_GRIP ), lined( new LeatherArmor(), RarityLine.COOLING ),
					lined( new ClothArmor(), RarityLine.RAINPROOF ), lined( new LeatherArmor(), RarityLine.FLEET ) };
			s.after( 0.2f, () -> {
				GLog.i( "  warm grip, cooling, rainproof and fleet: one mote each, at the info window's pace (moonlit's glint by night only)" );
				for (int i = 0; i < hinted.length; i++) show( hinted[i], s.cell( -3 + 2 * i, -4 ), 6.6f );
			} );
			s.after( 1f, () -> {
				GLog.i( "  the item window: each line that puts a chip on the card leads its row with that chip, the vestige its hollow mark" );
				Sword w = lined( new Sword(), RarityLine.SLAYER, RarityLine.EXECUTIONER );
				Quality q = Quality.of( w );
				q.slayer = SlayerFamily.UNDEAD;
				q.vestige = new Shocking();
				q.lines.add( 0, RarityLine.VESTIGE );
				q.rolls.add( 0, 1f );
				final WndInfoItem wnd = new WndInfoItem( w );
				GameScene.show( wnd );
				//closed by the scene's own clock, not the page's timers: a page that ends first
				//(its timers dropped) still sees it go
				if (wnd.parent == null) return;
				wnd.parent.add( new Tweener( wnd, 5.5f ){
					@Override
					protected void updateValues( float progress ){}

					@Override
					protected void onComplete(){
						if (wnd.parent != null) wnd.hide();
					}
				} );
			} );
		} );
	}

	//a line's idle on a puppet every its pace, from `first` s to the exhibit's end
	private static void repeat( FxStage s, FxStage.Puppet p, int look, float first ){
		float every = LineLooks.EVERY[look];
		for (float t = first; t < 9.8f; t += every){
			s.after( t, () -> LineLooks.stage( p.sprite, look, false ) );
		}
	}

	//a blow of a on d through the ledger with these chips on its card (the lines' own entry is
	//Quality.edge; here their chips are put by hand, at any hour)
	private static void blow( FxStage.Puppet a, FxStage.Puppet d, int number, int... chips ){
		if (a.sprite.parent == null || d.sprite.parent == null) return;
		a.sprite.attack( d.ch.pos );
		Blow.begin( a.ch, d.ch );
		try {
			for (int c : chips) Blow.chip( d.ch, c, 0 );
			d.sprite.showStatusWithIcon( CharSprite.NEGATIVE, Integer.toString( number ), FloatingText.PHYS_DMG );
			d.sprite.flash();
			d.sprite.hitFx( a.sprite.x + a.sprite.width() / 2f, a.sprite.y + a.sprite.height() / 2f, number, HitFx.SLASH, false );
		} finally {
			Blow.end();
		}
	}

	//an item with these lines at a full roll, known as identified (the game's Random held apart, no
	//catalog touched)
	static <T extends Item> T lined( T item, RarityLine... lines ){
		Random.pushGenerator( 5 );
		try {
			item.quality = null;
			Quality.roll( item, Rarity.RARE, ItemType.GAMMA, 0 );
		} finally {
			Random.popGenerator();
		}
		Quality q = item.quality;
		q.lines.clear();
		q.rolls.clear();
		for (RarityLine l : lines){
			q.lines.add( l );
			q.rolls.add( 1f );
		}
		if (item instanceof Weapon) ((Weapon) item).enchant( null );
		item.levelKnown = item.cursedKnown = true;
		return item;
	}

	//an item as the info window shows it, on a cell for `life` s, then gone with its quads
	private static void show( Item item, int cell, float life ){
		if (cell == -1) return;
		final ItemSprite sp = new ItemSprite().mode( ItemSprite.Mode.PANEL );
		sp.view( item );
		sp.place( cell );
		GameScene.effect( sp );
		if (sp.parent == null) return;
		sp.parent.add( new Tweener( sp, life ){
			@Override
			protected void updateValues( float progress ){}

			@Override
			protected void onComplete(){
				Fx.dispose( sp );
			}
		} );
	}
}

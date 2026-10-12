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

package xyz.gabriwar.warpedpixeldungeon.homestead.travel;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Chrome;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.buffs.Hunger;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.WorldLayers;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.HomesteadSprites;
import xyz.gabriwar.warpedpixeldungeon.ui.Button;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.ScrollPane;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import xyz.gabriwar.warpedpixeldungeon.windows.IconTitle;
import xyz.gabriwar.warpedpixeldungeon.windows.WndOptions;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;

import java.util.List;

/**
 * The stones' travel window, opened at a hearthstone or a waystone: where the hero can go from it -
 * every home and attuned waystone on any slice but this one - each a row with its stone's picture,
 * its name, its slice, how far it is and what the trip costs in hunger (about how many meals, and the
 * exact hunger), with a warning when the trip would leave him hungry or starving, hurt him or kill
 * him (that one is refused). A tap on a row asks once more, with the cost, and then he goes
 * (Travel.go). The list scrolls; the window fits a phone held either way.
 */
public class WndTravel extends Window {

	private static final int WIDTH_P = 120, WIDTH_L = 160;
	private static final int GAP = 2;
	/** A home's name colour and a waystone's. */
	static final int HOME_COLOR = 0xFFD27F, STONE_COLOR = Travel.RUNE;
	static final int HUNGRY_COLOR = 0xFFAA44, STARVING_COLOR = 0xFF5544;

	private final Travel.Place from;

	public WndTravel( Travel.Place from ){
		super();
		this.from = from;

		int width = PixelScene.landscape() ? WIDTH_L : WIDTH_P;

		IconTitle title = new IconTitle( icon( from.kind ), Messages.get( this, "title", from.name ) );
		title.setRect( 0, 0, width, 0 );
		add( title );
		float top = title.bottom() + GAP * 2;

		Hunger hunger = Dungeon.hero == null ? null : Dungeon.hero.buff( Hunger.class );
		RenderedTextBlock intro = PixelScene.renderTextBlock( Messages.get( this, "intro",
				hunger == null ? 0 : hunger.hunger(), (int)Hunger.HUNGRY, (int)Hunger.STARVING ), 6 );
		intro.maxWidth( width );
		intro.setPos( 0, top );
		add( intro );
		top = intro.bottom() + GAP * 2;

		List<Travel.Place> places = Travel.destinations( from );
		if (places.isEmpty()){
			RenderedTextBlock none = PixelScene.renderTextBlock( Messages.get( this, "nowhere" ), 6 );
			none.maxWidth( width );
			none.setPos( 0, top );
			add( none );
			resize( width, (int)Math.ceil( none.bottom() ) );
			return;
		}

		//the pane before its rows: pointer listeners fire newest first, so a pane made after the
		//rows under it would take their taps (see WndDebugScenes); dragged over them it scrolls
		ScrollPane pane = new ScrollPane( new Component() );
		Component content = pane.content();
		float pos = 0;
		for (final Travel.Place to : places){
			Row row = new Row( to, width );
			row.setRect( 0, pos, width, row.reqHeight() );
			content.add( row );
			pos = row.bottom() + GAP;
		}
		content.setSize( width, Math.max( 0, pos - GAP ) );
		pane.dragOverButtons();

		int maxH = (int)(PixelScene.uiCamera.height - chrome.marginVer() - 20);
		float paneH = Math.min( content.height(), Math.max( 40, maxH - top ) );

		//resize before the pane goes in, for its camera
		resize( width, (int)Math.ceil( top + paneH ) );
		add( pane );
		pane.setRect( 0, top, width, paneH );
	}

	/** The picture of a stone of a kind: a hearthstone, or an attuned waystone. */
	static Image icon( Travel.Kind kind ){
		Image img = new Image( HomesteadSprites.TEXTURE );
		img.frame( HomesteadSprites.film().get( kind == Travel.Kind.HOME ? HomesteadSprites.HEARTHSTONE : HomesteadSprites.WAYSTONE_ATTUNED ) );
		return img;
	}

	/** Where a slice is, in words. */
	static String slice( int depth ){
		int alt = WorldLayers.altitudeOf( depth );
		if (alt > 0) return Messages.get( WndTravel.class, "slice_up", alt );
		if (alt < 0) return Messages.get( WndTravel.class, "slice_down", -alt );
		return Messages.get( WndTravel.class, "slice_surface" );
	}

	//the warning on a trip's cost, or null: one that would starve him to death (Travel refuses it),
	//one that hurts him past starving, or one that leaves him starving or hungry
	private static String warning( Hero hero, int cost ){
		if (hero == null) return null;
		if (Travel.deadly( hero, cost )) return Messages.get( WndTravel.class, "warn_deadly" );
		int hurt = Travel.hurt( hero, cost );
		if (hurt > 0) return Messages.get( WndTravel.class, "warn_hurt", hurt );
		int leaves = Travel.leaves( hero, cost );
		return leaves == 2 ? Messages.get( WndTravel.class, "warn_starving" )
				: leaves == 1 ? Messages.get( WndTravel.class, "warn_hungry" ) : null;
	}

	//the warning's colour: starving's red when it leaves him starving or worse, hungry's amber else
	private static int warningColor( Hero hero, int cost ){
		return Travel.leaves( hero, cost ) == 2 ? STARVING_COLOR : HUNGRY_COLOR;
	}

	private void choose( final Travel.Place to ){
		final int cost = Travel.cost( from, to );
		String text = Messages.get( this, "confirm_cost", Travel.meals( cost ), cost );
		String warn = warning( Dungeon.hero, cost );
		if (warn != null) text += "\n\n" + warn;
		GameScene.show( new WndOptions( icon( to.kind ), Messages.get( this, "confirm_title", to.name ), text,
				Messages.get( this, "go" ), Messages.get( this, "stay" ) ){
			@Override
			protected void onSelect( int index ){
				if (index != 0) return;
				WndTravel.this.hide();
				if (Dungeon.hero != null) Travel.go( Dungeon.hero, from, to );
			}
		} );
	}

	//a place to go: its stone, its name, where it is, what it costs, and any warning
	private class Row extends Button {

		private final Travel.Place to;
		private NinePatch bg;
		private Image stone;
		private RenderedTextBlock name, where, cost, warn;

		Row( Travel.Place to, int width ){
			super();
			this.to = to;

			bg = Chrome.get( Chrome.Type.GREY_BUTTON_TR );
			addToBack( bg );

			stone = icon( to.kind );
			add( stone );

			int textW = width - 16 - 6 - (int)bg.marginHor();
			name = PixelScene.renderTextBlock( to.name, 8 );
			name.hardlight( to.kind == Travel.Kind.HOME ? HOME_COLOR : STONE_COLOR );
			name.maxWidth( textW );
			add( name );

			where = PixelScene.renderTextBlock( Messages.get( WndTravel.class,
					to.kind == Travel.Kind.HOME ? "row_home" : "row_waystone",
					slice( to.depth ), Travel.distance( from, to ) ), 6 );
			where.maxWidth( textW );
			add( where );

			int c = Travel.cost( from, to );
			cost = PixelScene.renderTextBlock( Messages.get( WndTravel.class, "row_cost", Travel.meals( c ), c ), 6 );
			cost.maxWidth( textW );
			add( cost );

			String w = warning( Dungeon.hero, c );
			if (w != null){
				warn = PixelScene.renderTextBlock( w, 6 );
				warn.hardlight( warningColor( Dungeon.hero, c ) );
				warn.maxWidth( textW );
				add( warn );
			}
		}

		float reqHeight(){
			float h = name.height() + 1 + where.height() + 1 + cost.height();
			if (warn != null) h += 1 + warn.height();
			return Math.max( 20, h + bg.marginVer() + 2 );
		}

		@Override
		protected void layout(){
			super.layout();
			bg.x = x;
			bg.y = y;
			bg.size( width, height );

			stone.x = x + bg.marginLeft() + 1;
			stone.y = y + (height - stone.height()) / 2f;
			PixelScene.align( stone );

			float tx = stone.x + 16 + 4;
			float ty = y + bg.marginTop() + 1;
			name.setPos( tx, ty );
			ty = name.bottom() + 1;
			where.setPos( tx, ty );
			ty = where.bottom() + 1;
			cost.setPos( tx, ty );
			if (warn != null) warn.setPos( tx, cost.bottom() + 1 );
		}

		@Override
		protected void onPointerDown(){
			bg.brightness( 1.2f );
			Sample.INSTANCE.play( Assets.Sounds.CLICK );
		}

		@Override
		protected void onPointerUp(){
			bg.resetColor();
		}

		@Override
		protected void onClick(){
			if (!Travel.stillThere( to )){
				GLog.w( Travel.Refusal.GONE.message() );
				return;
			}
			choose( to );
		}
	}
}

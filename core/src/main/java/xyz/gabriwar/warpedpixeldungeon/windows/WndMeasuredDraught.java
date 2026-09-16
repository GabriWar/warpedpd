/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
 *
 * Sprouted Pixel Dungeon
 * Copyright (C) 2015 dachhack
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

package xyz.gabriwar.warpedpixeldungeon.windows;

import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.items.Waterskin;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.ItemSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.OptionSlider;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;

/**
 * The Tinkerer's measured draught: drag to choose how many drops to drink, see
 * exactly how much health and mana they give, then drink that and no more.
 */
public class WndMeasuredDraught extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final float GAP      = 2;

	private final RenderedTextBlock preview;

	public WndMeasuredDraught( final Waterskin skin, final Hero hero ) {

		super();

		IconTitle titlebar = new IconTitle();
		titlebar.icon( new ItemSprite( skin.image(), null ) );
		titlebar.label( Messages.titleCase( skin.name() ) );
		titlebar.setRect( 0, 0, WIDTH, 0 );
		add( titlebar );

		final int volume = skin.getVolume();

		final OptionSlider slider = new OptionSlider( Messages.get(this, "drops"),
				"1", Integer.toString( volume ), 1, volume ) {
			@Override
			protected void onChange() {
				updatePreview( skin, hero, getSelectedValue() );
			}
		};
		slider.setRect( 0, titlebar.bottom() + GAP, WIDTH, 24 );
		add( slider );

		preview = PixelScene.renderTextBlock( 6 );
		preview.maxWidth( WIDTH );
		preview.setPos( 0, slider.bottom() + GAP );
		add( preview );

		//start on what it takes to top up both pools, capped at what is in the skin
		int forHP = (int) Math.ceil( (hero.HT - hero.HP) / (hero.HT * Waterskin.DROP_FRACTION) );
		int forMP = (int) Math.ceil( Math.max( 0, hero.MT - hero.MP ) / Math.max( 1f, hero.MT * Waterskin.DROP_FRACTION ) );
		int suggested = Math.max( 1, Math.min( volume, Math.max( forHP, forMP ) ) );
		slider.setSelectedValue( suggested );
		updatePreview( skin, hero, suggested );

		RedButton btnDrink = new RedButton( Messages.get(this, "drink") ) {
			@Override
			protected void onClick() {
				hide();
				skin.drinkMeasured( hero, slider.getSelectedValue() );
			}
		};
		btnDrink.setRect( 0, preview.bottom() + GAP * 2, (WIDTH - GAP) / 2, BTN_HEIGHT );
		add( btnDrink );

		RedButton btnCancel = new RedButton( Messages.get(this, "cancel") ) {
			@Override
			protected void onClick() {
				hide();
			}
		};
		btnCancel.setRect( btnDrink.right() + GAP, btnDrink.top(), (WIDTH - GAP) / 2, BTN_HEIGHT );
		add( btnCancel );

		resize( WIDTH, (int) btnDrink.bottom() );
	}

	private void updatePreview( Waterskin skin, Hero hero, int drops ){
		preview.text( Messages.get(this, "preview", drops,
				skin.measuredHeal( hero, drops ), skin.measuredMana( hero, drops )) );
		preview.maxWidth( WIDTH );
	}
}

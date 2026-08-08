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

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.Hero;
import xyz.gabriwar.warpedpixeldungeon.actors.hero.HeroClass;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.RedButton;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import xyz.gabriwar.warpedpixeldungeon.ui.Window;
import xyz.gabriwar.warpedpixeldungeon.utils.GLog;
import com.watabou.utils.Random;

import java.util.Locale;

public class WndLevelUp extends Window {

	private static final int WIDTH      = 120;
	private static final int BTN_HEIGHT = 18;
	private static final float GAP      = 2;

	public WndLevelUp( Hero hero ) {

		super();

		IconTitle title = new IconTitle();
		title.icon( HeroSprite.avatar( hero.heroClass, hero.tier() ) );
		title.label( Messages.get(this, "title", hero.lvl, hero.className())
				.toUpperCase( Locale.ENGLISH ) );
		title.color( Window.SHPX_COLOR );
		title.setRect( 0, 0, WIDTH, 0 );
		add( title );

		final int strpts = getStr( hero );
		final int magpts = getMag( hero );
		final int spdpts = getSpd( hero );

		RenderedTextBlock message = PixelScene.renderTextBlock(
				Messages.get(this, "message"), 6 );
		message.maxWidth( WIDTH );
		message.setPos( 0, title.bottom() + GAP );
		add( message );

		RedButton btnStr = new RedButton(
				Messages.get(this, "str", strpts).toUpperCase( Locale.ENGLISH ) ) {
			@Override
			protected void onClick() {
				Dungeon.hero.levelup = false;
				Dungeon.hero.STR += strpts;
				Dungeon.hero.sprite.showStatus( CharSprite.POSITIVE,
						Messages.get(WndLevelUp.class, "str_status", strpts) );
				GLog.p( Messages.get(WndLevelUp.class, "str_msg") );
				hide();
			}
		};
		btnStr.setRect( 0, message.top() + message.height() + GAP, WIDTH, BTN_HEIGHT );
		add( btnStr );

		RedButton btnMagic = new RedButton(
				Messages.get(this, "magic", magpts).toUpperCase( Locale.ENGLISH ) ) {
			@Override
			protected void onClick() {
				Dungeon.hero.levelup = false;
				Dungeon.hero.MT += 2;
				Dungeon.hero.magicLevel += magpts;
				Dungeon.hero.sprite.showStatus( CharSprite.POSITIVE,
						Messages.get(WndLevelUp.class, "magic_status", magpts) );
				GLog.p( Messages.get(WndLevelUp.class, "magic_msg") );
				hide();
			}
		};
		btnMagic.setRect( 0, btnStr.bottom() + GAP, WIDTH, BTN_HEIGHT );
		add( btnMagic );

		RedButton btnSpeed = new RedButton(
				Messages.get(this, "speed", spdpts).toUpperCase( Locale.ENGLISH ) ) {
			@Override
			protected void onClick() {
				Dungeon.hero.levelup = false;
				Dungeon.hero.speedLevel += spdpts;
				Dungeon.hero.sprite.showStatus( CharSprite.POSITIVE,
						Messages.get(WndLevelUp.class, "speed_status", spdpts) );
				GLog.p( Messages.get(WndLevelUp.class, "speed_msg") );
				hide();
			}
		};
		btnSpeed.setRect( 0, btnMagic.bottom() + GAP, WIDTH, BTN_HEIGHT );
		add( btnSpeed );

		resize( WIDTH, (int) btnSpeed.bottom() );
	}

	private int getStr( Hero hero ) {
		if (hero.heroClass == HeroClass.WARRIOR)  return Random.IntRange( 1, 3 );
		if (hero.heroClass == HeroClass.ROGUE)    return Random.IntRange( 1, 2 );
		if (hero.heroClass == HeroClass.MAGE)     return Random.IntRange( 1, 1 );
		if (hero.heroClass == HeroClass.HUNTRESS) return Random.IntRange( 1, 3 );
		return 1;
	}

	private int getMag( Hero hero ) {
		if (hero.heroClass == HeroClass.WARRIOR)  return Random.IntRange( 1, 1 );
		if (hero.heroClass == HeroClass.ROGUE)    return Random.IntRange( 1, 2 );
		if (hero.heroClass == HeroClass.MAGE)     return Random.IntRange( 1, 3 );
		if (hero.heroClass == HeroClass.HUNTRESS) return Random.IntRange( 1, 2 );
		return 1;
	}

	private int getSpd( Hero hero ) {
		if (hero.heroClass == HeroClass.WARRIOR)  return Random.IntRange( 1, 2 );
		if (hero.heroClass == HeroClass.ROGUE)    return Random.IntRange( 1, 3 );
		if (hero.heroClass == HeroClass.MAGE)     return Random.IntRange( 1, 2 );
		if (hero.heroClass == HeroClass.HUNTRESS) return Random.IntRange( 1, 2 );
		return 1;
	}
}

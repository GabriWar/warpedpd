/*
 * Pixel Dungeon
 * Copyright (C) 2012-2015 Oleg Dolya
 *
 * Shattered Pixel Dungeon
 * Copyright (C) 2014-2026 Evan Debenham
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

import xyz.gabriwar.warpedpixeldungeon.actors.TileTemperature;
import xyz.gabriwar.warpedpixeldungeon.actors.mobs.Mob;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.sprites.CharSprite;
import xyz.gabriwar.warpedpixeldungeon.ui.BuffIndicator;
import xyz.gabriwar.warpedpixeldungeon.ui.HealthBar;
import xyz.gabriwar.warpedpixeldungeon.ui.RenderedTextBlock;
import com.watabou.noosa.ui.Component;

public class WndInfoMob extends WndTitledMessage {
	
	public WndInfoMob( Mob mob ) {

		super( new MobTitle( mob ), mob.info() + tempInfo(mob) );

	}

	private static String tempInfo( Mob mob ) {
		float tileT = TileTemperature.tileTemp(mob.pos);
		String info = "\n\n_Tile temp:_ " + Messages.decimalFormat("#.#", tileT) + "°C";
		if (!Float.isNaN(mob.bodyTemp)) {
			info += "\n_Body temp:_ " + Messages.decimalFormat("#.#", mob.bodyTemp) + "°C";
		}
		return info;
	}
	
	private static class MobTitle extends Component {

		private static final int GAP	= 2;
		
		private CharSprite image;
		private RenderedTextBlock name;
		private HealthBar health;
		private BuffIndicator buffs;
		
		public MobTitle( Mob mob ) {
			
			name = PixelScene.renderTextBlock( Messages.titleCase( mob.name() ), 9 );
			name.hardlight( TITLE_COLOR );
			add( name );
			
			image = mob.sprite();
			add( image );

			health = new HealthBar();
			health.level(mob);
			add( health );

			buffs = new BuffIndicator( mob, false );
			add( buffs );
		}
		
		@Override
		protected void layout() {
			
			image.x = 0;
			image.y = Math.max( 0, name.height() + health.height() - image.height() );

			float w = width - image.width() - GAP;

			name.setPos(x + image.width() + GAP,
					image.height() > name.height() ? y +(image.height() - name.height()) / 2 : y);

			health.setRect(image.width() + GAP, name.bottom() + GAP, w, health.height());

			buffs.maxBuffs = 50; //infinite, effectively
			buffs.setRect(name.right(), name.bottom() - BuffIndicator.SIZE_SMALL-2, w - name.width(), 8);

			//If buff bar doesn't have enough room, move it below
			if (!buffs.allBuffsVisible()){
				buffs.setRect(0, health.bottom(), width, 8);
				height = Math.max(image.y + image.height(), buffs.bottom());
			} else {
				height = Math.max(image.y + image.height(), health.bottom());
			}
		}
	}
}

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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Game;
import com.watabou.noosa.ui.Component;

public class CurrencyIndicator extends Component {

	private static final float TIME	= 2f;
	
	private int lastGold = 0;
	private int lastEnergy = 0;
	private int lastBullet = 0;
	
	private BitmapText gold;
	private BitmapText energy;
	private BitmapText bullet;
	
	private float goldTime;
	private float energyTime;
	private float bulletTime;

	public static boolean showGold = false;
	
	@Override
	protected void createChildren() {
		gold = new BitmapText( PixelScene.pixelFont);
		gold.text( Integer.toString(lastGold) );
		gold.measure();
		gold.hardlight( 0xFFFF00 );
		add( gold );

		energy = new BitmapText( PixelScene.pixelFont);
		energy.text( Integer.toString(lastEnergy) );
		energy.measure();
		energy.hardlight( 0x44CCFF );
		add( energy );

		bullet = new BitmapText( PixelScene.pixelFont);
		bullet.text( Integer.toString(lastBullet) );
		bullet.measure();
		bullet.hardlight( 0xFFFFFF );
		add( bullet );
		
		gold.visible = energy.visible = bullet.visible = false;
	}
	
	@Override
	protected void layout() {
		gold.x = x+1;
		gold.y = top() + 1;

		energy.x = x+1;
		if (gold.visible) {
			energy.y = top() + energy.height() - 1;
		} else {
			energy.y = top() + 1;
		}

		bullet.x = x+1;
		bullet.y = energy.y + (energy.visible ? bullet.height() - 2 : 0);
	}
	
	@Override
	public void update() {
		super.update();
		
		if (gold.visible) {
			
			goldTime -= Game.elapsed;
			if (goldTime > 0) {
				gold.alpha( goldTime > TIME / 2 ? 1f : goldTime * 2 / TIME );
			} else {
				gold.visible = false;
			}
			
		}

		if (energy.visible) {

			energyTime -= Game.elapsed;
			if (energyTime > 0) {
				energy.alpha( energyTime > TIME / 2 ? 1f : energyTime * 2 / TIME );
			} else {
				energy.visible = false;
			}

		}

		if (bullet.visible) {

			bulletTime -= Game.elapsed;
			if (bulletTime > 0) {
				bullet.alpha( bulletTime > TIME / 2 ? 1f : bulletTime * 2 / TIME );
			} else {
				bullet.visible = false;
			}

		}

		if (Dungeon.gold != lastGold) {
			
			lastGold = Dungeon.gold;
			
			gold.text( Integer.toString(lastGold) );
			gold.measure();
			
			gold.visible = true;
			goldTime = TIME;
			
			layout();
		}

		if (Dungeon.energy != lastEnergy) {
			lastEnergy = Dungeon.energy;

			energy.text( Integer.toString(lastEnergy) );
			energy.measure();

			energy.visible = true;
			energyTime = TIME;

			layout();
		}

		if (Dungeon.bullet != lastBullet) {
			lastBullet = Dungeon.bullet;

			bullet.text( Integer.toString(lastBullet) );
			bullet.measure();

			bullet.visible = true;
			bulletTime = TIME;

			layout();
		}

		if (showGold){
			if (!gold.visible){
				gold.visible = true;
				layout();
			}
			goldTime = TIME/2;
		}

	}
}

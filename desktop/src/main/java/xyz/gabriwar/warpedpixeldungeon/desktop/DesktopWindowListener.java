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

package xyz.gabriwar.warpedpixeldungeon.desktop;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Window;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3WindowListener;
import xyz.gabriwar.warpedpixeldungeon.WPDSettings;
import com.watabou.noosa.audio.Music;

public class DesktopWindowListener implements Lwjgl3WindowListener {
	
	@Override
	public void created ( Lwjgl3Window lwjgl3Window ) { }
	
	@Override
	public void maximized ( boolean b ) {
		WPDSettings.windowMaximized( b );
		if (b){
			WPDSettings.windowResolution(DesktopPlatformSupport.previousSizes[1]);
		}
	}
	
	@Override
	public void iconified ( boolean b ) { }
	public void focusLost () {
		if (!WPDSettings.playMusicInBackground()) {
			Music.INSTANCE.pause();
		}
	}
	public void focusGained () {
		if (!WPDSettings.playMusicInBackground()){
			Music.INSTANCE.resume();
		}
	}
	public boolean closeRequested () { return true; }
	public void filesDropped ( String[] strings ) { }
	public void refreshRequested () { }
}

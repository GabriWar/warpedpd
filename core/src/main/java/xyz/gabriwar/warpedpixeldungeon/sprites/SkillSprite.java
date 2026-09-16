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
 * Skill system ported from Skillful Pixel Dungeon by bilboldev (Moussa)
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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

public class SkillSprite extends Image {

	public static final int SIZE = 16;

	private static TextureFilm film;

	private int image = -1;
    private boolean inactive;

	public SkillSprite(){
		super( Assets.Sprites.HERO_SKILLS );
		if (film == null){
			film = new TextureFilm( texture, SIZE, SIZE );
		}
	}

	public SkillSprite( int image ){
		this();
		view( image );
	}

	public SkillSprite view( int image ){
		this.image = image;
		frame( film.get( image ) );
		return this;
	}

	/**
	 * Drains the colour out of this icon, or puts it back. A skill you switch
	 * on and off is drawn grey while it is off, so a glance at the tree says
	 * which ones are running.
	 */
	public SkillSprite grey( boolean grey ){
        inactive=grey;
		texture( grey ? greyscale() : TextureCache.get( Assets.Sprites.HERO_SKILLS ) );
		if (image >= 0) frame( film.get( image ) );
		return this;
	}

    @Override public void update(){
        super.update();
        SmartTexture current=inactive?greyscale():TextureCache.get(Assets.Sprites.HERO_SKILLS);
        if(texture!=current){texture(current);if(image>=0)frame(film.get(image));}
    }

    // Asset-backed like every other sprite: cache clear, context reload and new windows
    // all acquire the current texture instead of holding a deleted generated texture.
    private static SmartTexture greyscale(){
        return TextureCache.get("sprites/hero_skills_grey.png");
    }
}

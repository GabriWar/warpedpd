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

package xyz.gabriwar.warpedpixeldungeon.sprites;

import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.Char;
import xyz.gabriwar.warpedpixeldungeon.actors.WorldClock;
import xyz.gabriwar.warpedpixeldungeon.bounty.Bounties;
import xyz.gabriwar.warpedpixeldungeon.bounty.BountyShare;
import xyz.gabriwar.warpedpixeldungeon.bounty.NoticeBoard;
import xyz.gabriwar.warpedpixeldungeon.levels.overworld.OverworldLevel;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.TextureFilm;

/**
 * A village's notice board (bounty/NoticeBoard, sprites/notice_board.png from
 * tools/assets/bounty_art.py): the board on its legs under its little roof, with as many notices
 * pinned up as the board still shows (0 to 3), and a gold coin hung under it while a reward waits
 * there for the hero. What it shows is asked a couple of times a second, so it follows the board on
 * any screen: on a co-op guest's the board is a mirror that knows only where it stands, and the
 * host's record shipped to the guest (BountyShare) says the rest. Furniture: it never turns, and its
 * run, attack and death are clones of its idle, never the idle itself.
 */
public class NoticeBoardSprite extends MobSprite {

	public static final String TEXTURE = "sprites/notice_board.png";
	/** Frames: 0..3 notices up, then the same with the reward's coin. */
	public static final int FRAMES = 8, COIN = 4;
	//how often the board is read again, seconds
	private static final float CHECK_EVERY = 0.5f;

	private final Animation[] looks = new Animation[FRAMES];
	private int showing = -1;
	private float checkIn;

	public NoticeBoardSprite(){
		super();

		texture( TEXTURE );
		TextureFilm film = new TextureFilm( texture, 16, 16 );
		for (int i = 0; i < FRAMES; i++){
			looks[i] = new Animation( 1, true );
			looks[i].frames( film, i );
		}

		idle = looks[3];
		//never the idle instance itself: a looped idle that IS attack fires onAttackComplete
		//every cycle (see TemperingAnvilSprite)
		run = idle.clone();
		attack = idle.clone();
		die = idle.clone();

		play( idle );
	}

	/** The board with its three notices up, for a window's title or a list. */
	public static Image icon(){
		Image img = new Image( TEXTURE );
		img.frame( img.texture.uvRectBySize( 16 * 3, 0, 16, 16 ) );
		return img;
	}

	@Override
	public void link( Char ch ){
		super.link( ch );
		renderShadow = false;
		checkIn = 0;
	}

	@Override
	public void update(){
		super.update();
		checkIn -= Game.elapsed;
		if (checkIn <= 0 && ch != null){
			checkIn = CHECK_EVERY;
			show( frame( ch ) );
		}
	}

	//what the board of this char shows now: the notices still up and the coin of a reward waiting
	private static int frame( Char ch ){
		if (!(Dungeon.level instanceof OverworldLevel)) return 3;
		OverworldLevel ow = (OverworldLevel) Dungeon.level;
		long village;
		if (ch instanceof NoticeBoard){
			village = ((NoticeBoard) ch).key;
		} else {
			if (ch.pos < 0 || ch.pos >= ow.length()) return 3;
			village = Bounties.villageAt( ow.worldSeed, ow.worldX + ch.pos % ow.width(), ow.worldY + ch.pos / ow.width(), 2 );
		}
		if (village == Long.MIN_VALUE) return 3;
		int open = Math.min( 3, Bounties.openOffers( ow, village, WorldClock.day(), BountyShare.world() ).size() );
		return open + (BountyShare.readyAt( village ) ? COIN : 0);
	}

	private void show( int frame ){
		if (frame == showing) return;
		showing = frame;
		idle = looks[frame];
		play( idle, true );
	}

	@Override
	public void turnTo( int from, int to ){
		//a board faces the well, always
	}

	@Override
	public int blood(){
		return 0xFF7A522E;
	}
}

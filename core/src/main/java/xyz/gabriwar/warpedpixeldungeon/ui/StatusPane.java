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

import com.watabou.noosa.ColorBlock;
import xyz.gabriwar.warpedpixeldungeon.Assets;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.WPDAction;
import xyz.gabriwar.warpedpixeldungeon.Statistics;
import xyz.gabriwar.warpedpixeldungeon.actors.Actor;
import xyz.gabriwar.warpedpixeldungeon.effects.CircleArc;
import xyz.gabriwar.warpedpixeldungeon.effects.Speck;
import xyz.gabriwar.warpedpixeldungeon.messages.Messages;
import xyz.gabriwar.warpedpixeldungeon.scenes.GameScene;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import xyz.gabriwar.warpedpixeldungeon.items.rings.RingOfMagic;
import xyz.gabriwar.warpedpixeldungeon.sprites.HeroSprite;
import xyz.gabriwar.warpedpixeldungeon.windows.WndHero;
import xyz.gabriwar.warpedpixeldungeon.windows.WndKeyBindings;
import com.watabou.input.GameAction;
import com.watabou.noosa.BitmapText;
import com.watabou.noosa.Camera;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import com.watabou.noosa.NinePatch;
import com.watabou.noosa.particles.Emitter;
import com.watabou.noosa.ui.Component;
import com.watabou.gltextures.TextureCache;
import com.watabou.utils.ColorMath;
import com.watabou.utils.GameMath;

public class StatusPane extends Component {

	private NinePatch bg;
	private Image avatar;
	private Button heroInfo;
	public static float talentBlink;
	private float warning;

	public static final float FLASH_RATE = (float)(Math.PI*1.5f); //1.5 blinks per second

	private int lastTier = 0;

	private Image shieldHP;
	private Image hp;
	private Image mpTrack;
	private Image mp;
	private Image mpCap;
	private Image mpBack;
	private int mpCapW = 2;
	private float mpPulse = 0;

	private static final Object MANA_KEY = "status-mana";
	private float mpBarWidth;
	private BitmapText mpText;
	private Image Dot; //a visual darkening over HP and shield that shows total incoming DOT
	private BitmapText hpText;
	private Button heroInfoOnBar;

	private Image exp;
	private BitmapText expText;

	private int lastLvl = -1;

	private BitmapText level;

	private BuffIndicator buffs;
	private Compass compass;

	private BusyIndicator busy;
	private CircleArc counter;

	private boolean large;

	//potentially extends the hero portrait space to avoid some cutouts
	public static float heroPaneExtraWidth = 0;
	private NinePatch heroPaneCutout;
	//potentially shrinks and/or repositions the hp bar to avoid some cutouts
	public static int hpBarMaxWidth = 50;
	private Image hpCutout;
	//potentially adjusts the row(s) of the the buff indicator to avoid some cutouts
	public static float[] buffBarRowMaxWidths;
	public static float[] buffBarRowAdjusts;

	public StatusPane( boolean large ){
		super();

		String asset = Assets.Interfaces.STATUS;

		this.large = large;

		if (large)  bg = new NinePatch( asset, 0, 64, 41, 39, 33, 0, 4, 0 );
		else        bg = new NinePatch( asset, 0,  0, 82, 38, 32, 0, 5, 0 );
		add( bg );

		heroPaneCutout = new NinePatch(asset, 0, 0, 5, 36, 4, 0, 0, 0);
		heroPaneCutout.visible = false;
		add(heroPaneCutout);

		hpCutout = new Image(asset, 90, 0, 12, 9);
		hpCutout.visible = false;
		add(hpCutout);

		heroInfo = new Button(){
			@Override
			protected void onClick () {
				Camera.main.panTo( Dungeon.hero.sprite.center(), 5f );
				GameScene.show( new WndHero() );
			}
			
			@Override
			public GameAction keyAction() {
				return WPDAction.HERO_INFO;
			}

			@Override
			protected String hoverText() {
				return Messages.titleCase(Messages.get(WndKeyBindings.class, "hero_info"));
			}
		};
		add(heroInfo);

		avatar = HeroSprite.avatar( Dungeon.hero );
		add( avatar );

		talentBlink = 0;

		compass = new Compass( Statistics.amuletObtained ? Dungeon.level.entrance() : Dungeon.level.exit() );
		add( compass );

		if (large)  shieldHP = new Image(asset, 0, 112, 128, 9);
		else        shieldHP = new Image(asset, 0, 44, 50, 4);
		add(shieldHP);

		if (large)  hp = new Image(asset, 0, 103, 128, 9);
		else        hp = new Image(asset, 0, 40, 50, 4);
		add( hp );

		if (large)  Dot = new Image(asset, 0, 103, 128, 9);
		else        Dot = new Image(asset, 0, 40, 50, 4);
		Dot.hardlight(0, 0, 0);
		Dot.alpha(0.25f);
		add( Dot );

		hpText = new BitmapText(PixelScene.pixelFont);
		hpText.alpha(0.6f);
		add(hpText);

		//skills system: a mana bar tucked between health and experience. The dark track
		//keeps it findable when empty; the blue count rides the right end of the hp bar
		//painted like the health bar above it: a lit top row over a deeper blue, a dark
		//sunken track behind it, the same rounded end; it flashes when mana moves
		manaArt();
		mpBack = new Image( MANA_KEY );
		mpBack.frame( 8, 18, 1, 5 );
		add( mpBack );
		mpTrack = new Image( MANA_KEY );
		mpTrack.frame( 0, 11, 128, 5 );
		add( mpTrack );
		mp = new Image( MANA_KEY );
		mp.frame( 0, 0, 128, 5 );
		add( mp );
		mpCap = new Image( MANA_KEY );
		mpCap.frame( 0, 18, 3, 5 );
		add( mpCap );

		mpText = new BitmapText(PixelScene.pixelFont);
		mpText.hardlight( 0xA8D8FF );
		mpText.alpha(1f);
		add(mpText);

		heroInfoOnBar = new Button(){
			@Override
			protected void onClick () {
				Camera.main.panTo( Dungeon.hero.sprite.center(), 5f );
				GameScene.show( new WndHero() );
			}
		};
		add(heroInfoOnBar);

		if (large)  exp = new Image(asset, 0, 121, 128, 7);
		else        exp = new Image(asset, 0, 48, 17, 4);
		add( exp );

		expText = new BitmapText(PixelScene.pixelFont);
		expText.hardlight( 0xFFFFAA );
		expText.alpha(0.6f);
		add(expText);

		level = new BitmapText( PixelScene.pixelFont);
		level.hardlight( 0xFFFFAA );
		add( level );

		buffs = new BuffIndicator( Dungeon.hero, large );
		add( buffs );

		busy = new BusyIndicator();
		add( busy );

		counter = new CircleArc(18, 4.25f);
		counter.color( 0x808080, true );
		counter.show(this, busy.center(), 0f);
	}

	@Override
	protected void layout() {

		height = large ? 39 : 38;

		float heroPaneWidth = 30 + heroPaneExtraWidth;

		bg.x = x + heroPaneExtraWidth;
		bg.y = y;
		if (large)  bg.size( 160, bg.height ); //HP bars must be 128px wide atm
		else        bg.size(hpBarMaxWidth+32, bg.height ); //default max right is 50px health bar + 32

		avatar.x = bg.x - avatar.width / 2f + 15;
		avatar.y = bg.y - avatar.height / 2f + 16;
		PixelScene.align(avatar);

		heroInfo.setRect( x, y, heroPaneWidth, large ? 40 : 36 );

		compass.x = avatar.x + avatar.width / 2f - compass.origin.x;
		compass.y = avatar.y + avatar.height / 2f - compass.origin.y;
		PixelScene.align(compass);

		if (large) {
			exp.x = x + 30;
			exp.y = y + 32;

			//health two rows higher than vanilla, to make room for mana above experience
			hp.x = shieldHP.x = Dot.x = x + 30;
			hp.y = shieldHP.y = Dot.y = y + 17;

			mpBack.visible = false;
			mpBarWidth = 128;
			mpCapW = 3;   //the large arrowhead is three columns wide, like the small one
			mp.frame( 0, 5, 128, 6 );
			mpTrack.frame( 0, 25, 128, 5 );   //the large track, arrowhead end, a row shorter than the fill
			mpCap.frame( 4, 18, 3, 6 );
			//one column further right than health, so its arrowhead sits in the frame's corner
			mp.x = mpTrack.x = hp.x + 1;
			mp.y = mpTrack.y = mpCap.y = y + 26;
			mp.scale.set( 0, 1 );
			mpTrack.scale.set( 1, 1 );

			mpText.scale.set( 1 );

			hpText.x = hp.x + (128 - hpText.width())/2f;
			hpText.y = hp.y + 1;
			PixelScene.align(hpText);

			expText.x = exp.x + (128 - expText.width())/2f;
			expText.y = exp.y;
			PixelScene.align(expText);

			heroInfoOnBar.setRect(heroInfo.right(), y + 17, 130, 22);

			//little extra for 14th buff
			buffs.setRect(x + 31, y, 142, 16);

			busy.x = x + bg.width + 1;
			busy.y = y + bg.height - 9;
		} else {
			exp.x = x+2;
			exp.y = y+30;

			if (heroPaneExtraWidth > 0){
				heroPaneCutout.visible = true;
				heroPaneCutout.x = x;
				heroPaneCutout.y = y;
				heroPaneCutout.size(heroPaneExtraWidth+4, heroPaneCutout.height);
			}

			float hpleft = x + heroPaneWidth;
			if (hpBarMaxWidth < 82){
				//the class variable assumes the left of the bar can't move, but we can inset it 9px
				int hpWidth = (int)hpBarMaxWidth;
				if (hpWidth <= 41){
					hpleft -= 9;
					hpWidth += 9;
					hpCutout.visible = true;
					hpCutout.x = hpleft - 2;
					hpCutout.y = y;
				}
				hp.frame(50-hpWidth, 40, 50, 4);
				shieldHP.frame(50-hpWidth, 44, 50, 4);
			}

			hp.x = shieldHP.x = Dot.x = hpleft;
			hp.y = shieldHP.y = Dot.y = y + 2;

			//mirror the hp bar frame width so the bars stay flush
			mpBarWidth = 50;
			if (hpBarMaxWidth < 82){
				mpBarWidth = (int)hpBarMaxWidth;
				if (mpBarWidth <= 41) mpBarWidth += 9;
			}
			mpBack.visible = true;
			mpBack.x = hp.x;
			mpBack.y = y + 6;
			//the strip's content runs exactly as wide as the bar and ends open, on the
			//track's arrowhead, with no wall after the point
			mpBack.scale.set( mpBarWidth, 1 );
			mpCapW = 3;
			mp.frame( 0, 0, (int) mpBarWidth, 5 );
			mpTrack.frame( 128 - (int) mpBarWidth, 11, (int) mpBarWidth, 4 );   //arrowhead end, a row shorter than the fill
			mpCap.frame( 0, 18, 3, 5 );
			mp.x = mpTrack.x = hp.x;
			mp.y = mpTrack.y = mpCap.y = y + 6;
			mp.scale.set( 0, 1 );
			mpTrack.scale.set( 1, 1 );

			mpText.scale.set(PixelScene.align(0.5f));
			mpText.y = hp.y + (hp.height - (mpText.baseLine()+mpText.scale.y))/2f;
			mpText.y -= 0.001f;

			hpText.scale.set(PixelScene.align(0.5f));
			hpText.x = hp.x + 1;
			hpText.y = hp.y + (hp.height - (hpText.baseLine()+hpText.scale.y))/2f;
			hpText.y -= 0.001f; //prefer to be slightly higher
			PixelScene.align(hpText);

			expText.scale.set(PixelScene.align(0.5f));
			expText.x = exp.x + 1;
			expText.y = exp.y + (exp.height - (expText.baseLine()+expText.scale.y))/2f;
			expText.y -= 0.001f; //prefer to be slightly higher
			PixelScene.align(expText);

			heroInfoOnBar.setRect(heroInfo.right(), y, 50, 9);

			if (buffBarRowMaxWidths != null){
				buffs.rowWidthLimits = buffBarRowMaxWidths;
			}
			if (buffBarRowAdjusts != null){
				buffs.rowHeightAdjusts = buffBarRowAdjusts;
			}
			buffs.setRect( x + heroPaneWidth + 1, y + 11, 55, 14 );

			busy.x = x + 1;
			busy.y = y + 37;
		}

		counter.point(busy.center());
	}
	
	private static final int[] warningColors = new int[]{0x660000, 0xCC0000, 0x660000};

	private int oldHP = 0;
	private int oldShield = 0;
	private int oldMax = 0;
	private int oldMP = -1;
	private int oldMT = -1;

	@Override
	public void update() {
		super.update();
		
		int health = Dungeon.hero.HP;
		int shield = Dungeon.hero.shielding();
		int incomingDOT = Dungeon.hero.incomingDOT();
		int max = Dungeon.hero.HT;

		if (!Dungeon.hero.isAlive()) {
			avatar.tint(0x000000, 0.5f);
		} else if ((health/(float)max) < 0.334f) {
			warning += Game.elapsed * 5f *(0.4f - (health/(float)max));
			warning %= 1f;
			avatar.tint(ColorMath.interpolate(warning, warningColors), 0.5f );
		} else if (talentBlink > 0.33f){ //stops early so it doesn't end in the middle of a blink
			talentBlink -= Game.elapsed;
			avatar.tint(1, 1, 0, (float)Math.abs(Math.cos(talentBlink*FLASH_RATE))/2f);
		} else {
			avatar.resetColor();
		}

		float healthPercent = health/(float)max;
		float shieldPercent = shield/(float)max;
		float DOTPercent    = incomingDOT/(float)max;

		if (healthPercent + shieldPercent > 1f){
			float excess = healthPercent + shieldPercent;
			healthPercent /= excess;
			shieldPercent /= excess;
			DOTPercent    /= excess;
		}

		hp.scale.x = healthPercent;
		shieldHP.scale.x = healthPercent + shieldPercent;
		Dot.scale.x = Math.min(DOTPercent, shieldHP.scale.x);
		Dot.x = shieldHP.x + shieldHP.width() - Dot.width();

		int effectiveMT = Dungeon.hero.MT + RingOfMagic.manaBonus( Dungeon.hero );
		float manaPercent = GameMath.gate( 0, Dungeon.hero.MP / (float)Math.max( 1, effectiveMT ), 1 );
		//the body stops on a whole pixel and the arrowhead sits right after it
		int body = Math.round( manaPercent * (mpBarWidth - mpCapW - 1) );
		mp.scale.x = body / mpBarWidth;
		mpCap.visible = Dungeon.hero.MP > 0;
		mpCap.x = mp.x + body;

		if (oldMP != Dungeon.hero.MP || oldMT != effectiveMT){
			mpText.text(Dungeon.hero.MP + "/" + effectiveMT);
			mpText.measure();
			//spent or regained: the bar flashes so the change is caught in the corner of the eye
			if (oldMP != -1) mpPulse = 1f;
			oldMP = Dungeon.hero.MP;
			oldMT = effectiveMT;
		}
		if (mpPulse > 0){
			mpPulse = Math.max( 0, mpPulse - Game.elapsed * 3f );
			mp.brightness( 1f + 0.9f * mpPulse );
			mpCap.brightness( 1f + 0.9f * mpPulse );
			mpText.brightness( 1f + 0.5f * mpPulse );
		}
		//inside the bar, the way the health count sits in its bar
		if (large){
			mpText.x = hp.x + (128 - mpText.width()) / 2f;
			mpText.y = mp.y;
		} else {
			mpText.x = mp.x + 1;
			mpText.y = mp.y + (mp.height - (mpText.baseLine() + mpText.scale.y)) / 2f - 0.001f;
		}
		PixelScene.align(mpText);

		if (oldHP != health || oldShield != shield || oldMax != max){
			if (shield <= 0) {
				hpText.text(health + "/" + max);
			} else {
				hpText.text(health + "+" + shield + "/" + max);
			}
			oldHP = health;
			oldShield = shield;
			oldMax = max;
		}

		if (large) {
			exp.scale.x = (128 / exp.width) * Dungeon.hero.exp / Dungeon.hero.maxExp();

			hpText.measure();
			hpText.x = hp.x + (128 - hpText.width())/2f;

			expText.text(Dungeon.hero.exp + "/" + Dungeon.hero.maxExp());
			expText.measure();
			expText.x = hp.x + (128 - expText.width())/2f;

		} else {
			exp.scale.x = ((17 + heroPaneExtraWidth) / exp.width) * Dungeon.hero.exp / Dungeon.hero.maxExp();
			expText.text(Dungeon.hero.exp + "/" + Dungeon.hero.maxExp());
		}

		if (Dungeon.hero.lvl != lastLvl) {

			if (lastLvl != -1) {
				showStarParticles();
			}

			lastLvl = Dungeon.hero.lvl;

			if (large){
				level.text( "lv. " + lastLvl );
				level.measure();
				level.x = x + (30f - level.width()) / 2f;
				level.y = y + 33f - level.baseLine() / 2f;
			} else {
				level.text( Integer.toString( lastLvl ) );
				level.measure();
				level.x = x + heroPaneExtraWidth + 25.5f - level.width() / 2f;
				level.y = y + 31.0f - level.baseLine() / 2f;
			}
			PixelScene.align(level);
		}

		int tier = Dungeon.hero.tier();
		if (tier != lastTier) {
			lastTier = tier;
			avatar.copy( HeroSprite.avatar( Dungeon.hero ) );
		}

		counter.setSweep((1f - Actor.now()%1f)%1f);
	}

	public void updateAvatar(){
		avatar.copy( HeroSprite.avatar( Dungeon.hero ) );
	}

	/**
	 * The mana bar's art, painted once. Bodies run the full width (the fill is scaled
	 * and ends in a separate cap): a 4-row body at y 0, a 7-row body at y 4, a 7-row
	 * track at y 11. The caps are chevrons, an arrowhead the bar ends in: 2x4 at 0,18
	 * and 4x7 at 4,18.
	 */
	private static final int[] FIVE = { 0xA6D2FFFF, 0x7FB4F4FF, 0x5A96E0FF, 0x3E74C0FF, 0x2A5498FF };
	private static final int[] SIX  = { 0xA6D2FFFF, 0x8CC0F8FF, 0x6FA8F0FF, 0x5090DCFF, 0x3A6FB8FF, 0x2A5498FF };

	/**
	 * The mana bar's art, painted once. Bodies run the full width (the fill is scaled
	 * and ends in a separate cap): a 5-row body at y 0 for the compact pane, a 6-row
	 * body at y 5 for the large one, a 7-row track at y 11 whose last column is grey,
	 * the way the health bar ends. The caps are chevrons, the arrowhead the bar ends
	 * in: 3x5 at 0,18 and 4x6 at 4,18. Column 8 from y 18 is the compact strip's
	 * extension (five rows of fill, the light rule, the border), stretched sideways.
	 */
	private static void manaArt(){
		if (TextureCache.contains( MANA_KEY )) return;
		com.watabou.gltextures.SmartTexture tx = TextureCache.create( MANA_KEY, 128, 32 );
		com.badlogic.gdx.graphics.Pixmap pm = tx.bitmap;
		pm.setBlending( com.badlogic.gdx.graphics.Pixmap.Blending.None );
		pm.setColor( 0 ); pm.fill();
		for (int x = 0; x < 128; x++){
			for (int r = 0; r < 5; r++) pm.drawPixel( x, r, FIVE[r] );
			for (int r = 0; r < 6; r++) pm.drawPixel( x, 5 + r, SIX[r] );
		}
		//the tracks end in the same arrowhead the fill does, drawn in grey, so a full bar
		//is the blue arrowhead sitting exactly in the grey one: a 5-row track at y 11 for
		//the compact pane, a 6-row one at y 25 for the large
		int[] smallW = { 1, 2, 3, 2, 1 };
		int[] largeW = { 1, 2, 3, 3, 2, 1 };
		for (int r = 0; r < 5; r++){
			int end = 124 + smallW[r];
			int col = r == 0 ? 0x1A2A4AFF : r == 4 ? 0x0A1428FF : 0x0F1D36FF;
			for (int x = 0; x <= end; x++) pm.drawPixel( x, 11 + r, x == end ? (r < 2 ? 0xC8C8C8FF : 0x8A8A8AFF) : col );
		}
		for (int r = 0; r < 6; r++){
			int end = 124 + largeW[r];
			int col = r == 0 ? 0x1A2A4AFF : r == 5 ? 0x0A1428FF : 0x0F1D36FF;
			for (int x = 0; x <= end; x++) pm.drawPixel( x, 25 + r, x == end ? (r < 3 ? 0xC8C8C8FF : 0x8A8A8AFF) : col );
		}
		for (int r = 0; r < 5; r++) for (int x = 0; x < smallW[r]; x++) pm.drawPixel( x, 18 + r, FIVE[r] );
		for (int r = 0; r < 6; r++) for (int x = 0; x < largeW[r]; x++) pm.drawPixel( 4 + x, 18 + r, SIX[r] );
		//five rows of fill and nothing under them: the bar is the strip's last thing
		for (int r = 0; r < 5; r++) pm.drawPixel( 8, 18 + r, 0x25271EFF );
		tx.bitmap( pm );
		tx.filter( com.watabou.glwrap.Texture.NEAREST, com.watabou.glwrap.Texture.NEAREST );
	}

	public void alpha( float value ){
		value = GameMath.gate(0, value, 1f);
		bg.alpha(value);
		heroPaneCutout.alpha(value);
		hpCutout.alpha(value);
		avatar.alpha(value);
		shieldHP.alpha(value);
		hp.alpha(value);
		mpBack.alpha(value);
		mpTrack.alpha(value);
		mp.alpha(value);
		mpCap.alpha(value);
		mpText.alpha(value);
		hpText.alpha(0.6f*value);
		exp.alpha(value);
		if (expText != null) expText.alpha(0.6f*value);
		level.alpha(value);
		compass.alpha(value);
		busy.alpha(value);
		counter.alpha(value);
	}

	public void showStarParticles(){
		Emitter emitter = (Emitter)recycle( Emitter.class );
		emitter.revive();
		emitter.pos( avatar.center() );
		emitter.burst( Speck.factory( Speck.STAR ), 12 );
	}

}

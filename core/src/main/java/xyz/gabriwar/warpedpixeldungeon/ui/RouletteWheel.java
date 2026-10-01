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

package xyz.gabriwar.warpedpixeldungeon.ui;

import xyz.gabriwar.warpedpixeldungeon.Assets;
import com.badlogic.gdx.graphics.Pixmap;
import com.watabou.gltextures.SmartTexture;
import com.watabou.gltextures.TextureCache;
import com.watabou.glwrap.Texture;
import com.watabou.noosa.Game;
import com.watabou.noosa.Image;
import xyz.gabriwar.warpedpixeldungeon.scenes.PixelScene;
import com.watabou.noosa.audio.Sample;
import com.watabou.noosa.ui.Component;
import com.watabou.utils.Callback;
import com.watabou.utils.Random;

/**
 * A roulette wheel, drawn and spun like the real thing: a fixed wooden bowl with the
 * ball track and its eight brass deflectors, a wheelhead of 37 pockets between brass
 * frets turning on a wooden cone with a brass turret, and a ball.
 *
 * A spin is told where it ends: {@link #spin} takes the pocket the outcome already
 * chose. The head turns one way, the ball runs the other way round the track, loses
 * speed, drops off the track, bounces over the frets and settles into that pocket,
 * riding it round until the head stops. The ball's path is the head's angle plus a
 * relative angle that winds down to exactly zero, so the landing is exact.
 *
 * Pockets are numbered clockwise from the top of the head; their colours and labels
 * are the caller's (red/black/green and the numbers for the casino, the rarity colours
 * and initials for the smith). The labels are real text riding the number ring,
 * upright at every angle.
 */
public class RouletteWheel extends Component {

	public static final int POCKETS = 37;

	//the numbers round a European wheel, clockwise from the zero
	public static final int[] EUROPEAN = { 0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11,
			30, 8, 23, 10, 5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26 };
	private static final int[] REDS = { 1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36 };

	public static final int RED = 0xB3261E, BLACK = 0x1E1B1A, GREEN = 0x1F7A3A;

	public static boolean isRed( int number ){
		for (int r : REDS) if (r == number) return true;
		return false;
	}

	/** the colour a number wears on the table */
	public static int colorOf( int number ){
		return number == 0 ? GREEN : isRed( number ) ? RED : BLACK;
	}

	/** the casino wheel's pocket colours, in wheel order */
	public static int[] europeanColors(){
		int[] out = new int[POCKETS];
		for (int i = 0; i < POCKETS; i++) out[i] = colorOf( EUROPEAN[i] );
		return out;
	}

	//geometry, as fractions of the wheel's radius, from the rim inward
	private static final float F_TRACK_OUT = 0.935f, F_TRACK_IN = 0.84f;
	private static final float F_NUMBERS_IN = 0.72f, F_POCKET_IN = 0.60f;
	private static final float F_CONE = 0.56f, F_TURRET = 0.15f;
	private static final float F_BALL_TRACK = 0.885f, F_BALL_POCKET = 0.66f;
	private static final float SLICE = 360f / POCKETS;

	private static final int OUTLINE = 0x1A120CFF;

	private final String key;
	private final int size;
	private final float c, r;
	private SmartTexture bowlTex, headTex;
	private Image bowl, head, ball;
	private final int[] colors = new int[POCKETS];
	//the pocket labels are real text riding the number ring, kept upright as the head
	//turns: baked into the turning texture, small digits smear at every angle but the top
	private final RenderedTextBlock[] labels = new RenderedTextBlock[POCKETS];
	private int glow = -1;

	private boolean spinning;
	private float t, dur;
	private float headFrom, headTo, relFrom, pocketAngle;
	private Callback landed;
	private int lastPocket = -1;
	private boolean wasUp = true;
	//where the ball is now, in the component's own coordinates
	private float ballAngle, ballRadius;

	/**
	 * A wheel {@code size} pixels across (keep it even). {@code key} names its textures;
	 * windows showing different wheels need different keys.
	 */
	public RouletteWheel( String key, int size ){
		super();
		this.key = key;
		this.size = size;
		this.c = size / 2f;
		this.r = size / 2f - 0.5f;
		ballRadius = r * F_BALL_TRACK;

		bowlTex = texture( key + "-bowl-" + size, size, size );
		paintBowl();
		bowl = new Image( bowlTex );
		add( bowl );

		headTex = texture( key + "-head-" + size, size, size );
		head = new Image( headTex );
		head.origin.set( c, c );
		add( head );

		int bs = Math.max( 5, Math.round( size / 24f ) | 1 );
		SmartTexture ballTex = texture( "roulette-ball-" + bs, bs, bs );
		paintBall( ballTex );
		ball = new Image( ballTex );
		add( ball );

		for (int i = 0; i < POCKETS; i++) colors[i] = GREEN;
		paintHead();

		int textSize = size >= 150 ? 7 : 6;
		for (int i = 0; i < POCKETS; i++){
			labels[i] = PixelScene.renderTextBlock( textSize );
			labels[i].hardlight( 0xF4F0E6 );
			add( labels[i] );
		}
		//the ball rolls over the labels
		bringToFront( ball );

		width = height = size;
	}

	private static SmartTexture texture( String key, int w, int h ){
		SmartTexture t = TextureCache.create( key, w, h );
		t.filter( Texture.NEAREST, Texture.NEAREST );
		t.bitmap.setBlending( Pixmap.Blending.None );
		return t;
	}

	/** Colours the pockets (RGB, in wheel order), labels them, and clears any glow. */
	public void pockets( int[] rgb, String[] text ){
		System.arraycopy( rgb, 0, colors, 0, POCKETS );
		for (int i = 0; i < POCKETS; i++){
			labels[i].text( text == null || text[i] == null ? "" : text[i] );
			labels[i].hardlight( 0xF4F0E6 );
		}
		glow = -1;
		paintHead();
		placeLabels();
	}

	/** the casino wheel's pocket numbers, in wheel order */
	public static String[] europeanLabels(){
		String[] out = new String[POCKETS];
		for (int i = 0; i < POCKETS; i++) out[i] = String.valueOf( EUROPEAN[i] );
		return out;
	}

	public boolean spinning(){
		return spinning;
	}

	/** The ball's position in the parent's coordinates (for bursts where it lands). */
	public float ballX(){ return x + c + ballRadius * (float)Math.sin( Math.toRadians( ballAngle ) ); }
	public float ballY(){ return y + c - ballRadius * (float)Math.cos( Math.toRadians( ballAngle ) ); }

	/**
	 * Spins onto {@code pocket} (0 to 36, clockwise from the head's top) over about
	 * {@code seconds}, then calls {@code landed}. The outcome is the caller's: this only
	 * shows it.
	 */
	public void spin( int pocket, float seconds, Callback landed ){
		if (spinning) return;
		this.landed = landed;
		glow = -1;
		paintHead();
		for (RenderedTextBlock l : labels) l.hardlight( 0xF4F0E6 );
		dur = seconds;
		t = 0;
		headFrom = head.angle % 360f;
		headTo = headFrom + 360f * (1.5f + Random.Float());
		//the ball runs the other way, six turns and a bit relative to the head
		relFrom = -(360f * 6 + Random.Float( 360f ));
		pocketAngle = (pocket + 0.5f) * SLICE + Random.Float( -SLICE * 0.18f, SLICE * 0.18f );
		lastPocket = -1;
		wasUp = true;
		spinning = true;
		Sample.INSTANCE.play( Assets.Sounds.CHARGEUP, 0.5f, 1.4f );
	}

	@Override
	public void update(){
		super.update();
		if (!spinning) return;
		t += Game.elapsed;
		float p = Math.min( 1f, t / dur );

		//the head coasts down across the whole spin
		float headAngle = headFrom + (headTo - headFrom) * (1f - (1f - p) * (1f - p));
		head.angle = headAngle;

		//the ball: fast round the track, off it at DROP, bouncing into the pocket by LOCK
		final float DROP = 0.5f, LOCK = 0.82f;
		final float track = r * F_BALL_TRACK, pocketR = r * F_BALL_POCKET;
		float q = Math.min( 1f, p / LOCK );
		float rel = relFrom * (1f - q) * (1f - q) * (1f - q);
		float radius;
		if (p < DROP){
			radius = track;
		} else if (p < LOCK){
			float s = (p - DROP) / (LOCK - DROP);
			float fall = (float)Math.pow( 1f - s, 1.4f );
			float hop = Math.abs( (float)Math.cos( 3.5f * Math.PI * s ) );
			radius = pocketR + (track - pocketR) * fall * (0.35f + 0.65f * hop);
			//a bounce off a fret: the ball skips a little sideways and clacks
			boolean up = hop > 0.25f;
			if (up && !wasUp){
				Sample.INSTANCE.play( Assets.Sounds.CLICK, 0.25f + 0.5f * fall, 1.5f );
			}
			wasUp = up;
			rel += (1f - s) * 5f * (float)Math.sin( 9f * Math.PI * s );
		} else {
			radius = pocketR;
		}
		ballAngle = headAngle + pocketAngle + rel;
		ballRadius = radius;

		//the ball rattling over the frets on its way in: a tick per pocket
		if (p >= DROP && p < LOCK){
			int under = (int)Math.floor( (((ballAngle - headAngle) % 360f) + 360f) % 360f / SLICE );
			if (under != lastPocket){
				if (lastPocket != -1) Sample.INSTANCE.play( Assets.Sounds.CLICK, 0.12f, 1.8f );
				lastPocket = under;
			}
		}
		placeBall();

		placeLabels();

		if (p >= 1f){
			spinning = false;
			glow = (int)Math.floor( ((pocketAngle % 360f) + 360f) % 360f / SLICE );
			paintHead();
			labels[glow].hardlight( 0xFFE27A );
			Sample.INSTANCE.play( Assets.Sounds.HIT, 0.4f, 1.3f );
			if (landed != null){
				Callback cb = landed;
				landed = null;
				cb.call();
			}
		}
	}

	@Override
	protected void layout(){
		super.layout();
		bowl.x = x;
		bowl.y = y;
		head.x = x;
		head.y = y;
		placeBall();
		placeLabels();
	}

	//each label at its pocket's spot on the number ring, wherever the head has turned
	private void placeLabels(){
		if (labels[0] == null) return;
		float ring = r * (F_TRACK_IN + F_NUMBERS_IN) / 2f;
		for (int i = 0; i < POCKETS; i++){
			double a = Math.toRadians( head.angle + (i + 0.5f) * SLICE );
			float lx = x + c + ring * (float)Math.sin( a ), ly = y + c - ring * (float)Math.cos( a );
			labels[i].setPos( lx - labels[i].width() / 2f, ly - labels[i].height() / 2f );
			PixelScene.align( labels[i] );
		}
	}

	private void placeBall(){
		if (ball == null) return;
		ball.x = ballX() - ball.width() / 2f;
		ball.y = ballY() - ball.height() / 2f;
	}

	// ----------------------------------------------------------------- painting

	private static int rgba( int rgb, float light ){
		int rr = Math.min( 255, Math.max( 0, Math.round( ((rgb >> 16) & 0xFF) * light ) ) );
		int g = Math.min( 255, Math.max( 0, Math.round( ((rgb >> 8) & 0xFF) * light ) ) );
		int b = Math.min( 255, Math.max( 0, Math.round( (rgb & 0xFF) * light ) ) );
		return (rr << 24) | (g << 16) | (b << 8) | 0xFF;
	}

	//light from the upper left, 0.8 .. 1.25
	private static float lit( float dx, float dy, float d ){
		return 1.02f + 0.23f * (-(dx + dy) / (1.414f * Math.max( 1f, d )));
	}

	private void paintBowl(){
		Pixmap pm = bowlTex.bitmap;
		float trackOut = r * F_TRACK_OUT, trackIn = r * F_TRACK_IN;
		for (int y = 0; y < size; y++){
			for (int x = 0; x < size; x++){
				float dx = x + 0.5f - c, dy = y + 0.5f - c;
				float d = (float)Math.sqrt( dx*dx + dy*dy );
				int col = 0;
				if (d <= r + 0.5f){
					if (d > r - 0.8f) col = OUTLINE;
					else if (d > trackOut){
						//the polished wooden rim, with its grain
						float grain = (((int)(Math.atan2( dy, dx ) * 60 + d * 0.7f)) & 3) == 0 ? 0.9f : 1f;
						col = rgba( 0x8A5429, lit( dx, dy, d ) * grain );
					} else if (d > trackOut - 1f){
						col = rgba( 0xD8B25A, lit( dx, dy, d ) );             //brass lip
					} else if (d > trackIn){
						col = rgba( 0x4A2C17, 0.9f + 0.25f * (d - trackIn) / (trackOut - trackIn) );
					}
				}
				pm.drawPixel( x, y, col );
			}
		}
		//the eight brass diamonds on the track that knock the ball off its line
		float dr = r * (F_TRACK_OUT + F_TRACK_IN) / 2f;
		int half = Math.max( 1, Math.round( size / 64f ) );
		for (int k = 0; k < 8; k++){
			double a = Math.toRadians( k * 45 + 22.5 );
			float cx = c + dr * (float)Math.sin( a ), cy = c - dr * (float)Math.cos( a );
			for (int oy = -half; oy <= half; oy++){
				for (int ox = -half; ox <= half; ox++){
					if (Math.abs( ox ) + Math.abs( oy ) > half) continue;
					int px = Math.round( cx - 0.5f ) + ox, py = Math.round( cy - 0.5f ) + oy;
					pm.drawPixel( px, py, ox + oy < 0 ? 0xF2D27AFF : 0xB8923AFF );
				}
			}
		}
		bowlTex.bitmap( pm );
	}

	private void paintHead(){
		Pixmap pm = headTex.bitmap;
		float numOut = r * F_TRACK_IN, numIn = r * F_NUMBERS_IN, pocketIn = r * F_POCKET_IN;
		float cone = r * F_CONE, turret = r * F_TURRET;
		for (int y = 0; y < size; y++){
			for (int x = 0; x < size; x++){
				float dx = x + 0.5f - c, dy = y + 0.5f - c;
				float d = (float)Math.sqrt( dx*dx + dy*dy );
				int col = 0;
				if (d <= numOut){
					float deg = (float)Math.toDegrees( Math.atan2( dx, -dy ) );
					if (deg < 0) deg += 360f;
					int s = Math.min( POCKETS - 1, (int)(deg / SLICE) );
					float toFret = Math.min( deg - s * SLICE, (s + 1) * SLICE - deg ) * (float)Math.PI / 180f * d;
					boolean shine = s == glow;
					if (d > numIn){
						//the number ring: the pocket's colour with its number in white, set
						//radially (its top toward the rim), as on a real wheel
						if (d > numOut - 0.8f || toFret < 0.5f) col = rgba( 0xD9B44A, lit( dx, dy, d ) );
						else col = rgba( colors[s], (shine ? 1.5f : 1f) * lit( dx, dy, d ) );
					} else if (d > pocketIn){
						if (toFret < 0.6f) col = rgba( 0xD9B44A, lit( dx, dy, d ) );       //brass fret
						else {
							//the pocket, darker toward its outer wall
							float depth = 0.75f - 0.25f * (d - pocketIn) / (numIn - pocketIn);
							col = rgba( colors[s], (shine ? 1.7f : 1f) * depth * lit( dx, dy, d ) );
						}
					} else if (d > cone){
						col = rgba( 0xC99A3E, lit( dx, dy, d ) );                         //brass ring
					} else if (d > turret){
						//the wooden cone, darker spokes every 45 degrees
						float spoke = Math.abs( ((deg + 22.5f) % 45f) - 22.5f ) * (float)Math.PI / 180f * d;
						float shade = 0.85f + 0.3f * (d - turret) / (cone - turret);
						col = spoke < 0.7f ? rgba( 0x3E2412, 1f ) : rgba( 0x7A4A24, shade * lit( dx, dy, d ) );
					} else {
						//the turret: a brass cross with a knob
						float arm = Math.max( 1.2f, size / 80f );
						boolean onArm = Math.abs( dx ) < arm || Math.abs( dy ) < arm;
						col = d < turret * 0.3f ? 0xFFF1B0FF : rgba( onArm ? 0xF0CC62 : 0x9C7A2E, lit( dx, dy, d ) );
					}
				}
				pm.drawPixel( x, y, col );
			}
		}
		headTex.bitmap( pm );
	}

	private static void paintBall( SmartTexture t ){
		Pixmap pm = t.bitmap;
		for (int y = 0; y < 5; y++){
			for (int x = 0; x < 5; x++){
				float dx = x - 2f, dy = y - 2f;
				float d = (float)Math.sqrt( dx*dx + dy*dy );
				int col = 0;
				if (d <= 2.3f){
					col = d > 1.7f ? 0x6E6A66FF : (dx + dy < -0.5f ? 0xFFFFFFFF : 0xD8D4CEFF);
				}
				pm.drawPixel( x, y, col );
			}
		}
		t.bitmap( pm );
	}
}

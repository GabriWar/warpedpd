package xyz.gabriwar.warpedpixeldungeon.net;

import xyz.gabriwar.warpedpixeldungeon.levels.Level;

import com.watabou.utils.PathFinder;

import java.util.HashMap;
import java.util.HashSet;

import com.watabou.utils.SparseArray;

public class SpectatorLevel extends Level {

	private String tilesTex;
	private String waterTex;

	// The host's own answer for this level. Without it cleanWalls() marks interior
	// walls undiscoverable and FogOfWar/WallBlockingTilemap paint them black no
	// matter what the FOV says — the blackout documented in CLAUDE.md, arriving
	// through the wire on every fully-lit level this class stands in for.
	private boolean noFog = false;

	public SpectatorLevel() {
		// Initialize collections that Level.create() normally sets up
		mobs = new HashSet<>();
		heaps = new SparseArray<>();
		blobs = new HashMap<>();
		plants = new SparseArray<>();
		traps = new SparseArray<>();
		customTiles = new java.util.ArrayList<>();
		customWalls = new java.util.ArrayList<>();
		butter = new SparseArray<>();
		transitions = new java.util.ArrayList<>();
	}

	public void setupFromNetwork(int[] mapData, int w, int h, String tilesTex, String waterTex, boolean noFog) {
		this.tilesTex = tilesTex;
		this.waterTex = waterTex;
		this.noFog = noFog;
		this.width = w;
		this.height = h;
		this.length = w * h;

		PathFinder.setMapSize(w, h);

		this.map = mapData;
		this.visited = new boolean[length];
		this.mapped = new boolean[length];
		this.discoverable = new boolean[length];
		this.heroFOV = new boolean[length];

		this.passable = new boolean[length];
		this.losBlocking = new boolean[length];
		this.flamable = new boolean[length];
		this.secret = new boolean[length];
		this.solid = new boolean[length];
		this.avoid = new boolean[length];
		this.water = new boolean[length];
		this.pit = new boolean[length];
		this.openSpace = new boolean[length];

		buildFlagMaps();
		cleanWalls();
	}

	@Override
	public boolean noFogOfWar() {
		return noFog;
	}

	@Override
	public String tilesTex() {
		return tilesTex;
	}

	@Override
	public String waterTex() {
		return waterTex;
	}

	@Override
	protected boolean build() {
		// No-op, level is built from network data
		return true;
	}

	@Override
	protected void createMobs() {}

	@Override
	protected void createItems() {}

	// Override to prevent music issues
	@Override
	public void playLevelMusic() {}

	@Override
	public int width() { return width; }

	@Override
	public int height() { return height; }

	@Override
	public int length() { return length; }
}

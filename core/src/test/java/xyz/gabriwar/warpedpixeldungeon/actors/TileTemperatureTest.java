package xyz.gabriwar.warpedpixeldungeon.actors;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import xyz.gabriwar.warpedpixeldungeon.Dungeon;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Fire;
import xyz.gabriwar.warpedpixeldungeon.actors.blobs.Freezing;
import xyz.gabriwar.warpedpixeldungeon.levels.Level;
import xyz.gabriwar.warpedpixeldungeon.levels.Terrain;
import com.watabou.utils.PathFinder;
import com.watabou.noosa.Game;

import static org.junit.Assert.*;

public class TileTemperatureTest {
	private Level previousLevel;
	private float previousTemperature;
	private String previousVersion;
	private boolean canFreeze;
	private Level level;
	private static final int CENTER = 24;

	@Before
	public void setUp() {
		previousVersion = Game.version;
		Game.version = "test";
		previousLevel = Dungeon.level;
		previousTemperature = ClimateManager.debugTempOverride;
		ClimateManager.debugTempOverride = -20f;
		level = new Level() {
			@Override protected boolean build() { return true; }
			@Override protected void createMobs() { }
			@Override protected void createItems() { }
			@Override public boolean waterCanFreeze() { return canFreeze; }
		};
		level.setSize(7, 7);
		java.util.Arrays.fill(level.solid, true);
		level.traps = new com.watabou.utils.SparseArray<>();
		level.plants = new com.watabou.utils.SparseArray<>();
		level.heaps = new com.watabou.utils.SparseArray<>();
		level.blobs = new java.util.HashMap<>();
		Dungeon.level = level;
		for (int y = 1; y < 6; y++) {
			for (int x = 1; x < 6; x++) Level.set(x + y * 7, Terrain.EMPTY, level);
		}
		Level.set(CENTER, Terrain.FROZEN_WATER, level);
	}

	@After
	public void tearDown() {
		Game.version = previousVersion;
		Dungeon.level = previousLevel;
		ClimateManager.debugTempOverride = previousTemperature;
		if (previousLevel != null) PathFinder.setMapSize(previousLevel.width(), previousLevel.height());
	}

	@Test
	public void fireRampsFastWithoutScalingIntensityByDuration() {
		Level.set(CENTER, Terrain.EMPTY, level);
		ClimateManager.debugTempOverride = 20f;
		Fire shortFire = new Fire();
		shortFire.seed(level, CENTER, 3);
		shortFire.act();
		assertEquals(105f, TileTemperature.tileTemp(CENTER), 0.01f);
		level.tileHeat[CENTER] = 0;
		Fire longFire = new Fire();
		longFire.seed(level, CENTER, 100);
		longFire.act();
		assertEquals(105f, TileTemperature.tileTemp(CENTER), 0.01f);
		for (int i = 0; i < 20; i++) longFire.act();
		assertEquals(120f, TileTemperature.tileTemp(CENTER), 0.01f);
	}

	@Test
	public void frostRampsFastAndStaysNearItsTarget() {
		Level.set(CENTER, Terrain.EMPTY, level);
		ClimateManager.debugTempOverride = 20f;
		Freezing frost = new Freezing();
		frost.seed(level, CENTER, 100);
		frost.act();
		assertEquals(-31f, TileTemperature.tileTemp(CENTER), 0.01f);
		for (int i = 0; i < 20; i++) frost.act();
		assertEquals(-40f, TileTemperature.tileTemp(CENTER), 0.01f);
	}

	@Test
	public void fireNeverCoolsHotterTilesAndFrostNeverWarmsColderOnes() {
		level.tileHeat[CENTER] = 1000f;
		TileTemperature.applyFireHeat(CENTER);
		assertEquals(1000f, level.tileHeat[CENTER], 0f);
		level.tileHeat[CENTER] = -1000f;
		TileTemperature.applyFrostCold(CENTER);
		assertEquals(-1000f, level.tileHeat[CENTER], 0f);
	}

	@Test
	public void sustainedFireMeltsIceInThreeTurnsButNotInstantly() {
		Fire fire = new Fire();
		fire.seed(level, CENTER, 16);
		for (int turn = 0; turn < 3; turn++) {
			fire.act();
			assertEquals(Terrain.FROZEN_WATER, level.map[CENTER]);
			TileTemperature.stepDiffusion(level);
			assertEquals(turn < 2 ? Terrain.FROZEN_WATER : Terrain.WATER, level.map[CENTER]);
		}
		assertTrue(level.water[CENTER]);
	}

	@Test
	public void manyHeatDepositsCannotSkipTheMinimumTime() {
		for (int i = 0; i < 20; i++) TileTemperature.depositHeat(CENTER, 1000f);
		assertEquals(Terrain.FROZEN_WATER, level.map[CENTER]);
		TileTemperature.stepDiffusion(level);
		assertEquals(Terrain.FROZEN_WATER, level.map[CENTER]);
	}

	@Test
	public void moderateWarmthTakesLongerThanExtremeHeat() {
		canFreeze = true;
		ClimateManager.debugTempOverride = 25f; // ice is 17 C: 12 degrees above thaw threshold
		for (int i = 0; i < 4; i++) {
			TileTemperature.stepDiffusion(level);
			assertEquals(Terrain.FROZEN_WATER, level.map[CENTER]);
		}
		TileTemperature.stepDiffusion(level);
		assertEquals(Terrain.WATER, level.map[CENTER]);
	}

	@Test
	public void heatDiffusingFromNextTileEventuallyMeltsIce() {
		for (int i = 0; i < 8 && level.map[CENTER] == Terrain.FROZEN_WATER; i++) {
			TileTemperature.depositHeat(CENTER - 1, 1000f);
			TileTemperature.stepDiffusion(level);
		}
		assertEquals(Terrain.WATER, level.map[CENTER]);
	}

	@Test
	public void sustainedColdFreezesInThreeTurnsButNotInstantly() {
		canFreeze = true;
		ClimateManager.debugTempOverride = 20f;
		Level.set(CENTER, Terrain.WATER, level);
		for (int i = 0; i < 3; i++) {
			TileTemperature.depositHeat(CENTER, -1000f);
			assertEquals(Terrain.WATER, level.map[CENTER]);
			TileTemperature.stepDiffusion(level);
			assertEquals(i < 2 ? Terrain.WATER : Terrain.FROZEN_WATER, level.map[CENTER]);
		}
		assertFalse(level.water[CENTER]);
	}

	@Test
	public void coldDiffusingFromNextTileEventuallyFreezesWater() {
		canFreeze = true;
		ClimateManager.debugTempOverride = 20f;
		Level.set(CENTER, Terrain.WATER, level);
		for (int i = 0; i < 8 && level.map[CENTER] == Terrain.WATER; i++) {
			TileTemperature.depositHeat(CENTER - 1, -1000f);
			TileTemperature.stepDiffusion(level);
		}
		assertEquals(Terrain.FROZEN_WATER, level.map[CENTER]);
	}

	@Test
	public void returningBelowThawThresholdCancelsPartialMelting() {
		canFreeze = true;
		ClimateManager.debugTempOverride = 100f;
		TileTemperature.stepDiffusion(level);
		assertTrue(level.waterPhaseProgress[CENTER] > 0);
		ClimateManager.debugTempOverride = -20f;
		TileTemperature.stepDiffusion(level);
		assertEquals(0f, level.waterPhaseProgress[CENTER], 0f);
		assertEquals(Terrain.FROZEN_WATER, level.map[CENTER]);
	}

	@Test
	public void insufficientColdLeavesWaterLiquid() {
		canFreeze = true;
		ClimateManager.debugTempOverride = 20f;
		Level.set(CENTER, Terrain.WATER, level);
		TileTemperature.depositHeat(CENTER, -1f);
		for (int i = 0; i < 10; i++) TileTemperature.stepDiffusion(level);
		assertEquals(Terrain.WATER, level.map[CENTER]);
	}

	@Test
	public void lavaLevelsRemainExcludedFromFreezing() {
		Level.set(CENTER, Terrain.WATER, level);
		for (int i = 0; i < 10; i++) {
			TileTemperature.depositHeat(CENTER, -1000f);
			TileTemperature.stepDiffusion(level);
		}
		assertEquals(Terrain.WATER, level.map[CENTER]);
	}

	@Test
	public void heatAndPartialPhaseMoveTogetherWhenWindowScrolls() {
		level.tileHeat[CENTER] = 120f;
		level.waterPhaseProgress[CENTER] = 0.5f;
		TileTemperature.shift(level, 1, -1);
		int moved = CENTER - 1 + level.width();
		assertEquals(120f, level.tileHeat[moved], 0f);
		assertEquals(0.5f, level.waterPhaseProgress[moved], 0f);
		assertEquals(0f, level.tileHeat[CENTER], 0f);
		assertEquals(0f, level.waterPhaseProgress[CENTER], 0f);
	}
}

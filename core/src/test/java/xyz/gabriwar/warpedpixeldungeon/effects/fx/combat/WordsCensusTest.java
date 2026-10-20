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

package xyz.gabriwar.warpedpixeldungeon.effects.fx.combat;

import org.junit.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * The words that floated over characters in fights and became icons on the hit cards stay icons:
 * a source scan of the game's code for the shapes they had (a defence verb shown as a word, a
 * skill's shout, the hard-coded proc words, rage, immunity, a deferral, a resisted effect). It
 * guards against these coming back, not against every word a future change might float.
 */
public class WordsCensusTest {

	private static final Path SRC = Paths.get( "src/main/java" );

	/** What came out, as the shapes it had: a pattern a showStatus call's line must not match. */
	private static final String[] REMOVED = {
			//a defence verb as a word (Blow.missed / Blow.miss show its icon)
			"showStatus\\w*\\([^;]*defenseVerb\\(\\)",
			//hard-coded proc words on the struck one
			"showStatus\\([^;]*\"(TERRIFIED|Pinned|Pierced|Frozen|Chilled|Bound|Mad|Caught|Zzz)\"",
			//rage, enrage, immunity, deferral, resistance, a fuse, a free reload, a paralysis broken
			"showStatus\\([^;]*Messages\\.get\\([^;]*\"(rage|enraged|invulnerable|deferred|resisted|warm|hot|reload|out)\"",
			//a skill's own words (its shout is its icon: Skill.chipOn)
			"showStatus\\([^;]*Messages\\.get\\([^;]*\"(bullseye|caught|shrug|reel|pickpocket|planted|bound|break|crush|stagger|branded|rally|crippled|slam|free|choke|down|combo|breathe|fear|riposte|pulse|leap|harvest|roll|vault|dazed|pinned|fury|due|judged|circle|hush|overload|jolt|plates_left|grit|spared|hold|brace|snap|ricochet|parry|blocked|burst|snared|reform|charged|shatter|ready)\"",
	};

	private static List<Path> sources() throws IOException {
		assertTrue( "run from core/", Files.isDirectory( SRC ) );
		List<Path> out = new ArrayList<>();
		try (Stream<Path> s = Files.walk( SRC )){
			s.filter( p -> p.toString().endsWith( ".java" ) ).forEach( out::add );
		}
		return out;
	}

	@Test
	public void noRemovedWordComesBack() throws IOException {
		List<String> back = new ArrayList<>();
		Pattern[] pats = new Pattern[REMOVED.length];
		for (int i = 0; i < pats.length; i++) pats[i] = Pattern.compile( REMOVED[i] );
		for (Path p : sources()){
			//the debug exhibits draw what they like
			if (p.toString().contains( "effects/fx/modules/" )) continue;
			List<String> lines = Files.readAllLines( p, StandardCharsets.UTF_8 );
			for (int n = 0; n < lines.size(); n++){
				for (Pattern pat : pats){
					if (pat.matcher( lines.get( n ) ).find()) back.add( SRC.relativize( p ) + ":" + (n + 1) + ": " + lines.get( n ).trim() );
				}
			}
		}
		assertEquals( "words that came back: " + back, 0, back.size() );
	}

	@Test
	public void aSkillsShoutIsItsIconNotAWord() throws IOException {
		String skill = new String( Files.readAllBytes( SRC.resolve( "xyz/gabriwar/warpedpixeldungeon/actors/hero/skills/Skill.java" ) ),
				StandardCharsets.UTF_8 );
		Matcher m = Pattern.compile( "public void castTextYell\\(\\)\\s*\\{(.*?)\\n\\t}", Pattern.DOTALL ).matcher( skill );
		assertTrue( "castTextYell is there", m.find() );
		assertFalse( "it floats no word", m.group( 1 ).contains( "showStatus" ) );
		assertTrue( "its icon goes on a card", m.group( 1 ).contains( "chipOn" ) );
	}
}

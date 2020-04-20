/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.crawler.toolbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import herma.crawler.toolbox.common.Common;

public class MatchStatistics {
	
	private static final Pattern TAB_PATTERN = Common.TAB_PATTERN;
	private static final Pattern SPACE_PATTERN = Pattern.compile(Pattern.quote(" "));
	private static final Pattern SPACES_PATTERN = Pattern.compile("\\s+");
	
	public static void main(final String[] args) {
		if (args.length != 4) {
			System.err.println("Invalid number of command line arguments.");
			System.err.println("Expecting four arguments:");
			System.err.println("path to a keyphrases file");
			System.err.println("path to a matches.txt");
			System.err.println("path to the keyphrases statistics output file");
			System.err.println("path to the match statistics output file");
			System.exit(1);
			return;
		}
		
		final FileSystem fs = FileSystems.getDefault();
		final Path keyphrasesFile = Common.loadPath(fs, args[0]);
		final Path matchesFile = Common.loadPath(fs, args[1]);
		final Path keyphrasesStatisticsOutputFile = Common.loadPath(fs, args[2]);
		final Path matchesStatisticsOutputFile = Common.loadPath(fs, args[3]);
		
		final HashMap<String, BigInteger> matchesStatistics = new HashMap<>();
		final HashMap<String, BigInteger> keyphrasesStatistics = new HashMap<>();
		
		try {
			final ArrayList<Keyphrase> keyphrases = loadKeyphrases(keyphrasesFile);
			produceStatistics(matchesFile, keyphrases, matchesStatistics, keyphrasesStatistics);
			
			try (final BufferedWriter writer = Files.newBufferedWriter(keyphrasesStatisticsOutputFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				output(keyphrasesStatistics, writer);
			}
			
			try (final BufferedWriter writer = Files.newBufferedWriter(matchesStatisticsOutputFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				output(matchesStatistics, writer);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private static ArrayList<Keyphrase> loadKeyphrases(final Path keyphrasesFile) throws IOException {
		try (final Stream<String> lines = Files.lines(keyphrasesFile, StandardCharsets.UTF_8)) {
			return lines
					.map(Keyphrase::new)
					.collect(Collectors.toCollection(ArrayList::new));
		}
	}
	
	private static void produceStatistics(final Path matchesFile, final Iterable<? extends Keyphrase> keyphrases, final HashMap<String, BigInteger> matchesStatistics, final HashMap<String, BigInteger> keyphrasesStatistics) throws IOException {
		try (final BufferedReader reader = Files.newBufferedReader(matchesFile, StandardCharsets.UTF_8)) {
			while (true) {
				final String line = reader.readLine();
				if (line == null)
					break;
				final String[] parts = TAB_PATTERN.split(line, -1);
				final String countStr = parts[2];
				final BigInteger count;
				try {
					count = new BigInteger(countStr);
				} catch (final NumberFormatException e) {
					System.err.println("not a valid number: " + countStr);
					continue;
				}
				final String match = parts[1];
				add(matchesStatistics, match, count);
				final String[] matchWordsLowercased = SPACE_PATTERN.split(match.toLowerCase(Locale.ROOT));
				for (final Keyphrase keyphrase : keyphrases)
					if (isMatch(matchWordsLowercased, keyphrase))
						add(keyphrasesStatistics, keyphrase.original, count);
			}
		}
	}
	
	private static boolean isMatch(final String[] matchWordsLowercased, final Keyphrase keyphrase) {
		final int n = matchWordsLowercased.length;
		if (n != keyphrase.wordsLowercased.length)
			return false;
		for (int i = 0; i < n; i++) {
			if (!matchWordsLowercased[i].contains(keyphrase.wordsLowercased[i]))
				return false;
		}
		return true;
	}
	
	private static <T> void add(final HashMap<T, BigInteger> map, final T key, final BigInteger toAdd) {
		map.put(key, toAdd.add(map.getOrDefault(key, BigInteger.ZERO)));
	}
	
	private static void output(final HashMap<String, BigInteger> statistics, final Appendable target) throws IOException {
		final ArrayList<Entry<String, BigInteger>> sorted = new ArrayList<>(statistics.entrySet());
		Collections.sort(sorted, (e1, e2) -> e2.getValue().compareTo(e1.getValue()));
		for (final Entry<String, BigInteger> entry : sorted) {
			target.append(entry.getValue().toString());
			target.append('\t');
			target.append(entry.getKey());
			target.append('\n');
		}
	}
	
	private static class Keyphrase {
		
		public final String original;
		public final String[] wordsLowercased;
		
		public Keyphrase(final String keyphraseLine) {
			original = keyphraseLine;
			wordsLowercased = SPACES_PATTERN.split(keyphraseLine.toLowerCase(Locale.ROOT).trim());
		}
	}
	
}

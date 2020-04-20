/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.crawler.toolbox;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Stream;

import herma.crawler.toolbox.common.Common;

public class Umtriebe {
	
	public static void main(final String[] args) {
		if (args.length != 2) {
			System.err.println("Invalid number of command line arguments.");
			System.err.println("Expecting two arguments:");
			System.err.println("crawler output directory");
			System.err.println("output file");
			System.exit(1);
			return;
		}
		
		final FileSystem fs = FileSystems.getDefault();
		
		final Path processedUrlsDir = Common.loadPath(fs, args[0]).resolve("meta").resolve("processedurls");
		final Path outputFile = Common.loadPath(fs, args[1]);
		
		try {
			final HashMap<String, BigInteger> hostCounts = countHosts(processedUrlsDir);
			
			final ArrayList<Entry<String, BigInteger>> hostCountsSorted = sort(hostCounts);
			
			try (final BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				for (final Entry<String, BigInteger> entry : hostCountsSorted) {
					writer.append(entry.getKey());
					writer.append('\t');
					writer.append(entry.getValue().toString());
					writer.append('\n');
				}
				writer.flush();
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private static HashMap<String, BigInteger> countHosts(final Path processedUrlsDir) throws IOException {
		final HashMap<String, BigInteger> result = new HashMap<>();
		try (final DirectoryStream<Path> files = Files.newDirectoryStream(processedUrlsDir)) {
			for (final Path file : files) {
				try (final Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
					lines.map(Umtriebe::hostOf).forEachOrdered(host -> increment(result, host));
				}
			}
		}
		return result;
	}
	
	private static String hostOf(final String url) {
		final int doubleSlashIndex = url.indexOf("//");
		final int hostStart = doubleSlashIndex < 0 ? 0 : doubleSlashIndex + 2;
		final int endIndex1 = url.indexOf('/', hostStart);
		final int endIndex2 = url.indexOf('?', hostStart);
		final int endIndex3 = url.indexOf('#', hostStart);
		final int hostEnd = minPos(endIndex1, minPos(endIndex2, endIndex3));
		if (hostEnd < 0)
			return url.substring(hostStart);
		return url.substring(hostStart, hostEnd);
	}
	
	private static int minPos(final int int1, final int int2) {
		if (int1 < 0)
			return int2;
		if (int2 < 0)
			return int1;
		if (int1 < int2)
			return int1;
		return int2;
	}
	
	private static <K> void increment(final HashMap<K, BigInteger> table, final K key) {
		table.put(key, BigInteger.ONE.add(table.getOrDefault(key, BigInteger.ZERO)));
	}
	
	private static ArrayList<Entry<String, BigInteger>> sort(final HashMap<String, BigInteger> hostCounts) {
		final ArrayList<Entry<String, BigInteger>> hostCountsSorted = new ArrayList<>(hostCounts.entrySet());
		Collections.sort(hostCountsSorted, Umtriebe::lexicographicallyCompareByHost);
		return hostCountsSorted;
	}
	
	// domain name from right to left, components as usual
	private static int lexicographicallyCompareByHost(final Entry<String, ?> entry1, final Entry<String, ?> entry2) {
		final String host1 = entry1.getKey();
		final String host2 = entry2.getKey();
		
		int start1 = host1.length();
		int start2 = host2.length();
		while (true) {
			final int dotIndex1 = host1.lastIndexOf('.', start1 - 1);
			final int dotIndex2 = host2.lastIndexOf('.', start2 - 1);
			
			final int cmp = host1.substring(dotIndex1 + 1, start1).compareTo(host2.substring(dotIndex2 + 1, start2));
			if (cmp != 0)
				return cmp;
			
			final boolean no1 = dotIndex1 < 0;
			final boolean no2 = dotIndex2 < 0;
			if (no1 && no2)
				return 0;
			if (no1)
				return -1;
			if (no2)
				return +1;
			
			start1 = dotIndex1 - 1;
			start2 = dotIndex2 - 1;
		}
	}
	
}

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
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.HashMap;

import herma.crawler.toolbox.common.Common;
import herma.crawler.toolbox.common.CommonPaths;

public class TokenBasedDuplicateFinder {
	
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
		
		final Path crawlDir = Common.loadPath(fs, args[0]);
		final Path outputFile = Common.loadPath(fs, args[1]);
		
		final Path tokenFilesDir = crawlDir.resolve(CommonPaths.TEXT_DIR).resolve(CommonPaths.TEXT_TOKENS_DIR);
		
		final HashMap<HashableTokenSequence, ArrayList<String>> candidates = new HashMap<>();
		
		try {
			final ArrayList<String> temp = new ArrayList<>();
			try (final DirectoryStream<Path> files = Files.newDirectoryStream(tokenFilesDir)) {
				for (final Path file : files) {
					final HashableTokenSequence key = new HashableTokenSequence(getTokenSequence(file, temp));
					ArrayList<String> list = candidates.getOrDefault(key, null);
					if (list == null) {
						list = new ArrayList<>();
						candidates.put(key, list);
					}
					list.add(file.getFileName().toString());
				}
			}
			
			try (final BufferedWriter writer = Files.newBufferedWriter(outputFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
				for (final ArrayList<String> list : candidates.values()) {
					if (list.size() > 1) {
						boolean first = true;
						for (final String filename : list) {
							if (first)
								first = false;
							else
								writer.append('\t');
							writer.append(filename);
						}
						writer.append('\n');
					}
				}
				writer.flush();
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private static String[] getTokenSequence(final Path tokensFile, final ArrayList<String> temp) throws IOException {
		try (final BufferedReader reader = Files.newBufferedReader(tokensFile, StandardCharsets.UTF_8)) {
			while (true) {
				final String line = reader.readLine();
				if (line == null)
					break;
				if ("".equals(line))
					continue;
				temp.add(line);
			}
		}
		final String[] result = temp.toArray(new String[temp.size()]);
		temp.clear();
		return result;
	}
	
	private static class HashableTokenSequence {
		
		private final int pHashCode;
		private final String[] pTokenSequence;
		
		public HashableTokenSequence(final String[] tokenSequence) {
			pTokenSequence = tokenSequence;
			pHashCode = hashTokenSequence(tokenSequence);
		}
		
		private static int hashTokenSequence(final String[] tokenSequence) {
			int hashCode = 0;
			for (final String str : tokenSequence)
				hashCode ^= str.hashCode();
			return hashCode;
		}
		
		@Override
		public int hashCode() {
			return pHashCode;
		}
		
		@Override
		public boolean equals(final Object obj) {
			if (obj instanceof HashableTokenSequence)
				return equals((HashableTokenSequence) obj);
			return super.equals(obj);
		}
		
		public boolean equals(final HashableTokenSequence obj) {
			if (this == obj)
				return true;
			final int n = pTokenSequence.length;
			if (n != obj.pTokenSequence.length)
				return false;
			for (int i = 0; i < n; i++)
				if (!pTokenSequence[i].equals(obj.pTokenSequence[i]))
					return false;
			return true;
		}
	}
	
}

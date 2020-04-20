/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.crawler.toolbox;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import herma.crawler.toolbox.common.Common;
import herma.crawler.toolbox.common.CommonPaths;

public class MetadataConsistencyCheck {
	
	public static void main(final String[] args) {
		if (args.length != 1) {
			System.err.println("Invalid number of command line arguments.");
			System.err.println("Expecting one argument: crawler output directory");
			System.exit(1);
			return;
		}
		
		final Path crawlDir = Common.loadPath(FileSystems.getDefault(), args[0]);
		
		long errorCount = 0L;
		try {
			System.out.println("Reading " + CommonPaths.URLS_FILE);
			final HashSet<String> urlFilenames = readColumnAsSet(crawlDir.resolve(CommonPaths.URLS_FILE), 3);
			
			final HashSet<String> originalFilenames = new HashSet<>();
			final HashSet<String> extractFilenames = new HashSet<>();
			final HashSet<String> tokensFilenames = new HashSet<>();
			final HashSet<String> posLemmaFilenames = new HashSet<>();
			final HashSet<String> parseFilenames = new HashSet<>();
			System.out.println("Reading " + CommonPaths.FILES_FILE);
			readMetadata(crawlDir.resolve(CommonPaths.FILES_FILE), originalFilenames, extractFilenames, tokensFilenames, posLemmaFilenames, parseFilenames);
			
			System.out.println("Reading " + CommonPaths.MATCHES_FILE);
			final HashSet<String> matchFilenames = readColumnAsSet(crawlDir.resolve(CommonPaths.MATCHES_FILE), 0);
			
			System.out.println();
			System.out.println("Checking integrity within metadata");
			System.out.println();
			
			errorCount += checkSetEquality(urlFilenames, CommonPaths.URLS_FILE, originalFilenames, CommonPaths.FILES_FILE);
			errorCount += checkSetEquality(posLemmaFilenames, CommonPaths.FILES_FILE, matchFilenames, CommonPaths.MATCHES_FILE);
			
			System.out.println();
			System.out.println("Checking integrity with respect to saved files");
			System.out.println();
			
			errorCount += checkBijectionToFiles(urlFilenames, crawlDir, CommonPaths.ORIGINAL_DIR, "", CommonPaths.URLS_FILE);
			errorCount += checkBijectionToTextFiles(extractFilenames, tokensFilenames, posLemmaFilenames, parseFilenames, crawlDir, CommonPaths.TEXT_DIR, CommonPaths.FILES_FILE);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		
		System.out.println();
		if (errorCount == 0) {
			System.out.println("Finished. No inconsistencies found.");
		} else if (errorCount == 1) {
			System.out.println("Finished. 1 inconsistency found.");
		} else {
			System.out.print("Finished. ");
			System.out.print(Long.toString(errorCount));
			System.out.println(" inconsistencies found.");
		}
	}
	
	private static HashSet<String> readColumnAsSet(final Path file, final int index) throws IOException {
		try (final Stream<String> lines = Files.lines(file, StandardCharsets.UTF_8)) {
			return Common.selectColumn(lines, index).collect(Collectors.toCollection(HashSet::new));
		}
	}
	
	private static void readMetadata(final Path filesFile, final HashSet<String> originalFilenames, final HashSet<String> extractFilenames, final HashSet<String> tokensFilenames, final HashSet<String> posLemmaFilenames, final HashSet<String> parseFilenames) throws IOException {
		try (final BufferedReader reader = Files.newBufferedReader(filesFile, StandardCharsets.UTF_8)) {
			while (true) {
				final String line = reader.readLine();
				if (line == null)
					break;
				final String[] parts = Common.TAB_PATTERN.split(line);
				originalFilenames.add(parts[0]);
				addIfPresent(parts, 2, extractFilenames);
				addIfPresent(parts, 3, tokensFilenames);
				addIfPresent(parts, 4, posLemmaFilenames);
				addIfPresent(parts, 5, parseFilenames);
			}
		}
	}
	
	private static <T> void addIfPresent(final T[] parts, final int index, final HashSet<? super T> set) {
		if (parts.length > index)
			set.add(parts[index]);
	}
	
	private static long checkSetEquality(final HashSet<String> set1, final String origin1, final HashSet<String> set2, final String origin2) {
		return
		  checkSubset(set1, origin1, set2, origin2)
		+ checkSubset(set2, origin2, set1, origin1);
	}
	
	private static long checkBijectionToTextFiles(final HashSet<String> extractFilenames, final HashSet<String> tokensFilenames, final HashSet<String> posLemmaFilenames, final HashSet<String> parseFilenames, final Path crawlDir, final String subdir, final String metadataLocation) throws IOException {
		final Path txtDir = crawlDir.resolve(subdir);
		final String fileLocationPrefix = subdir + '/';
		return
		  checkBijectionToFiles(extractFilenames, txtDir, CommonPaths.TEXT_ORIGINAL_DIR, fileLocationPrefix, metadataLocation)
		+ checkBijectionToFiles(tokensFilenames, txtDir, CommonPaths.TEXT_TOKENS_DIR, fileLocationPrefix, metadataLocation)
		+ checkBijectionToFiles(posLemmaFilenames, txtDir, CommonPaths.TEXT_POS_LEMMA_DIR, fileLocationPrefix, metadataLocation)
		+ checkBijectionToFiles(parseFilenames, txtDir, CommonPaths.TEXT_PARSE_DIR, fileLocationPrefix, metadataLocation);
	}
	
	private static long checkBijectionToFiles(final HashSet<String> filenames, final Path dir, String subdir, String fileLocationPrefix, String metadataLocation) throws IOException {
		return checkBijectionToFiles(dir.resolve(subdir), fileLocationPrefix + subdir, filenames, metadataLocation);
	}
	
	private static long checkBijectionToFiles(final Path dir, final String dirname, final HashSet<String> filenames, final String metadataLocation) throws IOException {
		long errorCount = 0L;
		final HashSet<String> existingFiles = new HashSet<>();
		try (final DirectoryStream<Path> files = Files.newDirectoryStream(dir)) {
			for (final Path file : files) {
				final String filename = file.getFileName().toString();
				existingFiles.add(filename);
				if (!filenames.contains(filename)) {
					logMissingItem(filename, dirname, metadataLocation);
					errorCount++;
				}
			}
		}
		
		return errorCount + checkSubset(filenames, metadataLocation, existingFiles, dirname);
	}
	
	private static long checkSubset(final HashSet<String> subset, final String subsetOrigin, final HashSet<String> superset, final String supersetOrigin) {
		long errorCount = 0L;
		for (final String str1 : subset) {
			if (!superset.contains(str1)) {
				logMissingItem(str1, subsetOrigin, supersetOrigin);
				errorCount++;
			}
		}
		return errorCount;
	}
	
	private static void logMissingItem(final String item, final String presentIn, final String missingIn) {
		System.err.println(item);
		System.err.print("\tin ");
		System.err.print(presentIn);
		System.err.print(" but not in ");
		System.err.println(missingIn);
	}
	
}

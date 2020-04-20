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
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import herma.crawler.toolbox.common.Common;
import herma.crawler.toolbox.common.CommonPaths;

public class Merge {
	
	private static final String DUPLICATION_LOG_FILE_NAME = "merge-info.txt";
	
	public static void main(final String[] args) {
		final int numberOfCrawlDirs = args.length - 3;
		if (numberOfCrawlDirs < 0) {
			System.err.println("Invalid number of command line arguments.");
			System.err.println("Expecting three or more arguments:");
			System.err.println("shortlist file");
			System.err.println("input column (\"" + CommonPaths.ORIGINAL_DIR + "\", \"" + CommonPaths.TEXT_ORIGINAL_DIR + "\", \"" + CommonPaths.TEXT_TOKENS_DIR + "\", \"" + CommonPaths.TEXT_POS_LEMMA_DIR + "\", or \"" + CommonPaths.TEXT_PARSE_DIR + "\")");
			System.err.println("output directory");
			System.err.println("input directory 1");
			System.err.println("input directory 2");
			System.err.println("...");
			System.exit(1);
			return;
		} else if (numberOfCrawlDirs == 0) {
			System.out.println("No input directories have been specified.");
			System.out.println("Nothing to do.");
			return;
		}
		
		final int inputColumn = loadInputColumn(args[1]);
		if (inputColumn < 0) {
			System.exit(1);
			return;
		}
		
		final FileSystem fs = FileSystems.getDefault();
		final Path shortlistFile = Common.loadPath(fs, args[0]);
		final Path targetDir = Common.loadPath(fs, args[2]);
		
		final ArrayList<Path> crawlDirs = new ArrayList<>(numberOfCrawlDirs);
		for (int i = 3; i < args.length; i++)
			crawlDirs.add(Common.loadPath(fs, args[i]));
		
		try {
			
			final HashSet<String> shortlist = loadShortlist(shortlistFile);
			
			final HashMap<String, ArrayList<DownloadInfo>> downloadsByUrl = new HashMap<>(); 
			
			final ArrayList<Path> sourceUrlsFiles = new ArrayList<>(numberOfCrawlDirs);
			final ArrayList<Path> sourceFilesFiles = new ArrayList<>(numberOfCrawlDirs);
			final ArrayList<Path> sourceMatchesFiles = new ArrayList<>(numberOfCrawlDirs);
			
			for (final Path crawlDir : crawlDirs) {
				final Path urlsFile = crawlDir.resolve(CommonPaths.URLS_FILE);
				final Path filesFile = crawlDir.resolve(CommonPaths.FILES_FILE);
				sourceUrlsFiles.add(urlsFile);
				sourceFilesFiles.add(filesFile);
				sourceMatchesFiles.add(crawlDir.resolve(CommonPaths.MATCHES_FILE));
				loadMetadata(crawlDir, urlsFile, filesFile, inputColumn, downloadsByUrl);
			}
			
			final HashMap<String, DownloadInfo> retainedOriginalsMap = new HashMap<>();
			final HashMap<String, DownloadInfo> retainedTextExtractsMap = new HashMap<>();
			final HashMap<String, DownloadInfo> retainedLemmaFilesMap = new HashMap<>();
			
			final ArrayList<DownloadInfo> retain = new ArrayList<>();
			final ArrayList<DuplicationLogEntry> duplicationLog = new ArrayList<>();
			determineDownloadsToRetain(downloadsByUrl, shortlist, retain, duplicationLog, retainedOriginalsMap, retainedTextExtractsMap, retainedLemmaFilesMap);
			
			logDuplications(duplicationLog, targetDir.resolve(DUPLICATION_LOG_FILE_NAME));
			
			mergeMetadata(sourceUrlsFiles, targetDir.resolve(CommonPaths.URLS_FILE), retainedOriginalsMap.keySet(), 3);
			mergeMetadata(sourceFilesFiles, targetDir.resolve(CommonPaths.FILES_FILE), retainedTextExtractsMap.keySet(), 2);
			mergeMetadata(sourceMatchesFiles, targetDir.resolve(CommonPaths.MATCHES_FILE), retainedLemmaFilesMap.keySet(), 0);
			copyFiles(retain, targetDir);
			
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private static int loadInputColumn(final String arg) {
		switch (arg) {
			case CommonPaths.ORIGINAL_DIR:
				return 0;
			case CommonPaths.TEXT_ORIGINAL_DIR:
				return 2;
			case CommonPaths.TEXT_TOKENS_DIR:
				return 3;
			case CommonPaths.TEXT_POS_LEMMA_DIR:
				return 4;
			case CommonPaths.TEXT_PARSE_DIR:
				return 5;
			default:
				System.err.print("Invalid input column: ");
				System.err.println(arg);
				return -1;
		}
	}
	
	private static HashSet<String> loadShortlist(final Path shortlistFile) throws IOException {
		try (final Stream<String> lines = Files.lines(shortlistFile, StandardCharsets.UTF_8)) {
			return lines.collect(Collectors.toCollection(HashSet::new));
		}
	}
	
	private static void loadMetadata(final Path crawlDir, final Path urlsFile, final Path filesFile, final int inputColumn, final HashMap<String, ArrayList<DownloadInfo>> downloadsByUrl) throws IOException {
		final HashMap<String, String> originalUrlMap = loadUrls(urlsFile);
		try (final BufferedReader reader = Files.newBufferedReader(filesFile, StandardCharsets.UTF_8)) {
			while (true) {
				final String line = reader.readLine();
				if (line == null)
					break;
				final String[] parts = Common.TAB_PATTERN.split(line);
				final String originalFilename = parts[0];
				final String url = originalUrlMap.getOrDefault(originalFilename, null);
				if (url == null) {
					System.err.println("file without URL: " + originalFilename);
					continue;
				}
				ArrayList<DownloadInfo> list = downloadsByUrl.getOrDefault(url, null);
				if (list == null) {
					list = new ArrayList<>();
					downloadsByUrl.put(url, list);
				}
				list.add(new DownloadInfo(crawlDir, originalFilename, parts[2], parts[3], parts[4], parts[5], parts[inputColumn]));
			}
		}
	}
	
	private static HashMap<String, String> loadUrls(final Path urlsFile) throws IOException {
		final HashMap<String, String> result = new HashMap<>();
		try (final Stream<String> lines = Files.lines(urlsFile, StandardCharsets.UTF_8)) {
			lines
			.map(Common.TAB_PATTERN::split)
			.forEachOrdered(parts -> result.put(parts[3], parts[0]));
		}
		return result;
	}
	
	private static void determineDownloadsToRetain(final HashMap<String, ArrayList<DownloadInfo>> downloadsByUrl, final HashSet<String> shortlist, final ArrayList<DownloadInfo> retain, final ArrayList<DuplicationLogEntry> duplicationLog, final HashMap<String, DownloadInfo> retainedOriginalsMap, final HashMap<String, DownloadInfo> retainedTextExtractsMap, final HashMap<String, DownloadInfo> retainedLemmaFilesMap) throws IOException {
		for (final ArrayList<DownloadInfo> list : downloadsByUrl.values()) {
			final int listSize = list.size(); // > 0
			if (listSize == 1) {
				registerForRetention(list.get(0), retain, retainedOriginalsMap, retainedTextExtractsMap, retainedLemmaFilesMap);
				continue;
			}
			
			final ArrayList<DownloadInfo> discarded = new ArrayList<>();
			DownloadInfo result = findDownloadOnShortlist(list, shortlist);
			final String message;
			if (result == null) {
				message = "last";
				result = list.get(listSize - 1);
			} else {
				message = "on shortlist";
			}
			final DownloadInfo toRetain = result;
			registerForRetention(toRetain, retain, retainedOriginalsMap, retainedTextExtractsMap, retainedLemmaFilesMap);
			duplicationLog.add(new DuplicationLogEntry(toRetain.originalFile, message, discarded));
			final ArrayList<DownloadInfo> shouldBeEqual = new ArrayList<>();
			shouldBeEqual.add(toRetain);
			for (final DownloadInfo downloadInfo : list) {
				if (downloadInfo == toRetain)
					continue;
				if (tokenizationsDiffer(toRetain, downloadInfo)) {
					registerForRetention(downloadInfo, retain, retainedOriginalsMap, retainedTextExtractsMap, retainedLemmaFilesMap);
					duplicationLog.add(new DuplicationLogEntry(downloadInfo.originalFile, "tokens differ", shouldBeEqual));
					continue;
				}
				discarded.add(downloadInfo);
			}
		}
	}
	
	private static DownloadInfo findDownloadOnShortlist(final ArrayList<DownloadInfo> downloadsList, final HashSet<String> shortlist) {
		DownloadInfo result = null;
		for (final DownloadInfo downloadInfo : downloadsList) {
			if (onShortlist(downloadInfo, shortlist)) {
				if (result == null) {
					result = downloadInfo;
					continue;
				}
				System.err.print("Warning: duplicate in shortlist (");
				System.err.print(result.filenameToMatchOnShortlist);
				System.err.print(" and ");
				System.err.print(downloadInfo.filenameToMatchOnShortlist);
				System.err.println(')');
			}
		}
		return result;
	}
	
	private static boolean onShortlist(final DownloadInfo downloadInfo, final HashSet<String> shortlist) {
		return shortlist.contains(downloadInfo.filenameToMatchOnShortlist);
	}
	
	private static void registerForRetention(final DownloadInfo toRetain, final ArrayList<DownloadInfo> retain, final HashMap<String, DownloadInfo> retainedOriginalsMap, final HashMap<String, DownloadInfo> retainedTextExtractsMap, final HashMap<String, DownloadInfo> retainedLemmaFilesMap) {
		retain.add(toRetain);
		putOrComplain(retainedOriginalsMap, toRetain, toRetain.originalFile);
		putOrComplain(retainedTextExtractsMap, toRetain, toRetain.extractFile);
		putOrComplain(retainedLemmaFilesMap, toRetain, toRetain.lemmaFile);
	}
	
	private static boolean tokenizationsDiffer(final DownloadInfo toRetain, final DownloadInfo other) throws IOException {
		try (final BufferedReader reader1 = Files.newBufferedReader(tokensFilePath(toRetain), StandardCharsets.UTF_8)) {
			try (final BufferedReader reader2 = Files.newBufferedReader(tokensFilePath(other), StandardCharsets.UTF_8)) {
				while (true) {
					final String line1 = reader1.readLine();
					final String line2 = reader2.readLine();
					final boolean no1 = (line1 == null);
					final boolean no2 = (line2 == null);
					if (no1 && no2)
						return false;
					if (no1 || no2)
						return true;
					if (line1.equals(line2))
						continue;
					return true;
				}
			}
		}
	}

	private static Path tokensFilePath(final DownloadInfo downloadInfo) {
		return downloadInfo.base.resolve(CommonPaths.TEXT_DIR).resolve(CommonPaths.TEXT_TOKENS_DIR).resolve(downloadInfo.tokensFile);
	}
	
	private static void logDuplications(final ArrayList<DuplicationLogEntry> duplicationLog, final Path logFile) throws IOException {
		Collections.sort(duplicationLog, (x1, x2) -> x1.name.compareTo(x2.name));
		
		try (final BufferedWriter writer = Files.newBufferedWriter(logFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			for (final DuplicationLogEntry x : duplicationLog) {
				writer.append(x.name);
				writer.append('\t');
				writer.append(x.message);
				for (final DownloadInfo duplikat : x.other)
					writer.append('\t').append(duplikat.originalFile);
				writer.append('\n');
			}
			writer.flush();
		}
	}
	
	private static void putOrComplain(final HashMap<String, DownloadInfo> targetMap, final DownloadInfo downloadInfo, final String filename) {
		final DownloadInfo alt = targetMap.getOrDefault(filename, null);
		if (alt == null) {
			targetMap.put(filename, downloadInfo);
			return;
		}
		System.err.println("File name conflict: There are at least two files with the same name to be copied into the same directory.");
		System.err.print("Noticed in this file name: ");
		System.err.println(filename);
		System.err.println("appearing in (sub-directories of) at least these two crawler output directories:");
		System.err.println(alt.base);
		System.err.println(downloadInfo.base);
		System.exit(2);
	}
	
	private static void copyFiles(final ArrayList<DownloadInfo> retain, final Path targetDir) throws IOException {
		final Path targetOriginalDir = targetDir.resolve(CommonPaths.ORIGINAL_DIR);
		final Path targetTextDir = targetDir.resolve(CommonPaths.TEXT_DIR);
		final Path targetTextOriginalDir = targetTextDir.resolve(CommonPaths.TEXT_ORIGINAL_DIR);
		final Path targetTextTokensDir = targetTextDir.resolve(CommonPaths.TEXT_TOKENS_DIR);
		final Path targetTextLemmaDir = targetTextDir.resolve(CommonPaths.TEXT_POS_LEMMA_DIR);
		final Path targetTextParserInputDir = targetTextDir.resolve(CommonPaths.TEXT_PARSER_INPUT_DIR);
		final Path targetTextParseDir = targetTextDir.resolve(CommonPaths.TEXT_PARSE_DIR);
		
		Files.createDirectory(targetOriginalDir);
		Files.createDirectory(targetTextDir);
		Files.createDirectory(targetTextOriginalDir);
		Files.createDirectory(targetTextTokensDir);
		Files.createDirectory(targetTextLemmaDir);
		Files.createDirectory(targetTextParserInputDir);
		Files.createDirectory(targetTextParseDir);
		
		for (final DownloadInfo downloadInfo : retain) {
			final Path sourceOriginalDir = downloadInfo.base.resolve(CommonPaths.ORIGINAL_DIR);
			final Path sourceTextDir = downloadInfo.base.resolve(CommonPaths.TEXT_DIR);
			final Path sourceTextOriginalDir = sourceTextDir.resolve(CommonPaths.TEXT_ORIGINAL_DIR);
			final Path sourceTextTokensDir = sourceTextDir.resolve(CommonPaths.TEXT_TOKENS_DIR);
			final Path sourceTextLemmaDir = sourceTextDir.resolve(CommonPaths.TEXT_POS_LEMMA_DIR);
			final Path sourceTextParserInputDir = sourceTextDir.resolve(CommonPaths.TEXT_PARSER_INPUT_DIR);
			final Path sourceTextParseDir = sourceTextDir.resolve(CommonPaths.TEXT_PARSE_DIR);
			
			copy(sourceOriginalDir, targetOriginalDir, downloadInfo.originalFile);
			copy(sourceTextOriginalDir, targetTextOriginalDir, downloadInfo.extractFile);
			copy(sourceTextTokensDir, targetTextTokensDir, downloadInfo.tokensFile);
			copy(sourceTextLemmaDir, targetTextLemmaDir, downloadInfo.lemmaFile);
			copy(sourceTextParseDir, targetTextParseDir, downloadInfo.parsedFile);
			
			final Path sourceTextParserInputFile = sourceTextParserInputDir.resolve(downloadInfo.parsedFile);
			if (Files.exists(sourceTextParserInputFile))
				Files.copy(sourceTextParserInputFile, targetTextParserInputDir.resolve(downloadInfo.parsedFile));
		}
	}
	
	private static void copy(final Path sourceDir, final Path targetDir, final String filename) throws IOException {
		Files.copy(sourceDir.resolve(filename), targetDir.resolve(filename));
	}
	
	private static void mergeMetadata(final ArrayList<Path> sourceFiles, final Path targetFile, final Set<String> retain, final int columnIndex) throws IOException {
		try (final BufferedWriter writer = Files.newBufferedWriter(targetFile, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			for (final Path sourceFile : sourceFiles) {
				try (final BufferedReader reader = Files.newBufferedReader(sourceFile, StandardCharsets.UTF_8)) {
					while (true) {
						final String line = reader.readLine();
						if (line == null)
							break;
						final Optional<String> filename = Common.TAB_PATTERN.splitAsStream(line).skip(columnIndex).findFirst();
						if (filename.isPresent() && retain.contains(filename.get()))
							writer.append(line).append('\n');
					}
				}
			}
			writer.flush();
		}
	}
	
	private static class DuplicationLogEntry {
		public final String name;
		public final String message;
		public final ArrayList<DownloadInfo> other;
		
		public DuplicationLogEntry(final String name, final String message, final ArrayList<DownloadInfo> other) {
			this.name = name;
			this.message = message;
			this.other = other;
		}
	}
	
	private static class DownloadInfo {
		public final Path base;
		public final String originalFile;
		public final String extractFile;
		public final String tokensFile;
		public final String lemmaFile;
		public final String parsedFile;
		public final String filenameToMatchOnShortlist;
		
		public DownloadInfo(final Path base, final String originalFile, final String extractFile, final String tokensFile, final String lemmaFile, final String parsedFile, final String filenameToMatchOnShortlist) {
			this.base = base;
			this.originalFile = originalFile;
			this.extractFile = extractFile;
			this.tokensFile = tokensFile;
			this.lemmaFile = lemmaFile;
			this.parsedFile = parsedFile;
			this.filenameToMatchOnShortlist = filenameToMatchOnShortlist;
		}
	}
	
}

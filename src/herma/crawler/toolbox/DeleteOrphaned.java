/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.crawler.toolbox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import herma.crawler.toolbox.common.Common;
import herma.crawler.toolbox.common.CommonPaths;

public class DeleteOrphaned {
	
	public static void main(final String[] args) {
		final boolean mock;
		switch (args.length) {
			case 1:
				mock = false;
				break;
			case 2:
				if ("mock".equals(args[1])) {
					mock = true;
					break;
				}
			default:
				System.err.println("Invalid command line arguments.");
				System.err.println("Expecting one or two arguments:");
				System.err.println("crawler output directory");
				System.err.println("optional: \"mock\" to not actually delete files");
				System.exit(1);
				return;
		}
		
		final Path crawlDir = Common.loadPath(FileSystems.getDefault(), args[0]);
		
		final Path originalDir = crawlDir.resolve(CommonPaths.ORIGINAL_DIR);
		final Path textDir = crawlDir.resolve(CommonPaths.TEXT_DIR);
		final Path textPosLemmaDir = textDir.resolve(CommonPaths.TEXT_POS_LEMMA_DIR);
		final Path urlsFile = crawlDir.resolve(CommonPaths.URLS_FILE);
		final Path filesFile = crawlDir.resolve(CommonPaths.FILES_FILE);
		final Path matchesFile = crawlDir.resolve(CommonPaths.MATCHES_FILE);
		
		try {
			System.out.println("Reading " + CommonPaths.URLS_FILE);
			final HashSet<String> originalFilenamesWithUrlMetadata = collectOriginalFilenamesWithUrlMetadata(urlsFile, originalDir);
			System.out.println("Reading " + CommonPaths.MATCHES_FILE);
			final HashSet<String> posLemmaFilenamesWithMatchMetadata = collectPosLemmaFilenamesWithMatchMetadata(matchesFile, textPosLemmaDir);
			
			System.out.println();
			
			if (mock) {
				final Deleter deleter = new Deleter(textDir, textPosLemmaDir, originalFilenamesWithUrlMetadata, posLemmaFilenamesWithMatchMetadata);
				
				System.out.println("Mocking the deletion of entries in " + CommonPaths.FILES_FILE);
				FromMetadataFileDeletion.mockDeleteFromMetaFile(filesFile, deleter);
				System.out.println("Deleted (mock).");
				
				final HashSet<String> originalFilesWithMetadata = deleter.getOriginalFilesWithMetadata();
				final HashSet<String> posLemmaFilesWithMetadata = deleter.getPosLemmaFilesWithMetadata();
				
				System.out.println();
				
				System.out.println("Mocking the deletion from other metadata:");
				mockRetainReferencedMetadata(crawlDir, CommonPaths.URLS_FILE, originalFilesWithMetadata, 3);
				mockRetainReferencedMetadata(crawlDir, CommonPaths.MATCHES_FILE, posLemmaFilesWithMetadata, 0);
				
				System.out.println();
				
				System.out.println("Mocking the deletion of files without metadata:");
				deleteFilesWithoutMetadata(
						crawlDir,
						originalFilesWithMetadata,
						deleter.getExtractedTextFilesWithMetadata(),
						deleter.getTokensFilesWithMetadata(),
						posLemmaFilesWithMetadata,
						deleter.getParseFilesWithMetadata(),
						path -> mockDelete(crawlDir, path)
					);
			} else {
				final Deleter deleter = new Deleter(textDir, textPosLemmaDir, originalFilenamesWithUrlMetadata, posLemmaFilenamesWithMatchMetadata);
				
				System.out.println("Deleting entries in " + CommonPaths.FILES_FILE);
				FromMetadataFileDeletion.deleteFromMetaFile(filesFile, deleter);
				System.out.println("Deleted.");
				
				final HashSet<String> originalFilesWithMetadata = deleter.getOriginalFilesWithMetadata();
				final HashSet<String> posLemmaFilesWithMetadata = deleter.getPosLemmaFilesWithMetadata();
				
				System.out.println();
				
				System.out.println("Deleting from other metadata:");
				retainReferencedMetadata(crawlDir, CommonPaths.URLS_FILE, originalFilesWithMetadata, 3);
				retainReferencedMetadata(crawlDir, CommonPaths.MATCHES_FILE, posLemmaFilesWithMetadata, 0);
				
				System.out.println();
				
				System.out.println("Deleting files without metadata");
				deleteFilesWithoutMetadata(
						crawlDir,
						originalFilesWithMetadata,
						deleter.getExtractedTextFilesWithMetadata(),
						deleter.getTokensFilesWithMetadata(),
						posLemmaFilesWithMetadata,
						deleter.getParseFilesWithMetadata(),
						DeleteOrphaned::delete
					);
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		
		System.out.println();
		System.out.println("Done.");
	}
	
	private static HashSet<String> collectOriginalFilenamesWithUrlMetadata(final Path urlsFile, final Path originalDir) throws IOException {
		try (final Stream<String> lines = Files.lines(urlsFile, StandardCharsets.UTF_8)) {
			return Common.selectColumn(lines, 3)
					.filter(fn -> Files.exists(originalDir.resolve(fn), LinkOption.NOFOLLOW_LINKS))
					.collect(Collectors.toCollection(HashSet::new));
		}
	}
	
	private static HashSet<String> collectPosLemmaFilenamesWithMatchMetadata(final Path matchesFile, final Path posLemmaDir) throws IOException {
		try (final Stream<String> lines = Files.lines(matchesFile, StandardCharsets.UTF_8)) {
			return Common.selectColumn(lines, 0).collect(Collectors.toCollection(HashSet::new));
		}
	}
	
	private static void mockRetainReferencedMetadata(final Path crawlDir, final String metadataFilename, final HashSet<String> referencedFilenames, final int column) throws IOException {
		System.out.println(metadataFilename);
		FromMetadataFileDeletion.mockDeleteFromMetaFile(crawlDir.resolve(metadataFilename), fields -> !referencedFilenames.contains(fields[column]));
	}
	
	private static void retainReferencedMetadata(final Path crawlDir, final String metadataFilename, final HashSet<String> referencedFilenames, final int column) throws IOException {
		System.out.println(metadataFilename);
		FromMetadataFileDeletion.deleteFromMetaFile(crawlDir.resolve(metadataFilename), fields -> !referencedFilenames.contains(fields[column]));
	}
	
	private static void deleteFilesWithoutMetadata(final Path crawlDir, final HashSet<String> originalFilesWithMetadata, final HashSet<String> extractedTextFilesWithMetadata, final HashSet<String> tokensFilesWithMetadata, final HashSet<String> posLemmaFilesWithMetadata, final HashSet<String> parseFilesWithMetadata, final Consumer<? super Path> deletionOperation) throws IOException {
		final Path textDir = crawlDir.resolve(CommonPaths.TEXT_DIR);
		processDirectory(crawlDir.resolve(CommonPaths.ORIGINAL_DIR), originalFilesWithMetadata, deletionOperation);
		processDirectory(textDir.resolve(CommonPaths.TEXT_ORIGINAL_DIR), extractedTextFilesWithMetadata, deletionOperation);
		processDirectory(textDir.resolve(CommonPaths.TEXT_TOKENS_DIR), tokensFilesWithMetadata, deletionOperation);
		processDirectory(textDir.resolve(CommonPaths.TEXT_POS_LEMMA_DIR), posLemmaFilesWithMetadata, deletionOperation);
		processDirectory(textDir.resolve(CommonPaths.TEXT_PARSE_DIR), parseFilesWithMetadata, deletionOperation);
		processDirectory(textDir.resolve(CommonPaths.TEXT_PARSER_INPUT_DIR), parseFilesWithMetadata, deletionOperation);
	}
	
	private static void processDirectory(final Path directory, final HashSet<String> filesWithMetadata, final Consumer<? super Path> deletionOperation) throws IOException {
		try (final DirectoryStream<Path> files = Files.newDirectoryStream(directory)) {
			for (final Path file : files) {
				if (filesWithMetadata.contains(file.getFileName().toString()))
					continue;
				deletionOperation.accept(file);
			}
		}
	}
	
	private static void delete(final Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
	}
	
	private static void mockDelete(final Path crawlDir, final Path file) {
		System.out.print("\tMocking the deletion of ");
		System.out.println(crawlDir.relativize(file).toString());
	}
	
	private static class Deleter implements Predicate<String[]> {
		
		private final Path pTextDir;
		private final Path pTextOriginalDir;
		private final Path pTextTokensDir;
		private final Path pTextPosLemmaDir;
		private final Path pTextParseDir;
		
		private final HashSet<String> pOriginalFilenamesWithUrlMetadata;
		private final HashSet<String> pPosLemmaFilenamesWithMatchMetadata;
		
		private final HashSet<String> pOriginalFilesWithMetadata;
		private final HashSet<String> pExtractedTextFilesWithMetadata;
		private final HashSet<String> pTokensFilesWithMetadata;
		private final HashSet<String> pPosLemmaFilesWithMetadata;
		private final HashSet<String> pParseFilesWithMetadata;
		
		public Deleter(final Path textDir, final Path textPosLemmaDir, final HashSet<String> originalFilenamesWithUrlMetadata, final HashSet<String> posLemmaFilenamesWithMatchMetadata) {
			pTextDir = textDir;
			pTextOriginalDir = pTextDir.resolve(CommonPaths.TEXT_ORIGINAL_DIR);
			pTextTokensDir = pTextDir.resolve(CommonPaths.TEXT_TOKENS_DIR);
			pTextPosLemmaDir = textPosLemmaDir;
			pTextParseDir = pTextDir.resolve(CommonPaths.TEXT_PARSE_DIR);
			
			pOriginalFilenamesWithUrlMetadata = originalFilenamesWithUrlMetadata;
			pPosLemmaFilenamesWithMatchMetadata = posLemmaFilenamesWithMatchMetadata;
			
			pOriginalFilesWithMetadata = new HashSet<>();
			pExtractedTextFilesWithMetadata = new HashSet<>();
			pTokensFilesWithMetadata = new HashSet<>();
			pPosLemmaFilesWithMetadata = new HashSet<>();
			pParseFilesWithMetadata = new HashSet<>();
		}
		
		@Override
		public boolean test(final String[] parts) {
			final String originalFileName = parts[0];
			final String extractedTextFileName = parts[2];
			final String tokensFileName = parts[3];
			final String posLemmaFileName = parts[4];
			final String parseFilename = parts[5];
			
			if (isConsistent(originalFileName, extractedTextFileName, tokensFileName, posLemmaFileName, parseFilename)) {
				pOriginalFilesWithMetadata.add(originalFileName);
				pExtractedTextFilesWithMetadata.add(extractedTextFileName);
				pTokensFilesWithMetadata.add(tokensFileName);
				pPosLemmaFilesWithMetadata.add(posLemmaFileName);
				pParseFilesWithMetadata.add(parseFilename);
				
				return false;
			}
			
			return true;
		}
		
		private boolean isConsistent(final String originalFileName, final String extractedTextFileName, final String tokensFileName, final String posLemmaFileName, final String parseFilename) {
			return pOriginalFilenamesWithUrlMetadata.contains(originalFileName) &&
					pPosLemmaFilenamesWithMatchMetadata.contains(posLemmaFileName) &&
					allFilesExist(
							pTextOriginalDir.resolve(extractedTextFileName),
							pTextTokensDir.resolve(tokensFileName),
							pTextPosLemmaDir.resolve(posLemmaFileName),
							pTextParseDir.resolve(parseFilename)
						);
		}
		
		private static boolean allFilesExist(final Path extractedTextFile, final Path tokensFile, final Path posLemmaFile, final Path parseFile) {
			return
					Files.exists(extractedTextFile, LinkOption.NOFOLLOW_LINKS) &&
					Files.exists(tokensFile, LinkOption.NOFOLLOW_LINKS) &&
					Files.exists(posLemmaFile, LinkOption.NOFOLLOW_LINKS) &&
					Files.exists(parseFile, LinkOption.NOFOLLOW_LINKS);
		}
		
		public HashSet<String> getOriginalFilesWithMetadata() {
			return pOriginalFilesWithMetadata;
		}
		
		public HashSet<String> getExtractedTextFilesWithMetadata() {
			return pExtractedTextFilesWithMetadata;
		}
		
		public HashSet<String> getTokensFilesWithMetadata() {
			return pTokensFilesWithMetadata;
		}
		
		public HashSet<String> getPosLemmaFilesWithMetadata() {
			return pPosLemmaFilesWithMetadata;
		}
		
		public HashSet<String> getParseFilesWithMetadata() {
			return pParseFilesWithMetadata;
		}
		
	}
	
}

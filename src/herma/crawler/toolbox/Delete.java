/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.crawler.toolbox;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import herma.crawler.toolbox.common.Common;
import herma.crawler.toolbox.common.CommonPaths;

public class Delete {
	
	public static void main(final String[] args) {
		final boolean mock;
		switch (args.length) {
			case 3:
				mock = false;
				break;
			case 4:
				if ("mock".equals(args[3])) {
					mock = true;
					break;
				}
			default:
				System.err.println("Invalid command line arguments.");
				System.err.println("Expecting three or four arguments:");
				System.err.println("crawler output directory");
				System.err.println("file with names of files to delete");
				System.err.println("input column (\"" + CommonPaths.ORIGINAL_DIR + "\", \"" + CommonPaths.TEXT_ORIGINAL_DIR + "\", \"" + CommonPaths.TEXT_TOKENS_DIR + "\", \"" + CommonPaths.TEXT_POS_LEMMA_DIR + "\", or \"" + CommonPaths.TEXT_PARSE_DIR + "\")");
				System.err.println("optional: \"mock\" to not actually delete files");
				System.exit(1);
				return;
		}
		
		final int inputColumn = loadInputColumn(args[2]);
		if (inputColumn < 0) {
			System.exit(1);
			return;
		}
		
		final FileSystem fs = FileSystems.getDefault();
		
		final Path crawlDir = Common.loadPath(fs, args[0]);
		final Path inputFile = Common.loadPath(fs, args[1]);
		
		final Path filesFile = crawlDir.resolve(CommonPaths.FILES_FILE);
		
		final HashSet<String> notDeletedFilenames;
		
		try {
			final HashSet<String> filenamesToDelete = loadFilenamesToDelete(inputFile);
			notDeletedFilenames = new HashSet<>(filenamesToDelete);
			
			if (mock) {
				final Deleter deleter = new Deleter(crawlDir, filenamesToDelete, inputColumn, path -> mockDelete(crawlDir, path));
				
				System.out.println("Mocking the deletion of files and entries in " + CommonPaths.FILES_FILE);
				FromMetadataFileDeletion.mockDeleteFromMetaFile(filesFile, deleter);
				deleter.finishDeletion();
				System.out.println("Deleted (mock).");
				
				System.out.println();
				
				System.out.println("Mocking the deletion of corresponding metadata:");
				mockDeleteFromMetadataFile(crawlDir, CommonPaths.URLS_FILE, deleter.getDeletedOriginalFilenames(), 3);
				mockDeleteFromMetadataFile(crawlDir, CommonPaths.MATCHES_FILE, deleter.getDeletedPosLemmaFilenames(), 0);
				
				notDeletedFilenames.removeAll(deleter.getDeletedFilenames());
			} else {
				final Deleter deleter = new Deleter(crawlDir, filenamesToDelete, inputColumn, Delete::delete);
				
				System.out.println("Deleting files and entries in " + CommonPaths.FILES_FILE);
				FromMetadataFileDeletion.deleteFromMetaFile(filesFile, deleter);
				deleter.finishDeletion();
				System.out.println("Deleted.");
				
				System.out.println();
				
				System.out.println("Deleting corresponding metadata:");
				deleteFromMetadataFile(crawlDir, CommonPaths.URLS_FILE, deleter.getDeletedOriginalFilenames(), 3);
				deleteFromMetadataFile(crawlDir, CommonPaths.MATCHES_FILE, deleter.getDeletedPosLemmaFilenames(), 0);
				
				notDeletedFilenames.removeAll(deleter.getDeletedFilenames());
			}
		} catch (final IOException e) {
			throw new UncheckedIOException(e);
		}
		
		System.out.println();
		System.out.println("Done.");
		
		if (notDeletedFilenames.isEmpty())
			return;
		
		System.out.println();
		System.out.println("For some files no matching metadata could be found; they were not deleted:");
		for (final String filename : notDeletedFilenames)
			System.out.println(filename);
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
	
	private static HashSet<String> loadFilenamesToDelete(final Path inputFile) throws IOException {
		try (final Stream<String> lines = Files.lines(inputFile, StandardCharsets.UTF_8)) {
			return lines.collect(Collectors.toCollection(HashSet::new));
		}
	}
	
	private static void mockDeleteFromMetadataFile(final Path crawlDir, final String metadataFilename, final HashSet<String> deletedFilenames, final int column) throws IOException {
		System.out.println(metadataFilename);
		FromMetadataFileDeletion.mockDeleteFromMetaFile(crawlDir.resolve(metadataFilename), fields -> deletedFilenames.contains(fields[column]));
	}
	
	private static void deleteFromMetadataFile(final Path crawlDir, final String metadataFilename, final HashSet<String> deletedFilenames, final int column) throws IOException {
		System.out.println(metadataFilename);
		FromMetadataFileDeletion.deleteFromMetaFile(crawlDir.resolve(metadataFilename), fields -> deletedFilenames.contains(fields[column]));
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
		
		private final Path pOriginalDir;
		private final Path pTextDir;
		private final Path pTextOriginalDir;
		private final Path pTextTokensDir;
		private final Path pTextPosLemmaDir;
		private final Path pTextParseDir;
		private final Path pTextParserInputDir;
		
		private final HashSet<String> pFilenamesToDelete;
		private final int pInputColumn;
		
		private final Consumer<? super Path> pDeletionOperation;
		
		private final HashSet<String> pDeletedFilenames;
		private final HashSet<String> pOriginalFilenamesToDelete;
		private final HashSet<String> pOriginalFilenamesToNotDelete;
		private final HashSet<String> pDeletedPosLemmaFilenames;
		
		public Deleter(final Path crawlDir, final HashSet<String> filenamesToDelete, final int inputColumn, final Consumer<? super Path> deletionOperation) {
			pOriginalDir = crawlDir.resolve(CommonPaths.ORIGINAL_DIR);
			pTextDir = crawlDir.resolve(CommonPaths.TEXT_DIR);
			pTextOriginalDir = pTextDir.resolve(CommonPaths.TEXT_ORIGINAL_DIR);
			pTextTokensDir = pTextDir.resolve(CommonPaths.TEXT_TOKENS_DIR);
			pTextPosLemmaDir = pTextDir.resolve(CommonPaths.TEXT_POS_LEMMA_DIR);
			pTextParseDir = pTextDir.resolve(CommonPaths.TEXT_PARSE_DIR);
			pTextParserInputDir = pTextDir.resolve(CommonPaths.TEXT_PARSER_INPUT_DIR);
			
			pFilenamesToDelete = filenamesToDelete;
			pInputColumn = inputColumn;
			
			pDeletionOperation = deletionOperation;
			
			pDeletedFilenames = new HashSet<>();
			pDeletedPosLemmaFilenames = new HashSet<>();
			pOriginalFilenamesToDelete = new HashSet<>();
			pOriginalFilenamesToNotDelete = new HashSet<>();
		}
		
		@Override
		public boolean test(final String[] parts) {
			final String filename = parts[pInputColumn];
			final String originalFileName = parts[0];
			
			if (pFilenamesToDelete.contains(filename)) {
				final String posLemmaFileName = parts[4];
				final String parseFilename = parts[5];
				
				pDeletionOperation.accept(pTextOriginalDir.resolve(parts[2]));
				pDeletionOperation.accept(pTextTokensDir.resolve(parts[3]));
				pDeletionOperation.accept(pTextPosLemmaDir.resolve(posLemmaFileName));
				pDeletionOperation.accept(pTextParseDir.resolve(parseFilename));
				pDeletionOperation.accept(pTextParserInputDir.resolve(parseFilename));
				
				pDeletedFilenames.add(filename);
				pDeletedPosLemmaFilenames.add(posLemmaFileName);
				pOriginalFilenamesToDelete.add(originalFileName);
				return true;
			}
			
			pOriginalFilenamesToNotDelete.add(originalFileName);
			return false;
		}
		
		public void finishDeletion() {
			pDeletedFilenames.removeAll(pOriginalFilenamesToNotDelete);
			pOriginalFilenamesToDelete.removeAll(pOriginalFilenamesToNotDelete);
			for (final String originalFileName : pOriginalFilenamesToDelete)
				pDeletionOperation.accept(pOriginalDir.resolve(originalFileName));
		}
		
		public HashSet<String> getDeletedFilenames() {
			return pDeletedFilenames;
		}
		
		public HashSet<String> getDeletedOriginalFilenames() {
			return pOriginalFilenamesToDelete;
		}
		
		public HashSet<String> getDeletedPosLemmaFilenames() {
			return pDeletedPosLemmaFilenames;
		}
		
	}
	
}

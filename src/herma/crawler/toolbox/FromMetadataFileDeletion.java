/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.crawler.toolbox;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.function.Predicate;

import herma.crawler.toolbox.common.Common;

/**
 * Contains utility methods for deleting from the web crawler metadata files.
 */
public class FromMetadataFileDeletion {
	
	/**
	 * Deletes lines from a metadata file that match some condition
	 * tested by a {@link Predicate}.
	 * <p>
	 * {@link Predicate#test(Object)}
	 * is called for each line of the metadata file,
	 * split by {@link Common#TAB_PATTERN},
	 * and the line is deleted if the method
	 * returns {@code true}. 
	 * </p>
	 * 
	 * @param metafile
	 * (a {@link Path} locating) the metadata file;
	 * not {@code null}
	 * 
	 * @param deletionDecision
	 * a {@link Predicate};
	 * not {@code null}
	 * 
	 * @throws IOException
	 * if an I/O error occurs
	 */
	public static void deleteFromMetaFile(final Path metafile, final Predicate<? super String[]> deletionDecision) throws IOException {
		final Path tempFile = tempCopy(metafile);
		try (final BufferedWriter writer = Files.newBufferedWriter(metafile, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.WRITE)) {
			try (final BufferedReader reader = Files.newBufferedReader(tempFile, StandardCharsets.UTF_8)) {
				while (true) {
					final String line = reader.readLine();
					if (line == null)
						break;
					final String[] columns = Common.TAB_PATTERN.split(line, -1);
					if (deletionDecision.test(columns))
						continue;
					writer.append(line).append('\n');
				}
			}
			writer.flush();
		}
		Files.deleteIfExists(tempFile);
	}
	
	/**
	 * Reads all lines from a metadata file, calls the
	 * {@link Predicate#test(Object)}
	 * method of a given {@link Predicate}
	 * for all of them and logs those to
	 * {@link System#out}
	 * for which the method returns {@code true}.
	 * In other words, it logs those lines that
	 * {@link #deleteFromMetaFile(Path, Predicate)}
	 * would delete when called with the same arguments
	 * as this method.
	 * 
	 * @param metafile
	 * (a {@link Path} locating) the metadata file;
	 * not {@code null}
	 * 
	 * @param deletionDecision
	 * a {@link Predicate};
	 * not {@code null}
	 * 
	 * @throws IOException
	 * if an I/O error occurs
	 */
	public static void mockDeleteFromMetaFile(final Path metafile, final Predicate<? super String[]> deletionDecision) throws IOException {
		try (final BufferedReader reader = Files.newBufferedReader(metafile, StandardCharsets.UTF_8)) {
			while (true) {
				final String line = reader.readLine();
				if (line == null)
					break;
				if (deletionDecision.test(Common.TAB_PATTERN.split(line, -1))) {
					System.out.print("\tMocking the deletion of metadata line: ");
					System.out.println(line);
				}
			}
		}
	}
	
	private static Path tempCopy(final Path file) throws IOException {
		final Path tempFile = Files.createTempFile(file.getParent(), file.getFileName().toString(), "");
		try (final OutputStream outputStream = Files.newOutputStream(tempFile, StandardOpenOption.WRITE)) {
			Files.copy(file, outputStream);
			outputStream.flush();
		}
		return tempFile;
	}
	
}

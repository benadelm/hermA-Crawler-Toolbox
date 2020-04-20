/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.crawler.toolbox.common;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Contains methods and constants needed by several tools.
 */
public class Common {
	
	/**
	 * A {@link Pattern} that matches a single tabulator
	 * ({@literal "\t"}) character.
	 * Many output files of the web crawler consist of lines
	 * with columns separated by this character, so that
	 * such lines can be split into columns using the
	 * {@link Pattern#split(CharSequence)}
	 * method (or one of the related methods) of this
	 * {@link Pattern}.
	 */
	public static final Pattern TAB_PATTERN = Pattern.compile(Pattern.quote("\t"));
	
	/**
	 * Turns a {@link String} into a normalized absolute {@link Path}
	 * of the given {@link FileSystem}.
	 * 
	 * @param fileSystem
	 * the {@link FileSystem},
	 * not {@code null}
	 * 
	 * @param pathString
	 * the {@link String} to be turned into a {@link Path},
	 * not {@code null}
	 * 
	 * @return
	 * a normalized absolute {@link Path}
	 * of the given {@link FileSystem},
	 * corresponding to the given {@link String};
	 * not {@code null}
	 */
	public static Path loadPath(final FileSystem fileSystem, final String pathString) {
		return fileSystem.getPath(pathString).toAbsolutePath().normalize();
	}
	
	/**
	 * Takes a {@link Stream} of {@link String} objects,
	 * splits them using {@link #TAB_PATTERN},
	 * selects the item at the specified index (if present)
	 * and returns a stream of these items.
	 * 
	 * @param lines
	 * a {@link Stream} of {@link String} objects,
	 * not {@code null}
	 * 
	 * @param columnIndex
	 * the (0-based) index of the item to select
	 * 
	 * @return
	 * a {@link Stream} of the selected items;
	 * not {@code null}
	 */
	public static Stream<String> selectColumn(final Stream<String> lines, final int columnIndex) {
		return lines
				.map(Common.TAB_PATTERN::splitAsStream)
				.map(s -> s.skip(columnIndex))
				.map(Stream::findFirst)
				.filter(Optional::isPresent)
				.map(Optional::get);
	}
	
}

/* This Source Code Form is subject to the terms of the hermA Licence.
 * If a copy of the licence was not distributed with this file, You have
 * received this Source Code Form in a manner that does not comply with
 * the terms of the licence.
 */
package herma.crawler.toolbox.common;

/**
 * Contains {@link String} constants with paths in the output
 * of the web crawler, which are needed by several tools.
 */
public class CommonPaths {
	
	/**
	 * The name of the directory inside the output directory
	 * where the original web documents are saved:
	 * {@value #ORIGINAL_DIR}
	 */
	public static final String ORIGINAL_DIR = "original";
	
	/**
	 * The name of the directory inside the output directory
	 * where the text extracted from web documents
	 * and the results of linguistically processing that text
	 * are saved:
	 * {@value #TEXT_DIR}
	 */
	public static final String TEXT_DIR = "txt";
	
	/**
	 * The name of the directory inside the text directory
	 * ({@link #TEXT_DIR}) where the text extracted from
	 * the web documents, without any linguistic processing,
	 * is saved:
	 * {@value #TEXT_ORIGINAL_DIR}
	 */
	public static final String TEXT_ORIGINAL_DIR = "01_Originale";
	
	/**
	 * The name of the directory inside the text directory
	 * ({@link #TEXT_DIR}) where the token sequences of the
	 * texts extracted from the web documents are saved:
	 * {@value #TEXT_TOKENS_DIR}
	 */
	public static final String TEXT_TOKENS_DIR = "02_Tokenisierung";
	
	/**
	 * The name of the directory inside the text directory
	 * ({@link #TEXT_DIR}) where the results of part-of-speech
	 * (POS) tagging and lemmatizing the texts are saved:
	 * {@value #TEXT_POS_LEMMA_DIR}
	 */
	public static final String TEXT_POS_LEMMA_DIR = "03_POS_Lemma";
	
	/**
	 * The name of the directory inside the text directory
	 * ({@link #TEXT_DIR}) where the results of parsing the
	 * texts are saved:
	 * {@value #TEXT_PARSE_DIR}
	 */
	public static final String TEXT_PARSE_DIR = "04_Parse";
	
	/**
	 * The name of the directory inside the text directory
	 * ({@link #TEXT_DIR}) where the parser inputs are saved
	 * if they are different from the data in
	 * {@link #TEXT_POS_LEMMA_DIR}:
	 * {@value #TEXT_PARSER_INPUT_DIR}
	 */
	public static final String TEXT_PARSER_INPUT_DIR = "03a_ParserInput";
	
	/**
	 * The name of the file inside the output directory
	 * where the filenames (in {@link #ORIGINAL_DIR})
	 * and URLs of the saved files
	 * and their meta-data are saved:
	 * {@value #URLS_FILE}
	 */
	public static final String URLS_FILE = "urls.txt";
	
	/**
	 * The name of the file inside the output directory
	 * where the files in
	 * {@link #ORIGINAL_DIR}
	 * are linked to one or more texts extracted from
	 * them (stored in sub-directories of
	 * {@link #TEXT_DIR})
	 * and where the text extraction method used
	 * is documented:
	 * {@value #FILES_FILE}
	 */
	public static final String FILES_FILE = "files.txt";
	
	/**
	 * The name of the file inside the output directory
	 * where the phrases from the extracted texts
	 * matching the key phrases are listed:
	 * {@value #MATCHES_FILE}
	 */
	public static final String MATCHES_FILE = "matches.txt";
	
}

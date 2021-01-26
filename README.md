Toolbox with various tools for analyzing or post-processing outputs of the [hermA-Crawler](https://github.com/benadelm/hermA-Crawler).

# System Requirements

The software has been tested with Windows 10 and Linux. Being written in Java, it should run on any platform Java supports; you will need a Java runtime to run the software. It has been developed and tested with Java 8, but newer versions may also work.

# Running

You should be familiar with running Java programs from the command line.

Every tool has a corresponding main class to be specified in order to run the tool, for example:

	java herma.crawler.toolbox.MetadataConsistencyCheck <arguments...>

for the Metadata Consistency Check (see below). Depending on the current directory or when using a `.jar` bundle, you may have to specify a [class path](https://en.wikipedia.org/wiki/Classpath), for example:

	java -cp some/directory herma.crawler.toolbox.MetadataConsistencyCheck <arguments...>

where `some/directory/herma/crawler/toolbox/MetadataConsistencyCheck.class` is the class file of the main class or

	java -cp hermA-Crawler-Toolbox.jar herma.crawler.toolbox.MetadataConsistencyCheck <arguments...>

for invoking the main class from the `.jar` file `hermA-Crawler-Toolbox.jar`.

# The Tools

The tools deal with the metadata files `urls.txt`, `files.txt` and `matches.txt` in the crawler output directory as well as files in its `original` sub-directory and the sub-directories of `txt` (`01_Originale`, `02_Tokenisierung`, `03_POS_Lemma`, `03a_ParserInput`, `04_Parse`):

	urls.txt
	files.txt
	matches.txt
	original/...
	txt/01_Originale/...
	txt/02_Tokenisierung/...
	txt/03_POS_Lemma/...
	txt/03a_ParserInput/...
	txt/04_Parse/...

## Metadata Consistency Check

Main class: `herma.crawler.toolbox.MetadataConsistencyCheck`

Arguments:

1. the path to the crawler output directory

This tool checks the consistency of the data stored by the web crawler. Specifically, it checks

* integrity within the metadata:
  * every entry in `urls.txt` has an associated entry in `files.txt`, and vice versa;
  * every entry in `files.txt` has an associated entry in `matches.txt`, and vice versa;
* integrity with respect to saved files:
  * every entry in `urls.txt` has an associated file in the `original` directory;
  * every entry in `files.txt` has associated files in the sub-directories of `txt` (excluding `03a_ParserInput`).

Inconsistencies are reported, but no attempt is made to correct those inconsistencies.

## Deletion of Orphaned Files and Metadata

Main class: `herma.crawler.toolbox.DeleteOrphaned`

Arguments:

1. the path to the crawler output directory
2. optional: `mock` to not actually delete files

This tool removes all metadata entries with missing files (‘orphaned metadata’) and deletes all files with missing metadata (‘orphaned files’). This is the only way to enforce consistency without having to rely on heuristics for correcting corrupt metadata.

Only `urls.txt`, `files.txt`, `matches.txt` and the files in `original` and the sub-directories of `txt` will be affected; other metadata like the list of visited URLs will not be changed.

If you specify `mock` as the second command-line argument, the tool will not actually delete files but print instead which files and which metadata lines it would delete.

When deleting from a metadata file such as `urls.txt`, the tool creates a backup copy of that file (in the same directory) and replaces the original file. The backup is deleted once the original file has been successfully replaced. If any error occurs, the metadata file may be in an inconsistent state (that is, incompletely written), but the backup copy with the state before running the tool is still there, similarly named (for example, `urls.txt6486380869255500438`).

## Consistent Deletion

Main class: `herma.crawler.toolbox.Delete`

Arguments:

1. the path to the crawler output directory
2. a file with the names of files to delete
3. the column (in `files.txt`) in which to look for the file names; possible values:
   * `original` for files in the `original` directory (first column of `files.txt`)
   * `01_Originale` for files in the `01_Originale` sub-directory of `txt` (third column of `files.txt`)
   * `02_Tokenisierung` for files in the `02_Tokenisierung` sub-directory of `txt` (fourth column of `files.txt`)
   * `03_POS_Lemma` for files in the `03_POS_Lemma` sub-directory of `txt` (fifth column of `files.txt`)
   * `04_Parse` for files in the `04_Parse` sub-directory of `txt` (sixth column of `files.txt`)
4. optional: `mock` to not actually delete files

This tool takes a list of (names of) files saved by the web crawler and deletes them together with all corresponding files and metadata. For example, given a list of filenames from the `original` directory the tool would delete these files as well as the corresponding files in the `txt` directory and all metadata (in `urls.txt`, `files.txt` and `matches.txt`) referring to any of these files. This can be useful for ‘filtering’ steps where (potentially many) false positives in the crawler output have been identified (manually or automatically) and should be deleted without corrupting the metadata.

The file given as second argument to the tool is a UTF-8 plain text file with the name of one file to delete per line. The file names must consistently come from one of the output directories (`original` or one of the sub-directories of `txt`) of the web crawler. The `files.txt` metadata file is used to relate the given filenames to all corresponding other files. For example, let the input list of files to delete contain `c05_kasseler-stottertherapie_000086.pdf` and let `files.txt` contain the following metadata line:

	c05_kasseler-stottertherapie_000086.pdf	XPDF pdftotext	c05_kasseler-stottertherapie_000086_d.txt	c05_kasseler-stottertherapie_000086_d.txt	c05_kasseler-stottertherapie_000086_d.txt	c05_kasseler-stottertherapie_000086_d.txt

Then this line will be deleted; the file `c05_kasseler-stottertherapie_000086.pdf` will be deleted from the directory `original`, and files named `c05_kasseler-stottertherapie_000086_d.txt` will be deleted from the sub-directories of `txt`.

Other metadata like the list of visited URLs will not be changed; only `urls.txt`, `files.txt` and `matches.txt` will be affected.

The third command-line argument is used to determine the column of `files.txt` where to look for the filenames in the input list. In this case, `original` would have been specified to have the tool look for `c05_kasseler-stottertherapie_000086.pdf` in the first column, which corresponds to files in the `original` directory.

If there is more than one line in `files.txt` corresponding to the same line in `urls.txt` and at least one of them is not deleted, the corresponding line in `urls.txt` and the corresponding file in `original` are not deleted either.

**WARNING:** The files are **deleted** from the file system. They are **not** moved to a *trash bin* or *recycle bin* directory (like they would when pressing ‘delete’ in a file browser) and it may be impossible to restore them. Therefore, it is *strongly recommended* to make a backup copy of the web crawler output before running this tool.

If you specify `mock` as the fourth command-line argument, the tool will not actually delete files but print instead which files and which metadata lines it would delete.

When deleting from a metadata file such as `urls.txt`, the tool creates a backup copy of that file (in the same directory) and replaces the original file. The backup is deleted once the original file has been successfully replaced. If any error occurs, the metadata file may be in an inconsistent state (that is, incompletely written), but the backup copy with the state before running the tool is still there, similarly named (for example, `urls.txt6486380869255500438`).

## Token-Based Duplicate Detection

Main class: `herma.crawler.toolbox.TokenBasedDuplicateFinder`

Arguments:

1. the path to the crawler output directory
2. the path to a file where to write the output

Although the web crawler does not request the same URL more than once, it may still happen to download the same web page more than one time. For example, web servers may send the same data for different URLs. This tool identifies files in the crawler output where text extraction resulted in exactly the same token sequence (as stored in the files in the `02_Tokenisierung` sub-directory of `txt`). Sentence boundaries are ignored, so files are considered equivalent if they are tokenized to the same tokens in the same order, even if these token sequences are split differently into sentences.

The output is a UTF-8 plain text file with every line corresponding to one token sequence shared by at least two files in `02_Tokenisierung`. The lines consist of the names of these files, separated by tabulator characters. For example:

	mycrawl_bvitg_005361_d.txt	mycrawl_bvitg_005391_d.txt	mycrawl_ztg-nrw_000472_d.txt
	mycrawl_bkk-extraplus_000193_a.txt	mycrawl_extra-plus_000306_a.txt

In this example, the three files `mycrawl_bvitg_005361_d.txt`, `mycrawl_bvitg_005391_d.txt` and `mycrawl_ztg-nrw_000472_d.txt` share the same token sequence, and the two files `mycrawl_bkk-extraplus_000193_a.txt` and `mycrawl_extra-plus_000306_a.txt` share another token sequence.

The tool works by building a hash table of token sequences in memory, so you may want to increase Java’s heap memory budget (`-Xmx` option, see [Java options](https://docs.oracle.com/javase/7/docs/technotes/tools/windows/java.html)).

## Visited Hosts Statistics (‘<span lang="de">Umtriebe</span>’)

Main class: `herma.crawler.toolbox.Umtriebe`

Arguments:

1. the path to the crawler output directory
2. the path to a file where to write the output

This tool reads from the `processedurls` sub-directory of `meta` the lists of URLs processed (but not necessarily saved) by the web crawler and outputs a list of all host names occurring there, together with the respective number of URLs. This can be useful for getting an overview of where the crawler has been searching (and how intensively) or determining whether the crawler has possibly been distracted towards implausible web sites.

In contrast, the file `hosts.txt` from the crawler output contains only the counts of *relevant web documents* per host. 

The output is a UTF-8 plain text file. Every line in the file corresponds to one web host and consists of two fields separated by one tabulator character:

1. the web host (such as `blast.ncbi.nlm.nih.gov` or `accessdenied.abn.ergo.de`)
2. the number of URLs with that host component

For example:

	www.apple.com	1023
	seelisch-gesund-aufwachsen.de	21
	www.spleens4you.de	618
	europa.eu	2388

In this example the web crawler has processed a total of 1023 URLs with host `www.apple.com`, 21 URLs with host `seelisch-gesund-aufwachsen.de`, 618 URLs with host `www.spleens4you.de`, and 2388 URLs with host `europa.eu`.

The list is sorted lexicographically by host components, from right to left: `.com` hosts come before `.de` hosts, which come before `.net` hosts, which come before `.org` hosts and so on. The same principle applies within top-level domains (`mozilla.org` comes before `wikipedia.org`), sub-domains (`support.mozilla.org` comes before `www.mozilla.org` comes before `de.wikipedia.org` comes before `en.wikipedia.org`) and further down.

For those whom it may interest: When looking for a concise expression to refer to these statistics we humorously used the term ‘<span lang="de">Umtriebe</span>’ in German to refer to the places on the Internet visited by the crawler. The word is a nominalization of the adjective ‘<span lang="de">umtriebig</span>’ used to describe people who roam a lot of places poking their nose into or getting involved in a variety of issues.

## Match Statistics

Main class: `herma.crawler.toolbox.MatchStatistics`

Arguments:

1. path to a keyphrases file
2. path to the `matches.txt` from a crawler output
3. path to the output file for keyphrase statistics
4. path to the output file for match statistics

This tool produces some (very basic) statistics about the keyphrase matches that caused the crawler to save the web documents it has saved. This can be useful for determining the usefulness of a used set of keyphrases or refining it for subsequent runs of the web crawler (for example, by removing phrases that turned out to produce too many false positives).

The format of the keyphrases file is the same as in the web crawler input, which means that you may use the same file you used as input to the web crawler. However, you may also use another file (which may be useful, for example, when testing against an alternative set of keyphrases).

Both output files are UTF-8 plain text files. Every line consists of two fields separated by a tabulator character. The first field is an absolute frequency (count); the lines are sorted in descending order by this field. In the keyphrase statistics file, the second field is a keyphrase (as read from the keyphrases file); in the match statistics file, the second field is a match string (as read from the `matches.txt` input file).

In the match statistics file, the count in the first field of a line specifies how often exactly the contents of the second field of that line appeared as a match string in `matches.txt`. ‘Exactly’ means in particular that this is case-sensitive.

In the keyphrase statistics file, the count the first field of a line specifies how many match strings from `matches.txt` matched the keyphrase in the second field. However, the matching logic used here is *not* fully equivalent to the logic used in the web crawler itself: A match string and a keyphrase match if (and only if) they have the same number of words and every word of the keyphrase appears as a substring (ignoring case) of the corresponding word in the match string. Unlike in the web crawler, no lemmatization of the match string is performed. Also note that one keyphrase may match more than one match string.

A match statistics file could look like this:

	617	Telematikinfrastruktur
	373	gematik
	259	Telemedizin
	243	Telematik
	...

So `Telematikinfrastruktur` appears 617 times as a match string in `matches.txt`, `gematik` 373 times and so on.

A keyphrase statistics file could look like this:

	1201	Telematik
	851	Telemedizin
	444	gematik
	434	E-Health

So 1201 entries from `matches.txt` have a match string matching `Telematik` (this includes the above occurrences of `Telematikinfrastruktur`), 851 entries have a match string matching `Telemedizin`, and so on.

## Merging

Main class: `herma.crawler.toolbox.Merge`

Arguments:

1. the path to the shortlist file
2. the column (in `files.txt`) where to look for file names on the shortlist; possible values:
   * `original` for files in the `original` directory (first column of `files.txt`)
   * `01_Originale` for files in the `01_Originale` sub-directory of `txt` (third column of `files.txt`)
   * `02_Tokenisierung` for files in the `02_Tokenisierung` sub-directory of `txt` (fourth column of `files.txt`)
   * `03_POS_Lemma` for files in the `03_POS_Lemma` sub-directory of `txt` (fifth column of `files.txt`)
   * `04_Parse` for files in the `04_Parse` sub-directory of `txt` (sixth column of `files.txt`)
3. the path to the target directory
4. After these three arguments the tool accepts an arbitrary number of paths to input directories, which are web crawler output directories.

This tool merges the output from two or more runs of the web crawler by copying files to common directories and combining the metadata, trying to retain only one version where URLs have been downloaded in more than one crawler run. It copies the files from the `original` directory and the sub-directories of `txt` of the specified crawler output directories to corresponding sub-directories of the target directory and concatenates the metadata files (`urls.txt`, `files.txt`, `matches.txt`) into corresponding files there. Other directories and files are not copied. The target directory has to exist and should be empty (sub-directories will be created by the tool, failing if they already exist).

If files corresponding to the same URL are present in more than one of the crawler output directories, the tool tries to select only one of them to be retained in the merged version. To give the tool a hint you have to specify a *shortlist* of files you want retained. (The file may be empty.) If among several files belonging to the same URL there is a file that is also on the shortlist, that file is copied to the output directory. Otherwise (if none of the files in question is on the shortlist) the file from the last specified input directory is used. If *more than one* of the files is on the shortlist, only the one from the input directory with the smallest index is used and a warning (`duplicate in shortlist`) is issued.

After choosing a file to retain the tool checks the corresponding tokenization against the tokenizations corresponding to the other files in question. Files whose tokenization does not match the tokenization of the file to be retained are retained, too. The tool creates a file (`merge-info.txt`) in the target directory where such decisions are documented.

The tool does not rename files, so if there are two (or more) files with the same name to be copied into the same directory, merging fails. The simplest way to avoid this is to use different prefixes when running the crawler.

The *shortlist file* is a UTF-8 plain text file in which every line is exactly one filename. The `merge-info.txt` file created by the tool is a UTF-8 plain text file with every line corresponding to one retained file from an `original` directory. Files that were the only file corresponding to their URL are not listed. Every line consists of two or more fields separated by tabulator characters; the first field is the file name, the second field gives the reason why this file was retained:

* `on shortlist`: The file is retained because it is on the shortlist. The following fields (if present) are the names of files that were not copied to the target directory because this file was preferred to them.
* `last`: There is no file on the shortlist for the corresponding URL, so this file is retained because it is from the last specified input directory. The following fields (if present) are the names of files that were not copied to the target directory because this file was preferred to them.
* `tokens differ`: Another file corresponding to the same URL has been selected to be retained, but this file has a different token sequence and is therefore retained, too. The following field is the name of the file originally selected to be retained.

The list is sorted by filename.

Do not specify any of the crawler output directories as target directory. That would lead to loss of metadata in the affected crawler output directory. However, if the target directory is different from all crawler output directories, this tool does not change anything in the crawler output directories (only in the target directory).
package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;

import java.io.*;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Created by Christopher Alan Mosher on 2017-08-10
 */
public class GedcomExtract {
    private static final Logger log = Logger.getLogger("");

    private final File fileGedcom;
    private final File fileIds;
    private final File fileSkeletonIds;

    private final Set<String> extracts = new HashSet<>();
    private final Set<String> skeletons = new HashSet<>();

    private GedcomTree gt;

    private GedcomExtract(final String filenameGedcom, final String filenameIds, final String filenameSkeletonIds) {
        this.fileGedcom = new File(filenameGedcom);
        this.fileIds = new File(filenameIds);
        this.fileSkeletonIds = filenameSkeletonIds == null ? null : new File(filenameSkeletonIds);
    }

    public static void main(final String... args) throws InvalidLevel, IOException {
        if (args.length < 2 || 3 < args.length) {
            throw new IllegalArgumentException("usage: java -jar gedcom-extract.jar in.ged in.ids [skel.ids]");
        } else {
            new GedcomExtract(args[0], args[1], args.length > 2 ? args[2] : null).main();
        }
    }

    private void main() throws IOException, InvalidLevel {
        readValues(this.fileIds, this.extracts);
        readValues(this.fileSkeletonIds, this.skeletons);

        loadGedcom();

        extract();

        System.err.flush();
        System.out.flush();
    }

    private void loadGedcom() throws IOException, InvalidLevel {
        final Charset charset = Gedcom.getCharset(this.fileGedcom);
        this.gt = Gedcom.parseFile(fileGedcom, charset, false);
    }

    private static void readValues(final File file, final Set<String> set) throws IOException {
        if (file == null) {
            return;
        }
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        int c = 0;
        for (String s = in.readLine(); s != null; s = in.readLine()) {
            set.add(s);
            ++c;
        }
        in.close();
        log.info("read "+Integer.toString(c)+" lines from "+file.getName());
    }

    private void extract() {
        for (final TreeNode<GedcomLine> rec : this.gt.getRoot()) {
            final GedcomLine recLn = rec.getObject();
            final String id = recLn.getID();
            final GedcomTag tag = recLn.getTag();
            if (tag.equals(GedcomTag.HEAD)) {
                extractHead(rec);
            } else if (tag.equals(GedcomTag.TRLR)) {
                extractFull(rec);
            } else if (tag.equals(GedcomTag.FAM)) {
                // TODO only extract full if HUSB is extracted, or HUSB doesn't exists
                // (the way it works now will extract all events to husb and
                // wife tree if they are in different trees)
                extractFull(rec);
            } else if (this.extracts.contains(id)) {
                extractFull(rec);
            } else if (this.skeletons.contains(id)) {
                extractSkeleton(rec);
            }
        }
    }

    private static final Set<GedcomTag> setIndiChildSkel = Collections.unmodifiableSet(new HashSet<GedcomTag>() {{
        add(GedcomTag.NAME);
        add(GedcomTag.SEX);
        add(GedcomTag.REFN);
        add(GedcomTag.RIN);
    }});

    private void extractSkeleton(final TreeNode<GedcomLine> record) {
        System.out.println(record);
        if (record.getObject().getTag().equals(GedcomTag.INDI)) {
            for (final TreeNode<GedcomLine> c : record) {
                final GedcomLine ln = c.getObject();
                final GedcomTag tag = ln.getTag();
                if (setIndiChildSkel.contains(tag)) {
                    System.out.println(c);
                } else if (tag.equals(GedcomTag.BIRT) || tag.equals(GedcomTag.DEAT)) {
                    System.out.println(c);
                    for (final TreeNode<GedcomLine> c2 : c) {
                        if (c2.getObject().getTag().equals(GedcomTag.DATE)) {
                            System.out.println(c2);
                        }
                    }
                } else if (tag.equals(GedcomTag.FAMC) || tag.equals(GedcomTag.FAMS)) {
                    if (ln.isPointer() && (this.extracts.contains(ln.getPointer()) || this.skeletons.contains(ln.getPointer()))) {
                        System.out.println(c);
                    }
                }
            }
        } else {
            log.warning("Non-INDI skeleton requested; only writing level 0 line: "+record);
        }
    }

    private static void extractHead(final TreeNode<GedcomLine> record) {
        if (record.getObject().getTagString().equals("_ROOT")) {
            // TODO add smart _ROOT processing
            log.warning("Dropping .HEAD._ROOT line: "+record);
        } else {
            System.out.println(record);
        }
        record.forEach(GedcomExtract::extractHead);
    }

    private static void extractFull(final TreeNode<GedcomLine> record) {
        System.out.println(record);
        record.forEach(GedcomExtract::extractFull);
    }
}

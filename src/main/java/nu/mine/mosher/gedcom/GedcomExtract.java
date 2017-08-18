package nu.mine.mosher.gedcom;

import joptsimple.OptionParser;
import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;

import java.io.*;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static nu.mine.mosher.logging.Jul.log;

/**
 * Created by Christopher Alan Mosher on 2017-08-10
 */
public class GedcomExtract implements Gedcom.Processor {
    private final GedcomExtractOptions options;

    private final Set<String> extracts = new HashSet<>();
    private final Set<String> skeletons = new HashSet<>();

    public static void main(final String... args) throws InvalidLevel, IOException {
        final GedcomExtractOptions options = new GedcomExtractOptions(new OptionParser());
        options.parse(args);
        new Gedcom(options, new GedcomExtract(options)).main();
    }

    private GedcomExtract(final GedcomExtractOptions options) {
        this.options = options;
    }

    @Override
    public boolean process(final GedcomTree tree) {
        try {
            readSet(this.options.fileIndis(), this.extracts);
            readSet(this.options.fileSkeletons(), this.skeletons);

            extract(tree);
        } catch (final Throwable e) {
            throw new IllegalStateException(e);
        }

        return false;
    }

    private static void readSet(final File file, final Set<String> set) throws IOException {
        if (file == null) {
            return;
        }
        final BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(file), "UTF-8"));
        for (String s = in.readLine(); s != null; s = in.readLine()) {
            set.add(s);
        }
        in.close();
    }

    private void extract(final GedcomTree tree) {
        for (final TreeNode<GedcomLine> rec : tree.getRoot()) {
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
                    if (ln.isPointer() && (this.extracts.contains(ln.getPointer()) || this.skeletons
                        .contains(ln.getPointer()))) {
                        System.out.println(c);
                    }
                }
            }
        } else {
            log().warning("Non-INDI skeleton requested; only writing level 0 line: " + record);
        }
    }

    private static void extractHead(final TreeNode<GedcomLine> record) {
        if (record.getObject().getTagString().equals("_ROOT")) {
            // TODO add smart _ROOT processing
            log().warning("Dropping .HEAD._ROOT line: " + record);
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

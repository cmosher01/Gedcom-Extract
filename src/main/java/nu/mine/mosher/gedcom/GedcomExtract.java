package nu.mine.mosher.gedcom;

import nu.mine.mosher.collection.TreeNode;
import nu.mine.mosher.gedcom.exception.InvalidLevel;
import nu.mine.mosher.mopper.ArgParser;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static nu.mine.mosher.logging.Jul.log;

/**
 * Created by Christopher Alan Mosher on 2017-08-10
 */
public class GedcomExtract {
    private final GedcomExtractOptions options;
    private final Set<String> extracts = new HashSet<>();
    private final Set<String> skeletons = new HashSet<>();
    private GedcomTree tree;

    public static void main(final String... args) throws InvalidLevel, IOException {
        log();
        new GedcomExtract(new ArgParser<>(new GedcomExtractOptions()).parse(args).verify()).main();
        System.out.flush();
        System.err.flush();
    }

    private GedcomExtract(final GedcomExtractOptions options) {
        this.options = options;
    }

    private void main() throws IOException, InvalidLevel {
        if (this.options.help) {
            return;
        }
        readGedcom();
        readValues();
        readSkels();
        extract();
    }

    private void readGedcom() throws IOException, InvalidLevel {
        this.tree = Gedcom.readFile(new BufferedInputStream(new FileInputStream(this.options.gedcom)));
    }

    private void readValues() throws IOException {
        new BufferedReader(new InputStreamReader(new FileInputStream(FileDescriptor.in), StandardCharsets.UTF_8)).lines().forEach(this.extracts::add);
    }

    private void readSkels() throws IOException {
        if (this.options.fringe == null) {
            return;
        }
        new BufferedReader(new InputStreamReader(new FileInputStream(this.options.fringe), StandardCharsets.UTF_8))
            .lines()
            .forEach(this.skeletons::add);
    }

    private void extract() {
        // TODO only extract full if HUSB is extracted, or HUSB doesn't exists
        // (the way it works now will extract all events to husb and
        // wife tree if they are in different trees)
        for (final TreeNode<GedcomLine> rec : this.tree.getRoot()) {
            final GedcomLine recLn = rec.getObject();
            final String id = recLn.getID();
            final GedcomTag tag = recLn.getTag();
            if (tag.equals(GedcomTag.HEAD)) {
                extractHead(rec);
            } else if (tag.equals(GedcomTag.TRLR)) {
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
                final String sTag = ln.getTagString();
                if (setIndiChildSkel.contains(tag) || sTag.equals("_XY")) {
                    System.out.println(c);
                } else if (tag.equals(GedcomTag.BIRT) || tag.equals(GedcomTag.DEAT)) {
                    // TODO only extract preferred BIRT or DEAT
                    System.out.println(c);
                    for (final TreeNode<GedcomLine> c2 : c) {
                        if (c2.getObject().getTag().equals(GedcomTag.DATE)) {
                            System.out.println(c2);
                        }
                    }
                } else if (tag.equals(GedcomTag.FAMC) || tag.equals(GedcomTag.FAMS)) {
                    if (ln.isPointer() && (
                        this.extracts.contains(ln.getPointer()) || this.skeletons.contains(ln.getPointer()))) {
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

package nu.mine.mosher.gedcom;

import joptsimple.OptionParser;
import joptsimple.OptionSpec;

import java.io.File;
import java.util.List;

public class GedcomExtractOptions extends GedcomOptions {
    private final OptionSpec<File> files;

    public GedcomExtractOptions(final OptionParser parser) {
        super(parser);
        this.files = parser.nonOptions("indis.id.in [skeletons.id.in]").ofType(File.class).describedAs("FILES");
    }

    private List<File> files() {
        return this.files.values(get());
    }

    public File fileIndis() {
        if (files().size() <= 0) {
            throw new IllegalArgumentException("Missing indis.id.in file.");
        }
        return files().get(0);
    }

    public File fileSkeletons() {
        if (files().size() <= 1) {
            return null;
        }
        return files().get(1);
    }
}

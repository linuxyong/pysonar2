package org.yinwang.pysonar.demos;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.yinwang.pysonar.*;

import java.io.File;
import java.util.*;
import java.util.Map.Entry;
import java.util.regex.Pattern;


/**
 * Collects per-file hyperlinks, as well as styles that require the
 * symbol table to resolve properly.
 */
class Linker {

    private static final Pattern CONSTANT = Pattern.compile("[A-Z_][A-Z0-9_]*");

    // Map of file-path to semantic styles & links for that path.
    @NotNull
    private Map<String, List<StyleRun>> fileStyles = new HashMap<>();

    private File outDir;  // where we're generating the output html
    private String rootPath;

    // prevent duplication in def and ref links
    Set<Integer> seenDef = new HashSet<>();
    Set<Integer> seenRef = new HashSet<>();


    /**
     * Constructor.
     *
     * @param root   the root of the directory tree being indexed
     * @param outdir the html output directory
     */
    public Linker(String root, File outdir) {
        rootPath = root;
        outDir = outdir;
    }


    public void findLinks(@NotNull Indexer indexer) {
        _.msg("Adding xref links");

        FancyProgress progress = new FancyProgress(indexer.getAllBindings().size(), 50);
        for (Binding b : indexer.getAllBindings()) {
            addSemanticStyles(b);
            processDef(b);
            progress.tick();

        }

        // highlight definitions
        _.msg("\nAdding ref links");
        progress = new FancyProgress(indexer.getReferences().size(), 50);

        for (Entry<Ref, List<Binding>> e : indexer.getReferences().entrySet()) {
            processRef(e.getKey(), e.getValue());
            progress.tick();
        }


//        for (List<Diagnostic> ld: indexer.semanticErrors.values()) {
//            for (Diagnostic d: ld) {
//                processDiagnostic(d);
//            }
//        }

//        for (List<Diagnostic> ld: indexer.parseErrors.values()) {
//            for (Diagnostic d: ld) {
//                processDiagnostic(d);
//            }
//        }
    }


    private void processDef(@NotNull Binding b) {
        int hash = b.hashCode();

        if (b.getFile() == null || b.getIdentStart() < 0 || seenDef.contains(hash)) {
            return;
        }

        seenDef.add(hash);
        StyleRun style = new StyleRun(StyleRun.Type.ANCHOR, b.getIdentStart(), b.getIdentLength());
        style.message = b.getType().toString();
        style.id = Integer.toString(Math.abs(hash));
        style.url = style.id;

        Set<Ref> refs = b.getRefs();
        style.highlight = new ArrayList<>();


        for (Ref r : refs) {
            style.highlight.add(Integer.toString(Math.abs(r.hashCode())));
        }
        addFileStyle(b.getFile(), style);
    }


    void processRef(@NotNull Ref ref, @NotNull List<Binding> bindings) {
        int hash = ref.hashCode();

        if (!seenRef.contains(hash)) {
            seenRef.add(hash);

            StyleRun link = new StyleRun(StyleRun.Type.LINK, ref.start(), ref.length());
            link.id = Integer.toString(Math.abs(hash));

            List<String> typings = new ArrayList<>();
            for (Binding b : bindings) {
                typings.add(b.getType().toString());
            }
            link.message = _.joinWithSep(typings, " | ", "{", "}");

            link.highlight = new ArrayList<>();
            for (Binding b : bindings) {
                link.highlight.add(Integer.toString(Math.abs(b.hashCode())));
            }

            link.url = Integer.toString(Math.abs(bindings.get(0).hashCode()));
            addFileStyle(ref.getFile(), link);
        }
    }


    /**
     * Returns the styles (links and extra styles) generated for a given file.
     *
     * @param path an absolute source path
     * @return a possibly-empty list of styles for that path
     */
    public List<StyleRun> getStyles(String path) {
        return stylesForFile(path);
    }


    private List<StyleRun> stylesForFile(String path) {
        List<StyleRun> styles = fileStyles.get(path);
        if (styles == null) {
            styles = new ArrayList<StyleRun>();
            fileStyles.put(path, styles);
        }
        return styles;
    }


    private void addFileStyle(String path, StyleRun style) {
        stylesForFile(path).add(style);
    }


    /**
     * Add additional highlighting styles based on information not evident from
     * the AST.
     */
    private void addSemanticStyles(@NotNull Binding b) {
        boolean isConst = CONSTANT.matcher(b.getName()).matches();
        switch (b.getKind()) {
            case SCOPE:
                if (isConst) {
                    addSemanticStyle(b, StyleRun.Type.CONSTANT);
                }
                break;
            case VARIABLE:
                addSemanticStyle(b, isConst ? StyleRun.Type.CONSTANT : StyleRun.Type.IDENTIFIER);
                break;
            case PARAMETER:
                addSemanticStyle(b, StyleRun.Type.PARAMETER);
                break;
            case CLASS:
                addSemanticStyle(b, StyleRun.Type.TYPE_NAME);
                break;
        }
    }


    private void addSemanticStyle(@NotNull Binding b, StyleRun.Type type) {
        String file = b.getFile();
        if (file != null) {
            addFileStyle(file, new StyleRun(type, b.getIdentStart(), b.getIdentLength()));
        }
    }


    private void processDiagnostic(@NotNull Diagnostic d) {
        StyleRun style = new StyleRun(StyleRun.Type.WARNING, d.start, d.end - d.start);
        style.message = d.msg;
        style.url = d.file;
        addFileStyle(d.file, style);
    }


    /**
     * Generate a URL for a reference to a binding.
     *
     * @param binding  the referenced binding
     * @param filename the path containing the reference, or null if there was an error
     */
    @Nullable
    private String toURL(@NotNull Binding binding, String filename) {

        if (binding.isBuiltin()) {
            return binding.getFile();
        }

        String destPath;
        if (binding.getType().isModuleType()) {
            destPath = binding.getType().asModuleType().getFile();
        } else {
            destPath = binding.getFile();
        }

        if (destPath == null) {
            return null;
        }

        String anchor = "#" + binding.getQname();
        if (binding.getFile().equals(filename)) {
            return anchor;
        }

        if (destPath.startsWith(rootPath)) {
            String relpath;
            if (filename != null) {
                relpath = _.relPath(filename, destPath);
            } else {
                relpath = destPath;
            }

            if (relpath != null) {
                return relpath + ".html" + anchor;
            } else {
                return anchor;
            }
        } else {
            return "file://" + destPath + anchor;
        }
    }

}

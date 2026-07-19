import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;

/** JDK-only structural validation for environments without the Android SDK. */
public class ValidateProject {
    public static void main(String[] args) throws Exception {
        Path root = args.length == 0 ? Path.of(".") : Path.of(args[0]);
        Path main = root.resolve("app/src/main");
        List<Path> xmlFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(main)) {
            paths.filter(path -> path.toString().endsWith(".xml")).forEach(xmlFiles::add);
        }
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        for (Path xml : xmlFiles) factory.newDocumentBuilder().parse(xml.toFile());

        Set<String> definitions = new HashSet<>();
        Set<String> references = new HashSet<>();
        Pattern idDefinition = Pattern.compile("@\\+id/([A-Za-z0-9_.]+)");
        Pattern namedDefinition = Pattern.compile("<(string|color|style|dimen|integer|bool|array|string-array)[^>]*\\bname=\"([A-Za-z0-9_.]+)\"");
        Pattern reference = Pattern.compile("(?<!@android:)@([a-zA-Z-]+)/([A-Za-z0-9_.]+)");

        for (Path xml : xmlFiles) {
            String content = Files.readString(xml, StandardCharsets.UTF_8);
            Matcher ids = idDefinition.matcher(content);
            while (ids.find()) definitions.add("id/" + ids.group(1));
            Matcher names = namedDefinition.matcher(content);
            while (names.find()) definitions.add(names.group(1) + "/" + names.group(2));
            Matcher refs = reference.matcher(content);
            while (refs.find()) if (!"+id".equals(refs.group(1))) references.add(refs.group(1) + "/" + refs.group(2));

            Path relative = main.resolve("res").relativize(xml);
            if (relative.getNameCount() >= 2) {
                String folder = relative.getName(0).toString().replaceAll("-.*$", "");
                if (folder.equals("layout") || folder.equals("drawable") || folder.equals("xml")
                        || folder.equals("mipmap") || folder.equals("menu")) {
                    String name = xml.getFileName().toString().replaceFirst("\\.xml$", "");
                    definitions.add(folder + "/" + name);
                }
            }
        }
        Pattern javaReference = Pattern.compile("R\\.(id|layout|drawable|string|color|xml|style)\\.([A-Za-z0-9_]+)");
        List<Path> javaFiles = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(main.resolve("java"))) {
            javaFiles = paths.filter(path -> path.toString().endsWith(".java")).toList();
            for (Path source : javaFiles) {
                Matcher refs = javaReference.matcher(Files.readString(source, StandardCharsets.UTF_8));
                while (refs.find()) references.add(refs.group(1) + "/" + refs.group(2));
            }
        }

        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("JDK compiler module unavailable");
        DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
        try (StandardJavaFileManager manager = compiler.getStandardFileManager(diagnostics, null, StandardCharsets.UTF_8)) {
            Iterable<? extends JavaFileObject> units = manager.getJavaFileObjectsFromPaths(javaFiles);
            JavacTask task = (JavacTask) compiler.getTask(null, manager, diagnostics,
                    List.of("-proc:none", "--release", "17"), null, units);
            task.parse();
        }
        List<String> syntaxErrors = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
            if (diagnostic.getKind() == Diagnostic.Kind.ERROR)
                syntaxErrors.add((diagnostic.getSource() == null ? "Java" : diagnostic.getSource().getName())
                        + ":" + diagnostic.getLineNumber() + " " + diagnostic.getMessage(null));
        }
        if (!syntaxErrors.isEmpty()) throw new IllegalStateException("Java syntax errors: " + syntaxErrors);
        Set<String> missing = new HashSet<>(references);
        missing.removeAll(definitions);
        if (!missing.isEmpty()) throw new IllegalStateException("Missing resources: " + missing);

        String manifest = Files.readString(main.resolve("AndroidManifest.xml"), StandardCharsets.UTF_8);
        Matcher components = Pattern.compile("android:name=\"\\.([A-Za-z0-9_]+)\"").matcher(manifest);
        int componentCount = 0;
        while (components.find()) {
            componentCount++;
            Path source = main.resolve("java/com/astratechnologies/astrasathi/" + components.group(1) + ".java");
            if (!Files.exists(source)) throw new IllegalStateException("Missing manifest class: " + components.group(1));
        }

        System.out.println(xmlFiles.size() + "টি XML document valid।");
        System.out.println(references.size() + "টি resource reference resolved।");
        System.out.println(componentCount + "টি manifest component class resolved।");
        System.out.println(javaFiles.size() + "টি Android Java source syntax-valid।");
    }
}

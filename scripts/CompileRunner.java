import javax.tools.JavaCompiler;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Compiler bridge for slim JDK images that include jdk.compiler but omit the javac launcher. */
public class CompileRunner {
    public static void main(String[] args) throws Exception {
        if (args.length < 2) throw new IllegalArgumentException("Output directory and sources are required");
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) throw new IllegalStateException("Java compiler module is unavailable");
        StandardJavaFileManager files = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        List<File> sources = new ArrayList<>();
        for (String source : Arrays.copyOfRange(args, 1, args.length)) sources.add(new File(source));
        List<String> options = Arrays.asList("-encoding", "UTF-8", "-d", args[0]);
        boolean success = compiler.getTask(null, files, null, options, null,
                files.getJavaFileObjectsFromFiles(sources)).call();
        files.close();
        if (!success) throw new IllegalStateException("Compilation failed");
    }
}

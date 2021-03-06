package com.github.plusvic.yara.embedded;

import com.github.plusvic.yara.*;
import net.jcip.annotations.NotThreadSafe;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.easymock.EasyMock.createNiceMock;
import static org.junit.Assert.*;

/**
 * User: pba
 * Date: 6/7/15
 * Time: 6:38 PM
 */
@NotThreadSafe
public class YaraScannerImplTest {
    private static final String YARA_RULES = "import \"pe\"\n" +
            "rule HelloWorld : Hello World\n"+
            "{\n"+
            "\tmeta:\n" +
            "	my_identifier_1 = \"Some string data\"\n" +
            "	my_identifier_2 = 24\n" +
            "	my_identifier_3 = true\n" +
            "\tstrings:\n"+
            "\t\t$a = \"ⁱ\"wide\n"+
            "\t\t$c= \"Hello world\"\n"+
            "\t\t$b = { 48 65 6c 6c 6f 20 77 6f 72 6c 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 64 }\n"+
            "\n"+
            "\tcondition:\n"+
            "\t\tany of them\n"+
            "}" +
            "rule NoMatch \n"+
            "{\n"+
            "\tmeta:\n" +
            "	my_identifier_1 = \"Some string data\"\n" +
            "	my_identifier_2 = 24\n" +
            "	my_identifier_3 = true\n" +
            "\tstrings:\n"+
            "\t\t$a = \"nomatch\"\n"+
            "\n"+
            "\tcondition:\n"+
            "\t\t$a\n"+
            "}";

    private YaraImpl yara;

    @Before
    public void setup() {
        this.yara = new YaraImpl();
    }

    @After
    public void teardown() throws Exception {
        yara.close();
    }


    @Test(expected = IllegalArgumentException.class)
    public void testCreateNoRules() throws IOException {
        new YaraScannerImpl(createNiceMock(YaraLibrary.class), 0);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCreateNoLibrary() {
        new YaraScannerImpl(null, 1);
    }

    @Test
    public void testCreate() {
        new YaraScannerImpl(createNiceMock(YaraLibrary.class), 1);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testWrongTimeout() {
        new YaraScannerImpl(createNiceMock(YaraLibrary.class), 1).setTimeout(-1);
    }

    @Test
    public void testSetCallback() throws Exception {
        //
        YaraCompilationCallback compileCallback = new YaraCompilationCallback() {
            @Override
            public void onError(ErrorLevel errorLevel, String fileName, long lineNumber, String message) {
                fail();
            }
        };

        YaraScanCallback scanCallback = new YaraScanCallback() {
            @Override
            public void onMatch(YaraRule v) {
            }
        };

        // Create compiler and get scanner
        try (YaraCompiler compiler = yara.createCompiler()) {
            compiler.setCallback(compileCallback);
            compiler.addRulesContent(YARA_RULES, null);

            try (YaraScanner scanner = compiler.createScanner()) {
                assertNotNull(scanner);

                scanner.setCallback(scanCallback);
            }
        }
    }

    @Test
    public void testScanMatch() throws Exception {
        // Write test file
        File temp = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        Files.write(Paths.get(temp.getAbsolutePath()), "Hello world".getBytes(), StandardOpenOption.WRITE);


        //
        YaraCompilationCallback compileCallback = new YaraCompilationCallback() {
            @Override
            public void onError(ErrorLevel errorLevel, String fileName, long lineNumber, String message) {
                fail();
            }
        };

        final AtomicBoolean match = new AtomicBoolean();

        YaraScanCallback scanCallback = new YaraScanCallback() {
            @Override
            public void onMatch(YaraRule v) {
                assertEquals("HelloWorld", v.getIdentifier());
                assertMetas(v.getMetadata());
                assertStrings(v.getStrings());
                assertTags(v.getTags());

                match.set(true);
            }
        };

        // Create compiler and get scanner
        try (YaraCompiler compiler = yara.createCompiler()) {
            compiler.setCallback(compileCallback);
            compiler.addRulesContent(YARA_RULES, null);

            try (YaraScanner scanner = compiler.createScanner()) {
                assertNotNull(scanner);

                scanner.setCallback(scanCallback);
                scanner.scan(temp);
            }
        }

        assertTrue(match.get());
    }

    @Test
    public void testScanNegateMatch() throws Exception {
        /*
            Negate and try matching on an UUID, we should have two matches
         */
        // Write test file
        File temp = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        Files.write(Paths.get(temp.getAbsolutePath()), UUID.randomUUID().toString().getBytes(),
                StandardOpenOption.WRITE);


        //
        YaraCompilationCallback compileCallback = new YaraCompilationCallback() {
            @Override
            public void onError(ErrorLevel errorLevel, String fileName, long lineNumber, String message) {
                fail();
            }
        };

        final AtomicInteger match = new AtomicInteger();

        YaraScanCallback scanCallback = new YaraScanCallback() {
            @Override
            public void onMatch(YaraRule v) {
                assertMetas(v.getMetadata());
                assertFalse(v.getStrings().next().getMatches().hasNext());

                match.incrementAndGet();
            }
        };

        // Create compiler and get scanner
        try (YaraCompiler compiler = yara.createCompiler()) {
            compiler.setCallback(compileCallback);
            compiler.addRulesContent(YARA_RULES, null);

            try (YaraScanner scanner = compiler.createScanner()) {
                scanner.setNotSatisfiedOnly(true);
                assertNotNull(scanner);

                scanner.setCallback(scanCallback);
                scanner.scan(temp);
            }
        }

        assertEquals(2, match.get());
    }

    @Test
    public void testScanNegateLimitMatch() throws Exception {
         /*
            Negate and try matching on an UUID with limit one,
            we should have a single match
         */
        // Write test file
        File temp = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        Files.write(Paths.get(temp.getAbsolutePath()), UUID.randomUUID().toString().getBytes(),
                StandardOpenOption.WRITE);


        //
        YaraCompilationCallback compileCallback = new YaraCompilationCallback() {
            @Override
            public void onError(ErrorLevel errorLevel, String fileName, long lineNumber, String message) {
                fail();
            }
        };

        final AtomicInteger match = new AtomicInteger();

        YaraScanCallback scanCallback = new YaraScanCallback() {
            @Override
            public void onMatch(YaraRule v) {
                assertMetas(v.getMetadata());
                assertFalse(v.getStrings().next().getMatches().hasNext());

                match.incrementAndGet();
            }
        };

        // Create compiler and get scanner
        try (YaraCompiler compiler = yara.createCompiler()) {
            compiler.setCallback(compileCallback);
            compiler.addRulesContent(YARA_RULES, null);

            try (YaraScanner scanner = compiler.createScanner()) {
                scanner.setNotSatisfiedOnly(true);
                scanner.setMaxRules(1);
                assertNotNull(scanner);

                scanner.setCallback(scanCallback);
                scanner.scan(temp);
            }
        }

        assertEquals(1, match.get());
    }

    @Test
    public void testScanNoMatch() throws Exception {
        // Write test file
        File temp = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        Files.write(Paths.get(temp.getAbsolutePath()), UUID.randomUUID().toString().getBytes(),
                StandardOpenOption.WRITE);


        //
        YaraCompilationCallback compileCallback = new YaraCompilationCallback() {
            @Override
            public void onError(ErrorLevel errorLevel, String fileName, long lineNumber, String message) {
                fail();
            }
        };

        final AtomicBoolean match = new AtomicBoolean();

        YaraScanCallback scanCallback = new YaraScanCallback() {
            @Override
            public void onMatch(YaraRule v) {
                match.set(true);
            }
        };

        // Create compiler and get scanner
        try (YaraCompiler compiler = yara.createCompiler()) {
            compiler.setCallback(compileCallback);
            compiler.addRulesContent(YARA_RULES, null);

            try (YaraScanner scanner = compiler.createScanner()) {
                assertNotNull(scanner);

                scanner.setCallback(scanCallback);
                scanner.scan(temp);
            }
        }

        assertFalse(match.get());
    }

    @Test
    public void testScanModule() throws Exception {
        // Write test file
        File temp = File.createTempFile(UUID.randomUUID().toString(), ".tmp");
        Files.write(Paths.get(temp.getAbsolutePath()), "Hello world".getBytes(), StandardOpenOption.WRITE);

        Map<String, String> args = new HashMap();
        args.put("pe", temp.getAbsolutePath());


        //
        YaraCompilationCallback compileCallback = new YaraCompilationCallback() {
            @Override
            public void onError(ErrorLevel errorLevel, String fileName, long lineNumber, String message) {
                System.out.println(String.format("%s: [%s:%d] %s", errorLevel, fileName, lineNumber, message));
                fail();
            }
        };

        final AtomicBoolean match = new AtomicBoolean();

        YaraScanCallback scanCallback = new YaraScanCallback() {
            @Override
            public void onMatch(YaraRule v) {
                match.set(true);
            }
        };

        // Create compiler and get scanner
        try (YaraCompiler compiler = yara.createCompiler()) {
            compiler.setCallback(compileCallback);
            compiler.addRulesContent(YARA_RULES, null);

            try (YaraScanner scanner = compiler.createScanner()) {
                assertNotNull(scanner);

                scanner.setCallback(scanCallback);
                scanner.scan(temp, args);
            }
        }

        assertTrue(match.get());
    }

    private void assertMetas(Iterator<YaraMeta> metas) {
        assertNotNull(metas);

        YaraMeta meta = metas.next();
        assertEquals(YaraMeta.Type.STRING, meta.getType());
        assertEquals("my_identifier_1", meta.getIndentifier());
        assertEquals("Some string data", meta.getString());

        meta = metas.next();
        assertEquals(YaraMeta.Type.INTEGER, meta.getType());
        assertEquals("my_identifier_2", meta.getIndentifier());
        assertEquals(24, meta.getInteger());

        meta = metas.next();
        assertEquals(YaraMeta.Type.BOOLEAN, meta.getType());
        assertEquals("my_identifier_3", meta.getIndentifier());
        assertEquals(1, meta.getInteger());

        assertFalse(metas.hasNext());
    }

    private void assertStrings(Iterator<YaraString> strings) {
        assertNotNull(strings);

        YaraString string = strings.next();

        assertEquals("$a", string.getIdentifier());

        Iterator<YaraMatch> matches = string.getMatches();
        assertTrue(matches.hasNext());

        YaraMatch match = matches.next();
        assertEquals(0, match.getOffset());
        assertEquals("Hello world", match.getValue());
        assertFalse(matches.hasNext());

        assertTrue(strings.hasNext());
        string = strings.next();
        matches = string.getMatches();
        assertTrue(matches.hasNext());

        match = matches.next();
        assertEquals(0, match.getOffset());
        assertEquals("48 65 6C 6C 6F 20 77 6F 72 6C 64", match.getValue());
        
    }

    private void assertTags(Iterator<String> tags) {
        assertNotNull(tags);

        assertEquals("Hello", tags.next());
        assertEquals("World", tags.next());
        assertFalse(tags.hasNext());
    }
}

# Bundled third-party software

Sharpener's own licence and attribution are in `META-INF/LICENSE` and
`META-INF/NOTICE`. Dependencies below are bundled unmodified. Their original
notices are preserved under `META-INF/third-party/<artifact>-<version>/`,
including notices obtained from corresponding upstream sources when the jar
omits them.

| Component | Version | Licence / upstream |
|---|---|---|
| Gson | 2.14.0 | Apache-2.0; https://github.com/google/gson |
| Commons Lang | 3.21.0 | Apache-2.0; https://commons.apache.org/proper/commons-lang/ |
| FlatLaf, including platform native libraries | 3.7.2 | Apache-2.0; https://github.com/JFormDesigner/FlatLaf |
| RSyntaxTextArea | 4.0.1 | BSD-3-Clause; https://github.com/bobbylight/RSyntaxTextArea |
| Reflections | 0.10.2 | WTFPL / Apache-2.0 as declared upstream; https://github.com/ronmamo/reflections |
| Javassist | 3.28.0-GA | Apache-2.0 option of MPL/LGPL/Apache licensing; https://github.com/jboss-javassist/javassist |
| SLF4J API | 1.7.32 | MIT; https://www.slf4j.org/ |
| JSR-305 annotations | 3.0.2 | BSD-3-Clause in original RI source; Maven POM instead declares Apache-2.0. Both upstream records are retained; https://code.google.com/archive/p/jsr-305/ |
| Error Prone annotations | 2.48.0 | Apache-2.0; https://github.com/google/error-prone |
| Java Native Access (JNA), including jnidispatch | 5.19.1 | Apache-2.0 option of dual Apache/LGPL licensing; https://github.com/java-native-access/jna |
| libffi embedded by JNA | source tree identifies 3.4.4 | MIT-style licence, Anthony Green, Red Hat, Inc. and others; https://github.com/libffi/libffi |

The vendored preference classes remain AGPLv3, copyright Corey Arthur, from
Burp-Montoya-Utilities; see Sharpener's `NOTICE` and their original headers.
No ownership or licence transfer is implied.

Montoya API is supplied by Burp and is not bundled. JUnit, Mockito, Byte Buddy,
Objenesis, OpenTest4J, JSpecify and API Guardian are test inputs and are not bundled.
Burp may supply its own copies of libraries through its parent class loader;
this inventory describes Sharpener's bytes, not Burp's runtime distribution.

Dated admission evidence, hashes, provenance limitations, security findings,
and release blockers are in `docs/dependency-review.md` in the source repository.

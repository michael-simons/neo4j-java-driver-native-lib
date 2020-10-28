## Build and run

This shows how to use GraalVM native image to create a shared library from a Java program.
To define an entry point for a native library inside your Java program, you will need the following artifact in your build:
`org.graalvm.sdk:graal-sdk`.

The library in this project uses the [Neo4j Java Driver](https://github.com/neo4j/neo4j-java-driver) to 
connect against a Neo4j instance and exeecute a single query.

The entry point to the shared library is defined in `org.neo4j.examples.drivernative.DriverNativeLib`
and looks like this:

```java
@CEntryPoint(name = "execute_query_and_print_results")
public static long executeQueryAndPrintResults(
    IsolateThread isolate, CCharPointer uri, CCharPointer password, CCharPointer query
) {
    // Some interaction with the driver and Neo4j.
    return 4711L;
}
```

The `@CEntryPoint` defines the entrypoint. Non trivial types are passed as pointers from the outside.
The `IsolateThread` is a control structure *required* as first argument by GraalVM.

The last ingredience to create a shared library is the appropriate flag to the `native-image` tool:
`--shared -H:Name=libneo4j` which is applied in the `pom.xml`.

On macOS the following artifacts are build

```
graal_isolate.h
graal_isolate_dynamic.h
libneo4j.dylib
libneo4j.h
libneo4j_dynamic.h
```

and can be used for example from C or through FFI in Ruby or Rust.

### Start a Neo4j Docker instance

```
docker run --publish=7474:7474 --publish=7687:7687 -e 'NEO4J_AUTH=neo4j/secret' -e NEO4J_ACCEPT_LICENSE_AGREEMENT=yes neo4j:4.0
```

### Install GraalVM

Download the JDK 11 version for your operating system from
https://github.com/oracle/graal/releases

All community downloads are available on
https://github.com/graalvm/graalvm-ce-builds/releases

Here we used version 20.1.0

Exports should be as follows:

```
export GRAALVM_HOME=/Library/Java/JavaVirtualMachines/graalvm-ce-java11-20.1.0/Contents/Home
export PATH=$PATH:$GRAALVM_HOME/bin
export JAVA_HOME=$GRAALVM_HOME
```

Then install the native image tool

```
gu install native-image
gu list
```

### Build the project

```
./mvnw clean package 
```

#### Usage from C

Please have a look at [`src/main/c/executeQuery.c`](https://github.com/michael-simons/neo4j-java-driver-native-lib/blob/master/src/main/c/executeQuery.c). 
The following is tested with:

```
gcc --version
Apple clang version 12.0.0 (clang-1200.0.32.2)
Target: x86_64-apple-darwin19.6.0
```

On macOS: Compile from the project roots with

```
gcc -Wall -Isrc/main/native -Ltarget -Itarget target/libneo4j.dylib src/main/c/executeQueryAndPrintResults.c -o target/executeQueryAndPrintResults
```

And run as

```
target/executeQueryAndPrintResults
```

#### Usage from Ruby

Please have a look at [`src/main/ruby/executeQuery.rb`](https://github.com/michael-simons/neo4j-java-driver-native-lib/blob/master/src/main/ruby/executeQuery.rb). 
The following is tested with:

```
ruby --version
ruby 2.7.2p137 (2020-10-01 revision 5445e04352) [x86_64-darwin19]
```

You need to have the Ruby `ffi` gem installed (via `gem install ffi`).
With FFI in place, run the script like this (from the root of this repository):

```
ruby src/main/ruby/executeQueryAndPrintResults.rb
```

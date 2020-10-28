package org.neo4j.examples.drivernative;

import static org.neo4j.examples.drivernative.ConfigHolder.SINGLE_CONNECTION_FAIL_FAST;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.UnmanagedMemory;
import org.graalvm.nativeimage.c.CContext;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.struct.CField;
import org.graalvm.nativeimage.c.struct.CPointerTo;
import org.graalvm.nativeimage.c.struct.CStruct;
import org.graalvm.nativeimage.c.struct.SizeOf;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.graalvm.word.PointerBase;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.Values;
import org.neo4j.driver.types.Node;
import org.neo4j.driver.types.TypeSystem;

import com.oracle.svm.core.c.ProjectHeaderFile;

@CContext(DriverNativeLib.CInterfaceTutorialDirectives.class)
public final class DriverNativeLib {

	/**
	 * Executes a query and prints the results from Java.
	 *
	 * @param isolate
	 * @param uri
	 * @param password
	 * @param query
	 * @return
	 */
	@CEntryPoint(name = "execute_query_and_print_results")
	public static long executeQueryAndPrintResults(IsolateThread isolate, CCharPointer uri, CCharPointer password,
		CCharPointer query) {

		final String _password = CTypeConversion.toJavaString(password);
		final String _uri = CTypeConversion.toJavaString(uri);
		final String _query = CTypeConversion.toJavaString(query);

		AuthToken auth = AuthTokens.basic("neo4j", _password);

		try (
			Driver driver = GraphDatabase.driver(_uri, auth, SINGLE_CONNECTION_FAIL_FAST);
			Session session = driver.session()
		) {
			TypeSystem typeSystem = driver.defaultTypeSystem();
			List<Map<String, Object>> unwrappedRecords = session.run(_query).list(r -> {
				Map<String, Object> unwrappedRecord = new HashMap<>();
				for (String key : r.keys()) {
					Value value = r.get(key);
					unwrappedRecord.put(key, typeSystem.MAP().isTypeOf(value) ? value.asMap() : value.asObject());
				}
				return unwrappedRecord;
			});
			unwrappedRecords.forEach(System.out::println);
			return unwrappedRecords.size();
		}
	}

	// A somewhat more complex example returning a record like structure.
	// The C record is defined in src/main/c/c_record.h

	static class CInterfaceTutorialDirectives implements CContext.Directives {

		@Override
		public List<String> getHeaderFiles() {
			return Collections
				.singletonList(ProjectHeaderFile.resolve("org.neo4j.examples.drivernative", "node.h"));
		}
	}

	@CStruct("c_node")
	interface CNodePointer extends PointerBase {

		@CField("id")
		void setId(long id);

		@CField("label")
		CCharPointer getLabel();

		@CField("label")
		void setLabel(CCharPointer label);

		@CField("name")
		CCharPointer getName();

		@CField("name")
		void setName(CCharPointer name);

		CNodePointer addressOf(int index);
	}

	@CPointerTo(CNodePointer.class)
	interface CNodePointerPointer extends PointerBase {
		void write(CNodePointer value);
	}

	@CEntryPoint(name = "execute_query_and_get_nodes")
	protected static int executeQueryAndGetNodes(@SuppressWarnings("unused") IsolateThread thread, CCharPointer uri,
		CCharPointer password, CCharPointer query, CNodePointerPointer out) {

		final String _password = CTypeConversion.toJavaString(password);
		final String _uri = CTypeConversion.toJavaString(uri);
		final String _query = CTypeConversion.toJavaString(query);

		AuthToken auth = AuthTokens.basic("neo4j", _password);

		try (
			Driver driver = GraphDatabase.driver(_uri, auth, SINGLE_CONNECTION_FAIL_FAST);
			Session session = driver.session()
		) {
			TypeSystem typeSystem = driver.defaultTypeSystem();
			List<Node> nodes = session.run(_query).list(r -> {
				List<Node> inner = new ArrayList<>();
				for (String key : r.keys()) {
					Value value = r.get(key);
					if (typeSystem.NODE().isTypeOf(value)) {
						inner.add(value.asNode());
					}
				}
				return inner;
			}).stream().flatMap(List::stream).collect(Collectors.toList());

			CNodePointer returnedNodes = UnmanagedMemory.calloc(nodes.size() * SizeOf.get(CNodePointer.class));
			int cnt = 0;
			for (Node node : nodes) {

				CNodePointer cNode = returnedNodes.addressOf(cnt++);
				cNode.setId(node.id());

				String firstLabel = StreamSupport.stream(node.labels().spliterator(), false).findFirst().orElse("n/a");
				String nameAttribute;
				Value identifier;
				if (node.containsKey("name")) {
					identifier = node.get("name");
				} else if (node.containsKey("title")) {
					identifier = node.get("title");
				} else {
					identifier = Values.NULL;
				}
				nameAttribute = identifier.asString();

				// The following method would allocate a pinned object that needs to be freed
				// I don't want to keep it around and allocate the byte array for the string myself.
				// cNode.setLabel(CTypeConversion.toCString(firstLabel).get());

				cNode.setLabel(toCCharPointer(firstLabel));
				cNode.setName(toCCharPointer(nameAttribute));
			}
			out.write(returnedNodes);
			return cnt;
		}
	}

	/**
	 * Creates a byte array for a string which can be freed after wards.
	 *
	 * @param string
	 * @return
	 */
	private static CCharPointer toCCharPointer(String string) {
		byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
		CCharPointer charPointer = UnmanagedMemory.calloc((bytes.length + 1) * SizeOf.get(CCharPointer.class));
		for (int i = 0; i < bytes.length; ++i) {
			charPointer.write(i, bytes[i]);
		}
		charPointer.write(bytes.length, (byte) 0);
		return charPointer;
	}

	@CEntryPoint(name = "free_results")
	protected static void freeResults(@SuppressWarnings("unused") IsolateThread thread, CNodePointer results,
		int numResults) {
		for (int i = 0; i < numResults; ++i) {
			UnmanagedMemory.free(results.addressOf(i).getLabel());
			UnmanagedMemory.free(results.addressOf(i).getName());
		}
		UnmanagedMemory.free(results);
	}

	private DriverNativeLib() {
	}
}

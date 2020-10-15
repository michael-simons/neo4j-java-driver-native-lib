package org.neo4j.examples.drivernative;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.graalvm.nativeimage.IsolateThread;
import org.graalvm.nativeimage.c.function.CEntryPoint;
import org.graalvm.nativeimage.c.type.CCharPointer;
import org.graalvm.nativeimage.c.type.CTypeConversion;
import org.neo4j.driver.AuthToken;
import org.neo4j.driver.AuthTokens;
import org.neo4j.driver.Config;
import org.neo4j.driver.Driver;
import org.neo4j.driver.GraphDatabase;
import org.neo4j.driver.Logging;
import org.neo4j.driver.Session;
import org.neo4j.driver.Value;
import org.neo4j.driver.types.TypeSystem;

public final class DriverNativeLib {

	private static final Config SINGLE_CONNECTION_FAIL_FAST = Config.builder()
		.withMaxConnectionPoolSize(1)
		.withEventLoopThreads(1)
		.withConnectionTimeout(10, TimeUnit.SECONDS)
		.withConnectionAcquisitionTimeout(10, TimeUnit.SECONDS)
		.withMaxTransactionRetryTime(20, TimeUnit.SECONDS)
		.withConnectionLivenessCheckTimeout(1, TimeUnit.MINUTES)
		.withLogging(Logging.javaUtilLogging(Level.FINE))
		.build();

	@CEntryPoint(name = "execute_query")
	public static long executeQuery(IsolateThread isolate, CCharPointer uri, CCharPointer password, CCharPointer query) {

		final String _password = CTypeConversion.toJavaString(password);
		final String _uri = CTypeConversion.toJavaString(uri);
		final String _query = CTypeConversion.toJavaString(query);

		AuthToken auth = AuthTokens.basic("neo4j", _password);

		try (Driver driver = GraphDatabase.driver(_uri, auth, SINGLE_CONNECTION_FAIL_FAST)) {
			TypeSystem typeSystem = driver.defaultTypeSystem();
			try (Session session = driver.session()) {
				List<Map<String, Object>> titles = session.run(_query).list(r -> {
					Map<String, Object> unwrappedRecord = new HashMap<>();
					for (String key : r.keys()) {
						Value value = r.get(key);
						unwrappedRecord.put(key, typeSystem.MAP().isTypeOf(value) ? value.asMap() : value.asObject());
					}
					return unwrappedRecord;
				});
				titles.forEach(System.out::println);
				return titles.size();
			}
		}
	}

	private DriverNativeLib() {
	}
}

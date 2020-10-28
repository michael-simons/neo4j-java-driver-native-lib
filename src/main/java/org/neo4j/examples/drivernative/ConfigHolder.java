package org.neo4j.examples.drivernative;

import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

import org.neo4j.driver.Config;
import org.neo4j.driver.Logging;

final class ConfigHolder {

	static final Config SINGLE_CONNECTION_FAIL_FAST = Config.builder()
		.withMaxConnectionPoolSize(1)
		.withEventLoopThreads(1)
		.withConnectionTimeout(10, TimeUnit.SECONDS)
		.withConnectionAcquisitionTimeout(10, TimeUnit.SECONDS)
		.withMaxTransactionRetryTime(20, TimeUnit.SECONDS)
		.withConnectionLivenessCheckTimeout(1, TimeUnit.MINUTES)
		.withLogging(Logging.javaUtilLogging(Level.FINE))
		.build();

	private ConfigHolder() {
	}
}

// Tested with
//   Apple clang version 12.0.0 (clang-1200.0.32.2)
//   Target: x86_64-apple-darwin19.6.0
// Compile from the project roots with
//   gcc -Wall -Ltarget -Itarget target/libneo4j.dylib src/main/c/executeQueryAndGetNodes.c -o target/executeQueryAndGetNodes
// And run as
//   target/executeQueryAndGetNodes

#include <stdio.h>
#include <stdlib.h>
#include "node.h"
#include "libneo4j.h"

int main(void) {

	graal_create_isolate_params_t isolate_params;
	graal_isolate_t* isolate;
	graal_isolatethread_t *thread = NULL;

	int ret = graal_create_isolate(&isolate_params, &isolate, &thread);
	if( ret != 0) {
		fprintf(stderr, "graal_create_isolate: %d\n", ret);
		exit(0);
	}

    // Prepare an output pointer
    c_node *nodes;
    int numResults = execute_query_and_get_nodes(
        thread, "bolt://localhost:7687", "secret",
        "MATCH (tom:Person {name: \"Tom Hanks\"})-[:ACTED_IN]->(tomHanksMovies) RETURN tom, tomHanksMovies",
        &nodes);

    int i;
    for (i = 0; i < numResults; i++) {
        fprintf(stdout, "(%ld:%s name:%s) \n", nodes[i].id, nodes[i].label, nodes[i].name);
    }

    free_results(thread, nodes, numResults);

    if (graal_detach_thread(thread) != 0) {
        fprintf(stderr, "graal_detach_thread error\n");
        return 1;
    }
}



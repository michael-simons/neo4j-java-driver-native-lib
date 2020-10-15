// Tested with
//   Apple clang version 12.0.0 (clang-1200.0.32.2)
//   Target: x86_64-apple-darwin19.6.0
// Compile from the project roots with
//   gcc -Wall -Ltarget -Itarget target/libneo4j.dylib src/main/c/executeQuery.c -o target/executeQuery
// And run as
//   target/executeQuery

#include <stdio.h>
#include <stdlib.h>
#include "libneo4j.h"

int main(void) {
	
	int ret;
	graal_create_isolate_params_t isolate_params;
	graal_isolate_t* isolate;
	graal_isolatethread_t *thread = NULL;

	ret = graal_create_isolate(&isolate_params, &isolate, &thread);
	if( ret != 0) {
		fprintf(stderr, "graal_create_isolate: %d\n", ret);
		exit(0);
	}
	
	int count = execute_query(thread, "bolt://localhost:7687", "secret", "MATCH (m:Movie) RETURN m");
	fprintf(stdout, "Number of movies printed: %d\n", count);
	
	if (graal_detach_thread(thread) != 0) {
	  fprintf(stderr, "graal_detach_thread error\n");
	  return 1;
	}
}

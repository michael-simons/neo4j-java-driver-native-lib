# Tested with
# ruby --version
# ruby 2.7.2p137 (2020-10-01 revision 5445e04352) [x86_64-darwin19]
#
# gem install ffi
# ruby src/main/ruby/executeQuery.rb

require 'ffi'

class GRAAL_CREATE_ISOLATE_PARAMS_T < FFI::Struct

  layout :version,:int,
     :reserved_address_space_size,:long,
     :auxiliary_image_path,:string,
     :auxiliary_image_reserved_space_size,:long
end

class GRAAL_ISOLATE_T < FFI::Struct
end

class GRAAL_ISOLATETHREAD_T < FFI::Struct
end

module LibNeo4j extend FFI::Library

  ffi_lib '.target/libneo4j.so' if RUBY_PLATFORM =~ /linux/
  ffi_lib './target/libneo4j.dylib' if RUBY_PLATFORM =~ /darwin/

  attach_function :graal_create_isolate, [ GRAAL_CREATE_ISOLATE_PARAMS_T.by_ref, :pointer, :pointer], :int
  attach_function :graal_detach_thread, [ :pointer ], :int  
  attach_function :ffi_execute_query, :execute_query, [:pointer, :string, :string, :string ], :long
    
  class << self
    def execute_query(uri, password, query)

      isolate_params = GRAAL_CREATE_ISOLATE_PARAMS_T.new

      cnt = 0
      FFI::MemoryPointer.new(:pointer) do |isolate|
        FFI::MemoryPointer.new(:pointer) do |thread|
          graal_create_isolate(isolate_params, isolate, thread)
          cnt = ffi_execute_query(thread.get_pointer(0), uri, password, query)
          graal_detach_thread(thread.get_pointer(0))
        end
      end
      cnt
    end

    # Make the attached method private and only use our orchestrated version
    private :ffi_execute_query, :graal_create_isolate, :graal_detach_thread
  end
end

LibNeo4j.execute_query('bolt://localhost:7687', 'secret', 'MATCH (m:Movie) RETURN m')

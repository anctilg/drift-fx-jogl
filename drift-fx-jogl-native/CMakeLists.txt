cmake_minimum_required(VERSION 3.11)

project(driftx)

set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -std=c++11")

enable_testing()

add_compile_definitions(GLEW_STATIC)
add_compile_definitions($<$<CONFIG:Debug>:WITH_LOGGING>)

# JNI
# TODO improve this (look for jdk8 JNI)
find_package(JNI REQUIRED)

message("JNI: " ${JNI_INCLUDE_DIRS})
message("JNI: " ${JNI_LIBRARIES})

# OpenGL
find_package(OpenGL REQUIRED)

include_directories(${JNI_INCLUDE_DIRS})

add_subdirectory(lib/glew)
add_subdirectory(lib/utils)

add_subdirectory(DriftFX)


list(APPEND LinkLibraries ${OPENGL_LIBRARIES})

add_custom_target(debug
	COMMAND ${CMAKE_COMMAND} -DCMAKE_BUILD_TYPE=Debug ${CMAKE_SOURCE_DIR}
	COMMAND ${CMAKE_COMMAND} --build ${CMAKE_BINARY_DIR} --target all
	COMMENT "Creating binaries in debug mode"
)

add_custom_target(release
	COMMAND ${CMAKE_COMMAND} -DCMAKE_BUILD_TYPE=Release ${CMAKE_SOURCE_DIR}
	COMMAND ${CMAKE_COMMAND} --build ${CMAKE_BINARY_DIR} --target all
	COMMENT "Creating binaries in release mode"
)

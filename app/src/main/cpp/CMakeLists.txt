cmake_minimum_required(VERSION 3.22.1)

project(omgminer LANGUAGES C CXX)

add_library(omgminer SHARED native_bridge.cpp)

find_library(android_log log)

target_link_libraries(omgminer PRIVATE ${android_log})

cmake_minimum_required(VERSION 3.4.1)

set(CMAKE_CXX_FLAGS -std=c++11)
link_directories(${PROJECT_SOURCE_DIR}/../openCVLibrary320/native/libs/${ANDROID_ABI} ${PROJECT_SOURCE_DIR}/../openCVLibrary320/native/3rdparty/libs/${ANDROID_ABI})
include_directories(${ANDROID_NDK}/sysroot/usr/include/arm-linux-androideabi ${PROJECT_SOURCE_DIR}/../openCVLibrary320/native/jni/include)

add_library(native-vision SHARED src/cpp/vision.cpp src/cpp/vision.c)

target_link_libraries(native-vision android EGL GLESv2 dl log z m
    libjpeg libwebp libpng libtiff libjasper IlmImf tegra_hal tbb
    opencv_ml.a opencv_java3.so opencv_imgproc.a opencv_highgui.a opencv_core.a)

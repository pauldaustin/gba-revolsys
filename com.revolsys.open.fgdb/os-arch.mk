ESRI_FILE_GBD_HOME=/opt/EsriFileGdb/1.4/${OS}/${ARCH}
ESRI_FILE_GBD_INCLUDE=$(ESRI_FILE_GBD_HOME)/include
JAVA_HOME=$(shell /usr/libexec/java_home -v 1.7)

TARGET_OBJ=target/o/libEsriFileGdbJni-${ARCH}-${OS}.o
TARGET_DIR=src/main/resources/native/${OS}/${ARCH}
TARGET_LIB=${TARGET_DIR}/libEsriFileGdbJni.${EXT}

all: clean ${TARGET_LIB}

clean:
	rm -f ${TARGET_OBJ} ${TARGET_LIB}

src/main/cxx/EsriFileGdb_wrap.cxx:

${TARGET_OBJ}: src/main/cxx/EsriFileGdb_wrap.cxx
	mkdir -p target/o
	clang++ \
		-c \
		-O2 \
		-m64 \
		-arch x86_64 \
		-stdlib=libc++ \
		-I${ESRI_FILE_GBD_INCLUDE} \
		-I${JAVA_HOME}/include/ \
		-I${JAVA_HOME}/include/darwin \
		-c src/main/cxx/EsriFileGdb_wrap.cxx \
		-o ${TARGET_OBJ}

${TARGET_LIB}: target/o/libEsriFileGdbJni-${ARCH}-${OS}.o
	mkdir -p ${TARGET_DIR}
	clang++ \
		${LDFLAGS} \
		-stdlib=libc++ \
		-m64 \
		-arch x86_64 \
		-fpic \
		-shared \
		-lpthread \
		-lfgdbunixrtl \
		-lFileGDBAPI \
		-L${ESRI_FILE_GBD_HOME}/lib/ \
		${TARGET_OBJ} \
		-o ${TARGET_LIB}

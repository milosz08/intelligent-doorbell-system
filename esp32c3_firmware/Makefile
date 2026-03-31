PROJECT_NAME := esp32_firmware

VER ?= 0.0.1-local-dev
VER_FLAG := -DCMD_APP_VER="$(VER)"

PORT ?= COM3 # communication port for ESP32 board
BAUD ?= 921600 # flashing speed

export VER # export version for doxygen (win/unix)

.PHONY: build build-app rebuild flash flash-app monitor run clean erase menuconfig help

# default target
all: run

# select esp32c3 chip
set-chip:
	idf.py set-target esp32c3

# normal building
build:
	idf.py build $(VER_FLAG)

# builds only the application binary (skips bootloader, partitions, data images)
build-app:
	idf.py app $(VER_FLAG)

# full rebuild (CMakeLists.txt or partition structure)
rebuild:
	idf.py reconfigure
	idf.py build $(VER_FLAG)

# flash with high speed (app, bootloader, partition table, storage)
flash: build
	idf.py -p $(PORT) -b $(BAUD) flash

# flash only app (without other partitions)
flash-app: build
	idf.py -p $(PORT) -b $(BAUD) app-flash

# monitor with decoder
monitor:
	idf.py -p $(PORT) monitor

# flash and run
run: flash monitor

# clean output files
clean:
	idf.py fullclean

# clean flash (remove NVS and other partitions)
erase:
	idf.py -p $(PORT) erase-flash

menuconfig:
	idf.py menuconfig

# generate docs via doxygen (only for .c and .h sources inside components and main directory)
docs:
	doxygen

# run simple python http  server for check doxygen documentation locally
check-docs:
	python -m http.server -d docs

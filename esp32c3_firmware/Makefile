PROJECT_NAME := esp32_firmware

VER ?= 0.0.1-local-dev
VER_FLAG := -DCMD_APP_VER="$(VER)"

PORT ?= /dev/ttyACM0 # communication port for ESP32 board
BAUD ?= 921600 # flashing speed

export VER # export version for doxygen (win/unix)

.PHONY: build build-app rebuild flash flash-app monitor run clean erase menuconfig help

# default target
all: run

# select esp32c3 chip
.PHONY: set-chip
set-chip:
	idf.py set-target esp32c3

# normal building
.PHONY: build
build:
	idf.py build $(VER_FLAG)

# builds only the application binary (skips bootloader, partitions, data images)
.PHONY: build-app
build-app:
	idf.py app $(VER_FLAG)

# full rebuild (CMakeLists.txt or partition structure)
.PHONY: rebuild
rebuild:
	idf.py reconfigure
	idf.py build $(VER_FLAG)

# flash with high speed (app, bootloader, partition table, storage)
.PHONY: flash
flash: build
	idf.py -p $(PORT) -b $(BAUD) flash

# flash only app (without other partitions)
.PHONY: flash-app
flash-app: build
	idf.py -p $(PORT) -b $(BAUD) app-flash

# monitor with decoder
.PHONY: monitor
monitor:
	idf.py -p $(PORT) monitor

# flash and run
.PHONY: run
run: flash monitor

# clean output files
.PHONY: clean
clean:
	idf.py fullclean

# clean flash (remove NVS and other partitions)
.PHONY: erase
erase:
	idf.py -p $(PORT) erase-flash

.PHONY: menuconfig
menuconfig:
	idf.py menuconfig

# generate docs via doxygen (only for .c and .h sources inside components and main directory)
.PHONY: docs
docs:
	doxygen

# run simple python http  server for check doxygen documentation locally
.PHONY: check-docs
check-docs:
	python -m http.server -d docs

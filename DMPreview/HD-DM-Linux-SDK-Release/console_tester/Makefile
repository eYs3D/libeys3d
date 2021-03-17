MAKEFILE = Makefile

ifeq ($(CPU),X86)
	TARGET = test_x86
	LIBS = -L../eSPDI -leSPDI_X86_64 \
	       -L../eSPDI/turbojpeg/x86_64/lib/ -lturbojpeg \
	       -lrt -lm -ldl -lpthread -lX11 -ludev
endif
ifeq ($(CPU), NVIDIA)
	TARGET = test_arm64_tx2
	LIBS = -L../eSPDI -leSPDI_$(CPU)_64 \
	       -L../eSPDI/turbojpeg/TX2/lib/ -lturbojpeg \
	       -lrt
endif

CFLAGS = -Wall -W -std=c++11
LFLAGS = -Wall -W 
LIBS += -lpthread
OBJECTS = main.o ColorPaletteGenerator.o RegisterSettings.o
INCPATH = -I../eSPDI -I../eSPDI/turbojpeg/armhf_32/include/


####### Implicit rules

.SUFFIXES: .o .cpp

.cpp.o:
	g++ -c $(CFLAGS) $(INCPATH) -g -o $@ $<

all: $(TARGET)

####### Build rules
$(TARGET): $(OBJECTS)
	g++ $(LFLAGS) -g -o $@ $^ $(LIBS) 

####### clean rules
clean: 
	rm $(OBJECTS) $(TARGET)
distclean:
	rm $(OBJECTS)

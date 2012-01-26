# Rapid-prototyping protection schemes with IEC 61850 #

The goal of this software is to automatically generate C/C++ code which reads and writes GOOSE and Sampled Value packets. Any valid IEC 61850 Substation Configuration Description (SCD) file, describing GOOSE and/or SV communications, can be used as the input. The output code is lightweight and platform-independent, so it can run on a variety of devices, including low-cost microcontrollers. It's ideal for rapid-prototyping new power system protection and control systems that require communications. But the software could also be used to implement the communications for any general purpose system.

This readme file describes how to set up the software, and its basic use.

*The code is meant to be a proof of concept, and is highly experimental. It has not been tested on many SCD files. The code is also still in development at the moment, so some features may be broken or incomplete.*

<img style="float:right" src="http://personal.strath.ac.uk/steven.m.blair/mbed-cropped-small.jpg" />

## Features ##

 - Implements sending and receiving GOOSE and Sampled Value packets
 - Lightweight, and suitable for low-cost microcontrollers
 - Platform-independent, and any C/C++ compiler should work
 - Performs validation of the SCD file, and reports any problems
 - Can optionally support fixed-length GOOSE encoding, which reduces GOOSE encoding time by approximately 25%
 - Supports initialisation of data types, and instance-specific data
 - Open source, under the GPL 2 license

## Installation ##

The software requires Eclipse, with the Eclipse Modeling Framework (EMF). Java Emitter Templates (JET) is needed for development, but not to run the code. It's easiest to start with the version of Eclipse that comes with the Modeling Tools bundle (see here: http://www.eclipse.org/downloads/).

There are two source code trees: `emf` (in Java), and `c` (obviously written in C). Each should be a separate project in Eclipse. The Java `emf` project directory is organised as follows:

 - `src/`
   - `scdCodeGenerator/`: code that does the bulk of the conversion from an SCD file to C code. The class `Main` contains the `main()` function for the project, and contains the filename for the input SCD file.
   - `scdCodeGeneratorTemplates/`: template classes that are generated by JET.
   - `ch/`: the EMF Java model implementation. These files are all automatically generated by EMF, but are included in the repo for convenience.
 - `model/`: the IEC 61850 XML Schema files. EMF uses these to generate the model.
 - `templates/`: the template source files used by JET.

### EMF import process ###

Note that the SCL model has been augmented to help with code generation, so the original IEC 61850 XML Schema files are not used to generate the model. Instead, the augmented model is defined in `scl.ecore`.

 1. Create an "EMF Project" called "emf", at the location of the repository code.
 2. Select "Ecore model" as the Model Importer type.
 3. Select `scl.ecore` from the File System as the model URI.
 4. Select `scl` as the root package to import.
 5. Create a new project of type "Convert Projects to JET Projects", and select the `emf` project. For the `emf` project, go to Project Properties > JET Settings, and set Template Containers to "templates", and Source Container to "src". Delete the `sclToCHelper` directory in the root of `emf` that was created before JET was configured correctly.
 6. Open `SCL.genmodel` and right-click on the root of the model tree. Select "Show Properties View" and ensure that "Compliance Level" is set to "6.0". Right-click on the root again and select "Generate Model Code". This should re-generate the model implementation files, and set up the project properly for using the generated code.
 7. The package `org.eclipse.em.query` may need to be add to the Plug-in Dependencies. This can be done from the tooltip for the compiler error at the `import` statements.
 8. You may need to include an additional JAR library for the project to compile. In the Project Properties for `emf`, go to Java Build Path > Libraries. Click on "Add External JARs..." and find `com.ibm.icu_4.2.1.v20100412.jar` (or a similar version). It should be located in the "plugins" directory within the Eclipse installation.

<!--
### EMF import process ###

 1. Create an "EMF Project" called "emf", at the location of the repository code.
 2. Select "XML Schema" as the Model Importer type. Select all the IEC 61850 XML Schema documents in the `emf/model` directory.
 3. Select the three root packages that are imported (although, only `scl` is used).
 4. Create a new project of type "Convert Projects to JET Projects", and select the `emf` project. For the `emf` project, go to Project Properties > JET Settings, and set Template Containers to "templates", and Source Container to "src". Delete the `sclToCHelper` directory in the root of `emf` that was created before JET was configured correctly.
 5. Open `SCL.genmodel` and right-click on the root of the model tree. Select "Show Properties View" and ensure that "Compliance Level" is set to "6.0". Right-click on the root again and select "Generate Model Code". This should re-generate the model implementation files, and set up the project properly for using the generated code.
-->

### C code project example ###

An example SCD file and a `main.c` file are provided. Many of the other C files are generated automatically. For the C code to compile on Windows, you should have MinGW installed and add `C:\MinGW\bin;` to `PATH` in the Project Properties > C/C++ Build > Environment options. (Other compilers should work too.) In Project Properties > C/C++ Build > Settings > GCC Compiler Includes, set `"${workspace_loc:/${ProjName}/Include}"` as an include path. Also, in Project Properties > C/C++ Build > Settings > MinGW C Linker, add `wpcap` and `ws2_32` (assuming you are using Windows) to "Libraries" and add `"${workspace_loc:/${ProjName}/Lib}"` and `"C:\MinGW\lib"` to "Library search path". The WinPcap library files and header files (from http://www.winpcap.org/devel.htm) have been included in the repository for convenience; the PC must also have WinPcap driver installed (from http://www.winpcap.org/install/default.htm).

The accompanying mbed microcontroller example code is available [here](http://mbed.org/users/sblair/programs/rapid61850example/lyox9z). A [Processing](http://processing.org/) GUI is located in the `/processing/PACWorldClient` directory. For this to work, execute the example C project, start the microcontroller code, then start the Processing executable.


## Using the code ##

First open the file `Main.java`. In the `Main` class, set the value of `SCD_FILENAME` to the filename of the SCD file. The SCD file should be in the same directory as the `Main.java` file. Run the Java project to generate the C implementation. If the SCD parser complains, ensure that the first two lines of the SCD file exactly match those from the example `scd.xml` in the repository.

A basic C `main()` function will look something like:

```C
#include "iec61850.h"

int length = 0;
unsigned char buffer[2048] = {0};

int main() {
	initialise_iec61850();											// initialise all data structures

	// send GOOSE packet
	E1Q1SB1.S1.C1.TVTRa_1.Vol.instMag.f = 1.024;					// set a value that appears in the dataset used by the "ItlPositions" GOOSE Control
	length = E1Q1SB1.S1.C1.LN0.ItlPositions.send(buffer, 1, 512);	// generate a goose packet, and store the bytes in "buffer"
	send_ethernet_packet(buffer, length);							// platform-specific call to send an Ethernet packet


	// in another IED...


	// receive GOOSE or SV packet
	length = recv_ethernet_packet(buffer);							// platform-specific call to receive an Ethernet packet
	gse_sv_packet_filter(buffer, length);							// deals with any GOOSE or SV dataset that is able to be processed

	// read value that was updated by the packet (it will equal 1.024)
	float inputValue = D1Q1SB4.S1.C1.RSYNa_1.gse_inputs_ItlPositions.E1Q1SB1_C1_Positions.C1_TVTR_1_Vol_instMag.f;

	return 0;
}
```

The data structures used for generating GOOSE and SV packets are stored within `LN0`. GOOSE packets are generated by calling the appropriate `send(buffer, statusChange, timeAllowedToLive)` function, where `statusChange` should be `1` if any value in the dataset has changed, and where `timeAllowedToLive` is the time in milliseconds for the receiver to wait for the next re-transmission. SV packets are sent by calling the `update(buffer)` function, which returns `0` if the next ASDU was written, but other ASDUs are free. It returns the size of the packet when all ASDUs have been written (and `buffer` contains the packet data). Clearly, a real implementation might include the use of platform-specific timers, interrupts and callbacks, where needed.

The generated C code implements all IEDs specified in the SCD file. You can use the code to emulate the communications between several IEDs, or just use one IED's implementation.

### Callbacks after a dataset is decoded ###

Callbacks should be set up in the form:

```C
void SVcallbackFunction(CTYPE_INT16U smpCnt) {
	;
}

void GSEcallbackFunction(CTYPE_INT32U timeAllowedToLive, CTYPE_TIMESTAMP T, CTYPE_INT32U stNum, CTYPE_INT32U sqNum) {
	;
}

//...

D1Q1SB4.S1.C1.exampleMMXU_1.sv_inputs_rmxuCB.datasetDecodeDone = &SVcallbackFunction;
D1Q1SB4.S1.C1.RSYNa_1.gse_inputs_ItlPositions.datasetDecodeDone = &GSEcallbackFunction;
```

where `D1Q1SB4.S1.C1.exampleMMXU_1` is a Logical Node defined in `datatypes.h` (and `ied.h`). `rmxuCB` is the name of the `SampledValueControl`, in a different IED, which send the SV packets. After being initialised, the callback function will be executed after this dataset is successfully decoded, to allow the LN to deal with the new data. For example, by default, only one packet of data is saved for each GSE or SV Control - and is overwritten when a new packet arrives. Therefore, it may be useful to use the callback to log the data to a separate memory buffer.

### Fixed-length GOOSE encoding ###

To enable fixed-length GOOSE encoding, in `ctypes.h` set the value of `GOOSE_FIXED_SIZE` to '1'. Otherwise, it should have a value of `0`. This can only be enabled globally for all GOOSE encoding, rather than on a per Control basis.

### Platform-specific options ###

All platform-specific options can be edited in `ctypes.h` or `ctypes.c`. For example, for a big endian platform, change:

```C
#define LITTLE_ENDIAN		1
```

to:

```C
#define LITTLE_ENDIAN		0
```

All `CTYPE_*` definitions must map to local datatypes of the correct size and sign.

In `ctypes.c`, the basic library function `memcopy()` is used to copy bytes in order (according to platform endianness), and `reversememcpy()` copies the bytes of multi-byte data types in reverse order (for converting between endianness). Although these should work, they can be replaced with platform-specific alternatives for better performance.

The value of `TIMESTAMP_SUPPORTED` should be set to `0`, unless generating timestamps has been implemented for your platform. An implementation for Windows has been included by default.

## Known issues and possible features ##

 - Several data types are not yet supported. However, the main *useful* data types (integer, floating-point, and boolean) are supported.
 - FCDAs and ExtRefs cannot use the syntax "vector.mag.f" as values for data object or data attribute references.
 - Data types cannot contain arrays.
 - Does not find ExtRef DA satisfied by container DO within a dataset, where the DA is not explicitly in a dataset.

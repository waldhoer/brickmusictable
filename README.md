# BrickMusic

The Brick Music instrument is a LEGO brick based instrument that's played using LEGO bricks on LEGO plates in
combination with a web camera. The bricks are captured, tracked and sent to the audio engine, powered
by [Sonic Pi](https://sonic-pi.net).

## Visual setup

Ensure that the environment containing LEGO bricks and plates has good lightning.
In case of insufficient lightning environments or heavy shadows use additional light sources as lamps or flashlights.
For better results try putting the ground plate on a single-colored background or paper.
Ensure that the visual plate is not affected by shadows of the interacting musician.
If a background paper is added for better results or the LEGO ground plate color changes this needs to be adapted in
code.

## Maven Project Setup

To setup the program, use the attached Maven project structure for dependency import. Ensure that native opencv libraries are configured correctly.
Ensure that the correct webcamera driver is enabled in code before execution. (See the VisualManager)

## NXT Device Support

_LEGO Mindstorms NXT_ offers additional interaction possibilities.
To enable ensure that all required python libraries are installed
for [nxt-python](https://ni.srht.site/nxt-python/latest/index.html).
See its [installation steps](https://ni.srht.site/nxt-python/latest/installation.html).
If you are working on Windows and USB connection fails
try [Win32 LibUSB](https://sourceforge.net/projects/libusb-win32/files/).
To allow communication ensure that both java and python have permission to access a local network.

### NXT Error Handling

The NXT robot can help improving interaction methods. Upon NXT failure (due to errors in script,
communication or hardware) Brick Music falls back to its original state after a specific
timeout, ignoring missing inputs. This may lead to loss in quality but ensures stability.

## Configuration Variables

BrickMusic can be configured using the attached json settings file. It contains the following options:

* **BLOCKING_TIMEOUT:** The timeout for foot pedal blocking messages in seconds 
* **BPM:** The speed of playing in beats per minutes 
* **VOLUME:** The initial general volume as decimal 
* **CAMERA_INDEX:** Index for camera selection, usually 0 or 1 
* **DEBUG_MODE_ACTIVE:** Boolean defining if a general debug mode is active 
* **DEBUG_LEVEL_FINE:** Boolean defining if a fine debug mode is active 
* **ENABLE_CLICK:** Boolean defining if a click sound should be sent on each beat 
* **FRAME_BUFFER_SIZE:** Size of frame buffer used for brick map average  
* **FRAME_DELAY:** Delay between image analysis in milliseconds 
* **MAX_ROTATION_DIFF:** Maximum angle of bricks being annulled as error tolerance
* **SCRIPT_OUTPUT:** The log path of the nxt script file name 
* **SCRIPT_NAME:** The nxt python script file name
* **USE_HISTORY_AVERAGE:** Defines if a frame buffer average should be used 
* **90_DEGREE_ROTATION:** Defines if bricks are allowed to be rotated by 90 degrees
* **CROP_AREA_ZOOM:** Factor for ROI zoom as double
* **ROI_HORIZONTAL_SHIFT:** Absolute ROI horizontal correction shift in pixel
* **ROI_VERTICAL_SHIFT:** Absolute ROI correction vertical shift in pixel
* **ROI_HORIZONTAL_SCALE:** Horizontal ROI scaling factor
* **ROI_VERTICAL_SCALE:** Horizontal ROI scaling factor


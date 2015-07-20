[Popcorn Time for Android TV](https://github.com/r87DtqFjKerJ/popcorn-android-tv)
----

This project is built on top of [Popcorn Time for Android](https://github.com/popcorn-official/popcorn-android)
It uses the [Android TV Leanback framework](https://developer.android.com/training/tv/start/start.html) for a more TV-friendly UI, comparing to the Android mobile version.

## Important Notes ##
This project is NOT backward-compatible with the official popcorn-android version yet. It has been tested only on Nexus Player.

## What's Working ##
* Watching YouTube trailers
* Steaming movie-type media
* Subtitle support
* In-app search for movie-type media

## What's NOT Working ##
* TV-show-type media support
* Showing results in Android TV's global search
* Settings, including VPN support


## Build Instructions ##

The [gradle build system](http://tools.android.com/tech-docs/new-build-system/user-guide) will fetch all dependencies and generate
files you need to build the project. You first need to generate the
local.properties (replace YOUR_SDK_DIR by your actual android sdk dir)
file:

    $ echo "sdk.dir=YOUR_SDK_DIR" > local.properties

You can now sync, build and install the project:

    $ ./gradlew assembleDebug # assemble the debug .apk
    $ ./gradlew installDebug  # install the debug .apk if you have an
                              # emulator or an Android device connected

You can use [Android Studio](http://developer.android.com/sdk/installing/studio.html) by importing the project as a Gradle project.

## Directory structure ##

    `|-- base                            # base module (contains providers and streamer)
     |    |-- build.gradle               # base build script
     |    `-- src
     |          |-- main
     |                |-- assets         # base module assets
     |                |-- java           # base module java code
     |                `-- res            # base module resources
    `|-- app                             # tv app module
     |    |-- build.gradle               # tv app build script
     |    `-- src
     |          |-- main
     |                |-- java           # mobile module java code
     |                `-- res            # mobile module resources    
    `|-- vlc                             # vlc module (unused. TV module uses Android's native VideoView.)
     |    |-- build.gradle               # vlc module build script
     |    `-- src
     |          |-- main
     |                |-- jniLibs        # native LibVLC libraries
     |                |-- java           # LibVLC Java code
    `|-- xmlrpc                          # xmlrpc module
     |    |-- build.gradle               # xmlrpc build script
     |    `-- src
     |          |-- main
     |                |-- java           # xmlrpc module java code
    `|-- connectsdk                      # connectsdk module (unused. TV module doesn't need casting.)
          |-- build.gradle               # connectsdk build script
          `-- src
          |     |-- java                 # connectsdk module java code
          `-- core
          |     |-- src                  # connectsdk module core java code
          `-- modules
                |-- google_cast
                      |-- src            # connectsdk module google cast java code



## License

If you distribute a copy or make a fork of the project, you have to credit this project as source.

This program is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License for more details.

You should have received a copy of the GNU General Public License along with this program.  If not, see http://www.gnu.org/licenses/.

Note: some dependencies are external libraries, which might be covered by a different license compatible with the GPLv3. They are mentioned in NOTICE.md.

***

Released under the [GPL V3 license](https://git.popcorntime.io/popcorntime/android/blob/development/LICENSE.md).
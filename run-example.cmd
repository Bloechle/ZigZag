@echo off

REM Define directory paths
set INPUT_DIR=C:\Projects\ZigZag\eval\TestIn
set OUTPUT_DIR=C:\Projects\ZigZag\eval\TestOut
set JAR_PATH=C:\Projects\ZigZag\ZigZag.jar

REM Define modes
set MODE_BINARY=0
set MODE_UPSAMPLED=1
set MODE_ANTIALIASED=2
set MODE_GRAY_LEVEL=3
set MODE_COLOR=4

REM Apply the parameters
set MODE=%MODE_ANTIALIASED%
set SIZE=30
set PERCENT=100
set LOSSLESS=true
set DEBUG=true

REM Execute the Java command to convert the files
java -cp "%JAR_PATH%" sugarcube.zigzag.ZigZag -mode %MODE% -size %SIZE% -percent %PERCENT% -lossless %LOSSLESS% -debug %DEBUG% -input "%INPUT_DIR%" -output "%OUTPUT_DIR%"

echo Conversion done.
pause

<img src="logo.png" alt="ZigZag Logo" width="100"/> 

# **ZigZag**

**ZigZag** is a robust algorithm for both image binarization and background removal. Leveraging the strengths of our previous algorithm, YinYang, ZigZag provides superior speed, accuracy, and simplicity.

The **ZigZag.jar** Java archive is generated periodically from the source code for convenience. Be careful, as it may not be the latest version (but should).

**Check out the <a href="https://d23kqqls.codepad.live/" target="_blank">demo CodePad</a>** for a live demonstration of ZigZag in action.


## **Requires**

- **Java 8** (that's all)

## **How to Run ZigZag**

### **By calling Java API**

To use the API, create a new instance of **ZigZag** with the desired parameters and call the filter method with the input and output image paths.

```java
ZigZag zigzag = new ZigZag(int size, int percent, int mode);
zigzag.applyFilter(inputImagePath, outputImagePath);
```

**Example:**

```java
ZigZag zigzag = new ZigZag(30, 100, ImageFilter.MODE_BINARY_UPSAMPLED);
zigzag.applyFilter("C:/MyImage.jpg", "C:/MyBinarizedImage.png");
```

### **By launching Java runtime**

To run the tool using the Java runtime, use the following command format (have a watch at 'run-example.cmd') :

```sh
java -cp <zigZagJarPath> zig.zag.ZigZag -size <size> -percent <percent> -mode <mode> -threads <threads> -input <inputFilePath> [-output <outputFilePath>] [-debug <true/false>] [-exit <true/false>]
```

**Example:**

```sh
java -cp "C:/path/ZigZag.jar" zig.zag.ZigZag -size 30 -percent 100 -mode 3 -threads 4 -input "C:/path/MyImage.jpg" -output "C:/path/MyBinarizedImage.png"
```

### **Command Line Options**

- **-input <inputFilePath>**: Path to the input image file or directory (required).
- **-output <outputFilePath>**: Path to the output image file or directory (optional, default is inputFilePath with 'ZZ' postfix).
- **-size <size>**: Windows size, typical values can range from 10 to 100 pixels (optional, default is 30).
- **-percent <percent>**: Mean weight for historical documents, typical values can range from 50 to 100 percent (optional, default is 100).
- **-mode <mode>**: Processing mode (optional, default is 1). Available modes:
  - 0: Standard binarization mode.
  - 1: Upsampled binarization mode (x2).
  - 2: Antialiased binarization mode.
  - 3: Background removal with gray-level foreground.
  - 4: Background removal with color foreground.
- **-threads <threads>**: Number of threads for processing (optional, default is half the number of available processors).
- **-lossless <true/false>**: Enable or disable lossless compression (optional, default is true). 
- **-debug <true/false>**: Enable or disable debug mode (optional, default is false).
- **-exit <true/false>**: Whether to exit the application after processing (optional, default is false).

## **Citation**

If you use this code for your research, please cite the following paper available on ACM Open Access:

```
@inproceedings{10.1145/3573128.3609354,
author = {Bloechle, Jean-Luc and Hennebert, Jean and Gisler, Christophe},
title = {YinYang, a Fast and Robust Adaptive Document Image Binarization for Optical Character Recognition},
year = {2023},
isbn = {9798400700279},
publisher = {Association for Computing Machinery},
address = {New York, NY, USA},
url = {https://doi.org/10.1145/3573128.3609354},
doi = {10.1145/3573128.3609354},
abstract = {Optical Character Recognition (OCR) from document photos taken by cell phones is a challenging task. Most OCR methods require prior binarization of the image, which can be difficult to achieve when documents are captured with various mobile devices in unknown lighting conditions. For example, shadows cast by the camera or the camera holder on a hard copy can jeopardize the binarization process and hinder the next OCR step. In the case of highly uneven illumination, binarization methods using global thresholding simply fail, and state-of-the-art adaptive algorithms often deliver unsatisfactory results. In this paper, we present a new binarization algorithm using two complementary local adaptive passes and taking advantage of the color components to improve results over current image binarization methods. The proposed approach gave remarkable results at the DocEng'22 competition on the binarization of photographed documents.},
booktitle = {Proceedings of the ACM Symposium on Document Engineering 2023},
articleno = {19},
numpages = {4},
keywords = {image thresholding, image processing, binarization, OCR},
location = {Limerick, Ireland},
series = {DocEng '23}
}
```

**ZigZag** paper is will be published in **DocEng'24**, in the meantime, citing the **YinYang** paper is just fine :-)

## **License**

**ZigZag** is provided under the terms of **GPLv3**. The GNU General Public License version 3 or GPLv3 is a copyleft license, which means that any derivative works (modifications or extensions) of a GPL-licensed software must also be licensed under the GPL-3 or a compatible license. This requirement ensures that open-source code remains open source.
